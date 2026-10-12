package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import weather2.util.WeatherUtilBlock;

@Mixin(value = WeatherUtilBlock.class, remap = false)
public abstract class Weather2PrecipitationHeightMixin {
    @Inject(
            method = "getPrecipitationHeightSafe(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/Heightmap$Types;)Lnet/minecraft/core/BlockPos;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void integratedSuperheroes$skipUnloadedChunks(Level level, BlockPos pos, Heightmap.Types type, CallbackInfoReturnable<BlockPos> cir) {
        if (level instanceof ServerLevel serverLevel
                && serverLevel.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())) == null) {
            cir.setReturnValue(new BlockPos(pos.getX(), -255, pos.getZ()));
        }
    }
}
