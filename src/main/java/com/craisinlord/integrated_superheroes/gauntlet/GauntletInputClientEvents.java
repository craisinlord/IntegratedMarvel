package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.gauntlet.InfinityGauntletStones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class GauntletInputClientEvents {
    private static MindControlInputPayload lastMindInput;

    private GauntletInputClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            lastMindInput = null;
            return;
        }
        if (minecraft.screen == null && GauntletKeyMappings.PREVIOUS_STONE.consumeClick()) {
            PacketDistributor.sendToServer(new GauntletInputPayload(GauntletAbilityController.PREVIOUS_STONE, 0.0D));
        }
        if (minecraft.screen == null && GauntletKeyMappings.NEXT_STONE.consumeClick()) {
            PacketDistributor.sendToServer(new GauntletInputPayload(GauntletAbilityController.NEXT_STONE, 0.0D));
        }
        if (minecraft.screen == null && GauntletKeyMappings.PREVIOUS_ABILITY.consumeClick()) {
            PacketDistributor.sendToServer(new GauntletInputPayload(GauntletAbilityController.PREVIOUS_ABILITY, 0.0D));
        }
        if (minecraft.screen == null && GauntletKeyMappings.NEXT_ABILITY.consumeClick()) {
            PacketDistributor.sendToServer(new GauntletInputPayload(GauntletAbilityController.NEXT_ABILITY, 0.0D));
        }
        var gauntlet = InfinityGauntletStones.equippedGauntlet(minecraft.player);
        if (minecraft.screen == null && GauntletKeyMappings.SPACE_DESTINATION.consumeClick()
                && InfinityGauntletStones.hasStone(gauntlet, "space")
                && InfinityGauntletStones.selectedStone(gauntlet).equals("space")) {
            minecraft.setScreen(new SpaceDestinationScreen());
        }
        if (MindControlClientState.state().controlled()) {
            minecraft.player.setYRot(MindControlClientState.state().yaw());
            minecraft.player.setXRot(MindControlClientState.state().pitch());
            minecraft.player.setSprinting(MindControlClientState.state().sprint());
        }
        if ((MindControlClientState.controlling() || MindLinkClient.active())
                && !gauntlet.isEmpty() && InfinityGauntletStones.hasStone(gauntlet, "mind")) {
            MindControlInputPayload input = new MindControlInputPayload(
                    minecraft.screen == null && minecraft.options.keyUp.isDown(),
                    minecraft.screen == null && minecraft.options.keyDown.isDown(),
                    minecraft.screen == null && minecraft.options.keyLeft.isDown(),
                    minecraft.screen == null && minecraft.options.keyRight.isDown(),
                    minecraft.screen == null && minecraft.options.keyJump.isDown(),
                    minecraft.screen == null && minecraft.options.keySprint.isDown(),
                    minecraft.screen == null && minecraft.options.keyAttack.isDown(),
                    minecraft.player.getYRot(), minecraft.player.getXRot());
            if (!input.equals(lastMindInput)) {
                PacketDistributor.sendToServer(input);
                lastMindInput = input;
            }
        } else {
            lastMindInput = null;
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !Screen.hasShiftDown()) return;
        boolean aimingAtLiving = minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity;
        if (!GauntletAbilityController.canScroll(minecraft.player, aimingAtLiving)) return;
        double delta = Math.clamp(event.getScrollDeltaY(), -1.0D, 1.0D);
        if (delta == 0.0D) return;
        PacketDistributor.sendToServer(new GauntletInputPayload(GauntletAbilityController.ABILITY_SCROLL, delta));
        event.setCanceled(true);
    }
}
