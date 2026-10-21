package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteInvasions;
import dev.baranhan.viltrumitecore.event.CoreEvents;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CoreEvents.class, remap = false)
public abstract class ViltrumiteInvasionGateMixin {
    @Inject(method = "onLevelTick", at = @At("HEAD"), cancellable = true)
    private static void integratedSuperheroes$gateRandomInvasions(LevelTickEvent.Post event, CallbackInfo ci) {
        if (!ViltrumiteInvasions.gated()) {
            return;
        }
        ci.cancel();
        if (event.getLevel() instanceof ServerLevel level) {
            ViltrumiteInvasions.roll(level);
        }
    }
}
