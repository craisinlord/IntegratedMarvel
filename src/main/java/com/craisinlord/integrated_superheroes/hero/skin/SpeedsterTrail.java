package com.craisinlord.integrated_superheroes.hero.skin;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class SpeedsterTrail {
    private SpeedsterTrail() {
    }

    public static boolean emit(ServerPlayer player, ServerLevel level, Vec3 behind) {
        Optional<SerumSkin> skin = SerumSkins.current(player);
        if (skin.isEmpty()) {
            return false;
        }
        RandomSource random = player.getRandom();
        Vec3 back = player.getViewVector(0.0F).multiply(-1.0D, 0.0D, -1.0D).normalize();
        Vec3 point = behind.add(0.0D, 0.4D + random.nextDouble() * 1.0D, 0.0D);
        for (int i = 0; i < 6; i++) {
            Vec3 next = point.add(back.scale(0.35D)).add((random.nextDouble() - 0.5D) * 0.5D, (random.nextDouble() - 0.5D) * 0.5D,
                    (random.nextDouble() - 0.5D) * 0.5D);
            HeroAbilities.particleLine(level, point, next, HeroAbilities.emissive(skin.get().primary(), 0.7F), 0.12D);
            point = next;
        }
        level.sendParticles(HeroAbilities.emissive(skin.get().secondary(), 0.5F), behind.x, behind.y + 1.0D, behind.z, 4, 0.3D, 0.5D, 0.3D, 0.0D);
        return true;
    }
}
