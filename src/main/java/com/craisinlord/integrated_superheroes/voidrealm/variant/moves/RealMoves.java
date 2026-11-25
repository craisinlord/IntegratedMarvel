package com.craisinlord.integrated_superheroes.voidrealm.variant.moves;

import com.craisinlord.integrated_superheroes.hero.ServerState;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.HeroCaster;
import com.craisinlord.integrated_superheroes.hero.HeroPower;
import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantConfig;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantProfile;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantTiers;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VoidVariants;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RealMoves {
    public static final String POWER = "power:";
    public static final String SUIT = "suit:";

    public enum Mode { CAST, STATE, SKIP }

    public record Tuning(Mode mode, double minRange, double maxRange, int weight, int castTicks, int activeTicks, int stateTicks) {
        static final Tuning DEFAULT = new Tuning(Mode.CAST, 0.0D, 16.0D, 10, 10, 40, 100);
    }

    @FunctionalInterface
    public interface Source {
        double run(ServerLevel level, Mob mob, LivingEntity target);
    }

    private static final Map<String, Source> SOURCES = new LinkedHashMap<>();
    private static final Map<String, List<String>> KITS = new HashMap<>();
    private static volatile Map<String, Tuning> tunings = Map.of();
    private static volatile Map<String, List<String>> dataKits = Map.of();
    private static final int FAILED_RETRY_TICKS = 60;
    private static final java.util.Set<Integer> FAILED = ServerState.all(new java.util.HashSet<>());
    private static boolean bootstrapped;
    private static boolean registered;

    private RealMoves() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        HeroCaster.register();
        NeoForge.EVENT_BUS.addListener(RealMoves::onIncomingDamage);
    }

    public static void source(String id, Source source) {
        SOURCES.put(id, source);
    }

    public static void kit(String family, List<String> moves) {
        KITS.put(family, List.copyOf(moves));
    }

    private static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        for (HeroPower power : HeroPowers.all()) {
            List<String> kit = new ArrayList<>();
            for (HeroPower.Ability ability : power.abilities()) {
                if (ability.cast() == null) {
                    continue;
                }
                String id = POWER + power.id() + "/" + ability.name();
                HeroPower.Cast cast = ability.cast();
                source(id, (level, mob, target) -> cast.press(mob, -1));
                kit.add(id);
            }
            if (!kit.isEmpty()) {
                List<String> merged = new ArrayList<>(kit);
                for (String extra : KITS.getOrDefault(power.id(), List.of())) {
                    if (!merged.contains(extra)) {
                        merged.add(extra);
                    }
                }
                kit(power.id(), merged);
            }
        }
    }

    public static boolean isReal(String id) {
        return id.startsWith(POWER) || id.startsWith(SUIT);
    }

    public static Tuning tuning(String id) {
        return tunings.getOrDefault(id, Tuning.DEFAULT);
    }

    public static List<String> kitFor(String family) {
        bootstrap();
        List<String> kit = dataKits.get(family);
        if (kit == null) {
            kit = KITS.getOrDefault(family, List.of());
        }
        List<String> out = new ArrayList<>();
        for (String id : kit) {
            if (SOURCES.containsKey(id) && tuning(id).mode() != Mode.SKIP) {
                out.add(id);
            }
        }
        return out;
    }

    public static VariantMoves.Move get(String id) {
        if (!isReal(id)) {
            return null;
        }
        bootstrap();
        Source source = SOURCES.get(id);
        if (source == null) {
            return null;
        }
        Tuning tuning = tuning(id);
        if (tuning.mode() == Mode.SKIP) {
            return null;
        }
        return new VariantMoves.Move(id, tuning.minRange(), tuning.maxRange(), (level, mob, target) -> use(level, mob, target, id, source, tuning));
    }

    public static boolean consumeFailed(Mob mob) {
        return FAILED.remove(mob.getId());
    }

    public static boolean ready(Mob mob, String id) {
        return !isReal(id) || HeroCaster.ready(mob, id);
    }

    private static void use(ServerLevel level, Mob mob, LivingEntity target, String id, Source source, Tuning tuning) {
        HeroCaster.cast(mob, target, tuning.castTicks(), tuning.activeTicks());
        double cooldown;
        try {
            cooldown = source.run(level, mob, target);
        } catch (RuntimeException error) {
            IntegratedSuperheroes.LOGGER.warn("Void Variants: {} failed on {}", id, mob, error);
            HeroCaster.cooldown(mob, id, 20 * 60);
            return;
        }
        if (cooldown <= 0.0D) {
            HeroCaster.cooldown(mob, id, FAILED_RETRY_TICKS);
            FAILED.add(mob.getId());
            return;
        }
        HeroCaster.cooldown(mob, id, ticks(cooldown) + (tuning.mode() == Mode.STATE ? tuning.stateTicks() : 0));
        if (tuning.mode() == Mode.STATE) {
            HeroCaster.extend(mob, tuning.stateTicks());
            VillainWorld.later(level, tuning.stateTicks(), () -> {
                if (!mob.isAlive()) {
                    return;
                }
                LivingEntity now = HeroCaster.target(mob);
                if (now != null) {
                    HeroCaster.aim(mob, now, 360.0F);
                }
                double next = source.run(level, mob, now != null ? now : target);
                HeroCaster.cooldown(mob, id, ticks(Math.max(cooldown, next)));
            });
        }
    }

    private static int ticks(double seconds) {
        double scale = VariantConfig.ready() ? VariantConfig.ABILITY_COOLDOWN_SCALE.get() : 1.0D;
        return (int) Math.round(Math.max(seconds, 1.0D) * 20.0D * scale);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Mob mob) || !VoidVariants.isVariant(mob)) {
            return;
        }
        if (VoidVariants.allied(mob, event.getEntity()) && event.getEntity() != mob) {
            event.setCanceled(true);
            return;
        }
        boolean ability = event.getSource().getDirectEntity() != mob || HeroCaster.abilityActive(mob);
        if (!ability) {
            return;
        }
        VariantProfile profile = VoidVariants.profile(mob);
        if (profile == null || kitFor(familyKey(profile)).isEmpty()) {
            return;
        }
        int tier = VariantTiers.power(profile.tier());
        double scale = VariantConfig.ready() ? VariantConfig.ABILITY_DAMAGE_SCALE.get() * (1.0D + VariantConfig.DAMAGE_PER_TIER.get() * tier) : 1.0D;
        event.setAmount((float) (event.getAmount() * scale));
    }

    public static String familyKey(VariantProfile profile) {
        return profile.family();
    }

    public static void load(ResourceManager manager) {
        Map<String, Tuning> moves = new HashMap<>();
        Map<String, List<String>> kits = new HashMap<>();
        Map<ResourceLocation, Resource> files = manager.listResources("void_variants/real_moves",
                id -> id.getNamespace().equals(IntegratedSuperheroes.MODID) && id.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> file : files.entrySet()) {
            try (Reader reader = file.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("moves")) {
                    for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("moves").entrySet()) {
                        moves.put(entry.getKey(), parse(entry.getValue().getAsJsonObject()));
                    }
                }
                if (json.has("kits")) {
                    for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("kits").entrySet()) {
                        List<String> kit = new ArrayList<>();
                        entry.getValue().getAsJsonArray().forEach(move -> kit.add(move.getAsString()));
                        kits.put(entry.getKey(), List.copyOf(kit));
                    }
                }
            } catch (Exception error) {
                IntegratedSuperheroes.LOGGER.error("Void Variants: could not read {}", file.getKey(), error);
            }
        }
        tunings = Map.copyOf(moves);
        dataKits = Map.copyOf(kits);
        bootstrap();
        List<String> unknown = new ArrayList<>();
        for (String id : moves.keySet()) {
            if (!SOURCES.containsKey(id)) {
                unknown.add(id);
            }
        }
        IntegratedSuperheroes.LOGGER.info("Void Variants: {} real ability moves, {} tuned, {} kit overrides", SOURCES.size(), moves.size(), kits.size());
        if (!unknown.isEmpty()) {
            IntegratedSuperheroes.LOGGER.warn("Void Variants: tuned but not registered: {}", unknown);
        }
    }

    private static Tuning parse(JsonObject json) {
        Tuning base = Tuning.DEFAULT;
        Mode mode = json.has("mode") ? Mode.valueOf(json.get("mode").getAsString().toUpperCase(java.util.Locale.ROOT)) : base.mode();
        return new Tuning(mode,
                json.has("min_range") ? json.get("min_range").getAsDouble() : base.minRange(),
                json.has("max_range") ? json.get("max_range").getAsDouble() : base.maxRange(),
                json.has("weight") ? json.get("weight").getAsInt() : base.weight(),
                json.has("cast_ticks") ? json.get("cast_ticks").getAsInt() : base.castTicks(),
                json.has("active_ticks") ? json.get("active_ticks").getAsInt() : base.activeTicks(),
                json.has("state_ticks") ? json.get("state_ticks").getAsInt() : base.stateTicks());
    }
}
