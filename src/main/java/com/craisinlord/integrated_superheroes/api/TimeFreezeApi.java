package com.craisinlord.integrated_superheroes.api;

import com.craisinlord.integrated_superheroes.gauntlet.TimeFreezeManager;
import net.minecraft.server.level.ServerPlayer;

public final class TimeFreezeApi {
    private TimeFreezeApi() {
    }

    public static boolean freeze(ServerPlayer owner, int ticks) {
        return TimeFreezeManager.startTimed(owner, ticks);
    }

    public static boolean isFreezing(ServerPlayer owner) {
        return !TimeFreezeManager.owners(owner.serverLevel().dimension()).isEmpty()
                && TimeFreezeManager.owners(owner.serverLevel().dimension()).contains(owner.getUUID());
    }
}
