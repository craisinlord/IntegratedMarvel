package com.craisinlord.integrated_superheroes.hero.skin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class SkinCommand {
    private static final String KEY = "commands.integrated_superheroes.heroskin.";

    private SkinCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("heroskin")
                .executes(context -> list(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("skin", StringArgumentType.word())
                        .suggests(SkinCommand::suggest)
                        .executes(context -> pick(context.getSource(), List.of(context.getSource().getPlayerOrException()),
                                StringArgumentType.getString(context, "skin")))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> pick(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                        StringArgumentType.getString(context, "skin"))))));
    }

    private static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> options = new ArrayList<>();
        options.add(SkinData.DEFAULT);
        options.add(SkinData.OFF);
        ServerPlayer player = context.getSource().getEntity() instanceof ServerPlayer found ? found : null;
        String power = player == null ? "" : SkinData.power(player);
        Collection<SerumSkin> skins = power.isEmpty() ? SerumSkins.all() : SerumSkins.forPower(power);
        skins.forEach(skin -> options.add(skin.id()));
        SerumLooks.forPower(power).forEach(look -> options.add(look.id()));
        if (player != null) {
            options.addAll(WornSuits.variants(player).keySet());
        }
        return SharedSuggestionProvider.suggest(options, builder);
    }

    private static List<SerumLooks.Look> looks(String power) {
        boolean skins = SkinData.enabled();
        return SerumLooks.forPower(power).stream().filter(look -> skins || look.skin().isEmpty()).toList();
    }

    private static int list(CommandSourceStack source, ServerPlayer player) {
        String power = SkinData.power(player);
        Map<String, String> suits = WornSuits.variants(player);
        if (power.isEmpty() && suits.isEmpty()) {
            source.sendFailure(Component.translatable(KEY + "no_serum"));
            return 0;
        }
        List<SerumSkin> skins = !power.isEmpty() && SkinData.enabled() ? SerumSkins.forPower(power) : List.of();
        List<SerumLooks.Look> looks = power.isEmpty() ? List.of() : looks(power);
        String look = SkinData.lookId(player);
        String suitLook = SkinData.suitLook(player);
        String current = suits.containsValue(suitLook) ? suits.entrySet().stream().filter(entry -> entry.getValue().equals(suitLook))
                .map(Map.Entry::getKey).findFirst().orElse(SkinData.DEFAULT)
                : SkinData.costumeOff(player) ? SkinData.OFF : !look.isEmpty() ? look
                : SkinData.get(player).isEmpty() ? SkinData.DEFAULT : SkinData.get(player);
        if (skins.isEmpty() && looks.isEmpty() && suits.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(KEY + "only_default"), false);
            return 1;
        }
        MutableComponent options = Component.empty();
        List<String> ids = new ArrayList<>();
        ids.add(SkinData.DEFAULT);
        looks.stream().filter(value -> value.skin().isEmpty()).forEach(value -> ids.add(value.id()));
        for (SerumSkin skin : skins) {
            ids.add(skin.id());
            looks.stream().filter(value -> value.skin().equals(skin.id())).forEach(value -> ids.add(value.id()));
        }
        suits.keySet().stream().filter(id -> !ids.contains(id)).forEach(ids::add);
        if (!power.isEmpty()) {
            ids.add(SkinData.OFF);
        }
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            if (i > 0) {
                options.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            }
            Optional<SerumSkin> skin = SerumSkins.byId(id);
            Optional<SerumLooks.Look> option = SerumLooks.byId(power, id);
            Optional<String> suit = Optional.ofNullable(suits.get(id));
            Component hover = skin.<Component>map(value -> Component.translatable(value.nameKey()))
                    .or(() -> option.map(SerumLooks.Look::name))
                    .or(() -> suit.map(SerumLooks::name))
                    .orElseGet(() -> Component.translatable(KEY + "hover_" + id));
            options.append(Component.literal(id).withStyle(style -> style
                    .withColor(id.equals(current) ? ChatFormatting.GREEN : ChatFormatting.YELLOW)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/heroskin " + id))));
        }
        source.sendSuccess(() -> Component.translatable(KEY + "list", options), false);
        return 1;
    }

    private static int pick(CommandSourceStack source, Collection<ServerPlayer> targets, String id) {
        int changed = 0;
        for (ServerPlayer target : targets) {
            boolean self = target == source.getEntity();
            String power = SkinData.power(target);
            String suitLook = WornSuits.variants(target).get(id);
            boolean serumOption = SerumSkins.byId(id).isPresent() || SerumLooks.byId(power, id).isPresent();
            if (suitLook != null && !serumOption) {
                SkinData.setSuitLook(target, suitLook);
                Component name = SerumLooks.name(suitLook);
                target.sendSystemMessage(Component.translatable(KEY + "set", name));
                if (!self) {
                    source.sendSuccess(() -> Component.translatable(KEY + "set_other", target.getDisplayName(), name), true);
                }
                changed++;
                continue;
            }
            if (power.isEmpty() && SkinData.DEFAULT.equals(id) && !SkinData.suitLook(target).isEmpty()) {
                SkinData.setSuitLook(target, "");
                target.sendSystemMessage(Component.translatable(KEY + "default"));
                changed++;
                continue;
            }
            if (power.isEmpty()) {
                source.sendFailure(self ? Component.translatable(KEY + "no_serum")
                        : Component.translatable(KEY + "no_serum_other", target.getDisplayName()));
                continue;
            }
            Optional<SerumSkin> skin = SerumSkins.byId(id);
            boolean special = SkinData.DEFAULT.equals(id) || SkinData.OFF.equals(id);
            if (!special && skin.isEmpty()) {
                Optional<SerumLooks.Look> look = SerumLooks.byId(power, id);
                if (look.isEmpty()) {
                    Optional<SerumLooks.Look> elsewhere = SerumLooks.anywhere(id);
                    if (elsewhere.isEmpty()) {
                        source.sendFailure(Component.translatable(KEY + "unknown", id));
                        return changed;
                    }
                    source.sendFailure(self ? Component.translatable(KEY + "wrong_serum", elsewhere.get().name())
                            : Component.translatable(KEY + "wrong_serum_other", target.getDisplayName(), elsewhere.get().name()));
                    continue;
                }
                if (!look.get().skin().isEmpty() && !SkinData.enabled()) {
                    source.sendFailure(Component.translatable(KEY + "disabled"));
                    return changed;
                }
                SkinData.chooseLook(target, look.get());
                Component name = look.get().name();
                target.sendSystemMessage(Component.translatable(KEY + "set", name));
                if (!self) {
                    source.sendSuccess(() -> Component.translatable(KEY + "set_other", target.getDisplayName(), name), true);
                }
                changed++;
                continue;
            }
            if (!special) {
                if (skin.isEmpty()) {
                    source.sendFailure(Component.translatable(KEY + "unknown", id));
                    return changed;
                }
                if (!SkinData.enabled()) {
                    source.sendFailure(Component.translatable(KEY + "disabled"));
                    return changed;
                }
                if (!skin.get().powerId().equals(power)) {
                    source.sendFailure(self ? Component.translatable(KEY + "wrong_serum", Component.translatable(skin.get().nameKey()))
                            : Component.translatable(KEY + "wrong_serum_other", target.getDisplayName(), Component.translatable(skin.get().nameKey())));
                    continue;
                }
            }
            SkinData.choose(target, id);
            Component message = message(id, skin, null);
            target.sendSystemMessage(message);
            if (!self) {
                Component feedback = message(id, skin, target);
                source.sendSuccess(() -> feedback, true);
            }
            changed++;
        }
        return changed;
    }

    private static Component message(String id, Optional<SerumSkin> skin, ServerPlayer other) {
        if (SkinData.DEFAULT.equals(id)) {
            return other == null ? Component.translatable(KEY + "default") : Component.translatable(KEY + "default_other", other.getDisplayName());
        }
        if (SkinData.OFF.equals(id)) {
            return other == null ? Component.translatable(KEY + "off") : Component.translatable(KEY + "off_other", other.getDisplayName());
        }
        Component name = Component.translatable(skin.orElseThrow().nameKey());
        return other == null ? Component.translatable(KEY + "set", name) : Component.translatable(KEY + "set_other", other.getDisplayName(), name);
    }
}
