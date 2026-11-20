package com.craisinlord.integrated_superheroes.compat.thematic;

import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.CommandEvent;

import java.util.List;

final class ThematicStarters {
    private ThematicStarters() {
    }

    static void onCommand(CommandEvent event) {
        if (!ThematicOptions.on(ThematicOptions.DISABLE_STARTERS)) {
            return;
        }
        CommandContextBuilder<CommandSourceStack> context = event.getParseResults().getContext();
        List<ParsedCommandNode<CommandSourceStack>> nodes = context.getNodes();
        if (nodes.size() < 2 || !"thematic".equals(nodes.get(0).getNode().getName()) || !"starter".equals(nodes.get(1).getNode().getName())) {
            return;
        }
        CommandSourceStack source = context.getSource();
        if (source.hasPermission(2)) {
            return;
        }
        event.setCanceled(true);
        source.sendFailure(Component.translatable("commands.integrated_superheroes.starters_off"));
    }
}
