package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.KittyPower;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerPhaseMixin {
    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;noPhysics:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void integratedMarvel$phase(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (KittyPower.isPhasing(self)) {
            self.noPhysics = true;
        }
    }
}
