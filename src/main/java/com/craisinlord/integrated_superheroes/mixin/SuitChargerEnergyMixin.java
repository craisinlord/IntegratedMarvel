package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.IronManEnergy;
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
    private long integratedSuperheroes$energy;

    @Override
    public long integratedSuperheroes$getEnergy() {
        return integratedSuperheroes$energy;
    }

    @Override
    public void integratedSuperheroes$setEnergy(long energy) {
        integratedSuperheroes$energy = Math.max(0L, Math.min(energy, integratedSuperheroes$getCapacity()));
    }

    @Override
    public long integratedSuperheroes$getCapacity() {
        return IronManEnergy.SUIT_CHARGER_BUFFER.get();
    }

    @Override
    public long integratedSuperheroes$getMaxInput() {
        return IronManEnergy.SUIT_CHARGER_MAX_INPUT.get();
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = false)
    private void integratedSuperheroes$saveEnergy(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        tag.putLong(IronManEnergy.CHARGER_ENERGY_TAG, integratedSuperheroes$energy);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = false)
    private void integratedSuperheroes$loadEnergy(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        integratedSuperheroes$energy = tag.getLong(IronManEnergy.CHARGER_ENERGY_TAG);
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private static void integratedSuperheroes$chargeFromBuffer(Level level, BlockPos pos, BlockState state, SuitChargerBlockEntity be, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }
        IronManEnergy.ChargerBuffer buffer = (IronManEnergy.ChargerBuffer) be;
        long energy = buffer.integratedSuperheroes$getEnergy();
        if (energy <= 0L) {
            return;
        }
        long drawn = IronManEnergy.chargeFromBuffer(be.items(), energy);
        if (drawn > 0L) {
            buffer.integratedSuperheroes$setEnergy(energy - drawn);
            be.setChanged();
        }
    }
}
