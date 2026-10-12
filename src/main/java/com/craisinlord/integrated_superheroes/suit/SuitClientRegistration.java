package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.registry.SuitEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SuitClientRegistration {
    private SuitClientRegistration() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SuitEntities.SUIT_MISSILE.get(), SuitMissileRenderer::new);
    }
}
