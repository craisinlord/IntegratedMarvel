package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

final class SurfaceClingClient {
    private SurfaceClingClient() {
    }

    static boolean jumping(Player player) {
        return player instanceof LocalPlayer local && local.input != null && local.input.jumping;
    }
}
