package com.craisinlord.integrated_superheroes.voidrealm.variant;

import java.util.Locale;

public enum VariantAlignment {
    HERO,
    VILLAIN;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean opposes(VariantAlignment other) {
        return other != null && other != this;
    }

    public static VariantAlignment byKey(String key) {
        return "villain".equalsIgnoreCase(key) ? VILLAIN : HERO;
    }
}
