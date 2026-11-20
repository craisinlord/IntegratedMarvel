package com.craisinlord.integrated_superheroes.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class UltronSummonEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final BlockPos cradle;

    public UltronSummonEvent(ServerPlayer player, BlockPos cradle) {
        this.player = player;
        this.cradle = cradle;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public BlockPos getCradle() {
        return this.cradle;
    }
}
