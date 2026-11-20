package com.craisinlord.integrated_superheroes.hero.skin;

import java.util.Set;

public record SerumSkin(String id, String powerId, String nameKey, int primary, int secondary, Set<String> traits, String costume) {
    public SerumSkin {
        traits = Set.copyOf(traits);
        costume = costume == null ? "" : costume;
    }

    public SerumSkin(String id, String powerId, String nameKey, int primary, int secondary, Set<String> traits) {
        this(id, powerId, nameKey, primary, secondary, traits, SerumCostumes.forSkin(id));
    }

    public boolean has(String trait) {
        return this.traits.contains(trait);
    }
}
