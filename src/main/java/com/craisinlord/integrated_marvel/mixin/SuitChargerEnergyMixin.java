package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.IronManEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.tintankgames.marvel.world.level.block.entity.SuitChargerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SuitChargerBlockEntity.class, remap = false)
public abstract class SuitChargerEnergyMixin implements IronManEnergy.ChargerBuffer {
    @Unique
    private long integratedMarvel$energy;

    @Override
    public long integratedMarvel$getEnergy() {
        return integratedMarvel$energy;
    }

    @Override
    public void integratedMarvel$setEnergy(long energy) {
        integratedMarvel$energy = Math.max(0L, Math.min(energy, integratedMarvel$getCapacity()));
    }

    @Override
    public long integratedMarvel$getCapacity() {
        return IronManEnergy.SUIT_CHARGER_BUFFER.get();
    }

    @Override
    public long integratedMarvel$getMaxInput() {
        return IronManEnergy.SUIT_CHARGER_MAX_INPUT.get();
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = false)
    private void integratedMarvel$saveEnergy(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        tag.putLong(IronManEnergy.CHARGER_ENERGY_TAG, integratedMarvel$energy);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = false)
    private void integratedMarvel$loadEnergy(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        integratedMarvel$energy = tag.getLong(IronManEnergy.CHARGER_ENERGY_TAG);
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private static void integratedMarvel$chargeFromBuffer(Level level, BlockPos pos, BlockState state, SuitChargerBlockEntity be, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }
        IronManEnergy.ChargerBuffer buffer = (IronManEnergy.ChargerBuffer) be;
        long energy = buffer.integratedMarvel$getEnergy();
        if (energy <= 0L) {
            return;
        }
        long drawn = IronManEnergy.chargeFromBuffer(be.items(), energy);
        if (drawn > 0L) {
            buffer.integratedMarvel$setEnergy(energy - drawn);
            be.setChanged();
        }
    }
}
