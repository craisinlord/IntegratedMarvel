package com.craisinlord.integrated_superheroes.compat.thematic;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

final class ThematicRaidCommand {
    private static final List<String> CREWS = List.of("penguin", "two_face");

    private ThematicRaidCommand() {
    }

    static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("heroraid").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> run(context, null))
                        .then(Commands.argument("crew", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(CREWS, builder))
                                .executes(context -> run(context, StringArgumentType.getString(context, "crew"))))));
    }

    private static int run(CommandContext<CommandSourceStack> context, String crew) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        if (crew != null && !CREWS.contains(crew)) {
            context.getSource().sendFailure(Component.literal("Unknown crew: " + crew));
            return 0;
        }
        if (!ThematicVillains.startCrew(player.serverLevel(), player, crew)) {
            context.getSource().sendFailure(Component.literal("Could not start the raid"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Started a raid on " + player.getGameProfile().getName()), true);
        return 1;
    }
}
