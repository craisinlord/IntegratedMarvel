package com.craisinlord.integrated_superheroes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import weather2.weathersystem.storm.TornadoHelper;

@Mixin(value = TornadoHelper.class, remap = false)
public abstract class Weather2TornadoChunkMixin {
    @WrapOperation(
            method = {"tick", "tryRip"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
            remap = false
    )
    private BlockState integratedSuperheroes$skipUnloadedChunks(Level level, BlockPos pos, Operation<BlockState> original) {
        if (level instanceof ServerLevel serverLevel
                && serverLevel.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())) == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return original.call(level, pos);
    }
}
