package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.ColossusPower;
import com.craisinlord.integrated_superheroes.hero.HeroSkin;
import com.craisinlord.integrated_superheroes.hero.HumanTorchPower;
import com.craisinlord.integrated_superheroes.hero.IcemanPower;
import com.craisinlord.integrated_superheroes.hero.ThingPower;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class HeroSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void integratedSuperheroes$heroSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (ThingPower.isThing(player)) {
            cir.setReturnValue(HeroSkin.apply(cir.getReturnValue(), HeroSkin.Material.ROCK));
        } else if (ColossusPower.isSteel(player)) {
            cir.setReturnValue(HeroSkin.apply(cir.getReturnValue(), HeroSkin.Material.STEEL));
        } else if (HumanTorchPower.showsFlames(player)) {
            cir.setReturnValue(HeroSkin.apply(cir.getReturnValue(), HeroSkin.Material.MAGMA));
        } else if (IcemanPower.showsIceForm(player)) {
            cir.setReturnValue(HeroSkin.apply(cir.getReturnValue(), HeroSkin.Material.ICE));
        }
    }
}
