package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HeroGrapple {
    private static final Map<UUID, Line> LINES = new HashMap<>();
    private static final Set<UUID> SAFE_FALL = new HashSet<>();

    private HeroGrapple() {
    }

    private record Line(Vec3 anchor, int ticksLeft, ParticleOptions particle) {
    }

    public static Vec3 anchor(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : null;
    }

    public static void start(ServerPlayer player, Vec3 anchor, ParticleOptions particle) {
        LINES.put(player.getUUID(), new Line(anchor, 40, particle));
        SAFE_FALL.add(player.getUUID());
    }

    public static boolean isPulling(ServerPlayer player) {
        return LINES.containsKey(player.getUUID());
    }

    public static boolean protectsFall(ServerPlayer player) {
        return SAFE_FALL.contains(player.getUUID());
    }

    public static void cut(ServerPlayer player) {
        LINES.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        Line line = LINES.get(player.getUUID());
        if (line == null) {
            if (SAFE_FALL.contains(player.getUUID()) && (player.onGround() || player.isInWater() || player.getAbilities().flying)) {
                SAFE_FALL.remove(player.getUUID());
            }
            return;
        }
        Vec3 hand = player.getEyePosition().subtract(0.0D, 0.4D, 0.0D);
        Vec3 to = line.anchor().subtract(hand);
        if (to.length() < 2.0D || line.ticksLeft() <= 0 || player.isPassenger()) {
            LINES.remove(player.getUUID());
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 0.5D, Math.max(motion.y, 0.3D), motion.z * 0.5D);
            player.hurtMarked = true;
            return;
        }
        Vec3 pull = to.normalize().scale(Math.min(1.6D, 0.6D + to.length() * 0.08D));
        player.setDeltaMovement(pull.x, pull.y + 0.08D, pull.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        if (player.tickCount % 2 == 0) {
            HeroAbilities.particleLine(player.serverLevel(), hand, line.anchor(), line.particle(), 0.6D);
        }
        LINES.put(player.getUUID(), new Line(line.anchor(), line.ticksLeft() - 1, line.particle()));
    }

    public static void clear(ServerPlayer player) {
        LINES.remove(player.getUUID());
        SAFE_FALL.remove(player.getUUID());
    }
}
