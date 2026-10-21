package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerModel.class, priority = 1500)
public abstract class ViltrumitePoseGuardMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.client.mixin.PlayerModelMixin", name = "onSetAngles")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$onlyViltrumitePose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                                        float headPitch, CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteGuards.styled(entity)) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.client.mixin.SlowFlyingModelMixin", name = "onSetupAnim")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$onlyPlayerFlightPose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                                          float headPitch, CallbackInfo original, CallbackInfo ci) {
        if (ViltrumiteGuards.performance() && !(entity instanceof Player)) {
            ci.cancel();
        }
    }
}
