package com.craisinlord.integrated_superheroes.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public class GauntletSnapEvent extends Event {
    private final ServerPlayer player;
    private final int victims;

    public GauntletSnapEvent(ServerPlayer player, int victims) {
        this.player = player;
        this.victims = victims;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public int getVictims() {
        return this.victims;
    }
}
