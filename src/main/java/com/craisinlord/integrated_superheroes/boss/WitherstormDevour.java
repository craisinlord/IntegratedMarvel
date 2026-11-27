package com.craisinlord.integrated_superheroes.boss;

import com.craisinlord.integrated_superheroes.voidworld.VoidConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

final class WitherstormDevour {
    private static final int FLY_ONE_IN = 10;
    private static final int FLY_MAX_AGE = 200;
    private static final int BREAK_EFFECTS_PER_TICK = 2;

    private final WitherstormEntity storm;
    private final List<FallingBlockEntity> flying = new ArrayList<>();
    private final List<LivingEntity> caught = new ArrayList<>();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private int ticks;

    WitherstormDevour(WitherstormEntity storm) {
        this.storm = storm;
    }

    void tick(ServerLevel level) {
        this.ticks++;
        Vec3 mouth = this.storm.mouth();
        double s = this.storm.scale();
        Vec3 center = new Vec3(mouth.x, this.storm.groundY(), mouth.z);
        boolean engaged = this.storm.getTarget() != null;
        if (engaged || this.ticks % 10 == 0) this.eat(level, center, mouth, s, engaged);
        if (this.ticks % 2 == 0) this.steerFlying(level, mouth, s);
        if (engaged && this.ticks % 10 == 0) this.gather(level, center, mouth, s);
        if (engaged && this.ticks % 2 == 1) this.pull(level, mouth, s);
        if (!engaged) this.caught.clear();
        if (this.ticks % (engaged ? 3 : 12) == 0) this.storm.beam(level, mouth, center, 3);
        if (this.ticks % (engaged ? 4 : 16) == 0) this.swirl(level, s);
        if (this.ticks % 80 == 0) this.storm.playAnim(this.ticks % 160 == 0 ? "devour" : "tractor_beam");
    }

    private static int budget() {
        return VoidConfig.SPEC.isLoaded() ? VoidConfig.STORM_BLOCKS_PER_TICK.get() : 24;
    }

    private static int maxFlying() {
        return VoidConfig.SPEC.isLoaded() ? VoidConfig.STORM_MAX_FLYING.get() : 24;
    }

    private void eat(ServerLevel level, Vec3 center, Vec3 mouth, double s, boolean engaged) {
        int budget = engaged ? budget() : Math.min(budget(), 3);
        if (budget <= 0) return;
        RandomSource random = this.storm.getRandom();
        double radius = Mth.clamp(1.6D * s, 4.0D, 16.0D);
        int columns = Math.max(1, budget / 3);
        int removed = 0;
        int effects = 0;
        int maxFlying = engaged ? maxFlying() : 0;
        for (int i = 0; i < columns && removed < budget; i++) {
            int x = Mth.floor(center.x + (random.nextDouble() + random.nextDouble() - 1.0D) * radius);
            int z = Mth.floor(center.z + (random.nextDouble() + random.nextDouble() - 1.0D) * radius);
            this.cursor.set(x, 0, z);
            if (!level.isLoaded(this.cursor)) continue;
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            if (top <= level.getMinBuildHeight()) continue;
            int bite = 1 + random.nextInt(3);
            for (int d = 0; d < bite && removed < budget; d++) {
                this.cursor.set(x, top - d, z);
                BlockState state = level.getBlockState(this.cursor);
                if (state.isAir()) continue;
                if (!this.storm.breakable(level, this.cursor, state)) break;
                this.storm.rememberEaten(state);
                removed++;
                if (this.flying.size() < maxFlying && random.nextInt(FLY_ONE_IN) == 0 && !state.getCollisionShape(level, this.cursor).isEmpty()) {
                    this.launch(level, this.cursor.immutable(), state, mouth, s);
                    continue;
                }
                level.setBlock(this.cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                if (effects < BREAK_EFFECTS_PER_TICK && random.nextInt(6) == 0) {
                    level.levelEvent(2001, this.cursor, Block.getId(state));
                    effects++;
                }
            }
        }
        if (removed > 0) this.storm.feed(removed);
    }

    private void launch(ServerLevel level, BlockPos pos, BlockState state, Vec3 mouth, double s) {
        FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
        block.setNoGravity(true);
        block.disableDrop();
        this.aim(block, mouth, s);
        this.flying.add(block);
    }

    private void aim(FallingBlockEntity block, Vec3 mouth, double s) {
        Vec3 to = mouth.subtract(block.position());
        double length = to.length();
        if (length < 0.01D) return;
        block.setDeltaMovement(to.scale((0.5D + 0.12D * s) / length));
        block.hasImpulse = true;
        block.hurtMarked = true;
    }

    private void steerFlying(ServerLevel level, Vec3 mouth, double s) {
        double swallow = Math.max(3.0D, 1.5D * s);
        Iterator<FallingBlockEntity> iterator = this.flying.iterator();
        while (iterator.hasNext()) {
            FallingBlockEntity block = iterator.next();
            if (block.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (block.time > FLY_MAX_AGE || block.distanceToSqr(mouth) < swallow * swallow) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, block.getX(), block.getY(), block.getZ(), 3, 0.3D, 0.3D, 0.3D, 0.01D);
                block.discard();
                iterator.remove();
                continue;
            }
            this.aim(block, mouth, s);
        }
    }

