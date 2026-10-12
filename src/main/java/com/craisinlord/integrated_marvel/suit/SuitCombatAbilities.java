package com.craisinlord.integrated_marvel.suit;

import com.craisinlord.integrated_marvel.compat.FallbackGrenade;
import com.craisinlord.integrated_marvel.compat.ModCompat;
import com.craisinlord.integrated_marvel.compat.ScorchedGunsWeapons;
import com.craisinlord.integrated_marvel.hero.HeroAbilities;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tintankgames.marvel.sounds.MarvelSoundEvents;
import net.tintankgames.marvel.world.level.MarvelGameRules;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Comparator;

public final class SuitCombatAbilities {
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));
    private static final int RING_COLOR = 0xFF4040;

    private SuitCombatAbilities() {
    }

    static boolean isBoss(Entity entity) {
        return entity.getType().is(BOSSES);
    }

    static void loseTrackOf(ServerPlayer player, double range) {
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(range), mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
        }
    }

    static SuitAbility.Result flares(SuitContext context) {
        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        double radius = SuitAbilityConfig.FLARES_RADIUS.get();
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius), projectile -> {
            Entity owner = projectile.getOwner();
            return owner != player && (owner == null || !player.isAlliedTo(owner));
        })) {
            level.sendParticles(ParticleTypes.FIREWORK, projectile.getX(), projectile.getY(), projectile.getZ(), 6, 0.1D, 0.1D, 0.1D, 0.05D);
            projectile.discard();
        }
        loseTrackOf(player, 16.0D);
        if (SuitAbilityConfig.FLARES_FLASHBANG.get()) {
            Entity flashbang;
            if (ModCompat.SCORCHED_GUNS) {
                Projectile stun = ScorchedGunsWeapons.flashbang(level, player, 25);
                stun.shootFromRotation(player, player.getXRot() - 10.0F, player.getYRot(), 0.0F, 1.4F, 1.0F);
                flashbang = stun;
            } else {
                flashbang = FallbackGrenade.create(level, player, 25).flashbang(8.0F)
                        .throwFrom(player, player.getXRot() - 10.0F, player.getYRot(), 1.4F);
            }
            SuitGrenadeAbilities.markSuitOwned(flashbang);
            level.addFreshEntity(flashbang);
            SuitFx.sound(level, player.position(), SuitFx.GRENADE_PIN, SoundEvents.TRIPWIRE_CLICK_ON, 1.0F, 1.0F);
        }
        level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.0D, player.getZ(), 60, 1.5D, 1.0D, 1.5D, 0.15D);
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 1.5D, 1.0D, 1.5D, 0.05D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.2F, 0.8F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result laserRing(SuitContext context) {
        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        float damage = context.damage(SuitAbilityConfig.LASER_RING);
        double radius = SuitAbilityConfig.LASER_RING_RADIUS.get();
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            if (SuitTargeting.canHit(player, target)) {
                target.invulnerableTime = 0;
                HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
            }
        }
        double y = player.getY() + 1.1D;
        var laser = SuitFx.particle(SuitFx.SMALL_LASER, ParticleTypes.ELECTRIC_SPARK);
        for (double r = 1.0D; r <= radius; r += 1.0D) {
            int points = (int) (r * 12);
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                double x = player.getX() + Math.cos(angle) * r;
                double z = player.getZ() + Math.sin(angle) * r;
                level.sendParticles(HeroAbilities.emissive(RING_COLOR, 1.2F), x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                if (r == Math.floor(radius)) {
                    level.sendParticles(laser, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
        SuitFx.burst(level, SuitFx.PLASMA_RING, ParticleTypes.FLASH, new Vec3(player.getX(), y, player.getZ()), 1, 0.0D, 0.0D);
        SuitFx.sound(level, player.position(), SuitFx.LASER_FIRE, MarvelSoundEvents.IRON_MAN_UNIBEAM_SHOOT.get(), 1.2F, 1.0F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result stealth(SuitContext context) {
        ServerPlayer player = context.player();
        int ticks = SuitAbilityConfig.STEALTH_TICKS.get();
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
        SuitStates.get(player).stealthUntil = player.level().getGameTime() + ticks;
        loseTrackOf(player, 32.0D);
        player.serverLevel().sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.4D, 0.8D, 0.4D, 0.02D);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result overcharge(SuitContext context) {
        ServerPlayer player = context.player();
        SuitStates.State state = SuitStates.get(player);
        if (state.overchargeArmed) {
            player.displayClientMessage(Component.literal("§eUnibeam already overcharged"), true);
            return SuitAbility.Result.FAILED;
        }
        state.overchargeArmed = true;
        player.displayClientMessage(Component.literal("§bUnibeam overcharged: next shot drains the suit"), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MarvelSoundEvents.IRON_MAN_UNIBEAM_CHARGE.get(), SoundSource.PLAYERS, 1.0F, 0.7F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result brace(SuitContext context) {
        ServerPlayer player = context.player();
        int ticks = SuitAbilityConfig.BRACE_TICKS.get();
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 2, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 4, false, false, true));
        SuitStates.get(player).braceUntil = player.level().getGameTime() + ticks;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.8F, 0.6F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result hydraulicPunch(SuitContext context) {
        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 center = player.getEyePosition().add(look.scale(2.5D));
        AABB area = new AABB(center, center).inflate(1.5D);
        float damage = context.damage(SuitAbilityConfig.HYDRAULIC_PUNCH);
        int stun = SuitAbilityConfig.HYDRAULIC_PUNCH_STUN_TICKS.get();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, entity -> SuitTargeting.canHit(player, entity))) {
            target.invulnerableTime = 0;
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
            target.knockback(1.6D, -look.x, -look.z);
            if (stun > 0) {
                target.addEffect(new MobEffectInstance(ACEffectRegistry.STUNNED, stun, 0));
            }
        }
        if (level.getGameRules().getBoolean(MarvelGameRules.RULE_SUPERPOWERGRIEFING)) {
            double maxHardness = SuitAbilityConfig.HYDRAULIC_PUNCH_MAX_HARDNESS.get();
            BlockPos origin = BlockPos.containing(center);
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(1, 1, 1))) {
                BlockState state = level.getBlockState(pos);
                float hardness = state.getDestroySpeed(level, pos);
                if (!state.isAir() && hardness >= 0.0F && hardness <= maxHardness && level.getBlockEntity(pos) == null) {
                    level.destroyBlock(pos, true, player);
                }
            }
        }
        SuitFx.burst(level, SuitFx.SONIC_BLAST, ParticleTypes.EXPLOSION, center, 1, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 2, 0.5D, 0.5D, 0.5D, 0.0D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0F, 0.6F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result grapple(SuitContext context) {
        ServerPlayer player = context.player();
        LivingEntity target = SuitTargeting.crosshair(player, SuitAbilityConfig.GRAPPLE_RANGE.get());
        if (target == null || isBoss(target)) {
            player.displayClientMessage(Component.literal(target == null ? "§cNo target" : "§cToo heavy to grapple"), true);
            return SuitAbility.Result.FAILED;
        }
        Vec3 pull = player.position().subtract(target.position());
        double speed = Math.min(2.5D, pull.length() * 0.18D);
        target.setDeltaMovement(pull.normalize().scale(speed).add(0.0D, 0.35D, 0.0D));
        target.hurtMarked = true;
        Vec3 hand = player.getEyePosition().add(0.0D, -0.4D, 0.0D);
        if (ModCompat.SCORCHED_GUNS) {
            ScorchedGunsWeapons.traumaHook(player, hand, target.getBoundingBox().getCenter());
        } else {
            HeroAbilities.particleLine(player.serverLevel(), hand, target.getBoundingBox().getCenter(), ParticleTypes.ENCHANTED_HIT, 0.25D);
        }
        HeroAbilities.particleLine(player.serverLevel(), hand, target.getBoundingBox().getCenter(), ParticleTypes.CRIT, 0.4D);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.7F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result heavyThrow(SuitContext context) {
        ServerPlayer player = context.player();
        if (SuitStates.isCarrying(player)) {
            SuitStates.throwCarried(player);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.6F);
            return SuitAbility.Result.FINISHED;
        }
        LivingEntity target = HeroAbilities.cone(player, 3.5D, 60.0D).stream()
                .filter(entity -> SuitTargeting.canHit(player, entity) && !isBoss(entity) && entity.getBbWidth() <= 2.5F)
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .orElse(null);
        if (target == null) {
            player.displayClientMessage(Component.literal("§cNothing to grab"), true);
            return SuitAbility.Result.FAILED;
        }
        SuitStates.grab(player, target, context.damage(SuitAbilityConfig.HEAVY_THROW));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
        return SuitAbility.Result.STARTED;
    }

    static SuitAbility.Result rally(SuitContext context) {
        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        int ticks = SuitAbilityConfig.RALLY_TICKS.get();
        double radius = SuitAbilityConfig.RALLY_RADIUS.get();
        for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), entity -> isAlly(player, entity))) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, ally.getX(), ally.getY() + ally.getBbHeight(), ally.getZ(), 6, 0.3D, 0.3D, 0.3D, 0.0D);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
        return SuitAbility.Result.FIRED;
    }

    private static boolean isAlly(ServerPlayer player, LivingEntity entity) {
        if (entity == player || player.isAlliedTo(entity)) {
            return true;
        }
        if (entity instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID())) {
            return true;
        }
        return entity instanceof Player && !SuitTargeting.canHit(player, entity);
    }

    static SuitAbility.Result protonCannon(SuitContext context) {
        ServerPlayer player = context.player();
        SuitStates.State state = SuitStates.get(player);
        if (state.protonTicks > 0) {
            return SuitAbility.Result.FAILED;
        }
        state.protonTicks = SuitAbilityConfig.PROTON_CANNON_TICKS.get();
        state.protonDamage = (float) (context.unibeam() * SuitAbilityConfig.PROTON_CANNON.damageMultiplier().get());
        ServerLevel level = player.serverLevel();
        SuitFx.burst(level, SuitFx.PLASMA_RING, ParticleTypes.FLASH, beamOrigin(player), 1, 0.0D, 0.0D);
        SuitFx.sound(level, player.position(), SuitFx.BEAM_FIRE, MarvelSoundEvents.IRON_MAN_UNIBEAM_SHOOT.get(), 1.2F, 0.8F);
        return SuitAbility.Result.FIRED;
    }

    private static Vec3 beamOrigin(ServerPlayer player) {
        return player.getEyePosition().add(player.getLookAngle().scale(0.7D)).subtract(0.0D, 0.35D, 0.0D);
    }

    static void protonPulse(ServerPlayer player, int ticksLeft, float damage) {
        ServerLevel level = player.serverLevel();
        Vec3 start = beamOrigin(player);
        Vec3 look = player.getLookAngle();
        double range = SuitAbilityConfig.PROTON_CANNON_RANGE.get();
        BlockHitResult block = level.clip(new ClipContext(start, start.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getType() == HitResult.Type.MISS ? start.add(look.scale(range)) : block.getLocation();

        HeroAbilities.particleLine(level, start, end, SuitFx.particle(SuitFx.LASER, ParticleTypes.ELECTRIC_SPARK), 0.6D);
        level.sendParticles(SuitFx.particle(SuitFx.PLASMA_EXPLOSION, ParticleTypes.FLASH), end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

        if (ticksLeft % SuitAbilityConfig.PROTON_CANNON_HIT_INTERVAL.get() != 0) {
            return;
        }
        Vec3 finalEnd = end;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1.0D),
                entity -> SuitTargeting.canHit(player, entity) && entity.getBoundingBox().inflate(0.6D).clip(start, finalEnd).isPresent())) {
            target.invulnerableTime = 0;
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
        }
        if (block.getType() != HitResult.Type.MISS && level.getGameRules().getBoolean(MarvelGameRules.RULE_SUPERPOWERGRIEFING)) {
            BlockPos pos = block.getBlockPos();
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness >= 0.0F && hardness <= SuitAbilityConfig.HYDRAULIC_PUNCH_MAX_HARDNESS.get() && level.getBlockEntity(pos) == null) {
                level.destroyBlock(pos, true, player);
            }
        }
        SuitFx.sound(level, end, SuitFx.LASER_FIRE, SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.4F);
    }
}
