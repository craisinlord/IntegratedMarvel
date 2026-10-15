package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record GauntletCast(ServerPlayer player, ItemStack gauntlet, GauntletAbility ability, boolean infinity,
                           boolean crouching) {
    public ServerLevel level() {
        return player.serverLevel();
    }

    public double power() {
        return GauntletConfig.powerMultiplier() * (infinity ? 1.5D : 1.0D);
    }

    public void cooldown() {
        GauntletAbilityController.addAbilityCooldown(player, ability.stone(), ability.index(), ability.cooldownTicks(infinity));
    }

    public void cooldown(int ticks) {
        GauntletAbilityController.addAbilityCooldown(player, ability.stone(), ability.index(), infinity ? ticks / 2 : ticks);
    }

    public void refundCooldown() {
        GauntletAbilityController.clearAbilityCooldown(player, ability.stone(), ability.index());
    }

    public boolean onCooldown() {
        return GauntletAbilityController.isAbilityOnCooldown(player, ability.stone(), ability.index());
    }

    public void message(String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.integrated_superheroes.gauntlet." + key, args), true);
    }
}
