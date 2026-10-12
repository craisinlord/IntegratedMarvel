package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.craisinlord.antarchy.content.AntarchyTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class ScarletWitchPower {
    public static final String ID = "chaos_magic_wanda_maximoff";
    private static final ResourceLocation FLIGHT_ID = ResourceLocation.fromNamespaceAndPath("integrated_superheroes", "wanda_flight");
    private static final DustParticleOptions HEX_RED = new DustParticleOptions(new Vector3f(0.86F, 0.05F, 0.16F), 1.4F);
    private static final double BOLT_SPEED = 1.1D;
    private static final List<EntityType<? extends Mob>> HARMLESS = List.of(EntityType.CHICKEN, EntityType.RABBIT, EntityType.SHEEP,
            EntityType.PIG, EntityType.COD, EntityType.BAT);
    public static final HeroPower.Toggle HEX_FIELD = new HeroPower.Toggle("integrated_superheroes_off_hex_field", "Hex Field",
            () -> HeroFx.item("witchery:happenstance_oil", net.minecraft.world.item.Items.DRAGON_BREATH));
    private static final Map<UUID, List<Bolt>> BOLTS = new HashMap<>();
    private static final Map<Integer, Curse> MISFIRE = new HashMap<>();
    private static final Map<Integer, Curse> BAD_LUCK = new HashMap<>();
    private static final Map<Integer, Curse> TURNED = new HashMap<>();
    private static final Map<UUID, Float> LAST_HEALTH = new HashMap<>();
    private static final Map<UUID, Long> PROBABILITY_READY = new HashMap<>();

    private ScarletWitchPower() {
    }

    private static final class Bolt {
        private Vec3 pos;
        private Vec3 velocity;
        private int age;
        private final int maxAge;
        private final float damage;

        private Bolt(Vec3 pos, Vec3 velocity, int maxAge, float damage) {
            this.pos = pos;
            this.velocity = velocity;
            this.maxAge = maxAge;
            this.damage = damage;
        }
    }

    private record Curse(UUID wanda, long expires) {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Chaos Magic (Wanda Maximoff)",
                syringe,
                MarvelBalanceConfig.SCARLET_WITCH,
                List.of(),
                List.of(
                        new HeroPower.Ability("hex_bolt", "Hex Bolt", ScarletWitchPower::hexBolt),
                        new HeroPower.Ability("telekinesis", "Telekinesis", ScarletWitchPower::telekinesis),
                        new HeroPower.Ability("reality_warp", "Reality Warp", ScarletWitchPower::realityWarp),
                        HeroToggles.WHEEL
                ),
                List.of(HeroToggles.FLIGHT, HEX_FIELD),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        HeroFlight.tick(player, FLIGHT_ID, HeroToggles.isOn(player, HeroToggles.FLIGHT), MarvelBalanceConfig.WANDA_FLY_SPEED.get().floatValue());
                        HeroGrip.tick(player);
                        HeroBlockLift.tick(player);
                        tickBolts(player);
                        LAST_HEALTH.put(player.getUUID(), player.getHealth());
                        if (player.getAbilities().flying && player.tickCount % 2 == 0) {
                            player.serverLevel().sendParticles(HEX_RED, player.getX(), player.getY() + 0.2D, player.getZ(), 2, 0.25D, 0.1D, 0.25D, 0.0D);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroFlight.remove(player, FLIGHT_ID);
                        HeroGrip.clear(player);
                        HeroBlockLift.drop(player);
                        BOLTS.remove(player.getUUID());
                        LAST_HEALTH.remove(player.getUUID());
                        PROBABILITY_READY.remove(player.getUUID());
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (!HeroToggles.isOn(player, HEX_FIELD) || !(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == player) {
                            return;
                        }
                        double radius = MarvelBalanceConfig.HEX_FIELD_RADIUS.get();
                        if (attacker.distanceToSqr(player) > radius * radius || player.getRandom().nextDouble() >= MarvelBalanceConfig.HEX_FIELD_CHANCE.get()) {
                            return;
                        }
                        event.setCanceled(true);
                        player.causeFoodExhaustion(0.5F);
                        Vec3 center = player.getBoundingBox().getCenter();
                        player.serverLevel().sendParticles(HEX_RED, center.x, center.y, center.z, 16, 0.5D, 0.6D, 0.5D, 0.0D);
                        HeroFx.play(player, SoundEvents.ILLUSIONER_CAST_SPELL, 0.6F, 1.6F);
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        long now = player.level().getGameTime();
                        Long ready = PROBABILITY_READY.get(player.getUUID());
                        if (ready != null && now < ready) {
                            return;
                        }
                        event.setCanceled(true);
                        player.setHealth(Math.max(1.0F, LAST_HEALTH.getOrDefault(player.getUUID(), 1.0F)));
                        PROBABILITY_READY.put(player.getUUID(), now + (long) (MarvelBalanceConfig.PROBABILITY_DODGE_COOLDOWN.get() * 20.0D));
                        Vec3 center = player.getBoundingBox().getCenter();
                        player.serverLevel().sendParticles(HEX_RED, center.x, center.y, center.z, 60, 0.6D, 1.0D, 0.6D, 0.0D);
                        player.serverLevel().sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        HeroFx.play(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.2F, 0.8F);
                        player.displayClientMessage(Component.literal("§cProbability bends. It missed."), true);
                        HeroAdvancements.count(player, "avengers/scarlet_witch/it_missed", 1.0D, 10.0D);
                    }
                }
        );
    }

    private static double hexBolt(ServerPlayer player, int slot) {
        float damage = (float) (HeroPowers.powerDamage(player) * MarvelBalanceConfig.HEX_BOLT_DAMAGE_MULTIPLIER.get());
        int maxAge = (int) Math.ceil(MarvelBalanceConfig.HEX_BOLT_RANGE.get() / BOLT_SPEED);
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.8D)).subtract(0.0D, 0.2D, 0.0D);
        BOLTS.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>()).add(new Bolt(start, player.getLookAngle().scale(BOLT_SPEED), maxAge, damage));
        HeroFx.play(player, SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.3F);
        HeroAnimations.play(player, "web_shoot_r");
        return MarvelBalanceConfig.HEX_BOLT_COOLDOWN.get();
    }

    private static void tickBolts(ServerPlayer player) {
        List<Bolt> bolts = BOLTS.get(player.getUUID());
        if (bolts == null || bolts.isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Iterator<Bolt> iterator = bolts.iterator();
        while (iterator.hasNext()) {
            Bolt bolt = iterator.next();
            Bolt current = bolt;
            LivingEntity seek = level.getEntitiesOfClass(LivingEntity.class, new AABB(bolt.pos, bolt.pos).inflate(6.0D),
                            entity -> entity != player && entity.isAlive() && entity.getBoundingBox().getCenter().subtract(current.pos).dot(current.velocity) > 0.0D)
                    .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(current.pos))).orElse(null);
            if (seek != null) {
                Vec3 want = seek.getBoundingBox().getCenter().subtract(bolt.pos).normalize().scale(BOLT_SPEED);
                bolt.velocity = bolt.velocity.scale(0.82D).add(want.scale(0.18D)).normalize().scale(BOLT_SPEED);
            }
            Vec3 next = bolt.pos.add(bolt.velocity);
            HitResult blockHit = level.clip(new ClipContext(bolt.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 end = blockHit.getType() == HitResult.Type.MISS ? next : blockHit.getLocation();
            LivingEntity victim = level.getEntitiesOfClass(LivingEntity.class, new AABB(bolt.pos, end).inflate(0.4D),
                            entity -> entity != player && entity.isAlive() && entity.getBoundingBox().inflate(0.4D).clip(current.pos, end).isPresent())
                    .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(current.pos))).orElse(null);
            HeroAbilities.particleLine(level, bolt.pos, end, HEX_RED, 0.35D);
            if (victim != null) {
                hit(player, victim, bolt.damage);
                iterator.remove();
                continue;
            }
            if (blockHit.getType() != HitResult.Type.MISS || ++bolt.age >= bolt.maxAge) {
                level.sendParticles(ParticleTypes.WITCH, end.x, end.y, end.z, 10, 0.2D, 0.2D, 0.2D, 0.05D);
                iterator.remove();
                continue;
            }
            bolt.pos = next;
        }
    }

    private static void hit(ServerPlayer player, LivingEntity target, float damage) {
        target.hurt(player.damageSources().indirectMagic(player, player), damage);
        Vec3 center = target.getBoundingBox().getCenter();
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.WITCH, center.x, center.y, center.z, 20, 0.3D, 0.4D, 0.3D, 0.05D);
        level.sendParticles(HEX_RED, center.x, center.y, center.z, 20, 0.4D, 0.5D, 0.4D, 0.0D);
        HeroFx.play(target, SoundEvents.EVOKER_CAST_SPELL, 0.8F, 0.7F);
        if (!target.isAlive() || !HeroAbilities.canControl(target)) {
            return;
        }
        List<? extends String> hexes = MarvelBalanceConfig.HEX_EFFECTS.get();
        if (hexes.isEmpty()) {
            return;
        }
        String hex = hexes.get(player.getRandom().nextInt(hexes.size()));
        long now = level.getGameTime();
        switch (hex) {
            case "jinxed" -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
            case "misfire" -> MISFIRE.put(target.getId(), new Curse(player.getUUID(), now + 160));
            case "turned" -> {
                if (target instanceof Mob) {
                    TURNED.put(target.getId(), new Curse(player.getUUID(), now + 120));
                    retarget(level, (Mob) target);
                }
            }
            case "pratfall" -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 3));
            case "bad_luck" -> BAD_LUCK.put(target.getId(), new Curse(player.getUUID(), now + 400));
            case "unravel" -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
            case "dazed" -> {
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                if (target instanceof Mob mob) {
                    mob.setTarget(null);
                }
            }
            default -> {
                return;
            }
        }
        player.displayClientMessage(Component.literal("§cHex: " + label(hex)), true);
        HeroAdvancements.flag(player, "hex_" + hex);
        if (java.util.stream.Stream.of("jinxed", "misfire", "turned", "pratfall", "bad_luck", "unravel", "dazed")
                .allMatch(name -> HeroAdvancements.flagged(player, "hex_" + name))) {
            HeroAdvancements.award(player, "avengers/scarlet_witch/hex_roulette");
        }
    }

    private static String label(String hex) {
        return switch (hex) {
            case "jinxed" -> "Jinxed";
            case "misfire" -> "Misfire";
            case "turned" -> "Turned";
            case "pratfall" -> "Pratfall";
            case "bad_luck" -> "Bad Luck";
            case "unravel" -> "Unravel";
            case "dazed" -> "Dazed";
            default -> hex;
        };
    }

    private static void retarget(ServerLevel level, Mob mob) {
        LivingEntity other = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(12.0D),
                        entity -> entity != mob && entity.isAlive() && entity instanceof Mob)
                .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(mob))).orElse(null);
        mob.setTarget(other);
    }

    public static void tickLevel(ServerLevel level) {
        if (TURNED.isEmpty() && MISFIRE.isEmpty() && BAD_LUCK.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, Curse>> iterator = TURNED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Curse> entry = iterator.next();
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null) {
                continue;
            }
            if (!(entity instanceof Mob mob) || !mob.isAlive() || now >= entry.getValue().expires()) {
                if (entity instanceof Mob mob) {
                    mob.setTarget(null);
                }
                iterator.remove();
                continue;
            }
            if (now % 10 == 0 && (mob.getTarget() == null || !mob.getTarget().isAlive() || mob.getTarget() instanceof ServerPlayer)) {
                retarget(level, mob);
            }
        }
        MISFIRE.values().removeIf(curse -> now >= curse.expires());
        BAD_LUCK.values().removeIf(curse -> now >= curse.expires());
    }

    public static void onAnyDamage(LivingIncomingDamageEvent event) {
        if (MISFIRE.isEmpty() && BAD_LUCK.isEmpty() || !(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (!(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = attacker.getBoundingBox().getCenter();
        Curse misfire = MISFIRE.remove(attacker.getId());
        if (misfire != null) {
            event.setCanceled(true);
            ServerPlayer wanda = level.getServer().getPlayerList().getPlayer(misfire.wanda());
            if (wanda != null && wanda.isAlive()) {
                wanda.heal(Math.min(event.getAmount(), wanda.getMaxHealth() * 0.25F));
                level.sendParticles(ParticleTypes.HEART, wanda.getX(), wanda.getY() + 2.0D, wanda.getZ(), 3, 0.3D, 0.2D, 0.3D, 0.0D);
            }
            level.sendParticles(HEX_RED, center.x, center.y, center.z, 12, 0.4D, 0.4D, 0.4D, 0.0D);
            return;
        }
        if (BAD_LUCK.remove(attacker.getId()) != null) {
            event.setCanceled(true);
            level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 8, 0.3D, 0.3D, 0.3D, 0.01D);
            level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, attacker.getSoundSource(), 1.0F, 0.8F);
        }
    }

    private static double telekinesis(ServerPlayer player, int slot) {
        if (HeroGrip.isHolding(player) || HeroBlockLift.isLifting(player)) {
            if (HeroGrip.isHolding(player)) {
                HeroGrip.release(player, MarvelBalanceConfig.TELEKINESIS_THROW_SPEED.get(), (float) (HeroPowers.powerDamage(player) * 0.8D));
            } else {
                HeroBlockLift.throwHeld(player);
            }
            HeroAnimations.play(player, "celestisynth:cs_crescentia_throw");
            HeroFx.play(player, SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 0.8F);
            return MarvelBalanceConfig.TELEKINESIS_COOLDOWN.get();
        }
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), MarvelBalanceConfig.TELEKINESIS_RANGE.get(), 1);
        if (beam.targets().isEmpty()) {
            if (HeroBlockLift.lift(player, blockLift())) {
                HeroAnimations.play(player, "web_shoot_both");
            }
            return 0.0D;
        }
        boolean grabbed = HeroGrip.grab(player, beam.targets().get(0), 120, HEX_RED,
                held -> held.addEffect(new MobEffectInstance(MobEffects.GLOWING, 5, 0, false, false)));
        if (grabbed) {
            HeroAnimations.play(player, "web_shoot_both");
            HeroFx.play(player, SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 1.2F);
        }
        return 0.0D;
    }

    private static HeroBlockLift.Profile blockLift() {
        return new HeroBlockLift.Profile(MarvelBalanceConfig.TELEKINESIS_RANGE.get(), MarvelBalanceConfig.TELEKINESIS_MAX_BLOCKS.get(),
                MarvelBalanceConfig.TELEKINESIS_HOLD_TICKS.get(), MarvelBalanceConfig.TELEKINESIS_THROW_SPEED.get(), 0.8D,
                state -> !(state.getBlock() instanceof LiquidBlock) && !state.is(BlockTags.BEDS) && !state.is(AntarchyTags.Blocks.GRAVITY_GUN_BLACKLIST),
                (level, center) -> level.sendParticles(HEX_RED, center.x, center.y, center.z, 4, 0.45D, 0.45D, 0.45D, 0.0D),
                () -> SoundEvents.ILLUSIONER_CAST_SPELL, () -> SoundEvents.BEACON_AMBIENT, () -> SoundEvents.ILLUSIONER_CAST_SPELL);
    }

    private static double realityWarp(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        double radius = MarvelBalanceConfig.REALITY_WARP_RADIUS.get();
        List<Mob> targets = new ArrayList<>(level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
                mob -> mob instanceof Enemy && mob.isAlive() && HeroAbilities.canControl(mob) && mob.distanceToSqr(player) <= radius * radius));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.literal("§7Nothing here to rewrite."), true);
            return 0.0D;
        }
        targets.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
        int count = Math.min(targets.size(), MarvelBalanceConfig.REALITY_WARP_MAX_TARGETS.get());
        int rewritten = 0;
        for (int i = 0; i < count; i++) {
            Mob mob = targets.get(i);
            Vec3 center = mob.getBoundingBox().getCenter();
            EntityType<? extends Mob> type = HARMLESS.get(player.getRandom().nextInt(HARMLESS.size()));
            if (mob.convertTo(type, false) != null) {
                rewritten++;
                level.sendParticles(HEX_RED, center.x, center.y, center.z, 30, 0.5D, 0.6D, 0.5D, 0.0D);
                level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 8, 0.3D, 0.3D, 0.3D, 0.02D);
            }
        }
        if (rewritten >= 6) {
            HeroAdvancements.award(player, "avengers/scarlet_witch/no_more_mobs");
        }
        level.sendParticles(HEX_RED, player.getX(), player.getY() + 1.0D, player.getZ(), 120, radius * 0.4D, 1.0D, radius * 0.4D, 0.0D);
        HeroFx.play(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.5F, 0.7F);
        HeroFx.play(player, SoundEvents.EVOKER_PREPARE_WOLOLO, 1.5F, 0.8F);
        HeroAnimations.play(player, "celestisynth:cs_solaris_spin");
        player.displayClientMessage(Component.literal("§cNo more."), true);
        return MarvelBalanceConfig.REALITY_WARP_COOLDOWN.get();
    }
}
