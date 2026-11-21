package com.craisinlord.integrated_superheroes.voidrealm.variant;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicVariants;
import com.craisinlord.integrated_superheroes.hero.HeroPower;
import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import com.craisinlord.integrated_superheroes.hero.IronManSuits;
import com.craisinlord.integrated_superheroes.hero.ViltrumitePower;
import com.craisinlord.integrated_superheroes.hero.skin.SerumCostumes;
import com.craisinlord.integrated_superheroes.hero.skin.SerumSkin;
import com.craisinlord.integrated_superheroes.hero.skin.SerumSkins;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VariantPool {
    private static final Pattern MARK = Pattern.compile("mark_(\\d+)$");
    private static volatile VariantPool current;
    private static volatile boolean dirty = true;

    private final VariantPoolData data;
    private final Map<VariantKind, Map<String, VariantFamily>> families = new EnumMap<>(VariantKind.class);

    private VariantPool(VariantPoolData data) {
        this.data = data;
        for (VariantKind kind : VariantKind.values()) {
            families.put(kind, new LinkedHashMap<>());
        }
    }

    public static void markDirty() {
        dirty = true;
    }

    public static VariantPool get(MinecraftServer server) {
        VariantPool pool = current;
        if (pool == null || dirty) {
            synchronized (VariantPool.class) {
                if (current == null || dirty) {
                    dirty = false;
                    current = build(server);
                }
                pool = current;
            }
        }
        return pool;
    }

    public static void clear() {
        current = null;
        dirty = true;
    }

    private static VariantPool build(MinecraftServer server) {
        VariantPool pool = new VariantPool(VariantPoolData.load(server.getResourceManager()));
        com.craisinlord.integrated_superheroes.voidrealm.variant.moves.ThematicKits.load(server.getResourceManager());
        if (ModCompat.THEMATIC) {
            try {
                ThematicVariants.applyAllowlist(pool.data);
                ThematicVariants.families(pool.data).forEach(pool::add);
            } catch (RuntimeException | LinkageError error) {
                IntegratedSuperheroes.LOGGER.error("Void Variants: could not read the Thematic suits", error);
            }
        }
        pool.addSerums();
        pool.dropSerumCostumes();
        pool.addMarvel();
        pool.addEntities();
        IntegratedSuperheroes.LOGGER.info("Void Variants pool: {}", pool.summary());
        return pool;
    }

    public VariantPoolData data() {
        return data;
    }

    public void add(VariantFamily family) {
        if (!data.excluded.contains(family.key())) {
            families.get(family.kind()).put(family.id(), family);
        }
    }

    private void addSerums() {
        SerumSkins.bootstrap();
        for (HeroPower power : HeroPowers.all()) {
            String id = power.id();
            boolean viltrumite = ViltrumitePower.ID.equals(id);
            VariantKind kind = viltrumite ? VariantKind.VILTRUMITE : VariantKind.SERUM;
            List<String> looks = new ArrayList<>();
            List<SerumSkin> skins = SerumSkins.forPower(id);
            if (!viltrumite || skins.isEmpty()) {
                looks.add(id);
            }
            skins.forEach(skin -> looks.add(skin.id()));
            String key = kind.key() + ":" + id;
            add(new VariantFamily(kind, id, looks, data.gate(key, viltrumite ? 3 : 0), VariantAlignment.HERO,
                    data.weight(key), false, "serum"));
        }
        for (String id : data.legacySerums) {
            String key = "serum:" + id;
            add(new VariantFamily(VariantKind.SERUM, id, List.of(id), data.gate(key, 0), VariantAlignment.HERO, data.weight(key), false, "legacy"));
        }
    }

    private void dropSerumCostumes() {
        Map<String, VariantFamily> thematic = families.get(VariantKind.THEMATIC);
        for (VariantKind kind : List.of(VariantKind.SERUM, VariantKind.VILTRUMITE)) {
            for (VariantFamily family : families.get(kind).values()) {
                for (String look : family.looks()) {
                    String worn = costume(kind, family.id(), look);
                    if (worn != null && !worn.isEmpty()) {
                        thematic.remove(worn.substring(worn.indexOf(':') + 1));
                    }
                }
            }
        }
    }

    private void addMarvel() {
        for (ResourceLocation key : BuiltInRegistries.ITEM.keySet()) {
            if (!"marvel".equals(key.getNamespace()) || !key.getPath().endsWith("_chestplate")) {
                continue;
            }
            String set = key.getPath().substring(0, key.getPath().length() - "_chestplate".length());
            if (IronManSuits.get(set) == null && !data.marvelKits.contains(set)) {
                continue;
            }
            String familyKey = "marvel:" + set;
            add(new VariantFamily(VariantKind.MARVEL, set, List.of(set), data.gate(familyKey, marvelGate(set)), VariantAlignment.HERO,
                    data.weight(familyKey), false, IronManSuits.get(set) != null ? "iron_man" : "kit"));
        }
    }

    private static int marvelGate(String set) {
        Matcher matcher = MARK.matcher(set);
        if (set.startsWith("iron_man") && matcher.find()) {
            int mark = Integer.parseInt(matcher.group(1));
            return mark <= 3 ? 0 : mark <= 7 ? 1 : mark < 15 ? 2 : 3;
        }
        return set.startsWith("war_machine_mark_1") ? 1 : 2;
    }

    private void addEntities() {
        for (VariantPoolData.EntityEntry entry : data.entities) {
            ResourceLocation id = ResourceLocation.tryParse(entry.id());
            if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                continue;
            }
            String key = entry.kind().key() + ":" + id.getPath();
            add(new VariantFamily(entry.kind(), id.getPath(), List.of(id.toString()), data.gate(key, entry.gate()), entry.alignment(),
                    data.weight(key), true, "entity"));
        }
    }

    public Map<String, VariantFamily> families(VariantKind kind) {
        return Collections.unmodifiableMap(families.get(kind));
    }

    public VariantFamily family(VariantKind kind, String id) {
        return families.get(kind).get(id);
    }

    public static String costume(VariantKind kind, String family, String look) {
        if (kind != VariantKind.SERUM && kind != VariantKind.VILTRUMITE) {
            return "";
        }
        if (!look.equals(family)) {
            return SerumSkins.byId(look).map(SerumSkin::costume).orElse("");
        }
        return SerumCostumes.forPower(family);
    }

    public static boolean lookReady(VariantFamily family, String look) {
        if (family.entity()) {
            return true;
        }
        return switch (family.kind()) {
            case THEMATIC, SERUM, VILTRUMITE, MARVEL -> true;
            case ENTITY -> false;
        };
    }

    public static Item thematicItem(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        ResourceLocation key = ResourceLocation.tryParse(id.contains(":") ? id : "thematic:" + id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(key);
        return item == Items.AIR ? null : item;
    }

    public List<String> readyLooks(VariantFamily family) {
        List<String> looks = new ArrayList<>();
        for (String look : family.looks()) {
            if (lookReady(family, look)) {
                looks.add(look);
            }
        }
        return looks;
    }

    public VariantAlignment alignment(VariantFamily family, String look) {
        String lookKey = null;
        if (!look.equals(family.id())) {
            lookKey = family.kind() == VariantKind.THEMATIC ? "thematic:" + look : "skin:" + look;
        }
        return data.alignment(lookKey, family.key(), family.alignment());
    }

    public VariantProfile roll(RandomSource random, int tier, VariantKind only) {
        Map<VariantKind, List<VariantFamily>> eligible = new EnumMap<>(VariantKind.class);
        Map<VariantKind, Double> shares = new EnumMap<>(VariantKind.class);
        double budgetTotal = 0.0D;
        for (VariantKind kind : VariantKind.values()) {
            if (only != null && kind != only) {
                continue;
            }
            List<VariantFamily> list = new ArrayList<>();
            int spawnable = 0;
            for (VariantFamily family : families.get(kind).values()) {
                if (readyLooks(family).isEmpty()) {
                    continue;
                }
                spawnable++;
                if (!VariantTiers.locked() || family.gate() <= tier) {
                    list.add(family);
                }
            }
            double budget = only != null ? 1.0D : data.budgets.getOrDefault(kind, 0.0D) * list.size() / Math.max(1, spawnable);
            if (!list.isEmpty() && budget > 0.0D) {
                eligible.put(kind, list);
                shares.put(kind, budget);
                budgetTotal += budget;
            }
        }
        if (eligible.isEmpty()) {
            return null;
        }
        double pick = random.nextDouble() * budgetTotal;
        VariantKind chosen = null;
        for (VariantKind kind : eligible.keySet()) {
            chosen = kind;
            pick -= shares.get(kind);
            if (pick < 0.0D) {
                break;
            }
        }
        List<VariantFamily> list = eligible.get(chosen);
        double total = 0.0D;
        for (VariantFamily family : list) {
            total += weight(family, tier);
        }
        double roll = random.nextDouble() * total;
        VariantFamily family = list.get(list.size() - 1);
        for (VariantFamily candidate : list) {
            roll -= weight(candidate, tier);
            if (roll < 0.0D) {
                family = candidate;
                break;
            }
        }
        return profile(family, null, tier, random);
    }

    private static double weight(VariantFamily family, int tier) {
        return VariantTiers.locked() ? family.weightAt(tier) : family.weight() * VariantTiers.rarity(family.gate());
    }

    public VariantProfile profile(VariantFamily family, String look, int tier, RandomSource random) {
        List<String> looks = readyLooks(family);
        if (looks.isEmpty()) {
            return null;
        }
        String chosen = look != null && looks.contains(look) ? look : looks.get(random.nextInt(looks.size()));
        return new VariantProfile(family.kind(), family.id(), chosen, tier, alignment(family, chosen), List.of());
    }

    public String summary() {
        StringBuilder out = new StringBuilder();
        for (VariantKind kind : VariantKind.values()) {
            Map<String, VariantFamily> map = families.get(kind);
            int looks = 0;
            int ready = 0;
            for (VariantFamily family : map.values()) {
                looks += family.looks().size();
                if (!readyLooks(family).isEmpty()) {
                    ready++;
                }
            }
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(kind.key()).append(' ').append(map.size()).append(" families (").append(looks).append(" looks, ")
                    .append(ready).append(" spawnable)");
        }
        return out.toString();
    }

    public int[] gateCounts(VariantKind kind) {
        int[] counts = new int[VariantTiers.MAX_GATE + 1];
        for (VariantFamily family : families.get(kind).values()) {
            counts[Math.min(VariantTiers.MAX_GATE, family.gate())]++;
        }
        return counts;
    }

    public int villains(VariantKind kind) {
        int count = 0;
        for (VariantFamily family : families.get(kind).values()) {
            if (alignment(family, family.id()) == VariantAlignment.VILLAIN) {
                count++;
            }
        }
        return count;
    }
}
