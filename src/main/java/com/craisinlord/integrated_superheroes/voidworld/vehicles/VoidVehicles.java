package com.craisinlord.integrated_superheroes.voidworld.vehicles;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.mts.MtsVehicles;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantConfig;
import com.craisinlord.integrated_superheroes.voidworld.VoidConfig;
import com.craisinlord.integrated_superheroes.voidworld.VoidTier;
import com.craisinlord.integrated_superheroes.voidworld.VoidWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class VoidVehicles {
    private static final long FORGET_TICKS = 24000L * 30L;
    private static List<? extends String> cachedAllow;
    private static List<? extends String> cachedDeny;
    private static List<Pattern> allowPatterns = List.of();
    private static List<Pattern> denyPatterns = List.of();

    private VoidVehicles() {
    }

    public static void register() {
        if (!ModCompat.MTS) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(VoidVehicles::onLevelTick);
        VehicleTears.register();
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !VoidWorld.isVoid(level) || !VoidConfig.SPEC.isLoaded()) {
            return;
        }
        long now = level.getGameTime();
        if (now % 20L != 0L) {
            return;
        }
        List<Vec3> loaded = upkeep(level, now);
        if (!VoidConfig.VEHICLES_ENABLED.get() || level.players().isEmpty()) {
            return;
        }
        long period = Math.max(1L, VoidConfig.VEHICLE_CHECK_SECONDS.get());
        for (ServerPlayer player : level.players()) {
            if ((now / 20L + player.getId()) % period != 0L || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (loaded.size() + VehicleTears.pending(level) >= VoidConfig.VEHICLE_MAX_LOADED.get()) {
                return;
            }
            Vec3 placed = check(level, player, loaded);
            if (placed != null) {
                loaded.add(placed);
            }
        }
    }

    private static List<Vec3> upkeep(ServerLevel level, long now) {
        List<Vec3> loaded = new ArrayList<>();
        VoidVehicleData data = VoidVehicleData.get(level);
        if (data.tracked.isEmpty()) {
            return loaded;
        }
        double range = VoidConfig.VEHICLE_DESPAWN_DISTANCE.get();
        long despawnTicks = VoidConfig.VEHICLE_DESPAWN_SECONDS.get() * 20L;
        boolean changed = now % 1200L == 0L;
        Iterator<Map.Entry<UUID, VoidVehicleData.Entry>> it = data.tracked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, VoidVehicleData.Entry> next = it.next();
            UUID id = next.getKey();
            VoidVehicleData.Entry entry = next.getValue();
            VehicleState state = MtsVehicles.state(level, id);
            if (state == null) {
                if (now - entry.lastSeen > FORGET_TICKS && !VehicleTears.busy(id)) {
                    it.remove();
                    changed = true;
                }
                continue;
            }
            entry.lastSeen = now;
            if (state.playerAboard()) {
                it.remove();
                changed = true;
                continue;
            }
            boolean busy = VehicleTears.busy(id);
            if (state.othersAboard() && !busy) {
                MtsVehicles.eject(level, id);
            }
            if (despawnTicks <= 0L || busy || nearPlayer(level, state.position(), range)) {
                entry.farSince = -1L;
            } else if (entry.farSince < 0L) {
                entry.farSince = now;
            } else if (now - entry.farSince >= despawnTicks) {
                MtsVehicles.remove(level, id);
                it.remove();
                changed = true;
                continue;
            }
            loaded.add(state.position());
        }
        if (changed) {
            data.setDirty();
        }
        return loaded;
    }

    private static boolean nearPlayer(ServerLevel level, Vec3 at, double range) {
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && horizontalSqr(player.position(), at) <= range * range) {
                return true;
            }
        }
        return false;
    }

    private static double horizontalSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    @Nullable
    private static Vec3 check(ServerLevel level, ServerPlayer player, List<Vec3> loaded) {
        int tier = VoidTier.forPlayer(player);
        double chance = VoidConfig.VEHICLE_CHANCE.get() + VoidConfig.VEHICLE_CHANCE_PER_TIER.get() * tier;
        if (chance <= 0.0D || level.random.nextDouble() >= chance) {
            return null;
        }
        double radius = VoidConfig.VEHICLE_DESPAWN_DISTANCE.get();
        int near = 0;
        for (Vec3 at : loaded) {
            if (horizontalSqr(at, player.position()) <= radius * radius) {
                near++;
            }
        }
        if (near >= VoidConfig.VEHICLE_MAX_NEAR_PLAYER.get()) {
            return null;
        }
        boolean tear = level.random.nextDouble() < VoidConfig.VEHICLE_TEAR_CHANCE.get();
        Result result = spawn(level, player, tier, tear);
        return result == null ? null : result.at();
    }

    public record Result(Vec3 at, String what, boolean tear) {
    }

    @Nullable
    public static Result force(ServerLevel level, ServerPlayer player, boolean tear) {
        if (!ModCompat.MTS || !VoidConfig.SPEC.isLoaded()) {
            return null;
        }
        return spawn(level, player, VoidTier.forPlayer(player), tear);
    }

    @Nullable
    private static Result spawn(ServerLevel level, ServerPlayer player, int tier, boolean tear) {
        boolean aircraft = tier >= VoidConfig.VEHICLE_AIRCRAFT_MIN_TIER.get() && level.random.nextDouble() < VoidConfig.VEHICLE_AIRCRAFT_CHANCE.get();
        double fuel = fuel(level);
        if (tear) {
            boolean driver = VariantConfig.ready() && VariantConfig.ENABLED.get() && level.random.nextDouble() < VoidConfig.VEHICLE_DRIVER_CHANCE.get();
            boolean launch = level.random.nextDouble() < VoidConfig.VEHICLE_LAUNCH_CHANCE.get();
            Vec3 at = VehicleTears.open(level, player, tier, aircraft, fuel, driver, launch);
            return at == null ? null : new Result(at, "a tear" + (driver ? " with a driver" : "") + (launch ? ", launching" : ""), true);
        }
        double min = VoidConfig.VEHICLE_MIN_DISTANCE.get();
        double max = Math.max(min + 1.0D, VoidConfig.VEHICLE_MAX_DISTANCE.get());
        Vec3 at = groundSpot(level, player.position(), min, max);
        if (at == null) {
            return null;
        }
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        Vec3 facing = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        UUID id = MtsVehicles.spawn(level, at, facing, 0.0D, level.random, filter(), aircraft, fuel);
        if (id == null) {
            return null;
        }
        VoidVehicleData.get(level).track(id, level.getGameTime());
        String what = MtsVehicles.describe(level, id);
        return new Result(at, what == null ? "a vehicle" : what, false);
    }

    static double fuel(ServerLevel level) {
        double low = VoidConfig.VEHICLE_FUEL_MIN.get();
        double high = Math.max(low, VoidConfig.VEHICLE_FUEL_MAX.get());
        return low + level.random.nextDouble() * (high - low);
    }

    @Nullable
    static Vec3 groundSpot(ServerLevel level, Vec3 around, double min, double max) {
        for (int attempt = 0; attempt < 48; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = min + level.random.nextDouble() * (max - min);
            int x = Mth.floor(around.x + Math.cos(angle) * distance);
            int z = Mth.floor(around.z + Math.sin(angle) * distance);
            Integer y = groundAt(level, x, z);
            if (y != null && flat(level, x, y, z, 1)) {
                return new Vec3(x + 0.5D, y, z + 0.5D);
            }
        }
        return null;
    }

    @Nullable
    static Integer groundAt(ServerLevel level, int x, int z) {
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return null;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() || y <= VoidConfig.CATCH_Y.get() + 2) {
            return null;
        }
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBlockState(pos.below()).getFluidState().isEmpty() || level.getBlockState(pos.below()).isAir()
                || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return null;
        }
        return y;
    }

    static boolean flat(ServerLevel level, int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx += radius) {
            for (int dz = -radius; dz <= radius; dz += radius) {
                Integer h = groundAt(level, x + dx, z + dz);
                if (h == null || Math.abs(h - y) > 3) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean runway(ServerLevel level, Vec3 from, Vec3 facing, int length) {
        int y = Mth.floor(from.y);
        int misses = 0;
        for (int step = 3; step <= length; step += 3) {
            Vec3 at = from.add(facing.scale(step));
            Integer h = groundAt(level, Mth.floor(at.x), Mth.floor(at.z));
            if ((h == null || Math.abs(h - y) > 4) && ++misses > 1) {
                return false;
            }
        }
        return true;
    }

    static Predicate<List<String>> filter() {
        List<? extends String> allow = VoidConfig.VEHICLE_ALLOWED.get();
        List<? extends String> deny = VoidConfig.VEHICLE_DENIED.get();
        if (allow != cachedAllow || deny != cachedDeny) {
            allowPatterns = compile(allow);
            denyPatterns = compile(deny);
            cachedAllow = allow;
            cachedDeny = deny;
        }
        List<Pattern> allowed = allowPatterns;
        List<Pattern> denied = denyPatterns;
        return keys -> matches(allowed, keys) && !matches(denied, keys);
    }

    private static boolean matches(List<Pattern> patterns, List<String> keys) {
        for (Pattern pattern : patterns) {
            for (String key : keys) {
                if (pattern.matcher(key).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<Pattern> compile(List<? extends String> globs) {
        List<Pattern> patterns = new ArrayList<>();
        for (String glob : globs) {
            StringBuilder regex = new StringBuilder();
            for (char c : glob.trim().toLowerCase().toCharArray()) {
                if (c == '*') {
                    regex.append(".*");
                } else if (c == '?') {
                    regex.append('.');
                } else {
                    regex.append(Pattern.quote(String.valueOf(c)));
                }
            }
            patterns.add(Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE));
        }
        return patterns;
    }
}
