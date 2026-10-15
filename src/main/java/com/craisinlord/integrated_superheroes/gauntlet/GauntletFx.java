package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.anthology.shake.CameraShakeSync;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

public final class GauntletFx {
    private GauntletFx() {
    }

    public static DustParticleOptions dust(String stone, float scale) {
        int color = GauntletAbilities.stoneColor(stone);
        return new DustParticleOptions(new Vector3f((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F,
                (color & 255) / 255.0F), scale);
    }

    public static void burst(ServerLevel level, Vec3 at, String stone, int count, double spread) {
        level.sendParticles(dust(stone, 1.6F), at.x, at.y, at.z, count, spread, spread, spread, 0.05D);
    }

    public static void particles(ServerLevel level, ParticleOptions type, Vec3 at, int count, double spread, double speed) {
        level.sendParticles(type, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    public static void ring(ServerLevel level, Vec3 center, double radius, String stone) {
        int points = Math.clamp((int) (radius * 12.0D), 12, 240);
        shape(level, new GauntletShapePayload(dust(stone, 2.0F), GauntletShapePayload.RING, center.x, center.y + 0.2D, center.z,
                0.0D, 0.0D, 0.0D, (float) radius, points, 0.05F, 0.0F), center, radius);
    }

    public static void sphere(ServerLevel level, Vec3 center, double radius, String stone, int points) {
        shape(level, new GauntletShapePayload(dust(stone, 1.4F), GauntletShapePayload.SPHERE, center.x, center.y, center.z,
                0.0D, 0.0D, 0.0D, (float) radius, points, 0.0F, 0.0F), center, radius);
    }

    public static void box(ServerLevel level, ParticleOptions particle, Vec3 low, Vec3 high, int steps) {
        Vec3 center = low.add(high).scale(0.5D);
        shape(level, new GauntletShapePayload(particle, GauntletShapePayload.BOX, low.x, low.y, low.z, high.x, high.y, high.z,
                0.0F, steps, 0.0F, 0.0F), center, high.distanceTo(low) * 0.5D);
    }

    public static void line(ServerLevel level, ParticleOptions particle, Vec3 start, Vec3 end, int points, float spread) {
        Vec3 center = start.add(end).scale(0.5D);
        shape(level, new GauntletShapePayload(particle, GauntletShapePayload.LINE, start.x, start.y, start.z, end.x, end.y, end.z,
                0.0F, points, spread, 0.0F), center, end.distanceTo(start) * 0.5D);
    }

    public static void ellipse(ServerLevel level, ParticleOptions particle, Vec3 center, Vec3 horizontal, double vertical, int points,
                               double phase) {
        shape(level, new GauntletShapePayload(particle, GauntletShapePayload.ELLIPSE, center.x, center.y, center.z,
                horizontal.x, horizontal.y, horizontal.z, (float) vertical, points, 0.0F, (float) phase), center,
                Math.max(horizontal.length(), vertical));
    }

    private static void shape(ServerLevel level, GauntletShapePayload payload, Vec3 center, double reach) {
        double range = 32.0D + reach;
        double rangeSqr = range * range;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) <= rangeSqr) PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void cast(ServerPlayer player, String stone, SoundEvent sound, float pitch) {
        player.swing(InteractionHand.MAIN_HAND, true);
        Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.8D)).add(0.0D, -0.4D, 0.0D);
        burst(player.serverLevel(), hand, stone, 18, 0.25D);
        if (sound != null) sound(player.serverLevel(), player.position(), sound, 1.0F, pitch);
    }

    public static void shake(ServerLevel level, Vec3 at, float intensity, int duration, float radius) {
        CameraShakeSync.sendNearby(level, at, intensity, duration, radius);
    }
}
