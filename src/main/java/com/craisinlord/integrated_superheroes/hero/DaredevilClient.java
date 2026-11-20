package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class DaredevilClient {
    public static final int OUTLINE = 0xFF2020;
    public static final int XRAY_OUTLINE = 0x8FD8FF;
    public static final int SEA_OUTLINE = 0x3AE0C8;
    private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "shaders/post/radar_sense.json");
    private static final DustParticleOptions PULSE = new DustParticleOptions(new Vector3f(0.95F, 0.12F, 0.1F), 1.0F);
    private static boolean on;
    private static double rangeSqr;
    private static boolean loaded;
    private static int pulseAge = -1;
    private static int clock;
    private static int senseMode;
    private static double senseRadiusSqr;
    private static final String SHADER_NAME = SHADER.toString();

    private DaredevilClient() {
    }

    public static boolean highlights(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || entity == player) {
            return false;
        }
        if (on) {
            return entity.distanceToSqr(player) <= rangeSqr;
        }
        return senseMode != 0 && sensed(player, entity) != 0;
    }

    public static int outline(Entity entity) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (on || player == null) {
            return OUTLINE;
        }
        int sense = sensed(player, entity);
        return sense == 1 ? XRAY_OUTLINE : sense == 3 ? SEA_OUTLINE : OUTLINE;
    }

    private static int sensed(LocalPlayer player, Entity entity) {
        return switch (senseMode) {
            case 1 -> (entity instanceof Enemy || entity instanceof Player) && entity.distanceToSqr(player) <= senseRadiusSqr ? 1 : 0;
            case 2 -> (entity instanceof Mob || entity instanceof Player) && entity.distanceToSqr(player) <= senseRadiusSqr ? 2 : 0;
            case 3 -> entity instanceof Mob && entity.isInWater() && entity.distanceToSqr(player) <= senseRadiusSqr ? 3 : 0;
            default -> 0;
        };
    }

    private static void updateSense(LocalPlayer player) {
        senseMode = 0;
        if (player == null || !player.hasData(com.stxllarplanes.marvellegacy.init.AttachmentsRegistry.PLAYER_DATA.get())) {
            return;
        }
        double radius;
        if (SerumKit.is(player, KryptonianPower.ID) && HeroToggles.isOn(player, HeroToggles.SENSES)) {
            senseMode = 1;
            radius = NewSerumConfig.XRAY_RADIUS.get();
        } else if (SerumKit.is(player, HomelanderPower.ID) && HeroToggles.isOn(player, HomelanderPower.SUPER_HEARING)) {
            senseMode = 2;
            radius = NewSerumConfig.SUPER_HEARING_RADIUS.get();
        } else if (SerumKit.is(player, AtlanteanPower.ID) && player.isUnderWater()) {
            senseMode = 3;
            radius = NewSerumConfig.SEA_SENSE_RADIUS.get();
        } else {
            return;
        }
        senseRadiusSqr = radius * radius;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        on = player != null && DaredevilPower.isRadarOn(player);
        updateSense(on ? null : player);
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
        boolean showing = current != null && SHADER_NAME.equals(current.getName());
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
        senseMode = 0;
        pulseAge = -1;
        Minecraft minecraft = Minecraft.getInstance();
        if (loaded && minecraft.gameRenderer != null && minecraft.gameRenderer.currentEffect() != null
                && SHADER.toString().equals(minecraft.gameRenderer.currentEffect().getName())) {
            minecraft.gameRenderer.shutdownEffect();
        }
        loaded = false;
    }
}
