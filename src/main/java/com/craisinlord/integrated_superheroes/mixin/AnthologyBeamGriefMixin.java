package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.anthology.beam.BeamController;
import com.craisinlord.anthology.beam.BeamSettings;
import com.craisinlord.integrated_superheroes.hero.HeroGrief;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BeamController.class, remap = false)
public abstract class AnthologyBeamGriefMixin {
    @Shadow
    @Final
    private LivingEntity owner;

    @Inject(method = "destroyBeamBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$guardDestroy(Level level, BlockPos pos, BlockState existing, BeamSettings settings, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel) || !HeroGrief.isPlayerOwned(owner)) return;
        if (existing.isAir()
                || level.getBlockEntity(pos) != null
                || existing.getDestroySpeed(level, pos) < 0.0F
                || existing.getBlock().getExplosionResistance() > settings.maxTerrainResistance()
                || existing.getCollisionShape(level, pos).isEmpty()) {
            return;
        }
        if (!HeroGrief.allowBreak(HeroGrief.responsiblePlayer(owner), serverLevel, pos)) ci.cancel();
    }

    @Inject(method = "buildIceBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$guardIce(Level level, BlockPos pos, BlockState existing, BeamSettings settings, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel) || !HeroGrief.isPlayerOwned(owner)) return;
        boolean replaceable = existing.isAir() || existing.canBeReplaced() || !existing.getFluidState().isEmpty();
        if (!replaceable || level.getBlockEntity(pos) != null || existing.is(BlockTags.ICE)) return;
        if (!HeroGrief.canPlace(HeroGrief.responsiblePlayer(owner), serverLevel, pos)) ci.cancel();
    }
}
