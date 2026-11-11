package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reality Stone block costs. Without every stone in the Gauntlet, pasting or permanently
 * transmuting into a copied block uses up the matching items from the player's inventory.
 */
public final class RealityCost {
    private RealityCost() {
    }

    public static boolean free(GauntletCast cast) {
        return cast.infinity() || cast.player().getAbilities().instabuild;
    }

    /** The item a placed state costs, or {@link Items#AIR} when it has no item form and can't be placed for a price. */
    public static Item item(BlockState state) {
        return state.getBlock().asItem();
    }

    /** How many of {@link #item} one placed state costs. Upper door halves and bed heads ride along with their other half. */
    public static int count(BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) return 0;
        if (state.hasProperty(BlockStateProperties.BED_PART) && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) return 0;
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE) && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) return 2;
        return 1;
    }

    public static boolean placeable(BlockState state) {
        return item(state) != Items.AIR;
    }

    public static Map<Item, Integer> bill(Collection<BlockState> states) {
        Map<Item, Integer> bill = new LinkedHashMap<>();
        for (BlockState state : states) {
            Item item = item(state);
            int count = count(state);
            if (item == Items.AIR || count == 0) continue;
            bill.merge(item, count, Integer::sum);
        }
        return bill;
    }

    /** Plain stacks only, so a filled shulker box or a named, enchanted or damaged item is never eaten. */
    private static boolean plain(ItemStack stack, Item item) {
        return stack.is(item) && stack.getComponentsPatch().isEmpty();
    }

    public static int held(ServerPlayer player, Item item) {
        Inventory inventory = player.getInventory();
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (plain(stack, item)) total += stack.getCount();
        }
        return total;
    }

    /** Items still missing for the bill, empty when the player can pay. */
    public static Map<Item, Integer> missing(ServerPlayer player, Map<Item, Integer> bill) {
        Map<Item, Integer> missing = new LinkedHashMap<>();
        bill.forEach((item, needed) -> {
            int gap = needed - held(player, item);
            if (gap > 0) missing.put(item, gap);
        });
        return missing;
    }

    public static void take(ServerPlayer player, Map<Item, Integer> bill) {
        Inventory inventory = player.getInventory();
        bill.forEach((item, needed) -> {
            int left = needed;
            for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!plain(stack, item)) continue;
                int used = Math.min(left, stack.getCount());
                stack.shrink(used);
                left -= used;
            }
        });
        inventory.setChanged();
    }

    public static void refund(ServerPlayer player, Map<Item, Integer> bill) {
        bill.forEach((item, amount) -> {
            int left = amount;
            int max = item.getDefaultMaxStackSize();
            while (left > 0) {
                int count = Math.min(left, max);
                player.getInventory().placeItemBackInInventory(new ItemStack(item, count));
                left -= count;
            }
        });
    }

    /** "12 Oak Planks, 3 Glass" for the first few missing items. */
    public static Component describe(Map<Item, Integer> missing) {
        MutableComponent text = Component.empty();
        int shown = 0;
        for (Map.Entry<Item, Integer> entry : missing.entrySet()) {
            if (shown == 3) {
                text.append(Component.translatable("message.integrated_superheroes.gauntlet.reality_missing_more", missing.size() - 3));
                break;
            }
            if (shown > 0) text.append(", ");
            text.append(entry.getValue() + " ").append(entry.getKey().getDescription());
            shown++;
        }
        return text;
    }
}
