package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class SwarmSquirrelNavigation extends GroundPathNavigation {
    private static final double MIN_GLIDE_DISTANCE = 10.0D;
    private static final double MAX_GLIDE_DISTANCE = 64.0D;
    private static final double MIN_GLIDE_HEIGHT = 3.0D;
    private static final int LAUNCH_EDGE_SEARCH_RADIUS = 7;
    private static final int REPATH_INTERVAL = 10;

    private final SwarmSquirrel squirrel;
    @Nullable
    private BlockPos requestedTarget;
    @Nullable
    private BlockPos adjustedTarget;
    @Nullable
    private BlockPos launchStandPos;
    @Nullable
    private BlockPos pathToPosition;
    private double requestedSpeed;
    private boolean glidingToLaunch;
    private boolean glideStarted;
    private int repathDelay;
    private int repathFailStreak;

    public SwarmSquirrelNavigation(SwarmSquirrel squirrel, Level level) {
        super(squirrel, level);
        this.squirrel = squirrel;
        this.setCanFloat(true);
    }

    boolean requestTravelTo(BlockPos target, double speed) {
        this.requestedTarget = target.immutable();
        this.requestedSpeed = speed;
        this.repathDelay = 0;
        this.adjustedTarget = this.adjustNavigationTarget(this.requestedTarget);
        this.launchStandPos = null;
        this.glidingToLaunch = false;
        this.glideStarted = false;
        if (!this.squirrel.isGliding() && this.squirrel.onGround()) {
            BlockPos launch = this.findLaunchPos(this.adjustedTarget);
            if (launch != null
                    && !this.squirrel.isRecentGlidePath(Vec3.atBottomCenterOf(launch), this.adjustedTarget)
                    && this.shouldGlideBetween(Vec3.atBottomCenterOf(launch), this.adjustedTarget)
                    && this.hasLaunchEdgeToward(launch, this.adjustedTarget)) {
                this.launchStandPos = launch;
                this.glidingToLaunch = true;
            }
        }
        return this.beginGroundOrLaunchMove();
    }

    @Override
    public boolean moveTo(double x, double y, double z, double speed) {
        return this.requestTravelTo(BlockPos.containing(x, y, z), speed);
    }

    @Override
    public boolean moveTo(Entity entity, double speed) {
        Path path = this.createPath(entity, 0);
        if (path != null) {
            return this.moveTo(path, speed);
        }
        this.pathToPosition = entity.blockPosition().immutable();
        this.requestedTarget = this.pathToPosition;
        this.adjustedTarget = this.adjustNavigationTarget(this.pathToPosition);
        this.requestedSpeed = speed;
        this.launchStandPos = null;
        this.glidingToLaunch = false;
        this.glideStarted = false;
        return true;
    }

    @Override
    @Nullable
    public Path createPath(BlockPos pos, int accuracy) {
        this.pathToPosition = pos.immutable();
        return super.createPath(this.adjustNavigationTarget(pos), accuracy);
    }

    @Override
    @Nullable
    public Path createPath(Entity entity, int accuracy) {
        this.pathToPosition = entity.blockPosition().immutable();
        return super.createPath(entity, accuracy);
    }

    @Override
    public boolean isStableDestination(BlockPos pos) {
        return SwarmSquirrel.isOpenStandPos(this.level, pos) || this.isTransitionPos(pos) || super.isStableDestination(pos);
    }

    @Override
    protected boolean canUpdatePath() {
        return !this.squirrel.isGliding();
    }

    @Override
    public void stop() {
        super.stop();
        this.clearTraversal();
    }

    void cancelTraversal() {
        super.stop();
        this.clearTraversal();
    }

    @Override
    public void tick() {
        if (this.requestedTarget == null) {
            super.tick();
            return;
        }
        if (this.squirrel.isGliding()) {
            this.glideStarted = true;
            return;
        }
        if (this.glideStarted) {
            this.glideStarted = false;
            this.glidingToLaunch = false;
            this.launchStandPos = null;
            if (this.adjustedTarget != null && !this.squirrel.isAtStandPos(this.adjustedTarget)) {
                this.beginGroundOrLaunchMove();
            } else {
                this.clearTraversal();
            }
            return;
        }
        if (this.glidingToLaunch && this.launchStandPos != null && this.adjustedTarget != null) {
            this.tickGlideApproach();
            return;
        }
        this.tickGroundNav();
        if (this.adjustedTarget != null && this.squirrel.isAtStandPos(this.adjustedTarget)) {
            this.clearTraversal();
            return;
        }
        if (this.repathDelay > 0) {
            this.repathDelay--;
        }
        if (this.repathDelay <= 0 && (this.isDone() || this.getPath() == null)) {
            this.beginGroundOrLaunchMove();
        }
    }

    private void tickGlideApproach() {
        BlockPos launch = this.launchStandPos;
        if (launch == null || this.adjustedTarget == null) {
            this.glidingToLaunch = false;
            this.beginGroundOrLaunchMove();
            return;
        }
        if (this.squirrel.isAtStandPos(launch)) {
            if (this.hasLaunchEdgeToward(launch, this.adjustedTarget) && this.shouldGlideBetween(Vec3.atBottomCenterOf(launch), this.adjustedTarget)) {
                this.squirrel.startGliding(this.adjustedTarget, false);
                this.glideStarted = true;
                return;
            }
            this.glidingToLaunch = false;
            this.launchStandPos = null;
            this.beginGroundOrLaunchMove();
            return;
        }
        this.tickGroundNav();
        if (this.repathDelay > 0) {
            this.repathDelay--;
        }
        if (this.repathDelay <= 0 && (this.isDone() || this.getPath() == null)) {
            this.tryStartMoveToward(launch, this.requestedSpeed);
        }
    }

    private boolean beginGroundOrLaunchMove() {
        BlockPos target = this.glidingToLaunch && this.launchStandPos != null ? this.launchStandPos : this.adjustedTarget;
        return target != null && this.tryStartMoveToward(target, this.requestedSpeed);
    }

    private void tickGroundNav() {
        super.tick();
        this.tickPathToPositionFallback();
    }

    private boolean tryStartMoveToward(BlockPos target, double speed) {
        this.pathToPosition = target.immutable();
        Path path = super.createPath(target, 0);
        if (path != null && super.moveTo(path, speed)) {
            this.repathFailStreak = 0;
            this.repathDelay = REPATH_INTERVAL;
            return true;
        }
        this.repathFailStreak++;
        this.repathDelay = REPATH_INTERVAL * Math.min(this.repathFailStreak, 4);
        this.speedModifier = speed;
        return true;
    }

    private void tickPathToPositionFallback() {
        if (!this.isDone() || this.pathToPosition == null) {
            return;
        }
        double threshold = Math.max(this.mob.getBbWidth(), 1.0F);
        if (this.pathToPosition.closerToCenterThan(this.mob.position(), threshold)
                || this.mob.getY() > this.pathToPosition.getY()
                && BlockPos.containing(this.pathToPosition.getX(), this.mob.getY(), this.pathToPosition.getZ()).closerToCenterThan(this.mob.position(), threshold)) {
            this.pathToPosition = null;
            return;
        }
        this.mob.getMoveControl().setWantedPosition(this.pathToPosition.getX() + 0.5D, this.pathToPosition.getY(), this.pathToPosition.getZ() + 0.5D, this.speedModifier);
    }

    private BlockPos adjustNavigationTarget(BlockPos desired) {
        if (SwarmSquirrel.isOpenStandPos(this.level, desired) || this.isTransitionPos(desired)) {
            return desired.immutable();
        }
        BlockPos adjusted = this.findNearbyStandPos(desired, 4, 4);
        return adjusted != null ? adjusted : desired.immutable();
    }

    private boolean isTransitionPos(BlockPos pos) {
        return SwarmSquirrel.isStandableSurface(this.level.getBlockState(pos.below())) && SwarmSquirrel.hasNearbyCanopy(this.level, pos);
    }

    @Nullable
    private BlockPos findNearbyStandPos(BlockPos target, int horizontalRadius, int verticalRadius) {
        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (BlockPos candidate : BlockPos.betweenClosed(target.offset(-horizontalRadius, -verticalRadius, -horizontalRadius),
                target.offset(horizontalRadius, verticalRadius, horizontalRadius))) {
            if (!SwarmSquirrel.isOpenStandPos(this.level, candidate)) {
                continue;
            }
            double score = -Vec3.atBottomCenterOf(candidate).distanceToSqr(Vec3.atBottomCenterOf(target));
            if (SwarmSquirrel.isAdjacentToLog(this.level, candidate)) {
                score += 6.0D;
            }
            if (SwarmSquirrel.hasNearbyCanopy(this.level, candidate)) {
                score += 4.0D;
            }
            if (candidate.getY() > target.getY()) {
                score += (candidate.getY() - target.getY()) * 0.35D;
            }
            if (score > bestScore) {
                bestScore = score;
                best = candidate.immutable();
            }
        }
        return best;
    }

    private boolean shouldGlideBetween(Vec3 from, BlockPos stand) {
        if (this.squirrel.isGliding()) {
            return false;
        }
        double dx = stand.getX() + 0.5D - from.x;
        double dz = stand.getZ() + 0.5D - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double drop = from.y - (stand.getY() + 0.5D);
        return horizontal >= MIN_GLIDE_DISTANCE && horizontal <= MAX_GLIDE_DISTANCE && drop >= MIN_GLIDE_HEIGHT;
    }

    @Nullable
    private BlockPos findLaunchPos(BlockPos target) {
        BlockPos origin = this.squirrel.blockPosition();
        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-LAUNCH_EDGE_SEARCH_RADIUS, -2, -LAUNCH_EDGE_SEARCH_RADIUS),
                origin.offset(LAUNCH_EDGE_SEARCH_RADIUS, 2, LAUNCH_EDGE_SEARCH_RADIUS))) {
            if (!SwarmSquirrel.isOpenStandPos(this.level, candidate) || !this.hasLaunchEdgeToward(candidate, target)) {
                continue;
            }
            boolean here = this.squirrel.isAtStandPos(candidate);
            Vec3 launch = Vec3.atBottomCenterOf(candidate);
            if (!here && launch.distanceToSqr(this.squirrel.position()) > 36.0D || this.squirrel.isRecentGlidePath(launch, target)) {
                continue;
            }
            double glideScore = this.squirrel.scoreGlideTargetFrom(launch, target);
            if (glideScore == Double.NEGATIVE_INFINITY) {
                continue;
            }
            double score = glideScore - launch.distanceTo(this.squirrel.position()) * 1.75D + (here ? 3.0D : 0.0D);
            if (score > bestScore) {
                bestScore = score;
                best = candidate.immutable();
            }
        }
        return best;
    }

    private boolean hasLaunchEdgeToward(BlockPos launch, BlockPos target) {
        double dx = target.getX() - launch.getX();
        double dz = target.getZ() - launch.getZ();
        Direction direction = Math.abs(dx) >= Math.abs(dz) ? dx >= 0.0D ? Direction.EAST : Direction.WEST : dz >= 0.0D ? Direction.SOUTH : Direction.NORTH;
        BlockPos edge = launch.relative(direction);
        return SwarmSquirrel.isOpenAir(this.level, edge) && !SwarmSquirrel.isStandableSurface(this.level.getBlockState(edge.below()));
    }

    private void clearTraversal() {
        this.requestedTarget = null;
        this.adjustedTarget = null;
        this.requestedSpeed = 0.0D;
        this.launchStandPos = null;
        this.glidingToLaunch = false;
        this.glideStarted = false;
        this.repathDelay = 0;
        this.repathFailStreak = 0;
        this.pathToPosition = null;
    }
}
