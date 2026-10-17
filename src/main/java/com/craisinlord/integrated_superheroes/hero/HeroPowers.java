package com.craisinlord.integrated_superheroes.hero;

import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.init.AttachmentsRegistry;
import com.stxllarplanes.marvellegacy.init.ItemRegistry;
import com.stxllarplanes.marvellegacy.power.ActivePowerList;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HeroPowers {
    private static final Map<String, HeroPower> HEROES = new LinkedHashMap<>();
    private static final Map<String, MarvelBalanceConfig.HeroStats> STATS = new HashMap<>();
    private static final Map<String, HeroPower.Ability> ABILITIES = new HashMap<>();
    private static final java.util.Set<String> RETIRED = java.util.Set.of("parachute", "flame_dash", "bond", "tendril_lash", "katana_flurry", "blink");

    private HeroPowers() {
    }

    public static void registerLegacyHero(String id, MarvelBalanceConfig.HeroStats stats) {
        STATS.put(id, stats);
    }

    public static void register(HeroPower hero) {
        HEROES.put(hero.id(), hero);
        STATS.put(hero.id(), hero.stats());
        hero.abilities().forEach(ability -> ABILITIES.put(ability.name(), ability));
    }

    public static HeroPower get(String id) {
        return HEROES.get(id);
    }

    public static Iterable<HeroPower> all() {
        return HEROES.values();
    }

    public static void registerLegacyPowerEntries() {
        ABILITIES.values().forEach(ability -> ActivePowerList.activePowerList.put(ability.name(), newPower(ability)));
    }

    public static PlayerData data(Player player) {
        return player.getData(AttachmentsRegistry.PLAYER_DATA.get());
    }

    public static HeroPower activeHero(Player player) {
        return HEROES.get(data(player).getPrimaryPower());
    }

    public static void pruneStale(ServerPlayer player) {
        PlayerData data = data(player);
        HeroPower hero = activeHero(player);
        java.util.Set<String> keep = new java.util.HashSet<>();
        if (hero != null) {
            hero.abilities().forEach(ability -> keep.add(ability.name()));
        }
        java.util.Set<String> stale = new java.util.HashSet<>();
        for (Power power : data.getActivePowers()) {
            if (power != null && isStale(power.getName(), keep)) {
                stale.add(power.getName());
            }
        }
        for (Power power : data.getAvailablePowers()) {
            if (power != null && isStale(power.getName(), keep)) {
                stale.add(power.getName());
            }
        }
        if (stale.isEmpty()) {
            return;
        }
        stale.forEach(name -> {
            data.removeActivePowers(name);
            data.removeAvailablePowers(name);
        });
        PlayerData.sync(player);
    }

    private static boolean isStale(String name, java.util.Set<String> keep) {
        return name != null && !keep.contains(name) && (RETIRED.contains(name) || ABILITIES.containsKey(name));
    }

    public static boolean inject(ServerPlayer player, HeroPower hero) {
        PlayerData data = data(player);
        if (!"none".equals(data.getPrimaryPower())) {
            return false;
        }
        data.setPrimaryPower(hero.id());
        hero.passives().forEach(data::addPassivePower);
        data.retainActivePowerSize();
        hero.abilities().forEach(ability -> data.addActivePowers(newPower(ability)));
        HeroSlots.arrange(data);
        HeroAttributes.apply(player, statsFor(player));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SYRINGE_EMPTY.get()));
        pruneStale(player);
        PlayerData.sync(player);
        return true;
    }

    public static void extract(ServerPlayer player, HeroPower hero) {
        extract(player, hero, true);
    }

    public static void strip(ServerPlayer player, HeroPower hero) {
        extract(player, hero, false);
    }

    private static void extract(ServerPlayer player, HeroPower hero, boolean refill) {
        PlayerData data = data(player);
        HeroAttributes.clear(player);
        hero.passives().forEach(data::removePassivePower);
        HeroToggles.clearFlags(player, hero);
        hero.abilities().forEach(ability -> {
            data.removeActivePowers(ability.name());
            data.removeAvailablePowers(ability.name());
        });
        data.setPrimaryPower(null);
        HeroSlots.arrange(data);
        hero.hooks().extract(player);
        if (refill) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(hero.syringe().get()));
        }
        pruneStale(player);
        PlayerData.sync(player);
    }

    public static boolean handleAbility(ServerPlayer player, String abilityName, int slot) {
        HeroPower.Ability ability = ABILITIES.get(abilityName);
        if (ability == null) {
            return false;
        }
        if (com.craisinlord.integrated_superheroes.villain.VillainWorld.suppressed(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("integrated_superheroes.villains.suppressed")
                    .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
            return true;
        }
        HeroPower hero = activeHero(player);
        if (hero == null || !hero.abilities().contains(ability) || HealingFactor.isDowned(player)) {
            return true;
        }
        PlayerData data = data(player);
        Power slotPower = data.getActivePower(slot);
        if (!abilityName.equals(slotPower.getName()) || slotPower.getCooldown() > 0.0D) {
            return true;
        }
        double cooldown = ability.handler().press(player, slot);
        if (cooldown > 0.0D) {
            slotPower.setCooldown(cooldown);
            PlayerData.sync(player);
        }
        return true;
    }

    public static MarvelBalanceConfig.HeroStats statsFor(Player player) {
        HeroPower hero = activeHero(player);
        if (hero != null) {
            MarvelBalanceConfig.HeroStats override = hero.hooks().stats(player);
            if (override != null) {
                return override;
            }
        }
        return STATS.get(data(player).getPrimaryPower());
    }

    public static void syncStats(ServerPlayer player) {
        MarvelBalanceConfig.HeroStats stats = statsFor(player);
        if (stats != null) {
            HeroAttributes.apply(player, stats);
        } else {
            HeroAttributes.syncExtras(player, null);
            HeroAttributes.syncStrength(player, 0);
        }
    }

    public static double unarmedBonus(Player player) {
        MarvelBalanceConfig.HeroStats stats = statsFor(player);
        return stats == null ? 0.0D : stats.unarmedDamage.get();
    }

    private static Power newPower(HeroPower.Ability ability) {
        return new Power(ability.name(), ability.displayName(), 2, 0.0D, false, false);
    }
}
