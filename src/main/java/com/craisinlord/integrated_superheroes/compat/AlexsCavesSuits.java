package com.craisinlord.integrated_superheroes.compat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.DepthChargeEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.NuclearExplosionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class AlexsCavesSuits {
    private AlexsCavesSuits() {
    }

    public static void depthCharge(ServerPlayer player) {
        DepthChargeEntity charge = new DepthChargeEntity(player.level(), player);
        charge.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.0F, 1.0F);
        player.level().addFreshEntity(charge);
    }

    public static boolean nuke(ServerLevel level, Entity source) {
        NuclearExplosionEntity explosion = ACEntityRegistry.NUCLEAR_EXPLOSION.get().create(level);
        if (explosion == null) {
            return false;
        }
        explosion.copyPosition(source);
        explosion.setSize(AlexsCaves.COMMON_CONFIG.nukeExplosionSizeModifier.get().floatValue());
        level.addFreshEntity(explosion);
        return true;
    }
}
