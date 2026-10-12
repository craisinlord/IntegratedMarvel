package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DeadpoolDeathScreen extends Screen {
    private Button nah;
    private boolean ready;
    private boolean sent;

    public DeadpoolDeathScreen() {
        super(Component.translatable("deathScreen.title"));
    }

    public void ready() {
        ready = true;
        if (nah != null) {
            nah.active = !sent;
        }
    }

    @Override
    protected void init() {
        nah = addRenderableWidget(Button.builder(Component.translatable("integrated_superheroes.deadpool.screen.nah"), button -> {
            if (ready && !sent) {
                sent = true;
                button.active = false;
                PacketDistributor.sendToServer(DeadpoolStandUpPayload.INSTANCE);
            }
        }).bounds(width / 2 - 100, height / 4 + 72, 200, 20).build());
        nah.active = ready && !sent;
        Button titleScreen = addRenderableWidget(Button.builder(Component.translatable("deathScreen.titleScreen"), button -> {
        }).bounds(width / 2 - 100, height / 4 + 96, 200, 20).build());
        titleScreen.active = false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().pushPose();
        graphics.pose().scale(2.0F, 2.0F, 2.0F);
        graphics.drawCenteredString(font, title, width / 2 / 2, 30, 0xFFFFFF);
        graphics.pose().popPose();
        graphics.drawCenteredString(font, Component.translatable("integrated_superheroes.deadpool.screen.score"), width / 2, 100, 0xFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0x60500000, 0xA0803030);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
