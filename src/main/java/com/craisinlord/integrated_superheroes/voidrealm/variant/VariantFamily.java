package com.craisinlord.integrated_superheroes.voidrealm.variant;

import java.util.List;

public record VariantFamily(VariantKind kind, String id, List<String> looks, int gate, VariantAlignment alignment,
                            double weight, boolean entity, String note) {
    public VariantFamily {
        looks = List.copyOf(looks);
        note = note == null ? "" : note;
    }

    public String key() {
        return kind.key() + ":" + id;
    }

    public double weightAt(int tier) {
        return gate > 0 && tier >= gate ? weight * 2.0D : weight;
    }
}
