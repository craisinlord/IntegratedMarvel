package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.antarchy.content.entity.MissileSquidEntity;
import com.craisinlord.antarchy.content.entity.OctopusBombEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class AntarchySeaLife {
    private static final ResourceLocation MISSILE_SQUID = ResourceLocation.fromNamespaceAndPath("antarchy", "missile_squid");
    private static final ResourceLocation OCTOPUS_BOMB = ResourceLocation.fromNamespaceAndPath("antarchy", "octopus_bomb");

    private AntarchySeaLife() {
    }

    public static Mob missileSquid(ServerLevel level, LivingEntity owner, Vec3 at, Vec3 velocity) {
        if (!(BuiltInRegistries.ENTITY_TYPE.get(MISSILE_SQUID).create(level) instanceof MissileSquidEntity squid)) {
            return null;
        }
        squid.moveTo(at.x, at.y, at.z, owner.getYRot(), owner.getXRot());
        squid.setCanFly(true);
        squid.setSpawnedByKraken(true);
        squid.launchAsProjectile(owner, velocity);
        return squid;
    }

    public static Mob octopus(ServerLevel level, Vec3 at, float yaw) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(OCTOPUS_BOMB);
        if (!(type.create(level) instanceof OctopusBombEntity octopus)) {
            return null;
        }
        octopus.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        octopus.setCanFly(true);
        octopus.setNoAi(true);
        octopus.setNoGravity(true);
        octopus.setInvulnerable(true);
        return octopus;
    }
}
