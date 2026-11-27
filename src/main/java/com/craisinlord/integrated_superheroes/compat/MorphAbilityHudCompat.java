package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.morph.Morph;
import com.craisinlord.morph.api.MorphApi;
import com.craisinlord.morph.api.morph.MorphInfo;
import com.craisinlord.morph.api.morph.MorphVariant;
import com.craisinlord.morph.client.hud.MorphAbilityClientState;
import com.craisinlord.morph.client.hud.MorphAbilityHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Where Morph's bottom-right ability panel is, so the hero HUD can sit above it.
 * Mirrors the visibility checks and size maths in Morph's {@code MorphAbilityHud.renderMorphPanel}
 * (Morph 1.3.10); Morph itself is not changed.
 */
public final class MorphAbilityHudCompat {
    private static boolean broken;

    private MorphAbilityHudCompat() {
    }

    /** Top edge of Morph's ability panel in GUI pixels, or -1 when it isn't drawn. */
    public static float panelTop(Minecraft minecraft, GuiGraphics graphics) {
        if (!ModCompat.MORPH || broken) {
            return -1F;
        }
        try {
            return Panel.top(minecraft, graphics);
        } catch (RuntimeException | LinkageError e) {
            broken = true;
            return -1F;
        }
    }

    private static final class Panel {
        static float top(Minecraft minecraft, GuiGraphics graphics) {
            if (minecraft.player == null || minecraft.level == null || Morph.configClient == null
                    || !Morph.configClient.abilityHudEnabled || minecraft.options.hideGui || minecraft.screen != null
                    || minecraft.player.getMainHandItem().is(MorphAbilityHud.HIDES_ABILITY_HUD)
                    || minecraft.player.getOffhandItem().is(MorphAbilityHud.HIDES_ABILITY_HUD)) {
                return -1F;
            }
            MorphInfo info = MorphApi.getApiImpl().getMorphInfo(minecraft.player);
            MorphVariant variant = info != null && info.isMorphed() && info.nextState != null ? info.nextState.variant : null;
            if (variant == null || variant.id == null) {
                return -1F;
            }
            int active = MorphAbilityClientState.getActive(variant.id).size();
            if (active == 0) {
                return -1F;
            }
            int rowHeight = minecraft.font.lineHeight + 3;
            int innerHeight = rowHeight * (active + 1) + 2;
            float scale = (float) Math.max(0.75D, 1D / minecraft.getWindow().getGuiScale());
            return graphics.guiHeight() - 4F - innerHeight * scale;
        }
    }
}
