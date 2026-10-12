package com.craisinlord.integrated_marvel.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import weather2.weathersystem.storm.StormObject;

@Mixin(value = StormObject.class, remap = false)
public abstract class Weather2StormTopBlockMixin {
    @WrapOperation(
            method = "calculateTopYBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
            remap = false
    )
    private BlockState integratedMarvel$readLoadedChunkOnly(Level level, BlockPos pos, Operation<BlockState> original) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return original.call(level, pos);
        }
        LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        if (chunk == null) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        if (level.isOutsideBuildHeight(pos)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }
        return chunk.getBlockState(pos);
    }
}
