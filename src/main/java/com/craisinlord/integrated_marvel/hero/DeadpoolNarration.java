package com.craisinlord.integrated_marvel.hero;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DeadpoolNarration {
    public static final HeroPower.Toggle TOGGLE = new HeroPower.Toggle("integrated_marvel_off_narration", "Narration", () -> Items.WRITABLE_BOOK);
    private static final Map<String, Trigger> TRIGGERS = new HashMap<>();
    private static final Map<UUID, Memory> MEMORY = new HashMap<>();

    static {
        priority("downed", 3);
        priority("death_screen", 2);
        priority("stand", 3);
        priority("last_revive", 2);
        priority("real_death", 2);
        priority("fumble", 2);
        priority("inject", 2);
        priority("swap", 1);
        priority("snap", 1);
        add("low_health", 2, 60.0D, 1.0D);
        add("combo", 2, 20.0D, 1.0D);
        add("two_mobs", 1, 120.0D, 0.25D);
        add("superhero_landing", 1, 30.0D, 1.0D);
        add("unload", 2, 15.0D, 0.5D);
        add("no_gun", 1, 5.0D, 1.0D);
        add("grenade", 2, 15.0D, 0.5D);
        add("creeper", 2, 20.0D, 1.0D);
        add("skeleton", 1, 60.0D, 0.5D);
        add("deep_dark", 1, 600.0D, 1.0D);
        add("warden", 1, 120.0D, 1.0D);
        add("dragon", 1, 300.0D, 1.0D);
        add("retcon", 2, 10.0D, 1.0D);
        add("smash_cut", 2, 0.0D, 1.0D);
        add("pause", 2, 10.0D, 1.0D);
        add("rage_quit", 3, 15.0D, 1.0D);
        add("dodge", 2, 15.0D, 1.0D);
        add("nether", 1, 300.0D, 1.0D);
        add("end", 1, 300.0D, 1.0D);
        add("night", 1, 600.0D, 1.0D);
        add("rain", 1, 600.0D, 1.0D);
        add("deep", 1, 600.0D, 1.0D);
        add("dig_down", 1, 300.0D, 1.0D);
        add("sleep", 1, 300.0D, 1.0D);
        add("eat", 2, 120.0D, 0.5D);
        add("inventory_full", 1, 300.0D, 1.0D);
        add("fall", 1, 60.0D, 1.0D);
        add("drowning", 1, 60.0D, 1.0D);
        add("idle", 2, 300.0D, 1.0D);
        add("crafting", 1, 300.0D, 0.5D);
        add("watcher", 1, 600.0D, 1.0D);
        add("hydra", 1, 120.0D, 1.0D);
        add("lucky", 1, 120.0D, 1.0D);
    }

    private DeadpoolNarration() {
    }

    private record Trigger(int lines, long cooldownTicks, double chance, boolean priority) {
    }

    private static final class Memory {
        private long globalReady;
        private final Map<String, Long> ready = new HashMap<>();
        private final Map<String, Integer> last = new HashMap<>();
    }

    private static void priority(String name, int lines) {
        TRIGGERS.put(name, new Trigger(lines, 0L, 1.0D, true));
    }

    private static void add(String name, int lines, double cooldownSeconds, double chance) {
        TRIGGERS.put(name, new Trigger(lines, (long) (cooldownSeconds * 20.0D), chance, false));
    }

    public static boolean say(ServerPlayer player, String trigger) {
        if (!DeadpoolPower.ID.equals(HeroPowers.data(player).getPrimaryPower()) || !HeroToggles.isOn(player, TOGGLE)) {
            return false;
        }
        return speak(player, trigger);
    }

    public static boolean speak(ServerPlayer player, String trigger) {
        Trigger entry = TRIGGERS.get(trigger);
        if (entry == null) {
            return false;
        }
        long now = player.server.getTickCount();
        Memory memory = MEMORY.computeIfAbsent(player.getUUID(), ignored -> new Memory());
        if (!entry.priority() && now < memory.globalReady || now < memory.ready.getOrDefault(trigger, 0L)) {
            return false;
        }
        if (entry.chance() < 1.0D && player.getRandom().nextDouble() >= entry.chance()) {
            return false;
        }
        int previous = memory.last.getOrDefault(trigger, -1);
        int line = player.getRandom().nextInt(entry.lines());
        if (entry.lines() > 1 && line == previous) {
            line = (line + 1 + player.getRandom().nextInt(entry.lines() - 1)) % entry.lines();
        }
        memory.last.put(trigger, line);
        memory.ready.put(trigger, now + entry.cooldownTicks());
        memory.globalReady = now + (long) (MarvelBalanceConfig.NARRATION_COOLDOWN.get() * 20.0D);
        broadcast(player, "integrated_marvel.deadpool." + trigger + "." + line);
        return true;
    }

    private static void broadcast(ServerPlayer speaker, String key) {
        DeadpoolCaptionPayload payload = new DeadpoolCaptionPayload(speaker.getId(), key);
        double range = MarvelBalanceConfig.NARRATION_RANGE.get();
        for (ServerPlayer viewer : speaker.serverLevel().players()) {
            if (viewer == speaker || viewer.distanceToSqr(speaker) <= range * range) {
                PacketDistributor.sendToPlayer(viewer, payload);
            }
        }
    }

    public static void forget(UUID player) {
        MEMORY.remove(player);
    }
}