    private void gather(ServerLevel level, Vec3 center, Vec3 mouth, double s) {
        this.caught.clear();
        double radius = Mth.clamp(2.5D * s, 8.0D, 28.0D);
        AABB column = new AABB(center.x - radius, center.y - 8.0D, center.z - radius, center.x + radius, mouth.y + 4.0D, center.z + radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, column, this::edible)) {
            this.caught.add(entity);
        }
    }

    private boolean edible(LivingEntity entity) {
        if (!entity.isAlive() || entity instanceof WitherstormEntity) return false;
        if (entity instanceof Player player) return !player.isCreative() && !player.isSpectator();
        return !entity.getType().is(Tags.EntityTypes.BOSSES);
    }

    private void pull(ServerLevel level, Vec3 mouth, double s) {
        if (this.caught.isEmpty()) return;
        double swallow = Math.max(4.0D, 1.5D * s);
        for (LivingEntity entity : this.caught) {
            if (!entity.isAlive() || entity.level() != level) continue;
            Vec3 to = mouth.subtract(entity.position());
            double distance = to.length();
            if (distance < swallow) {
                this.swallow(level, entity);
                continue;
            }
            Vec3 tug = to.scale(0.14D / distance).add(0.0D, 0.05D, 0.0D);
            Vec3 motion = entity.getDeltaMovement().scale(0.85D).add(tug);
            double speed = motion.length();
            if (speed > 1.2D) motion = motion.scale(1.2D / speed);
            entity.setDeltaMovement(motion);
            entity.fallDistance = 0.0F;
            entity.hurtMarked = true;
        }
    }

    private void swallow(ServerLevel level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            if (this.ticks % 10 == 1) player.hurt(this.storm.damageSources().mobAttack(this.storm), this.storm.attackDamage() * 0.4F);
            return;
        }
        level.sendParticles(ParticleTypes.SQUID_INK, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 12, 0.4D, 0.4D, 0.4D, 0.05D);
        boolean variant = entity instanceof net.minecraft.world.entity.Mob mob && com.craisinlord.integrated_superheroes.voidrealm.variant.VoidVariants.isVariant(mob);
        entity.discard();
        this.storm.feed(variant ? 1000.0D : 200.0D);
    }

    private void swirl(ServerLevel level, double s) {
        RandomSource random = this.storm.getRandom();
        double yaw = Math.toRadians(this.storm.getYRot());
        double pivotX = -Math.sin(yaw) * 1.5625D * s;
        double pivotZ = Math.cos(yaw) * 1.5625D * s;
        for (ServerPlayer player : level.players()) {
            for (int i = 0; i < 2; i++) {
                double height = random.nextDouble();
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = (2.5D + 9.0D * height) * s;
                double y = this.storm.getY() + (-25.0D + 33.0D * height) * s;
                double x = this.storm.getX() + pivotX + Math.cos(angle) * radius;
                double z = this.storm.getZ() + pivotZ + Math.sin(angle) * radius;
                level.sendParticles(player, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.02D);
            }
        }
    }

    void release() {
        for (Entity block : this.flying) {
            if (!block.isRemoved()) block.discard();
        }
        this.flying.clear();
        this.caught.clear();
    }
}
