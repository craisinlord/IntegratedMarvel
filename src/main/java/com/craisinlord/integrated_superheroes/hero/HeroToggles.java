package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HeroToggles {
    public static final String WHEEL_ABILITY = "hero_abilities";
    public static final HeroPower.Ability WHEEL = new HeroPower.Ability(WHEEL_ABILITY, "Hero Abilities", (player, slot) -> 0.0D);

    public static final HeroPower.Toggle SPEED = new HeroPower.Toggle("integrated_superheroes_off_speed", "Speed", () -> Items.SUGAR);
    public static final HeroPower.Toggle JUMP = new HeroPower.Toggle("integrated_superheroes_off_jump", "Jump Boost", () -> Items.RABBIT_FOOT);
    public static final HeroPower.Toggle SUPER_LEAP = new HeroPower.Toggle("integrated_superheroes_off_jump", "Super Leap", () -> Items.RABBIT_FOOT);
    public static final HeroPower.Toggle SENSES = new HeroPower.Toggle("integrated_superheroes_off_senses", "Heightened Senses", () -> Items.ENDER_EYE);
    public static final HeroPower.Toggle FLIGHT = new HeroPower.Toggle("integrated_superheroes_off_flight", "Flight", () -> Items.ELYTRA);
    public static final HeroPower.Toggle SUPERSONIC = new HeroPower.Toggle("integrated_superheroes_on_supersonic", "Supersonic", () -> Items.FIREWORK_ROCKET, false);

    private static final String SPIDER_ID = "spider_powers_peter_parker";
    private static final Set<String> SPIDER_TOGGLES = Set.of("wall_crawling", "super_jump", "spider_sense");

    private HeroToggles() {
    }

    public static List<HeroPower.Toggle> flying(HeroPower.Toggle... others) {
        List<HeroPower.Toggle> toggles = new ArrayList<>();
        toggles.add(FLIGHT);
        if (ModCompat.VILTRUMITE) {
            toggles.add(SUPERSONIC);
        }
        toggles.addAll(List.of(others));
        return List.copyOf(toggles);
    }

    public static boolean isOn(Player player, HeroPower.Toggle toggle) {
        return HeroPowers.data(player).hasPassivePower(toggle.flag()) != toggle.defaultOn();
    }

    public static boolean mayToggle(Player player, String flag) {
        String primary = HeroPowers.data(player).getPrimaryPower();
        if (SPIDER_ID.equals(primary)) {
            return SPIDER_TOGGLES.contains(flag);
        }
        HeroPower hero = HeroPowers.get(primary);
        return hero != null && hero.toggles().stream().anyMatch(toggle -> toggle.flag().equals(flag));
    }

    public static void applyEffects(ServerPlayer player, Map<Holder<MobEffect>, Integer> amplifiers, Map<Holder<MobEffect>, HeroPower.Toggle> toggles) {
        amplifiers.forEach((effect, amplifier) -> {
            HeroPower.Toggle toggle = toggles.get(effect);
            MobEffectInstance current = player.getEffect(effect);
            if (toggle == null || isOn(player, toggle)) {
                if (current == null || current.getAmplifier() < amplifier) {
                    player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true, false, true));
                }
            } else if (current != null && current.isInfiniteDuration() && current.isAmbient()) {
                player.removeEffect(effect);
            }
        });
    }

    public static void ensureAbilities(ServerPlayer player, HeroPower hero) {
        PlayerData data = HeroPowers.data(player);
        boolean changed = false;
        for (HeroPower.Ability ability : hero.abilities()) {
            if (!data.hasActivePower(ability.name()) && !data.hasAvailablePower(ability.name())) {
                data.addActivePowers(HeroPowers.newPower(ability));
                changed = true;
            }
        }
        if (changed) {
            HeroSlots.arrange(data);
            PlayerData.sync(player);
        }
    }

    public static void clearFlags(ServerPlayer player, HeroPower hero) {
        PlayerData data = HeroPowers.data(player);
        hero.toggles().forEach(toggle -> data.removePassivePower(toggle.flag()));
    }
}
