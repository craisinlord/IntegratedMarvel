package com.craisinlord.integrated_superheroes.boss.command.client;

import com.craisinlord.integrated_superheroes.boss.command.StormCommandBlocks;
import com.craisinlord.integrated_superheroes.boss.command.StormCommandEditPayload;
import com.craisinlord.integrated_superheroes.boss.command.StormCommandOpenPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class StormCommandBlockScreen extends Screen {
    private static final Component SET_COMMAND_LABEL = Component.translatable("advMode.setCommand");
    private static final Component COMMAND_LABEL = Component.translatable("advMode.command");
    private static final Component PREVIOUS_OUTPUT_LABEL = Component.translatable("advMode.previousOutput");
    private static final int PREVIOUS_Y = 135;
    private final BlockPos pos;
    private final Component lastOutput;
    private String command;
    private boolean trackOutput;
    private boolean conditional;
    private boolean automatic;
    private EditBox commandEdit;
    private EditBox previousEdit;
    private CommandSuggestions suggestions;

    public StormCommandBlockScreen(StormCommandOpenPayload payload) {
        super(Component.translatable("block.integrated_superheroes.storm_command_block"));
        this.pos = payload.pos();
        this.command = payload.command();
        this.lastOutput = payload.lastOutput();
        this.trackOutput = payload.trackOutput();
        this.conditional = payload.conditional();
        this.automatic = payload.automatic();
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onDone())
                .bounds(this.width / 2 - 4 - 150, this.height / 4 + 120 + 12, 150, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
                .bounds(this.width / 2 + 4, this.height / 4 + 120 + 12, 150, 20).build());
        this.addRenderableWidget(CycleButton.booleanBuilder(Component.literal("O"), Component.literal("X"))
                .withInitialValue(this.trackOutput)
                .displayOnlyValue()
                .create(this.width / 2 + 150 - 20, PREVIOUS_Y, 20, 20, Component.translatable("advMode.trackOutput"), (button, value) -> {
                    this.trackOutput = value;
                    this.updatePreviousOutput();
                }));
        this.addRenderableWidget(CycleButton.booleanBuilder(Component.translatable("advMode.mode.conditional"), Component.translatable("advMode.mode.unconditional"))
                .displayOnlyValue()
                .withInitialValue(this.conditional)
                .create(this.width / 2 - 100 - 2, 165, 100, 20, Component.translatable("advMode.type"), (button, value) -> this.conditional = value));
        this.addRenderableWidget(CycleButton.booleanBuilder(Component.translatable("advMode.mode.autoexec.bat"), Component.translatable("advMode.mode.redstoneTriggered"))
                .displayOnlyValue()
                .withInitialValue(this.automatic)
                .create(this.width / 2 + 2, 165, 100, 20, Component.translatable("advMode.triggering"), (button, value) -> this.automatic = value));
        this.commandEdit = new EditBox(this.font, this.width / 2 - 150, 50, 300, 20, COMMAND_LABEL);
        this.commandEdit.setMaxLength(StormCommandEditPayload.MAX_COMMAND_LENGTH);
        this.commandEdit.setValue(this.command);
        this.addWidget(this.commandEdit);
        this.previousEdit = new EditBox(this.font, this.width / 2 - 150, PREVIOUS_Y, 276, 20, PREVIOUS_OUTPUT_LABEL);
        this.previousEdit.setMaxLength(StormCommandEditPayload.MAX_COMMAND_LENGTH);
        this.previousEdit.setEditable(false);
        this.addWidget(this.previousEdit);
        this.suggestions = new CommandSuggestions(this.minecraft, this, this.commandEdit, this.font, true, true, 0, 7, false, Integer.MIN_VALUE);
        this.suggestions.setAllowSuggestions(true);
        this.commandEdit.setResponder(value -> {
            this.command = value;
            this.suggestions.updateCommandInfo();
        });
        this.suggestions.updateCommandInfo();
        this.updatePreviousOutput();
    }

    private void updatePreviousOutput() {
        this.previousEdit.setValue(this.trackOutput ? this.lastOutput.getString() : "-");
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.commandEdit);
    }

    @Override
    public void tick() {
        if (this.minecraft == null || this.minecraft.level == null || !this.minecraft.level.getBlockState(this.pos).is(StormCommandBlocks.BLOCK.get())) {
            this.onClose();
        }
    }

    private void onDone() {
        PacketDistributor.sendToServer(new StormCommandEditPayload(this.pos, this.commandEdit.getValue(), this.trackOutput, this.conditional, this.automatic));
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.suggestions.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            this.onDone();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.suggestions.mouseScrolled(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.suggestions.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, SET_COMMAND_LABEL, this.width / 2, 20, 0xFFFFFF);
        graphics.drawString(this.font, COMMAND_LABEL, this.width / 2 - 150 + 1, 40, 0xA0A0A0);
        this.commandEdit.render(graphics, mouseX, mouseY, partialTick);
        if (!this.previousEdit.getValue().isEmpty()) {
            int labelY = 75 + 5 * 9 + 1 + PREVIOUS_Y - 135;
            graphics.drawString(this.font, PREVIOUS_OUTPUT_LABEL, this.width / 2 - 150 + 1, labelY + 4, 0xA0A0A0);
            this.previousEdit.render(graphics, mouseX, mouseY, partialTick);
        }
        this.suggestions.render(graphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
    }
}
