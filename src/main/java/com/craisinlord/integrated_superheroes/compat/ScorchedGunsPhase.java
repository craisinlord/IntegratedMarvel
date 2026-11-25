package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.hero.KittyPower;
import net.neoforged.neoforge.common.NeoForge;
import top.ribs.scguns.event.GunFireEvent;

public final class ScorchedGunsPhase {
    private ScorchedGunsPhase() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ScorchedGunsPhase::onFire);
    }

    private static void onFire(GunFireEvent.Pre event) {
        if (KittyPower.isPhasing(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
