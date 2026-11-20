package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class WingedFlightClient {
    private static final ResourceLocation BAR_BACKGROUND = ResourceLocation.withDefaultNamespace("hud/experience_bar_background");

    private WingedFlightClient() {
    }

    private static WingedFlightPayload own(LocalPlayer player) {
        if (player == null || !AngelPower.isAngel(player)) {
            return null;
        }
        return WingedFlightPayload.client(player.getId());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        WingedFlightPayload state = own(player);
        if (state == null || state.mode() != WingedFlightPayload.GLIDE || player.onGround() || player.getAbilities().flying
                || player.isInWater() || player.isInLava() || player.isFallFlying() || player.isPassenger() || player.isShiftKeyDown()
                || player.onClimbable()) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        Vec3 look = player.getLookAngle();
        double dive = Math.max(0.0D, -look.y);
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize();
        double speed = AngelConfig.GLIDE_SPEED.get() * (1.0D + dive);
        double x = Mth.lerp(0.1D, motion.x, flat.x * speed);
        double z = Mth.lerp(0.1D, motion.z, flat.z * speed);
        double sink = AngelConfig.GLIDE_SINK.get() + dive * 0.5D;
        double y = motion.y > 0.0D ? motion.y : Math.max(motion.y, -sink);
        player.setDeltaMovement(x, y, z);
    }

    @SubscribeEvent
    public static void onGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        boolean bar = event.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR);
        boolean level = event.getName().equals(VanillaGuiLayers.EXPERIENCE_LEVEL);
        if (!bar && !level) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        WingedFlightPayload state = own(minecraft.player);
        if (state == null) {
            return;
        }
        event.setCanceled(true);
        if (!bar || minecraft.options.hideGui || minecraft.gameMode == null || !minecraft.gameMode.hasExperience()) {
            return;
        }
        renderBar(event.getGuiGraphics(), state.lift());
    }

    private static void renderBar(GuiGraphics graphics, int value) {
        int x = graphics.guiWidth() / 2 - 91;
        int y = graphics.guiHeight() - 32 + 3;
        RenderSystem.enableBlend();
        graphics.blitSprite(BAR_BACKGROUND, x, y, 182, 5);
        int filled = Math.round(180.0F * Mth.clamp(value, 0, 100) / 100.0F);
        if (filled > 0) {
            int top = value >= 100 ? 0xFFFFFF : 0xF2F6FF;
            int bottom = value <= 20 ? 0xD86A5A : 0x8FB8E8;
            graphics.fillGradient(x + 1, y + 1, x + 1 + filled, y + 4, 0xFF000000 | top, 0xFF000000 | bottom);
        }
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WingedFlightPayload.clearClient();
        AngelWingLayer.clearPoses();
    }
}
