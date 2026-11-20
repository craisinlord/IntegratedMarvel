package com.craisinlord.integrated_superheroes.boss.command;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class StormCommandBlockItem extends BlockItem {
    private static final int PREVIEW_LENGTH = 40;

    public StormCommandBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.integrated_superheroes.storm_command_block").withStyle(ChatFormatting.GRAY));
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null) {
            String command = data.copyTag().getString("Command");
            if (!command.isEmpty()) {
                if (command.length() > PREVIEW_LENGTH) {
                    command = command.substring(0, PREVIEW_LENGTH) + "...";
                }
                tooltip.add(Component.translatable("tooltip.integrated_superheroes.storm_command_block.command",
                        Component.literal(command).withStyle(ChatFormatting.LIGHT_PURPLE)).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
