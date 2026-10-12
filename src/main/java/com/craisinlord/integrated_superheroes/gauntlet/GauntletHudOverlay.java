package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.gauntlet.InfinityGauntletStones;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class GauntletHudOverlay {
    private static final ResourceLocation PANEL = screen("ui_overhaul");
    private static final ResourceLocation SELECTED = screen("select_overhaul");
    private static final ResourceLocation LOCKED = screen("locked_overhaul");
    private static final int PANEL_WIDTH = 45;
    private static final int PANEL_HEIGHT = 52;
    private static final List<String> SLOT_ORDER = List.of("power", "space", "reality", "soul", "time", "mind", "snap");
    private static final int[][] SLOT_POSITIONS = {{21, 1}, {31, 1}, {21, 11}, {31, 11}, {21, 21}, {31, 21}, {26, 31}};
    private static final Map<String, int[]> ABILITY_ICONS = Map.of(
            "power", new int[]{1, 3},
            "space", new int[]{4, 6},
            "reality", new int[]{8, 9, 7},
            "soul", new int[]{11, 12, 10},
            "time", new int[]{20, 13, 13},
            "mind", new int[]{15, 15, 14},
            "snap", new int[]{19, 16}
    );

    private GauntletHudOverlay() {
    }

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "gauntlet_hud"),
                GauntletHudOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(minecraft.player);
        if (gauntlet.isEmpty()) return;
        int left = graphics.guiWidth() / 2 + 162;
        int top = graphics.guiHeight() / 2 + 59;
        String selected = InfinityGauntletStones.selectedStone(gauntlet);

        RenderSystem.enableBlend();
        graphics.blit(PANEL, left, top, 0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
        for (int index = 0; index < SLOT_ORDER.size(); index++) {
            String stone = SLOT_ORDER.get(index);
            int x = left + SLOT_POSITIONS[index][0];
            int y = top + SLOT_POSITIONS[index][1];
            if (!isUnlocked(gauntlet, stone)) {
                graphics.blit(LOCKED, x, y, 0.0F, 0.0F, 8, 8, 8, 8);
            } else if (stone.equals(selected)) {
                graphics.blit(SELECTED, x, y, 0.0F, 0.0F, 8, 8, 8, 8);
            }
        }

        int[] icons = ABILITY_ICONS.get(selected);
        if (icons == null || !isUnlocked(gauntlet, selected)) {
            RenderSystem.disableBlend();
            return;
        }
        int ability = Math.clamp(InfinityGauntletStones.selectedAbility(gauntlet, selected), 0, icons.length - 1);
        int iconX = left + 2;
        int iconY = top + 34;
        graphics.blit(screen("icon_" + icons[ability]), iconX, iconY, 0.0F, 0.0F, 16, 16, 16, 16);
        RenderSystem.disableBlend();

        float remaining = GauntletAbilityController.getAbilityCooldownPercent(minecraft.player);
        if (remaining > 0.0F) {
            int covered = Math.max(1, (int) Math.ceil(16 * remaining));
            graphics.fill(iconX, iconY + 16 - covered, iconX + 16, iconY + 16, 0x99000000);
            long seconds = (GauntletCooldownClientState.remainingMillis(minecraft.player) + 999L) / 1000L;
            graphics.drawString(minecraft.font, seconds + "s", left + 23, top + 42, 0xFF3C3C3C, false);
        }
    }

    private static boolean isUnlocked(ItemStack gauntlet, String stone) {
        if (stone.equals("snap")) {
            return InfinityGauntletStones.isFullGauntlet(gauntlet) && InfinityGauntletStones.hasAllStones(gauntlet);
        }
        return InfinityGauntletStones.hasStone(gauntlet, stone);
    }

    private static ResourceLocation screen(String name) {
        return ResourceLocation.fromNamespaceAndPath("infinity_gauntlet", "textures/screens/" + name + ".png");
    }
}
