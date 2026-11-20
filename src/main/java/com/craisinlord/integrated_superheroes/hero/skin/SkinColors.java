package com.craisinlord.integrated_superheroes.hero.skin;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

public final class SkinColors {
    private SkinColors() {
    }

    public static Vector3f vector(int color) {
        return new Vector3f((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
    }

    public static float red(int color) {
        return (color >> 16 & 0xFF) / 255.0F;
    }

    public static float green(int color) {
        return (color >> 8 & 0xFF) / 255.0F;
    }

    public static float blue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    public static DustParticleOptions dust(int color, float size) {
        return new DustParticleOptions(vector(color), size);
    }

    public static DustParticleOptions primaryDust(Player player, DustParticleOptions fallback) {
        return SerumSkins.current(player).map(skin -> dust(skin.primary(), fallback.getScale())).orElse(fallback);
    }

    public static DustParticleOptions secondaryDust(Player player, DustParticleOptions fallback) {
        return SerumSkins.current(player).map(skin -> dust(skin.secondary(), fallback.getScale())).orElse(fallback);
    }

    public static int lerp(int from, int to, float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        int red = (int) ((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * clamped);
        int green = (int) ((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * clamped);
        int blue = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * clamped);
        return red << 16 | green << 8 | blue;
    }
}
