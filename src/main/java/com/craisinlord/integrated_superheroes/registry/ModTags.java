package com.craisinlord.integrated_superheroes.registry;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final TagKey<EntityType<?>> DEADPOOL_WATCHERS = TagKey.create(Registries.ENTITY_TYPE, IntegratedSuperheroes.id("deadpool_watchers"));
    public static final TagKey<EntityType<?>> REALITY_WARP_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, IntegratedSuperheroes.id("reality_warp_immune"));
    public static final TagKey<Block> UNLIFTABLE = TagKey.create(Registries.BLOCK, IntegratedSuperheroes.id("unliftable"));

    private ModTags() {
    }
}
