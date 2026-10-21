package com.craisinlord.integrated_superheroes.mixin;

import dev.baranhan.viltrumiteflight.client.render.FlightAnimManager;
import java.util.Map;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = FlightAnimManager.class, remap = false)
public interface ViltrumiteFlightAnimAccessor {
    @Accessor("STATES")
    static Map<UUID, FlightAnimManager.AnimState> integratedSuperheroes$states() {
        throw new AssertionError();
    }
}
