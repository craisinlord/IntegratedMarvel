package com.craisinlord.integrated_superheroes.registry;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModTags {
    public static final TagKey<EntityType<?>> DEADPOOL_WATCHERS = TagKey.create(Registries.ENTITY_TYPE, IntegratedSuperheroes.id("deadpool_watchers"));

    private ModTags() {
    }
}
