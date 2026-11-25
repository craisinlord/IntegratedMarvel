package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.hero.HeroCaster;
import net.minecraft.world.entity.item.PrimedTnt;
import com.craisinlord.integrated_superheroes.compat.AlexsCavesSuits;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.compat.IceAndFireCharges;
import com.craisinlord.integrated_superheroes.compat.FallbackGrenade;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Comparator;

public final class SuitIntegrationAbilities {
    private static final String CHARGE_DAMAGE = "integrated_superheroes_suit_charge_damage";
    private static final String CHARGE_KIND = "integrated_superheroes_suit_charge_kind";
    private static final String CRYO = "cryo";
    private static final String ARC = "arc";

    private SuitIntegrationAbilities() {
    }

    static SuitAbility.Result cryoCharge(SuitContext context) {
        if (ModCompat.ICE_AND_FIRE) {
            return fireCharge(context, IceAndFireCharges.ice(context.caster().level()), CRYO, SuitAbilityConfig.CRYO_CHARGE, SoundEvents.GLASS_BREAK);
        }
        LivingEntity player = context.caster();
        Snowball snowball = new Snowball(player.level(), player);
        snowball.setItem(new ItemStack(Items.SNOWBALL));
        snowball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.0F, 0.5F);
        tag(snowball, context.damage(SuitAbilityConfig.CRYO_CHARGE), CRYO);
        player.level().addFreshEntity(snowball);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result fireCharge(SuitContext context) {
        Level level = context.caster().level();
        AbstractHurtingProjectile charge = ModCompat.ICE_AND_FIRE
                ? IceAndFireCharges.fire(level)
                : new SmallFireball(level, context.caster(), context.caster().getLookAngle());
        return fireCharge(context, charge, "fire", SuitAbilityConfig.FIRE_CHARGE, SoundEvents.BLAZE_SHOOT);
    }

