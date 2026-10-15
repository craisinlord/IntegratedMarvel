package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.gauntlet.InfinityGauntletStones;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class GauntletSelectionToast {
    private static final long DISPLAY_MILLIS = 2600L;
    private static final int BACKGROUND = 0xA8000000;
    private static Component heading = Component.empty();
    private static Component description = Component.empty();
    private static long expiresAt;

    private GauntletSelectionToast() {
    }

    public static void show(String stone, int ability) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !InfinityGauntletStones.isKnownStone(stone)) {
            return;
        }
        int color = GauntletAbilities.numeralColor(ability);
        String roman = GauntletAbilities.numeral(ability);
        MutableComponent title = Component.literal(roman + " ")
                .withStyle(Style.EMPTY.withColor(color).withBold(true))
                .append(Component.translatable("tooltip.integrated_superheroes.gauntlet.stone." + stone)
                        .withStyle(Style.EMPTY.withColor(0xFFFFFF)))
                .append(Component.literal(" · ").withStyle(Style.EMPTY.withColor(0x888888)))
                .append(Component.translatable("tooltip.integrated_superheroes.gauntlet.ability." + stone + "." + (ability + 1))
                        .withStyle(Style.EMPTY.withColor(color).withBold(true)));
        heading = title;
        description = Component.translatable("tooltip.integrated_superheroes.gauntlet.description." + stone + "." + (ability + 1));
        expiresAt = System.currentTimeMillis() + DISPLAY_MILLIS;
    }

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "gauntlet_selection_toast"),
                GauntletSelectionToast::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || System.currentTimeMillis() >= expiresAt) {
            return;
        }
        int scaleWidth = Math.max(minecraft.font.width(heading), minecraft.font.width(description));
        float scale = 0.75F;
        int boxWidth = (int) (scaleWidth * scale) + 12;
        int boxHeight = (int) ((minecraft.font.lineHeight * 2 + 3) * scale) + 8;
        int left = (graphics.guiWidth() - boxWidth) / 2;
        int statusHeight = Math.max(minecraft.gui.leftHeight, minecraft.gui.rightHeight);
        int actionBarTop = graphics.guiHeight() - Math.max(statusHeight + 9, 68) - 6;
        int top = Math.max(2, actionBarTop - boxHeight);
        graphics.fill(left, top, left + boxWidth, top + boxHeight, BACKGROUND);
        graphics.pose().pushPose();
        graphics.pose().translate(graphics.guiWidth() / 2.0F, top + 4.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(minecraft.font, heading, -minecraft.font.width(heading) / 2, 0, 0xFFFFFF, true);
        graphics.drawString(minecraft.font, description, -minecraft.font.width(description) / 2,
                minecraft.font.lineHeight + 2, 0xDDDDDD, true);
        graphics.pose().popPose();
    }
}
