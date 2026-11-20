package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.api.RogueAbsorption;
import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.craisinlord.integrated_superheroes.hero.skin.SerumCostumes;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class JeanGreyPower {
    public static final String ID = "telekinesis_jean_grey";
    public static final String GRAB = "jean_telekinetic_grab";
    public static final String THROW = "jean_throw";
    public static final String PHOENIX = "jean_phoenix";
    public static final HeroPower.Toggle LEVITATION = new HeroPower.Toggle(HeroToggles.FLIGHT.flag(), "Levitation", () -> Items.PHANTOM_MEMBRANE);
    public static final HeroPower.Toggle RESCUE = new HeroPower.Toggle("integrated_superheroes_off_jean_rescue", "Lift Allies", () -> Items.LEAD);
    private static final ResourceLocation FLIGHT_ID = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "jean_levitation");
    private static final int TK_PINK = 0xFF4FA8;
    private static final int PHOENIX_GOLD = 0xFFFF8A1E;
    private static final Map<UUID, Phoenix> PHOENIXES = new HashMap<>();
    private static RogueMemory rogue;
    private static boolean registered;

    private static final class Phoenix {
        private float meter;
        private long lastCombat;
        private long burnStart;
        private long burnUntil;
    }

    private JeanGreyPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            TelekineticFormation.register();
            NeoForge.EVENT_BUS.addListener(JeanGreyPower::onLogout);
            NeoForge.EVENT_BUS.addListener(JeanGreyPower::onChangeDimension);
            SerumCostumes.register(ID, "thematic:jean_grey");
            RogueAbsorption.remember(rogueMemory());
        }
        return new HeroPower(
                ID,
                "Telekinesis (Jean Grey)",
                syringe,
                JeanGreyConfig.STATS,
                List.of(),
                List.of(
                        new HeroPower.Ability(GRAB, "Telekinetic Grab", JeanGreyPower::grab),
                        new HeroPower.Ability(THROW, "Throw", JeanGreyPower::fling),
                        new HeroPower.Ability(PHOENIX, "Phoenix", JeanGreyPower::phoenix),
                        HeroToggles.WHEEL
                ),
                List.of(LEVITATION, RESCUE),
                new HeroPower.Hooks() {
                    @Override
                    public MarvelBalanceConfig.HeroStats stats(Player player) {
                        return ignited(player) ? JeanGreyConfig.PHOENIX_STATS : null;
                    }

                    @Override
                    public void tick(ServerPlayer player) {
                        boolean burning = ignited(player);
                        boolean levitate = HeroToggles.isOn(player, LEVITATION) && !VillainWorld.suppressed(player) && !HealingFactor.isDowned(player);
                        float speed = (burning ? JeanGreyConfig.PHOENIX_FLY_SPEED : JeanGreyConfig.FLY_SPEED).get().floatValue();
                        HeroFlight.tick(player, FLIGHT_ID, levitate, speed);
                        if (player.getAbilities().flying && player.tickCount % 4 == 0) {
                            player.serverLevel().sendParticles(burning ? ParticleTypes.FLAME : HeroAbilities.emissive(color(player), 0.6F),
                                    player.getX(), player.getY() + 0.1D, player.getZ(), 2, 0.25D, 0.05D, 0.25D, 0.0D);
                        }
                        tickPhoenix(player);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (ignited(player) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
                            event.setCanceled(true);
                            player.clearFire();
                            return;
                        }
                        charge(player, event.getAmount() / JeanGreyConfig.DAMAGE_TAKEN_PER_PERCENT.get().floatValue());
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        charge(player, event.getAmount() / JeanGreyConfig.DAMAGE_DEALT_PER_PERCENT.get().floatValue());
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroFlight.remove(player, FLIGHT_ID);
                        TelekineticFormation.releaseOwner(player.getUUID());
                        PHOENIXES.remove(player.getUUID());
                        SerumKit.meter(player, -1.0F);
                        SerumKit.clearMeter(player);
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        if (rebirth(player)) {
                            event.setCanceled(true);
                            return;
                        }
                        TelekineticFormation.releaseOwner(player.getUUID());
                        PHOENIXES.remove(player.getUUID());
                        SerumKit.meter(player, -1.0F);
                    }
                }
        );
    }

    public static boolean ignited(Player player) {
        Phoenix state = PHOENIXES.get(player.getUUID());
        return state != null && state.burnUntil > 0L;
    }

    private static int color(Player player) {
        return SerumKit.primary(player, TK_PINK) & 0xFFFFFF;
    }

    private static TelekineticFormation.Style style(Player player) {
        return ignited(player) ? new TelekineticFormation.Style(PHOENIX_GOLD & 0xFFFFFF, ParticleTypes.FLAME)
                : new TelekineticFormation.Style(color(player), ParticleTypes.END_ROD);
    }

    static boolean willing(ServerPlayer player, LivingEntity target) {
        if (target instanceof Player other) {
            return other.isShiftKeyDown() && !other.isCreative() && !other.isSpectator();
        }
        return target instanceof TamableAnimal tamable && tamable.isTame() && player.getUUID().equals(tamable.getOwnerUUID());
    }

    static LivingEntity aim(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(range));
        HitResult block = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        Vec3 stop = end;
        return player.level().getEntitiesOfClass(LivingEntity.class, new AABB(start, stop).inflate(1.0D),
                        entity -> entity != player && entity.isAlive() && !(entity instanceof ArmorStand)
                                && !TelekineticFormation.holding(player, entity)
                                && entity.getBoundingBox().inflate(0.3D).clip(start, stop).isPresent())
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(start)))
                .orElse(null);
    }

    private static double grab(ServerPlayer player, int slot) {
        if (TelekineticFormation.count(player) >= JeanGreyConfig.MAX_HELD.get()) {
            player.displayClientMessage(Component.literal("§7You can't hold any more. Throw or set something down first."), true);
            return 0.0D;
        }
        LivingEntity target = aim(player, JeanGreyConfig.GRAB_RANGE.get());
        if (target == null) {
            player.displayClientMessage(Component.literal("§7Look at something to lift it."), true);
            return 0.0D;
        }
        boolean willing = willing(player, target);
        if (willing && !HeroToggles.isOn(player, RESCUE)) {
            player.displayClientMessage(Component.literal("§7Lifting allies is switched off."), true);
            return 0.0D;
        }
        if (!willing) {
            if (target instanceof Player) {
                player.displayClientMessage(Component.literal("§7They have to crouch to let you lift them."), true);
                return 0.0D;
            }
            if (!KineticBurst.affects(player, target)) {
                player.displayClientMessage(Component.literal("§7You won't lift a friend without asking."), true);
                return 0.0D;
            }
            if (HeroAbilities.isBoss(target) || !HeroAbilities.canControl(target)) {
                player.displayClientMessage(Component.literal("§7Too strong to lift."), true);
                return 0.0D;
            }
        }
        int ticks = willing ? JeanGreyConfig.RESCUE_HOLD_TICKS.get() : JeanGreyConfig.HOSTILE_HOLD_TICKS.get();
        float crush = (float) (MarvelBalanceConfig.GRIP_THROW_DAMAGE.get() * JeanGreyConfig.CRUSH_MULTIPLIER.get());
        TelekineticFormation.Result result = TelekineticFormation.hold(player, target, ticks, willing, 0.0F, crush, JeanGreyConfig.MAX_HELD.get(), style(player));
        switch (result) {
            case BUSY -> {
                player.displayClientMessage(Component.literal("§7Something else already has a hold on that."), true);
                return 0.0D;
            }
            case IMMUNE -> {
                player.displayClientMessage(Component.literal("§7It's still shaking off the last lift."), true);
                return 0.0D;
            }
            case FULL -> {
                return 0.0D;
            }
            case INELIGIBLE -> {
                player.displayClientMessage(Component.literal("§7Too strong to lift."), true);
                return 0.0D;
            }
            default -> {
            }
        }
        ServerLevel level = player.serverLevel();
        Vec3 center = target.getBoundingBox().getCenter();
        int tint = ignited(player) ? PHOENIX_GOLD & 0xFFFFFF : color(player);
        HeroAbilities.particleLine(level, player.getEyePosition().add(player.getLookAngle()), center, HeroAbilities.emissive(tint, 0.7F), 0.4D);
        SerumKit.ring(level, new Vec3(center.x, target.getY() + 0.1D, center.z), HeroAbilities.emissive(tint, 0.7F), target.getBbWidth() + 0.3D, 12);
        HeroFx.play(target, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.2F);
        HeroFx.play(player, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 0.5F, 1.6F);
        HeroAnimations.play(player, "web_shoot_r");
        if (TelekineticFormation.count(player) >= 3) {
            HeroAdvancements.award(player, "x_men/jean_grey/juggler");
        }
        if (willing) {
            player.displayClientMessage(Component.literal("§d" + target.getName().getString() + " is safe with you."), true);
            if (target instanceof ServerPlayer other) {
                other.displayClientMessage(Component.literal("§dYou're being carried. Crouch again to drop."), true);
            }
        }
        return JeanGreyConfig.GRAB_COOLDOWN.get();
    }

    private static double fling(ServerPlayer player, int slot) {
        if (TelekineticFormation.count(player) == 0) {
            player.displayClientMessage(Component.literal("§7You aren't holding anything."), true);
            return 0.0D;
        }
        if (player.isShiftKeyDown()) {
            TelekineticFormation.setDownAll(player);
            HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F);
            HeroAnimations.play(player, "web_shoot_both");
            return JeanGreyConfig.SET_DOWN_COOLDOWN.get();
        }
        double anchor = MarvelBalanceConfig.GRIP_THROW_DAMAGE.get();
        double speed = MarvelBalanceConfig.GRIP_THROW_SPEED.get() * JeanGreyConfig.THROW_SPEED_MULTIPLIER.get();
        int thrown = TelekineticFormation.throwAll(player, player.getLookAngle(), speed,
                anchor * JeanGreyConfig.THROW_DAMAGE_MULTIPLIER.get(), anchor * JeanGreyConfig.THROW_CAST_CAP_MULTIPLIER.get());
        HeroAnimations.play(player, "web_shoot_both");
        if (thrown == 0) {
            HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F);
            return JeanGreyConfig.SET_DOWN_COOLDOWN.get();
        }
        HeroFx.play(player, SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 1.4F);
        HeroFx.play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 0.7F);
        Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(1.2D));
        player.serverLevel().sendParticles(ignited(player) ? ParticleTypes.FLAME : ParticleTypes.END_ROD, hand.x, hand.y, hand.z, 8, 0.2D, 0.2D, 0.2D, 0.15D);
        return JeanGreyConfig.THROW_COOLDOWN.get();
    }

    static void phoenixBurst(ServerPlayer player, LivingEntity landed, Vec3 center, float throwDamage) {
        ServerLevel level = player.serverLevel();
        double radius = JeanGreyConfig.BURST_RADIUS.get();
        float damage = (float) (throwDamage * JeanGreyConfig.BURST_MULTIPLIER.get());
        if (radius <= 0.0D) {
            return;
        }
        landed.igniteForSeconds(5.0F);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                candidate -> candidate != landed && candidate != player && candidate.isAlive()
                        && candidate.distanceToSqr(center) <= radius * radius && KineticBurst.affects(player, candidate))) {
            HeroAbilities.runAsAbilityDamage(() -> other.hurt(player.damageSources().playerAttack(player), damage));
            other.igniteForSeconds(5.0F);
        }
        if (!HeroFx.vfx(level, center, "pyre_detonation", PHOENIX_GOLD, (float) radius)) {
            level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 30, radius * 0.4D, 0.5D, radius * 0.4D, 0.05D);
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        HeroFx.play(landed, SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F);
    }

    private static Phoenix state(ServerPlayer player) {
        return PHOENIXES.computeIfAbsent(player.getUUID(), ignored -> new Phoenix());
    }

    private static void charge(ServerPlayer player, float percent) {
        if (percent <= 0.0F || VillainWorld.suppressed(player)) {
            return;
        }
        Phoenix state = state(player);
        state.lastCombat = player.level().getGameTime();
        if (state.burnUntil > 0L || state.meter >= 100.0F) {
            return;
        }
        state.meter = Math.min(100.0F, state.meter + percent);
        if (state.meter >= 100.0F) {
            player.displayClientMessage(Component.literal("§6The Phoenix is ready."), true);
            HeroFx.play(player, SoundEvents.BLAZE_AMBIENT, 0.8F, 0.6F);
        }
    }

    private static void tickPhoenix(ServerPlayer player) {
        Phoenix state = PHOENIXES.get(player.getUUID());
        if (state == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (state.burnUntil > 0L) {
            if (now >= state.burnUntil || VillainWorld.suppressed(player)) {
                extinguish(player, state);
                HeroAbilities.setCooldown(player, PHOENIX, JeanGreyConfig.FADE_COOLDOWN.get());
                player.displayClientMessage(Component.literal("§7The fire goes out."), true);
                return;
            }
            SerumKit.meter(player, (float) (state.burnUntil - now) / Math.max(1L, state.burnUntil - state.burnStart));
            burn(player, now);
            return;
        }
        if (now - state.lastCombat > JeanGreyConfig.METER_DECAY_DELAY_TICKS.get() && state.meter > 0.0F) {
            state.meter = Math.max(0.0F, state.meter - JeanGreyConfig.METER_DECAY_PER_SECOND.get().floatValue() / 20.0F);
        }
        SerumKit.meter(player, state.meter <= 0.0F ? -1.0F : state.meter / 100.0F);
    }

    private static void burn(ServerPlayer player, long now) {
        ServerLevel level = player.serverLevel();
        if (player.isOnFire()) {
            player.clearFire();
        }
        if (now % 3L == 0L) {
            Vec3 back = player.position().add(0.0D, 1.2D, 0.0D).subtract(player.getLookAngle().multiply(1.0D, 0.0D, 1.0D).scale(0.4D));
            level.sendParticles(ParticleTypes.FLAME, back.x, back.y, back.z, 4, 0.6D, 0.5D, 0.6D, 0.01D);
            level.sendParticles(HeroAbilities.emissive(PHOENIX_GOLD & 0xFFFFFF, 0.8F), back.x, back.y, back.z, 2, 0.8D, 0.4D, 0.8D, 0.0D);
        }
        double radius = JeanGreyConfig.AURA_RADIUS.get();
        if (radius > 0.0D && now % 20L == 0L) {
            for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
                if (target instanceof Enemy && KineticBurst.affects(player, target)) {
                    target.igniteForSeconds(3.0F);
                }
            }
            SerumKit.ring(level, player.position().add(0.0D, 0.1D, 0.0D), ParticleTypes.FLAME, radius, 24);
        }
    }

    private static double phoenix(ServerPlayer player, int slot) {
        Phoenix state = state(player);
        if (state.burnUntil > 0L) {
            supernova(player, state);
            return JeanGreyConfig.SUPERNOVA_COOLDOWN.get();
        }
        if (state.meter < 100.0F) {
            player.displayClientMessage(Component.literal("§7The fire isn't ready yet. " + Math.round(state.meter) + "%"), true);
            return 0.0D;
        }
        if (VillainWorld.suppressed(player)) {
            return 0.0D;
        }
        ignite(player, state);
        return 0.0D;
    }

    private static void ignite(ServerPlayer player, Phoenix state) {
        long now = player.level().getGameTime();
        state.meter = 0.0F;
        state.burnStart = now;
        state.burnUntil = now + JeanGreyConfig.PHOENIX_TICKS.get();
        HeroPowers.syncStats(player);
        player.clearFire();
        ServerLevel level = player.serverLevel();
        if (!HeroFx.vfx(level, player.position(), "ignition_burst", PHOENIX_GOLD, 3.0F)) {
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 80, 1.0D, 1.2D, 1.0D, 0.1D);
        }
        HeroFx.vfx(level, player.position(), "pyre_pillar", PHOENIX_GOLD, 2.0F);
        HeroFx.cue(player, "thematic:ignite", 1.4F, 0.7F);
        HeroFx.play(player, SoundEvents.ENDER_DRAGON_GROWL, 0.6F, 1.6F);
        HeroFx.shake(level, player.position(), 16.0D, 0.5F, 15);
        HeroAnimations.play(player, "celestisynth:cs_solaris_spin");
        player.displayClientMessage(Component.literal("§6The Phoenix burns."), true);
    }

    private static void extinguish(ServerPlayer player, Phoenix state) {
        state.burnUntil = 0L;
        state.burnStart = 0L;
        state.lastCombat = player.level().getGameTime();
        HeroPowers.syncStats(player);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        SerumKit.meter(player, -1.0F);
        HeroFx.play(player, SoundEvents.FIRE_EXTINGUISH, 0.8F, 0.8F);
    }

    private static void supernova(ServerPlayer player, Phoenix state) {
        ServerLevel level = player.serverLevel();
        double radius = JeanGreyConfig.SUPERNOVA_RADIUS.get();
        float damage = JeanGreyConfig.SUPERNOVA_DAMAGE.get().floatValue();
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            if (!KineticBurst.affects(player, target)) {
                continue;
            }
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
            target.igniteForSeconds(8.0F);
            Vec3 push = target.position().subtract(player.position()).normalize().scale(1.6D);
            target.setDeltaMovement(push.x, 0.6D, push.z);
            target.hurtMarked = true;
        }
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        boolean shown = HeroFx.vfx(level, player.position(), "supernova_bubble", PHOENIX_GOLD, (float) radius);
        shown |= HeroFx.vfx(level, center, "solar_explosion", PHOENIX_GOLD, (float) radius);
        if (!shown) {
            for (int i = 0; i < 60; i++) {
                double angle = Math.PI * 2.0D * i / 60.0D;
                level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 0, Math.cos(angle), 0.05D, Math.sin(angle), 0.8D);
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        HeroFx.play(player, SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 0.6F);
        HeroFx.play(player, SoundEvents.ENDER_DRAGON_GROWL, 0.8F, 1.2F);
        HeroFx.shake(level, center, radius * 2.5D, 1.0F, 25);
        HeroAnimations.play(player, "web_shoot_both");
        extinguish(player, state);
        player.displayClientMessage(Component.literal("§6Supernova!"), true);
    }

    private static boolean rebirth(ServerPlayer player) {
        Phoenix state = PHOENIXES.get(player.getUUID());
        if (!JeanGreyConfig.REBIRTH.get() || state == null || state.burnUntil > 0L || state.meter < 100.0F
                || !CheatDeath.ready(player) || VillainWorld.suppressed(player)) {
            return false;
        }
        CheatDeath.use(player);
        player.removeAllEffects();
        ignite(player, state);
        player.setHealth(player.getMaxHealth());
        ServerLevel level = player.serverLevel();
        HeroFx.vfx(level, player.position(), "supernova_bubble", PHOENIX_GOLD, 4.0F);
        level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0D, player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        player.displayClientMessage(Component.literal("§6You rise from the ashes."), true);
        return true;
    }

    public static RogueMemory rogueMemory() {
        if (rogue == null) {
            rogue = RogueKit.memory("jean_grey", "Jean Grey")
                    .active("Telekinetic Lift", JeanGreyPower::rogueLift)
                    .passive(new RogueMemory.Passive() {
                        @Override
                        public void end(ServerPlayer player) {
                            TelekineticFormation.releaseOwner(player.getUUID());
                        }
                    })
                    .build();
        }
        return rogue;
    }

    private static double rogueLift(ServerPlayer player) {
        LivingEntity target = aim(player, JeanGreyConfig.ROGUE_RANGE.get());
        if (target == null || target instanceof Player || !RogueKit.affects(player, target) || HeroAbilities.isBoss(target)) {
            player.displayClientMessage(Component.literal("§7Look at a creature to lift it."), true);
            return 0.0D;
        }
        float damage = RogueKit.dmg(MarvelBalanceConfig.GRIP_THROW_DAMAGE.get() * JeanGreyConfig.ROGUE_DAMAGE_MULTIPLIER.get());
        TelekineticFormation.Result result = TelekineticFormation.hold(player, target, JeanGreyConfig.ROGUE_HOLD_TICKS.get(), false, damage, 1, style(player));
        if (result != TelekineticFormation.Result.HELD) {
            player.displayClientMessage(Component.literal("§7It won't budge."), true);
            return 0.0D;
        }
        HeroFx.play(target, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.2F);
        HeroAnimations.play(player, "web_shoot_r");
        return JeanGreyConfig.ROGUE_COOLDOWN.get();
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TelekineticFormation.releaseOwner(player.getUUID());
            PHOENIXES.remove(player.getUUID());
        }
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TelekineticFormation.releaseOwner(player.getUUID());
        }
    }
}
