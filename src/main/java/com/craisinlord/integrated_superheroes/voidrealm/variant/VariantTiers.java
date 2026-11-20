package com.craisinlord.integrated_superheroes.voidrealm.variant;

import com.craisinlord.integrated_superheroes.voidworld.VoidTier;
import com.craisinlord.integrated_superheroes.voidworld.VoidWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.List;

public final class VariantTiers {
    public static final int MAX = 5;
    public static final int MAX_GATE = 3;

    private VariantTiers() {
    }

    public static boolean voidLevel(ServerLevel level) {
        return VoidWorld.isVoid(level);
    }

    public static boolean spawnsIn(ServerLevel level) {
        if (voidLevel(level)) {
            return true;
        }
        String id = level.dimension().location().toString();
        return VariantConfig.ready() && VariantConfig.TEST_DIMENSIONS.get().contains(id);
    }

    public static int at(ServerLevel level, BlockPos pos) {
        int tier = voidLevel(level) ? VoidTier.at(level, pos) : VariantConfig.ready() ? VariantConfig.TEST_TIER.get() : 0;
        return Mth.clamp(tier, 0, MAX);
    }

    public static int power(int tier) {
        return VariantConfig.ready() && VariantConfig.FULL_POWER.get() ? MAX : tier;
    }

    public static double rarity(int gate) {
        if (!VariantConfig.ready()) {
            return 1.0D;
        }
        List<? extends Double> list = VariantConfig.RARITY_BY_GATE.get();
        return list.isEmpty() ? 1.0D : list.get(Mth.clamp(gate, 0, list.size() - 1));
    }

    public static boolean locked() {
        return VariantConfig.ready() && VariantConfig.TIER_LOCKS.get();
    }

    public static int forPlayer(ServerPlayer player) {
        int tier = voidLevel(player.serverLevel()) ? VoidTier.forPlayer(player) : at(player.serverLevel(), player.blockPosition());
        return Mth.clamp(tier, 0, MAX);
    }
}
