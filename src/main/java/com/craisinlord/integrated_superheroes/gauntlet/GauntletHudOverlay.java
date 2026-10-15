package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class GauntletHudOverlay {
    private static final ResourceLocation PANEL = screen("ui_overhaul");
    private static final ResourceLocation SELECTED = screen("select_overhaul");
    private static final ResourceLocation LOCKED = screen("locked_overhaul");
    private static final int PANEL_WIDTH = 45;
    private static final int PANEL_HEIGHT = 52;
    private static final int[][] SLOT_POSITIONS = {{21, 1}, {31, 1}, {21, 11}, {31, 11}, {21, 21}, {31, 21}, {26, 31}};
    private static final float TEXT_SCALE = 0.6F;
    private static final float MIN_TEXT_SCALE = 0.4F;
    private static final int WRAP_WIDTH = 200;
    private static final String PREFIX = "tooltip.integrated_superheroes.gauntlet.";
    private static CardLayout cachedLayout;

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
        for (int index = 0; index < GauntletAbilities.STONES.size(); index++) {
            String stone = GauntletAbilities.STONES.get(index);
            int x = left + SLOT_POSITIONS[index][0];
            int y = top + SLOT_POSITIONS[index][1];
            if (!GauntletAbilities.unlocked(gauntlet, stone)) {
                graphics.blit(LOCKED, x, y, 0.0F, 0.0F, 8, 8, 8, 8);
            } else if (stone.equals(selected)) {
                graphics.blit(SELECTED, x, y, 0.0F, 0.0F, 8, 8, 8, 8);
            }
        }

        GauntletAbility ability = GauntletAbilities.selected(gauntlet);
        if (ability == null) {
            RenderSystem.disableBlend();
            return;
        }
        int iconX = left + 2;
        int iconY = top + 34;
        drawIcon(graphics, ability.icon(), iconX, iconY);
        RenderSystem.disableBlend();

        float remaining = GauntletAbilityController.getAbilityCooldownPercent(minecraft.player);
        if (remaining > 0.0F) {
            int covered = Math.max(1, (int) Math.ceil(16 * remaining));
            graphics.fill(iconX, iconY + 16 - covered, iconX + 16, iconY + 16, 0x99000000);
            long seconds = (GauntletCooldownClientState.remainingMillis(minecraft.player) + 999L) / 1000L;
            graphics.drawString(minecraft.font, seconds + "s", left + 23, top + 42, 0xFF3C3C3C, false);
        }
        drawCard(graphics, minecraft, gauntlet, ability, left + PANEL_WIDTH, top + PANEL_HEIGHT + 2);
    }

    private static void drawIcon(GuiGraphics graphics, int icon, int x, int y) {
        if (icon >= 1000) {
            graphics.blit(screen("icon_" + (icon - 1000)), x, y, 16, 16, 16.0F, 0.0F, -16, 16, 16, 16);
        } else if (icon < 0) {
            graphics.blit(screen("icon_" + -icon), x, y, 16, 16, 0.0F, 16.0F, 16, -16, 16, 16);
        } else {
            graphics.blit(screen("icon_" + icon), x, y, 0.0F, 0.0F, 16, 16, 16, 16);
        }
    }

    private static void drawCard(GuiGraphics graphics, Minecraft minecraft, ItemStack gauntlet, GauntletAbility ability,
                                 int right, int top) {
        GauntletClientConfig.CardMode mode = GauntletClientConfig.cardMode();
        boolean held = minecraft.screen == null && GauntletKeyMappings.ABILITY_INFO.isDown();
        if (mode == GauntletClientConfig.CardMode.OFF && !held) return;
        boolean expanded = held || mode == GauntletClientConfig.CardMode.FULL;
        boolean infinity = GauntletAbilities.infinity(gauntlet);
        boolean scorched = minecraft.level != null && SnapStone.isScorched(gauntlet, minecraft.level.getGameTime());
        CardKey key = new CardKey(ability, GauntletClientState.hud(), expanded, infinity, scorched, graphics.guiWidth(),
                graphics.guiHeight(), minecraft.getWindow().getGuiScale(), right, top,
                minecraft.getLanguageManager().getSelected(), GauntletKeyMappings.ABILITY_INFO.saveString());
        CardLayout layout = cachedLayout;
        if (layout == null || !layout.key().equals(key)) {
            layout = layoutCard(minecraft, ability, key);
            cachedLayout = layout;
        }
        float scale = layout.scale();
        List<FormattedCharSequence> rows = layout.rows();
        List<Integer> colors = layout.colors();
        int width = layout.width();
        int lineHeight = minecraft.font.lineHeight + 1;
        int screenRight = graphics.guiWidth() - 2;
        int cardRight = Math.min(right, screenRight);
        int scaledRight = (int) (cardRight / scale);
        int scaledTop = (int) (top / scale) + 2;
        int height = rows.size() * lineHeight;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.fill(scaledRight - width - 3, scaledTop - 2, scaledRight + 2, scaledTop + height, 0x90000000);
        for (int index = 0; index < rows.size(); index++) {
            FormattedCharSequence row = rows.get(index);
            graphics.drawString(minecraft.font, row, scaledRight - layout.widths().get(index), scaledTop + index * lineHeight,
                    colors.get(index), true);
        }
        graphics.pose().popPose();
    }

    private static CardLayout layoutCard(Minecraft minecraft, GauntletAbility ability, CardKey key) {
        String suffix = ability.translationSuffix();
        List<CardLine> lines = new ArrayList<>();
        lines.add(new CardLine(Component.literal(GauntletAbilities.numeral(ability.index()) + " ")
                .append(Component.translatable(PREFIX + "ability." + suffix)),
                0xFF000000 | GauntletAbilities.stoneColor(ability.stone())));
        control(lines, "use", PREFIX + "use." + suffix, true);
        control(lines, "crouch", PREFIX + "crouch." + suffix, ability.crouchVariant());
        control(lines, "scroll", PREFIX + "scroll." + suffix, ability.scroll() != GauntletAbility.Scroll.NONE);
        Component status = status(ability);
        if (status != null) lines.add(new CardLine(status, 0xFFFFE080));
        if (key.infinity()) {
            lines.add(new CardLine(Component.translatable("hud.integrated_superheroes.gauntlet.infinity"), 0xFFFFD700));
        }
        if (key.scorched()) {
            lines.add(new CardLine(Component.translatable("hud.integrated_superheroes.gauntlet.scorched"), 0xFF999999));
        }
        if (key.expanded()) {
            lines.add(new CardLine(Component.translatable(PREFIX + "description." + suffix), 0xFFBBBBBB));
        } else {
            lines.add(new CardLine(Component.translatable("hud.integrated_superheroes.gauntlet.details",
                    GauntletKeyMappings.ABILITY_INFO.getTranslatedKeyMessage()), 0xFF808080));
        }
        int screenRight = key.guiWidth() - 2;
        int cardRight = Math.min(key.right(), screenRight);
        int hotbarRight = key.guiWidth() / 2 + 95;
        int availableWidth = Math.max(60, cardRight - hotbarRight);
        int availableHeight = key.guiHeight() - 2 - key.top();
        int lineHeight = minecraft.font.lineHeight + 1;
        float minScale = (float) Math.max(MIN_TEXT_SCALE, 1.0D / key.guiScale());
        float scale = TEXT_SCALE;
        List<FormattedCharSequence> rows = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        while (true) {
            rows.clear();
            colors.clear();
            int wrap = Math.min(WRAP_WIDTH, (int) (availableWidth / scale));
            for (CardLine line : lines) {
                for (FormattedCharSequence row : minecraft.font.split(line.text(), wrap)) {
                    rows.add(row);
                    colors.add(line.color());
                }
            }
            if ((rows.size() * lineHeight + 4) * scale <= availableHeight || scale <= minScale) break;
            scale = Math.max(minScale, scale - 0.05F);
        }
        int maxRows = Math.max(1, (int) ((availableHeight / scale - 4) / lineHeight));
        while (rows.size() > maxRows) {
            rows.removeLast();
            colors.removeLast();
        }
        int width = 0;
        List<Integer> widths = new ArrayList<>(rows.size());
        for (FormattedCharSequence row : rows) {
            int rowWidth = minecraft.font.width(row);
            widths.add(rowWidth);
            width = Math.max(width, rowWidth);
        }
        return new CardLayout(key, scale, List.copyOf(rows), List.copyOf(colors), List.copyOf(widths), width);
    }

    private static void control(List<CardLine> lines, String label, String key, boolean present) {
        if (!present || !I18n.exists(key)) return;
        lines.add(new CardLine(Component.translatable("hud.integrated_superheroes.gauntlet." + label)
                .withStyle(style -> style.withColor(0x9A9A9A))
                .append(Component.translatable(key).withStyle(style -> style.withColor(0xFFFFFF))), 0xFFFFFFFF));
    }

    private static Component status(GauntletAbility ability) {
        GauntletHudPayload hud = GauntletClientState.hud();
        return switch (ability.id()) {
            case "realityClipboard" -> {
                if (hud.clipLabel().isEmpty()) yield Component.translatable("hud.integrated_superheroes.gauntlet.clipboard_empty");
                if (hud.clipLabel().equals("region")) {
                    yield Component.translatable("hud.integrated_superheroes.gauntlet.clipboard_region", hud.clipCount(),
                            hud.clipSize().get(0), hud.clipSize().get(1), hud.clipSize().get(2), hud.clipRotation() * 90);
                }
                yield Component.translatable("hud.integrated_superheroes.gauntlet.clipboard", Component.translatable(hud.clipLabel()));
            }
            case "transmute" -> Component.translatable("hud.integrated_superheroes.gauntlet.radius", hud.realityRadius());
            case "timeField" -> Component.translatable("hud.integrated_superheroes.gauntlet.time_rate", hud.timeRate());
            case "rewind" -> Component.translatable("hud.integrated_superheroes.gauntlet.rewind",
                    bar(hud.rewindFill()), Math.round(hud.rewindFill() * 100.0F));
            default -> null;
        };
    }

    private record CardLine(Component text, int color) {
    }

    private record CardKey(GauntletAbility ability, GauntletHudPayload hud, boolean expanded, boolean infinity, boolean scorched,
                           int guiWidth, int guiHeight, double guiScale, int right, int top, String language, String infoKey) {
    }

    private record CardLayout(CardKey key, float scale, List<FormattedCharSequence> rows, List<Integer> colors, List<Integer> widths,
                              int width) {
    }

    private static String bar(float fill) {
        int filled = Math.round(fill * 10.0F);
        return "|".repeat(filled) + ".".repeat(10 - filled);
    }

    private static ResourceLocation screen(String name) {
        return ResourceLocation.fromNamespaceAndPath("infinity_gauntlet", "textures/screens/" + name + ".png");
    }
}
