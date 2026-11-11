package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class IronManReactor {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final double SPEND_THRESHOLD = 200.0D;

    private static final Map<UUID, Double> LAST_TOTAL = new HashMap<>();
    private static final Map<UUID, Long> PAUSED_UNTIL = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_POSITION = new HashMap<>();

    private IronManReactor() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !IronManSuits.isLoaded()) {
            return;
        }
        UUID id = player.getUUID();
        String suitId = player.isSpectator() ? null : IronManSuits.fullSuitId(player);
        IronManSuits.Stats stats = suitId == null ? null : IronManSuits.get(suitId);
        if (stats == null || !IronManEnergy.hasEnergy(player.getItemBySlot(EquipmentSlot.CHEST))) {
            forget(id);
            return;
        }

        long now = player.serverLevel().getGameTime();
        double total = total(player);
        Double last = LAST_TOTAL.get(id);
        if (last != null && last - total > SPEND_THRESHOLD) {
            PAUSED_UNTIL.put(id, now + Math.round(IronManEnergy.REACTOR_PAUSE_SECONDS.get() * 20.0D));
        }

        Vec3 position = player.position();
        Vec3 lastPosition = LAST_POSITION.put(id, position);
        if (now >= PAUSED_UNTIL.getOrDefault(id, 0L)) {
            double output = stats.reactorOutput().get();
            if (standingStill(player, position, lastPosition)) {
                output *= IronManEnergy.REACTOR_IDLE_MULTIPLIER.get();
            }
            double cap = stats.reactorCap().get();
            for (EquipmentSlot slot : SLOTS) {
                ItemStack piece = player.getItemBySlot(slot);
                double room = IronManEnergy.capacity(piece) * cap - IronManEnergy.getFE(piece);
                if (room > 0.0D) {
                    IronManEnergy.addFE(piece, Math.min(room, output / SLOTS.length));
                }
            }
            total = total(player);
        }
        LAST_TOTAL.put(id, total);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        forget(event.getEntity().getUUID());
    }

    private static void forget(UUID id) {
        LAST_TOTAL.remove(id);
        PAUSED_UNTIL.remove(id);
        LAST_POSITION.remove(id);
    }

    private static boolean standingStill(ServerPlayer player, Vec3 position, Vec3 lastPosition) {
        return lastPosition != null && player.onGround() && !player.getAbilities().flying
                && position.distanceToSqr(lastPosition) < 1.0E-4D;
    }

    private static double total(ServerPlayer player) {
        double total = 0.0D;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack piece = player.getItemBySlot(slot);
            if (IronManEnergy.hasEnergy(piece)) {
                total += IronManEnergy.getFE(piece);
            }
        }
        return total;
    }
}
