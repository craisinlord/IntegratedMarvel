package com.craisinlord.integrated_superheroes.voidworld.client;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.voidworld.VoidWorld;
import com.craisinlord.integrated_superheroes.voidworld.client.sky.TimelinePalette;
import com.craisinlord.integrated_superheroes.voidworld.client.sky.TimelineSky;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

public final class VoidSkyClient {
    private static final float[][] SHADES = {
            {0x8E / 255F, 0x86 / 255F, 0xA2 / 255F},
            {0xA3 / 255F, 0x9C / 255F, 0xB3 / 255F},
            {0x84 / 255F, 0x8C / 255F, 0x94 / 255F}
    };
    private static final float NEAR = 24F;
    private static final float FAR = 160F;
    private static final float STORM_FAR = 56F;
    private static final double ABYSS_TOP = 56.0;
    private static final double ABYSS_BOTTOM = 24.0;
    private static final double DRIFT_SCALE = 1.0 / 900.0;
    private static final float HUE_NUDGE = 0.08F;
    private static final float LIGHTNING_LIFT = 0.07F;
    private static final float[] HUE = new float[3];
    private static final float[] FLASH = new float[3];
    private static final float[] RGB = new float[3];

    private static volatile float stormPressure;

    private VoidSkyClient() {
    }

    public static void setStormPressure(float value) {
        stormPressure = Float.isFinite(value) ? Mth.clamp(value, 0F, 1F) : 0F;
    }

    public static float stormPressure() {
        return stormPressure;
    }

    private static boolean inVoid() {
        return VoidWorld.isVoid(Minecraft.getInstance().level);
    }

    private static float[] colour(Vec3 camera) {
        double phase = (Math.sin(camera.x * DRIFT_SCALE) + Math.cos(camera.z * DRIFT_SCALE * 1.3)) * 0.5 + 0.5;
        double scaled = Mth.clamp(phase, 0.0, 0.9999) * (SHADES.length - 1);
        int index = (int) scaled;
        float t = (float) (scaled - index);
        float[] a = SHADES[index];
        float[] b = SHADES[index + 1];
        float r = Mth.lerp(t, a[0], b[0]);
        float g = Mth.lerp(t, a[1], b[1]);
        float bl = Mth.lerp(t, a[2], b[2]);
        TimelinePalette.hue(TimelinePalette.level(camera.x, camera.z), HUE);
        float lum = (r + g + bl) / 3F * 1.25F;
        r += (HUE[0] * lum - r) * HUE_NUDGE;
        g += (HUE[1] * lum - g) * HUE_NUDGE;
        bl += (HUE[2] * lum - bl) * HUE_NUDGE;
        float dark = 1F - 0.72F * stormPressure;
        float abyss = (float) Mth.clamp((camera.y - ABYSS_BOTTOM) / (ABYSS_TOP - ABYSS_BOTTOM), 0.0, 1.0);
        float scale = dark * (0.12F + 0.88F * abyss);
        float flash = TimelineSky.lightning(FLASH) * LIGHTNING_LIFT * (0.3F + 0.7F * abyss);
        RGB[0] = r * scale + FLASH[0] * flash;
        RGB[1] = g * scale + FLASH[1] * flash;
        RGB[2] = bl * scale + FLASH[2] * flash;
        return RGB;
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
    public static final class GameEvents {
        private GameEvents() {
        }

        @SubscribeEvent
        public static void colour(ViewportEvent.ComputeFogColor event) {
            if (!inVoid()) {
                return;
            }
            float[] rgb = VoidSkyClient.colour(event.getCamera().getPosition());
            event.setRed(rgb[0]);
            event.setGreen(rgb[1]);
            event.setBlue(rgb[2]);
        }

        @SubscribeEvent
        public static void distance(ViewportEvent.RenderFog event) {
            if (!inVoid() || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
                return;
            }
            float far = Mth.lerp(stormPressure, FAR, STORM_FAR);
            float near = Math.min(NEAR, far * 0.5F);
            event.setNearPlaneDistance(Math.min(near, event.getFarPlaneDistance()));
            event.setFarPlaneDistance(Math.min(far, event.getFarPlaneDistance()));
            event.setFogShape(FogShape.SPHERE);
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void strands(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || !inVoid()) {
                return;
            }
            TimelineSky.renderStrands(event.getModelViewMatrix(), event.getProjectionMatrix(), event.getCamera());
        }

        @SubscribeEvent
        public static void tick(ClientTickEvent.Post event) {
            TimelineSky.clientTick();
        }
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void effects(RegisterDimensionSpecialEffectsEvent event) {
            event.register(IntegratedSuperheroes.id("the_void"),
                    new DimensionSpecialEffects(Float.NaN, false, DimensionSpecialEffects.SkyType.NONE, false, false) {
                        @Override
                        public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
                            return color;
                        }

                        @Override
                        public boolean isFoggyAt(int x, int y) {
                            return false;
                        }

                        @Override
                        public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                                                 Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
                            return TimelineSky.renderBackdrop(modelViewMatrix, projectionMatrix, camera);
                        }
                    });
        }
    }
}
