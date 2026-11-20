package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.thematic.ThematicFixes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "bond.thematic.api.content.flight.FlightTrait", remap = false)
public abstract class ThematicFlightTraitMixin {
    @Inject(method = "abilities", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$keepOtherAbilities(Player player, Abilities abilities, CallbackInfoReturnable<Abilities> cir) {
        if (ThematicFixes.keepAbilities(player, abilities)) {
            cir.setReturnValue(abilities);
        }
    }

    @Inject(method = "allowElytra", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$allowOtherElytras(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ThematicFixes.otherElytra(entity)) {
            cir.setReturnValue(true);
        }
    }
}
