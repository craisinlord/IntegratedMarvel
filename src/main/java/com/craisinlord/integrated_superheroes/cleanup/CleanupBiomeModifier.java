package com.craisinlord.integrated_superheroes.cleanup;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

public final class CleanupBiomeModifier implements BiomeModifier {
    public static final CleanupBiomeModifier INSTANCE = new CleanupBiomeModifier();
    public static final MapCodec<CleanupBiomeModifier> CODEC = MapCodec.unit(INSTANCE);

    private CleanupBiomeModifier() {
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.REMOVE || Cleanup.removedFeatures().isEmpty()) {
            return;
        }
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            builder.getGenerationSettings().getFeatures(step).removeIf(CleanupBiomeModifier::removed);
        }
    }

    private static boolean removed(Holder<PlacedFeature> feature) {
        return feature.unwrapKey().map(key -> Cleanup.removedFeatures().contains(key.location())).orElse(false);
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
