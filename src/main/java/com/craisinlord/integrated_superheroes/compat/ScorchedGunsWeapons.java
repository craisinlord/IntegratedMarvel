package com.craisinlord.integrated_superheroes.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.entity.projectile.ProjectileEntity;
import top.ribs.scguns.entity.projectile.TraumaHookEntity;
import top.ribs.scguns.entity.throwable.ThrowableGrenadeEntity;
import top.ribs.scguns.entity.throwable.ThrowableHellfireBombEntity;
import top.ribs.scguns.entity.throwable.ThrowableStunGrenadeEntity;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.item.GunItem;

public final class ScorchedGunsWeapons {
    private ScorchedGunsWeapons() {
    }

    public static boolean isGun(ItemStack stack) {
        return stack.getItem() instanceof GunItem;
    }

    public static boolean isBullet(Entity entity) {
        return entity instanceof ProjectileEntity;
    }

    public static boolean isThrowable(Entity entity) {
        return entity instanceof top.ribs.scguns.entity.throwable.ThrowableItemEntity;
    }

    public static LivingEntity bulletShooter(Entity entity) {
        return entity instanceof ProjectileEntity bullet ? bullet.getShooter() : null;
    }

    public static Projectile grenade(ServerLevel level, LivingEntity owner, int fuse, float damage, float radius) {
        ThrowableGrenadeEntity grenade = new ThrowableGrenadeEntity(level, owner, fuse);
        grenade.setExplosionDamage(damage);
        grenade.setExplosionRadius(radius);
        return grenade;
    }

    public static Projectile hellfireBomb(ServerLevel level, LivingEntity owner, int fuse) {
        return new ThrowableHellfireBombEntity(level, owner, fuse);
    }

    public static Projectile flashbang(ServerLevel level, LivingEntity owner, int fuse) {
        return new ThrowableStunGrenadeEntity(level, owner, fuse);
    }

    public static void traumaHook(ServerPlayer player, Vec3 hand, Vec3 target) {
        TraumaHookEntity hook = new TraumaHookEntity(ModEntities.TRAUMA_HOOK.get(), player, player.level());
        hook.moveTo(hand.x, hand.y, hand.z, player.getYRot(), player.getXRot());
        hook.setDeltaMovement(target.subtract(hand).normalize().scale(2.5D));
        player.level().addFreshEntity(hook);
    }
}
