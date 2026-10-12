package com.craisinlord.integrated_marvel.registry;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GauntletContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IntegratedMarvel.MODID);

    public static final DeferredItem<Item> INFINITY_GAUNTLET = ITEMS.register("infinity_gauntlet",
            () -> new InfinityGauntletItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> NANO_GAUNTLET = ITEMS.register("nano_gauntlet",
            () -> new NanoGauntletItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> POWER_STONE = stone("power_stone");
    public static final DeferredItem<Item> SPACE_STONE = stone("space_stone");
    public static final DeferredItem<Item> REALITY_STONE = stone("reality_stone");
    public static final DeferredItem<Item> SOUL_STONE = stone("soul_stone");
    public static final DeferredItem<Item> TIME_STONE = stone("time_stone");
    public static final DeferredItem<Item> MIND_STONE = stone("mind_stone");

    private GauntletContent() {
    }

    private static DeferredItem<Item> stone(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(16)));
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(NANO_GAUNTLET.get());
            event.accept(INFINITY_GAUNTLET.get());
            event.accept(POWER_STONE.get());
            event.accept(SPACE_STONE.get());
            event.accept(REALITY_STONE.get());
            event.accept(SOUL_STONE.get());
            event.accept(TIME_STONE.get());
            event.accept(MIND_STONE.get());
        }
    }
}
