package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public record AbilityRow(Component name, int slot, Component key, double cooldown, boolean active, Lock lock,
                         Component suffix, ItemStack icon, boolean suit, boolean emphasised) {
    public enum Lock { NONE, EXCLUSIVE, BLOCKED }

    public static AbilityRow simple(Component name, int slot, Component key, boolean suit) {
        return new AbilityRow(name, slot, key, 0.0D, false, Lock.NONE, Component.empty(), ItemStack.EMPTY, suit, false);
    }
}
