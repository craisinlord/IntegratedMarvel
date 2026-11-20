package com.craisinlord.integrated_superheroes.boss;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class WitherstormClient {
    private WitherstormClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WitherstormContent.WITHERSTORM.get(), WitherstormRenderer::new);
    }
}
