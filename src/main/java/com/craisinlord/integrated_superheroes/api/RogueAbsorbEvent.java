package com.craisinlord.integrated_superheroes.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class RogueAbsorbEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final LivingEntity target;
    private RogueMemory memory;
    private double seconds;

    public RogueAbsorbEvent(ServerPlayer player, LivingEntity target, RogueMemory memory, double seconds) {
        this.player = player;
        this.target = target;
        this.memory = memory;
        this.seconds = seconds;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    public RogueMemory getMemory() {
        return this.memory;
    }

    public void setMemory(RogueMemory memory) {
        this.memory = memory;
    }

    public double getSeconds() {
        return this.seconds;
    }

    public void setSeconds(double seconds) {
        this.seconds = seconds;
    }
}
