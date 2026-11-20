package com.craisinlord.integrated_superheroes.voidworld.vehicles;

import com.craisinlord.integrated_superheroes.portal.MarvelPortal;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantTears;
import com.craisinlord.integrated_superheroes.compat.AntarchyDimensionalTear;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.mts.MtsVehicles;
import com.craisinlord.integrated_superheroes.gauntlet.GauntletFx;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VoidVariants;
import com.craisinlord.integrated_superheroes.voidworld.VoidWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class VehicleTears {
    static final String TEAR_TAG = "integrated_superheroes_vehicle_tear";
    private static final int OPEN_TICKS = 30;
    private static final int LIFETIME = 90;
    private static final int RUN_TIMEOUT = 220;
    private static final double LIFT = 0.25D;
    private static final double LAUNCH_HEIGHT = 1.2D;
    private static final double CENTER = 1.4D;
    private static final double WIDTH = 1.9D;
    private static final double FAR_RANGE = 160.0D;
    private static final Vector3f COLOR_A = new Vector3f(0.62F, 0.25F, 0.95F);
    private static final Vector3f COLOR_B = new Vector3f(1.0F, 0.72F, 0.25F);
    private static final Map<ResourceKey<Level>, List<Tear>> TEARS = new HashMap<>();
    private static final Set<UUID> LIVE = new HashSet<>();
    private static final Map<UUID, Run> RUNS = new HashMap<>();
    private static final Set<UUID> SETTLED = new HashSet<>();

    private VehicleTears() {
    }

    private static final class Tear {
        final Vec3 ground;
        final Vec3 base;
        final Vec3 facing;
        final long opened;
        long closes;
        final int tier;
        final boolean aircraft;
        final double fuel;
        final boolean driver;
        final boolean launch;
        final List<UUID> visuals = new ArrayList<>();
        boolean emerged;

        Tear(Vec3 ground, Vec3 facing, long opened, int tier, boolean aircraft, double fuel, boolean driver, boolean launch) {
            this.ground = ground;
            this.base = ground.add(0.0D, LIFT + (launch ? 1.0D : 0.0D), 0.0D);
            this.facing = facing;
            this.opened = opened;
            this.closes = opened + LIFETIME + 2L;
            this.tier = tier;
            this.aircraft = aircraft;
            this.fuel = fuel;
            this.driver = driver;
            this.launch = launch;
        }

        Vec3 center() {
            return base.add(0.0D, CENTER, 0.0D);
        }

        Vec3 right() {
            return new Vec3(-facing.z, 0.0D, facing.x);
        }
    }

    private static final class Run {
        final ResourceKey<Level> dimension;
        final Vec3 push;
        final long started;
        final long brakeAt;
        @Nullable
        UUID driver;

        Run(ResourceKey<Level> dimension, Vec3 push, long started, long brakeAt, @Nullable UUID driver) {
            this.dimension = dimension;
            this.push = push;
            this.started = started;
            this.brakeAt = brakeAt;
            this.driver = driver;
        }
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(VehicleTears::onLevelTick);
        NeoForge.EVENT_BUS.addListener(VehicleTears::onJoin);
        NeoForge.EVENT_BUS.addListener(VehicleTears::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(VehicleTears::onServerStopped);
    }

    static int pending(ServerLevel level) {
        List<Tear> list = TEARS.get(level.dimension());
        if (list == null) {
            return 0;
        }
        int count = 0;
        for (Tear tear : list) {
            if (!tear.emerged) {
                count++;
            }
        }
        return count;
    }

    static boolean busy(UUID vehicle) {
        return RUNS.containsKey(vehicle);
    }

    @Nullable
    static Vec3 open(ServerLevel level, ServerPlayer player, int tier, boolean aircraft, double fuel, boolean driver, boolean launch) {
        for (int attempt = 0; attempt < 16; attempt++) {
            Vec3 ground = VoidVehicles.groundSpot(level, player.position(), 16.0D, 30.0D);
            if (ground == null) {
                return null;
            }
            Vec3 toward = new Vec3(player.getX() - ground.x, 0.0D, player.getZ() - ground.z);
            if (toward.lengthSqr() < 1.0E-4D) {
                continue;
            }
            double turn = Math.toRadians((25.0D + level.random.nextDouble() * 25.0D) * (level.random.nextBoolean() ? 1.0D : -1.0D));
            Vec3 facing = toward.normalize().yRot((float) turn);
            if (!VoidVehicles.runway(level, ground, facing, 12)) {
                continue;
            }
            long now = level.getGameTime();
            Tear tear = new Tear(ground, facing, now, tier, aircraft, fuel, driver, launch);
            TEARS.computeIfAbsent(level.dimension(), key -> new ArrayList<>()).add(tear);
            BlockPos at = BlockPos.containing(tear.center());
            if (ModCompat.ANTARCHY) {
                Vec3 right = tear.right();
                visual(level, tear, tear.base.add(right.scale(WIDTH * 0.5D)), facing, LIFETIME);
                visual(level, tear, tear.base.add(right.scale(-WIDTH * 0.5D)), facing, LIFETIME);
            } else {
                level.playSound(null, at, SoundEvents.PORTAL_TRIGGER, SoundSource.NEUTRAL, 0.6F, 0.6F);
            }
            level.playSound(null, at, SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 1.0F, 0.5F);
            return ground;
        }
        return null;
    }

    private static float yaw(Vec3 facing) {
        return (float) Math.toDegrees(Math.atan2(-facing.x, facing.z));
    }

    private static SoundEvent emergeSound() {
        return ModCompat.ANTARCHY ? AntarchyDimensionalTear.emergeSound() : SoundEvents.ENDERMAN_TELEPORT;
    }

    private static void visual(ServerLevel level, Tear tear, Vec3 at, Vec3 facing, int lifetime) {
        Entity entity = AntarchyDimensionalTear.create(level, at, yaw(facing), lifetime);
        entity.addTag(TEAR_TAG);
        LIVE.add(entity.getUUID());
        if (level.addFreshEntity(entity)) {
            tear.visuals.add(entity.getUUID());
        } else {
            LIVE.remove(entity.getUUID());
            entity.discard();
        }
    }

    private static void finish(ServerLevel level, Tear tear) {
        for (UUID id : tear.visuals) {
            LIVE.remove(id);
            Entity entity = level.getEntity(id);
            if (entity != null && entity.isAlive()) {
                entity.discard();
            }
        }
        tear.visuals.clear();
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        List<Tear> list = TEARS.get(level.dimension());
        if (list != null && !list.isEmpty()) {
            for (Tear tear : new ArrayList<>(list)) {
                if (now >= tear.closes) {
                    finish(level, tear);
                    list.remove(tear);
                    continue;
                }
                tickTear(level, tear, now);
            }
        }
        if (!RUNS.isEmpty()) {
            tickRuns(level, now);
        }
        if (now % 20L == 10L && VoidWorld.isVoid(level)) {
            settle(level);
        }
    }

    private static void settle(ServerLevel level) {
        VoidVehicleData data = VoidVehicleData.get(level);
        if (data.tracked.isEmpty()) {
            return;
        }
        if (SETTLED.size() > 512) {
            SETTLED.retainAll(data.tracked.keySet());
        }
        for (UUID id : data.tracked.keySet()) {
            if (SETTLED.contains(id) || RUNS.containsKey(id)) {
                continue;
            }
            VehicleState state = MtsVehicles.state(level, id);
            if (state == null) {
                continue;
            }
            if (!state.playerAboard()) {
                MtsVehicles.park(level, id);
            }
            SETTLED.add(id);
        }
    }

    private static void tickTear(ServerLevel level, Tear tear, long now) {
        if (!tear.emerged && now >= tear.opened + OPEN_TICKS) {
            tear.emerged = true;
            if (!emerge(level, tear, now)) {
                tear.closes = Math.min(tear.closes, now + 12L);
            }
        }
        Vec3 center = tear.center();
        if (!level.hasChunkAt(BlockPos.containing(center)) || now % 2L != 0L) {
            return;
        }
        double grow = Mth.clamp((now - tear.opened) / (double) OPEN_TICKS, 0.15D, 1.0D);
        double shrink = Mth.clamp((tear.closes - now) / 12.0D, 0.0D, 1.0D);
        double size = Math.min(grow, shrink);
        Vec3 right = tear.right();
        if (!ModCompat.ANTARCHY) {
            GauntletFx.farEllipse(level, ParticleTypes.REVERSE_PORTAL, center, right.scale(1.9D * size), 1.5D * size, 24, now * 0.2D, FAR_RANGE);
            GauntletFx.farEllipse(level, ParticleTypes.END_ROD, center, right.scale(1.2D * size), 1.0D * size, 10, -now * 0.3D, FAR_RANGE);
            GauntletFx.farParticles(level, ParticleTypes.PORTAL, center, 8, 0.8D * size, 0.6D * size, 0.8D * size, 0.4D, FAR_RANGE);
            if ((now - tear.opened) % 40L == 0L) {
                level.playSound(null, BlockPos.containing(center), SoundEvents.PORTAL_AMBIENT, SoundSource.NEUTRAL, 0.5F, 0.6F);
            }
        }
        ellipse(level, new DustParticleOptions(COLOR_A, 1.2F), center, right.scale(2.2D * size), 1.6D * size, 18, now * 0.12D);
        if (now % 4L == 0L) {
            ellipse(level, new DustParticleOptions(COLOR_B, 0.9F), center, right.scale(1.6D * size), 1.2D * size, 12, -now * 0.15D);
        }
    }

    private static void ellipse(ServerLevel level, ParticleOptions particle, Vec3 center, Vec3 horizontal, double vertical, int points, double phase) {
        if (ModCompat.ANTARCHY) {
            GauntletFx.ellipse(level, particle, center, horizontal, vertical, points, phase);
        } else {
            GauntletFx.farEllipse(level, particle, center, horizontal, vertical, points, phase, FAR_RANGE);
        }
    }

    private static void burst(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double dx, double dy, double dz, double speed) {
        if (ModCompat.ANTARCHY) {
            level.sendParticles(particle, at.x, at.y, at.z, count, dx, dy, dz, speed);
        } else {
            GauntletFx.farParticles(level, particle, at, count, dx, dy, dz, speed, FAR_RANGE);
        }
    }

    private static boolean emerge(ServerLevel level, Tear tear, long now) {
        if (!level.hasChunkAt(BlockPos.containing(tear.ground))) {
            return false;
        }
        UUID id = MtsVehicles.spawn(level, tear.ground, tear.facing, tear.launch ? LAUNCH_HEIGHT : 0.0D, level.random, VoidVehicles.filter(), tear.aircraft, tear.fuel);
        if (id == null) {
            return false;
        }
        VoidVehicleData.get(level).track(id, now);
        SETTLED.remove(id);
        double speed = 0.45D + level.random.nextDouble() * 0.25D;
        Vec3 push = tear.facing.scale(speed).add(0.0D, tear.launch ? 0.3D : 0.0D, 0.0D);
        MtsVehicles.push(level, id, push);
        long brakeAt = now + (tear.launch ? 30L : 16L) + level.random.nextInt(10);
        Run run = new Run(level.dimension(), new Vec3(push.x, 0.0D, push.z), now, brakeAt, null);
        RUNS.put(id, run);
        if (tear.driver) {
            Vec3 side = tear.ground.add(tear.right().scale(3.0D));
            if (MarvelPortal.enabled()) {
                Vec3 spot = Vec3.atBottomCenterOf(BlockPos.containing(side));
                VariantTears.arrive(level, spot, tear.tier, tear.ground, mob -> {
                    if (RUNS.get(id) == run) {
                        seatDriver(level, id, run, mob);
                    }
                });
            } else {
                seatDriver(level, id, run, VoidVariants.spawn(level, BlockPos.containing(side), tear.tier));
            }
        }
        Vec3 center = tear.center();
        burst(level, new DustParticleOptions(COLOR_A, 1.5F), center, 24, 0.8D, 0.6D, 0.8D, 0.02D);
        burst(level, new DustParticleOptions(COLOR_B, 1.1F), center, 12, 0.8D, 0.6D, 0.8D, 0.02D);
        burst(level, ParticleTypes.REVERSE_PORTAL, tear.ground.add(0.0D, 1.0D, 0.0D), 40, 0.9D, 0.6D, 0.9D, 0.1D);
        level.playSound(null, BlockPos.containing(center), emergeSound(), SoundSource.NEUTRAL, 1.0F, 0.6F);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 0.35F, 1.6F);
        return true;
    }

    private static void seatDriver(ServerLevel level, UUID id, Run run, @Nullable Mob mob) {
        if (mob != null && mob.getBbWidth() <= 1.4F && mob.getBbHeight() <= 2.6F && MtsVehicles.seat(level, id, mob)) {
            run.driver = mob.getUUID();
        }
    }

    private static void tickRuns(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, Run>> it = RUNS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Run> next = it.next();
            Run run = next.getValue();
            if (run.dimension != level.dimension()) {
                continue;
            }
            UUID id = next.getKey();
            VehicleState state = MtsVehicles.state(level, id);
            if (state == null) {
                dismount(level, run);
                it.remove();
                continue;
            }
            if (now - run.started <= 3L && state.speed() < run.push.length() * 0.5D) {
                MtsVehicles.push(level, id, run.push);
            }
            if (state.playerAboard()) {
                dismount(level, run);
                it.remove();
                continue;
            }
            if (now >= run.brakeAt) {
                MtsVehicles.brake(level, id, 0.7D);
            }
            if ((now >= run.brakeAt + 5L && state.speed() < 0.03D) || now - run.started > RUN_TIMEOUT) {
                MtsVehicles.park(level, id);
                SETTLED.add(id);
                dismount(level, run);
                it.remove();
            }
        }
    }

    private static void dismount(ServerLevel level, Run run) {
        if (run.driver == null) {
            return;
        }
        Entity driver = level.getEntity(run.driver);
        run.driver = null;
        if (driver != null && driver.isPassenger()) {
            driver.stopRiding();
        }
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity().getTags().contains(TEAR_TAG) && !LIVE.contains(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<Tear> list = TEARS.remove(level.dimension());
        if (list != null) {
            for (Tear tear : list) {
                tear.visuals.forEach(LIVE::remove);
            }
        }
        RUNS.values().removeIf(run -> run.dimension == level.dimension());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        TEARS.clear();
        LIVE.clear();
        RUNS.clear();
        SETTLED.clear();
    }
}
