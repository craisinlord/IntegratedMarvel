package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteBridge;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ItemInHandRenderer.class, priority = 1500)
public abstract class ViltrumiteOffhandMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.client.mixin.AlwaysVisibleOffhandMixin", name = "forceRenderEmptyOffhand")
    @ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At(value = "FIELD", target = "Ldev/baranhan/viltrumitecore/config/ViltrumiteClientConfig;alwaysRenderOffhand:Z"), remap = false)
    private boolean integratedSuperheroes$offhandOnlyForViltrumites(boolean always, @Local(argsOnly = true) AbstractClientPlayer player) {
        return always && ViltrumiteBridge.isViltrumite(player);
    }
}
