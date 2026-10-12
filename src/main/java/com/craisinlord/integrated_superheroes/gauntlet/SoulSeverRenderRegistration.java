package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SoulSeverRenderRegistration {
    private SoulSeverRenderRegistration() {
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var entityType : event.getEntityTypes()) {
            addLayer(event.getRenderer(entityType));
        }
        for (var skin : event.getSkins()) {
            addLayer(event.getSkin(skin));
        }
    }

    private static void addLayer(EntityRenderer<?> renderer) {
        if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
            addLivingLayer(livingRenderer);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addLivingLayer(LivingEntityRenderer renderer) {
        renderer.addLayer(new SoulSeverRenderLayer(renderer));
    }
}
