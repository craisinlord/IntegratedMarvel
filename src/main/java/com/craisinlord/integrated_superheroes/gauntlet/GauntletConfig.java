package com.craisinlord.integrated_superheroes.gauntlet;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GauntletConfig {
    public static final ModConfigSpec SPEC;
    private static final Map<String, List<ModConfigSpec.BooleanValue>> ABILITIES = new LinkedHashMap<>();
    private static final Map<String, List<ModConfigSpec.IntValue>> COOLDOWNS = new LinkedHashMap<>();
    public static final ModConfigSpec.DoubleValue POWER_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue INFINITY_TIER;
    public static final ModConfigSpec.BooleanValue TERRAIN_GRIEFING;
    public static final ModConfigSpec.BooleanValue WORLD_WIDE_EFFECTS;
    public static final ModConfigSpec.BooleanValue REALITY_BLOCK_ENTITY_CONTENTS;
    public static final ModConfigSpec.IntValue REALITY_MAX_REGION_VOLUME;
    public static final ModConfigSpec.IntValue BLOCK_EDITS_PER_TICK;
    public static final ModConfigSpec.IntValue SOUL_ALLY_CAP;
    public static final ModConfigSpec.IntValue BANISH_DISTANCE;
    public static final ModConfigSpec.IntValue REWIND_SECONDS;
    public static final ModConfigSpec.IntValue TIME_LOOP_COOLDOWN;
    public static final ModConfigSpec.IntValue TIME_SURGE_TICK_MULTIPLIER;
    public static final ModConfigSpec.IntValue TIME_SURGE_CHUNK_RADIUS;
    public static final ModConfigSpec.IntValue TIME_SURGE_DAY_SPEED;
    public static final ModConfigSpec.IntValue TIME_SURGE_BLOCK_ENTITY_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("abilities");
        for (String stone : GauntletAbilities.STONES) {
            builder.push(stone);
            List<ModConfigSpec.BooleanValue> values = new ArrayList<>();
            for (GauntletAbility ability : GauntletAbilities.forStone(stone)) {
                values.add(builder.define(ability.id(), true));
            }
            ABILITIES.put(stone, values);
            builder.pop();
        }
        builder.pop();
        builder.push("cooldowns");
        for (String stone : GauntletAbilities.STONES) {
            builder.push(stone);
            List<ModConfigSpec.IntValue> values = new ArrayList<>();
            for (GauntletAbility ability : GauntletAbilities.forStone(stone)) {
                values.add(builder.defineInRange(ability.id(), ability.defaultCooldown(), 0, 72000));
            }
            COOLDOWNS.put(stone, values);
            builder.pop();
        }
        builder.pop();
        builder.push("general");
        POWER_MULTIPLIER = builder.defineInRange("powerMultiplier", 1.0D, 0.1D, 10.0D);
        INFINITY_TIER = builder.define("infinityTier", true);
        TERRAIN_GRIEFING = builder.define("terrainGriefing", true);
        WORLD_WIDE_EFFECTS = builder.define("worldWideEffects", true);
        BLOCK_EDITS_PER_TICK = builder.defineInRange("blockEditsPerTick", 1024, 16, 65536);
        builder.pop();
        builder.push("reality");
        REALITY_BLOCK_ENTITY_CONTENTS = builder.define("copyBlockEntityContents", true);
        REALITY_MAX_REGION_VOLUME = builder.defineInRange("maxRegionVolume", 32768, 1, 1048576);
        builder.pop();
        builder.push("space");
        BANISH_DISTANCE = builder.defineInRange("banishDistance", 5000, 64, 1000000);
        builder.pop();
        builder.push("soul");
        SOUL_ALLY_CAP = builder.defineInRange("allyCap", 8, 0, 64);
        builder.pop();
        builder.push("time");
        REWIND_SECONDS = builder.defineInRange("rewindSeconds", 10, 2, 60);
        TIME_LOOP_COOLDOWN = builder.defineInRange("timeLoopCooldown", 6000, 0, 720000);
        builder.pop();
        builder.push("timeSurge");
        TIME_SURGE_TICK_MULTIPLIER = builder.defineInRange("tickMultiplier", 20, 1, 200);
        TIME_SURGE_CHUNK_RADIUS = builder.defineInRange("chunkRadius", 4, 1, 8);
        TIME_SURGE_DAY_SPEED = builder.defineInRange("daySpeed", 20, 0, 200);
        TIME_SURGE_BLOCK_ENTITY_TICKS = builder.defineInRange("blockEntityTicks", 8, 0, 64);
        builder.pop();
        SPEC = builder.build();
    }

    private GauntletConfig() {
    }

    private static volatile Map<String, List<Boolean>> syncedEnabled;
    private static volatile Boolean syncedInfinityTier;

    public static GauntletConfigSyncPayload syncPayload() {
        List<Boolean> flags = new ArrayList<>();
        for (String stone : GauntletAbilities.STONES) {
            for (ModConfigSpec.BooleanValue value : ABILITIES.get(stone)) flags.add(value.get());
        }
        return new GauntletConfigSyncPayload(flags, INFINITY_TIER.get());
    }

    public static void applySync(GauntletConfigSyncPayload payload) {
        Map<String, List<Boolean>> synced = new LinkedHashMap<>();
        int cursor = 0;
        for (String stone : GauntletAbilities.STONES) {
            List<Boolean> flags = new ArrayList<>();
            for (int index = 0; index < GauntletAbilities.count(stone); index++) {
                flags.add(cursor < payload.enabled().size() ? payload.enabled().get(cursor) : Boolean.TRUE);
                cursor++;
            }
            synced.put(stone, List.copyOf(flags));
        }
        syncedEnabled = synced;
        syncedInfinityTier = payload.infinityTier();
    }

    public static void clearSync() {
        syncedEnabled = null;
        syncedInfinityTier = null;
    }

    public static boolean isEnabled(String stone, int abilityIndex) {
        Map<String, List<Boolean>> synced = syncedEnabled;
        if (synced != null) {
            List<Boolean> flags = synced.get(stone);
            return flags != null && abilityIndex >= 0 && abilityIndex < flags.size() && flags.get(abilityIndex);
        }
        List<ModConfigSpec.BooleanValue> values = ABILITIES.get(stone);
        return values != null && abilityIndex >= 0 && abilityIndex < values.size()
                && values.get(abilityIndex).get();
    }

    public static boolean hasEnabledAbility(String stone) {
        Map<String, List<Boolean>> synced = syncedEnabled;
        if (synced != null) return synced.getOrDefault(stone, List.of()).contains(Boolean.TRUE);
        List<ModConfigSpec.BooleanValue> values = ABILITIES.get(stone);
        return values != null && values.stream().anyMatch(ModConfigSpec.BooleanValue::get);
    }

    public static int cooldown(String stone, int abilityIndex, int fallback) {
        List<ModConfigSpec.IntValue> values = COOLDOWNS.get(stone);
        return values != null && abilityIndex >= 0 && abilityIndex < values.size() ? values.get(abilityIndex).get() : fallback;
    }

    public static double powerMultiplier() {
        return POWER_MULTIPLIER.get();
    }

    public static boolean infinityTierEnabled() {
        Boolean synced = syncedInfinityTier;
        return synced != null ? synced : INFINITY_TIER.get();
    }

    public static boolean griefing() {
        return TERRAIN_GRIEFING.get();
    }

    public static boolean worldWide() {
        return WORLD_WIDE_EFFECTS.get();
    }
}
