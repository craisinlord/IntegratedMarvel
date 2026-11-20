package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.mixin.TimerQueueAccessor;
import com.google.common.collect.Table;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.timers.TimerQueue;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.tintankgames.marvel.world.level.timers.SetItemInCurioSlotCallback;
import net.tintankgames.marvel.world.level.timers.SetItemInSlotCallback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Marvel mod's necklace suit-up/suit-down (Black Panther, Shuri, Killmonger) and the Mark 5 suitcase
 * schedule each armor piece on the overworld timer queue, keyed by player UUID. The necklace is built
 * from a snapshot of the armor when the key is pressed but only lands in the curio slot 30 ticks later,
 * after the last piece is cleared. Dying inside that window drops the pieces that were still worn and
 * then hands the respawned player the full-suit necklace anyway (dupe). Logging out inside it leaves a
 * curio callback with no player, which throws.
 *
 * Fix: when the player dies (for real) or logs out, run that player's pending swap steps right now, in
 * order, so the swap finishes before death drops or logout save happen.
 */
@EventBusSubscriber(modid = IntegratedSuperheroes.MODID)
public final class MarvelSuitSwapGuard {
    private MarvelSuitSwapGuard() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            finishPendingSwaps(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            finishPendingSwaps(player);
        }
    }

    @SuppressWarnings("unchecked")
    public static void finishPendingSwaps(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TimerQueue<MinecraftServer> queue = server.getWorldData().overworldData().getScheduledEvents();
        Table<String, Long, TimerQueue.Event<MinecraftServer>> events =
                ((TimerQueueAccessor<MinecraftServer>) (Object) queue).integratedSuperheroes$events();
        String prefix = player.getStringUUID() + "_";
        List<TimerQueue.Event<MinecraftServer>> pending = new ArrayList<>();
        for (TimerQueue.Event<MinecraftServer> entry : events.values()) {
            if (entry.id.startsWith(prefix)
                    && (entry.callback instanceof SetItemInSlotCallback || entry.callback instanceof SetItemInCurioSlotCallback)) {
                pending.add(entry);
            }
        }
        if (pending.isEmpty()) {
            return;
        }
        pending.sort(Comparator.<TimerQueue.Event<MinecraftServer>>comparingLong(entry -> entry.triggerTime)
                .thenComparing(entry -> entry.sequentialId));
        for (TimerQueue.Event<MinecraftServer> entry : pending) {
            queue.remove(entry.id);
        }
        long now = server.overworld().getGameTime();
        for (TimerQueue.Event<MinecraftServer> entry : pending) {
            entry.callback.handle(server, queue, now);
        }
    }
}
