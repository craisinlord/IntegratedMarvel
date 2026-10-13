package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.antarchy.content.AntarchySoundEvents;
import com.craisinlord.antarchy.content.entity.portal.DimensionalTearEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class AntarchyDimensionalTear {
    private AntarchyDimensionalTear() {
    }

    public static Entity create(ServerLevel level, Vec3 center, float yaw, int lifetimeTicks) {
        return DimensionalTearEntity.createGauntletVisual(level, center, yaw, lifetimeTicks);
    }

    public static boolean isTear(Entity entity) {
        return entity instanceof DimensionalTearEntity;
    }

    public static SoundEvent idleSound() {
        return AntarchySoundEvents.DIMENSIONAL_TEAR_IDLE.get();
    }
}
