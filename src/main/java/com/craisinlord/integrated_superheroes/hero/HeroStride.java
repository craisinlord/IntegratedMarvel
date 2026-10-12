package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HeroStride {
    private static final Map<UUID, Stride> STRIDE = new HashMap<>();

    private HeroStride() {
    }

    private record Stride(Vec3 last, double distance) {
    }

    public static void tick(ServerPlayer player, double length, SoundEvent sound, float volume, float pitch, int dust, float shake) {
        if (!player.onGround() || player.isCrouching() || player.isPassenger()) {
            STRIDE.remove(player.getUUID());
            return;
        }
        Stride previous = STRIDE.get(player.getUUID());
        Vec3 now = player.position();
        if (previous == null) {
            STRIDE.put(player.getUUID(), new Stride(now, 0.0D));
            return;
        }
        double dx = now.x - previous.last().x;
        double dz = now.z - previous.last().z;
        double stride = previous.distance() + Math.sqrt(dx * dx + dz * dz);
        if (stride < length) {
            STRIDE.put(player.getUUID(), new Stride(now, stride));
            return;
        }
        STRIDE.put(player.getUUID(), new Stride(now, 0.0D));
        ServerLevel level = player.serverLevel();
        BlockPos below = player.blockPosition().below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isAir() && dust > 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), player.getX(), player.getY() + 0.1D, player.getZ(),
                    dust, 0.5D, 0.05D, 0.5D, 0.15D);
        }
        HeroFx.play(player, sound, volume, pitch + player.getRandom().nextFloat() * 0.1F);
        if (shake > 0.0F) {
            HeroFx.shake(level, player.position(), 20.0D, shake, 6);
        }
    }

    public static void clear(ServerPlayer player) {
        STRIDE.remove(player.getUUID());
    }
}
