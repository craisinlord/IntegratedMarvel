package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public final class GrootBranches {
    public static final int STRIKE = 0;
    public static final int HOLD = 1;
    public static final int PULL = 2;
    public static final int CLEAR = 3;
    private static final Map<Integer, Branch> BRANCHES = new HashMap<>();

    private GrootBranches() {
    }

    public record Branch(int mode, int targetId, Vec3 point, long start, int ticks) {
    }

    public static void send(ServerPlayer player, int mode, Entity target, Vec3 point, int ticks) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new GrootPayload(player.getId(), target == null ? -1 : target.getId(), mode,
                (float) point.x, (float) point.y, (float) point.z, ticks));
    }

    public static void clear(ServerPlayer player) {
        send(player, CLEAR, null, player.position(), 0);
    }

    static void receive(GrootPayload payload, long now) {
        if (payload.mode() == CLEAR || payload.ticks() <= 0) {
            BRANCHES.remove(payload.entityId());
            return;
        }
        BRANCHES.put(payload.entityId(), new Branch(payload.mode(), payload.targetId(), new Vec3(payload.x(), payload.y(), payload.z()), now, payload.ticks()));
    }

    public static Branch get(Player player) {
        Branch branch = BRANCHES.get(player.getId());
        if (branch == null) {
            return null;
        }
        if (player.level().getGameTime() - branch.start() > branch.ticks()) {
            BRANCHES.remove(player.getId());
            return null;
        }
        return branch;
    }

    public static float extension(Branch branch, Player player, float partial) {
        float t = Mth.clamp((player.level().getGameTime() - branch.start() + partial) / Math.max(1, branch.ticks()), 0.0F, 1.0F);
        return switch (branch.mode()) {
            case STRIKE -> Mth.sin(t * Mth.PI);
            case PULL -> 1.0F - t;
            default -> Mth.clamp(t * 4.0F, 0.0F, 1.0F);
        };
    }

    public static Vec3 point(Branch branch, Player player, float partial) {
        if (branch.targetId() >= 0) {
            Entity target = player.level().getEntity(branch.targetId());
            if (target != null && target.isAlive()) {
                return target.getPosition(partial).add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            }
        }
        return branch.point();
    }

    public static Vec3 shoulder(Player player, float partial) {
        float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double scale = player.getScale();
        Vec3 side = new Vec3(-Mth.cos(yaw), 0.0D, -Mth.sin(yaw)).scale(0.33D * scale);
        return player.getPosition(partial).add(0.0D, player.getBbHeight() * 0.76D, 0.0D).add(side);
    }
}
