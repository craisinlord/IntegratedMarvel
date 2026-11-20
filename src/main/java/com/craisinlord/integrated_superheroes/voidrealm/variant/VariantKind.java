package com.craisinlord.integrated_superheroes.voidrealm.variant;

import java.util.Locale;

public enum VariantKind {
    THEMATIC,
    SERUM,
    MARVEL,
    VILTRUMITE,
    ENTITY;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static VariantKind byKey(String key) {
        for (VariantKind kind : values()) {
            if (kind.key().equalsIgnoreCase(key)) {
                return kind;
            }
        }
        return null;
    }
}
