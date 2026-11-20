package com.craisinlord.integrated_superheroes.compat.thematic;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

import java.util.HashMap;
import java.util.Map;

final class ThematicRules {
    private ThematicRules() {
    }

    static void apply(MinecraftServer server) {
        Map<String, GameRules.Key<?>> keys = new HashMap<>();
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                keys.put(key.getId(), key);
            }
        });
        GameRules rules = server.getGameRules();
        if (ThematicOptions.on(ThematicOptions.NO_CONSUMABLE_COOLDOWNS)) {
            setBoolean(server, rules, keys.get("disableConsumableCooldowns"), false, true);
        }
        if (ThematicOptions.on(ThematicOptions.NO_RESISTANCE_CAP)) {
            setInt(server, rules, keys.get("maxResistanceAmplifier"), 2, -1);
        }
        if (ThematicOptions.on(ThematicOptions.ALLOW_TRIPWIRE)) {
            setBoolean(server, rules, keys.get("disableTripWire"), true, false);
        }
        if (ThematicOptions.on(ThematicOptions.VANILLA_STATUS_EFFECTS)) {
            setBoolean(server, rules, keys.get("oldStatusEffects"), false, true);
        }
        setDouble(server, rules, keys.get("thematic.base_defense.min"), 70.0, 1.25);
        setDouble(server, rules, keys.get("thematic.base_defense.max"), 78.0, 4.0);
        setDouble(server, rules, keys.get("thematic.base_generic_attack.min"), 6.5, 5.5);
        setDouble(server, rules, keys.get("thematic.base_generic_attack.max"), 8.0, 26.0);
    }

    private static void setDouble(MinecraftServer server, GameRules rules, GameRules.Key<?> key, double thematicDefault, double value) {
        if (key == null) {
            return;
        }
        double current;
        try {
            current = Double.parseDouble(rules.getRule(key).serialize());
        } catch (NumberFormatException e) {
            return;
        }
        if (Double.compare(current, thematicDefault) != 0) {
            return;
        }
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4);
        server.getCommands().performPrefixedCommand(source, "gamerule " + key.getId() + " " + value);
    }

    private static void setBoolean(MinecraftServer server, GameRules rules, GameRules.Key<?> key, boolean thematicDefault, boolean value) {
        if (key != null && rules.getRule(key) instanceof GameRules.BooleanValue rule && rule.get() == thematicDefault) {
            rule.set(value, server);
        }
    }

    private static void setInt(MinecraftServer server, GameRules rules, GameRules.Key<?> key, int thematicDefault, int value) {
        if (key != null && rules.getRule(key) instanceof GameRules.IntegerValue rule && rule.get() == thematicDefault) {
            rule.set(value, server);
        }
    }
}
