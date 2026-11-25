package com.craisinlord.integrated_superheroes.boss.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class StormCommandFilter {
    private StormCommandFilter() {
    }

    @Nullable
    public static Component blocked(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, String command) {
        String text = command.toLowerCase(Locale.ROOT);
        for (String fragment : StormCommandBlockConfig.blockedText()) {
            if (text.contains(fragment)) {
                return Component.translatable("message.integrated_superheroes.storm_command_block.blocked_text", fragment);
            }
        }
        String name = blockedCommand(dispatcher, source, command);
        return name == null ? null : Component.translatable("message.integrated_superheroes.storm_command_block.blocked", name);
    }

    @Nullable
    private static String blockedCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, String command) {
        StringReader reader = new StringReader(command);
        if (reader.canRead() && reader.peek() == '/') {
            reader.skip();
        }
        Set<String> blocked = StormCommandBlockConfig.blockedCommands();
        String first = firstWord(reader.getRemaining());
        if (blocked.contains(first)) {
            return first;
        }
        ParseResults<CommandSourceStack> parse = dispatcher.parse(reader, source);
        CommandNode<CommandSourceStack> root = dispatcher.getRoot();
        CommandNode<CommandSourceStack> execute = root.getChild("execute");
        for (CommandContextBuilder<CommandSourceStack> context = parse.getContext(); context != null; context = context.getChild()) {
            List<ParsedCommandNode<CommandSourceStack>> nodes = context.getNodes();
            if (nodes.isEmpty()) {
                continue;
            }
            CommandNode<CommandSourceStack> start = context.getRootNode();
            int subcommandIndex = -1;
            if (start == root) {
                String name = nodes.get(0).getNode().getName().toLowerCase(Locale.ROOT);
                if (blocked.contains(name)) {
                    return name;
                }
                if (nodes.get(0).getNode() == execute) {
                    subcommandIndex = 1;
                }
            } else if (start == execute) {
                subcommandIndex = 0;
            }
            if (subcommandIndex >= 0 && subcommandIndex < nodes.size()) {
                String sub = nodes.get(subcommandIndex).getNode().getName();
                if (blocked.contains(sub)) {
                    return "execute " + sub;
                }
                if ((sub.equals("if") || sub.equals("unless")) && subcommandIndex + 1 < nodes.size()) {
                    String test = nodes.get(subcommandIndex + 1).getNode().getName();
                    if (blocked.contains(test)) {
                        return "execute " + sub + " " + test;
                    }
                }
            }
        }
        return null;
    }

    private static String firstWord(String command) {
        String word = command.strip();
        int space = word.indexOf(' ');
        return (space < 0 ? word : word.substring(0, space)).toLowerCase(Locale.ROOT);
    }
}
