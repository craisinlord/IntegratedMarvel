package com.craisinlord.integrated_superheroes.compat;

import net.minecraft.world.entity.player.Player;
import top.ribs.scguns.Config;
import top.ribs.scguns.item.GunItem;

public final class ScorchedGunsHud {
    private ScorchedGunsHud() {
    }

    public static boolean showsAmmo(Player player) {
        return ModCompat.SCORCHED_GUNS && Hud.showsAmmo(player);
    }

    private static final class Hud {
        static boolean showsAmmo(Player player) {
            return Config.CLIENT.display.displayGunInfo.get() && player.getMainHandItem().getItem() instanceof GunItem;
        }
    }
}
