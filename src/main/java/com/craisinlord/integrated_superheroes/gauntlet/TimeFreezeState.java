package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public final class TimeFreezeState {
    private static volatile Set<UUID> clientOwners = Set.of();

    private TimeFreezeState() {
    }

    public static void setClientOwners(Collection<UUID> owners) {
        clientOwners = Set.copyOf(owners);
    }

    public static Set<UUID> clientOwners() {
        return clientOwners;
    }

    public static boolean clientActive() {
        return !clientOwners.isEmpty();
    }

    public static boolean isFrozen(Entity entity) {
        Level level = entity.level();
        Set<UUID> owners = level.isClientSide ? clientOwners : TimeFreezeManager.owners(level.dimension());
        if (owners.isEmpty() || owners.contains(entity.getUUID())) return false;
        if (!level.isClientSide && entity instanceof Player) return false;
        if (entity instanceof LivingEntity living && living.isDeadOrDying()) return false;
        for (Entity passenger : entity.getIndirectPassengers()) {
            if (owners.contains(passenger.getUUID())) return false;
        }
        return true;
    }

    public static boolean isFrozenPlayer(Entity entity) {
        if (!(entity instanceof Player player) || player.isDeadOrDying()) return false;
        Level level = entity.level();
        Set<UUID> owners = level.isClientSide ? clientOwners : TimeFreezeManager.owners(level.dimension());
        return !owners.isEmpty() && !owners.contains(entity.getUUID());
    }
}
