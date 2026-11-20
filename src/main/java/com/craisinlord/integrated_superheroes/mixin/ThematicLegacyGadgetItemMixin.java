package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.cleanup.CatalogCleanup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "bond.thematic.api.registries.item.ThematicGadgetItem", remap = false)
public abstract class ThematicLegacyGadgetItemMixin {
    @Unique
    private static boolean integratedSuperheroes$retired(Level level, Player player, ItemStack stack) {
        if (!CatalogCleanup.retiredLegacyGadget(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return false;
        if (!level.isClientSide()) {
            player.displayClientMessage(Component.literal("Legacy gadgets are retired. Use your suit's abilities."), true);
        }
        return true;
    }

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$redirectUse(Level level, Player player, InteractionHand hand,
                                                  CallbackInfoReturnable<InteractionResult> cir) {
        if (integratedSuperheroes$retired(level, player, player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$disableOldLoadout(Level level, Player player, ItemStack stack, CallbackInfo ci) {
        if (integratedSuperheroes$retired(level, player, stack)) ci.cancel();
    }
}
