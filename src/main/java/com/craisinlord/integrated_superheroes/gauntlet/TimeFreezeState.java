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
    private static volatile double clientRadius;
    private static volatile boolean clientInField;

    private TimeFreezeState() {
    }

    public static void setClientOwners(Collection<UUID> owners, double radius) {
        clientOwners = Set.copyOf(owners);
        clientRadius = radius;
    }

    public static void setClientInField(boolean inField) {
        clientInField = inField;
    }

    public static Set<UUID> clientOwners() {
        return clientOwners;
    }

    public static boolean clientActive() {
        return !clientOwners.isEmpty() && clientInField;
    }

    public static double serverRadius() {
        return GauntletConfig.CHRONOSTASIS_RADIUS.get();
    }

    public static boolean isFrozen(Entity entity) {
        Level level = entity.level();
        Set<UUID> owners = level.isClientSide ? clientOwners : TimeFreezeManager.owners(level.dimension());
        if (owners.isEmpty() || owners.contains(entity.getUUID())) return false;
        if (immune(entity)) return false;
        if (!level.isClientSide && entity instanceof Player) return false;
        if (entity instanceof LivingEntity living && living.isDeadOrDying()) return false;
        if (entity.isVehicle()) {
            for (Entity passenger : entity.getIndirectPassengers()) {
                if (owners.contains(passenger.getUUID())) return false;
            }
        }
        return inField(entity, owners);
    }

    public static boolean immune(Entity entity) {
        Entity root = entity instanceof net.neoforged.neoforge.entity.PartEntity<?> part && part.getParent() != null ? part.getParent() : entity;
        return root.getType().is(com.craisinlord.anthology.time.TimeDilationTags.TIME_DILATION_IMMUNE);
    }

    public static boolean isFrozenPlayer(Entity entity) {
        if (!(entity instanceof Player player) || player.isDeadOrDying()) return false;
        Level level = entity.level();
        Set<UUID> owners = level.isClientSide ? clientOwners : TimeFreezeManager.owners(level.dimension());
        return !owners.isEmpty() && !owners.contains(entity.getUUID()) && inField(entity, owners);
    }

    public static boolean inField(Entity entity, Set<UUID> owners) {
        Level level = entity.level();
        double radius = level.isClientSide ? clientRadius : serverRadius();
        if (radius <= 0.0D) return true;
        double radiusSqr = radius * radius;
        for (UUID owner : owners) {
            Player freezer = level.getPlayerByUUID(owner);
            if (freezer != null && freezer.distanceToSqr(entity) <= radiusSqr) return true;
        }
        return false;
    }
}