    static SuitAbility.Result arcCharge(SuitContext context) {
        if (ModCompat.ICE_AND_FIRE) {
            return fireCharge(context, IceAndFireCharges.lightning(context.caster().level()), ARC, SuitAbilityConfig.ARC_CHARGE,
                    SoundEvents.TRIDENT_THUNDER.value());
        }
        LivingEntity player = context.caster();
        ServerLevel level = HeroCaster.level(player);
        double range = 32.0D;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        HitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D),
                entity -> entity instanceof LivingEntity living && SuitTargeting.canHit(player, living), eye.distanceToSqr(end));
        Vec3 strike = hit != null ? hit.getEntity().position() : end;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(strike.x, strike.y, strike.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        if (HeroFx.vfx(level, strike, "fork_burst", 0, 3.0F)) {
            HeroAbilities.particleLine(level, eye.add(0.0D, -0.3D, 0.0D), strike, HeroFx.particle("thematic:shock_particle", ParticleTypes.ELECTRIC_SPARK), 0.5D);
        } else {
            HeroAbilities.particleLine(level, eye.add(0.0D, -0.3D, 0.0D), strike, ParticleTypes.ELECTRIC_SPARK, 0.5D);
        }
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            float damage = context.damage(SuitAbilityConfig.ARC_CHARGE);
            target.invulnerableTime = 0;
            HeroAbilities.runAsSuitDamage(() -> target.hurt(SuitTargeting.attack(player), damage));
            chain(player, target, damage);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
        return SuitAbility.Result.FIRED;
    }

    private static void tag(Entity charge, float damage, String kind) {
        CompoundTag tag = charge.getPersistentData();
        tag.putFloat(CHARGE_DAMAGE, damage);
        tag.putString(CHARGE_KIND, kind);
    }

    private static SuitAbility.Result fireCharge(SuitContext context, AbstractHurtingProjectile charge, String kind, SuitAbilityConfig.Settings settings,
                                                 net.minecraft.sounds.SoundEvent sound) {
        LivingEntity player = context.caster();
        Vec3 look = player.getLookAngle();
        Vec3 from = player.getEyePosition().add(look.scale(1.0D)).add(0.0D, -0.3D, 0.0D);
        charge.setOwner(player);
        charge.moveTo(from.x, from.y, from.z, player.getYRot(), player.getXRot());
        charge.setDeltaMovement(look.scale(1.5D));
        charge.accelerationPower = 0.1D;
        tag(charge, context.damage(settings), kind);
        player.level().addFreshEntity(charge);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 0.8F, 1.2F);
        return SuitAbility.Result.FIRED;
    }

    static void onChargeDamage(LivingIncomingDamageEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        if (!(direct instanceof Projectile charge) || !charge.getPersistentData().contains(CHARGE_DAMAGE)
                || !(charge.getOwner() instanceof LivingEntity owner) || HeroAbilities.isAbilityDamage()) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (!SuitTargeting.canHit(owner, target)) {
            event.setCanceled(true);
            return;
        }
        float damage = charge.getPersistentData().getFloat(CHARGE_DAMAGE);
        event.setAmount(damage);
        target.invulnerableTime = 0;
        String kind = charge.getPersistentData().getString(CHARGE_KIND);
        if (CRYO.equals(kind)) {
            if (ModCompat.ICE_AND_FIRE) {
                target.addEffect(new MobEffectInstance(IceAndFireCharges.frozen(), 100, 0));
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 100));
            }
        } else if (ARC.equals(kind)) {
            chain(owner, target, damage);
        }
    }

    private static void chain(LivingEntity owner, LivingEntity from, float damage) {
        double range = SuitAbilityConfig.ARC_CHAIN_RANGE.get();
        LivingEntity next = from.level().getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(range),
                        entity -> entity != from && SuitTargeting.canHit(owner, entity) && entity.distanceToSqr(from) <= range * range)
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(from)))
                .orElse(null);
        if (next == null || !(from.level() instanceof ServerLevel level)) {
            return;
        }
        HeroAbilities.particleLine(level, from.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK, 0.3D);
        next.invulnerableTime = 0;
        HeroAbilities.runAsSuitDamage(() -> next.hurt(SuitTargeting.attack(owner), damage));
    }

    static SuitAbility.Result depthCharge(SuitContext context) {
        LivingEntity player = context.caster();
        if (!player.isInWater()) {
            HeroCaster.message(player, Component.literal("§cDepth charges only work underwater"));
            return SuitAbility.Result.FAILED;
        }
        if (ModCompat.ALEXS_CAVES) {
            SuitMobSupport.noGrief(player, AlexsCavesSuits.depthCharge(player));
        } else if (!HeroCaster.griefs(player)) {
            FallbackGrenade charge = FallbackGrenade.create(player.level(), player, 30).explosive(context.damage(SuitAbilityConfig.DEPTH_CHARGE), 3.0F);
            charge.setDeltaMovement(player.getLookAngle().scale(1.0D));
            player.level().addFreshEntity(charge);
        } else {
            PrimedTnt charge = new PrimedTnt(player.level(), player.getX(), player.getEyeY() - 0.2D, player.getZ(), player);
            charge.setFuse(30);
            charge.setDeltaMovement(player.getLookAngle().scale(1.0D));
            player.level().addFreshEntity(charge);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_LAUNCH, SoundSource.PLAYERS, 1.0F, 0.8F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result magnetPulse(SuitContext context) {
        LivingEntity player = context.caster();
        ServerLevel level = HeroCaster.level(player);
        double radius = SuitAbilityConfig.MAGNET_PULSE_RADIUS.get();
        AABB area = player.getBoundingBox().inflate(radius);
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        for (Entity loose : level.getEntities(player, area, entity -> entity instanceof ItemEntity || entity instanceof ExperienceOrb)) {
            loose.setDeltaMovement(center.subtract(loose.position()).normalize().scale(0.9D).add(0.0D, 0.2D, 0.0D));
            loose.hurtMarked = true;
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, entity -> SuitTargeting.canHit(player, entity) && wearsArmor(entity))) {
            HeroFx.effect("alexscaves:magnetizing").ifPresent(effect -> target.addEffect(new MobEffectInstance(effect, 60, 0)));
            target.setDeltaMovement(center.subtract(target.position()).normalize().scale(0.9D).add(0.0D, 0.25D, 0.0D));
            target.hurtMarked = true;
            HeroAbilities.particleLine(level, center, target.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK, 0.5D);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 40, radius * 0.3D, 0.5D, radius * 0.3D, 0.1D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.8F);
        return SuitAbility.Result.FIRED;
    }

    private static boolean wearsArmor(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (entity.getItemBySlot(slot).getItem() instanceof ArmorItem) {
                return true;
            }
        }
        return false;
    }
}
