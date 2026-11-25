package com.craisinlord.integrated_superheroes.hero;

import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ServerState {
    private static final List<Runnable> CLEARS = new CopyOnWriteArrayList<>();

    private ServerState() {
    }

    public static <K, V> Map<K, V> map(Map<K, V> map) {
        CLEARS.add(map::clear);
        return map;
    }

    public static <T, C extends Collection<T>> C all(C collection) {
        CLEARS.add(collection::clear);
        return collection;
    }

    public static void onStop(Runnable clear) {
        CLEARS.add(clear);
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        CLEARS.forEach(Runnable::run);
    }
}
