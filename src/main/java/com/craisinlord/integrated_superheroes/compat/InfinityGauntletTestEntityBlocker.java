package com.craisinlord.integrated_superheroes.compat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class InfinityGauntletTestEntityBlocker {
    private static final Map<EntityType<?>, Boolean> VERDICTS = new ConcurrentHashMap<>();

    private InfinityGauntletTestEntityBlocker() {
    }

    public static boolean isTestEntity(EntityType<?> type) {
        return VERDICTS.computeIfAbsent(type, InfinityGauntletTestEntityBlocker::check);
    }

    private static boolean check(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id.getNamespace().equals("infinity_gauntlet") && id.getPath().startsWith("test_entity");
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (isTestEntity(event.getEntity().getType())) {
            event.setCanceled(true);
        }
    }
}
