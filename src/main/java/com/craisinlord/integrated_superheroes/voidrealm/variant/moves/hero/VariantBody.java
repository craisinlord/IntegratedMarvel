package com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class VariantBody {
    public enum Flight {
        NONE,
        HOVER,
        SWOOP,
        LEVITATE
    }

    private static final String FLYING = "integrated_superheroes_variant_flying";
    private static final String ANCHOR = "integrated_superheroes_variant_anchor";
    private static final String SIZE_LOCK = "integrated_superheroes_variant_size_lock";
    private static final ResourceLocation SIZE = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "variant_size");
    private static final ResourceLocation HASTE = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "variant_haste");

    private VariantBody() {
    }

    public static void fly(Mob mob, LivingEntity target, Flight mode, ParticleOptions exhaust) {
        boolean flagged = mob.getPersistentData().getBoolean(FLYING);
        boolean want = mode != Flight.NONE && target != null && target.isAlive() && target.level() == mob.level()
                && mob.distanceToSqr(target) < 40.0D * 40.0D && !mob.isPassenger() && !mob.isInWaterOrBubble();
        if (!want) {
            land(mob);
            return;
        }
        if (!flagged) {
            mob.getPersistentData().putBoolean(FLYING, true);
        }
        if (grounded(mob)) {
            mob.getPersistentData().putLong(ANCHOR, mob.blockPosition().asLong());
        }
        mob.setNoGravity(true);
        mob.fallDistance = 0.0F;
        if (mob.getDeltaMovement().lengthSqr() > 0.8D * 0.8D) {
            return;
        }
        Vec3 goal = goal(mob, target, mode);
        Vec3 delta = goal.subtract(mob.position());
        Vec3 velocity = delta.scale(0.2D);
        double max = mode == Flight.LEVITATE ? 0.35D : 0.6D;
        if (velocity.lengthSqr() > max * max) {
            velocity = velocity.normalize().scale(max);
        }
        mob.setDeltaMovement(velocity);
        mob.hurtMarked = true;
        mob.getNavigation().stop();
        HeroKit.face(mob, target);
        if (exhaust != null && mob.level() instanceof ServerLevel level) {
            level.sendParticles(exhaust, mob.getX(), mob.getY() + 0.1D, mob.getZ(), 3, 0.15D, 0.05D, 0.15D, 0.01D);
        }
    }

    private static Vec3 goal(Mob mob, LivingEntity target, Flight mode) {
        int seed = mob.getId() * 31;
        double angle = (mob.tickCount + seed) * 0.03D;
        if (mode == Flight.SWOOP && (mob.tickCount + seed) / 70 % 2 == 1) {
            return target.position().add(0.0D, 0.3D, 0.0D);
        }
        if (mode == Flight.HOVER && (mob.tickCount + seed) / 100 % 3 == 2) {
            return target.position().add(Math.cos(angle) * 3.0D, 1.0D, Math.sin(angle) * 3.0D);
        }
        double radius = mode == Flight.LEVITATE ? 5.0D : 7.0D;
        double height = mode == Flight.LEVITATE ? 2.0D : 4.0D;
        return target.position().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }

    public static void land(Mob mob) {
        if (!mob.getPersistentData().getBoolean(FLYING)) {
            return;
        }
        mob.getPersistentData().remove(FLYING);
        mob.setNoGravity(false);
        if (!grounded(mob) && mob.getPersistentData().contains(ANCHOR)) {
            BlockPos anchor = BlockPos.of(mob.getPersistentData().getLong(ANCHOR));
            mob.teleportTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        }
        mob.getPersistentData().remove(ANCHOR);
        mob.fallDistance = 0.0F;
        if (!mob.onGround()) {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false));
        }
    }

    private static boolean grounded(Mob mob) {
        int top = mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING, mob.getBlockX(), mob.getBlockZ());
        return top > mob.level().getMinBuildHeight();
    }

    public static void size(Mob mob, double change) {
        modify(mob, Attributes.SCALE, SIZE, change);
    }

    public static void lockSize(Mob mob, double change, int ticks) {
        size(mob, change);
        mob.getPersistentData().putLong(SIZE_LOCK, mob.level().getGameTime() + ticks);
    }

    public static boolean sizeLocked(Mob mob) {
        return mob.level().getGameTime() < mob.getPersistentData().getLong(SIZE_LOCK);
    }

    public static void haste(Mob mob, double change) {
        modify(mob, Attributes.MOVEMENT_SPEED, HASTE, change);
    }

    private static void modify(Mob mob, Holder<Attribute> attribute, ResourceLocation id, double change) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (Math.abs(change) < 1.0E-4D) {
            instance.removeModifier(id);
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (current != null && Math.abs(current.amount() - change) < 1.0E-4D) {
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(id, change, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }
}
