package com.craisinlord.integrated_superheroes.hero;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MarvelWeaponConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue THROWN_MJOLNIR_DAMAGE;
    public static final ModConfigSpec.DoubleValue THROWN_STORMBREAKER_DAMAGE;
    public static final ModConfigSpec.DoubleValue VIBRANIUM_SHIELD_DAMAGE;
    public static final ModConfigSpec.DoubleValue PROTO_ADAMANTIUM_SHIELD_DAMAGE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Damage dealt by thrown Marvel weapons. -1 keeps the weapon's own damage.").push("thrown");
        THROWN_MJOLNIR_DAMAGE = builder.defineInRange("mjolnir", -1.0D, -1.0D, 100000.0D);
        THROWN_STORMBREAKER_DAMAGE = builder.defineInRange("stormbreaker", -1.0D, -1.0D, 100000.0D);
        builder.pop();
        builder.comment("Damage dealt by thrown shields. -1 keeps the shield's own damage.").push("shields");
        VIBRANIUM_SHIELD_DAMAGE = builder.defineInRange("vibranium", -1.0D, -1.0D, 100000.0D);
        PROTO_ADAMANTIUM_SHIELD_DAMAGE = builder.defineInRange("proto_adamantium", -1.0D, -1.0D, 100000.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private MarvelWeaponConfig() {
    }

    public static double or(ModConfigSpec.DoubleValue value, double fallback) {
        if (!SPEC.isLoaded()) {
            return fallback;
        }
        double configured = value.get();
        return configured < 0.0D ? fallback : configured;
    }
}
