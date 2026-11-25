package com.craisinlord.integrated_superheroes.voidworld.pruning;

import net.minecraft.server.packs.resources.ResourceManager;
import java.util.Collections;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.voidworld.PrunedDebrisPool;
import com.craisinlord.integrated_superheroes.voidworld.VoidWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public record PrunedPiece(ResourceLocation id, Rotation rotation, Mirror mirror, long seed, int tier,
                          BoundingBox box, List<StructureTemplate.StructureBlockInfo> blocks) {
    public static final int MIN_GROUND = 40;
    public static final int MAX_SPAN = 32;
    private static final String LOOT_PREFIX = IntegratedSuperheroes.MODID + ":chests/void_debris/";
    private static final Map<StructureTemplate, List<List<StructureTemplate.StructureBlockInfo>>> RAW = Collections.synchronizedMap(new WeakHashMap<>());

    public record Pick(ResourceLocation id, StructureTemplate template, Rotation rotation, Mirror mirror, long seed) {
        public int radius() {
            Vec3i size = this.template.getSize(this.rotation);
            return (Math.max(size.getX(), size.getZ()) + 1) / 2;
        }
    }

    public record Grid(int sizeX, int sizeY, int sizeZ, int[] palette, int[] cells) {
    }

    public static Pick pick(ResourceManager manager, int tier, RandomSource random) {
        for (int attempt = 0; attempt < 4; attempt++) {
            ResourceLocation id = PrunedDebrisPool.pick(PrunedDebrisPool.DEFAULT_GROUP, tier, random);
            if (id == null) {
                return null;
            }
            StructureTemplate template = PrunedDebrisPool.template(manager, id);
            if (template != null) {
                Rotation rotation = Rotation.getRandom(random);
                Mirror mirror = Mirror.values()[random.nextInt(Mirror.values().length)];
                palettes(template);
                return new Pick(id, template, rotation, mirror, random.nextLong());
            }
        }
        return null;
    }

    public static int groundAt(ServerLevel level, int x, int z, int radius) {
        if (!loaded(level, x, z)) {
            return Integer.MIN_VALUE;
        }
        int base = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (base < MIN_GROUND) {
            return Integer.MIN_VALUE;
        }
        int[][] samples = {{x - radius, z - radius}, {x + radius, z - radius}, {x - radius, z + radius}, {x + radius, z + radius}};
        int overVoid = 0;
        for (int[] sample : samples) {
            if (!loaded(level, sample[0], sample[1])) {
                return Integer.MIN_VALUE;
            }
            int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sample[0], sample[1]);
            if (height < MIN_GROUND) {
                overVoid++;
            } else {
                base = Math.min(base, height);
            }
        }
        return overVoid >= 4 ? Integer.MIN_VALUE : base;
    }

    private static boolean loaded(ServerLevel level, int x, int z) {
        return level.hasChunk(x >> 4, z >> 4);
    }

    public static boolean areaLoaded(ServerLevel level, BoundingBox box, int margin) {
        for (int cx = (box.minX() - margin) >> 4; cx <= (box.maxX() + margin) >> 4; cx++) {
            for (int cz = (box.minZ() - margin) >> 4; cz <= (box.maxZ() + margin) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static PrunedPiece prepare(ServerLevel level, Pick pick, int tier, BlockPos centre, int ground) {
        Optional<Holder.Reference<StructureProcessorList>> processors = level.registryAccess()
                .registryOrThrow(Registries.PROCESSOR_LIST).getHolder(VoidWorld.PRUNED_PROCESSORS);
        if (processors.isEmpty()) {
            return null;
        }
        RandomSource random = RandomSource.create(pick.seed());
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(pick.rotation())
                .setMirror(pick.mirror())
                .setRandom(random)
                .setIgnoreEntities(true)
                .setKnownShape(true)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        processors.get().value().list().forEach(settings::addProcessor);
        StructureTemplate template = pick.template();
        BoundingBox local = template.getBoundingBox(settings, BlockPos.ZERO);
        int offsetX = centre.getX() - (local.minX() + local.maxX()) / 2;
        int offsetZ = centre.getZ() - (local.minZ() + local.maxZ()) / 2;
        int height = template.getSize().getY();
        float sink = 0.12F + random.nextFloat() * 0.14F;
        int y = Mth.clamp(ground - Math.max(1, Math.round(height * sink)), level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - height - 1);
        BlockPos origin = new BlockPos(offsetX, y - local.minY(), offsetZ);
        List<List<StructureTemplate.StructureBlockInfo>> palettes = palettes(template);
        if (palettes.isEmpty()) {
            return null;
        }
        List<StructureTemplate.StructureBlockInfo> raw = palettes.get(random.nextInt(palettes.size()));
        List<StructureTemplate.StructureBlockInfo> processed;
        try {
            processed = StructureTemplate.processBlockInfos(level, origin, origin, settings, raw, template);
        } catch (RuntimeException e) {
            IntegratedSuperheroes.LOGGER.warn("Pruned piece {} failed to process: {}", pick.id(), e.getMessage());
            return null;
        }
        List<StructureTemplate.StructureBlockInfo> blocks = new ArrayList<>(processed.size());
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (StructureTemplate.StructureBlockInfo info : processed) {
            BlockState state = info.state().mirror(pick.mirror()).rotate(pick.rotation());
            if (state.isAir()) {
                continue;
            }
            CompoundTag nbt = info.nbt();
            if (nbt != null && nbt.getString("LootTable").startsWith(LOOT_PREFIX)) {
                nbt = nbt.copy();
                nbt.putString("LootTable", LOOT_PREFIX + "tier_" + tier);
            }
            BlockPos pos = info.pos();
            blocks.add(new StructureTemplate.StructureBlockInfo(pos, state, nbt));
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        if (blocks.size() < 8 || maxX - minX >= MAX_SPAN || maxY - minY >= MAX_SPAN || maxZ - minZ >= MAX_SPAN) {
            return null;
        }
        blocks.sort(Comparator.comparingInt(info -> info.pos().getY()));
        return new PrunedPiece(pick.id(), pick.rotation(), pick.mirror(), pick.seed(), tier,
                new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ), List.copyOf(blocks));
    }

    private static List<List<StructureTemplate.StructureBlockInfo>> palettes(StructureTemplate template) {
        List<List<StructureTemplate.StructureBlockInfo>> cached = RAW.get(template);
        if (cached == null) {
            cached = rawPalettes(template);
            RAW.put(template, cached);
        }
        return cached;
    }

    private static List<List<StructureTemplate.StructureBlockInfo>> rawPalettes(StructureTemplate template) {
        CompoundTag tag = template.save(new CompoundTag());
        List<ListTag> palettes = new ArrayList<>();
        if (tag.contains("palettes", Tag.TAG_LIST)) {
            ListTag list = tag.getList("palettes", Tag.TAG_LIST);
            for (int i = 0; i < list.size(); i++) {
                palettes.add(list.getList(i));
            }
        } else {
            palettes.add(tag.getList("palette", Tag.TAG_COMPOUND));
        }
        HolderGetter<Block> lookup = BuiltInRegistries.BLOCK.asLookup();
        ListTag list = tag.getList("blocks", Tag.TAG_COMPOUND);
        List<List<StructureTemplate.StructureBlockInfo>> out = new ArrayList<>(palettes.size());
        for (ListTag palette : palettes) {
            BlockState[] states = new BlockState[palette.size()];
            for (int i = 0; i < states.length; i++) {
                states[i] = NbtUtils.readBlockState(lookup, palette.getCompound(i));
            }
            List<StructureTemplate.StructureBlockInfo> blocks = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int index = entry.getInt("state");
                ListTag pos = entry.getList("pos", Tag.TAG_INT);
                if (index < 0 || index >= states.length || pos.size() < 3) {
                    continue;
                }
                CompoundTag nbt = entry.contains("nbt", Tag.TAG_COMPOUND) ? entry.getCompound("nbt") : null;
                blocks.add(new StructureTemplate.StructureBlockInfo(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), states[index], nbt));
            }
            if (!blocks.isEmpty()) {
                out.add(List.copyOf(blocks));
            }
        }
        return List.copyOf(out);
    }

    public PrunedPiece shift(int dy) {
        if (dy == 0) {
            return this;
        }
        List<StructureTemplate.StructureBlockInfo> moved = new ArrayList<>(this.blocks.size());
        for (StructureTemplate.StructureBlockInfo info : this.blocks) {
            moved.add(new StructureTemplate.StructureBlockInfo(info.pos().above(dy), info.state(), info.nbt()));
        }
        return new PrunedPiece(this.id, this.rotation, this.mirror, this.seed, this.tier, this.box.moved(0, dy, 0), List.copyOf(moved));
    }

    public BlockPos centre() {
        return new BlockPos((this.box.minX() + this.box.maxX()) / 2, this.box.minY(), (this.box.minZ() + this.box.maxZ()) / 2);
    }

    public int radius() {
        return (Math.max(this.box.getXSpan(), this.box.getZSpan()) + 1) / 2;
    }

    public Grid grid() {
        int sizeX = this.box.getXSpan();
        int sizeY = this.box.getYSpan();
        int sizeZ = this.box.getZSpan();
        int[] cells = new int[sizeX * sizeY * sizeZ];
        List<Integer> palette = new ArrayList<>();
        Map<Integer, Integer> index = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo info : this.blocks) {
            int id = Block.getId(info.state());
            Integer slot = index.get(id);
            if (slot == null) {
                slot = palette.size();
                palette.add(id);
                index.put(id, slot);
            }
            BlockPos pos = info.pos();
            int x = pos.getX() - this.box.minX();
            int y = pos.getY() - this.box.minY();
            int z = pos.getZ() - this.box.minZ();
            cells[(y * sizeZ + z) * sizeX + x] = slot + 1;
        }
        return new Grid(sizeX, sizeY, sizeZ, palette.stream().mapToInt(Integer::intValue).toArray(), cells);
    }
}
