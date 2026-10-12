package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.client.gui.Gui;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerHeartTypeEvent;
import net.tintankgames.marvel.client.MarvelClientEnumExtensions;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class SerumHeartClientEvents {
    private static final String WEAPON_X_LOGAN = "weapon_x_logan";

    private SerumHeartClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHeartType(PlayerHeartTypeEvent event) {
        if (event.getOriginalType() != Gui.HeartType.NORMAL) {
            return;
        }
        String power = HeroPowers.data(event.getEntity()).getPrimaryPower();
        if (DeadpoolPower.ID.equals(power)) {
            event.setType(MarvelClientEnumExtensions.DEADPOOL_HEART_TYPE.getValue());
        } else if (WEAPON_X_LOGAN.equals(power)) {
            event.setType(MarvelClientEnumExtensions.ADAMANTIUM_HEART_TYPE.getValue());
        }
    }
}
