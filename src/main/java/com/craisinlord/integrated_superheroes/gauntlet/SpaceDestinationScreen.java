package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpaceDestinationScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 206;
    private static final int FIELD_WIDTH = 74;
    private static final int FIELD_GAP = 12;
    private static final Map<String, String> KNOWN_NAMES = Map.of(
            "minecraft:overworld", "Overworld",
            "minecraft:the_nether", "The Nether",
            "minecraft:the_end", "The End");

    private final List<ResourceLocation> dimensions = new ArrayList<>();
    private int selected;
    private EditBox x;
    private EditBox y;
    private EditBox z;

    public SpaceDestinationScreen() {
        super(Component.literal("Space Stone Destination"));
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.getConnection() != null) {
            minecraft.getConnection().levels().stream()
                    .map(key -> key.location()).forEach(dimensions::add);
        }
        if (minecraft.level != null) {
            ResourceLocation current = minecraft.level.dimension().location();
            if (!dimensions.contains(current)) dimensions.add(current);
            dimensions.sort(Comparator.comparing(ResourceLocation::toString));
            selected = Math.max(0, dimensions.indexOf(current));
        }
    }

    private int left() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int top() {
        return Math.max(4, (height - PANEL_HEIGHT) / 2);
    }

    @Override
    protected void init() {
        int left = left();
        int top = top();
        int center = width / 2;
        String previousX = x == null ? Integer.toString(minecraft.player.getBlockX()) : x.getValue();
        String previousY = y == null ? Integer.toString(minecraft.player.getBlockY()) : y.getValue();
        String previousZ = z == null ? Integer.toString(minecraft.player.getBlockZ()) : z.getValue();
        int fieldsLeft = center - (FIELD_WIDTH * 3 + FIELD_GAP * 2) / 2;
        x = coordinateField(fieldsLeft, top + 98, "X", previousX);
        y = coordinateField(fieldsLeft + FIELD_WIDTH + FIELD_GAP, top + 98, "Y", previousY);
        z = coordinateField(fieldsLeft + (FIELD_WIDTH + FIELD_GAP) * 2, top + 98, "Z", previousZ);
        addRenderableWidget(Button.builder(Component.literal("‹"), button -> cycle(-1))
                .bounds(left + 12, top + 40, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("›"), button -> cycle(1))
                .bounds(left + PANEL_WIDTH - 32, top + 40, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Use my position"), button -> useMyPosition())
                .bounds(center - 60, top + 124, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Set destination"), button -> send(false))
                .bounds(left + 12, top + 154, 132, 20).build());
        addRenderableWidget(Button.builder(Component.literal(ModCompat.INFINITE_DIMENSIONS ? "Generate new dimension" : "Random dimension"), button -> send(true))
                .bounds(left + PANEL_WIDTH - 144, top + 154, 132, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(center - 40, top + 180, 80, 20).build());
    }

    private EditBox coordinateField(int fieldX, int fieldY, String name, String value) {
        EditBox field = new EditBox(font, fieldX, fieldY, FIELD_WIDTH, 20, Component.literal(name));
        field.setMaxLength(9);
        field.setFilter(text -> text.matches("-?\\d{0,8}"));
        field.setValue(value);
        addRenderableWidget(field);
        return field;
    }

    private void useMyPosition() {
        x.setValue(Integer.toString(minecraft.player.getBlockX()));
        y.setValue(Integer.toString(minecraft.player.getBlockY()));
        z.setValue(Integer.toString(minecraft.player.getBlockZ()));
    }

    private void cycle(int delta) {
        if (!dimensions.isEmpty()) selected = Math.floorMod(selected + delta, dimensions.size());
    }

    private void send(boolean generate) {
        int px = parse(x, minecraft.player.getBlockX());
        int py = parse(y, minecraft.player.getBlockY());
        int pz = parse(z, minecraft.player.getBlockZ());
        ResourceLocation dimension = dimensions.isEmpty() ? minecraft.level.dimension().location() : dimensions.get(selected);
        PacketDistributor.sendToServer(new SpaceDestinationPayload(dimension, px, py, pz, generate));
        onClose();
    }

    private int parse(EditBox field, int fallback) {
        try { return Integer.parseInt(field.getValue()); } catch (NumberFormatException ignored) { return fallback; }
    }

    private static String friendlyName(ResourceLocation id) {
        String known = KNOWN_NAMES.get(id.toString());
        if (known != null) return known;
        String key = "dimension." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        if (I18n.exists(key)) return I18n.get(key);
        String path = id.getPath();
        String[] words = path.substring(path.lastIndexOf('/') + 1).split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!name.isEmpty()) name.append(' ');
            name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return name.isEmpty() ? id.toString() : name.toString();
    }

    private String fit(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, maxWidth - font.width("…")) + "…";
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = left();
        int top = top();
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xD0101018);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF6A4FB0);
        graphics.fill(left + 12, top + 24, left + PANEL_WIDTH - 12, top + 25, 0x806A4FB0);
        graphics.fill(left + 12, top + 148, left + PANEL_WIDTH - 12, top + 149, 0x806A4FB0);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = left();
        int top = top();
        int center = width / 2;
        int nameWidth = PANEL_WIDTH - 84;
        graphics.drawCenteredString(font, title, center, top + 9, 0xFFFFFF);
        graphics.drawString(font, "Dimension", left + 12, top + 30, 0x9A9AB0, false);
        if (dimensions.isEmpty()) {
            graphics.drawCenteredString(font, "No dimensions available", center, top + 46, 0xAADDFF);
        } else {
            ResourceLocation dimension = dimensions.get(selected);
            graphics.drawCenteredString(font, fit(friendlyName(dimension), nameWidth), center, top + 41, 0xAADDFF);
            graphics.pose().pushPose();
            graphics.pose().translate(center, top + 53, 0.0F);
            graphics.pose().scale(0.75F, 0.75F, 1.0F);
            graphics.drawCenteredString(font, fit(dimension.toString(), (int) (nameWidth / 0.75F)), 0, 0, 0x8080A0);
            graphics.pose().popPose();
        }
        graphics.drawString(font, "Arrival coordinates", left + 12, top + 72, 0x9A9AB0, false);
        drawFieldLabel(graphics, x, "X");
        drawFieldLabel(graphics, y, "Y");
        drawFieldLabel(graphics, z, "Z");
    }

    private void drawFieldLabel(GuiGraphics graphics, EditBox field, String label) {
        graphics.drawCenteredString(font, label, field.getX() + field.getWidth() / 2, field.getY() - 11, 0xCCCCCC);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
