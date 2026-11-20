package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.registries.armors.armor.CharacterItem;
import com.craisinlord.integrated_superheroes.compat.thematic.SuitPieces;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "bond.thematic.api.client.SuitModelProxies", remap = false)
public abstract class ThematicSuitTakeoverMixin {
    @Inject(method = "renderArmor", at = @At("RETURN"), cancellable = true, remap = false)
    private static void integratedSuperheroes$wholeSetOnly(LivingEntity entity, CallbackInfoReturnable<CharacterItem> cir) {
        CharacterItem suit = cir.getReturnValue();
        if (suit != null && entity instanceof Player player && SuitPieces.partial(player, suit)) {
            cir.setReturnValue(null);
        }
    }
}
