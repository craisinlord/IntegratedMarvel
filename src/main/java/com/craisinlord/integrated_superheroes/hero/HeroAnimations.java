package com.craisinlord.integrated_superheroes.hero;

import com.stxllarplanes.marvellegacy.client.AnimationLayerType;
import com.stxllarplanes.marvellegacy.network.packet.ClientRequestAnimationPacket;
import com.stxllarplanes.marvellegacy.network.packet.PlayAnimationPacket;
import com.stxllarplanes.marvellegacy.network.packet.StopAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HeroAnimations {
    private static final String[] SPRINT_FLIPS = {"flip_both_2", "flip_low_2"};
    private static final Map<String, Integer> HOLD_TICKS = Map.ofEntries(
            Map.entry("web_shoot_r", 12),
            Map.entry("web_shoot_l", 12),
            Map.entry("web_shoot_both", 14),
            Map.entry("uppercut", 14),
            Map.entry("kick", 14),
            Map.entry("slide", 0),
            Map.entry("wolverine_pounce", 0),
            Map.entry("bettercombat:one_handed_slam", 20),
            Map.entry("bettercombat:two_handed_slam", 20),
            Map.entry("celestisynth:cs_breezebreaker_jump", 40),
            Map.entry("celestisynth:cs_crescentia_throw", 27),
            Map.entry("celestisynth:cs_frostbound_cryogenesis", 20),
            Map.entry("celestisynth:cs_keres_smash", 45),
            Map.entry("celestisynth:cs_rainfall_aim_right", 20),
            Map.entry("celestisynth:cs_solaris_spin", 60)
    );
    private static final Map<UUID, Long> STOP_AT = new HashMap<>();

    private HeroAnimations() {
    }

    public static void playLeapFlip(Player player) {
        String flip = player.isSprinting() ? SPRINT_FLIPS[player.getRandom().nextInt(SPRINT_FLIPS.length)] : "flip_low_1";
        requestFromClient(player, flip);
    }

    public static void requestFromClient(Player player, String animation) {
        if (player.level().isClientSide) {
            PacketDistributor.sendToServer(new ClientRequestAnimationPacket(player.getId(), id(animation), AnimationLayerType.BODY));
        }
    }

    public static void play(ServerPlayer player, String animation) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new PlayAnimationPacket(player.getId(), id(animation), AnimationLayerType.BODY));
        int hold = HOLD_TICKS.getOrDefault(animation, 20);
        if (hold > 0) {
            STOP_AT.put(player.getUUID(), player.level().getGameTime() + hold);
        } else {
            STOP_AT.remove(player.getUUID());
        }
    }

    public static void stop(ServerPlayer player) {
        STOP_AT.remove(player.getUUID());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new StopAnimationPacket(player.getId(), AnimationLayerType.BODY));
    }

    public static void tick(ServerPlayer player) {
        Long stopAt = STOP_AT.get(player.getUUID());
        if (stopAt != null && player.level().getGameTime() >= stopAt) {
            stop(player);
        }
    }

    private static ResourceLocation id(String animation) {
        if (animation.indexOf(':') >= 0) {
            return ResourceLocation.parse(animation);
        }
        return ResourceLocation.fromNamespaceAndPath("marvellegacy", animation);
    }
}
