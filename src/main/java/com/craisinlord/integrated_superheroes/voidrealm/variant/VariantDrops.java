package com.craisinlord.integrated_superheroes.voidrealm.variant;

import com.craisinlord.integrated_superheroes.hero.HeroPower;
import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;

final class VariantDrops {
    private VariantDrops() {
    }

    static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !VoidVariants.isVariant(entity) || !VariantConfig.ready()
                || !(event.getSource().getEntity() instanceof Player player)) {
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
                + VariantConfig.LOOTING_BONUS.get() * looting(level, player);
        List<ItemStack> drops = new ArrayList<>();
        switch (profile.kind()) {
            case THEMATIC -> {
                Item suit = VariantPool.thematicItem(profile.look());
                if (suit != null && level.random.nextDouble() < chance) {
                    drops.add(new ItemStack(suit));
                }
            }
            case SERUM, VILTRUMITE -> {
                Item syringe = syringe(profile.family());
                if (syringe != null && level.random.nextDouble() < chance) {
                    drops.add(new ItemStack(syringe));
                }
            }
            case MARVEL -> {
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    ItemStack worn = entity.getItemBySlot(slot);
                    ResourceLocation key = worn.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(worn.getItem());
                    if (key != null && "marvel".equals(key.getNamespace()) && key.getPath().startsWith(profile.family() + "_")
                            && level.random.nextDouble() < chance) {
                        drops.add(new ItemStack(worn.getItem()));
                    }
                }
            }
            default -> {
            }
        }
        for (ItemStack stack : drops) {
            ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY(0.5D), entity.getZ(), stack);
            item.setDefaultPickUpDelay();
            event.getDrops().add(item);
        }
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
