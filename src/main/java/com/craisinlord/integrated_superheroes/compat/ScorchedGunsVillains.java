package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.villain.VillainFx;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.config.GunnerMobSpawner;
import top.ribs.scguns.entity.throwable.ThrowableGrenadeEntity;
import top.ribs.scguns.entity.throwable.ThrowableStunGrenadeEntity;
import top.ribs.scguns.entity.weapon.ScGunsWeapon;
import top.ribs.scguns.item.GunItem;

public final class ScorchedGunsVillains {
    private ScorchedGunsVillains() {
    }

    public static boolean isGun(ItemStack stack) {
        return stack.getItem() instanceof GunItem;
    }

    public static void reassessGunner(PathfinderMob mob) {
        try {
            GunnerMobSpawner.reassessWeaponGoal(mob);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void prepareGunner(ItemStack weapon, CompoundTag data, int aiDifficulty) {
        if (!(weapon.getItem() instanceof GunItem)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, weapon, tag -> {
            tag.putBoolean("scguns:MobGun", true);
            tag.putInt("AmmoCount", 30);
        });
        data.putBoolean("scguns:GunnerChecked", true);
        data.putInt("scguns:GunnerAiDifficulty", aiDifficulty);
    }

    public static void throwGrenade(ServerLevel level, LivingEntity thrower, Vec3 spot, boolean stun) {
        ThrowableGrenadeEntity grenade = stun ? new ThrowableStunGrenadeEntity(level, thrower, 30) : new ThrowableGrenadeEntity(level, thrower, 30);
        Vec3 from = grenade.position();
        Vec3 delta = spot.subtract(from);
        double ticks = 16.0D;
        grenade.setDeltaMovement(delta.x / ticks, delta.y / ticks + 0.5D * 0.05D * ticks, delta.z / ticks);
        level.addFreshEntity(grenade);
    }

    public static void shoot(Mob mob, ItemStack gun, Vec3 at, float volume) {
        try {
            ScGunsWeapon weapon = new ScGunsWeapon(gun);
            if (weapon.getShootSound() != null) {
                VillainFx.sound(mob, weapon.getShootSound(), volume, 1.0F);
            }
            weapon.performRangedAttackIWeapon(mob, at.x, at.y, at.z, weapon.getProjectileSpeed());
        } catch (RuntimeException | LinkageError ignored) {
        }
    }
}
