package com.craisinlord.integrated_superheroes.compat;

import dev.baranhan.viltrumitecore.ViltrumiteCore;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ViltrumiteAnimationsClient {
    private static final int TICKS = 20;
    private static final List<PendingSound> PENDING = new ArrayList<>();
    private static boolean listening;

    private ViltrumiteAnimationsClient() {
    }

    public static void apply(int entityId, int move, int variant) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.level.getEntity(entityId) instanceof Player player) || !player.isAlive()) {
            return;
        }
        if (!(player instanceof ViltrumiteCorePlayer core) || core.isViltrumite() || core.isBlocking() || core.isBarraging() || core.isChargingPunch()) {
            return;
        }
        if (player instanceof ViltrumiteFlightPlayer flight && flight.getFlightState() != FlightState.NONE) {
            return;
        }
        core.setThunderclapTicks(0);
        core.setPunchTicks(0);
        core.setChopTicks(0);
        boolean left = (variant & 1) != 0;
        int soundDelay;
        switch (move) {
            case ViltrumiteAnimations.THUNDERCLAP -> {
                core.setThunderclapTicks(TICKS);
                soundDelay = 9;
            }
            case ViltrumiteAnimations.PUNCH -> {
                core.setLeftArmPunch(left);
                core.setPunchTicks(TICKS);
                core.setPunchStrength(0.0F);
                soundDelay = 5;
            }
            case ViltrumiteAnimations.CHOP -> {
                core.setLeftChop(left);
                core.setChopType((variant >> 1) & 1);
                core.setChopTicks(TICKS);
                soundDelay = 5;
            }
            default -> {
                return;
            }
        }
        if (player != minecraft.player) {
            if (!listening) {
                listening = true;
                NeoForge.EVENT_BUS.addListener(ViltrumiteAnimationsClient::onClientTick);
            }
            PENDING.removeIf(pending -> pending.entityId() == entityId);
            PENDING.add(new PendingSound(entityId, move, minecraft.level.getGameTime() + soundDelay + 1));
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            PENDING.clear();
            return;
        }
        long now = level.getGameTime();
        Iterator<PendingSound> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            PendingSound pending = iterator.next();
            if (now < pending.at() && pending.at() - now <= TICKS) {
                continue;
            }
            iterator.remove();
            if (now - pending.at() <= 2 && level.getEntity(pending.entityId()) instanceof Player player && player.isAlive()) {
                playMoveSounds(level, player, pending.move());
            }
        }
    }

    private static void playMoveSounds(ClientLevel level, Player player, int move) {
        switch (move) {
            case ViltrumiteAnimations.THUNDERCLAP -> {
                play(level, player, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.5F);
                play(level, player, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 1.5F);
            }
            case ViltrumiteAnimations.PUNCH -> {
                play(level, player, ViltrumiteCore.PUNCH_IMPACT_EVENT.get(), 1.5F, 1.0F);
                play(level, player, SoundEvents.GENERIC_EXPLODE.value(), 0.5F, 1.5F);
                play(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
            }
            case ViltrumiteAnimations.CHOP -> play(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.5F);
            default -> {
            }
        }
    }

    private static void play(ClientLevel level, Player player, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    private record PendingSound(int entityId, int move, long at) {
    }
}
