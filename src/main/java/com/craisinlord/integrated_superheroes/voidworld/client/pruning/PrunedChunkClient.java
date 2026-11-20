package com.craisinlord.integrated_superheroes.voidworld.client.pruning;

import com.craisinlord.integrated_superheroes.voidworld.pruning.PruningEntities;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class PrunedChunkClient {
    private PrunedChunkClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PrunedChunkClient::renderers);
        modBus.addListener(PrunedChunkClient::reloadListeners);
        NeoForge.EVENT_BUS.addListener(PrunedChunkClient::onLogout);
        NeoForge.EVENT_BUS.addListener(PrunedChunkClient::onTick);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PruningEntities.PRUNED_CHUNK.get(), PrunedChunkRenderer::new);
    }

    private static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> PrunedMeshCache.clear());
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        PrunedMeshCache.clear();
    }

    private static void onTick(ClientTickEvent.Post event) {
        PrunedMeshCache.sweep();
    }
}
