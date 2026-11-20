package com.craisinlord.integrated_superheroes.voidworld.pruning;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PruningEntities {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, IntegratedSuperheroes.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<PrunedChunkEntity>> PRUNED_CHUNK = TYPES.register("pruned_chunk",
            () -> EntityType.Builder.<PrunedChunkEntity>of(PrunedChunkEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .noSummon()
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(20)
                    .build("pruned_chunk"));

    private PruningEntities() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        if (FMLEnvironment.dist.isClient()) {
            com.craisinlord.integrated_superheroes.voidworld.client.pruning.PrunedChunkClient.register(modBus);
        }
    }
}
