package com.craisinlord.integrated_marvel.gauntlet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SpaceDestinationScreen extends Screen {
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

    @Override
    protected void init() {
        int center = width / 2;
        x = new EditBox(font, center - 105, height / 2 - 12, 62, 20, Component.literal("X"));
        y = new EditBox(font, center - 31, height / 2 - 12, 62, 20, Component.literal("Y"));
        z = new EditBox(font, center + 43, height / 2 - 12, 62, 20, Component.literal("Z"));
        x.setValue(Integer.toString((int) minecraft.player.getX()));
        y.setValue(Integer.toString((int) minecraft.player.getY()));
        z.setValue(Integer.toString((int) minecraft.player.getZ()));
        addRenderableWidget(x);
        addRenderableWidget(y);
        addRenderableWidget(z);
        addRenderableWidget(Button.builder(Component.literal("‹"), button -> cycle(-1)).bounds(center - 130, height / 2 - 54, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("›"), button -> cycle(1)).bounds(center + 90, height / 2 - 54, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Set destination"), button -> send(false)).bounds(center - 120, height / 2 + 22, 115, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Generate new dimension"), button -> send(true)).bounds(center + 5, height / 2 + 22, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose()).bounds(center - 35, height / 2 + 52, 70, 20).build());
    }

    private void cycle(int delta) {
        if (!dimensions.isEmpty()) selected = Math.floorMod(selected + delta, dimensions.size());
    }

    private void send(boolean generate) {
        int px = parse(x, (int) minecraft.player.getX());
        int py = parse(y, (int) minecraft.player.getY());
        int pz = parse(z, (int) minecraft.player.getZ());
        ResourceLocation dimension = dimensions.isEmpty() ? minecraft.level.dimension().location() : dimensions.get(selected);
        PacketDistributor.sendToServer(new SpaceDestinationPayload(dimension, px, py, pz, generate));
        onClose();
    }

    private int parse(EditBox field, int fallback) {
        try { return Integer.parseInt(field.getValue()); } catch (NumberFormatException ignored) { return fallback; }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 82, 0xFFFFFF);
        graphics.drawCenteredString(font, dimensions.isEmpty() ? "No dimensions available" : dimensions.get(selected).toString(), width / 2, height / 2 - 48, 0xAADDFF);
        graphics.drawCenteredString(font, "Select a dimension and exact arrival coordinates", width / 2, height / 2 + 2, 0xCCCCCC);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
