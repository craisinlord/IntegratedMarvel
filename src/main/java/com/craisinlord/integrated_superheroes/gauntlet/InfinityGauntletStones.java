package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Optional;

public final class InfinityGauntletStones {
    private static final List<ResourceLocation> GAUNTLET_IDS = List.of(
            ResourceLocation.parse("integrated_superheroes:infinity_gauntlet"),
            ResourceLocation.parse("integrated_superheroes:nano_gauntlet")
    );
    private static final String STONES_TAG = "integrated_superheroes_stones";
    private static final String ABILITIES_TAG = "integrated_superheroes_abilities";
    private static final int[] MODEL_BITS = {16, 2, 8, 4, 1, 32};

    private static final List<Stone> STONES = List.of(
            new Stone(ResourceLocation.parse("integrated_superheroes:power_stone"), "power", "Power"),
            new Stone(ResourceLocation.parse("integrated_superheroes:space_stone"), "space", "Space"),
            new Stone(ResourceLocation.parse("integrated_superheroes:reality_stone"), "reality", "Reality"),
            new Stone(ResourceLocation.parse("integrated_superheroes:soul_stone"), "soul", "Soul"),
            new Stone(ResourceLocation.parse("integrated_superheroes:time_stone"), "time", "Time"),
            new Stone(ResourceLocation.parse("integrated_superheroes:mind_stone"), "mind", "Mind")
    );

    private InfinityGauntletStones() {
    }

    public static boolean installHeldStone(Player player, ItemStack heldStack) {
        Optional<Stone> stone = stoneFor(heldStack);
        if (stone.isEmpty()) {
            return false;
        }

        ItemStack gauntlet = findGauntlet(player);
        if (gauntlet == null) {
            return false;
        }

        return installInto(player, heldStack, gauntlet, stone.get());
    }

    public static boolean installInto(Player player, ItemStack stoneStack, ItemStack gauntlet) {
        Optional<Stone> stone = stoneFor(stoneStack);
        return stone.isPresent() && isGauntlet(gauntlet)
                && installInto(player, stoneStack, gauntlet, stone.get());
    }

