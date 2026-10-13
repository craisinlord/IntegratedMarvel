package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.anthology.beam.BeamController;
import com.craisinlord.anthology.beam.BeamSettings;
import com.craisinlord.anthology.beam.BeamTerrainMode;
import com.craisinlord.integrated_superheroes.gauntlet.MindBeamStatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HeroBeams {
    public static final int KING_FIRE = 1;
    public static final int QUEEN_RED = 3;
    private static final Map<UUID, Map<Integer, Active>> BEAMS = new HashMap<>();

    private HeroBeams() {
    }

    public record Spec(int renderKind, BeamSettings settings, BeamTerrainMode terrain, float yawOffset,
                       double forwardOffset, double downOffset, int fireSeconds, float wobble) {
    }

    private static final class Active {
        private final BeamController controller;
        private final Spec spec;
        private final HeroAdvancements.Cast cast = HeroAdvancements.current();
        private int ticksLeft;
        private int age;

        private Active(BeamController controller, Spec spec) {
            this.controller = controller;
            this.spec = spec;
            this.ticksLeft = spec.settings().durationTicks();
        }
    }

    public static BeamSettings settings(double range, int durationTicks, float pathRadius, float impactRadius, double damage,
                                             float knockback, int terrainInterval, double terrainRadius, double dropChance,
                                             double maxResistance) {
        return new BeamSettings(range, 0.0D, 1.5D, durationTicks, 1, 1, pathRadius, impactRadius, (float) damage,
                knockback, terrainInterval, range, (float) (terrainRadius * 0.5D), (float) terrainRadius, 6, 1.0F,
                (float) dropChance, (float) maxResistance, terrainRadius > 0.0D, false);
    }

    public static void fire(ServerPlayer player, int channel, Spec spec) {
        stop(player, channel);
        BEAMS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).put(channel, new Active(new BeamController(player), spec));
    }

    public static boolean isFiring(ServerPlayer player, int channel) {
        Map<Integer, Active> beams = BEAMS.get(player.getUUID());
        return beams != null && beams.containsKey(channel);
    }

    public static void stop(ServerPlayer player, int channel) {
        Map<Integer, Active> beams = BEAMS.get(player.getUUID());
        if (beams == null) {
            return;
        }
        Active active = beams.remove(channel);
        if (active != null) {
            active.controller.stop();
            broadcast(player, channel, active.spec.renderKind(), false, Vec3.ZERO, Vec3.ZERO, 0);
        }
        if (beams.isEmpty()) {
            BEAMS.remove(player.getUUID());
        }
    }

    public static void stopAll(ServerPlayer player) {
        Map<Integer, Active> beams = BEAMS.get(player.getUUID());
        if (beams != null) {
            for (Integer channel : beams.keySet().toArray(Integer[]::new)) {
                stop(player, channel);
            }
        }
    }

    public static void tick(ServerPlayer player) {
        Map<Integer, Active> beams = BEAMS.get(player.getUUID());
        if (beams == null) {
            return;
        }
        if (!player.isAlive()) {
            stopAll(player);
            return;
        }
        for (Map.Entry<Integer, Active> entry : new ArrayList<>(beams.entrySet())) {
            int channel = entry.getKey();
            Active active = entry.getValue();
            Spec spec = active.spec;
            if (active.ticksLeft <= 0) {
                stop(player, channel);
                continue;
            }
            Vec3 start = origin(player, spec);
            Vec3 direction = direction(player, spec);
            active.controller.start(start, start.add(direction.scale(spec.settings().range())), active.ticksLeft);
            HeroAdvancements.within(active.cast, () -> HeroAbilities.runAsAbilityDamage(() ->
                    active.controller.tick(start, direction, null, spec.settings(), spec.terrain(), ignored -> {})));
            active.ticksLeft--;
            active.age++;
            Vec3 end = active.controller.beamEndPosition();
            if (end == null) {
                stop(player, channel);
                continue;
            }
            broadcast(player, channel, spec.renderKind(), true, start, end, active.age);
            if (spec.fireSeconds() > 0) {
                burn(player, start, end, spec);
            }
        }
    }

    private static void burn(ServerPlayer player, Vec3 start, Vec3 end, Spec spec) {
        ServerLevel level = player.serverLevel();
        AABB area = new AABB(start, end).inflate(spec.settings().impactDamageRadius());
        Vec3 path = end.subtract(start);
        double lengthSqr = Math.max(1.0E-4D, path.lengthSqr());
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player && entity.isAlive())) {
            Vec3 center = target.getBoundingBox().getCenter();
            double t = Math.max(0.0D, Math.min(1.0D, center.subtract(start).dot(path) / lengthSqr));
            Vec3 closest = start.add(path.scale(t));
            double radius = t >= 0.99D ? spec.settings().impactDamageRadius() : spec.settings().pathDamageRadius() + 0.5D;
            if (closest.distanceToSqr(center) <= radius * radius) {
                target.igniteForSeconds(spec.fireSeconds());
            }
        }
        level.sendParticles(ParticleTypes.FLAME, end.x, end.y, end.z, 6, 0.3D, 0.3D, 0.3D, 0.02D);
        if (spec.terrain() == BeamTerrainMode.DESTROY && player.tickCount % 5 == 0
                && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            BlockPos firePos = BlockPos.containing(end);
            if (level.getBlockState(firePos).isAir() && BaseFireBlock.canBePlacedAt(level, firePos, player.getDirection())) {
                level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
            }
        }
    }

    private static Vec3 origin(ServerPlayer player, Spec spec) {
        return player.getEyePosition().add(player.getLookAngle().scale(spec.forwardOffset())).subtract(0.0D, spec.downOffset(), 0.0D);
    }

    private static Vec3 direction(ServerPlayer player, Spec spec) {
        float yaw = spec.yawOffset();
        float pitch = 0.0F;
        if (spec.wobble() > 0.0F) {
            yaw += (player.getRandom().nextFloat() - 0.5F) * spec.wobble();
            pitch += (player.getRandom().nextFloat() - 0.5F) * spec.wobble();
        }
        return Vec3.directionFromRotation(player.getXRot() + pitch, player.getYRot() + yaw);
    }

    private static void broadcast(ServerPlayer owner, int channel, int kind, boolean active, Vec3 start, Vec3 end, int age) {
        int key = -(owner.getId() * 16 + channel + 1);
        MindBeamStatePayload payload = new MindBeamStatePayload(active, kind, key, start.x, start.y, start.z, end.x, end.y, end.z, age);
        for (ServerPlayer viewer : owner.serverLevel().players()) {
            if (viewer.distanceToSqr(owner) <= 192.0D * 192.0D) {
                PacketDistributor.sendToPlayer(viewer, payload);
            }
        }
    }
}
