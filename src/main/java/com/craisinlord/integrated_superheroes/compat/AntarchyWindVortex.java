package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.antarchy.content.AntarchyObjects;
import com.craisinlord.antarchy.content.entity.vortex.WindVortexEntity;
import com.craisinlord.antarchy.content.item.EyeOfTheStormItem;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class AntarchyWindVortex {
    private AntarchyWindVortex() {
    }

    public static void takeoff(ServerLevel level, LivingEntity owner) {
        WindVortexEntity vortex = WindVortexEntity.create(level, AntarchyObjects.WIND_VORTEX.get(), owner.position().subtract(0.0D, 1.0D, 0.0D), Vec3.ZERO, owner, false);
        vortex.setMode(WindVortexEntity.VortexMode.UPWARD);
        vortex.setAxis(Direction.UP);
        vortex.setVortexSize(5.0F, 1.5F);
        vortex.setVortexDurationTicks(25);
        level.addFreshEntity(vortex);
    }

    public static void gale(ServerLevel level, Vec3 origin, Vec3 direction, LivingEntity owner) {
        EyeOfTheStormItem.launchStormVortex(level, origin, direction, owner);
    }
}
