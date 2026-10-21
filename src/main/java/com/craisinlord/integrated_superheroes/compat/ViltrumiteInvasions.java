package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.hero.MarvelBalanceConfig;
import dev.baranhan.viltrumitecore.config.ViltrumiteCoreConfig;
import dev.baranhan.viltrumitecore.util.PunchImpactManager;
import dev.baranhan.viltrumitecore.util.ThunderClapManager;
import dev.baranhan.viltrumitecore.worldevent.WorldEventManager;
import dev.baranhan.viltrumitecore.worldevent.WorldEventType;
import dev.baranhan.viltrumitecore.worldevent.WorldEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public final class ViltrumiteInvasions {
    private static final Set<Entity> NPC_LAUNCHED = Collections.newSetFromMap(new WeakHashMap<>());
    private static Thread guardThread;
    private static int guardDepth;

    private ViltrumiteInvasions() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ViltrumiteInvasions::onPickup);
    }

    private static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (ViltrumiteBridge.isNpc(event.getPlayer()) && !MarvelBalanceConfig.VILTRUMITE_NPC_PICKUP.get()) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    public static boolean gated() {
        return MarvelBalanceConfig.VILTRUMITE_INVASION_NEEDS_DRAGON.get() || !MarvelBalanceConfig.VILTRUMITE_INVASION_ADVANCEMENT.get().isBlank();
    }

    public static void roll(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD || level.players().isEmpty()) {
            return;
        }
        WorldEventManager manager = WorldEventManager.get(level);
        if (!manager.activeEvents().isEmpty()) {
            return;
        }
        ViltrumiteCoreConfig config = ViltrumiteCoreConfig.INSTANCE;
        long cooldown = config.worldEventCooldownMinutes * 60L * 20L;
        if (level.getGameTime() - manager.lastEventTime() < cooldown) {
            return;
        }
        if (level.random.nextFloat() >= config.worldEventChancePercent * 0.01F) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (MarvelBalanceConfig.VILTRUMITE_INVASION_NEEDS_DRAGON.get() && !dragonKilled(server)) {
            return;
        }
        List<ServerPlayer> targets = targets(level, server);
        if (targets.isEmpty()) {
            return;
        }
        List<WorldEventType> types = new ArrayList<>();
        for (WorldEventType type : WorldEvents.all()) {
            if (!manager.isRunning(type)) {
                types.add(type);
            }
        }
        if (types.isEmpty()) {
            return;
        }
        WorldEventType type = types.get(level.random.nextInt(types.size()));
        ServerPlayer target = targets.get(level.random.nextInt(targets.size()));
        manager.start(level, type, target.position(), type.defaultWarmupTicks());
    }

    private static boolean dragonKilled(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        EndDragonFight fight = end == null ? null : end.getDragonFight();
        return fight != null && fight.hasPreviouslyKilledDragon();
    }

    private static List<ServerPlayer> targets(ServerLevel level, MinecraftServer server) {
        String id = MarvelBalanceConfig.VILTRUMITE_INVASION_ADVANCEMENT.get().trim();
        AdvancementHolder advancement = null;
        if (!id.isEmpty()) {
            ResourceLocation key = ResourceLocation.tryParse(id);
            advancement = key == null ? null : server.getAdvancements().get(key);
            if (advancement == null) {
                return List.of();
            }
        }
        List<ServerPlayer> targets = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (ViltrumiteBridge.isNpc(player) || player.isSpectator()) {
                continue;
            }
            if (advancement == null || player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                targets.add(player);
            }
        }
        return targets;
    }

    public static boolean enterGuard(boolean npc) {
        if (!npc || MarvelBalanceConfig.VILTRUMITE_NPC_BREAK_BLOCK_ENTITIES.get()) {
            return false;
        }
        guardThread = Thread.currentThread();
        guardDepth++;
        return true;
    }

    public static void exitGuard(boolean entered) {
        if (entered && guardDepth > 0) {
            guardDepth--;
        }
    }

    public static boolean protects(BlockBehaviour.BlockStateBase state) {
        return guardDepth > 0 && Thread.currentThread() == guardThread && state.hasBlockEntity();
    }

    public static Set<Entity> launchedSnapshot() {
        return new HashSet<>(PunchImpactManager.LAUNCHED_ENTITIES.keySet());
    }

    public static void markLaunched(Set<Entity> before) {
        for (Entity entity : PunchImpactManager.LAUNCHED_ENTITIES.keySet()) {
            if (!before.contains(entity)) {
                NPC_LAUNCHED.add(entity);
            }
        }
    }

    public static boolean npcMeteorsActive() {
        NPC_LAUNCHED.retainAll(PunchImpactManager.LAUNCHED_ENTITIES.keySet());
        return !NPC_LAUNCHED.isEmpty();
    }

    public static boolean npcClapsActive() {
        for (ThunderClapManager.ActiveClap clap : ThunderClapManager.ACTIVE_CLAPS) {
            if (ViltrumiteBridge.isNpc(clap.attacker)) {
                return true;
            }
        }
        return false;
    }
}
