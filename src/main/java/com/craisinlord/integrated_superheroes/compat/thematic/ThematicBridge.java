package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.config.ThematicConfig;
import bond.thematic.mod.Thematic;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;

final class ThematicBridge {
    private static boolean failed;

    private ThematicBridge() {
    }

    static void applyConfig() {
        if (failed) {
            return;
        }
        try {
            ThematicConfig config = Thematic.config;
            if (config == null) {
                return;
            }
            if (ThematicOptions.on(ThematicOptions.DISABLE_STARTERS)) {
                config.startersEnabled = false;
            }
            if (ThematicOptions.on(ThematicOptions.NO_START_ITEMS)) {
                config.startItems = new String[0];
            }
            if (ThematicOptions.on(ThematicOptions.VANILLA_DEATH_MESSAGES)) {
                config.overrideDeathMessage = false;
            }
            if (ThematicOptions.on(ThematicOptions.SHOW_NAME_TAGS)) {
                config.hideNameTagsIfFar = false;
            }
        } catch (Throwable error) {
            failed = true;
            IntegratedSuperheroes.LOGGER.warn("Couldn't adjust Thematic's config; this Thematic version may have changed it", error);
        }
    }
}
