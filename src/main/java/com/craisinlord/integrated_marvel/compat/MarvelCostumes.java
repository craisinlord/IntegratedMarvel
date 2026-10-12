package com.craisinlord.integrated_marvel.compat;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = IntegratedMarvel.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class MarvelCostumes {
    private static final List<String> FULL_SETS = List.of(
            "marvellegacy:spiderman_classic", "marvellegacy:spiderman_original", "marvellegacy:spiderman_bagman",
            "marvellegacy:spiderman_tasmii", "marvellegacy:wolverine_classic", "marvellegacy:wolverine_original",
            "marvel:spider_man", "marvel:spider_man_mcu", "marvel:miles_morales_suit", "marvel:spider_gwen",
            "marvel:spider_man_insomniac", "marvel:wolverine", "marvel:wolverine_brown", "marvel:wolverine_x_force",
            "marvel:deadpool", "marvel:deadpool_x_force", "marvel:cyclops", "marvel:cyclops_astonishing",
            "marvel:captain_america", "marvel:captain_america_stealth", "marvel:captain_carter",
            "marvel:red_guardian", "marvel:us_agent", "marvel:captain_marvel", "marvel:captain_marvel_838",
            "marvel:black_panther"
    );
    private static final List<String> HELMETLESS_SETS = List.of(
            "marvel:quicksilver", "marvel:quicksilver_mcu", "marvel:winter_soldier"
    );
    private static final List<String> EXTRA_PIECES = List.of(
            "marvel:wolverine_sleeveless_chestplate", "marvel:wolverine_brown_sleeveless_chestplate",
            "marvel:wolverine_x_force_sleeveless_chestplate"
    );
    private static final List<ResourceLocation> STRIPPED_COMPONENTS = List.of(
            ResourceLocation.fromNamespaceAndPath("marvel", "suit_effects"),
            ResourceLocation.fromNamespaceAndPath("marvel", "power_items")
    );
    private static final Set<ResourceLocation> COSTUMES = buildCostumes();

    private MarvelCostumes() {
    }

    public static boolean isCostume(Item item) {
        return COSTUMES.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    private static Set<ResourceLocation> buildCostumes() {
        Set<ResourceLocation> costumes = new HashSet<>();
        FULL_SETS.forEach(set -> List.of("helmet", "chestplate", "leggings", "boots")
                .forEach(slot -> costumes.add(ResourceLocation.parse(set + "_" + slot))));
        HELMETLESS_SETS.forEach(set -> List.of("chestplate", "leggings", "boots")
                .forEach(slot -> costumes.add(ResourceLocation.parse(set + "_" + slot))));
        EXTRA_PIECES.forEach(piece -> costumes.add(ResourceLocation.parse(piece)));
        return Set.copyOf(costumes);
    }

    @SubscribeEvent
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        List<DataComponentType<?>> stripped = STRIPPED_COMPONENTS.stream()
                .<DataComponentType<?>>map(BuiltInRegistries.DATA_COMPONENT_TYPE::get)
                .filter(type -> type != null)
                .toList();
        for (ResourceLocation id : COSTUMES) {
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> event.modify(item, builder -> {
                stripped.forEach(builder::remove);
                builder.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
            }));
        }
    }
}
