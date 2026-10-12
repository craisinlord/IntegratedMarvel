package com.craisinlord.integrated_superheroes.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public class HeroSensesEvent extends Event {
    private final ServerPlayer player;
    private final double radius;

    public HeroSensesEvent(ServerPlayer player, double radius) {
        this.player = player;
        this.radius = radius;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public double getRadius() {
        return this.radius;
    }
}
