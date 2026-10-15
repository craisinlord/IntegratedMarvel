package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class GauntletEntityLocks {
    private static final String TAG = "integrated_superheroes_lock";

    private GauntletEntityLocks() {
    }

    public static void remember(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.contains(TAG)) return;
        CompoundTag lock = new CompoundTag();
        lock.putBoolean("NoAi", entity instanceof Mob mob && mob.isNoAi());
        lock.putBoolean("Invulnerable", entity.isInvulnerable());
        lock.putBoolean("NoGravity", entity.isNoGravity());
        data.put(TAG, lock);
    }

    public static void forget(Entity entity) {
        entity.getPersistentData().remove(TAG);
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG)) return;
        CompoundTag lock = data.getCompound(TAG);
        if (entity instanceof Mob mob) mob.setNoAi(lock.getBoolean("NoAi"));
        entity.setInvulnerable(lock.getBoolean("Invulnerable"));
        entity.setNoGravity(lock.getBoolean("NoGravity"));
        data.remove(TAG);
    }
}
