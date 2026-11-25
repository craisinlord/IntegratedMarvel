package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class HeroTeleportSafety {
    private static final Map<UUID, Long> FAILED_AT = ServerState.map(new HashMap<>());
    private static final Map<Integer, List<BlockPos>> OFFSETS = new HashMap<>();

    private HeroTeleportSafety() {
    }

    public record Policy(double range, int horizontalSearch, int verticalSearch, boolean allowMidair, boolean allowWater,
                         double floorSnap, int failureCooldownTicks) {
        public Policy withRange(double newRange) {
            return new Policy(newRange, horizontalSearch, verticalSearch, allowMidair, allowWater, floorSnap, failureCooldownTicks);
        }
    }

    public static boolean throttled(LivingEntity player, Policy policy) {
        Long failed = FAILED_AT.get(player.getUUID());
        return failed != null && player.getServer().overworld().getGameTime() - failed < policy.failureCooldownTicks();
    }

    public static void failed(LivingEntity player) {
        FAILED_AT.put(player.getUUID(), player.getServer().overworld().getGameTime());
    }

    public static void forget(LivingEntity player) {
        FAILED_AT.remove(player.getUUID());
    }

    public static void clearAll() {
        FAILED_AT.clear();
    }

    public static boolean canStart(LivingEntity player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isVehicle() && !player.isSleeping()
                && !(player instanceof ServerPlayer serverPlayer && serverPlayer.isChangingDimension());
    }

    public static Optional<Vec3> aimed(LivingEntity player, Policy policy) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(policy.range()));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 desired;
        if (hit.getType() == HitResult.Type.MISS) {
            desired = end.subtract(0.0D, player.getEyeHeight(), 0.0D);
        } else {
            Vec3 at = hit.getLocation();
            Direction face = hit.getDirection();
            double halfWidth = player.getBbWidth() * 0.5D + 0.05D;
            desired = switch (face) {
                case UP -> at;
                case DOWN -> at.subtract(0.0D, player.getBbHeight() + 0.05D, 0.0D);
                default -> at.add(face.getStepX() * halfWidth, -player.getEyeHeight() * 0.5D, face.getStepZ() * halfWidth);
            };
        }
        if (desired.distanceTo(player.position()) < 1.0D) {
            return Optional.empty();
        }
        return near(player, desired, player.position(), policy);
    }

    public static Optional<Vec3> near(Entity entity, Vec3 desired, Vec3 from, Policy policy) {
        Vec3 snappedDesired = snap(entity, desired, policy.floorSnap());
        if (accept(entity, snappedDesired, from, policy)) {
            return Optional.of(snappedDesired);
        }
        BlockPos center = BlockPos.containing(desired);
        for (BlockPos offset : offsets(policy.horizontalSearch(), policy.verticalSearch())) {
            if (offset.equals(BlockPos.ZERO)) {
                continue;
            }
            BlockPos cell = center.offset(offset);
            Vec3 candidate = snap(entity, new Vec3(cell.getX() + 0.5D, cell.getY(), cell.getZ() + 0.5D), policy.floorSnap());
            if (accept(entity, candidate, from, policy)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static boolean accept(Entity entity, Vec3 feet, Vec3 from, Policy policy) {
        if (feet.distanceTo(from) > policy.range() + 2.0D) {
            return false;
        }
        if (!fits(entity, feet, policy.allowWater())) {
            return false;
        }
        if (!policy.allowMidair() && !supported(entity, feet)) {
            return false;
        }
        return clearPath(entity, from, feet);
    }

    public static boolean fits(Entity entity, Vec3 feet, boolean allowWater) {
        Level level = entity.level();
        AABB box = entity.getBoundingBox().move(feet.subtract(entity.position()));
        if (box.minY < level.getMinBuildHeight() + 1 || box.maxY > level.getMaxBuildHeight()) {
            return false;
        }
        if (!level.getWorldBorder().isWithinBounds(box)) {
            return false;
        }
        if (!level.hasChunksAt(Mth.floor(box.minX) - 1, Mth.floor(box.minZ) - 1, Mth.floor(box.maxX) + 1, Mth.floor(box.maxZ) + 1)) {
            return false;
        }
        if (!level.noCollision(entity, box)) {
            return false;
        }
        if (!allowWater && level.containsAnyLiquid(box.inflate(0.0D, 0.2D, 0.0D))) {
            return false;
        }
        AABB around = box.inflate(0.05D, 0.0D, 0.05D).expandTowards(0.0D, -0.6D, 0.0D);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(around.minX, around.minY, around.minZ),
                BlockPos.containing(around.maxX, around.maxY, around.maxZ))) {
            if (hazard(level.getBlockState(pos))) {
                return false;
            }
        }
        return true;
    }

    private static boolean hazard(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        return state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES) || state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.COBWEB) || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_GATEWAY) || state.is(Blocks.POINTED_DRIPSTONE);
    }

    public static boolean supported(Entity entity, Vec3 feet) {
        AABB box = entity.getBoundingBox().move(feet.subtract(entity.position()));
        AABB below = new AABB(box.minX + 0.01D, box.minY - 0.1D, box.minZ + 0.01D, box.maxX - 0.01D, box.minY, box.maxZ - 0.01D);
        return !entity.level().noCollision(entity, below);
    }

    public static boolean clearPath(Entity entity, Vec3 fromFeet, Vec3 toFeet) {
        Level level = entity.level();
        double[] heights = {entity.getEyeHeight(), entity.getBbHeight() * 0.35D};
        for (double height : heights) {
            Vec3 start = fromFeet.add(0.0D, height, 0.0D);
            Vec3 end = toFeet.add(0.0D, height, 0.0D);
            if (start.distanceToSqr(end) < 1.0E-4D) {
                continue;
            }
            HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            if (hit.getType() != HitResult.Type.MISS) {
                return false;
            }
        }
        return true;
    }

    private static Vec3 snap(Entity entity, Vec3 feet, double maxDrop) {
        if (maxDrop <= 0.0D) {
            return feet;
        }
        Level level = entity.level();
        Vec3 top = feet.add(0.0D, 0.5D, 0.0D);
        Vec3 bottom = feet.subtract(0.0D, maxDrop, 0.0D);
        BlockHitResult hit = level.clip(new ClipContext(top, bottom, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP) {
            return new Vec3(feet.x, hit.getLocation().y, feet.z);
        }
        return feet;
    }

    private static List<BlockPos> offsets(int horizontal, int vertical) {
        return OFFSETS.computeIfAbsent(horizontal * 100 + vertical, ignored -> {
            List<BlockPos> list = new ArrayList<>();
            for (int x = -horizontal; x <= horizontal; x++) {
                for (int y = -vertical; y <= vertical; y++) {
                    for (int z = -horizontal; z <= horizontal; z++) {
                        list.add(new BlockPos(x, y, z));
                    }
                }
            }
            list.sort(Comparator.comparingDouble(offset -> offset.getX() * offset.getX() + offset.getZ() * offset.getZ()
                    + offset.getY() * offset.getY() * (offset.getY() < 0 ? 1.5D : 0.8D)));
            return List.copyOf(list);
        });
    }

    public static boolean move(LivingEntity player, Vec3 feet, float yRot, float xRot, Policy policy) {
        if (!canStart(player)) {
            return false;
        }
        EntityTeleportEvent event = NeoForge.EVENT_BUS.post(new EntityTeleportEvent(player, feet.x, feet.y, feet.z));
        if (event.isCanceled()) {
            return false;
        }
        Vec3 target = event.getTarget();
        if (target.distanceToSqr(feet) > 1.0E-4D && (!fits(player, target, policy.allowWater())
                || target.distanceTo(player.position()) > policy.range() + 2.0D)) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.teleportTo(serverPlayer.serverLevel(), target.x, target.y, target.z, yRot, xRot);
        } else {
            player.teleportTo(target.x, target.y, target.z);
            player.setYRot(yRot);
            player.setXRot(xRot);
            player.setYHeadRot(yRot);
        }
        return player.position().distanceToSqr(target) < 0.01D;
    }
}
