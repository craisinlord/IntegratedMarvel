package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = IntegratedMarvel.MODID, value = Dist.CLIENT)
public final class DaredevilClient {
    public static final int OUTLINE = 0xFF2020;
    private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "shaders/post/radar_sense.json");
    private static final DustParticleOptions PULSE = new DustParticleOptions(new Vector3f(0.95F, 0.12F, 0.1F), 1.0F);
    private static boolean on;
    private static double rangeSqr;
    private static boolean loaded;
    private static int pulseAge = -1;
    private static int clock;

    private DaredevilClient() {
    }

    public static boolean highlights(Entity entity) {
        if (!on || !(entity instanceof LivingEntity)) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && entity != player && entity.distanceToSqr(player) <= rangeSqr;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        on = player != null && DaredevilPower.isRadarOn(player);
        if (on) {
            boolean dark = player.level().isNight() || player.level().getMaxLocalRawBrightness(player.blockPosition()) < 4
                    || player.hasEffect(MobEffects.BLINDNESS) || player.hasEffect(MobEffects.DARKNESS);
            double range = dark ? MarvelBalanceConfig.RADAR_DARK_RANGE.get() : MarvelBalanceConfig.RADAR_RANGE.get();
            rangeSqr = range * range;
        }
        tickShader(minecraft);
        if (!on) {
            pulseAge = -1;
            return;
        }
        clock++;
        if (clock % 40 == 0) {
            pulseAge = 0;
        }
        if (pulseAge >= 0) {
            double radius = 1.5D + pulseAge * 2.5D;
            int points = (int) Math.min(64, 12 + radius * 3);
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                player.level().addParticle(PULSE, player.getX() + Math.cos(angle) * radius, player.getY() + 0.15D,
                        player.getZ() + Math.sin(angle) * radius, 0.0D, 0.0D, 0.0D);
            }
            pulseAge++;
            if (pulseAge > 10) {
                pulseAge = -1;
            }
        }
    }

    private static void tickShader(Minecraft minecraft) {
        if (minecraft.gameRenderer == null) {
            return;
        }
        var current = minecraft.gameRenderer.currentEffect();
        boolean showing = current != null && SHADER.toString().equals(current.getName());
        if (on && !showing && current == null) {
            minecraft.gameRenderer.loadEffect(SHADER);
            loaded = true;
        } else if (!on && showing) {
            minecraft.gameRenderer.shutdownEffect();
            loaded = false;
        } else if (!showing) {
            loaded = false;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        on = false;
        pulseAge = -1;
        Minecraft minecraft = Minecraft.getInstance();
        if (loaded && minecraft.gameRenderer != null && minecraft.gameRenderer.currentEffect() != null
                && SHADER.toString().equals(minecraft.gameRenderer.currentEffect().getName())) {
            minecraft.gameRenderer.shutdownEffect();
        }
        loaded = false;
    }
}
