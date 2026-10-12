package com.craisinlord.integrated_superheroes.compat;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;

public final class AlexsCavesMovingBlocks {
    private static final Set<Entity> LIVE = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private static final Class<?> MOVING_BLOCK_CLASS = find();

    private AlexsCavesMovingBlocks() {
    }

    private static Class<?> find() {
        try {
            return Class.forName("com.github.alexmodguy.alexscaves.server.entity.item.AbstractMovingBlockEntity", false,
                    AlexsCavesMovingBlocks.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError exception) {
            return null;
        }
    }

    public static void onLevelCallback(Entity entity, boolean added) {
        if (MOVING_BLOCK_CLASS == null || !MOVING_BLOCK_CLASS.isInstance(entity)) {
            return;
        }
        if (added) {
            LIVE.add(entity);
        } else {
            LIVE.remove(entity);
        }
    }

    public static boolean anyExist() {
        return MOVING_BLOCK_CLASS == null || !LIVE.isEmpty();
    }
}
