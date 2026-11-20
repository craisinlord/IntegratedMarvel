package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.registries.armors.armor.CharacterItem;
import bond.thematic.api.util.StackData;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CharacterItem.class, remap = false)
public abstract class ThematicSuitRarityMixin {
    @Inject(method = "getArmorNBT", at = @At("HEAD"), remap = false)
    private void integratedSuperheroes$fixedRarity(Level level, ItemStack stack, CallbackInfoReturnable<CompoundTag> cir) {
        int rarity = ThematicOptions.fixedRarity();
        if (stack == null || rarity <= 0 || CharacterItem.isStarter(stack)
                || StackData.contains(stack, "ArmorUUID") || StackData.contains(stack, "IV")) {
            return;
        }
        StackData.update(stack, tag -> tag.putInt("IV", rarity));
    }
}
