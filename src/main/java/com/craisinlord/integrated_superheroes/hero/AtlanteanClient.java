package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class AtlanteanClient {
    private static boolean wasDown;
    private static boolean used;

    private AtlanteanClient() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            wasDown = false;
            return;
        }
        boolean down = minecraft.options.keyJump.isDown();
        boolean grounded = player.onGround() || player.isInWater() || player.onClimbable() || player.isPassenger()
                || player.getAbilities().flying;
        if (grounded) {
            used = false;
        } else if (down && !wasDown && !used && AtlanteanPower.isAtlantean(player)) {
            used = true;
            PacketDistributor.sendToServer(AtlanteanSurgePayload.INSTANCE);
        }
        wasDown = down;
    }
}
