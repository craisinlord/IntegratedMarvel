package com.craisinlord.integrated_superheroes.api;

import com.craisinlord.integrated_superheroes.hero.RoguePower;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class RogueAbsorption {
    private static final Map<ResourceLocation, Function<LivingEntity, RogueMemory>> BY_ENTITY = new HashMap<>();
    private static final List<Matcher> MATCHERS = new ArrayList<>();
    private static final Map<ResourceLocation, RogueMemory> BY_ID = new HashMap<>();

    private RogueAbsorption() {
    }

    private record Matcher(Predicate<LivingEntity> test, Function<LivingEntity, RogueMemory> memory) {
    }

    public static void register(ResourceLocation entityId, RogueMemory memory) {
        remember(memory);
        BY_ENTITY.put(entityId, entity -> memory);
    }

    public static void registerIfAbsent(ResourceLocation entityId, RogueMemory memory) {
        remember(memory);
        BY_ENTITY.putIfAbsent(entityId, entity -> memory);
    }

    public static void register(EntityType<?> type, RogueMemory memory) {
        register(BuiltInRegistries.ENTITY_TYPE.getKey(type), memory);
    }

    public static void register(ResourceLocation entityId, Function<LivingEntity, RogueMemory> factory, RogueMemory... variants) {
        for (RogueMemory variant : variants) {
            remember(variant);
        }
        BY_ENTITY.put(entityId, factory);
    }

    public static void registerIfAbsent(ResourceLocation entityId, Function<LivingEntity, RogueMemory> factory, RogueMemory... variants) {
        for (RogueMemory variant : variants) {
            remember(variant);
        }
        BY_ENTITY.putIfAbsent(entityId, factory);
    }

    public static void registerMatcher(Predicate<LivingEntity> test, RogueMemory memory) {
        remember(memory);
        MATCHERS.add(new Matcher(test, entity -> memory));
    }

    public static void remember(RogueMemory memory) {
        BY_ID.put(memory.id(), memory);
    }

    public static Optional<RogueMemory> memoryFor(LivingEntity entity) {
        Function<LivingEntity, RogueMemory> factory = BY_ENTITY.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        if (factory != null) {
            RogueMemory memory = factory.apply(entity);
            if (memory != null) {
                return Optional.of(memory);
            }
        }
        for (Matcher matcher : MATCHERS) {
            if (matcher.test().test(entity)) {
                return Optional.ofNullable(matcher.memory().apply(entity));
            }
        }
        return Optional.empty();
    }

    public static boolean hasMemory(EntityType<?> type) {
        return BY_ENTITY.containsKey(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    public static Optional<RogueMemory> byId(ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static boolean isRogue(Player player) {
        return RoguePower.isRogue(player);
    }

    public static List<RogueMemory> memories(ServerPlayer player) {
        return RoguePower.memories(player);
    }

    public static boolean grant(ServerPlayer player, RogueMemory memory, String source, double seconds) {
        return RoguePower.grant(player, memory, source, seconds);
    }
}
