package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;

public final class InfiniteDimensionsCompat {
    public static final String NAMESPACE = "infinity";

    public enum Generation {
        READY,
        PENDING,
        REJECTED,
        UNAVAILABLE
    }

    private InfiniteDimensionsCompat() {
    }

    public static ResourceLocation randomDimensionId(RandomSource random) {
        if (!ModCompat.INFINITE_DIMENSIONS) {
            return null;
        }
        try {
            Method method = Class.forName("net.codexarchonic.infinity.util.InfinityMethods").getMethod("getRandomId", RandomSource.class);
            return method.invoke(null, random) instanceof ResourceLocation id ? id : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            IntegratedSuperheroes.LOGGER.error("Failed to get a random Infinite Dimensions id", exception);
            return null;
        }
    }

    public static Generation requestCreation(MinecraftServer server, ResourceLocation id) {
        if (!ModCompat.INFINITE_DIMENSIONS) {
            return Generation.UNAVAILABLE;
        }
        if (server == null || id == null || !NAMESPACE.equals(id.getNamespace())) {
            return Generation.REJECTED;
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        if (server.getLevel(key) != null) {
            return Generation.READY;
        }
        try {
            Class<?> access = Class.forName("net.codexarchonic.infinity.access.MinecraftServerAccess");
            if (access.isInstance(server) && Boolean.TRUE.equals(access.getMethod("infinity$hasToAdd", ResourceKey.class).invoke(server, key))) {
                return Generation.PENDING;
            }
            Method create = Class.forName("net.codexarchonic.infinity.util.teleport.PortalCreator")
                    .getMethod("tryAddInfinityDimension", MinecraftServer.class, ResourceLocation.class);
            return Boolean.TRUE.equals(create.invoke(null, server, id)) ? Generation.PENDING : Generation.REJECTED;
        } catch (ReflectiveOperationException | LinkageError exception) {
            IntegratedSuperheroes.LOGGER.error("Failed to request Infinite Dimensions dimension {}", id, exception);
            return Generation.UNAVAILABLE;
        }
    }
}
