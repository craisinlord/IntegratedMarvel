package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class KittyClient {
    private static final ResourceLocation BAR_BACKGROUND = ResourceLocation.withDefaultNamespace("hud/experience_bar_background");

    private KittyClient() {
    }

    private static boolean showsMeter() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        String primary = HeroPowers.data(player).getPrimaryPower();
        return KittyPower.ID.equals(primary)
                || (KryptonianPower.ID.equals(primary) || AtlanteanPower.ID.equals(primary)) && KittyPower.clientMeter() >= 0;
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
        if (!bar || minecraft.options.hideGui || minecraft.gameMode == null || !minecraft.gameMode.hasExperience()) {
            return;
        }
        renderBar(event.getGuiGraphics(), KittyPower.clientMeter());
    }

    private static void renderBar(GuiGraphics graphics, int value) {
        int x = graphics.guiWidth() / 2 - 91;
        int y = graphics.guiHeight() - 32 + 3;
        RenderSystem.enableBlend();
        graphics.blitSprite(BAR_BACKGROUND, x, y, 182, 5);
        int filled = Math.round(180.0F * value / 100.0F);
        if (filled > 0) {
            LocalPlayer player = Minecraft.getInstance().player;
            String primary = HeroPowers.data(player).getPrimaryPower();
            int top;
            int bottom;
            if (KryptonianPower.ID.equals(primary)) {
                top = value >= 100 ? 0xFFFFFF : 0xFFF2A0;
                bottom = 0xFFB020;
            } else if (AtlanteanPower.ID.equals(primary)) {
                top = 0x9FE6FF;
                bottom = 0x2A7FD8;
            } else {
                top = com.craisinlord.integrated_superheroes.hero.skin.SerumSkins.secondary(player, 0xE6D2FF);
                bottom = com.craisinlord.integrated_superheroes.hero.skin.SerumSkins.primary(player, 0xA07CD8);
            }
            graphics.fillGradient(x + 1, y + 1, x + 1 + filled, y + 4, 0xFF000000 | top, 0xFF000000 | bottom);
        }
        RenderSystem.disableBlend();
    }
}
