package com.craisinlord.integrated_superheroes.voidworld;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.List;

public final class VoidTierChance extends PlacementFilter {
    public static final MapCodec<VoidTierChance> CODEC = Codec.floatRange(0.0F, 1.0F).listOf().fieldOf("chances")
            .xmap(VoidTierChance::new, filter -> filter.chances);

    private final List<Float> chances;

    private VoidTierChance(List<Float> chances) {
        this.chances = List.copyOf(chances);
    }

    public static float chanceFor(List<Float> chances, int tier) {
        if (chances.isEmpty()) {
            return 0.0F;
        }
        return chances.get(Math.min(tier, chances.size() - 1));
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        int tier = VoidTier.tierAt(pos.getX(), pos.getZ());
        return random.nextFloat() < chanceFor(this.chances, tier);
    }

    @Override
    public PlacementModifierType<?> type() {
        return VoidWorld.TIER_CHANCE.get();
    }
}
