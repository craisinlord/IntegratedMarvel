package com.craisinlord.integrated_superheroes.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public class GauntletCompletedEvent extends Event {
    private final ServerPlayer player;
    private final ItemStack gauntlet;

    public GauntletCompletedEvent(ServerPlayer player, ItemStack gauntlet) {
        this.player = player;
        this.gauntlet = gauntlet;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public ItemStack getGauntlet() {
        return this.gauntlet;
    }
}
