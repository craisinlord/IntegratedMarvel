package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class ViltrumiteGuardsClientEvents {
    private ViltrumiteGuardsClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (ModCompat.VILTRUMITE) {
            ViltrumiteClientGuards.tick();
        }
    }
}
