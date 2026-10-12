package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.init.AttachmentsRegistry;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class SpeedForceClientEvents {
    private SpeedForceClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !Screen.hasShiftDown() || event.getScrollDeltaY() == 0.0D) {
            return;
        }
        PlayerData data = minecraft.player.getData(AttachmentsRegistry.PLAYER_DATA.get());
        if (!QuicksilverPower.ID.equals(data.getPrimaryPower())) {
            return;
        }
        for (Power power : data.getActivePowers()) {
            if (power != null && QuicksilverPower.SPEED_FORCE.equals(power.getName()) && power.isActive()) {
                PacketDistributor.sendToServer(new SpeedForcePayload(Math.signum(event.getScrollDeltaY())));
                event.setCanceled(true);
                return;
            }
        }
    }
}
