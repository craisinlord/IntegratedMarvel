package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SoulRitualManager {
    private static final ResourceLocation TEMPLE_ID = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "soul_temple");
    private static final ResourceLocation SOUL_STONE_ITEM_ID = ResourceLocation.parse("integrated_superheroes:soul_stone");
    private static final long REWARD_COOLDOWN = 7L * 24L * 60L * 60L * 20L;
    private static final long ENTRY_MESSAGE_COOLDOWN = 30L * 20L;
    private static final double RECIPIENT_RANGE_SQR = 48.0D * 48.0D;
    private static final Map<UUID, Long> NEXT_ENTRY_MESSAGE = new HashMap<>();
    private static final Map<UUID, Boolean> INSIDE_TEMPLE = new HashMap<>();

    private SoulRitualManager() {
    }

    public static boolean isInsideTemple(ServerLevel level, BlockPos position) {
        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, TEMPLE_ID);
        var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(key);
        return holder.isPresent() && level.structureManager().getStructureWithPieceAt(position, holder.get().value()).isValid();
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                UUID id = player.getUUID();
                boolean inside = isInsideTemple(level, player.blockPosition());
                boolean wasInside = INSIDE_TEMPLE.getOrDefault(id, false);
                if (inside && !wasInside && now >= NEXT_ENTRY_MESSAGE.getOrDefault(id, 0L)) {
                    player.sendSystemMessage(Component.translatable("message.integrated_superheroes.soul_ritual.warning"));
                    NEXT_ENTRY_MESSAGE.put(id, now + ENTRY_MESSAGE_COOLDOWN);
                }
                INSIDE_TEMPLE.put(id, inside);
            }
        }
        INSIDE_TEMPLE.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        NEXT_ENTRY_MESSAGE.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null
                && now >= NEXT_ENTRY_MESSAGE.getOrDefault(id, 0L));
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        Entity deceased = event.getEntity();
        if (!isInsideTemple(level, deceased.blockPosition())) return;

        ServerPlayer recipient;
        if (deceased instanceof Player) {
            recipient = nearestEligiblePlayer(level, deceased);
        } else if (deceased instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownable.getOwnerUUID());
            recipient = owner != null && owner.isAlive() && owner.serverLevel() == level
                    && owner.distanceToSqr(deceased) <= RECIPIENT_RANGE_SQR
                    && isInsideTemple(level, owner.blockPosition()) ? owner : null;
        } else {
            return;
        }

        if (recipient == null) {
            broadcast(level, deceased, Component.translatable("message.integrated_superheroes.soul_ritual.no_recipient"));
            return;
        }

        long now = level.getServer().overworld().getGameTime();
        SoulRitualSavedData data = data(level);
        long until = data.cooldownUntil(recipient.getUUID());
        if (until > now) {
            long remainingDays = Math.max(1L, (until - now + 1_727_999L) / 1_728_000L);
            recipient.sendSystemMessage(Component.translatable("message.integrated_superheroes.soul_ritual.cooldown", remainingDays));
            broadcast(level, deceased, Component.translatable("message.integrated_superheroes.soul_ritual.cooldown_nearby", recipient.getDisplayName()));
            return;
        }

        var stone = BuiltInRegistries.ITEM.getOptional(SOUL_STONE_ITEM_ID);
        if (stone.isEmpty()) {
            IntegratedSuperheroes.LOGGER.error("Soul Stone item {} is unavailable; ritual reward was not granted", SOUL_STONE_ITEM_ID);
            return;
        }
        ItemStack reward = new ItemStack(stone.get());
        if (!recipient.getInventory().add(reward)) recipient.drop(reward, false);
        data.setCooldown(recipient.getUUID(), now + REWARD_COOLDOWN);
        recipient.sendSystemMessage(Component.translatable("message.integrated_superheroes.soul_ritual.claimed"));
        broadcast(level, deceased, Component.translatable("message.integrated_superheroes.soul_ritual.claimed_nearby", recipient.getDisplayName()));
    }

    public static void clear() {
        INSIDE_TEMPLE.clear();
        NEXT_ENTRY_MESSAGE.clear();
    }

    private static ServerPlayer nearestEligiblePlayer(ServerLevel level, Entity deceased) {
        ServerPlayer closest = null;
        double closestDistance = RECIPIENT_RANGE_SQR;
        ServerPlayer nearestCoolingDown = null;
        double nearestCoolingDownDistance = RECIPIENT_RANGE_SQR;
        for (ServerPlayer candidate : level.players()) {
            if (candidate == deceased || !candidate.isAlive() || !isInsideTemple(level, candidate.blockPosition())) continue;
            double distance = candidate.distanceToSqr(deceased);
            if (isEligible(candidate) && distance <= closestDistance) {
                closest = candidate;
                closestDistance = distance;
            } else if (!isEligible(candidate) && distance <= nearestCoolingDownDistance) {
                nearestCoolingDown = candidate;
                nearestCoolingDownDistance = distance;
            }
        }
        return closest != null ? closest : nearestCoolingDown;
    }

    private static boolean isEligible(ServerPlayer player) {
        return data(player.serverLevel()).cooldownUntil(player.getUUID()) <= player.server.overworld().getGameTime();
    }

    private static SoulRitualSavedData data(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(SoulRitualSavedData.FACTORY, SoulRitualSavedData.DATA_ID);
    }

    private static void broadcast(ServerLevel level, Entity center, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) <= RECIPIENT_RANGE_SQR) player.sendSystemMessage(message);
        }
    }
}
