package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.tintankgames.marvel.world.level.MarvelGameRules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class SonicPower {
    public static final String ID = "sonic_dinah_lance";
    private static final String CRY = "canary_cry";
    private static final int CRY_COLOR = 0xFFE9A8;
    private static final int SCREAM_ALPHA = 0x80000000;
    private static final Map<UUID, Scream> SCREAMS = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> LIFTED = ServerState.map(new HashMap<>());

    private record Scream(long start, Set<BlockPos> resonated) {
    }

    private static boolean registered;

    public static void clearStale(ServerPlayer player) {
        if (!SCREAMS.containsKey(player.getUUID())) {
            HeroAbilities.clearActive(player, CRY);
        }
    }

    private SonicPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            HeroCaster.onTick(SonicPower::tickMob);
            HeroCaster.onCleanup(caster -> {
                SCREAMS.remove(caster.getUUID());
                LIFTED.remove(caster.getUUID());
            });
        }
        return new HeroPower(
                ID,
                "Sonic Scream (Dinah Lance)",
                syringe,
                NewSerumConfig.SONIC,
                List.of("agility"),
                List.of(
                        HeroPower.Ability.of(CRY, "Canary Cry", SonicPower::canaryCry),
                        HeroPower.Ability.of("sonic_burst", "Sonic Burst", SonicPower::sonicBurst),
                        HeroPower.Ability.of("sonic_lift", "Sonic Lift", SonicPower::sonicLift)
                ),
                List.of(),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        tickScream(player);
                        Long lifted = LIFTED.get(player.getUUID());
                        if (lifted != null && player.level().getGameTime() - lifted > 5L && (player.onGround() || player.isInWater())) {
                            LIFTED.remove(player.getUUID());
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroAbilities.clearDash(player);
                        SCREAMS.remove(player.getUUID());
                        LIFTED.remove(player.getUUID());
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypes.SONIC_BOOM)) {
                            event.setCanceled(true);
                            return;
                        }
                        if (event.getSource().is(DamageTypeTags.IS_FALL) && LIFTED.containsKey(player.getUUID())) {
                            event.setCanceled(true);
                            LIFTED.remove(player.getUUID());
                        }
                    }
                }
        );
    }

    public static boolean isSonic(Player player) {
        return SerumKit.is(player, ID);
    }

    private static void tickMob(LivingEntity caster) {
        tickScream(caster);
        Long lifted = LIFTED.get(caster.getUUID());
        if (lifted != null) {
            caster.fallDistance = 0.0F;
            if (caster.level().getGameTime() - lifted > 5L && (caster.onGround() || caster.isInWater())) {
                LIFTED.remove(caster.getUUID());
            }
        }
    }

    private static net.minecraft.world.damagesource.DamageSource attack(LivingEntity caster) {
        return caster instanceof Player player ? caster.damageSources().playerAttack(player) : caster.damageSources().mobAttack(caster);
    }

    private static void deafen(LivingEntity target, int ticks) {
        if (!HeroAbilities.canControl(target)) {
            return;
        }
        HeroStun.stun(target, ticks);
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks + 60, 0));
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    private static double canaryCry(LivingEntity player, int slot) {
        Scream scream = SCREAMS.remove(player.getUUID());
        if (scream != null) {
            if (player instanceof ServerPlayer serverPlayer) {
                HeroAbilities.setActive(serverPlayer, CRY, false);
            }
            long held = player.level().getGameTime() - scream.start();
            double share = Math.min(1.0D, held / (double) NewSerumConfig.CANARY_CRY_MAX_TICKS.get());
            return Math.max(1.5D, NewSerumConfig.CANARY_CRY_COOLDOWN.get() * share);
        }
        SCREAMS.put(player.getUUID(), new Scream(player.level().getGameTime(), new HashSet<>()));
        if (player instanceof ServerPlayer serverPlayer) {
            HeroAbilities.setActive(serverPlayer, CRY, true);
        }
        if (HeroFx.thematic()) {
            HeroFx.cue(player, "thematic:canary_cry", 1.2F, 1.0F);
        } else {
            HeroFx.play(player, SoundEvents.WARDEN_SONIC_BOOM, 1.2F, 1.7F);
            HeroFx.play(player, SoundEvents.GOAT_SCREAMING_AMBIENT, 0.6F, 1.6F);
        }
        return player instanceof ServerPlayer ? 0.25D : NewSerumConfig.CANARY_CRY_COOLDOWN.get();
    }

    private static void tickScream(LivingEntity player) {
        Scream scream = SCREAMS.get(player.getUUID());
        if (scream == null) {
            return;
        }
        ServerLevel level = HeroCaster.level(player);
        long elapsed = level.getGameTime() - scream.start();
        int max = NewSerumConfig.CANARY_CRY_MAX_TICKS.get();
        if (elapsed >= max || !player.isAlive()) {
            SCREAMS.remove(player.getUUID());
            if (player instanceof ServerPlayer serverPlayer) {
                HeroAbilities.setActive(serverPlayer, CRY, false);
                HeroAbilities.setCooldown(serverPlayer, CRY, NewSerumConfig.CANARY_CRY_COOLDOWN.get());
            }
            return;
        }
        if (elapsed % 5L != 0L) {
            return;
        }
        double t = elapsed / (double) max;
        double range = lerp(NewSerumConfig.CANARY_CRY_RANGE.get(), NewSerumConfig.CANARY_CRY_MAX_RANGE.get(), t);
        double angle = lerp(NewSerumConfig.CANARY_CRY_ANGLE.get(), NewSerumConfig.CANARY_CRY_MAX_ANGLE.get(), t);
        float damage = NewSerumConfig.CANARY_CRY_DAMAGE.get().floatValue();
        double knockback = NewSerumConfig.CANARY_CRY_KNOCKBACK.get() * (1.0D + t);
        Vec3 look = player.getLookAngle();
        for (LivingEntity target : HeroAbilities.cone(player, range, angle)) {
            HeroAbilities.runAsAbilityDamage(() -> HeroHits.pulse(player, target, attack(player), damage));
            target.setDeltaMovement(target.getDeltaMovement().add(look.x * knockback, 0.1D + Math.max(0.0D, look.y) * knockback * 0.5D, look.z * knockback));
            target.hurtMarked = true;
            deafen(target, Math.max(10, NewSerumConfig.CANARY_CRY_STUN_TICKS.get() / 4));
        }
        Vec3 mouth = player.getEyePosition().subtract(0.0D, 0.15D, 0.0D);
        boolean thematic = false;
        if (HeroFx.thematic()) {
            int tint = SCREAM_ALPHA | SerumKit.primary(player, CRY_COLOR) & 0xFFFFFF;
            for (double d = 2.0D; d < range; d += 4.0D) {
                thematic |= HeroFx.vfx(level, mouth.add(look.scale(d)), "sonic_scream", tint, (float) Math.min(6.0D, 0.8D + d * Math.tan(Math.toRadians(angle)) * 0.6D));
            }
        }
        if (!thematic) {
            for (double d = 1.5D; d < range; d += 3.0D) {
                Vec3 at = mouth.add(look.scale(d));
                level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            for (int i = -2; i <= 2; i++) {
                Vec3 direction = Vec3.directionFromRotation(player.getXRot(), player.getYRot() + (float) (i * angle / 3.0D));
                HeroAbilities.particleLine(level, mouth.add(direction), mouth.add(direction.scale(range)), SerumKit.glow(player, CRY_COLOR, 0.7F), 1.6D);
            }
            if (elapsed % 15L == 0L) {
                HeroFx.play(player, SoundEvents.GOAT_SCREAMING_AMBIENT, 0.5F, 1.8F);
            }
        }
        int shattered = walk(player, level, mouth, range, angle, scream.resonated());
        if (shattered > 0 && player instanceof ServerPlayer serverPlayer) {
            HeroAdvancements.count(serverPlayer, "justice_league/sonic/glass_shatterer", shattered, 50.0D);
        }
        HeroFx.shake(level, player.position(), range, 0.2F, 5);
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private static boolean sculk(BlockState state) {
        return state.is(Blocks.SCULK_SENSOR) || state.is(Blocks.CALIBRATED_SCULK_SENSOR) || state.is(Blocks.SCULK_SHRIEKER);
    }

    private static int walk(LivingEntity player, ServerLevel level, Vec3 from, double range, double angle, Set<BlockPos> resonated) {
        boolean shatter = NewSerumConfig.CANARY_CRY_SHATTERS_GLASS.get() && player instanceof Player builder && builder.mayBuild()
                && level.getGameRules().getBoolean(MarvelGameRules.RULE_SUPERPOWERGRIEFING);
        Set<BlockPos> seen = new HashSet<>();
        int broken = 0;
        for (int yaw = -2; yaw <= 2; yaw++) {
            for (int pitch = -1; pitch <= 1; pitch++) {
                Vec3 direction = Vec3.directionFromRotation(player.getXRot() + (float) (pitch * angle / 3.0D),
                        player.getYRot() + (float) (yaw * angle / 2.5D));
                for (double d = 1.0D; d < range; d += 0.5D) {
                    BlockPos pos = BlockPos.containing(from.add(direction.scale(d)));
                    if (!seen.add(pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (sculk(state)) {
                        if (resonated.add(pos.immutable())) {
                            resonate(player, level, pos);
                        }
                        break;
                    }
                    if (!state.is(Tags.Blocks.GLASS_BLOCKS) && !state.is(Tags.Blocks.GLASS_PANES)) {
                        if (state.blocksMotion()) {
                            break;
                        }
                        continue;
                    }
                    if (shatter && HeroGrief.tryBreak(player, level, pos, false)) {
                        broken++;
                    }
                }
            }
        }
        return broken;
    }

    private static void resonate(LivingEntity player, ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        double radius = NewSerumConfig.RESONANCE_RADIUS.get();
        float damage = NewSerumConfig.RESONANCE_DAMAGE.get().floatValue();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                entity -> entity != player && entity.isAlive() && entity.distanceToSqr(center) <= radius * radius
                        && (HeroCaster.isPlayer(player) || HeroCaster.foe(player, entity)))) {
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().sonicBoom(player), damage));
            SerumKit.push(target, center, 1.0D, 0.4D);
            deafen(target, 20);
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 0.5D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 0.5D, center.z, 8, 0.4D, 0.4D, 0.4D, 0.02D);
        level.playSound(null, pos, SoundEvents.SCULK_SHRIEKER_SHRIEK, player.getSoundSource(), 1.2F, 1.4F);
    }

    private static double sonicBurst(LivingEntity player, int slot) {
        ServerLevel level = HeroCaster.level(player);
        double radius = NewSerumConfig.SONIC_BURST_RADIUS.get();
        float damage = NewSerumConfig.SONIC_BURST_DAMAGE.get().floatValue();
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(attack(player), damage));
            SerumKit.push(target, player.position(), 1.4D, 0.5D);
            deafen(target, NewSerumConfig.CANARY_CRY_STUN_TICKS.get() / 2);
        }
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        int tint = SCREAM_ALPHA | SerumKit.primary(player, CRY_COLOR) & 0xFFFFFF;
        if (HeroFx.vfx(level, center, "sonic_scream", tint, (float) radius)) {
            HeroFx.vfx(level, center, "air_burst_ring", 0, (float) radius);
            HeroFx.cue(player, "thematic:canary_cry", 1.2F, 0.8F);
        } else {
            for (int i = 1; i <= 3; i++) {
                SerumKit.ring(level, center, SerumKit.glow(player, CRY_COLOR, 0.9F), radius * i / 3.0D, 14 * i);
            }
            level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            HeroFx.play(player, SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 1.3F);
        }
        HeroFx.shake(level, player.position(), radius * 2.0D, 0.4F, 6);
        return NewSerumConfig.SONIC_BURST_COOLDOWN.get();
    }

    private static double sonicLift(LivingEntity player, int slot) {
        ServerLevel level = HeroCaster.level(player);
        Vec3 look = player.getLookAngle();
        double power = NewSerumConfig.SONIC_LIFT_POWER.get();
        player.setDeltaMovement(look.x * 0.5D, power, look.z * 0.5D);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        LIFTED.put(player.getUUID(), level.getGameTime());
        Vec3 feet = player.position();
        float damage = NewSerumConfig.SONIC_LIFT_DAMAGE.get().floatValue();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(feet, feet).inflate(3.0D, 2.0D, 3.0D),
                entity -> entity != player && entity.isAlive() && (HeroCaster.isPlayer(player) || HeroCaster.foe(player, entity)))) {
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(attack(player), damage));
            SerumKit.push(target, feet, 1.0D, 0.2D);
        }
        int tint = SCREAM_ALPHA | SerumKit.primary(player, CRY_COLOR) & 0xFFFFFF;
        if (HeroFx.vfx(level, feet, "air_burst_ring", tint, 3.0F)) {
            HeroFx.vfx(level, feet.add(0.0D, 0.3D, 0.0D), "sonic_scream", tint, 2.5F);
            HeroFx.cue(player, "thematic:canary_cry", 0.8F, 1.3F);
        } else {
            SerumKit.ring(level, feet.add(0.0D, 0.2D, 0.0D), SerumKit.glow(player, CRY_COLOR, 0.9F), 2.5D, 24);
            level.sendParticles(ParticleTypes.SONIC_BOOM, feet.x, feet.y + 0.3D, feet.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            HeroFx.play(player, SoundEvents.WARDEN_SONIC_BOOM, 0.7F, 1.9F);
        }
        HeroAnimations.play(player, "celestisynth:cs_breezebreaker_jump");
        return NewSerumConfig.SONIC_LIFT_COOLDOWN.get();
    }
}
