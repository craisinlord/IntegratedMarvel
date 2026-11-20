package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.ScorchedGunsWeapons;
import com.stxllarplanes.marvellegacy.init.AttachmentsRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID)
public final class HeroBulletproof {
    private HeroBulletproof() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!ModCompat.SCORCHED_GUNS || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (direct == null || !ScorchedGunsWeapons.isBullet(direct) || !player.hasData(AttachmentsRegistry.PLAYER_DATA.get())) {
            return;
        }
        float limit = limitFor(HeroPowers.data(player).getPrimaryPower());
        if (limit <= 0.0F || event.getAmount() > limit) {
            return;
        }
        event.setCanceled(true);
        ServerLevel level = player.serverLevel();
        Vec3 at = direct.position();
        Vec3 center = player.getBoundingBox().getCenter();
        if (at.distanceToSqr(center) > 4.0D) {
            at = center;
        }
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.1D, 0.1D, 0.1D, 0.3D);
        level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 2, 0.05D, 0.05D, 0.05D, 0.05D);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.25F, 1.9F + level.random.nextFloat() * 0.2F);
    }

    private static float limitFor(String power) {
        if (power == null) {
            return 0.0F;
        }
        List<? extends String> entries = MarvelBalanceConfig.BULLETPROOF_HEROES.get();
        for (String entry : entries) {
            int split = entry.indexOf('=');
            if (split > 0 && entry.substring(0, split).trim().equals(power)) {
                try {
                    return Float.parseFloat(entry.substring(split + 1).trim());
                } catch (NumberFormatException ignored) {
                    return 0.0F;
                }
            }
        }
        return 0.0F;
    }
}
