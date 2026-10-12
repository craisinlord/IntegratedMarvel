package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class HulkClient {
    private static final ResourceLocation VIGNETTE = ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");
    private static final ResourceLocation RAGE_SHADER = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "shaders/post/hulk_rage.json");
    private static final ResourceLocation RAGE_SHADER_HIGH = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "shaders/post/hulk_rage_high.json");
    private static final ResourceLocation BAR_BACKGROUND = ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
    private static int rage;
    private static float shown;
    private static float shownO;
    private static float warp;
    private static boolean warping;
    private static ResourceLocation loaded;
    private static int heartbeatTimer;
    private static int beatAge = 100;
    private static float shakeIntensity;
    private static int shakeTicks;
    private static int shakeTotal;
    private static int clock;

    private HulkClient() {
    }

    public static void setRage(int value) {
        rage = Mth.clamp(value, 0, 100);
        HulkPower.setClientRage(rage);
    }

    public static void shake(float intensity, int ticks) {
        float remaining = shakeTotal <= 0 ? 0.0F : shakeIntensity * shakeTicks / (float) shakeTotal;
        if (intensity >= remaining) {
            shakeIntensity = intensity;
            shakeTicks = ticks;
            shakeTotal = ticks;
        }
    }

    public static boolean isWarping() {
        return warping;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        clock++;
        if (shakeTicks > 0) {
            shakeTicks--;
        }
        if (player == null) {
            reset(minecraft);
            StormZones.clearClient();
            return;
        }
        StormZones.tickClient(player.level().getGameTime(), player.position());
        if (!HulkPower.ID.equals(HeroPowers.data(player).getPrimaryPower())) {
            rage = 0;
            HulkPower.setClientRage(0);
        }
        shownO = shown;
        shown += (rage - shown) * 0.15F;
        tickWarp(player);
        tickShader(minecraft);
        tickHeartbeat(minecraft);
    }

    private static void tickWarp(LocalPlayer player) {
        float target = rage >= 50 ? 0.1F + (rage - 50) / 50.0F * 0.35F : 0.0F;
        boolean blocked = player.hasEffect(MobEffects.CONFUSION) || player.portalProcess != null && player.portalProcess.isInsidePortalThisTick();
        if (blocked) {
            warping = false;
            warp = 0.0F;
            return;
        }
        float previous = warp;
        warp += (target - warp) * 0.1F;
        if (warp < 0.005F && target == 0.0F) {
            if (warping) {
                player.oSpinningEffectIntensity = 0.0F;
                player.spinningEffectIntensity = 0.0F;
            }
            warp = 0.0F;
            warping = false;
            return;
        }
        warping = true;
        player.oSpinningEffectIntensity = previous;
        player.spinningEffectIntensity = warp;
    }

    private static void tickShader(Minecraft minecraft) {
        if (minecraft.gameRenderer == null) {
            return;
        }
        var current = minecraft.gameRenderer.currentEffect();
        if (loaded != null && (current == null || !loaded.toString().equals(current.getName()))) {
            loaded = null;
        }
        ResourceLocation want = rage >= 85 ? RAGE_SHADER_HIGH : rage >= 50 ? RAGE_SHADER : null;
        if (want == loaded) {
            return;
        }
        if (loaded != null) {
            minecraft.gameRenderer.shutdownEffect();
            loaded = null;
        }
        if (want != null && minecraft.gameRenderer.currentEffect() == null) {
            minecraft.gameRenderer.loadEffect(want);
            loaded = want;
        }
    }

    private static void tickHeartbeat(Minecraft minecraft) {
        beatAge++;
        if (rage < 30) {
            heartbeatTimer = 0;
            return;
        }
        if (--heartbeatTimer > 0) {
            return;
        }
        heartbeatTimer = (int) Mth.lerp((rage - 30) / 70.0F, 40.0F, 15.0F);
        beatAge = 0;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 0.7F + rage / 300.0F, 0.6F + rage / 250.0F));
    }

    private static void reset(Minecraft minecraft) {
        rage = 0;
        shown = 0.0F;
        shownO = 0.0F;
        warp = 0.0F;
        warping = false;
        if (loaded != null && minecraft.gameRenderer != null && minecraft.gameRenderer.currentEffect() != null
                && loaded.toString().equals(minecraft.gameRenderer.currentEffect().getName())) {
            minecraft.gameRenderer.shutdownEffect();
        }
        loaded = null;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset(Minecraft.getInstance());
        StormZones.clearClient();
        shakeTicks = 0;
    }

    private static boolean showsMeter() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && HulkPower.ID.equals(HeroPowers.data(player).getPrimaryPower());
    }

    @SubscribeEvent
    public static void onGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        boolean bar = event.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR);
        boolean level = event.getName().equals(VanillaGuiLayers.EXPERIENCE_LEVEL);
        if (!bar && !level || !showsMeter()) {
            return;
        }
        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.gameMode == null || !minecraft.gameMode.hasExperience()) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float value = Mth.lerp(partial, shownO, shown);
        if (bar) {
            renderBar(event.getGuiGraphics(), value);
        } else {
            renderRageNumber(event.getGuiGraphics(), Math.round(value));
        }
    }

    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Post event) {
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float value = Mth.lerp(partial, shownO, shown);
        if (value >= 0.5F && event.getName().equals(VanillaGuiLayers.CAMERA_OVERLAYS)) {
            renderVignette(event.getGuiGraphics(), value / 100.0F * 0.7F);
        }
    }

    private static void renderVignette(GuiGraphics graphics, float alpha) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
        graphics.setColor(alpha, alpha, alpha, 1.0F);
        graphics.blit(VIGNETTE, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        graphics.setColor(alpha, 0.0F, 0.0F, 1.0F);
        graphics.blit(VIGNETTE, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void renderBar(GuiGraphics graphics, float value) {
        int x = graphics.guiWidth() / 2 - 91;
        int y = graphics.guiHeight() - 32 + 3;
        RenderSystem.enableBlend();
        graphics.blitSprite(BAR_BACKGROUND, x, y, 182, 5);
        int filled = Math.round(180.0F * value / 100.0F);
        if (filled > 0) {
            boolean pulse = value >= 99.5F && clock % 10 < 5;
            int top = pulse ? 0xFFFF6A5A : 0xFFE83A2A;
            int bottom = pulse ? 0xFFC81818 : 0xFF8C0E0E;
            graphics.fillGradient(x + 1, y + 1, x + 1 + filled, y + 4, top, bottom);
        }
        RenderSystem.disableBlend();
    }

    private static void renderRageNumber(GuiGraphics graphics, int value) {
        if (value <= 0) {
            return;
        }
        String text = Integer.toString(value);
        var font = Minecraft.getInstance().font;
        int x = (graphics.guiWidth() - font.width(text)) / 2;
        int y = graphics.guiHeight() - 31 - 4;
        graphics.drawString(font, text, x + 1, y, 0xFF000000, false);
        graphics.drawString(font, text, x - 1, y, 0xFF000000, false);
        graphics.drawString(font, text, x, y + 1, 0xFF000000, false);
        graphics.drawString(font, text, x, y - 1, 0xFF000000, false);
        graphics.drawString(font, text, x, y, value >= 100 ? 0xFFFF5040 : 0xFFE02020, false);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTicks <= 0 || shakeTotal <= 0) {
            return;
        }
        float scale = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        float time = clock + (float) event.getPartialTick();
        float strength = shakeIntensity * (shakeTicks / (float) shakeTotal) * scale * 2.0F;
        event.setYaw(event.getYaw() + Mth.sin(time * 2.9F) * strength);
        event.setPitch(event.getPitch() + Mth.sin(time * 3.7F + 1.3F) * strength);
        event.setRoll(event.getRoll() + Mth.sin(time * 2.3F + 2.1F) * strength * 0.8F);
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (rage < 100 || beatAge > 6) {
            return;
        }
        event.setNewFovModifier(event.getNewFovModifier() * (1.0F + 0.04F * (1.0F - beatAge / 6.0F)));
    }
}
