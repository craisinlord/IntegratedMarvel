package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID)
public final class SoulRitualEvents {
    private SoulRitualEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SoulRitualManager.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        SoulRitualManager.onLivingDeath(event);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        SoulRitualManager.onLivingFall(event);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SoulRitualManager.clear();
    }
}
