package com.craisinlord.integrated_superheroes.hero.skin;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SerumSkins {
    private static final Map<String, SerumSkin> SKINS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private SerumSkins() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        ExistingSerumSkins.registerAll();
        NewSerumSkins.registerAll();
    }

    public static SerumSkin register(SerumSkin skin) {
        if (SKINS.containsKey(skin.id())) {
            throw new IllegalStateException("Duplicate serum skin " + skin.id());
        }
        SKINS.put(skin.id(), skin);
        return skin;
    }

    public static Collection<SerumSkin> all() {
        return Collections.unmodifiableCollection(SKINS.values());
    }

    public static List<SerumSkin> forPower(String powerId) {
        List<SerumSkin> result = new ArrayList<>();
        for (SerumSkin skin : SKINS.values()) {
            if (skin.powerId().equals(powerId)) {
                result.add(skin);
            }
        }
        return result;
    }

    public static Optional<SerumSkin> byId(String id) {
        return id == null || id.isEmpty() ? Optional.empty() : Optional.ofNullable(SKINS.get(id));
    }

    public static Optional<SerumSkin> current(Player player) {
        if (player == null || !SkinData.enabled()) {
            return Optional.empty();
        }
        return byId(SkinData.get(player));
    }

    public static int primary(Player player, int fallback) {
        return current(player).map(SerumSkin::primary).orElse(fallback);
    }

    public static int secondary(Player player, int fallback) {
        return current(player).map(SerumSkin::secondary).orElse(fallback);
    }

    public static boolean has(Player player, String trait) {
        return current(player).map(skin -> skin.has(trait)).orElse(false);
    }
}
