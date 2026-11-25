package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.thematic.ThematicIdle;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KeyMapping.class, priority = 1500)
public abstract class ThematicKeyBlocksGuardMixin {
    @Shadow
    @Final
    private String category;

    @Dynamic("blocks is merged into KeyMapping by Thematic KeybindingMixin")
    @Inject(method = "blocks(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;Z)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$skipIdleBlocks(CallbackInfoReturnable<Boolean> cir, boolean consume, CallbackInfo ci) {
        if (!KeyMapping.CATEGORY_MOVEMENT.equalsIgnoreCase(this.category) && !KeyMapping.CATEGORY_GAMEPLAY.equalsIgnoreCase(this.category)
                && !KeyMapping.CATEGORY_INVENTORY.equalsIgnoreCase(this.category)) {
            ci.cancel();
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || ThematicIdle.movementQuiet(player)) {
            ci.cancel();
        }
    }
}
