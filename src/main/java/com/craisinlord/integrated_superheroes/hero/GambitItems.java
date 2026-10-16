package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class GambitItems {
    public static final TagKey<Item> UNTHROWABLE = tag("unthrowable");
    public static final TagKey<Item> CARDS = tag("cards");
    public static final TagKey<Item> HEAVY = tag("heavy");
    public static final TagKey<Item> EXPLOSIVE = tag("explosive");
    public static final TagKey<Item> RETURNS = tag("returns");
    public static final TagKey<Item> INCENDIARY = tag("incendiary");
    public static final TagKey<Item> FROST = tag("frost");
    public static final TagKey<Item> SHOCK = tag("shock");
    public static final TagKey<Item> TELEPORT = tag("teleport");
    public static final TagKey<Item> POTIONS = tag("potions");
    public static final TagKey<Item> FIREWORKS = tag("fireworks");
    public static final TagKey<Item> STAFFS = tag("staffs");

    public static final int CARD = 1;
    public static final int HEAVY_KIND = 1 << 1;
    public static final int EXPLOSIVE_KIND = 1 << 2;
    public static final int RETURNING = 1 << 3;
    public static final int FIRE = 1 << 4;
    public static final int ICE = 1 << 5;
    public static final int LIGHTNING = 1 << 6;
    public static final int WARP = 1 << 7;
    public static final int POTION = 1 << 8;
    public static final int FIREWORK = 1 << 9;
    public static final int BLOCK = 1 << 10;
    public static final int PHANTOM = 1 << 11;

    private GambitItems() {
    }

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "gambit/" + name));
    }

    public record Profile(int kinds, float damage, float radius, float speed) {
        public boolean has(int kind) {
            return (kinds & kind) != 0;
        }
    }

    public static boolean throwable(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(UNTHROWABLE) && !(stack.getItem() instanceof HeroSyringeItem);
    }

    public static boolean isStaff(ItemStack stack) {
        return !stack.isEmpty() && stack.is(STAFFS);
    }

    public static int kinds(ItemStack stack) {
        if (stack.isEmpty()) {
            return CARD | PHANTOM;
        }
        int kinds = 0;
        if (stack.is(CARDS)) {
            kinds |= CARD;
        }
        if (stack.is(HEAVY)) {
            kinds |= HEAVY_KIND;
        }
        if (stack.is(EXPLOSIVE)) {
            kinds |= EXPLOSIVE_KIND;
        }
        if (stack.is(RETURNS)) {
            kinds |= RETURNING;
        }
        if (stack.is(INCENDIARY)) {
            kinds |= FIRE;
        }
        if (stack.is(FROST)) {
            kinds |= ICE;
        }
        if (stack.is(SHOCK)) {
            kinds |= LIGHTNING;
        }
        if (stack.is(TELEPORT)) {
            kinds |= WARP;
        }
        if (stack.is(POTIONS) || stack.has(DataComponents.POTION_CONTENTS)) {
            kinds |= POTION;
        }
        if (stack.is(FIREWORKS) || stack.has(DataComponents.FIREWORKS) || stack.has(DataComponents.FIREWORK_EXPLOSION)) {
            kinds |= FIREWORK;
        }
        if (stack.getItem() instanceof BlockItem) {
            kinds |= BLOCK;
        }
        return kinds;
    }

    public static Profile profile(ItemStack stack, float baseDamage, float baseRadius) {
        int kinds = kinds(stack);
        float damage = baseDamage;
        float radius = baseRadius;
        float speed = 1.0F;
        if ((kinds & PHANTOM) != 0) {
            float phantom = MarvelBalanceConfig.PHANTOM_CARD_MULTIPLIER.get().floatValue();
            damage *= phantom;
            radius *= Math.max(0.5F, phantom);
        }
        if ((kinds & CARD) != 0) {
            radius *= MarvelBalanceConfig.CARD_RADIUS_MULTIPLIER.get().floatValue();
            speed *= 1.25F;
        }
        if ((kinds & HEAVY_KIND) != 0) {
            float heavy = MarvelBalanceConfig.HEAVY_MULTIPLIER.get().floatValue();
            damage *= heavy;
            radius *= 1.0F + (heavy - 1.0F) * 0.5F;
            speed *= 0.8F;
        }
        if ((kinds & EXPLOSIVE_KIND) != 0) {
            float explosive = MarvelBalanceConfig.EXPLOSIVE_MULTIPLIER.get().floatValue();
            damage *= explosive;
            radius *= explosive;
        }
        return new Profile(kinds, damage, radius, speed);
    }

    public static float gravity(int kinds) {
        if ((kinds & CARD) != 0) {
            return 0.004F;
        }
        if ((kinds & HEAVY_KIND) != 0) {
            return 0.06F;
        }
        if ((kinds & BLOCK) != 0) {
            return 0.045F;
        }
        return 0.03F;
    }
}
