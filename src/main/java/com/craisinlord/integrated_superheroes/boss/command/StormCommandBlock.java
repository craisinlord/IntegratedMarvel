package com.craisinlord.integrated_superheroes.boss.command;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class StormCommandBlock extends BaseEntityBlock {
    public static final MapCodec<StormCommandBlock> CODEC = simpleCodec(StormCommandBlock::new);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty CONDITIONAL = BlockStateProperties.CONDITIONAL;

    public StormCommandBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONDITIONAL, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StormCommandBlockEntity(pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof StormCommandBlockEntity entity)) {
            return;
        }
        boolean signal = level.hasNeighborSignal(pos);
        boolean was = entity.isPowered();
        entity.setPowered(signal);
        if (signal && !was && !entity.isAutomatic()) {
            entity.scheduleRun();
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof StormCommandBlockEntity entity)) {
            return;
        }
        boolean consume = entity.fire(level);
        if (consume && level.getBlockEntity(pos) == entity && level.getBlockState(pos).is(this)) {
            consume(level, pos, level.getBlockState(pos));
            return;
        }
        if (level.getBlockState(pos).is(this)) {
            level.updateNeighbourForOutputSignal(pos, this);
        }
    }

    public static void consume(ServerLevel level, BlockPos pos, BlockState state) {
        level.removeBlock(pos, false);
        level.levelEvent(2001, pos, Block.getId(state));
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.8F, 0.7F);
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 40, 0.35, 0.35, 0.35, 0.06);
        level.sendParticles(ParticleTypes.WITCH, center.x, center.y, center.z, 12, 0.4, 0.4, 0.4, 0.02);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof StormCommandBlockEntity entity)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            StormCommandBlocks.tryOpen(serverPlayer, entity);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof StormCommandBlockEntity entity && entity.isLockedFor(player)) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (level.getBlockEntity(pos) instanceof StormCommandBlockEntity entity && entity.isLockedFor(player)) {
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof StormCommandBlockEntity entity) || !entity.canDrop()) {
            return List.of();
        }
        ItemStack stack = new ItemStack(this);
        if (!entity.getCommandBlock().getCommand().isEmpty()) {
            BlockItem.setBlockEntityData(stack, entity.getType(), entity.itemData(params.getLevel().registryAccess()));
        }
        if (entity.getCommandBlock().getCustomName() != null) {
            stack.set(DataComponents.CUSTOM_NAME, entity.getCommandBlock().getCustomName());
        }
        return List.of(stack);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof StormCommandBlockEntity entity)) {
            return;
        }
        if (!stack.has(DataComponents.BLOCK_ENTITY_DATA)) {
            entity.getCommandBlock().setTrackOutput(level.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK));
        }
        if (placer instanceof Player player) {
            entity.setOwner(player.getUUID());
        }
        boolean signal = level.hasNeighborSignal(pos);
        entity.setPowered(signal);
        if (entity.isAutomatic() && !signal) {
            entity.scheduleRun();
        }
        level.sendBlockUpdated(pos, state, state, 3);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof StormCommandBlockEntity entity ? Math.min(entity.getCommandBlock().getSuccessCount(), 15) : 0;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONDITIONAL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }
}
