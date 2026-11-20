package com.craisinlord.integrated_superheroes.boss;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WitherstormContent {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, IntegratedSuperheroes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IntegratedSuperheroes.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<WitherstormEntity>> WITHERSTORM = ENTITY_TYPES.register("witherstorm",
            () -> EntityType.Builder.<WitherstormEntity>of(WitherstormEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 1.4F)
                    .fireImmune()
                    .immuneTo(Blocks.WITHER_ROSE)
                    .clientTrackingRange(64)
                    .updateInterval(2)
                    .build("witherstorm"));

    public static final DeferredItem<Item> SPAWN_EGG = ITEMS.register("witherstorm_spawn_egg",
            () -> new DeferredSpawnEggItem(WITHERSTORM, 0x1B1024, 0x8A3FD1, new Item.Properties()));

    private WitherstormContent() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(WitherstormContent::attributes);
        modEventBus.addListener(WitherstormContent::creativeTabs);
        NeoForge.EVENT_BUS.register(WitherstormEvents.class);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(WITHERSTORM.get(), WitherstormEntity.createAttributes().build());
    }

    private static void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(SPAWN_EGG.get());
    }
}
