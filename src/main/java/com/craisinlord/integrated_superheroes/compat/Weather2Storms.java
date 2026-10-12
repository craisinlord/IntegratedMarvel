package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.hero.MarvelBalanceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import weather2.ServerTickHandler;
import weather2.config.WeatherUtilConfig;
import weather2.weathersystem.WeatherManagerServer;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.storm.WeatherObject;
import weather2.weathersystem.storm.WeatherObjectParticleStorm;
import weather2.weathersystem.wind.WindManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Weather2Storms {
    private static final Set<UUID> WINDS = new HashSet<>();

    private Weather2Storms() {
    }

    public static boolean weatherDimension(Level level) {
        return WeatherUtilConfig.listDimensionsWeather.contains(level.dimension().location().toString());
    }

    private static WeatherManagerServer manager(ServerLevel level) {
        return weatherDimension(level) ? ServerTickHandler.getWeatherManagerFor(level) : null;
    }

    public static boolean managed(ServerLevel level) {
        return manager(level) != null;
    }

    public static void clear(ServerPlayer player) {
        WeatherManagerServer manager = manager(player.serverLevel());
        if (manager == null) {
            return;
        }
        double radius = MarvelBalanceConfig.WEATHER_CLEAR_RADIUS.get();
        for (WeatherObject storm : new ArrayList<>(manager.getStormObjects())) {
            double dx = storm.pos.x - player.getX();
            double dz = storm.pos.z - player.getZ();
            if (dx * dx + dz * dz <= radius * radius) {
                manager.removeWeatherObjectAndSync(storm);
            }
        }
        manager.getWindManager().stopHighWindEvent();
        manager.getWindManager().stopLowWindEvent();
        manager.syncWindUpdate(manager.getWindManager());
        WINDS.remove(player.getUUID());
    }

    public static boolean rain(ServerPlayer player) {
        return summon(player, StormObject.STATE_NORMAL, false) != null;
    }

    public static boolean thunder(ServerPlayer player) {
        return summon(player, StormObject.STATE_THUNDER, true) != null;
    }

    public static boolean hail(ServerPlayer player) {
        return summon(player, StormObject.STATE_HAIL, true) != null;
    }

    public static boolean blizzard(ServerPlayer player) {
        return particleStorm(player, WeatherObjectParticleStorm.StormType.SNOWSTORM);
    }

    public static boolean sandstorm(ServerPlayer player) {
        return particleStorm(player, WeatherObjectParticleStorm.StormType.SANDSTORM);
    }

    public static boolean winds(ServerPlayer player) {
        WeatherManagerServer manager = manager(player.serverLevel());
        if (manager == null) {
            return false;
        }
        WindManager wind = manager.getWindManager();
        if (WINDS.remove(player.getUUID())) {
            wind.stopHighWindEvent();
            player.displayClientMessage(Component.literal("§7The wind settles."), true);
        } else {
            wind.windAngleGlobal = Mth.positiveModulo(player.getYRot(), 360.0F);
            wind.stopLowWindEvent();
            wind.startHighWindEvent();
            WINDS.add(player.getUUID());
        }
        manager.syncWindUpdate(wind);
        return true;
    }

    public static boolean tornado(ServerPlayer player) {
        StormObject storm = summon(player, StormObject.STATE_STAGE1, false);
        if (storm == null) {
            return false;
        }
        storm.setupPlayerControlledTornado(player);
        storm.setPlayerControlledTimeLeft(MarvelBalanceConfig.WEATHER_TORNADO_SECONDS.get() * 20);
        return true;
    }

    private static StormObject summon(ServerPlayer player, int intensity, boolean real) {
        WeatherManagerServer manager = manager(player.serverLevel());
        if (manager == null) {
            return null;
        }
        StormObject storm = new StormObject(manager);
        storm.setupStorm(player);
        storm.levelCurIntensityStage = intensity;
        storm.levelStormIntensityMax = intensity;
        storm.initPositions(new Vec3(player.getX(), StormObject.layers.get(storm.layer), player.getZ()));
        if (real) {
            storm.initRealStorm(null, null);
            storm.levelCurIntensityStage = intensity;
            storm.levelStormIntensityMax = intensity;
        }
        manager.addStormObject(storm);
        manager.syncStormNew(storm);
        return storm;
    }

    private static boolean particleStorm(ServerPlayer player, WeatherObjectParticleStorm.StormType type) {
        WeatherManagerServer manager = manager(player.serverLevel());
        if (manager == null) {
            return false;
        }
        manager.spawnParticleStorm(BlockPos.containing(player.position()), type);
        manager.getWindManager().stopLowWindEvent();
        manager.getWindManager().startHighWindEvent();
        manager.syncWindUpdate(manager.getWindManager());
        return true;
    }

    public static boolean isStormy(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        WeatherManagerServer manager = manager(level);
        if (manager == null) {
            return false;
        }
        for (WeatherObject weather : manager.getStormObjects()) {
            double dx = weather.pos.x - player.getX();
            double dz = weather.pos.z - player.getZ();
            double size = weather.getSize();
            if (dx * dx + dz * dz > size * size) {
                continue;
            }
            if (weather instanceof WeatherObjectParticleStorm || weather instanceof StormObject storm && storm.attrib_precipitation) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIntense(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        WeatherManagerServer manager = manager(level);
        if (manager == null) {
            return false;
        }
        for (WeatherObject weather : manager.getStormObjects()) {
            double dx = weather.pos.x - player.getX();
            double dz = weather.pos.z - player.getZ();
            double size = weather.getSize();
            if (dx * dx + dz * dz > size * size) {
                continue;
            }
            if (weather instanceof WeatherObjectParticleStorm
                    || weather instanceof StormObject storm && storm.levelCurIntensityStage >= StormObject.STATE_THUNDER) {
                return true;
            }
        }
        return false;
    }

    public static void forget(ServerPlayer player) {
        WINDS.remove(player.getUUID());
    }
}
