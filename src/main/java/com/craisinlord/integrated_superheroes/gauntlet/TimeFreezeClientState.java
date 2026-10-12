package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class TimeFreezeClientState {
    private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(
            "infinity_gauntlet", "shader/time_stop.json");

    private TimeFreezeClientState() {
    }

    public static void set(List<UUID> owners) {
        TimeFreezeState.setClientOwners(owners);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null) return;
        if (!owners.isEmpty()) {
            if (minecraft.gameRenderer.currentEffect() == null) {
                minecraft.gameRenderer.loadEffect(EFFECT);
            }
        } else if (minecraft.gameRenderer.currentEffect() != null
                && EFFECT.toString().equals(minecraft.gameRenderer.currentEffect().getName())) {
            minecraft.gameRenderer.shutdownEffect();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !TimeFreezeState.isFrozenPlayer(player)) return;
        player.setOldPosAndRot();
        player.walkDistO = player.walkDist;
        player.oBob = player.bob;
        player.xBobO = player.xBob;
        player.yBobO = player.yBob;
        player.yBodyRotO = player.yBodyRot;
        player.yHeadRotO = player.yHeadRot;
        player.oAttackAnim = player.attackAnim;
    }

    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && TimeFreezeState.isFrozenPlayer(player)) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (TimeFreezeState.clientActive()) set(List.of());
    }
}
