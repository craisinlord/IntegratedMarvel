package com.craisinlord.integrated_superheroes.hero.skin;

import com.craisinlord.integrated_superheroes.hero.IronManSuits;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SuitSkins {
    private static final String KEY = "commands.integrated_superheroes.suitskin.";
    private static final String IRON_MAN = "iron_man";
    private static Map<String, Map<String, String>> families;

    private SuitSkins() {
    }

    private static synchronized Map<String, Map<String, String>> families() {
        if (families == null) {
            Map<String, Map<String, String>> found = new LinkedHashMap<>();
            try (var stream = SuitSkins.class.getResourceAsStream("/data/integrated_superheroes/suit_looks.json")) {
                if (stream != null) {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (var family : json.entrySet()) {
                        Map<String, String> looks = new LinkedHashMap<>();
                        family.getValue().getAsJsonArray().forEach(value -> {
                            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                                looks.put(id.getPath(), id.toString());
                            }
                        });
                        found.put(family.getKey(), Collections.unmodifiableMap(looks));
                    }
                }
            } catch (java.io.IOException | RuntimeException ignored) {
            }
            families = Collections.unmodifiableMap(found);
        }
        return families;
    }

    public static Map<String, String> ironManLooks() {
        return families().getOrDefault(IRON_MAN, Map.of());
    }

    public static boolean ironManLook(String costume) {
        return costume != null && !costume.isEmpty() && ironManLooks().containsValue(costume);
    }

    public static boolean wearsIronMan(Player player) {
        ItemStack chest = player.getInventory().armor.get(EquipmentSlot.CHEST.getIndex());
        return !chest.isEmpty() && IronManSuits.suitId(chest.getItem()) != null;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("suitskin")
                .executes(context -> list(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("look", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            List<String> options = new ArrayList<>(ironManLooks().keySet());
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
        if (ironManLooks().isEmpty()) {
            source.sendFailure(Component.translatable(KEY + "none"));
            return 0;
        }
        String current = SkinData.suitLook(player);
        MutableComponent options = Component.empty();
        List<String> ids = new ArrayList<>(ironManLooks().keySet());
        ids.add(SkinData.OFF);
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            if (i > 0) {
                options.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            }
            String costume = ironManLooks().get(id);
            boolean worn = costume == null ? current.isEmpty() : costume.equals(current);
            Component hover = costume == null ? Component.translatable(KEY + "hover_off") : SerumLooks.name(costume);
            options.append(Component.literal(id).withStyle(style -> style
                    .withColor(worn ? ChatFormatting.GREEN : ChatFormatting.YELLOW)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/suitskin " + id))));
        }
        source.sendSuccess(() -> Component.translatable(KEY + "list", options), false);
        return 1;
    }

    private static int pick(CommandSourceStack source, Collection<ServerPlayer> targets, String id) {
        boolean off = SkinData.OFF.equals(id);
        Optional<String> costume = Optional.ofNullable(ironManLooks().get(id));
        if (!off && costume.isEmpty()) {
            source.sendFailure(Component.translatable(KEY + "unknown", id));
            return 0;
        }
        int changed = 0;
        for (ServerPlayer target : targets) {
            boolean self = target == source.getEntity();
            SkinData.setSuitLook(target, costume.orElse(""));
            Component message = off ? Component.translatable(KEY + "off")
                    : Component.translatable(wearsIronMan(target) ? KEY + "set" : KEY + "set_later", SerumLooks.name(costume.get()));
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
