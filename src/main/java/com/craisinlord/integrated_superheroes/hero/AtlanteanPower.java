package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.AlexsCavesDeepOnes;
import com.craisinlord.integrated_superheroes.compat.AntarchySeaLife;
import com.craisinlord.integrated_superheroes.compat.IceAndFireCharges;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class AtlanteanPower {
    public static final String ID = "atlantean_arthur_curry";
    private static final String TRIDENT_TAG = "integrated_superheroes_atlantean_trident";
    private static final String SEA_LIFE_TAG = "integrated_superheroes_sea_life";
    private static final ResourceLocation SEA_LIFE_DAMAGE = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "sea_life_damage");
    private static final TagKey<EntityType<?>> SEA_KIN = TagKey.create(Registries.ENTITY_TYPE, IntegratedSuperheroes.id("sea_kin"));
    private static final DustParticleOptions INK = new DustParticleOptions(new Vector3f(0.02F, 0.02F, 0.02F), 1.2F);
    private static final int WATER_COLOR = 0x3AA8FF;
    private static final String CALL = "call";
    private static final String MISSILE = "missile";
    private static final String COMMAND = "command";
    private static final int COMMAND_CAP = 3;
    private static final Map<UUID, Integer> DRY = new HashMap<>();
    private static final Map<UUID, Ally> ALLIES = new HashMap<>();
    private static final Map<UUID, Flight> TRIDENTS = new HashMap<>();
    private static final Map<UUID, Long> PROVOKED = new HashMap<>();
    private static final Map<UUID, Bomb> BOMBS = new HashMap<>();
    private static final Map<UUID, Long> SURGED = new HashMap<>();

    private record Flight(UUID owner, long returnAt) {
    }

    private record Ally(UUID owner, long until, String group, float bite, long since) {
    }

    private static final class Bomb {
        private final UUID octopus;
        private Vec3 velocity;
        private UUID target;
        private int age;
        private int held;

        private Bomb(UUID octopus, Vec3 velocity) {
            this.octopus = octopus;
            this.velocity = velocity;
        }
    }

    private static boolean registered;

    private AtlanteanPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(AtlanteanPower::onJoin);
            NeoForge.EVENT_BUS.addListener(AtlanteanPower::onTarget);
            NeoForge.EVENT_BUS.addListener(AtlanteanPower::onDamage);
            if (ModCompat.ICE_AND_FIRE) {
                NeoForge.EVENT_BUS.addListener(AtlanteanPower::onEffect);
            }
        }
        List<HeroPower.Ability> abilities = new ArrayList<>();
        if (ModCompat.ANTARCHY) {
            abilities.add(new HeroPower.Ability("missile_squids", "Missile Squids", AtlanteanPower::missileSquids));
            abilities.add(new HeroPower.Ability("octopus_bomb", "Octopus Bomb", AtlanteanPower::octopusBomb));
        }
        abilities.add(new HeroPower.Ability("call_sea_life", "Call of the Sea", AtlanteanPower::callSeaLife));
        abilities.add(new HeroPower.Ability("speak_to_the_sea", "Speak to the Sea", AtlanteanPower::speak));
        abilities.add(new HeroPower.Ability("trident_throw", "Trident Throw", AtlanteanPower::tridentThrow));
        return new HeroPower(
                ID,
                "Atlantean (Arthur Curry)",
                syringe,
                NewSerumConfig.ATLANTEAN,
                List.of(),
                List.copyOf(abilities),
                List.of(),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (player.tickCount % 20 == 0) {
                            tickWater(player);
                        }
                        if (player.tickCount % 10 == 0) {
                            tickAllies(player);
                        }
                        tickBomb(player);
                        tickTridents(player.serverLevel());
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        DRY.remove(player.getUUID());
                        PROVOKED.remove(player.getUUID());
                        SURGED.remove(player.getUUID());
                        SerumKit.clearMeter(player);
                        dismiss(player, null);
                        dropBomb(player);
                        HeroAbilities.clearDash(player);
                        SerumKit.dropEffect(player, MobEffects.WATER_BREATHING);
                        SerumKit.dropEffect(player, MobEffects.DOLPHINS_GRACE);
                        SerumKit.dropEffect(player, MobEffects.CONDUIT_POWER);
                        SerumKit.dropEffect(player, MobEffects.NIGHT_VISION);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        Long surged = SURGED.get(player.getUUID());
                        if (surged != null && event.getSource().is(DamageTypeTags.IS_FALL)
                                && player.level().getGameTime() - surged < 100L) {
                            event.setCanceled(true);
                        }
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (kin(event.getEntity()) && !isAllyOf(player.getUUID(), event.getEntity())) {
                            PROVOKED.put(player.getUUID(), player.level().getGameTime() + 600L);
                        }
                        Entity direct = event.getSource().getDirectEntity();
                        if (direct != null && direct.getTags().contains(TRIDENT_TAG)) {
                            float amount = NewSerumConfig.TRIDENT_DAMAGE.get().floatValue();
                            if (wet(player)) {
                                amount += NewSerumConfig.TRIDENT_BOLT_DAMAGE.get().floatValue();
                                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(player.serverLevel());
                                if (bolt != null) {
                                    bolt.moveTo(event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ());
                                    bolt.setVisualOnly(true);
                                    player.serverLevel().addFreshEntity(bolt);
                                }
                            }
                            event.setAmount(amount);
                        }
                        event.setAmount(event.getAmount() * multiplier(player));
                    }
                }
        );
    }

    public static boolean isAtlantean(Player player) {
        return SerumKit.is(player, ID);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && !event.getLevel().isClientSide() && event.getEntity().getTags().contains(SEA_LIFE_TAG)) {
            event.setCanceled(true);
        }
    }

    private static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null) {
            return;
        }
        Ally ally = ALLIES.get(event.getEntity().getUUID());
        if (ally != null) {
            if (target instanceof Player || isAllyOf(ally.owner(), target)) {
                event.setCanceled(true);
            }
            return;
        }
        if (NewSerumConfig.SEA_RESPECTS_KING.get() && target instanceof ServerPlayer player && isAtlantean(player)
                && kin(event.getEntity()) && !provoked(player)) {
            event.setCanceled(true);
        }
    }

    private static void onDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        Ally ally = attacker == null ? null : ALLIES.get(attacker.getUUID());
        if (ally == null) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player || isAllyOf(ally.owner(), victim)) {
            event.setCanceled(true);
            return;
        }
        if (ally.bite() > 0.0F) {
            event.setAmount(ally.bite());
        }
    }

    private static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player && isAtlantean(player)
                && event.getEffectInstance().getEffect().value() == IceAndFireCharges.sirenCharm().value()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    private static boolean provoked(ServerPlayer player) {
        Long until = PROVOKED.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    private static boolean kin(LivingEntity entity) {
        return entity.getType().is(SEA_KIN);
    }

    private static boolean isAllyOf(UUID owner, Entity entity) {
        Ally ally = entity == null ? null : ALLIES.get(entity.getUUID());
        return ally != null && ally.owner().equals(owner);
    }

    private static boolean wet(ServerPlayer player) {
        return player.isInWaterRainOrBubble();
    }

    private static boolean dried(ServerPlayer player) {
        int limit = NewSerumConfig.DRY_SECONDS.get();
        return limit > 0 && DRY.getOrDefault(player.getUUID(), 0) >= limit;
    }

    private static float multiplier(ServerPlayer player) {
        if (wet(player)) {
            return NewSerumConfig.WATER_DAMAGE_MULTIPLIER.get().floatValue();
        }
        return dried(player) ? NewSerumConfig.DRY_DAMAGE_MULTIPLIER.get().floatValue() : 1.0F;
    }

    private static boolean deep(ServerPlayer player) {
        return player.isUnderWater() && (player.getY() < player.level().getSeaLevel() - 15
                || player.level().getBiome(player.blockPosition()).is(BiomeTags.IS_DEEP_OCEAN));
    }

    private static boolean chasm(ServerPlayer player) {
        return ModCompat.ALEXS_CAVES && AlexsCavesDeepOnes.inChasm(player.serverLevel(), player.blockPosition());
    }

    private static void tickWater(ServerPlayer player) {
        SerumKit.keepEffect(player, MobEffects.WATER_BREATHING, 0);
        boolean inWater = player.isInWater();
        if (inWater) {
            SerumKit.keepEffect(player, MobEffects.DOLPHINS_GRACE, 0);
            SerumKit.keepEffect(player, MobEffects.CONDUIT_POWER, 0);
        } else {
            SerumKit.dropEffect(player, MobEffects.DOLPHINS_GRACE);
            SerumKit.dropEffect(player, MobEffects.CONDUIT_POWER);
        }
        if (player.isUnderWater() && (player.getY() < player.level().getSeaLevel() - 20 || chasm(player))) {
            SerumKit.keepEffect(player, MobEffects.NIGHT_VISION, 0);
        } else {
            SerumKit.dropEffect(player, MobEffects.NIGHT_VISION);
        }
        boolean wasDry = dried(player);
        if (wet(player)) {
            DRY.put(player.getUUID(), 0);
            SerumKit.meter(player, -1.0F);
            if (wasDry) {
                player.displayClientMessage(Component.literal("§bThe water brings you back to full strength."), true);
            }
            if (player.isUnderWater() && player.getY() < player.level().getSeaLevel() - 30) {
                HeroAdvancements.award(player, "justice_league/atlantean/deep_dive");
            }
            return;
        }
        int dry = DRY.merge(player.getUUID(), 1, Integer::sum);
        int limit = NewSerumConfig.DRY_SECONDS.get();
        if (limit > 0) {
            SerumKit.meter(player, 1.0F - Math.min(1.0F, dry / (float) limit));
        }
        if (dried(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false, true));
            if (!wasDry) {
                player.displayClientMessage(Component.literal("§eYou're drying out. Get back in the water."), true);
            }
        }
    }

    private static boolean nearWater(ServerPlayer player, int radius) {
        BlockPos origin = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -2, -radius), origin.offset(radius, 1, radius))) {
            if (player.level().getFluidState(pos).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static void enlist(ServerPlayer player, Mob mob, long until, String group, float bite) {
        if (!COMMAND.equals(group)) {
            mob.addTag(SEA_LIFE_TAG);
            mob.setPersistenceRequired();
        }
        mob.setTarget(null);
        ALLIES.put(mob.getUUID(), new Ally(player.getUUID(), until, group, bite, player.level().getGameTime()));
    }

    private static double missileSquids(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        int count = NewSerumConfig.MISSILE_COUNT.get() + (wet(player) ? NewSerumConfig.MISSILE_WET_BONUS.get() : 0);
        long until = level.getGameTime() + NewSerumConfig.MISSILE_SECONDS.get() * 20L;
        float bite = NewSerumConfig.MISSILE_BITE_DAMAGE.get().floatValue();
        double speed = NewSerumConfig.MISSILE_SPEED.get();
        Vec3 eye = player.getEyePosition();
        int fired = 0;
        for (int i = 0; i < count; i++) {
            float yaw = player.getYRot() + (i - (count - 1) / 2.0F) * 9.0F;
            float pitch = player.getXRot() - 2.0F + level.random.nextFloat() * 4.0F;
            Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
            Vec3 at = eye.add(direction.scale(1.2D)).subtract(0.0D, 0.3D, 0.0D);
            Mob squid = AntarchySeaLife.missileSquid(level, player, at, direction.scale(speed).add(player.getDeltaMovement()));
            if (squid == null) {
                continue;
            }
            enlist(player, squid, until, MISSILE, bite);
            level.addFreshEntity(squid);
            level.sendParticles(player.isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.SPLASH, at.x, at.y, at.z, 8, 0.2D, 0.2D, 0.2D, 0.05D);
            fired++;
        }
        if (fired == 0) {
            return 0.0D;
        }
        HeroFx.play(player, SoundEvents.SQUID_SQUIRT, 1.2F, 0.7F);
        HeroFx.play(player, SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.3F);
        HeroAnimations.play(player, "celestisynth:cs_crescentia_throw");
        return NewSerumConfig.MISSILE_COOLDOWN.get();
    }

    private static double octopusBomb(ServerPlayer player, int slot) {
        if (BOMBS.containsKey(player.getUUID())) {
            return 0.0D;
        }
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 at = player.getEyePosition().add(look.scale(1.5D)).subtract(0.0D, 0.6D, 0.0D);
        Mob octopus = AntarchySeaLife.octopus(level, at, player.getYRot());
        if (octopus == null) {
            return 0.0D;
        }
        octopus.addTag(SEA_LIFE_TAG);
        octopus.setPersistenceRequired();
        level.addFreshEntity(octopus);
        BOMBS.put(player.getUUID(), new Bomb(octopus.getUUID(), look.scale(1.1D)));
        HeroFx.play(player, SoundEvents.TRIDENT_THROW.value(), 1.0F, 0.6F);
        HeroFx.play(player, SoundEvents.SQUID_SQUIRT, 1.0F, 0.5F);
        HeroAnimations.play(player, "celestisynth:cs_crescentia_throw");
        return NewSerumConfig.OCTOPUS_COOLDOWN.get();
    }

    private static void dropBomb(ServerPlayer player) {
        Bomb bomb = BOMBS.remove(player.getUUID());
        if (bomb != null) {
            Entity octopus = player.serverLevel().getEntity(bomb.octopus);
            if (octopus != null) {
                octopus.discard();
            }
        }
    }

    private static boolean bombable(ServerPlayer player, LivingEntity entity) {
        return entity != player && entity.isAlive() && !(entity instanceof Player) && !isAllyOf(player.getUUID(), entity)
                && !entity.getTags().contains(SEA_LIFE_TAG) && !entity.isSpectator();
    }

    private static void tickBomb(ServerPlayer player) {
        Bomb bomb = BOMBS.get(player.getUUID());
        if (bomb == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!(level.getEntity(bomb.octopus) instanceof Mob octopus) || !octopus.isAlive()) {
            BOMBS.remove(player.getUUID());
            return;
        }
        bomb.age++;
        octopus.setDeltaMovement(Vec3.ZERO);
        if (bomb.target == null) {
            Vec3 from = octopus.position();
            Vec3 to = from.add(bomb.velocity);
            LivingEntity found = level.getEntitiesOfClass(LivingEntity.class,
                            octopus.getBoundingBox().expandTowards(bomb.velocity).inflate(0.5D), entity -> bombable(player, entity))
                    .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(from))).orElse(null);
            if (found != null) {
                bomb.target = found.getUUID();
                level.playSound(null, found.getX(), found.getY(), found.getZ(), SoundEvents.SLIME_ATTACK, player.getSoundSource(), 1.2F, 0.6F);
                return;
            }
            double lift = octopus.getBbHeight() * 0.5D;
            HitResult hit = level.clip(new ClipContext(from.add(0.0D, lift, 0.0D), to.add(0.0D, lift, 0.0D),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, octopus));
            if (hit.getType() != HitResult.Type.MISS || bomb.age > 40) {
                detonate(player, octopus);
                return;
            }
            float yaw = (float) (Mth.atan2(bomb.velocity.z, bomb.velocity.x) * Mth.RAD_TO_DEG) - 90.0F;
            octopus.moveTo(to.x, to.y, to.z, yaw, 0.0F);
            bomb.velocity = bomb.velocity.add(0.0D, octopus.isInWater() ? -0.005D : -0.03D, 0.0D);
            level.sendParticles(octopus.isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.SPLASH, to.x, to.y + lift, to.z, 2, 0.2D, 0.2D, 0.2D, 0.0D);
            return;
        }
        if (!(level.getEntity(bomb.target) instanceof LivingEntity target) || !target.isAlive()) {
            detonate(player, octopus);
            return;
        }
        bomb.held++;
        octopus.moveTo(target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(), octopus.getYRot(), 0.0F);
        if (!HeroAbilities.isBoss(target)) {
            target.setDeltaMovement(0.0D, Math.min(0.0D, target.getDeltaMovement().y), 0.0D);
            target.hurtMarked = true;
            if (bomb.held % 10 == 1) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), player);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3), player);
            }
        }
        if (bomb.held % 4 == 0) {
            level.sendParticles(INK, target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(), 14,
                    target.getBbWidth() * 0.8D, target.getBbHeight() * 0.4D, target.getBbWidth() * 0.8D, 0.02D);
        }
        if (bomb.held >= NewSerumConfig.OCTOPUS_HOLD_TICKS.get()) {
            detonate(player, octopus);
        }
    }

    private static void detonate(ServerPlayer player, Mob octopus) {
        BOMBS.remove(player.getUUID());
        ServerLevel level = player.serverLevel();
        Vec3 center = octopus.position().add(0.0D, octopus.getBbHeight() * 0.5D, 0.0D);
        double radius = NewSerumConfig.OCTOPUS_RADIUS.get();
        float damage = NewSerumConfig.OCTOPUS_DAMAGE.get().floatValue();
        HeroAbilities.runAsAbilityDamage(() -> {
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                    entity -> bombable(player, entity))) {
                double distance = victim.getBoundingBox().getCenter().distanceTo(center);
                float falloff = (float) Mth.clamp(1.0D - distance / (radius + 1.0D), 0.25D, 1.0D);
                victim.hurt(player.damageSources().mobAttack(player), damage * falloff);
                SerumKit.push(victim, center, 0.8D * falloff, 0.4D * falloff);
            }
        });
        level.sendParticles(INK, center.x, center.y, center.z, 125, radius * 0.5D, radius * 0.5D, radius * 0.5D, 0.1D);
        level.sendParticles(INK, center.x, center.y, center.z, 75, radius * 0.2D, radius * 0.2D, radius * 0.2D, 0.2D);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 4, 0.5D, 0.5D, 0.5D, 0.0D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, player.getSoundSource(), 2.0F, 0.9F);
        octopus.discard();
    }

    private static double callSeaLife(ServerPlayer player, int slot) {
        if (!player.isInWater() && !nearWater(player, 4)) {
            return landSurge(player);
        }
        dismiss(player, CALL);
        ServerLevel level = player.serverLevel();
        int seconds = NewSerumConfig.SEA_LIFE_SECONDS.get();
        long until = level.getGameTime() + seconds * 20L;
        int count = NewSerumConfig.SEA_LIFE_COUNT.get();
        boolean chasm = chasm(player);
        boolean deep = !chasm && deep(player);
        int called = 0;
        for (int i = 0; i < count; i++) {
            Vec3 at = waterNear(player, i);
            Mob mob;
            if (chasm) {
                mob = AlexsCavesDeepOnes.summon(level, player, at, i == count - 1, seconds * 20);
            } else if (deep) {
                mob = EntityType.GUARDIAN.create(level);
            } else {
                mob = EntityType.DOLPHIN.create(level);
                if (mob != null) {
                    AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                    if (attack != null) {
                        attack.addPermanentModifier(new AttributeModifier(SEA_LIFE_DAMAGE, 9.0D, AttributeModifier.Operation.ADD_VALUE));
                    }
                }
            }
            if (mob == null) {
                continue;
            }
            if (!chasm) {
                mob.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
            }
            enlist(player, mob, until, CALL, 0.0F);
            level.addFreshEntity(mob);
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, at.x, at.y, at.z, 20, 0.4D, 0.4D, 0.4D, 0.1D);
            HeroFx.vfx(level, at, "whirlpool", 0, 1.2F);
            called++;
        }
        if (called == 0) {
            return 0.0D;
        }
        HeroFx.play(player, SoundEvents.CONDUIT_ACTIVATE, 1.0F, 1.2F);
        HeroFx.play(player, chasm ? SoundEvents.ELDER_GUARDIAN_AMBIENT : deep ? SoundEvents.GUARDIAN_AMBIENT : SoundEvents.DOLPHIN_PLAY, 1.0F, 1.0F);
        HeroAdvancements.award(player, "justice_league/atlantean/king_of_the_seas");
        return NewSerumConfig.SEA_LIFE_COOLDOWN.get();
    }

    private static double landSurge(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        float damage = NewSerumConfig.LAND_SURGE_DAMAGE.get().floatValue();
        HeroAbilities.runAsAbilityDamage(() -> HeroShockwaves.ring(player, 10, 5, damage));
        for (LivingEntity target : HeroAbilities.sphere(player, 5.0D)) {
            if (isAllyOf(player.getUUID(), target)) {
                continue;
            }
            target.clearFire();
            SerumKit.push(target, player.position(), 1.2D, 0.4D);
        }
        int tint = 0x80000000 | SerumKit.primary(player, WATER_COLOR) & 0xFFFFFF;
        if (!HeroFx.vfx(level, player.position(), "whirlpool", tint, 4.0F)) {
            SerumKit.ring(level, player.position().add(0.0D, 0.3D, 0.0D), ParticleTypes.SPLASH, 3.0D, 40);
            SerumKit.ring(level, player.position().add(0.0D, 0.3D, 0.0D), ParticleTypes.BUBBLE_POP, 4.5D, 50);
        }
        HeroFx.play(player, SoundEvents.GENERIC_SPLASH, 1.2F, 0.7F);
        HeroFx.play(player, SoundEvents.TRIDENT_RIPTIDE_3.value(), 1.0F, 0.8F);
        HeroAnimations.play(player, "bettercombat:two_handed_slam");
        return NewSerumConfig.SEA_LIFE_COOLDOWN.get() * 0.4D;
    }

    private static Vec3 waterNear(ServerPlayer player, int index) {
        BlockPos origin = player.blockPosition();
        List<BlockPos> water = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -2, -4), origin.offset(4, 1, 4))) {
            if (player.level().getFluidState(pos).is(FluidTags.WATER)) {
                water.add(pos.immutable());
            }
        }
        if (water.isEmpty()) {
            return player.position();
        }
        water.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
        BlockPos pick = water.get(Math.min(water.size() - 1, index * 3));
        return Vec3.atCenterOf(pick);
    }

    private static boolean listens(Mob mob) {
        return (kin(mob) || mob instanceof WaterAnimal) && HeroAbilities.canControl(mob) && !HeroAbilities.isBoss(mob);
    }

    private static double speak(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), NewSerumConfig.COMMAND_RANGE.get(), 1);
        LivingEntity target = beam.targets().isEmpty() ? null : beam.targets().get(0);
        if (!(target instanceof Mob mob) || ALLIES.containsKey(mob.getUUID()) || !listens(mob)) {
            player.displayClientMessage(Component.literal("§7Nothing out there is listening."), true);
            return 0.0D;
        }
        List<Map.Entry<UUID, Ally>> commanded = ALLIES.entrySet().stream()
                .filter(entry -> entry.getValue().owner().equals(player.getUUID()) && COMMAND.equals(entry.getValue().group()))
                .sorted(Comparator.comparingLong(entry -> entry.getValue().since()))
                .toList();
        for (int i = 0; i <= commanded.size() - COMMAND_CAP; i++) {
            UUID id = commanded.get(i).getKey();
            release(level, id, ALLIES.remove(id));
        }
        enlist(player, mob, level.getGameTime() + NewSerumConfig.COMMAND_SECONDS.get() * 20L, COMMAND, 0.0F);
        Vec3 at = mob.position().add(0.0D, mob.getBbHeight() * 0.5D, 0.0D);
        HeroAbilities.particleLine(level, player.getEyePosition(), at, ParticleTypes.BUBBLE, 0.6D);
        level.sendParticles(ParticleTypes.NAUTILUS, at.x, at.y, at.z, 30, mob.getBbWidth() * 0.6D, mob.getBbHeight() * 0.5D, mob.getBbWidth() * 0.6D, 0.4D);
        HeroFx.play(player, SoundEvents.CONDUIT_ATTACK_TARGET, 1.0F, 1.0F);
        player.displayClientMessage(Component.literal("§b").append(mob.getDisplayName()).append(Component.literal("§b answers your call.")), true);
        HeroAdvancements.award(player, "justice_league/atlantean/king_of_the_seas");
        return NewSerumConfig.COMMAND_COOLDOWN.get();
    }

    private static LivingEntity focus(ServerPlayer player) {
        LivingEntity hurt = player.getLastHurtMob();
        if (usable(player, hurt) && player.tickCount - player.getLastHurtMobTimestamp() < 200) {
            return hurt;
        }
        LivingEntity hurtBy = player.getLastHurtByMob();
        if (usable(player, hurtBy) && player.tickCount - player.getLastHurtByMobTimestamp() < 200) {
            return hurtBy;
        }
        return null;
    }

    private static boolean usable(ServerPlayer player, LivingEntity entity) {
        return entity != null && entity.isAlive() && !(entity instanceof Player) && !isAllyOf(player.getUUID(), entity)
                && entity.level() == player.level() && entity.distanceToSqr(player) < 32.0D * 32.0D;
    }

    private static void tickAllies(ServerPlayer player) {
        if (ALLIES.isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        LivingEntity focus = focus(player);
        List<LivingEntity> enemies = null;
        Iterator<Map.Entry<UUID, Ally>> iterator = ALLIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Ally> entry = iterator.next();
            Ally ally = entry.getValue();
            if (!ally.owner().equals(player.getUUID())) {
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                iterator.remove();
                continue;
            }
            if (now > ally.until()) {
                iterator.remove();
                release(level, entry.getKey(), ally);
                continue;
            }
            if (mob instanceof Dolphin dolphin) {
                dolphin.setAirSupply(dolphin.getMaxAirSupply());
                dolphin.setMoisntessLevel(2400);
            }
            LivingEntity current = mob.getTarget();
            boolean stale = current == null || !current.isAlive() || current instanceof Player || isAllyOf(player.getUUID(), current);
            if (focus != null && focus != mob && focus != current) {
                mob.setTarget(focus);
            } else if (stale) {
                if (enemies == null) {
                    enemies = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16.0D),
                            candidate -> candidate instanceof Enemy && candidate.isAlive() && !isAllyOf(player.getUUID(), candidate)
                                    && !(kin(candidate) && (!(candidate instanceof Mob other) || other.getTarget() != player)));
                }
                LivingEntity next = enemies.stream().min(Comparator.comparingDouble(enemy -> enemy.distanceToSqr(mob))).orElse(null);
                mob.setTarget(next);
            }
            if (mob.getTarget() == null && !MISSILE.equals(ally.group())) {
                double distance = mob.distanceToSqr(player);
                if (distance > 48.0D * 48.0D && CALL.equals(ally.group())) {
                    mob.teleportTo(player.getX(), player.getY(), player.getZ());
                } else if (distance > 64.0D) {
                    mob.getNavigation().moveTo(player, 1.2D);
                }
            }
        }
    }

    private static void release(ServerLevel level, UUID id, Ally ally) {
        Entity entity = level.getEntity(id);
        if (entity == null || ally == null) {
            return;
        }
        if (COMMAND.equals(ally.group())) {
            if (entity instanceof Mob mob) {
                mob.setTarget(null);
            }
            level.sendParticles(ParticleTypes.BUBBLE_POP, entity.getX(), entity.getY() + 0.5D, entity.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.05D);
            return;
        }
        level.sendParticles(ParticleTypes.BUBBLE_POP, entity.getX(), entity.getY(), entity.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.05D);
        entity.discard();
    }

    private static void dismiss(ServerPlayer player, String group) {
        ServerLevel level = player.serverLevel();
        Iterator<Map.Entry<UUID, Ally>> iterator = ALLIES.entrySet().iterator();
        List<Map.Entry<UUID, Ally>> gone = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Ally> entry = iterator.next();
            Ally ally = entry.getValue();
            if (ally.owner().equals(player.getUUID()) && (group == null || group.equals(ally.group()))) {
                gone.add(entry);
                iterator.remove();
            }
        }
        for (Map.Entry<UUID, Ally> entry : gone) {
            release(level, entry.getKey(), entry.getValue());
        }
    }

    private static double tridentThrow(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        ItemStack stack = new ItemStack(Items.TRIDENT);
        stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOYALTY), 3);
        ThrownTrident trident = new ThrownTrident(level, player, stack);
        trident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, NewSerumConfig.TRIDENT_SPEED.get().floatValue(), 0.5F);
        trident.pickup = AbstractArrow.Pickup.DISALLOWED;
        trident.addTag(TRIDENT_TAG);
        level.addFreshEntity(trident);
        TRIDENTS.put(trident.getUUID(), new Flight(player.getUUID(), level.getGameTime() + NewSerumConfig.TRIDENT_LIFE_TICKS.get()));
        HeroFx.play(player, SoundEvents.TRIDENT_THROW.value(), 1.0F, 0.8F);
        HeroAnimations.play(player, "celestisynth:cs_crescentia_throw");
        return NewSerumConfig.TRIDENT_COOLDOWN.get();
    }

    private static void tickTridents(ServerLevel level) {
        if (TRIDENTS.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        TRIDENTS.entrySet().removeIf(entry -> {
            Flight flight = entry.getValue();
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null) {
                return now > flight.returnAt() + 200L;
            }
            if (!(entity instanceof ThrownTrident trident)) {
                return true;
            }
            Entity owner = level.getEntity(flight.owner());
            if (owner == null || now > flight.returnAt() + 200L) {
                entity.discard();
                return true;
            }
            if (now > flight.returnAt() && !trident.isNoPhysics()) {
                trident.setNoPhysics(true);
            }
            if (now > flight.returnAt() - NewSerumConfig.TRIDENT_LIFE_TICKS.get() + 5L && trident.isNoPhysics()
                    && entity.distanceToSqr(owner.getEyePosition()) < 4.0D) {
                entity.discard();
                return true;
            }
            return false;
        });
    }

    static void airSurge(ServerPlayer player) {
        if (!isAtlantean(player) || player.onGround() || player.isInWater() || player.isPassenger()
                || player.getAbilities().flying || player.isSpectator()) {
            return;
        }
        long now = player.level().getGameTime();
        Long last = SURGED.get(player.getUUID());
        if (last != null && now - last < NewSerumConfig.SURGE_COOLDOWN.get() * 20.0D) {
            return;
        }
        SURGED.put(player.getUUID(), now);
        double speed = NewSerumConfig.SURGE_SPEED.get() * (wet(player) ? 1.25D : 1.0D);
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, Math.max(look.y, 0.25D), look.z).normalize();
        HeroAbilities.startDash(player, direction.scale(speed), 10, NewSerumConfig.SURGE_DAMAGE.get(), 2.0D,
                entity -> !(entity instanceof Player) && !isAllyOf(player.getUUID(), entity));
        player.resetFallDistance();
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 0.5D, player.getZ(), 40, 0.5D, 0.4D, 0.5D, 0.2D);
        level.sendParticles(ParticleTypes.BUBBLE_POP, player.getX(), player.getY() + 0.5D, player.getZ(), 20, 0.4D, 0.4D, 0.4D, 0.05D);
        HeroFx.vfx(level, player.position().add(0.0D, 0.5D, 0.0D), "air_burst_ring", 0xC0000000 | SerumKit.primary(player, WATER_COLOR) & 0xFFFFFF);
        HeroFx.play(player, SoundEvents.TRIDENT_RIPTIDE_2.value(), 1.0F, 1.0F);
    }
}
