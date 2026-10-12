package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.IronManSuits;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.tintankgames.marvel.world.item.RepulsorItem", remap = false)
public abstract class IronManRepulsorCooldownMixin {
    @Redirect(
            method = "releaseUsing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/Item;I)V"),
            remap = false
    )
    private void integratedMarvel$markCooldown(ItemCooldowns cooldowns, Item item, int ticks,
                                           ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        IronManSuits.Stats stats = IronManSuits.isLoaded() ? IronManSuits.forWearer(entity) : null;
        cooldowns.addCooldown(item, stats != null ? stats.repulsorCooldownTicks().get() : ticks);
    }
}
