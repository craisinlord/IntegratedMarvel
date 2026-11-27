package com.craisinlord.integrated_superheroes.voidrealm.variant;

import com.craisinlord.integrated_superheroes.hero.HeroPower;
import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class VariantDrops {
    private static final ResourceLocation SUIT_COMPONENT = ResourceLocation.fromNamespaceAndPath("integrated_superheroes", "suit");
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static WeakReference<RecipeManager> indexedFor = new WeakReference<>(null);
    private static Map<Item, List<Ingredient>> byItem = Map.of();
    private static Map<ResourceLocation, List<Ingredient>> bySuit = Map.of();

    private VariantDrops() {
    }

    static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !VoidVariants.isVariant(entity) || !VariantConfig.ready()) {
            return;
        }
        Player player = event.getSource().getEntity() instanceof Player p ? p : null;
        if (player == null && !event.isRecentlyHit()) {
            return;
        }
        VariantProfile profile = VariantProfile.of(entity);
        if (profile == null) {
            return;
        }
        VariantFamily family = VariantPool.get(level.getServer()).family(profile.kind(), profile.family());
        if (family == null || family.entity()) {
            return;
        }
        double chance = VariantConfig.DROP_CHANCE.get() + VariantConfig.DROP_CHANCE_PER_TIER.get() * profile.tier()
                + VariantConfig.LOOTING_BONUS.get() * (player == null ? 0 : looting(level, player));
        List<ItemStack> drops = new ArrayList<>();
        List<Ingredient> materials = new ArrayList<>();
        index(level);
        switch (profile.kind()) {
            case THEMATIC -> {
                Item suit = VariantPool.thematicItem(profile.look());
                if (suit != null) {
                    materials.addAll(byItem.getOrDefault(suit, List.of()));
                    materials.addAll(bySuit.getOrDefault(BuiltInRegistries.ITEM.getKey(suit), List.of()));
                    if (level.random.nextDouble() < chance) {
                        drops.add(new ItemStack(suit));
                    }
                }
            }
            case SERUM, VILTRUMITE -> {
                Item syringe = syringe(profile.family());
                if (syringe != null) {
                    materials.addAll(byItem.getOrDefault(syringe, List.of()));
                    if (level.random.nextDouble() < chance) {
                        drops.add(new ItemStack(syringe));
                    }
                }
            }
            case MARVEL -> {
                for (EquipmentSlot slot : ARMOR) {
                    ItemStack worn = entity.getItemBySlot(slot);
                    ResourceLocation key = worn.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(worn.getItem());
                    if (key != null && "marvel".equals(key.getNamespace()) && key.getPath().startsWith(profile.family() + "_")) {
                        materials.addAll(byItem.getOrDefault(worn.getItem(), List.of()));
                        if (level.random.nextDouble() < chance) {
                            drops.add(new ItemStack(worn.getItem()));
                        }
                    }
                }
            }
            default -> {
            }
        }
        double materialChance = VariantConfig.MATERIAL_CHANCE.get() + VariantConfig.MATERIAL_CHANCE_PER_TIER.get() * profile.tier();
        if (!materials.isEmpty() && level.random.nextDouble() < materialChance) {
            drops.addAll(rollMaterials(level.random, materials, 1 + level.random.nextInt(VariantConfig.MATERIAL_MAX.get())));
        }
        for (ItemStack stack : drops) {
            ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY(0.5D), entity.getZ(), stack);
            item.setDefaultPickUpDelay();
            event.getDrops().add(item);
        }
    }

    private static List<ItemStack> rollMaterials(RandomSource random, List<Ingredient> pool, int count) {
        List<Ingredient> modded = new ArrayList<>();
        for (Ingredient ingredient : pool) {
            for (ItemStack option : ingredient.getItems()) {
                if (usable(option) && !"minecraft".equals(BuiltInRegistries.ITEM.getKey(option.getItem()).getNamespace())) {
                    modded.add(ingredient);
                    break;
                }
            }
        }
        List<Ingredient> from = modded.isEmpty() ? pool : modded;
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            List<ItemStack> options = new ArrayList<>();
            for (ItemStack option : from.get(random.nextInt(from.size())).getItems()) {
                if (usable(option)) {
                    options.add(option);
                }
            }
            if (options.isEmpty()) {
                continue;
            }
            Item item = options.get(random.nextInt(options.size())).getItem();
            int amount = 1 + random.nextInt(2);
            boolean merged = false;
            for (ItemStack stack : out) {
                if (stack.is(item)) {
                    stack.setCount(Math.min(stack.getMaxStackSize(), stack.getCount() + amount));
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                out.add(new ItemStack(item, amount));
            }
        }
        return out;
    }

    private static boolean usable(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() > 1 && !stack.hasCraftingRemainingItem();
    }

    @SuppressWarnings("unchecked")
    private static synchronized void index(ServerLevel level) {
        RecipeManager manager = level.getRecipeManager();
        if (indexedFor.get() == manager) {
            return;
        }
        DataComponentType<ResourceLocation> suitType = (DataComponentType<ResourceLocation>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(SUIT_COMPONENT);
        Map<Item, List<Ingredient>> items = new HashMap<>();
        Map<ResourceLocation, List<Ingredient>> suits = new HashMap<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            ItemStack result;
            List<Ingredient> ingredients;
            try {
                result = holder.value().getResultItem(level.registryAccess());
                ingredients = holder.value().getIngredients();
            } catch (RuntimeException e) {
                continue;
            }
            if (result == null || result.isEmpty() || ingredients == null || ingredients.isEmpty()) {
                continue;
            }
            List<Ingredient> kept = new ArrayList<>();
            for (Ingredient ingredient : ingredients) {
                if (ingredient.isEmpty()) {
                    continue;
                }
                for (ItemStack option : ingredient.getItems()) {
                    if (usable(option)) {
                        kept.add(ingredient);
                        break;
                    }
                }
            }
            if (kept.isEmpty()) {
                continue;
            }
            ResourceLocation suit = suitType == null ? null : result.get(suitType);
            if (suit != null) {
                suits.computeIfAbsent(suit, key -> new ArrayList<>()).addAll(kept);
            } else {
                items.computeIfAbsent(result.getItem(), key -> new ArrayList<>()).addAll(kept);
            }
        }
        byItem = items;
        bySuit = suits;
        indexedFor = new WeakReference<>(manager);
    }

    static Item syringe(String powerId) {
        HeroPower power = HeroPowers.get(powerId);
        if (power != null) {
            return power.syringe().get();
        }
        ResourceLocation key = ResourceLocation.fromNamespaceAndPath("marvellegacy", "syringe_" + powerId);
        Item item = BuiltInRegistries.ITEM.containsKey(key) ? BuiltInRegistries.ITEM.get(key) : Items.AIR;
        return item == Items.AIR ? null : item;
    }

    private static int looting(ServerLevel level, Player player) {
        Holder<Enchantment> holder = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.LOOTING).orElse(null);
        return holder == null ? 0 : EnchantmentHelper.getEnchantmentLevel(holder, player);
    }
}
