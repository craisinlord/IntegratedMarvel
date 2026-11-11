package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Marvel Legacy's Spider-Man crawl probes (SpiderPowers.wallDetection, ceilingDetection, nextCeilingDetection,
 * getForwardCeilingHeight, getCurrentCeilingHeight) rewritten in the player's gravity frame. With s = 1 they match the
 * originals exactly; with flipped gravity (s = -1) "up" is world-down and the player's yaw points along a mirrored X.
 *
 * <p>Anthology's flipped frame is a half turn about Z: a player-frame offset (x, y, z) is (-x, -y, z) in the world.
 * Heights are returned as {@code getY() + distance along the player's up}, so Marvel Legacy's own arithmetic
 * ({@code target - getY()}) yields player-frame offsets. The setPos/AABB.move calls that consume them are converted
 * back to world space in {@code MarvelLegacySpiderGravityMixin}.
 */
public final class SpiderCrawlFrame {
    private static final double[] CEILING_OFFSETS = {1.8D, 1.9D, 2.0D, 2.1D, 2.2D};

    private SpiderCrawlFrame() {
    }

    /** World X of the player's forward (player frame yaw), per unit. */
    private static double forwardX(Player player, int s) {
        return s * -Math.sin((float) Math.toRadians(player.yHeadRot));
    }

    private static double forwardZ(Player player) {
        return Math.cos((float) Math.toRadians(player.yHeadRot));
    }

    /** Distance from the feet to a collision box's player-facing bottom, along the player's up. */
    private static double localBottom(Player player, int s, BlockPos pos, AABB box) {
        return s > 0 ? pos.getY() + box.minY - player.getY() : player.getY() - (pos.getY() + box.maxY);
    }

    /** Converts a world face direction into the player's frame (mirrors X and Y when flipped). */
    private static Direction toPlayerFrame(Direction direction, int s) {
        if (s > 0) {
            return direction;
        }
        return switch (direction) {
            case EAST -> Direction.WEST;
            case WEST -> Direction.EAST;
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            default -> direction;
        };
    }

    public static Direction wallDetection(Player player) {
        int s = HeroGravity.up(player);
        Level level = player.level();
        Vec3 start = player.getEyePosition().add(0.0D, -1.2D * s, 0.0D);
        Vec3 end = start.add(forwardX(player, s) * 0.6D, 0.0D, forwardZ(player) * 0.6D);
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? toPlayerFrame(hit.getDirection(), s) : null;
    }

    public static boolean ceilingDetection(Player player, double distance) {
        int s = HeroGravity.up(player);
        double height = player.getBbHeight();
        double halfWidth = player.getBbWidth() / 2.0F;
        double centerX = player.getX() - forwardX(player, s) * distance;
        double centerZ = player.getZ() - forwardZ(player) * distance;
        for (double offset : CEILING_OFFSETS) {
            if (anyBox(player, s, BlockPos.containing(player.getX(), player.getY() + offset * s, player.getZ()),
                    centerX, centerZ, halfWidth, height - 0.1D)) {
                return true;
            }
        }
        return false;
    }

    public static boolean nextCeilingDetection(Player player) {
        int s = HeroGravity.up(player);
        double height = player.getBbHeight();
        double halfWidth = player.getBbWidth() / 2.0F;
        for (double offset = height + 0.1D; offset <= height + 2.0D; offset += 0.1D) {
            if (anyBox(player, s, BlockPos.containing(player.getX(), player.getY() + offset * s, player.getZ()),
                    player.getX(), player.getZ(), halfWidth, height)) {
                return true;
            }
        }
        return false;
    }

    public static double forwardCeilingHeight(Player player) {
        int s = HeroGravity.up(player);
        double checkX = player.getX() + forwardX(player, s) * 0.38D;
        double checkZ = player.getZ() + forwardZ(player) * 0.38D;
        for (double offset = 0.1D; offset <= 3.0D; offset += 0.1D) {
            double found = firstBottom(player, s, BlockPos.containing(checkX, player.getY() + offset * s, checkZ), false, 0.0D);
            if (found != Double.MAX_VALUE) {
                return found;
            }
        }
        return Double.MAX_VALUE;
    }

    public static double currentCeilingHeight(Player player) {
        int s = HeroGravity.up(player);
        double height = player.getBbHeight();
        for (double offset = height + 0.1D; offset <= height + 2.5D; offset += 0.1D) {
            double found = firstBottom(player, s, BlockPos.containing(player.getX(), player.getY() + offset * s, player.getZ()), true, height);
            if (found != Double.MAX_VALUE) {
                return found;
            }
        }
        return Double.MAX_VALUE;
    }

    private static boolean anyBox(Player player, int s, BlockPos pos, double centerX, double centerZ, double halfWidth, double minBottom) {
        VoxelShape shape = shapeAt(player, pos);
        if (shape == null) {
            return false;
        }
        double minX = centerX - halfWidth + 0.1D;
        double maxX = centerX + halfWidth - 0.1D;
        double minZ = centerZ - halfWidth + 0.1D;
        double maxZ = centerZ + halfWidth - 0.1D;
        for (AABB box : shape.toAabbs()) {
            boolean overlapsX = pos.getX() + box.maxX > minX && pos.getX() + box.minX < maxX;
            boolean overlapsZ = pos.getZ() + box.maxZ > minZ && pos.getZ() + box.minZ < maxZ;
            if (overlapsX && overlapsZ && localBottom(player, s, pos, box) >= minBottom) {
                return true;
            }
        }
        return false;
    }

    /** First box bottom at this block past the threshold, as getY() plus its distance along the player's up. */
    private static double firstBottom(Player player, int s, BlockPos pos, boolean inclusive, double threshold) {
        VoxelShape shape = shapeAt(player, pos);
        if (shape == null) {
            return Double.MAX_VALUE;
        }
        for (AABB box : shape.toAabbs()) {
            double bottom = localBottom(player, s, pos, box);
            if (inclusive ? bottom >= threshold : bottom > threshold) {
                return player.getY() + bottom;
            }
        }
        return Double.MAX_VALUE;
    }

    private static VoxelShape shapeAt(Player player, BlockPos pos) {
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        return shape.isEmpty() ? null : shape;
    }
}
