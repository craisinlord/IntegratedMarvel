package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.platform.InputConstants;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.network.packet.PowerHandlerPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class HeroMouseKeysClient {
    private static final boolean[] DOWN = new boolean[HeroSlots.SLOTS];

    private HeroMouseKeysClient() {
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        PlayerData data = HeroPowers.data(player);
        InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
        KeyMapping[] keys = HeroSlotsClient.allKeys();
        for (int slot = 0; slot < keys.length && slot < DOWN.length; slot++) {
            KeyMapping key = keys[slot];
            if (key.getKey().getType() != InputConstants.Type.MOUSE) {
                continue;
            }
            if (event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null && key.isActiveAndMatches(input)
                    && !UnifiedSlots.suitOwned(slot)) {
                DOWN[slot] = true;
                PacketDistributor.sendToServer(new PowerHandlerPacket("press", data.getActivePower(slot).getName(), slot));
            } else if (event.getAction() == GLFW.GLFW_RELEASE && DOWN[slot] && key.getKey().equals(input)) {
                DOWN[slot] = false;
                PacketDistributor.sendToServer(new PowerHandlerPacket("release", data.getActivePower(slot).getName(), slot));
            }
        }
    }
}
