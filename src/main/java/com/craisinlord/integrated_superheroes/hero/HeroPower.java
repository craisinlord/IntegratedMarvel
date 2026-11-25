package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
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
    public record Ability(String name, String displayName, Handler handler, Cast cast) {
        public Ability(String name, String displayName, Handler handler) {
            this(name, displayName, handler, null);
        }

        public static Ability of(String name, String displayName, Cast cast) {
            return new Ability(name, displayName, cast::press, cast);
        }
    }

    public record Toggle(String flag, String label, Supplier<Item> icon, boolean defaultOn) {
        public Toggle(String flag, String label, Supplier<Item> icon) {
            this(flag, label, icon, true);
        }
    }

    @FunctionalInterface
    public interface Handler {
        double press(ServerPlayer player, int slot);
    }

    @FunctionalInterface
    public interface Cast {
        double press(LivingEntity caster, int slot);
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
