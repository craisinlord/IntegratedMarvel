package com.craisinlord.integrated_marvel.registry;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.craisinlord.integrated_marvel.suit.SuitMissileEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SuitEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, IntegratedMarvel.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SuitMissileEntity>> SUIT_MISSILE = ENTITY_TYPES.register("suit_missile",
            () -> EntityType.Builder.<SuitMissileEntity>of(SuitMissileEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("suit_missile"));

    private SuitEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
