package com.craisinlord.integrated_superheroes.voidworld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class PrunedDebrisFeature extends Feature<PrunedDebrisConfig> {
    private static final int MIN_GROUND = 40;
    private static final int ATTEMPTS = 3;

    public PrunedDebrisFeature() {
        super(PrunedDebrisConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<PrunedDebrisConfig> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        BlockPos centre = new BlockPos(chunk.getMiddleBlockX(), 0, chunk.getMiddleBlockZ());
        BoundingBox window = new BoundingBox(chunk.getMinBlockX() - 16, level.getMinBuildHeight(), chunk.getMinBlockZ() - 16,
                chunk.getMaxBlockX() + 16, level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ() + 16);
        PrunedDebrisConfig config = context.config();
        return placeAt(level, centre, config.group(), VoidTier.tierAt(centre.getX(), centre.getZ()), context.random(),
                config.processors(), config.minSink(), config.maxSink(), window) != null;
    }

    public static ResourceLocation placeAt(WorldGenLevel level, BlockPos centre, String group, int tier, RandomSource random,
                                           Holder<StructureProcessorList> processors, float minSink, float maxSink, BoundingBox window) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return null;
        }
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            ResourceLocation id = PrunedDebrisPool.pick(group, tier, random);
            if (id == null) {
                return null;
            }
            StructureTemplate template = PrunedDebrisPool.template(server.getResourceManager(), id);
            if (template == null) {
                continue;
            }
            return place(level, centre, template, random, processors, minSink, maxSink, window) ? id : null;
        }
        return null;
    }

    private static boolean place(WorldGenLevel level, BlockPos centre, StructureTemplate template, RandomSource random,
                                 Holder<StructureProcessorList> processors, float minSink, float maxSink, BoundingBox window) {
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.getRandom(random))
                .setMirror(Mirror.values()[random.nextInt(Mirror.values().length)])
                .setRandom(random)
                .setIgnoreEntities(true)
                .setKnownShape(true)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        processors.value().list().forEach(settings::addProcessor);
        if (window != null) {
            settings.setBoundingBox(window);
        }
        BoundingBox local = template.getBoundingBox(settings, BlockPos.ZERO);
        int offsetX = centre.getX() - (local.minX() + local.maxX()) / 2;
        int offsetZ = centre.getZ() - (local.minZ() + local.maxZ()) / 2;
        int minX = local.minX() + offsetX;
        int maxX = local.maxX() + offsetX;
        int minZ = local.minZ() + offsetZ;
        int maxZ = local.maxZ() + offsetZ;
        int[][] samples = {{minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ}};
        int base = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centre.getX(), centre.getZ());
        if (base < MIN_GROUND) {
            return false;
        }
        int overVoid = 0;
        for (int[] sample : samples) {
            int height = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, sample[0], sample[1]);
            if (height < MIN_GROUND) {
                overVoid++;
            } else {
                base = Math.min(base, height);
            }
        }
        if (overVoid >= 2) {
            return false;
        }
        int height = template.getSize().getY();
        float sink = minSink + random.nextFloat() * Math.max(0.0F, maxSink - minSink);
        int y = Mth.clamp(base - Math.round(height * sink), level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - height - 1);
        BlockPos pos = new BlockPos(offsetX, y - local.minY(), offsetZ);
        return template.placeInWorld(level, pos, pos, settings, random, Block.UPDATE_CLIENTS);
    }
}
