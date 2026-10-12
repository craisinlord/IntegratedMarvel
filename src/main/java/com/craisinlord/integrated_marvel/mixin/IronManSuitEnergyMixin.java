package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.IronManEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.tintankgames.marvel.world.item.EnergySuitItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = EnergySuitItem.class, remap = false)
public abstract class IronManSuitEnergyMixin {
    @Inject(method = "removeEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedMarvel$drainFE(ItemStack stack, float power, CallbackInfo ci) {
        IronManEnergy.drainPercent(stack, power);
        ci.cancel();
    }

    @Inject(method = "addEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedMarvel$noFreeCharge(ItemStack stack, float power, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "appendHoverText", at = @At("TAIL"), remap = false)
    private void integratedMarvel$feTooltip(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag, CallbackInfo ci) {
        if (lines.isEmpty() || !IronManEnergy.hasEnergy(stack)) {
            return;
        }
        Component last = lines.get(lines.size() - 1);
        if (last.getContents() instanceof TranslatableContents contents && "item.suit.energy".equals(contents.getKey())) {
            lines.remove(lines.size() - 1);
        }
        double fe = IronManEnergy.getFE(stack);
        long capacity = IronManEnergy.capacity(stack);
        lines.add(Component.translatableWithFallback("tooltip.integrated_marvel.suit_energy", "Power: %s / %s FE (%s)",
                        IronManEnergy.format(fe), IronManEnergy.format(capacity),
                        String.format("%.1f%%", fe / capacity * 100.0D))
                .withStyle(ChatFormatting.GRAY));
    }
}
