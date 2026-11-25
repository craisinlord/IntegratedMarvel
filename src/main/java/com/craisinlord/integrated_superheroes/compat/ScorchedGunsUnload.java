package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.mrcrayfish.framework.api.network.LevelLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.DistantGunSoundRouter;
import top.ribs.scguns.common.FireMode;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.ProjectileManager;
import top.ribs.scguns.common.SpreadTracker;
import top.ribs.scguns.entity.projectile.ProjectileEntity;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageBulletTrail;
import top.ribs.scguns.network.message.S2CMessageGunSound;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.GunModifierHelper;

import java.util.List;
import java.util.function.Consumer;

public final class ScorchedGunsUnload {
    private static final String TAG = "integrated_superheroes_unload";
    private static final String CHARGE = "ChargeProgress";
    private static final int DEFAULT_BEAM_COLOR = 0xFF4040;

    private ScorchedGunsUnload() {
    }

    public static boolean isUnloadShot(Entity entity) {
        return entity != null && entity.getPersistentData().getBoolean(TAG);
    }

    public static int interval(ItemStack stack, double speed) {
        if (!(stack.getItem() instanceof GunItem item)) {
            return 1;
        }
        Gun gun = item.getModifiedGun(stack);
        Gun.General general = gun.getGeneral();
        double delay = Math.max(1, GunModifierHelper.getModifiedRate(stack, GunEnchantmentHelper.getRate(stack, gun)));
        if (FireMode.PULSE.equals(general.getFireMode())) {
            delay = Math.max(delay, general.getFireTimer());
        } else if (FireMode.BURST.equals(general.getFireMode())) {
            int amount = Math.max(1, general.getBurstAmount());
            delay = ((amount - 1) * delay + Math.max(delay, general.getBurstCooldown())) / amount;
        }
        return Math.max(1, (int) Math.round(delay / speed));
    }

    public static List<LivingEntity> shoot(ServerPlayer player, ItemStack stack, Vec3 direction, float floor) {
        if (!(stack.getItem() instanceof GunItem item)) {
            return List.of();
        }
        Gun gun = item.getModifiedGun(stack);
        Gun.Projectile projectile = Gun.getLoadedProjectile(stack, gun);
        FireMode mode = gun.getGeneral().getFireMode();
        List<LivingEntity> hit = List.of();
        if (FireMode.BEAM.equals(mode) || FireMode.SEMI_BEAM.equals(mode)) {
            hit = beam(player, stack, gun, direction, floor);
        } else if (projectile.firesArrows()) {
            arrows(player, gun, projectile, direction, floor);
        } else {
            bullets(player, stack, item, gun, projectile, direction, floor);
        }
        sound(player, stack, gun, gun.getDisplay().getFlash() != null);
        return hit;
    }

    private static void bullets(ServerPlayer player, ItemStack stack, GunItem item, Gun gun, Gun.Projectile projectile, Vec3 direction, float floor) {
        float extra = Gun.getAdditionalDamage(stack);
        float global = Config.COMMON.gameplay.globalDamageMultiplier.get().floatValue();
        if (global > 0.0F) {
            extra = Math.max(extra, floor / global - projectile.getDamage());
        }
        launch(player, stack, item, gun, projectile, direction, extra, bullet -> bullet.getPersistentData().putBoolean(TAG, true));
    }

    static void launch(ServerPlayer player, ItemStack stack, GunItem item, Gun gun, Gun.Projectile projectile, Vec3 direction, float extra,
                       Consumer<ProjectileEntity> setup) {
        ServerLevel level = player.serverLevel();
        int count = Math.max(1, gun.getGeneral().getProjectileAmount());
        if (!gun.getGeneral().isAlwaysSpread() && gun.getGeneral().getSpread() > 0.0F) {
            SpreadTracker.get(player).update(player, item);
        }
        ProjectileEntity[] spawned = new ProjectileEntity[count];
        CompoundTag data = player.getPersistentData();
        boolean charged = data.contains(CHARGE);
        float charge = data.getFloat(CHARGE);
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        player.setYRot((float) Math.toDegrees(Mth.atan2(-direction.x, direction.z)));
        player.setXRot((float) -Math.toDegrees(Mth.atan2(direction.y, direction.horizontalDistance())));
        data.putFloat(CHARGE, 1.0F);
        try {
            for (int i = 0; i < count; i++) {
                ProjectileEntity bullet = ProjectileManager.getInstance().getFactory(BuiltInRegistries.ITEM.getKey(projectile.getItem()))
                        .create(level, player, stack, item, gun);
                bullet.setWeapon(stack);
                bullet.setAdditionalDamage(extra);
                setup.accept(bullet);
                spawned[i] = bullet;
            }
        } finally {
            player.setYRot(yRot);
            player.setXRot(xRot);
            if (charged) {
                data.putFloat(CHARGE, charge);
            } else {
                data.remove(CHARGE);
            }
        }
        for (ProjectileEntity bullet : spawned) {
            level.addFreshEntity(bullet);
            bullet.tick();
        }
        if (!projectile.shouldHideProjectile() && !projectile.shouldHideTrail()) {
            S2CMessageBulletTrail trail = new S2CMessageBulletTrail(spawned, projectile, player.getId(), GunEnchantmentHelper.getParticle(stack));
            double range = Config.COMMON.network.projectileTrackingRange.get();
            PacketHandler.getPlayChannel().sendToNearbyPlayers(
                    () -> LevelLocation.create(level, player.getX(), player.getY() + 1.0D, player.getZ(), range), trail);
        }
    }

