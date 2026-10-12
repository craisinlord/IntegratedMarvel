package com.craisinlord.integrated_marvel.hero;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public final class HeroWheels {
    private static final Map<String, Wheel> WHEELS = new HashMap<>();

    private HeroWheels() {
    }

    public record Option(String id, String label, Supplier<Item> icon) {
    }

    public record Wheel(String ability, List<Option> options, BiPredicate<Player, Option> available, BiConsumer<ServerPlayer, String> handler) {
    }

    public static void register(Wheel wheel) {
        WHEELS.put(wheel.ability(), wheel);
    }

    public static Wheel get(String ability) {
        return WHEELS.get(ability);
    }

    public static void choose(ServerPlayer player, String ability, String option) {
        Wheel wheel = WHEELS.get(ability);
        HeroPower hero = HeroPowers.activeHero(player);
        if (wheel == null || hero == null || hero.abilities().stream().noneMatch(entry -> entry.name().equals(ability))) {
            return;
        }
        if (wheel.options().stream().anyMatch(entry -> entry.id().equals(option))) {
            wheel.handler().accept(player, option);
        }
    }
}
