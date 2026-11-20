package com.craisinlord.integrated_superheroes.voidworld;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class PrunedDebrisPool {
    public static final String DEFAULT_GROUP = "debris";
    public static final int MAX_FOOTPRINT = 32;
    public static final int MAX_HEIGHT = 32;
    private static final int MIN_SOLID = 40;
    private static final int MAX_JIGSAWS = 2;
    private static final int CACHE_SIZE = 64;
    private static final FileToIdConverter POOL_FILES = FileToIdConverter.json("integrated_superheroes/void_debris");
    private static final FileToIdConverter TEMPLATE_FILES = new FileToIdConverter("structure", ".nbt");

    public record Entry(ResourceLocation source, int weight, int minTier, int maxTier, List<ResourceLocation> ids) {
        boolean allows(int tier) {
            return tier >= this.minTier && tier <= this.maxTier && !this.ids.isEmpty() && this.weight > 0;
        }
    }

    private static volatile Map<String, List<Entry>> groups = Map.of();
    private static final Map<ResourceLocation, Boolean> VERDICTS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, StructureTemplate> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(CACHE_SIZE, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<ResourceLocation, StructureTemplate> eldest) {
                    return this.size() > CACHE_SIZE;
                }
            });

    public static final SimplePreparableReloadListener<Map<String, List<Entry>>> LISTENER = new SimplePreparableReloadListener<>() {
        @Override
        protected Map<String, List<Entry>> prepare(ResourceManager manager, ProfilerFiller profiler) {
            return build(manager);
        }

        @Override
        protected void apply(Map<String, List<Entry>> built, ResourceManager manager, ProfilerFiller profiler) {
            groups = built;
            VERDICTS.clear();
            CACHE.clear();
            int templates = built.values().stream().flatMap(List::stream).mapToInt(entry -> entry.ids().size()).sum();
            IntegratedSuperheroes.LOGGER.info("Void debris pool: {} groups, {} template slots", built.size(), templates);
        }
    };

    private PrunedDebrisPool() {
    }

    public static Map<String, List<Entry>> groups() {
        return groups;
    }

    public static ResourceLocation pick(String group, int tier, RandomSource random) {
        List<Entry> entries = groups.getOrDefault(group, List.of());
        int total = 0;
        for (Entry entry : entries) {
            if (entry.allows(tier)) {
                total += entry.weight();
            }
        }
        if (total <= 0) {
            return null;
        }
        for (int tries = 0; tries < 8; tries++) {
            int roll = random.nextInt(total);
            for (Entry entry : entries) {
                if (!entry.allows(tier)) {
                    continue;
                }
                roll -= entry.weight();
                if (roll < 0) {
                    ResourceLocation id = entry.ids().get(random.nextInt(entry.ids().size()));
                    if (VERDICTS.get(id) != Boolean.FALSE) {
                        return id;
                    }
                    break;
                }
            }
        }
        return null;
    }

    public static StructureTemplate template(ResourceManager manager, ResourceLocation id) {
        StructureTemplate cached = CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        if (VERDICTS.get(id) == Boolean.FALSE) {
            return null;
        }
        CompoundTag tag;
        try (InputStream in = manager.open(TEMPLATE_FILES.idToFile(id))) {
            tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            int version = NbtUtils.getDataVersion(tag, 500);
            tag = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), tag, version);
        } catch (Exception e) {
            VERDICTS.put(id, Boolean.FALSE);
            return null;
        }
        HolderGetter<Block> blocks = BuiltInRegistries.BLOCK.asLookup();
        boolean ok;
        try {
            ok = accepts(blocks, tag);
        } catch (RuntimeException e) {
            ok = false;
        }
        VERDICTS.put(id, ok);
        if (!ok) {
            return null;
        }
        StructureTemplate template = new StructureTemplate();
        template.load(blocks, tag);
        CACHE.put(id, template);
        return template;
    }

    private static boolean accepts(HolderGetter<Block> blocks, CompoundTag tag) {
        ListTag size = tag.getList("size", Tag.TAG_INT);
        if (size.size() < 3) {
            return false;
        }
        if (size.getInt(0) > MAX_FOOTPRINT || size.getInt(1) > MAX_HEIGHT || size.getInt(2) > MAX_FOOTPRINT) {
            return false;
        }
        ListTag palette = tag.contains("palettes", Tag.TAG_LIST)
                ? tag.getList("palettes", Tag.TAG_LIST).getList(0)
                : tag.getList("palette", Tag.TAG_COMPOUND);
        BlockState[] states = new BlockState[palette.size()];
        for (int i = 0; i < states.length; i++) {
            states[i] = NbtUtils.readBlockState(blocks, palette.getCompound(i));
        }
        ListTag list = tag.getList("blocks", Tag.TAG_COMPOUND);
        int solid = 0;
        int jigsaws = 0;
        for (int i = 0; i < list.size(); i++) {
            int index = list.getCompound(i).getInt("state");
            if (index < 0 || index >= states.length) {
                continue;
            }
            BlockState state = states[index];
            if (state.is(VoidWorld.DEBRIS_REJECT)) {
                return false;
            }
            if (state.is(Blocks.JIGSAW)) {
                jigsaws++;
            } else if (!state.isAir() && !state.is(Blocks.STRUCTURE_VOID) && !(state.getBlock() instanceof LiquidBlock)) {
                solid++;
            }
        }
        return solid >= MIN_SOLID && jigsaws <= MAX_JIGSAWS;
    }

    private static Map<String, List<Entry>> build(ResourceManager manager) {
        List<ResourceLocation> templates = new ArrayList<>(TEMPLATE_FILES.listMatchingResources(manager).keySet().stream()
                .map(TEMPLATE_FILES::fileToId).toList());
        Collections.sort(templates);
        Map<ResourceLocation, JsonObject> files = new TreeMap<>();
        for (Map.Entry<ResourceLocation, Resource> file : POOL_FILES.listMatchingResources(manager).entrySet()) {
            try (Reader reader = file.getValue().openAsReader()) {
                files.put(POOL_FILES.fileToId(file.getKey()), GsonHelper.parse(reader));
            } catch (Exception e) {
                IntegratedSuperheroes.LOGGER.warn("Bad void debris pool file {}: {}", file.getKey(), e.getMessage());
            }
        }
        List<Pattern> everywhere = new ArrayList<>();
        for (JsonObject json : files.values()) {
            everywhere.addAll(globs(json, "exclude_everywhere"));
        }
        Map<String, List<Entry>> built = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonObject> file : files.entrySet()) {
            JsonObject json = file.getValue();
            if (!json.has("include")) {
                continue;
            }
            List<Pattern> include = globs(json, "include");
            List<Pattern> exclude = new ArrayList<>(globs(json, "exclude"));
            exclude.addAll(everywhere);
            List<ResourceLocation> ids = new ArrayList<>();
            for (ResourceLocation id : templates) {
                String text = id.toString();
                if (matchesAny(include, text) && !matchesAny(exclude, text)) {
                    ids.add(id);
                }
            }
            if (ids.isEmpty()) {
                continue;
            }
            String group = GsonHelper.getAsString(json, "group", DEFAULT_GROUP);
            int weight = GsonHelper.getAsInt(json, "weight", 10);
            int minTier = VoidTier.clamp(GsonHelper.getAsInt(json, "min_tier", 0));
            int maxTier = VoidTier.clamp(GsonHelper.getAsInt(json, "max_tier", VoidTier.MAX));
            built.computeIfAbsent(group, key -> new ArrayList<>()).add(new Entry(file.getKey(), weight, minTier, maxTier, List.copyOf(ids)));
        }
        Map<String, List<Entry>> frozen = new HashMap<>();
        built.forEach((key, value) -> frozen.put(key, List.copyOf(value)));
        return Map.copyOf(frozen);
    }

    private static boolean matchesAny(List<Pattern> patterns, String text) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(text).matches()) {
                return true;
            }
        }
        return false;
    }

    private static List<Pattern> globs(JsonObject json, String key) {
        if (!json.has(key)) {
            return List.of();
        }
        List<Pattern> patterns = new ArrayList<>();
        JsonArray array = GsonHelper.getAsJsonArray(json, key);
        for (JsonElement element : array) {
            patterns.add(glob(element.getAsString()));
        }
        return patterns;
    }

    static Pattern glob(String glob) {
        StringBuilder regex = new StringBuilder();
        if (!glob.contains(":")) {
            regex.append("[^:]+:");
        }
        boolean anyDepthSuffix = glob.endsWith("/**");
        if (anyDepthSuffix) {
            glob = glob.substring(0, glob.length() - 3);
        }
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                    regex.append(".*");
                    i++;
                } else {
                    regex.append("[^/]*");
                }
            } else if (c == '?') {
                regex.append("[^/]");
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        if (anyDepthSuffix) {
            regex.append("(/.*)?");
        }
        return Pattern.compile(regex.toString());
    }
}
