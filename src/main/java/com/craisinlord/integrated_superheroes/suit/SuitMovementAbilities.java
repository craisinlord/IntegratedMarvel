package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.HeroAnimations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class SuitMovementAbilities {
    private SuitMovementAbilities() {
    }

    static SuitAbility.Result afterburner(SuitContext context) {
        ServerPlayer player = context.player();
        Vec3 velocity = player.getLookAngle().scale(SuitAbilityConfig.AFTERBURNER_SPEED.get());
        if (player.onGround()) {
            velocity = velocity.add(0.0D, 0.35D, 0.0D);
        }
        HeroAbilities.startDash(player, velocity, SuitAbilityConfig.AFTERBURNER_TICKS.get(),
                context.damage(SuitAbilityConfig.AFTERBURNER), 1.2D, entity -> SuitTargeting.canHit(player, entity));
        SuitStates.noFall(player);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.2D, player.getZ(), 16, 0.2D, 0.1D, 0.2D, 0.05D);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2D, player.getZ(), 8, 0.2D, 0.1D, 0.2D, 0.02D);
        SuitFx.sound(level, player.position(), SuitFx.JETPACK_FIRE, SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result superheroLanding(SuitContext context) {
        ServerPlayer player = context.player();
        Vec3 motion = player.getDeltaMovement();
        if (player.onGround()) {
            player.setDeltaMovement(motion.x, 0.9D, motion.z);
        } else {
            player.setDeltaMovement(motion.x * 0.3D, -2.5D, motion.z * 0.3D);
        }
        player.hurtMarked = true;
        SuitStates.State state = SuitStates.get(player);
        state.landingArmed = true;
        state.landingTicks = 0;
        state.landingPeakY = player.getY();
        state.landingDamage = context.damage(SuitAbilityConfig.LANDING);
        SuitStates.noFall(player);
        player.serverLevel().sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.5D, player.getZ(), 10, 0.2D, 0.3D, 0.2D, 0.02D);
        return SuitAbility.Result.FIRED;
    }

    static void landingImpact(ServerPlayer player, double fallen, float baseDamage) {
        ServerLevel level = player.serverLevel();
        double multiplier = Math.min(SuitAbilityConfig.LANDING_MAX_MULTIPLIER.get(), 1.0D + SuitAbilityConfig.LANDING_PER_BLOCK.get() * Math.max(0.0D, fallen));
        float damage = (float) (baseDamage * multiplier);
        double radius = SuitAbilityConfig.LANDING_RADIUS.get();
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            if (!SuitTargeting.canHit(player, target)) {
                continue;
            }
            target.invulnerableTime = 0;
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
            Vec3 away = target.position().subtract(player.position());
            target.knockback(1.2D, -away.x, -away.z);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.4D, 0.0D));
        }
        BlockPos below = player.blockPosition().below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), player.getX(), player.getY() + 0.1D, player.getZ(),
                    80, radius * 0.4D, 0.1D, radius * 0.4D, 0.2D);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2D, player.getZ(), 3, 1.0D, 0.1D, 1.0D, 0.0D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 0.5F);
        HeroAnimations.play(player, "spiderman_land_1");
    }

    static SuitAbility.Result starboost(SuitContext context) {
        ServerPlayer player = context.player();
        SuitStates.State state = SuitStates.get(player);
        state.starboostTotal = SuitAbilityConfig.STARBOOST_TICKS.get();
        state.starboostTicks = state.starboostTotal;
        SuitStates.noFall(player);
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, 1.0D, motion.z);
        player.hurtMarked = true;
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 30, 0.5D, 0.1D, 0.5D, 0.05D);
        SuitFx.sound(level, player.position(), SuitFx.JETPACK_FIRE, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.2F, 0.8F);
        return SuitAbility.Result.FIRED;
    }
}
