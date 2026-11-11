package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.HeroGravity;
import com.craisinlord.integrated_superheroes.hero.SpiderCrawlFrame;
import com.stxllarplanes.marvellegacy.power.WolverinePowers;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wolverine's claw climb uses the same world-space wall probe as Spider-Man's crawl (see
 * {@link MarvelLegacySpiderGravityMixin}). While flipped it is answered in the player's frame; the climb itself only
 * sets player-frame velocity, so it already goes world-down.
 */
@Mixin(value = WolverinePowers.class, remap = false)
public abstract class MarvelLegacyWolverineGravityMixin {
    @Inject(method = "wallDetection", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$wallInPlayerFrame(Player player, CallbackInfoReturnable<Direction> cir) {
        if (HeroGravity.inverted(player)) {
            cir.setReturnValue(SpiderCrawlFrame.wallDetection(player));
        }
    }
}
