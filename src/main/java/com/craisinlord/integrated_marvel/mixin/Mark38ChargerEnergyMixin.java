package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.IronManEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.tintankgames.marvel.world.entity.VeronicaSentry;
import net.tintankgames.marvel.world.level.block.Mark38ChargerBlock;
import net.tintankgames.marvel.world.level.block.entity.Mark38ChargerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = Mark38ChargerBlockEntity.class, remap = false)
public abstract class Mark38ChargerEnergyMixin implements IronManEnergy.ChargerBuffer {
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
        return IronManEnergy.LARGE_CHARGER_BUFFER.get();
    }

    @Override
    public long integratedMarvel$getMaxInput() {
        return IronManEnergy.LARGE_CHARGER_MAX_INPUT.get();
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = false)
    private void integratedMarvel$saveEnergy(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        tag.putLong(IronManEnergy.CHARGER_ENERGY_TAG, integratedMarvel$energy);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = false)
    private void integratedMarvel$loadEnergy(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        integratedMarvel$energy = tag.getLong(IronManEnergy.CHARGER_ENERGY_TAG);
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private static void integratedMarvel$chargeSentries(Level level, BlockPos pos, BlockState state, Mark38ChargerBlockEntity be, CallbackInfo ci) {
        if (level.isClientSide || !state.hasProperty(Mark38ChargerBlock.FACING)) {
            return;
        }
        IronManEnergy.ChargerBuffer buffer = (IronManEnergy.ChargerBuffer) be;
        List<BlockPos> dock = new ArrayList<>(List.of(Mark38ChargerBlock.getExtraPlacementPositions(pos, state)));
        dock.add(pos);
        List<VeronicaSentry> sentries = new ArrayList<>();
        for (BlockPos part : dock) {
            for (VeronicaSentry sentry : level.getEntitiesOfClass(VeronicaSentry.class, new AABB(part))) {
                if (!sentries.contains(sentry)) {
                    sentries.add(sentry);
                }
            }
        }
        for (VeronicaSentry sentry : sentries) {
            long energy = buffer.integratedMarvel$getEnergy();
            long drawn = energy > 0L ? IronManEnergy.chargeFromBuffer(sentry.getArmorSlots(), energy) : 0L;
            if (drawn > 0L) {
                buffer.integratedMarvel$setEnergy(energy - drawn);
                be.setChanged();
            }
            boolean charging = energy > 0L;
            sentry.setCharging(charging);
            if (charging) {
                sentry.setYRot(state.getValue(Mark38ChargerBlock.FACING).toYRot());
            }
        }
    }
}
