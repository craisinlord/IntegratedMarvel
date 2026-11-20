package com.craisinlord.integrated_superheroes.voidworld;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

public record PrunedDebrisConfig(String group, Holder<StructureProcessorList> processors, float minSink, float maxSink)
        implements FeatureConfiguration {
    public static final Codec<PrunedDebrisConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", PrunedDebrisPool.DEFAULT_GROUP).forGetter(PrunedDebrisConfig::group),
            StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter(PrunedDebrisConfig::processors),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("min_sink", 0.25F).forGetter(PrunedDebrisConfig::minSink),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("max_sink", 0.6F).forGetter(PrunedDebrisConfig::maxSink)
    ).apply(instance, PrunedDebrisConfig::new));
}
