package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.registry.HeroEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HeroClientRegistration {
    private HeroClientRegistration() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HeroEntities.SWARM_SQUIRREL.get(), SwarmSquirrelRenderer::new);
        event.registerEntityRenderer(HeroEntities.FANTASTIC_SEAT.get(), NoopRenderer::new);
        event.registerEntityRenderer(HeroEntities.KINETIC_CHARGE.get(), KineticChargeRenderer::new);
    }
}
