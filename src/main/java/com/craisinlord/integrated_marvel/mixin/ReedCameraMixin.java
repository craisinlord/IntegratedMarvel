package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.FantasticClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class ReedCameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    public abstract Vec3 getPosition();

    @Inject(method = "setup", at = @At("TAIL"))
    private void integratedMarvel$reedCamera(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        if (entity instanceof Player player) {
            double offset = FantasticClient.cameraOffset(player, partialTick);
            if (offset != 0.0D) {
                this.setPosition(this.getPosition().add(0.0D, offset, 0.0D));
            }
        }
    }
}
