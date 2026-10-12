package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.gauntlet.MindControlClientState;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class MindControlledPlayerInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void integratedMarvel$applyMindControlInput(CallbackInfo callback) {
        var state = MindControlClientState.state();
        if (!state.controlled()) {
            return;
        }
        Input input = (Input) (Object) this;
        input.forwardImpulse = state.up() == state.down() ? 0.0F : state.up() ? 1.0F : -1.0F;
        input.leftImpulse = state.left() == state.right() ? 0.0F : state.left() ? 1.0F : -1.0F;
        input.jumping = state.jump();
        input.shiftKeyDown = false;
    }
}
