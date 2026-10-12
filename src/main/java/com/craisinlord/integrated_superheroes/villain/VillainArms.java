package com.craisinlord.integrated_superheroes.villain;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.ScorchedGunsVillains;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class VillainArms {
    private VillainArms() {
    }

    public static boolean gun(ItemStack stack) {
        return ModCompat.SCORCHED_GUNS && ScorchedGunsVillains.isGun(stack);
    }

    public static boolean disarmable(ItemStack stack) {
        if (ModCompat.SCORCHED_GUNS) {
            return ScorchedGunsVillains.isGun(stack);
        }
        return stack.getItem() instanceof CrossbowItem || stack.is(Items.IRON_SWORD) || stack.is(Items.IRON_AXE);
    }

    public static void shoot(Mob mob, ItemStack weapon, Vec3 at, float volume) {
        if (ModCompat.SCORCHED_GUNS) {
            ScorchedGunsVillains.shoot(mob, weapon, at, volume);
            return;
        }
        arrow(mob, at, 3.0F, 3.5D, volume);
    }

    public static void arrow(Mob mob, Vec3 at, float speed, double damage, float volume) {
        Arrow arrow = new Arrow(mob.level(), mob, new ItemStack(Items.ARROW), null);
        arrow.setBaseDamage(damage);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        double dx = at.x - arrow.getX();
        double dy = at.y - arrow.getY();
        double dz = at.z - arrow.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + flat * 0.06D, dz, speed, 1.0F);
        mob.level().addFreshEntity(arrow);
        VillainFx.sound(mob, SoundEvents.CROSSBOW_SHOOT, volume, 1.0F);
    }

    public static void grenade(ServerLevel level, LivingEntity thrower, Vec3 spot, boolean stun, float power) {
        if (ModCompat.SCORCHED_GUNS) {
            ScorchedGunsVillains.throwGrenade(level, thrower, spot, stun);
            return;
        }
        Snowball ball = new Snowball(level, thrower);
        ball.setItem(new ItemStack(stun ? Items.FIREWORK_STAR : Items.TNT));
        ball.addTag(Villains.COSMETIC_TAG);
        Vec3 delta = spot.subtract(ball.position());
        double ticks = 14.0D;
        ball.setDeltaMovement(delta.x / ticks, delta.y / ticks + 0.5D * 0.03D * ticks, delta.z / ticks);
        level.addFreshEntity(ball);
        VillainWorld.later(level, 16, () -> detonate(level, thrower, spot, stun, power));
    }

    private static void detonate(ServerLevel level, LivingEntity thrower, Vec3 spot, boolean stun, float power) {
        if (!stun) {
            level.explode(thrower.isAlive() ? thrower : null, spot.x, spot.y + 0.2D, spot.z, power, Level.ExplosionInteraction.NONE);
            return;
        }
        level.sendParticles(ParticleTypes.FLASH, spot.x, spot.y + 0.5D, spot.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.CLOUD, spot.x, spot.y + 0.3D, spot.z, 16, 1.0D, 0.3D, 1.0D, 0.02D);
        VillainFx.sound(level, spot, SoundEvents.FIREWORK_ROCKET_BLAST, 1.5F, 0.8F);
        for (LivingEntity victim : VillainWorld.victims(level, spot, 3.0D, 2.5D, e -> !Villains.isVillainSide(e))) {
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
        }
    }
}
