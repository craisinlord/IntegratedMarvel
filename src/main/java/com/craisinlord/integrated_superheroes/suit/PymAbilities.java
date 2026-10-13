package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import com.craisinlord.integrated_superheroes.hero.HeroWheels;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PymAbilities {
    public static final String CALL_COLONY = "pym_call_colony";
    public static final String CALL_HIVE = "pym_call_hive";
    public static final String PHEROMONES = "pym_pheromone_call";
    public static final String SIC_EM = "pym_sic_em";
    public static final String OVERRUN = "pym_overrun";
    public static final String STING_STORM = "pym_sting_storm";
    public static final String COLONY_ORDERS = "pym_colony_orders";
    public static final String HIVE_ORDERS = "pym_hive_orders";
    private static final Map<String, Ability> ABILITIES = new LinkedHashMap<>();
    private static final Map<String, String> ORDER_LABELS = Map.of(
            PymColony.DEFEND, "Defend",
            PymColony.ATTACK, "Attack",
            PymColony.FORAGE, "Forage",
            PymColony.CHEW, "Chew",
            PymColony.SCREEN, "Screen",
            PymColony.STAY, "Stay");
    private static final List<String> ANT_LOADOUT = List.of(CALL_COLONY, PHEROMONES, SIC_EM, OVERRUN, COLONY_ORDERS);
    private static final List<String> WASP_LOADOUT = List.of(CALL_HIVE, PHEROMONES, SIC_EM, STING_STORM, HIVE_ORDERS);

    public record Ability(String name, String displayName, Handler handler) {
    }

    @FunctionalInterface
    public interface Handler {
        double press(ServerPlayer player, PymHelmets.Kind kind);
    }

    static {
        register(new Ability(CALL_COLONY, "Call the Colony", PymColony::callColony));
        register(new Ability(CALL_HIVE, "Call the Hive", PymColony::callColony));
        register(new Ability(PHEROMONES, "Pheromone Call", PymColony::pheromones));
        register(new Ability(SIC_EM, "Sic 'Em", PymColony::sicEm));
        register(new Ability(OVERRUN, "Overrun", (player, kind) -> PymColony.overrun(player)));
        register(new Ability(STING_STORM, "Sting Storm", (player, kind) -> PymColony.stingStorm(player)));
        register(new Ability(COLONY_ORDERS, "Colony Orders", (player, kind) -> 0.0D));
        register(new Ability(HIVE_ORDERS, "Hive Orders", (player, kind) -> 0.0D));
    }

    private PymAbilities() {
    }

    private static void register(Ability ability) {
        ABILITIES.put(ability.name(), ability);
    }

    public static void registerWheels() {
        HeroWheels.register(new HeroWheels.Wheel(COLONY_ORDERS, List.of(
                new HeroWheels.Option(PymColony.DEFEND, "Defend", () -> Items.SHIELD),
                new HeroWheels.Option(PymColony.ATTACK, "Attack", () -> Items.IRON_SWORD),
                new HeroWheels.Option(PymColony.FORAGE, "Forage", () -> Items.HOPPER),
                new HeroWheels.Option(PymColony.CHEW, "Chew", () -> Items.OAK_LOG),
                new HeroWheels.Option(PymColony.STAY, "Stay", () -> Items.LEAD)
        ), (player, option) -> true, (player, option) -> order(player, PymHelmets.Kind.ANT, option)));
        HeroWheels.register(new HeroWheels.Wheel(HIVE_ORDERS, List.of(
                new HeroWheels.Option(PymColony.DEFEND, "Defend", () -> Items.SHIELD),
                new HeroWheels.Option(PymColony.ATTACK, "Attack", () -> Items.IRON_SWORD),
                new HeroWheels.Option(PymColony.SCREEN, "Screen", () -> HeroFx.item("antarchy:wasp_nest", Items.HONEYCOMB)),
                new HeroWheels.Option(PymColony.STAY, "Stay", () -> Items.LEAD)
        ), (player, option) -> true, (player, option) -> order(player, PymHelmets.Kind.WASP, option)));
    }

    private static void order(ServerPlayer player, PymHelmets.Kind kind, String option) {
        if (kind(player) == kind) {
            PymColony.giveOrder(player, kind, option);
        }
    }

    static String orderLabel(String order) {
        return ORDER_LABELS.getOrDefault(order, order);
    }

    public static boolean isPym(String name) {
        return name != null && ABILITIES.containsKey(name);
    }

    public static Map<String, String> names() {
        Map<String, String> names = new LinkedHashMap<>();
        ABILITIES.values().forEach(ability -> names.put(ability.name(), ability.displayName()));
        return names;
    }

    public static PymHelmets.Kind kind(Player player) {
        if (!PymConfig.ENABLED.get()) {
            return null;
        }
        return PymHelmets.kind(player);
    }

    public static Map<String, String> loadout(Player player) {
        Map<String, String> wanted = new LinkedHashMap<>();
        PymHelmets.Kind kind = kind(player);
        if (kind == null) {
            return wanted;
        }
        for (String name : kind == PymHelmets.Kind.ANT ? ANT_LOADOUT : WASP_LOADOUT) {
            wanted.put(name, ABILITIES.get(name).displayName());
        }
        return wanted;
    }

    public static boolean granted(Player player, String name) {
        return loadout(player).containsKey(name);
    }

    public static boolean handle(ServerPlayer player, String name, int slot, String inputType) {
        Ability ability = ABILITIES.get(name);
        if (ability == null) {
            return false;
        }
        if (!"press".equals(inputType)) {
            return true;
        }
        PymHelmets.Kind kind = kind(player);
        if (kind == null || !granted(player, name)) {
            return true;
        }
        PlayerData data = HeroPowers.data(player);
        if (slot < 0 || slot >= data.getActivePowers().size()) {
            return true;
        }
        Power slotPower = data.getActivePower(slot);
        if (slotPower == null || !name.equals(slotPower.getName()) || slotPower.getCooldown() > 0.0D) {
            return true;
        }
        double cooldown = ability.handler().press(player, kind);
        if (cooldown > 0.0D) {
            slotPower.setCooldown(cooldown);
            PlayerData.sync(player);
        }
        return true;
    }
}
