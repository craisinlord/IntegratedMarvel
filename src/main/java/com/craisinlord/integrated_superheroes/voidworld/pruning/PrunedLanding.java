package com.craisinlord.integrated_superheroes.voidworld.pruning;

import com.craisinlord.integrated_superheroes.portal.MarvelPortal;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantTears;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VoidVariants;
import com.craisinlord.integrated_superheroes.voidworld.VoidConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PrunedLanding {
    private static final int BLOCKS_PER_TICK = 1024;
    private static final int SHAPES_PER_TICK = 2048;

    private final PrunedPiece piece;
    private final PrunedChunkEntity chunk;
    private final RandomSource random;
    private final RandomSource lootRandom;
    private int placed;
    private int shaped;
    private boolean finished;

    PrunedLanding(PrunedPiece piece, PrunedChunkEntity chunk) {
        this.piece = piece;
        this.chunk = chunk;
        this.random = RandomSource.create(piece.seed() ^ 0x5DEECE66DL);
        this.lootRandom = RandomSource.create(piece.seed());
    }

    static boolean canLand(ServerLevel level, PrunedPiece piece) {
        return PrunedPiece.areaLoaded(level, piece.box(), 2);
    }

    void impact(ServerLevel level) {
        BlockPos centre = this.piece.centre();
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centre.getX(), centre.getZ());
        BlockState dust = level.getBlockState(new BlockPos(centre.getX(), ground - 1, centre.getZ()));
        if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            this.crater(level);
        }
        this.shockwave(level, ground, dust);
    }

    boolean finished() {
        return this.finished;
    }

    void step(ServerLevel level) {
        if (this.finished) {
            return;
        }
        List<StructureTemplate.StructureBlockInfo> blocks = this.piece.blocks();
        int end = Math.min(blocks.size(), this.placed + BLOCKS_PER_TICK);
        for (int i = this.placed; i < end; i++) {
            this.place(level, blocks.get(i));
        }
        this.placed = end;
        this.unbury(level);
        if (this.placed < blocks.size()) {
            return;
        }
        int shapeEnd = Math.min(blocks.size(), this.shaped + SHAPES_PER_TICK);
        for (int i = this.shaped; i < shapeEnd; i++) {
            BlockPos pos = blocks.get(i).pos();
            BlockState state = level.getBlockState(pos);
            BlockState updated = Block.updateFromNeighbourShapes(state, level, pos);
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            }
        }
        this.shaped = shapeEnd;
        if (this.shaped < blocks.size()) {
            return;
        }
        List<BlockPos> tops = this.tops(level);
        boolean hasLoot = false;
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            if (info.nbt() != null && info.nbt().contains("LootTable")) {
                hasLoot = true;
                break;
            }
        }
        if (!hasLoot) {
            this.chest(level, tops);
        }
        this.unbury(level);
        this.riders(level, tops);
        BlockPos centre = this.piece.centre();
        level.playSound(null, centre.getX() + 0.5D, centre.getY() + 1.0D, centre.getZ() + 0.5D,
                SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 4.0F, 0.5F);
        this.finished = true;
    }

    private void place(ServerLevel level, StructureTemplate.StructureBlockInfo info) {
        BlockPos pos = info.pos();
        CompoundTag nbt = info.nbt();
        if (nbt != null) {
            Clearable.tryClear(level.getBlockEntity(pos));
            level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
        }
        if (!level.setBlock(pos, info.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE) || nbt == null) {
            return;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return;
        }
        CompoundTag tag = nbt.copy();
        if (entity instanceof RandomizableContainer && tag.contains("LootTable")) {
            tag.putLong("LootTableSeed", this.lootRandom.nextLong());
        }
        entity.loadWithComponents(tag, level.registryAccess());
        entity.setChanged();
    }

    private void crater(ServerLevel level) {
        BoundingBox box = this.piece.box();
        int floor = box.minY();
        int reach = Math.max(4, this.piece.radius() / 3 + 3);
        double halfX = box.getXSpan() * 0.5D;
        double halfZ = box.getZSpan() * 0.5D;
        double cx = box.minX() + halfX;
        double cz = box.minZ() + halfZ;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = box.minX() - reach; x <= box.maxX() + reach; x++) {
            for (int z = box.minZ() - reach; z <= box.maxZ() + reach; z++) {
                if (x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ()) {
                    continue;
                }
                double ex = Math.max(0.0D, Math.abs(x + 0.5D - cx) - halfX);
                double ez = Math.max(0.0D, Math.abs(z + 0.5D - cz) - halfZ);
                double out = Math.sqrt(ex * ex + ez * ez);
                if (out > reach || !level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int depth = (int) Math.round(3.0D * (1.0D - out / reach) + this.random.nextDouble() * 0.8D);
                if (depth <= 0) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (Math.abs(top - floor) > 12) {
                    continue;
                }
                for (int d = 0; d < depth; d++) {
                    cursor.set(x, top - d, z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, cursor) < 0.0F) {
                        break;
                    }
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private List<BlockPos> tops(ServerLevel level) {
        Set<BlockPos> occupied = new HashSet<>();
        for (StructureTemplate.StructureBlockInfo info : this.piece.blocks()) {
            occupied.add(info.pos());
        }
        List<BlockPos> out = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : this.piece.blocks()) {
            BlockPos above = info.pos().above();
            if (occupied.contains(above)) {
                continue;
            }
            if (level.getBlockState(info.pos()).isFaceSturdy(level, info.pos(), Direction.UP)
                    && level.getBlockState(above).isAir() && level.getBlockState(above.above()).isAir()) {
                out.add(above);
            }
        }
        return out;
    }

    private void chest(ServerLevel level, List<BlockPos> tops) {
        BlockPos at;
        if (!tops.isEmpty()) {
            at = tops.remove(this.random.nextInt(tops.size()));
        } else {
            BlockPos centre = this.piece.centre();
            int x = this.piece.box().maxX() + 2;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, centre.getZ());
            if (y < PrunedPiece.MIN_GROUND) {
                return;
            }
            at = new BlockPos(x, y, centre.getZ());
        }
        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(this.random));
        if (!level.setBlock(at, chest, Block.UPDATE_ALL)) {
            return;
        }
        RandomizableContainer.setBlockEntityLootTable(level, this.lootRandom, at, ResourceKey.create(Registries.LOOT_TABLE,
                IntegratedSuperheroes.id("chests/void_debris/tier_" + this.piece.tier())));
    }

    private void unbury(ServerLevel level) {
        BoundingBox box = this.piece.box();
        AABB area = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (entity instanceof Player player && player.isSpectator()) {
                continue;
            }
            BlockPos feet = entity.blockPosition();
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                continue;
            }
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, feet.getX(), feet.getZ());
            entity.teleportTo(entity.getX(), top, entity.getZ());
        }
    }

    private void shockwave(ServerLevel level, int ground, BlockState dust) {
        BoundingBox box = this.piece.box();
        double halfX = box.getXSpan() * 0.5D;
        double halfZ = box.getZSpan() * 0.5D;
        double cx = box.minX() + halfX;
        double cz = box.minZ() + halfZ;
        double cy = ground + 0.5D;
        Vec3 at = new Vec3(cx, cy, cz);
        double reach = VoidConfig.SPEC.isLoaded() ? VoidConfig.PRUNE_SHOCKWAVE_RADIUS.get() : 8;
        double damage = VoidConfig.SPEC.isLoaded() ? VoidConfig.PRUNE_SHOCKWAVE_DAMAGE.get() : 8.0D;
        AABB area = new AABB(box.minX() - reach, box.minY() - 4, box.minZ() - reach, box.maxX() + 1 + reach, box.maxY() + 8, box.maxZ() + 1 + reach);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (entity instanceof ArmorStand || entity instanceof Player player && (player.isSpectator() || player.getAbilities().invulnerable)) {
                continue;
            }
            double ex = Math.max(0.0D, Math.abs(entity.getX() - cx) - halfX);
            double ez = Math.max(0.0D, Math.abs(entity.getZ() - cz) - halfZ);
            double out = Math.sqrt(ex * ex + ez * ez);
            if (out > reach) {
                continue;
            }
            double falloff = reach <= 0.0D ? 1.0D : 1.0D - out / reach;
            boolean pet = entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null;
            if (damage > 0.0D && falloff > 0.0D && !pet) {
                entity.hurt(level.damageSources().fallingBlock(this.chunk), (float) (damage * falloff));
            }
            double dx = entity.getX() - cx;
            double dz = entity.getZ() - cz;
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 0.01D) {
                dx = 1.0D;
                dz = 0.0D;
                length = 1.0D;
            }
            double push = 0.6D + 1.4D * falloff;
            entity.push(dx / length * push, 0.35D + 0.5D * falloff, dz / length * push);
            entity.hurtMarked = true;
        }
        level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 8.0F, 0.5F);
        level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 5.0F, 0.8F);
        level.playSound(null, cx, cy, cz, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.BLOCKS, 5.0F, 0.6F);
        level.playSound(null, cx, cy, cz, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 4.0F, 0.35F);
        VoidPruning.send(level, ParticleTypes.EXPLOSION_EMITTER, at, Math.max(1, this.piece.radius() / 4), halfX * 0.6D, 0.5D, halfZ * 0.6D, 0.0D);
        VoidPruning.send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, at, 40, halfX + 2.0D, 1.0D, halfZ + 2.0D, 0.02D);
        if (dust.isAir() || dust.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, dust);
        int points = Mth.clamp(this.piece.radius() * 4, 24, 72);
        double ring = this.piece.radius() + 1.0D;
        List<ServerPlayer> viewers = VoidPruning.viewers(level, at, 160.0D);
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2.0D / points;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            for (ServerPlayer player : viewers) {
                level.sendParticles(player, particle, true, cx + cos * ring, cy + 0.3D, cz + sin * ring, 0, cos, 0.25D, sin, 0.9D);
                level.sendParticles(player, ParticleTypes.POOF, true, cx + cos * ring, cy + 0.5D, cz + sin * ring, 0, cos, 0.1D, sin, 0.5D);
            }
        }
    }

    private void riders(ServerLevel level, List<BlockPos> tops) {
        double chance = VoidConfig.SPEC.isLoaded() ? VoidConfig.PRUNE_RIDER_CHANCE.get() : 0.35D;
        if (chance <= 0.0D) {
            return;
        }
        int slots = Math.min(3, 1 + this.piece.tier() / 2);
        List<BlockPos> spots = new ArrayList<>(tops);
        if (spots.isEmpty()) {
            BlockPos centre = this.piece.centre();
            spots.add(new BlockPos(centre.getX(), this.piece.box().maxY() + 1, centre.getZ()));
        }
        for (int i = 0; i < slots; i++) {
            if (this.random.nextDouble() >= chance) {
                continue;
            }
            if (VoidVariants.full(level)) {
                return;
            }
            try {
                BlockPos spot = spots.get(this.random.nextInt(spots.size()));
                if (MarvelPortal.enabled()) {
                    VariantTears.arrive(level, Vec3.atBottomCenterOf(spot), this.piece.tier(), null);
                } else {
                    VoidVariants.spawn(level, spot, this.piece.tier());
                }
            } catch (RuntimeException e) {
                IntegratedSuperheroes.LOGGER.warn("Pruned piece rider failed to spawn: {}", e.getMessage());
                return;
            }
        }
    }
}
