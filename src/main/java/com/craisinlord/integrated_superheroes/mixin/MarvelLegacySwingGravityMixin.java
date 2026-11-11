package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.integrated_superheroes.hero.HeroGravity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.stxllarplanes.marvellegacy.util.SwingManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Web swinging runs its rope physics in world space but reads and writes the player's velocity in the player's own
 * frame, and pulls down with world gravity. With flipped gravity the rope physics now read and write world-space
 * velocity and pull toward the flipped floor.
 */
@Mixin(value = SwingManager.class, remap = false)
public abstract class MarvelLegacySwingGravityMixin {
    @WrapOperation(method = "calculateSwingVelocity", remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private static void integratedSuperheroes$setWorldVelocity(Player player, Vec3 velocity, Operation<Void> original) {
        if (HeroGravity.inverted(player)) {
            GravityApi.setWorldVelocity(player, velocity);
        } else {
            original.call(player, velocity);
        }
    }

    @WrapOperation(method = "calculateSwingVelocity", remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 integratedSuperheroes$getWorldVelocity(Player player, Operation<Vec3> original) {
        return HeroGravity.inverted(player) ? GravityApi.getWorldVelocity(player) : original.call(player);
    }

    @ModifyExpressionValue(method = "calculateSwingVelocity", remap = false, at = @At(value = "FIELD",
            target = "Lcom/stxllarplanes/marvellegacy/util/SwingManager;GRAVITY:Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 integratedSuperheroes$flipSwingGravity(Vec3 gravity, Player player, Vec3 anchorPos, double ropeLength) {
        return HeroGravity.inverted(player) ? gravity.scale(-1.0D) : gravity;
    }
}
