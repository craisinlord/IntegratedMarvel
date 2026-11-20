package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.registries.armors.armor.CharacterItem;
import com.craisinlord.integrated_superheroes.compat.thematic.SuitPieces;
import com.craisinlord.integrated_superheroes.compat.thematic.PowerOwnership;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "bond.thematic.api.util.ThematicHelper", remap = false)
public abstract class ThematicFullSetMixin {
    @Inject(method = "getArmorStack(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void integratedSuperheroes$serumCostumeStack(LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();
        if (entity instanceof Player && stack != null && !stack.isEmpty()
                && PowerOwnership.costume(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "getArmor(Lnet/minecraft/world/entity/LivingEntity;)Lbond/thematic/api/registries/armors/armor/CharacterItem;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void integratedSuperheroes$fullSet(LivingEntity entity, CallbackInfoReturnable<CharacterItem> cir) {
        CharacterItem suit = cir.getReturnValue();
        if (suit != null && entity instanceof Player player
                && (PowerOwnership.costume(BuiltInRegistries.ITEM.getKey(suit).toString()) || !SuitPieces.complete(player, suit))) {
            cir.setReturnValue(null);
        }
    }
}
