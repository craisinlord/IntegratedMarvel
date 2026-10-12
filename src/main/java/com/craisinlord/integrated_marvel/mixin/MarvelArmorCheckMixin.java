package com.craisinlord.integrated_marvel.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.tintankgames.marvel.world.item.MarvelItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 1500)
public abstract class MarvelArmorCheckMixin {
    @TargetHandler(mixin = "net.tintankgames.marvel.mixin.LivingEntityMixin", name = "nightsYourVision")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedMarvel$skipOtherEffects(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir, CallbackInfo ci) {
        if (effect != MobEffects.NIGHT_VISION) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "net.tintankgames.marvel.mixin.LivingEntityMixin", name = "jesusMoment")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedMarvel$skipWithoutQuicksilverChest(FluidState fluid, CallbackInfoReturnable<Boolean> cir, CallbackInfo ci) {
        if (!((LivingEntity) (Object) this).getItemBySlot(EquipmentSlot.CHEST).is(MarvelItems.Tags.QUICKSILVER_ARMOR)) {
            ci.cancel();
        }
    }
}
