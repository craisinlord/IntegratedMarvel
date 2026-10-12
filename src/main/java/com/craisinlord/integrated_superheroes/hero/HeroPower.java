package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;
import java.util.function.Supplier;

public record HeroPower(
        String id,
        String tooltip,
        Supplier<Item> syringe,
        MarvelBalanceConfig.HeroStats stats,
        List<String> passives,
        List<Ability> abilities,
        List<Toggle> toggles,
        Hooks hooks
) {
    public record Ability(String name, String displayName, Handler handler) {
    }

    public record Toggle(String flag, String label, Supplier<Item> icon) {
    }

    @FunctionalInterface
    public interface Handler {
        double press(ServerPlayer player, int slot);
    }

    public interface Hooks {
        default void tick(ServerPlayer player) {
        }

        default void extract(ServerPlayer player) {
        }

        default void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
        }

        default void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
        }

        default MarvelBalanceConfig.HeroStats stats(Player player) {
            return null;
        }

        default void death(ServerPlayer player, LivingDeathEvent event) {
        }

        default void jump(Player player) {
        }
    }
}
