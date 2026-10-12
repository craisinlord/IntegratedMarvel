package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = IntegratedMarvel.MODID, value = Dist.CLIENT)
public final class TimeFreezeClientState {
    private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(
            "infinity_gauntlet", "shader/time_stop.json");
    private static boolean active;

    private TimeFreezeClientState() {
    }

    public static void set(boolean nextActive) {
        Minecraft minecraft = Minecraft.getInstance();
        active = nextActive;
        if (minecraft.gameRenderer == null) return;
        if (nextActive) {
            if (minecraft.gameRenderer.currentEffect() == null) {
                minecraft.gameRenderer.loadEffect(EFFECT);
            }
        } else if (minecraft.gameRenderer.currentEffect() != null
                && EFFECT.toString().equals(minecraft.gameRenderer.currentEffect().getName())) {
            minecraft.gameRenderer.shutdownEffect();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (active) set(false);
    }
}
