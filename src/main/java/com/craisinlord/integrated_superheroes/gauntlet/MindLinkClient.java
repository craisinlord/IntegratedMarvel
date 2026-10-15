package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class MindLinkClient {
    private static int linkedId = -1;

    private MindLinkClient() {
    }

    public static void set(MindLinkPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (payload.active()) {
            linkedId = payload.entityId();
        } else {
            release(minecraft);
        }
    }

    public static boolean active() {
        return linkedId >= 0;
    }

    private static void release(Minecraft minecraft) {
        linkedId = -1;
        if (minecraft.player != null) minecraft.setCameraEntity(minecraft.player);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active() || minecraft.level == null || minecraft.player == null) return;
        Entity target = minecraft.level.getEntity(linkedId);
        if (target == null || !target.isAlive()) {
            if (minecraft.getCameraEntity() != minecraft.player) minecraft.setCameraEntity(minecraft.player);
            return;
        }
        if (minecraft.getCameraEntity() != target) minecraft.setCameraEntity(target);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!active()) return;
        var input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active() || minecraft.player == null || minecraft.getCameraEntity() == minecraft.player) return;
        float partial = (float) event.getPartialTick();
        event.setYaw(minecraft.player.getViewYRot(partial));
        event.setPitch(minecraft.player.getViewXRot(partial));
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!active()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.isUseItem()) {
            PacketDistributor.sendToServer(new GauntletInputPayload(GauntletAbilityController.ACTIVATE, 0.0D));
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        release(Minecraft.getInstance());
    }
}