    public static boolean installFirstAvailableStone(Player player, ItemStack gauntlet) {
        if (!isGauntlet(gauntlet)) {
            return false;
        }
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.items) {
            Optional<Stone> stone = stoneFor(stack);
            if (stone.isPresent() && !hasStone(gauntlet, stone.get().key())) {
                return installInto(player, stack, gauntlet, stone.get());
            }
        }
        for (ItemStack stack : inventory.offhand) {
            Optional<Stone> stone = stoneFor(stack);
            if (stone.isPresent() && !hasStone(gauntlet, stone.get().key())) {
                return installInto(player, stack, gauntlet, stone.get());
            }
        }
        return false;
    }

    public static ItemStack equippedGauntlet(Player player) {
        if (isGauntlet(player.getMainHandItem())) {
            return player.getMainHandItem();
        }
        if (isGauntlet(player.getOffhandItem())) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    public static boolean hasStone(ItemStack gauntlet, String stoneKey) {
        CompoundTag data = gauntlet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return data.getCompound(STONES_TAG).getBoolean(stoneKey);
    }

    public static String selectedStone(ItemStack gauntlet) {
        CompoundTag data = gauntlet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return data.getString("integrated_superheroes_selected_stone");
    }

    public static int selectedAbility(ItemStack gauntlet, String stoneKey) {
        int count = abilityCount(stoneKey);
        if (count == 0) return 0;
        CompoundTag data = gauntlet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.clamp(data.getCompound(ABILITIES_TAG).getInt(stoneKey), 0, count - 1);
    }

    public static int selectedAbility(ItemStack gauntlet) {
        return selectedAbility(gauntlet, selectedStone(gauntlet));
    }

    public static void setSelectedAbility(ItemStack gauntlet, String stoneKey, int index) {
        int count = abilityCount(stoneKey);
        if (!isGauntlet(gauntlet) || count == 0) return;
        CompoundTag data = gauntlet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag abilities = data.getCompound(ABILITIES_TAG);
        abilities.putInt(stoneKey, Math.clamp(index, 0, count - 1));
        data.put(ABILITIES_TAG, abilities);
        gauntlet.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static int abilityCount(String stoneKey) {
        return switch (stoneKey) {
            case "power", "space", "snap" -> 2;
            case "reality", "soul", "time", "mind" -> 3;
            default -> 0;
        };
    }

    public static void setSelectedStone(ItemStack gauntlet, String key) {
        CompoundTag data = gauntlet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putString("integrated_superheroes_selected_stone", key);
        gauntlet.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    public static boolean hasAllStones(ItemStack gauntlet) {
        return STONES.stream().allMatch(stone -> hasStone(gauntlet, stone.key()));
    }

    public static int installedStoneModelIndex(ItemStack gauntlet) {
        CompoundTag data = gauntlet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag stones = data.getCompound(STONES_TAG);
        int modelIndex = 0;
        for (int index = 0; index < STONES.size(); index++) {
            if (stones.getBoolean(STONES.get(index).key())) {
                modelIndex |= MODEL_BITS[index];
            }
        }
        return modelIndex;
    }

    public static List<String> installedStoneKeys(ItemStack gauntlet) {
        return STONES.stream().map(Stone::key).filter(key -> hasStone(gauntlet, key)).toList();
    }

    public static boolean isKnownStone(String key) {
        return key.equals("snap") || STONES.stream().anyMatch(stone -> stone.key().equals(key));
    }

    public static Optional<String> stoneKey(ItemStack stack) {
        return stoneFor(stack).map(Stone::key);
    }

    private static boolean installInto(Player player, ItemStack stoneStack, ItemStack gauntlet, Stone stone) {
        if (gauntlet.isEmpty() || stoneStack.isEmpty()) {
            return false;
        }

        CompoundTag itemData = gauntlet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag stones = itemData.getCompound(STONES_TAG);
        String stoneKey = stone.key();
        if (stones.getBoolean(stoneKey)) {
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.gauntlet.stone_already_installed", stone.displayName()), true);
            return true;
        }

        stones.putBoolean(stoneKey, true);
        itemData.put(STONES_TAG, stones);
        if (!itemData.contains("integrated_superheroes_selected_stone")) {
            String installedKey = stone.key();
            itemData.putString("integrated_superheroes_selected_stone", installedKey);
        }
        gauntlet.set(DataComponents.CUSTOM_DATA, CustomData.of(itemData));
        if (hasAllStones(gauntlet)) {
            gauntlet.set(DataComponents.RARITY, Rarity.EPIC);
        }
        stoneStack.shrink(1);
        player.displayClientMessage(Component.translatable("message.integrated_superheroes.gauntlet.stone_installed", stone.displayName()), true);
        return true;
    }

    public static boolean isGauntlet(ItemStack stack) {
        return stack.getItemHolder().unwrapKey()
                .map(key -> GAUNTLET_IDS.contains(key.location()))
                .orElse(false);
    }

    public static boolean isFullGauntlet(ItemStack stack) {
        return stack.getItemHolder().unwrapKey()
                .map(key -> key.location().equals(ResourceLocation.parse("integrated_superheroes:infinity_gauntlet")))
                .orElse(false);
    }

    private static ItemStack findGauntlet(Player player) {
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.items) {
            if (isGauntlet(stack)) {
                return stack;
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (isGauntlet(stack)) {
                return stack;
            }
        }
        return null;
    }

    private static Optional<Stone> stoneFor(ItemStack stack) {
        return stack.getItemHolder().unwrapKey()
                .map(key -> key.location())
                .flatMap(id -> STONES.stream().filter(stone -> stone.itemId().equals(id)).findFirst());
    }

    private record Stone(ResourceLocation itemId, String key, String displayName) {
    }
}
