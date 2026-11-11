package com.craisinlord.integrated_superheroes.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.tintankgames.marvel.attachment.VeronicaData;
import net.tintankgames.marvel.world.item.component.SuitParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Marvel flips a flag in the stored piece's suit_parts list in place. That list is shared by every copy of the
 * stack (and by storage mods that key items on their components), so mutating it changes items that were never
 * touched. Hand Marvel its own copy to edit; it sets the component back afterwards anyway.
 */
@Mixin(value = VeronicaData.class, remap = false)
public abstract class VeronicaSuitPartsCopyMixin {
    @ModifyExpressionValue(method = "addSuitPart", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Ljava/util/function/Supplier;Ljava/lang/Object;)Ljava/lang/Object;",
            ordinal = 0), remap = false)
    private Object integratedSuperheroes$copyStoredParts(Object parts) {
        return parts instanceof SuitParts suitParts ? new SuitParts(suitParts.parts()) : parts;
    }
}
