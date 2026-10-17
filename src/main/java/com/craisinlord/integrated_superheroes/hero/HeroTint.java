package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class HeroTint {
    public static final int HULK = 0xFF73D959;
    private static final int PHASE_ALPHA = 0x66;
    private static final int INVISIBLE_ALPHA = 0x40;

    private HeroTint() {
    }

    public static int colorFor(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return -1;
        }
        if (HulkPower.isHulk(player)) {
            return hulk(HulkPower.clientRage(player) / 100.0F);
        }
        return -1;
    }

    private static int hulk(float rage) {
        if (rage <= 0.0F) {
            return HULK;
        }
        float t = Math.min(1.0F, rage);
        int red = (int) (0x73 + (0x52 - 0x73) * t);
        int green = (int) (0xD9 + (0xFF - 0xD9) * t);
        int blue = (int) (0x59 + (0x38 - 0x59) * t);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    public static int tint(LivingEntity entity, int color) {
        int tint = colorFor(entity);
        int result = tint == -1 ? color : FastColor.ARGB32.multiply(color, tint);
        if (entity instanceof Player player && KittyPower.isPhasing(player)) {
            result = (result & 0x00FFFFFF) | PHASE_ALPHA << 24;
        }
        if (entity instanceof Player player && InvisibleWomanPower.isInvisible(player)) {
            result = (result & 0x00FFFFFF) | INVISIBLE_ALPHA << 24;
        }
        return result;
    }

    public static int ghostAlpha(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return -1;
        }
        if (InvisibleWomanPower.isInvisible(player)) {
            return INVISIBLE_ALPHA;
        }
        return KittyPower.isPhasing(player) ? PHASE_ALPHA : -1;
    }

    public static boolean translucent(LivingEntity entity) {
        return entity instanceof Player player && (KittyPower.isPhasing(player) || InvisibleWomanPower.isInvisible(player));
    }
}
