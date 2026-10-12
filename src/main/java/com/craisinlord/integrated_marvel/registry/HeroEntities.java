package com.craisinlord.integrated_marvel.registry;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.craisinlord.integrated_marvel.hero.FantasticSeat;
import com.craisinlord.integrated_marvel.hero.SwarmSquirrel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HeroEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, IntegratedMarvel.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SwarmSquirrel>> SWARM_SQUIRREL = ENTITY_TYPES.register("swarm_squirrel",
            () -> EntityType.Builder.<SwarmSquirrel>of(SwarmSquirrel::new, MobCategory.MISC)
                    .sized(0.6F, 0.7F)
                    .clientTrackingRange(10)
                    .build("swarm_squirrel"));

    public static final DeferredHolder<EntityType<?>, EntityType<FantasticSeat>> FANTASTIC_SEAT = ENTITY_TYPES.register("fantastic_seat",
            () -> EntityType.Builder.<FantasticSeat>of(FantasticSeat::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .clientTrackingRange(10)
                    .build("fantastic_seat"));

    private HeroEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(HeroEntities::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(SWARM_SQUIRREL.get(), SwarmSquirrel.createAttributes().build());
    }
}
