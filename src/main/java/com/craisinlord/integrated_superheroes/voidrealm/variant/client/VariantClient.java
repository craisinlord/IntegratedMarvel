package com.craisinlord.integrated_superheroes.voidrealm.variant.client;

import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class VariantClient {
    private VariantClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(VariantClient::renderers);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VariantEntities.VARIANT.get(), VariantRenderer::new);
    }
}
