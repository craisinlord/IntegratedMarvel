package com.craisinlord.integrated_marvel.cleanup;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.craisinlord.integrated_marvel.registry.ModCreativeTab;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TieredItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UnifiedCreativeTab {
    private static final List<String> SOURCES = List.of(IntegratedMarvel.MODID, "marvel", "marvellegacy", "infinity_gauntlet");
    private static boolean harvesting;

    private UnifiedCreativeTab() {
    }

    private static boolean unified() {
        return CleanupConfig.on(CleanupConfig.UNIFIED_TAB);
    }

    private static boolean isSource(ItemStack stack) {
        return SOURCES.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    public static void fill(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        Set<ItemStack> collected = ItemStackLinkedSet.createTypeAndComponentsSet();
        for (Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
            if (entry.getKey().location().getNamespace().equals(IntegratedMarvel.MODID)) {
                collected.add(new ItemStack(entry.getValue()));
            }
        }
        if (unified()) {
            harvest(parameters, collected);
            for (Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
                if (SOURCES.contains(entry.getKey().location().getNamespace())) {
                    collected.add(new ItemStack(entry.getValue()));
                }
            }
        }
        CreativeOrder order = CreativeOrder.get();
        List<Entry> entries = new ArrayList<>();
        for (ItemStack stack : collected) {
            if (!stack.isEmpty() && !Cleanup.isHidden(stack.getItem())) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                entries.add(new Entry(stack, entries.size(), order.key(id, fallbackSection(stack, id))));
            }
        }
        entries.sort(Comparator.comparing((Entry entry) -> entry.key).thenComparingInt(entry -> entry.index));
        for (Entry entry : entries) {
            output.accept(entry.stack);
        }
    }

    private static void harvest(CreativeModeTab.ItemDisplayParameters parameters, Set<ItemStack> collected) {
        List<CreativeModeTab> sources = new ArrayList<>();
        for (Map.Entry<ResourceKey<CreativeModeTab>, CreativeModeTab> entry : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (SOURCES.contains(id.getNamespace()) && !entry.getKey().equals(ModCreativeTab.MAIN.getKey())
                    && entry.getValue().getType() == CreativeModeTab.Type.CATEGORY) {
                sources.add(entry.getValue());
            }
        }
        sources.sort(Comparator.comparingInt(tab -> SOURCES.indexOf(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).getNamespace())));
        for (CreativeModeTab tab : sources) {
            harvesting = true;
            try {
                tab.buildContents(parameters);
                for (ItemStack stack : tab.getDisplayItems()) {
                    if (isSource(stack)) {
                        collected.add(stack.copy());
                    }
                }
            } finally {
                harvesting = false;
            }
            tab.buildContents(parameters);
        }
    }

    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (harvesting || event.getTabKey().equals(ModCreativeTab.MAIN.getKey())) {
            return;
        }
        boolean unified = unified();
        List<ItemStack> drop = new ArrayList<>();
        for (ItemStack stack : event.getParentEntries()) {
            if (Cleanup.isHidden(stack.getItem()) || unified && isSource(stack)) {
                drop.add(stack);
            }
        }
        for (ItemStack stack : event.getSearchEntries()) {
            if ((Cleanup.isHidden(stack.getItem()) || unified && isSource(stack)) && !drop.contains(stack)) {
                drop.add(stack);
            }
        }
        for (ItemStack stack : drop) {
            event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    private static String fallbackSection(ItemStack stack, ResourceLocation id) {
        Item item = stack.getItem();
        String path = id.getPath();
        if (item instanceof SpawnEggItem) {
            return "spawn_eggs";
        }
        if (path.contains("syringe") || path.contains("serum")) {
            return "serums";
        }
        if (id.getNamespace().equals(IntegratedMarvel.MODID)) {
            return "gauntlet";
        }
        Equipable equipable = Equipable.get(stack);
        if (equipable != null && equipable.getEquipmentSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            return "suits";
        }
        if (item instanceof ShieldItem || item instanceof ProjectileWeaponItem || item instanceof TieredItem
                || stack.getAttributeModifiers().modifiers().stream().anyMatch(modifier -> modifier.attribute().is(Attributes.ATTACK_DAMAGE))) {
            return "weapons";
        }
        if (item instanceof BlockItem) {
            return "blocks";
        }
        return stack.getMaxStackSize() == 1 ? "gadgets" : "materials";
    }

    private record Entry(ItemStack stack, int index, CreativeOrder.Key key) {
    }
}
