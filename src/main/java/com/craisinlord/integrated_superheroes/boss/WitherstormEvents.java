package com.craisinlord.integrated_superheroes.boss;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class WitherstormEvents {
    private static final ResourceLocation STORM_COMMAND_BLOCK = IntegratedSuperheroes.id("storm_command_block");
    private static BlockPattern pattern;

    private WitherstormEvents() {
    }

    private static BlockPattern pattern() {
        if (pattern == null) {
            pattern = BlockPatternBuilder.start()
                    .aisle("^^^", "#@#", "~#~")
                    .where('#', block -> block.getState().is(BlockTags.WITHER_SUMMON_BASE_BLOCKS))
                    .where('^', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_SKULL)
                            .or(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_WALL_SKULL))))
                    .where('@', block -> isSummonCore(block.getState()))
                    .where('~', block -> block.getState().isAir())
                    .build();
        }
        return pattern;
    }

    private static boolean isSummonCore(BlockState state) {
        if (state.is(Blocks.COMMAND_BLOCK)) return true;
        return !state.isAir() && STORM_COMMAND_BLOCK.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        if (event.getEntity() instanceof ServerPlayer) {
            for (WitherstormEntity storm : level.getEntitiesOfClass(WitherstormEntity.class, new AABB(pos).inflate(64.0D))) {
                storm.recordPlaced(pos);
            }
        }
        BlockState placed = event.getPlacedBlock();
        if (placed.is(Blocks.WITHER_SKELETON_SKULL) || placed.is(Blocks.WITHER_SKELETON_WALL_SKULL)) {
            trySummon(level, pos);
        }
    }

    private static void trySummon(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() || level.getDifficulty() == Difficulty.PEACEFUL) return;
        BlockPattern.BlockPatternMatch match = pattern().find(level, pos);
        if (match == null) return;
        WitherstormEntity storm = WitherstormContent.WITHERSTORM.get().create(level);
        if (storm == null) return;
        CarvedPumpkinBlock.clearPatternBlocks(level, match);
        BlockPos base = match.getBlock(1, 2, 0).getPos();
        float yaw = match.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F;
        storm.moveTo(base.getX() + 0.5D, base.getY() + 0.55D, base.getZ() + 0.5D, yaw, 0.0F);
        storm.yBodyRot = yaw;
        storm.finalizeSpawn(level, level.getCurrentDifficultyAt(base), MobSpawnType.TRIGGERED, null);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, storm.getBoundingBox().inflate(50.0D))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, storm);
        }
        level.addFreshEntity(storm);
        CarvedPumpkinBlock.updatePatternBlocks(level, match);
    }
}
