package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.integrated_superheroes.hero.HeroGravity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.stxllarplanes.marvellegacy.power.webpowers.WebPowers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Web zip works out a world-space velocity toward the anchor and hands it straight to setDeltaMovement, which is in
 * the player's own frame. With flipped gravity that frame is turned over (x and y negated), so the zip pulled the
 * player backwards and away from the anchor. See {@link MarvelLegacySwingGravityMixin} for web swinging.
 */
@Mixin(value = WebPowers.class, remap = false)
public abstract class MarvelLegacyWebGravityMixin {
    @WrapOperation(method = "zipTick", remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private static void integratedSuperheroes$zipInWorldFrame(Player player, Vec3 velocity, Operation<Void> original) {
        if (HeroGravity.inverted(player)) {
            GravityApi.setWorldVelocity(player, velocity);
        } else {
            original.call(player, velocity);
        }
    }
}
