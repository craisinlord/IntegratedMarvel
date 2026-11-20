package com.craisinlord.integrated_superheroes.villain;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class VillainItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IntegratedSuperheroes.MODID);

    public static final DeferredItem<Item> KRYPTONIAN_BLOOD = ITEMS.registerSimpleItem("kryptonian_blood",
            new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
    public static final DeferredItem<Item> MARTIAN_BLOOD = ITEMS.registerSimpleItem("martian_blood",
            new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
    public static final DeferredItem<Item> ALIEN_BLOOD = ITEMS.registerSimpleItem("alien_blood",
            new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> BLACK_ADAM_SCARAB = ITEMS.registerSimpleItem("black_adam_scarab",
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    private VillainItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
