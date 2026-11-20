package com.craisinlord.integrated_superheroes.voidworld;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;

public final class PrunedDecayProcessor extends StructureProcessor {
    public static final MapCodec<PrunedDecayProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("erode_to").forGetter(p -> p.erodeTo),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("integrity", 0.9F).forGetter(p -> p.integrity),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("integrity_per_tier", 0.04F).forGetter(p -> p.integrityPerTier),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("height_fade", 0.02F).forGetter(p -> p.heightFade),
            Codec.intRange(0, 64).optionalFieldOf("fade_start", 4).forGetter(p -> p.fadeStart),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("erode_chance", 0.12F).forGetter(p -> p.erodeChance),
            Codec.STRING.optionalFieldOf("loot_prefix", IntegratedSuperheroes.MODID + ":chests/void_debris/tier_").forGetter(p -> p.lootPrefix)
    ).apply(instance, PrunedDecayProcessor::new));

    private final BlockStateProvider erodeTo;
    private final float integrity;
    private final float integrityPerTier;
    private final float heightFade;
    private final int fadeStart;
    private final float erodeChance;
    private final String lootPrefix;

    private PrunedDecayProcessor(BlockStateProvider erodeTo, float integrity, float integrityPerTier, float heightFade,
                                 int fadeStart, float erodeChance, String lootPrefix) {
        this.erodeTo = erodeTo;
        this.integrity = integrity;
        this.integrityPerTier = integrityPerTier;
        this.heightFade = heightFade;
        this.fadeStart = fadeStart;
        this.erodeChance = erodeChance;
        this.lootPrefix = lootPrefix;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pivot,
                                                             StructureTemplate.StructureBlockInfo original,
                                                             StructureTemplate.StructureBlockInfo current,
                                                             StructurePlaceSettings settings) {
        BlockState state = current.state();
        BlockPos pos = current.pos();
        if (state.isAir() || state.is(VoidWorld.DEBRIS_STRIP)) {
            return null;
        }
        if (state.getBlock() instanceof LiquidBlock) {
            return null;
        }
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, false);
        } else if (!state.getFluidState().isEmpty()) {
            return null;
        }
        boolean keepEntity = state.is(VoidWorld.DEBRIS_KEEP_ENTITY);
        if ((current.nbt() != null || state.hasBlockEntity()) && !keepEntity) {
            return null;
        }
        RandomSource random = settings.getRandom(pos);
        int tier = VoidTier.tierAt(pos.getX(), pos.getZ());
        float keep = this.integrity - this.integrityPerTier * tier
                - this.heightFade * Math.max(0, pos.getY() - offset.getY() - this.fadeStart);
        if (!keepEntity && random.nextFloat() > keep) {
            return null;
        }
        if (state.getBlock() instanceof FallingBlock) {
            state = this.erodeTo.getState(random, pos);
        } else if (!keepEntity && random.nextFloat() < this.erodeChance && state.isSolidRender(level, pos)) {
            state = this.erodeTo.getState(random, pos);
        }
        CompoundTag nbt = current.nbt();
        if (keepEntity && nbt != null) {
            nbt = nbt.copy();
            nbt.remove("Items");
            nbt.remove("item");
            nbt.remove("LootTable");
            nbt.remove("LootTableSeed");
        }
        return new StructureTemplate.StructureBlockInfo(pos, state, nbt);
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos offset, BlockPos pivot,
                                                                         List<StructureTemplate.StructureBlockInfo> originals,
                                                                         List<StructureTemplate.StructureBlockInfo> processed,
                                                                         StructurePlaceSettings settings) {
        List<Integer> containers = new ArrayList<>();
        for (int i = 0; i < processed.size(); i++) {
            StructureTemplate.StructureBlockInfo info = processed.get(i);
            if (info.nbt() != null && info.state().is(VoidWorld.DEBRIS_KEEP_ENTITY) && isContainer(originals, info, i)) {
                containers.add(i);
            }
        }
        if (containers.isEmpty()) {
            return processed;
        }
        int index = containers.get(settings.getRandom(offset).nextInt(containers.size()));
        StructureTemplate.StructureBlockInfo chosen = processed.get(index);
        CompoundTag nbt = chosen.nbt().copy();
        int tier = VoidTier.tierAt(chosen.pos().getX(), chosen.pos().getZ());
        nbt.putString("LootTable", this.lootPrefix + tier);
        List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>(processed);
        result.set(index, new StructureTemplate.StructureBlockInfo(chosen.pos(), chosen.state(), nbt));
        return result;
    }

    private static boolean isContainer(List<StructureTemplate.StructureBlockInfo> originals, StructureTemplate.StructureBlockInfo info, int index) {
        CompoundTag source = index < originals.size() ? originals.get(index).nbt() : null;
        if (source == null) {
            source = info.nbt();
        }
        return source.contains("Items") || source.contains("LootTable");
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return VoidWorld.PRUNED_DECAY.get();
    }
}
