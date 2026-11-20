package com.craisinlord.integrated_superheroes.hero.skin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class RedHulkHeat {
    public static final String TRAIT = "heat";

    private RedHulkHeat() {
    }

    public static boolean active(Player player) {
        return SerumSkins.has(player, TRAIT);
    }

    public static boolean blocksDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
        if (!active(player) || !event.getSource().is(DamageTypeTags.IS_FIRE)) {
            return false;
        }
        player.clearFire();
        return true;
    }

    public static void ignite(ServerPlayer player, LivingIncomingDamageEvent event, float rage) {
        if (!active(player) || event.getEntity() == player) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target.fireImmune()) {
            return;
        }
        float seconds = 3.0F + rage / 100.0F * 5.0F;
        target.igniteForSeconds(Math.max(seconds, target.getRemainingFireTicks() / 20.0F));
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    6 + (int) (rage / 10.0F), 0.3D, 0.4D, 0.3D, 0.03D);
        }
    }

    public static void tick(ServerPlayer player, float rage) {
        if (!active(player)) {
            return;
        }
        if (player.isOnFire()) {
            player.clearFire();
        }
        if (rage < 30.0F || player.tickCount % 4 != 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        int count = 1 + (int) (rage / 25.0F);
        level.sendParticles(rage >= 80.0F ? ParticleTypes.LAVA : ParticleTypes.FLAME, player.getX(), player.getY() + player.getBbHeight() * 0.5D,
                player.getZ(), rage >= 80.0F ? 1 : count, player.getBbWidth() * 0.4D, player.getBbHeight() * 0.4D, player.getBbWidth() * 0.4D, 0.01D);
        if (rage >= 80.0F) {
            level.sendParticles(ParticleTypes.SMALL_FLAME, player.getX(), player.getY() + player.getBbHeight() * 0.5D,
                    player.getZ(), count, player.getBbWidth() * 0.4D, player.getBbHeight() * 0.4D, player.getBbWidth() * 0.4D, 0.01D);
        }
    }
}
