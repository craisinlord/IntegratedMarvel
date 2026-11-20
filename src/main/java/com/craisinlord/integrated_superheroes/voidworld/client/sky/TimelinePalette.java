package com.craisinlord.integrated_superheroes.voidworld.client.sky;

import com.craisinlord.integrated_superheroes.voidworld.VoidConfig;
import com.craisinlord.integrated_superheroes.voidworld.VoidTier;
import java.util.List;
import net.minecraft.util.Mth;

public final class TimelinePalette {
    private static final float[][] HUES = {
            {1.00F, 0.86F, 0.55F},
            {0.72F, 0.95F, 0.62F},
            {0.45F, 0.85F, 0.78F},
            {0.70F, 0.55F, 0.95F},
            {0.62F, 0.38F, 0.95F},
            {0.55F, 0.25F, 0.85F}
    };
    private static final int[] DEFAULT_EDGES = {0, 600, 1800, 3600, 6000, 9000};

    private TimelinePalette() {
    }

    public static float level(double x, double z) {
        int tier = VoidTier.tierAt(Mth.floor(x), Mth.floor(z));
        if (tier >= VoidTier.MAX) {
            return VoidTier.MAX;
        }
        int from = edge(tier);
        int to = edge(tier + 1);
        if (from < 0 || to <= from) {
            return tier;
        }
        double distance = Math.sqrt(x * x + z * z);
        return tier + (float) Mth.clamp((distance - from) / (to - from), 0.0D, 1.0D);
    }

    public static void hue(float level, float[] out) {
        float clamped = Mth.clamp(level, 0.0F, HUES.length - 1.0F);
        int index = Math.min((int) clamped, HUES.length - 2);
        float t = clamped - index;
        float[] a = HUES[index];
        float[] b = HUES[index + 1];
        out[0] = Mth.lerp(t, a[0], b[0]);
        out[1] = Mth.lerp(t, a[1], b[1]);
        out[2] = Mth.lerp(t, a[2], b[2]);
    }

    private static int edge(int index) {
        if (VoidConfig.SPEC.isLoaded()) {
            List<? extends Integer> list = VoidConfig.RING_EDGES.get();
            if (!list.isEmpty()) {
                return index < list.size() ? list.get(index) : -1;
            }
        }
        return index < DEFAULT_EDGES.length ? DEFAULT_EDGES[index] : -1;
    }
}
