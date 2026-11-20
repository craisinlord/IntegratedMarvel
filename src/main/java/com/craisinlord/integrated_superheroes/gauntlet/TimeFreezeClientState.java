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

    public static void set(List<UUID> owners, double radius) {
        TimeFreezeState.setClientOwners(owners, radius);
        refresh();
    }

    private static void refresh() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        java.util.Set<UUID> owners = TimeFreezeState.clientOwners();
        boolean inField = player != null && !owners.isEmpty()
                && (owners.contains(player.getUUID()) || TimeFreezeState.inField(player, owners));
        TimeFreezeState.setClientInField(inField);
        if (minecraft.gameRenderer == null) return;
        if (inField) {
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
        if (!TimeFreezeState.clientOwners().isEmpty()) refresh();
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
        if (!TimeFreezeState.clientOwners().isEmpty()) set(List.of(), 0.0D);
    }
}
