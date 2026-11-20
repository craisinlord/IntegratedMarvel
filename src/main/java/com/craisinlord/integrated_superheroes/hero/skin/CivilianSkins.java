package com.craisinlord.integrated_superheroes.hero.skin;

import com.craisinlord.integrated_superheroes.cleanup.CatalogCleanup;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicLookGroups;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CivilianSkins {
    private static final String KEY = "commands.integrated_superheroes.civilianskin.";
    private static Map<String, String> looks;

    private CivilianSkins() {
    }

    public static synchronized Map<String, String> looks() {
        if (looks == null) {
            Map<String, String> found = new LinkedHashMap<>();
            if (ModCompat.THEMATIC) {
                List<ResourceLocation> ids = new ArrayList<>(BuiltInRegistries.ITEM.keySet());
                ids.sort(ResourceLocation::compareTo);
                for (ResourceLocation id : ids) {
                    if ("thematic".equals(id.getNamespace()) && CatalogCleanup.civilianLook(id) && ThematicLookGroups.isSuit(id.toString())) {
                        found.put(id.getPath(), id.toString());
                    }
                }
            }
            looks = java.util.Collections.unmodifiableMap(found);
        }
        return looks;
    }

    public static boolean valid(String costume) {
        return costume != null && !costume.isEmpty() && looks().containsValue(costume);
    }

    public static Optional<String> byId(String id) {
        return Optional.ofNullable(looks().get(id));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("civilianskin")
                .executes(context -> list(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("look", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            List<String> options = new ArrayList<>(looks().keySet());
                            options.add(SkinData.OFF);
                            return SharedSuggestionProvider.suggest(options, builder);
                        })
                        .executes(context -> pick(context.getSource(), List.of(context.getSource().getPlayerOrException()),
                                StringArgumentType.getString(context, "look")))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> pick(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                        StringArgumentType.getString(context, "look"))))));
    }

    private static int list(CommandSourceStack source, ServerPlayer player) {
        if (looks().isEmpty()) {
            source.sendFailure(Component.translatable(KEY + "none"));
            return 0;
        }
        String current = SkinData.civilian(player);
        MutableComponent options = Component.empty();
        boolean first = true;
        List<String> ids = new ArrayList<>(looks().keySet());
        ids.add(SkinData.OFF);
        for (String id : ids) {
            if (!first) {
                options.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            }
            first = false;
            String costume = looks().get(id);
            boolean worn = costume == null ? current.isEmpty() : costume.equals(current);
            Component hover = costume == null ? Component.translatable(KEY + "hover_off") : SerumLooks.name(costume);
            options.append(Component.literal(id).withStyle(style -> style
                    .withColor(worn ? ChatFormatting.GREEN : ChatFormatting.YELLOW)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/civilianskin " + id))));
        }
        source.sendSuccess(() -> Component.translatable(KEY + "list", options), false);
        return 1;
    }

    private static int pick(CommandSourceStack source, Collection<ServerPlayer> targets, String id) {
        boolean off = SkinData.OFF.equals(id);
        Optional<String> costume = byId(id);
        if (!off && costume.isEmpty()) {
            source.sendFailure(Component.translatable(KEY + "unknown", id));
            return 0;
        }
        int changed = 0;
        for (ServerPlayer target : targets) {
            boolean self = target == source.getEntity();
            SkinData.setCivilian(target, costume.orElse(""));
            Component message = off ? Component.translatable(KEY + "off") : Component.translatable(KEY + "set", SerumLooks.name(costume.get()));
            target.sendSystemMessage(message);
            if (!self) {
                source.sendSuccess(() -> off ? Component.translatable(KEY + "off_other", target.getDisplayName())
                        : Component.translatable(KEY + "set_other", target.getDisplayName(), SerumLooks.name(costume.get())), true);
            }
            changed++;
        }
        return changed;
    }
}
