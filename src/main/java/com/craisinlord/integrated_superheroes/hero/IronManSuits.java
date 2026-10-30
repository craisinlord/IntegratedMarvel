package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashMap;
import java.util.Map;

public final class IronManSuits {
    private static final Map<String, Stats> SUITS = new HashMap<>();
    private static final String[] SUFFIXES = {"_helmet", "_chestplate", "_leggings", "_boots"};

    public static ModConfigSpec.DoubleValue FLAMETHROWER_DAMAGE;
    public static ModConfigSpec.IntValue ENCHANTABILITY;

    private IronManSuits() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.comment("Iron Man / War Machine suit stats, one section per suit.",
                        "Armor values are per piece; armorToughness is added on every piece.",
                        "repulsorDamage and unibeamDamage also apply to sentry suits wearing that mark.",
                        "flightSpeed multiplies horizontal flying speed while the full suit is worn (Marvel's own Mk19 2x stacks on top).",
                        "reactorOutput is FE per tick the arc reactor adds to the whole suit while worn; reactorCap is the share",
                        "of the bar (0-1) it refills to. Above the cap only chargers, Veronica or lightning add power.")
                .push("ironMan");
        FLAMETHROWER_DAMAGE = builder.comment("Damage per flame from the Mk1 flamethrower (Marvel default: 2).")
                .defineInRange("flamethrowerDamage", 4.0D, 0.0D, 10000.0D);
        ENCHANTABILITY = builder.comment("Enchanting table enchantability of every Iron Man, War Machine and Iron Patriot piece.")
                .defineInRange("enchantability", 15, 1, 100);
        suit(builder, "iron_man_mark_1", 3, 8, 6, 3, 1.0, 0, 15, 0, 1.0, 20, 0.3);
        suit(builder, "iron_man_mark_2", 3, 8, 6, 3, 2.0, 10, 15, 0, 1.03, 40, 0.4);
        suit(builder, "iron_man_mark_3", 4, 8, 6, 3, 2.2, 11, 14, 0, 1.08, 50, 0.4);
        suit(builder, "iron_man_mark_5", 4, 9, 6, 3, 2.4, 11, 14, 0, 1.13, 65, 0.45);
        suit(builder, "iron_man_mark_6", 4, 9, 7, 3, 2.6, 12, 14, 0, 1.19, 80, 0.45);
        suit(builder, "iron_man_mark_7", 4, 9, 7, 4, 3.0, 13, 14, 40, 1.26, 120, 0.5);
        suit(builder, "iron_man_mark_11", 4, 10, 7, 4, 3.1, 14, 13, 42, 1.33, 150, 0.5);
        suit(builder, "iron_man_mark_15", 4, 10, 7, 4, 3.2, 15, 13, 46, 1.4, 180, 0.55);
        suit(builder, "iron_man_mark_17", 4, 10, 8, 4, 3.25, 17, 13, 102, 1.48, 210, 0.55);
        suit(builder, "iron_man_mark_19", 4, 10, 8, 4, 3.3, 19, 12, 57, 1.56, 240, 0.6);
        suit(builder, "iron_man_mark_20", 5, 10, 8, 4, 3.35, 20, 12, 65, 1.64, 270, 0.6);
        suit(builder, "iron_man_mark_21", 5, 10, 8, 4, 3.4, 22, 12, 73, 1.73, 300, 0.65);
        suit(builder, "iron_man_mark_22", 5, 10, 8, 5, 3.5, 24, 12, 82, 1.81, 400, 0.7);
        suit(builder, "iron_man_mark_23", 5, 11, 8, 5, 3.55, 27, 11, 93, 1.9, 450, 0.7);
        suit(builder, "iron_man_mark_24", 6, 12, 9, 5, 3.8, 29, 11, 156, 1.7, 500, 0.7);
        suit(builder, "iron_man_mark_25", 5, 11, 9, 5, 3.6, 31, 11, 115, 2.09, 550, 0.75);
        suit(builder, "iron_man_mark_28", 6, 12, 9, 5, 4.0, 34, 10, 128, 2.18, 620, 0.75);
        suit(builder, "iron_man_mark_30", 6, 12, 9, 5, 4.1, 37, 10, 141, 2.28, 700, 0.8);
        suit(builder, "iron_man_mark_33", 6, 12, 10, 5, 4.25, 40, 10, 154, 2.38, 800, 0.85);
        suit(builder, "iron_man_mark_35", 6, 13, 10, 5, 4.4, 43, 9, 169, 2.48, 900, 0.85);
        suit(builder, "iron_man_mark_37", 6, 13, 9, 6, 4.5, 46, 9, 184, 2.58, 1000, 0.9);
        suit(builder, "iron_man_mark_38", 6, 13, 10, 6, 4.6, 49, 9, 200, 2.3, 1100, 0.9);
        suit(builder, "iron_man_mark_39", 6, 13, 10, 6, 4.6, 53, 9, 216, 3.1, 1200, 0.95);
        suit(builder, "iron_man_mark_42", 6, 14, 10, 5, 4.8, 56, 8, 233, 2.89, 1500, 1.0);
        suit(builder, "iron_man_mark_43", 6, 14, 10, 6, 5.0, 60, 8, 250, 3.0, 1500, 1.0);
        suit(builder, "war_machine_mark_1", 4, 9, 7, 4, 3.0, 13, 14, 40, 1.2, 120, 0.5);
        suit(builder, "war_machine_mark_2", 5, 12, 9, 6, 3.6, 27, 11, 93, 1.8, 450, 0.7);
        suit(builder, "iron_patriot", 5, 12, 9, 6, 3.6, 27, 11, 93, 1.9, 450, 0.7);
        com.craisinlord.integrated_superheroes.suit.SuitAbilityConfig.define(builder);
        com.craisinlord.integrated_superheroes.suit.PymConfig.define(builder);
        IronManEnergy.define(builder);
        builder.pop();
    }

    private static void suit(ModConfigSpec.Builder builder, String id, double helmet, double chestplate, double leggings,
                             double boots, double toughness, double repulsor, int cooldown, double unibeam, double flightSpeed,
                             int reactorOutput, double reactorCap) {
        builder.push(id);
        SUITS.put(id, new Stats(
                builder.defineInRange("helmetArmor", helmet, 0.0D, 1000.0D),
                builder.defineInRange("chestplateArmor", chestplate, 0.0D, 1000.0D),
                builder.defineInRange("leggingsArmor", leggings, 0.0D, 1000.0D),
                builder.defineInRange("bootsArmor", boots, 0.0D, 1000.0D),
                builder.defineInRange("armorToughness", toughness, 0.0D, 1000.0D),
                builder.defineInRange("repulsorDamage", repulsor, 0.0D, 10000.0D),
                builder.defineInRange("repulsorCooldownTicks", cooldown, 1, 200),
                builder.defineInRange("unibeamDamage", unibeam, 0.0D, 10000.0D),
                builder.defineInRange("flightSpeed", flightSpeed, 0.1D, 20.0D),
                builder.defineInRange("reactorOutput", reactorOutput, 0, Integer.MAX_VALUE),
                builder.defineInRange("reactorCap", reactorCap, 0.0D, 1.0D)));
        builder.pop();
    }

    public static Stats forItem(Item item) {
        String id = suitId(item);
        return id == null ? null : SUITS.get(id);
    }

    public static Stats get(String suitId) {
        return SUITS.get(suitId);
    }

    public static String suitId(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (!"marvel".equals(key.getNamespace())) {
            return null;
        }
        String path = key.getPath();
        for (String suffix : SUFFIXES) {
            if (path.endsWith(suffix)) {
                String id = path.substring(0, path.length() - suffix.length());
                return SUITS.containsKey(id) ? id : null;
            }
        }
        return null;
    }

    public static String fullSuitId(LivingEntity living) {
        String id = null;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = living.getItemBySlot(slot);
            String piece = stack.isEmpty() ? null : suitId(stack.getItem());
            if (piece == null || (id != null && !id.equals(piece))) {
                return null;
            }
            id = piece;
        }
        return id;
    }

    public static Stats forWearer(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        ItemStack chest = living.getItemBySlot(EquipmentSlot.CHEST);
        return chest.isEmpty() ? null : forItem(chest.getItem());
    }

    public static boolean isLoaded() {
        return MarvelBalanceConfig.SPEC.isLoaded();
    }

    public record Stats(ModConfigSpec.DoubleValue helmet, ModConfigSpec.DoubleValue chestplate,
                        ModConfigSpec.DoubleValue leggings, ModConfigSpec.DoubleValue boots,
                        ModConfigSpec.DoubleValue toughness, ModConfigSpec.DoubleValue repulsorDamage,
                        ModConfigSpec.IntValue repulsorCooldownTicks, ModConfigSpec.DoubleValue unibeamDamage,
                        ModConfigSpec.DoubleValue flightSpeed, ModConfigSpec.IntValue reactorOutput,
                        ModConfigSpec.DoubleValue reactorCap) {
        public double armor(int index) {
            return switch (index) {
                case 0 -> helmet.get();
                case 1 -> chestplate.get();
                case 2 -> leggings.get();
                default -> boots.get();
            };
        }
    }
}
