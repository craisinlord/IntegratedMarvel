package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.Weather2Storms;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StormWeather {
    public static final String ABILITY = "weather_control";
    private static final Set<String> VANILLA = Set.of("clear", "rain", "thunder");
    private static final Map<UUID, Map<String, Long>> READY_AT = new HashMap<>();
    private static final Set<String> INTENSE = Set.of("thunder", "hail", "blizzard", "sandstorm", "tornado");
    private static final long INTENSE_TICKS = 6000L;
    private static final Map<UUID, Long> INTENSE_UNTIL = new HashMap<>();
    public static final List<HeroWheels.Option> OPTIONS = List.of(
            new HeroWheels.Option("clear", "Clear Skies", () -> Items.SUNFLOWER),
            new HeroWheels.Option("rain", "Rain", () -> Items.WATER_BUCKET),
            new HeroWheels.Option("thunder", "Thunderstorm", () -> Items.LIGHTNING_ROD),
            new HeroWheels.Option("hail", "Hailstorm", () -> Items.SNOWBALL),
            new HeroWheels.Option("blizzard", "Blizzard", () -> Items.POWDER_SNOW_BUCKET),
            new HeroWheels.Option("sandstorm", "Sandstorm", () -> Items.SAND),
            new HeroWheels.Option("winds", "Winds", () -> Items.FEATHER),
            new HeroWheels.Option("tornado", "Tornado", () -> HeroFx.item("antarchy:eye_of_the_storm", Items.WIND_CHARGE))
    );

    private StormWeather() {
    }

    public static HeroWheels.Wheel wheel() {
        List<HeroWheels.Option> options = ModCompat.WEATHER2 ? OPTIONS : OPTIONS.stream().filter(option -> VANILLA.contains(option.id())).toList();
        return new HeroWheels.Wheel(ABILITY, options, (player, option) -> available(player.level(), option.id()), StormWeather::cast);
    }

    private static boolean weather2Here(Level level) {
        return ModCompat.WEATHER2 && Weather2Storms.weatherDimension(level);
    }

    private static boolean available(Level level, String option) {
        if (VANILLA.contains(option)) {
            return level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling() || weather2Here(level);
        }
        return weather2Here(level);
    }

    private static double cooldown(String option) {
        return switch (option) {
            case "tornado" -> MarvelBalanceConfig.WEATHER_TORNADO_COOLDOWN.get();
            case "thunder", "hail", "blizzard", "sandstorm" -> MarvelBalanceConfig.WEATHER_MAJOR_COOLDOWN.get();
            default -> MarvelBalanceConfig.WEATHER_MINOR_COOLDOWN.get();
        };
    }

    private static String label(String option) {
        return OPTIONS.stream().filter(entry -> entry.id().equals(option)).map(HeroWheels.Option::label).findFirst().orElse(option);
    }

    public static void cast(ServerPlayer player, String option) {
        ServerLevel level = player.serverLevel();
        if (!available(level, option)) {
            player.displayClientMessage(Component.literal("§7The skies here don't answer."), true);
            return;
        }
        long now = level.getGameTime();
        Map<String, Long> ready = READY_AT.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        Long readyAt = ready.get(option);
        if (readyAt != null && now < readyAt) {
            player.displayClientMessage(Component.literal("§7" + label(option) + " gathers again in " + (readyAt - now + 19) / 20 + "s."), true);
            return;
        }
        boolean managed = weather2Here(level) && Weather2Storms.managed(level);
        if (!VANILLA.contains(option) && !managed) {
            player.displayClientMessage(Component.literal("§7The skies here don't answer."), true);
            return;
        }
        boolean wasStormy = isStormy(player);
        switch (option) {
            case "clear" -> {
                level.setWeatherParameters(12000, 0, false, false);
                if (managed) {
                    Weather2Storms.clear(player);
                }
                StormZones.release(player);
            }
            case "rain" -> {
                level.setWeatherParameters(0, 6000, true, false);
                StormZones.force(player, false);
                if (managed) {
                    Weather2Storms.rain(player);
                }
            }
            case "thunder" -> {
                level.setWeatherParameters(0, 6000, true, true);
                if (!wasStormy) {
                    HeroAdvancements.award(player, "x_men/storm/make_it_rain");
                }
                StormZones.force(player, false);
                if (managed) {
                    Weather2Storms.thunder(player);
                }
            }
            case "hail" -> {
                level.setWeatherParameters(0, 6000, true, false);
                StormZones.force(player, false);
                Weather2Storms.hail(player);
            }
            case "blizzard" -> {
                level.setWeatherParameters(0, 6000, true, false);
                StormZones.force(player, true);
                Weather2Storms.blizzard(player);
            }
            case "sandstorm" -> Weather2Storms.sandstorm(player);
            case "winds" -> Weather2Storms.winds(player);
            case "tornado" -> Weather2Storms.tornado(player);
            default -> {
                return;
            }
        }
        ready.put(option, now + (long) (cooldown(option) * 20.0D));
        if (INTENSE.contains(option)) {
            long duration = option.equals("tornado") ? MarvelBalanceConfig.WEATHER_TORNADO_SECONDS.get() * 20L : INTENSE_TICKS;
            INTENSE_UNTIL.merge(player.getUUID(), now + duration, Math::max);
        } else if (option.equals("clear")) {
            INTENSE_UNTIL.remove(player.getUUID());
        }
        HeroAbilities.setCooldown(player, ABILITY, cooldown(option));
        HeroAnimations.play(player, "web_shoot_both");
        HeroFx.play(player, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8F, 1.2F);
        HeroFx.play(player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.0F, 0.7F);
        player.displayClientMessage(Component.literal("§b" + label(option)), true);
    }

    public static boolean isStormy(Player player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        if (level.isThundering() || level.isRainingAt(pos.above())) {
            return true;
        }
        if (level.isClientSide || !weather2Here(level)) {
            return false;
        }
        return Weather2Storms.isStormy(player);
    }

    public static boolean isUnleashing(ServerPlayer player) {
        Long until = INTENSE_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (level.getGameTime() >= until) {
            INTENSE_UNTIL.remove(player.getUUID());
            return false;
        }
        if (level.isThundering() && level.isRainingAt(player.blockPosition().above())) {
            return true;
        }
        return weather2Here(level) && Weather2Storms.isIntense(player);
    }

    public static void forget(ServerPlayer player) {
        READY_AT.remove(player.getUUID());
        INTENSE_UNTIL.remove(player.getUUID());
        if (ModCompat.WEATHER2) {
            Weather2Storms.forget(player);
        }
    }
}