    private static void arrows(ServerPlayer player, Gun gun, Gun.Projectile projectile, Vec3 direction, float floor) {
        float speed = (float) projectile.getSpeed() * 0.35F;
        int count = Math.max(1, gun.getGeneral().getProjectileAmount());
        double damage = projectile.getDamage() * 0.15D;
        if (speed > 0.0F) {
            damage = Math.max(damage, floor / count / speed);
        }
        for (int i = 0; i < count; i++) {
            Arrow arrow = new Arrow(player.serverLevel(), player, Items.ARROW.getDefaultInstance(), null);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.setPos(player.getEyePosition().add(direction.x * 0.5D, -0.1D, direction.z * 0.5D));
            arrow.setDeltaMovement(direction.scale(speed));
            arrow.setYRot((float) Math.toDegrees(Mth.atan2(direction.x, direction.z)));
            arrow.setXRot((float) Math.toDegrees(Mth.atan2(direction.y, direction.horizontalDistance())));
            arrow.yRotO = arrow.getYRot();
            arrow.xRotO = arrow.getXRot();
            arrow.setBaseDamage(damage);
            arrow.getPersistentData().putBoolean(TAG, true);
            player.serverLevel().addFreshEntity(arrow);
        }
    }

    private static List<LivingEntity> beam(ServerPlayer player, ItemStack stack, Gun gun, Vec3 direction, float floor) {
        ServerLevel level = player.serverLevel();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, direction, gun.getGeneral().getBeamMaxDistance(), 1);
        DamageSource source = ModDamageTypes.Sources.projectile(level.registryAccess(), null, player);
        for (LivingEntity target : beam.targets()) {
            float damage = GunModifierHelper.getModifiedDamage(stack, gun, gun.getProjectile().getDamage());
            damage = GunEnchantmentHelper.getAcceleratorDamage(stack, damage);
            damage = GunEnchantmentHelper.getHeavyShotDamage(stack, damage);
            damage = GunEnchantmentHelper.getHotBarrelDamage(player, stack, damage);
            damage *= Config.COMMON.gameplay.globalDamageMultiplier.get().floatValue();
            damage += EnchantmentHelper.modifyDamage(level, stack, target, source, 0.0F);
            if (!(target instanceof Player)) {
                target.invulnerableTime = 0;
            }
            if (!target.hurt(source, Math.max(damage, floor))) {
                continue;
            }
            target.invulnerableTime = 0;
            if (stack.is(ModItems.FLAYED_GOD.get())) {
                wither(target);
            }
            GunEnchantmentHelper.applyElementalPopEffect(stack, target);
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, stack);
            if (GunEnchantmentHelper.shouldSetOnFire(player, stack)) {
                target.igniteForSeconds(5.0F);
            }
        }
        Vec3 barrel = player.getEyePosition().add(0.0D, -0.25D, 0.0D).add(direction.scale(0.7D));
        HeroAbilities.particleLine(level, barrel, beam.end(), HeroAbilities.emissive(color(gun.getGeneral().getBeamColor()), 0.8F), 0.4D);
        return beam.targets();
    }

    private static void wither(LivingEntity target) {
        if (target.getRandom().nextFloat() < 0.75F) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0, false, true));
        }
        if (target.getRandom().nextFloat() < 0.5F) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true));
        }
        if (target.getRandom().nextFloat() < 0.5F) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0, false, true));
        }
    }

    static void sound(ServerPlayer player, ItemStack stack, Gun gun, boolean flash) {
        ResourceLocation sound = null;
        if (GunModifierHelper.isSilencedFire(stack)) {
            sound = gun.getSounds().getSilencedFire();
        } else if (stack.isEnchanted()) {
            sound = gun.getSounds().getEnchantedFire();
        }
        if (sound == null) {
            sound = gun.getSounds().getFire();
        }
        if (sound == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        float volume = GunModifierHelper.getFireSoundVolume(stack);
        float pitch = 0.9F + level.random.nextFloat() * 0.2F;
        double radius = GunModifierHelper.getModifiedFireSoundRadius(stack, Config.SERVER.gunShotMaxDistance.get());
        S2CMessageGunSound message = new S2CMessageGunSound(sound, SoundSource.PLAYERS, (float) eye.x, (float) eye.y, (float) eye.z, volume, pitch,
                player.getId(), flash, false);
        PacketHandler.getPlayChannel().sendToNearbyPlayers(() -> LevelLocation.create(level, eye.x, eye.y, eye.z, radius), message);
        DistantGunSoundRouter.send(level, eye, SoundSource.PLAYERS, volume, pitch, player.getId(), GunModifierHelper.isSilencedFire(stack), radius);
    }

    private static int color(String raw) {
        if (raw == null) {
            return DEFAULT_BEAM_COLOR;
        }
        try {
            return (int) (Long.parseLong(raw.replace("#", ""), 16) & 0xFFFFFFL);
        } catch (NumberFormatException ignored) {
            return DEFAULT_BEAM_COLOR;
        }
    }
}
