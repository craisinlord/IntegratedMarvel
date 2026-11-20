package com.craisinlord.integrated_superheroes.voidworld;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public class VoidFallEvent extends Event {
    private final ServerPlayer player;
    private boolean handled;

    public VoidFallEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public boolean isHandled() {
        return this.handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }
}
