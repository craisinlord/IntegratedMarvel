package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.anthology.gravity.GravityApi;
import com.craisinlord.integrated_superheroes.hero.HeroGravity;
import com.craisinlord.integrated_superheroes.hero.SpiderCrawlFrame;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.stxllarplanes.marvellegacy.power.SpiderPowers;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Spider-Man's wall and ceiling crawl (client, while jump is held) measures walls and ceilings in world space: "the
 * ceiling" is 1.8 to 2.2 blocks above the feet and "forward" uses the vanilla yaw formula. With flipped gravity that
 * found the floor under the player's feet, so every jump grabbed a "ceiling", rolled the camera and launched the player
 * at 4.5 blocks a tick.
 *
 * <p>While flipped, the five probes are answered in the player's frame by {@link SpiderCrawlFrame}, and the two
 * position nudges (stepping along a ceiling, rounding a ceiling edge onto a wall) are turned from player-frame offsets
 * into world offsets. Everything else in the crawl already works in the player's frame: setDeltaMovement is
 * player-frame in Anthology, so climbing (+0.45) goes world-down and ceiling cling (+4.5) presses into the world floor;
 * yaw, body yaw and the camera roll are all relative to the flipped view. Crawling pauses during a gravity flip.
 */
@Mixin(value = SpiderPowers.class, remap = false)
public abstract class MarvelLegacySpiderGravityMixin {
    @Inject(method = "wallClimb", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$pauseCrawlWhileFlipping(PlayerTickEvent.Post event, CallbackInfo ci) {
        Player player = event.getEntity();
        if (player.level().isClientSide() && GravityApi.getTransitionRemaining(player) > 0) {
            SpiderPowers.animationCleanup(player);
            ci.cancel();
        }
    }

    @Inject(method = "wallDetection", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$wallInPlayerFrame(Player player, CallbackInfoReturnable<Direction> cir) {
        if (HeroGravity.inverted(player)) {
            cir.setReturnValue(SpiderCrawlFrame.wallDetection(player));
        }
    }

    @Inject(method = "ceilingDetection", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$ceilingInPlayerFrame(Player player, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (HeroGravity.inverted(player)) {
            cir.setReturnValue(SpiderCrawlFrame.ceilingDetection(player, distance));
        }
    }

    @Inject(method = "nextCeilingDetection", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$nextCeilingInPlayerFrame(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (HeroGravity.inverted(player)) {
            cir.setReturnValue(SpiderCrawlFrame.nextCeilingDetection(player));
        }
    }

    @Inject(method = "getForwardCeilingHeight", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$forwardCeilingInPlayerFrame(Player player, CallbackInfoReturnable<Double> cir) {
        if (HeroGravity.inverted(player)) {
            cir.setReturnValue(SpiderCrawlFrame.forwardCeilingHeight(player));
        }
    }

    @Inject(method = "getCurrentCeilingHeight", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$currentCeilingInPlayerFrame(Player player, CallbackInfoReturnable<Double> cir) {
        if (HeroGravity.inverted(player)) {
            cir.setReturnValue(SpiderCrawlFrame.currentCeilingHeight(player));
        }
    }

    /** Both setPos calls are "current position plus a player-frame offset"; flipped, that offset is (-x, -y, z) in the world. */
    @WrapOperation(method = "wallClimb", remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;setPos(DDD)V"))
    private static void integratedSuperheroes$nudgeInWorldFrame(Player player, double x, double y, double z, Operation<Void> original) {
        if (HeroGravity.inverted(player)) {
            original.call(player, 2.0D * player.getX() - x, 2.0D * player.getY() - y, z);
        } else {
            original.call(player, x, y, z);
        }
    }

    /** The free-space check before a ceiling step moves the bounding box by the same player-frame offset. */
    @WrapOperation(method = "wallClimb", remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/AABB;move(DDD)Lnet/minecraft/world/phys/AABB;"))
    private static AABB integratedSuperheroes$checkStepInWorldFrame(AABB box, double x, double y, double z, Operation<AABB> original,
            @Local(argsOnly = true) PlayerTickEvent.Post event) {
        return HeroGravity.inverted(event.getEntity()) ? original.call(box, -x, -y, z) : original.call(box, x, y, z);
    }
}
