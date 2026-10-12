package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.villain.Villains;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.tintankgames.marvel.world.entity.RedSkull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RedSkull.class, remap = false)
public abstract class RedSkullVillainMixin {
    @Shadow(remap = false)
    protected abstract boolean teleport();

    @Redirect(method = "customServerAiStep", at = @At(value = "INVOKE",
            target = "Lnet/tintankgames/marvel/world/entity/RedSkull;heal(F)V"), remap = false, require = 0)
    private void integratedSuperheroes$noPassiveHeal(RedSkull self, float amount) {
        if (!Villains.managed(self)) {
            self.heal(amount);
        }
    }

    @Redirect(method = "customServerAiStep", at = @At(value = "INVOKE",
            target = "Lnet/tintankgames/marvel/world/entity/RedSkull;setDropChance(Lnet/minecraft/world/entity/EquipmentSlot;F)V"),
            remap = false, require = 0)
    private void integratedSuperheroes$keepDropChance(RedSkull self, EquipmentSlot slot, float chance) {
        if (!Villains.managed(self)) {
            self.setDropChance(slot, chance);
        }
    }

    @Redirect(method = "isInvulnerableTo", at = @At(value = "INVOKE",
            target = "Lnet/tintankgames/marvel/world/entity/RedSkull;teleport()Z"), remap = false, require = 0)
    private boolean integratedSuperheroes$noDodgeTeleport(RedSkull self) {
        if (Villains.managed(self)) {
            return false;
        }
        return this.teleport();
    }

    @Redirect(method = "dropCustomDeathLoot", at = @At(value = "INVOKE",
            target = "Lnet/tintankgames/marvel/world/entity/RedSkull;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"),
            remap = false, require = 0)
    private ItemEntity integratedSuperheroes$noDiamondBlock(RedSkull self, ItemStack stack) {
        if (Villains.managed(self)) {
            return null;
        }
        return self.spawnAtLocation(stack);
    }
}
