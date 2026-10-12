package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.antarchy.content.client.renderer.FlyingSquirrelRenderer;
import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.craisinlord.integrated_marvel.registry.HeroEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = IntegratedMarvel.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HeroClientRegistration {
    private HeroClientRegistration() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HeroEntities.SWARM_SQUIRREL.get(), FlyingSquirrelRenderer::new);
        event.registerEntityRenderer(HeroEntities.FANTASTIC_SEAT.get(), NoopRenderer::new);
    }
}
