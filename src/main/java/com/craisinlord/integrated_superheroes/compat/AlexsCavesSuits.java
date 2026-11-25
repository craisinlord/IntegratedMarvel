package com.craisinlord.integrated_superheroes.compat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.DepthChargeEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.NuclearExplosionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class AlexsCavesSuits {
    private AlexsCavesSuits() {
    }

    public static Entity depthCharge(LivingEntity caster) {
        DepthChargeEntity charge = new DepthChargeEntity(caster.level(), caster);
        charge.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0F, 1.0F, 1.0F);
        caster.level().addFreshEntity(charge);
        return charge;
    }

    public static boolean nuke(ServerLevel level, Entity source, boolean griefing) {
        NuclearExplosionEntity explosion = ACEntityRegistry.NUCLEAR_EXPLOSION.get().create(level);
        if (explosion == null) {
            return false;
        }
        explosion.copyPosition(source);
        explosion.setSize(AlexsCaves.COMMON_CONFIG.nukeExplosionSizeModifier.get().floatValue());
        if (!griefing) {
            explosion.setNoGriefing(true);
        }
        level.addFreshEntity(explosion);
        return true;
    }
}
