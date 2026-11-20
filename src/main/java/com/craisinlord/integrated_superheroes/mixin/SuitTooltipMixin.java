package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.thematic.SuitVariantTooltipClient;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class SuitTooltipMixin {
    @WrapMethod(method = "getTooltipLines")
    private List<Component> integratedSuperheroes$suitTooltip(Item.TooltipContext context, Player player, TooltipFlag flag,
                                                              Operation<List<Component>> original) {
        return SuitVariantTooltipClient.decorate((ItemStack) (Object) this, original.call(context, player, flag));
    }
}
