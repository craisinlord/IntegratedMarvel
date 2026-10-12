package com.craisinlord.integrated_marvel.gauntlet;

import com.mojang.blaze3d.platform.InputConstants;
import net.redgitreds.infinitygauntlet.init.InfinityGauntletModKeyMappings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class LegacyGauntletControls {
    private static boolean legacyMappingsCleared;

    private LegacyGauntletControls() {
    }

    public static void unbindLegacyMappings(Minecraft minecraft) {
        if (legacyMappingsCleared) return;
        legacyMappingsCleared = true;
        boolean changed = false;
        for (KeyMapping mapping : List.of(InfinityGauntletModKeyMappings.GAUNTLET_POWERS,
                InfinityGauntletModKeyMappings.A_SWAP, InfinityGauntletModKeyMappings.UI_KEY,
                InfinityGauntletModKeyMappings.SELECT_U_IKEY)) {
            if (!mapping.isUnbound()) {
                mapping.setKey(InputConstants.UNKNOWN);
                changed = true;
            }
        }
        if (changed) {
            KeyMapping.resetMapping();
            minecraft.options.save();
        }
    }
}
