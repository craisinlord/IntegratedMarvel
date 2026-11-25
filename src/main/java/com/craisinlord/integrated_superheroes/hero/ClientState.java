package com.craisinlord.integrated_superheroes.hero;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ClientState {
    private static final List<Runnable> CLEARS = new CopyOnWriteArrayList<>();

    private ClientState() {
    }

    public static <K, V> Map<K, V> map(Map<K, V> map) {
        CLEARS.add(map::clear);
        return map;
    }

    public static void clear() {
        CLEARS.forEach(Runnable::run);
    }
}
