package com.craisinlord.integrated_marvel.hero;

import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.init.AttachmentsRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class BinaryFlight {
    private BinaryFlight() {
    }

    public static boolean isFlying(LivingEntity living) {
        if (!(living instanceof Player player) || !player.hasData(AttachmentsRegistry.PLAYER_DATA.get())) {
            return false;
        }
        PlayerData data = player.getData(AttachmentsRegistry.PLAYER_DATA.get());
        return HeroPowers.get(data.getPrimaryPower()) != null && data.hasState(BinaryPower.FLYING_STATE);
    }
}
