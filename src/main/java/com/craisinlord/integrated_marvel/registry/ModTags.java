package com.craisinlord.integrated_marvel.registry;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModTags {
    public static final TagKey<EntityType<?>> DEADPOOL_WATCHERS = TagKey.create(Registries.ENTITY_TYPE, IntegratedMarvel.id("deadpool_watchers"));

    private ModTags() {
    }
}
