package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.registries.armors.armor.CharacterItem;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.skin.SkinData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ThematicSerumLooks {
    private static final Map<String, ItemStack> STACKS = new ConcurrentHashMap<>();
    private static boolean failed;

    private ThematicSerumLooks() {
    }

    public static ItemStack standIn(LivingEntity entity) {
        if (failed || !(entity instanceof Player player) || !player.level().isClientSide) {
            return null;
        }
        try {
            String costume = SkinData.costume(player);
            if (costume.isEmpty() || !(player == Minecraft.getInstance().player ? ThematicLookOptions.showOwn() : ThematicLookOptions.showOthers())) {
                return null;
            }
            ItemStack stack = STACKS.computeIfAbsent(costume, ThematicSerumLooks::create);
            return stack.isEmpty() ? null : stack;
        } catch (Throwable error) {
            failed = true;
            IntegratedSuperheroes.LOGGER.warn("Serum costumes are off for this session; this Thematic version may have changed how suits render", error);
            return null;
        }
    }

    public static boolean covers(Player player) {
        return standIn(player) != null;
    }

    private static ItemStack create(String costume) {
        ResourceLocation id = ResourceLocation.tryParse(costume);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item instanceof CharacterItem ? new ItemStack(item) : ItemStack.EMPTY;
    }
}
