package com.craisinlord.integrated_superheroes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.common.ItemAbilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class ViltrumiteShieldBlockMixin {
    @WrapOperation(
            method = "isDamageSourceBlocked",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isBlocking()Z"))
    private boolean integratedSuperheroes$shieldBlocks(LivingEntity entity, Operation<Boolean> original, DamageSource source) {
        return original.call(entity) || (entity instanceof Player player && integratedSuperheroes$raisedShield(player));
    }

    @Unique
    private static boolean integratedSuperheroes$raisedShield(Player player) {
        if (!player.isUsingItem()) {
            return false;
        }
        ItemStack stack = player.getUseItem();
        if (stack.isEmpty()) {
            return false;
        }
        if (!stack.canPerformAction(ItemAbilities.SHIELD_BLOCK) && stack.getUseAnimation() != UseAnim.BLOCK) {
            return false;
        }
        return stack.getUseDuration(player) - player.getUseItemRemainingTicks() >= 5;
    }
}
