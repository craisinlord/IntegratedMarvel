package com.craisinlord.integrated_marvel.suit;

import com.craisinlord.integrated_marvel.compat.FallbackGrenade;
import com.craisinlord.integrated_marvel.compat.ModCompat;
import com.craisinlord.integrated_marvel.compat.ScorchedGunsWeapons;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class SuitGrenadeAbilities {
    private static final String SUIT_OWNED = "integrated_marvel_suit_owned";

    private SuitGrenadeAbilities() {
    }

    static void markSuitOwned(Entity entity) {
        entity.getPersistentData().putBoolean(SUIT_OWNED, true);
    }

    static boolean blocksFriendlyFire(LivingIncomingDamageEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        if (direct == null || !direct.getPersistentData().getBoolean(SUIT_OWNED)) {
            return false;
        }
        Entity owner = direct instanceof Projectile projectile ? projectile.getOwner() : event.getSource().getEntity();
        LivingEntity target = event.getEntity();
        return owner instanceof ServerPlayer player && !SuitTargeting.canHit(player, target);
    }

    static SuitAbility.Result grenadeLauncher(SuitContext context) {
        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        int count = SuitAbilityConfig.GRENADE_LAUNCHER_GRENADES.get();
        float damage = context.damage(SuitAbilityConfig.GRENADE_LAUNCHER);
        float radius = SuitAbilityConfig.GRENADE_LAUNCHER_RADIUS.get().floatValue();
        for (int i = 0; i < count; i++) {
            float spread = count == 1 ? 0.0F : (i / (float) (count - 1) - 0.5F) * 16.0F;
            Entity grenade;
            if (ModCompat.SCORCHED_GUNS) {
                Projectile shell = ScorchedGunsWeapons.grenade(level, player, 30 + i * 3, damage, radius);
                shell.shootFromRotation(player, player.getXRot() - 6.0F, player.getYRot() + spread, 0.0F, 1.6F, 1.0F);
                grenade = shell;
            } else {
                grenade = FallbackGrenade.create(level, player, 30 + i * 3).explosive(damage, radius)
                        .throwFrom(player, player.getXRot() - 6.0F, player.getYRot() + spread, 1.6F);
            }
            markSuitOwned(grenade);
            level.addFreshEntity(grenade);
        }
        Vec3 shoulder = player.getEyePosition().add(0.0D, -0.3D, 0.0D).add(player.getLookAngle().scale(0.8D));
        SuitFx.burst(level, SuitFx.MUZZLE_FLASH, ParticleTypes.FLAME, shoulder, 1, 0.0D, 0.0D);
        SuitFx.sound(level, player.position(), SuitFx.GRENADE_LAUNCHER_FIRE, SoundEvents.DISPENSER_LAUNCH, 1.0F, 1.0F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result hellfireBomb(SuitContext context) {
        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        Entity bomb;
        if (ModCompat.SCORCHED_GUNS) {
            Projectile hellfire = ScorchedGunsWeapons.hellfireBomb(level, player, 40);
            hellfire.shootFromRotation(player, player.getXRot() - 8.0F, player.getYRot(), 0.0F, 1.3F, 1.0F);
            bomb = hellfire;
        } else {
            bomb = FallbackGrenade.create(level, player, 40).explosive(Math.max(8.0F, context.damage(SuitAbilityConfig.HELLFIRE_BOMB)), 4.0F).incendiary()
                    .throwFrom(player, player.getXRot() - 8.0F, player.getYRot(), 1.3F);
        }
        markSuitOwned(bomb);
        level.addFreshEntity(bomb);
        SuitFx.sound(level, player.position(), SuitFx.GRENADE_PIN, SoundEvents.TRIPWIRE_CLICK_ON, 1.0F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.7F);
        return SuitAbility.Result.FIRED;
    }
}
