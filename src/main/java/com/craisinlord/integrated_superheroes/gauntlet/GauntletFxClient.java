package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class GauntletFxClient {
    private GauntletFxClient() {
    }

    public static void spawn(GauntletShapePayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        RandomSource random = level.getRandom();
        int points = payload.points();
        switch (payload.shape()) {
            case GauntletShapePayload.RING -> {
                for (int index = 0; index < points; index++) {
                    double angle = Math.PI * 2.0D * index / points;
                    emit(level, random, payload, payload.ax() + Math.cos(angle) * payload.radius(), payload.ay(),
                            payload.az() + Math.sin(angle) * payload.radius());
                }
            }
            case GauntletShapePayload.SPHERE -> {
                for (int index = 0; index < points; index++) {
                    double x = random.nextGaussian();
                    double y = random.nextGaussian();
                    double z = random.nextGaussian();
                    double length = x * x + y * y + z * z;
                    if (length < 1.0E-4D) continue;
                    double scale = payload.radius() / Math.sqrt(length);
                    emit(level, random, payload, payload.ax() + x * scale, payload.ay() + y * scale, payload.az() + z * scale);
                }
            }
            case GauntletShapePayload.BOX -> {
                double spanX = payload.bx() - payload.ax();
                double spanY = payload.by() - payload.ay();
                double spanZ = payload.bz() - payload.az();
                for (int step = 0; step <= points; step++) {
                    double t = step / (double) Math.max(1, points);
                    emit(level, random, payload, payload.ax() + spanX * t, payload.ay(), payload.az());
                    emit(level, random, payload, payload.ax() + spanX * t, payload.by(), payload.bz());
                    emit(level, random, payload, payload.ax(), payload.ay() + spanY * t, payload.az());
                    emit(level, random, payload, payload.bx(), payload.ay() + spanY * t, payload.bz());
                    emit(level, random, payload, payload.ax(), payload.ay(), payload.az() + spanZ * t);
                    emit(level, random, payload, payload.bx(), payload.by(), payload.az() + spanZ * t);
                }
            }
            case GauntletShapePayload.LINE -> {
                for (int index = 0; index <= points; index++) {
                    double t = index / (double) Math.max(1, points);
                    emit(level, random, payload, payload.ax() + (payload.bx() - payload.ax()) * t,
                            payload.ay() + (payload.by() - payload.ay()) * t, payload.az() + (payload.bz() - payload.az()) * t);
                }
            }
            case GauntletShapePayload.ELLIPSE -> {
                for (int index = 0; index < points; index++) {
                    double angle = payload.phase() + index * (Math.PI * 2.0D / points);
                    double cos = Math.cos(angle);
                    emit(level, random, payload, payload.ax() + payload.bx() * cos, payload.ay() + Math.sin(angle) * payload.radius(),
                            payload.az() + payload.bz() * cos);
                }
            }
            default -> {
            }
        }
    }

    public static void snapDust(SnapDustPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        RandomSource random = level.getRandom();
        SoundSource[] sources = SoundSource.values();
        for (SnapDustPayload.Entry entry : payload.entries()) {
            double centerY = entry.y() + entry.height() * 0.5D;
            double spread = Math.max(0.25D, Math.min(0.8D, entry.width() * 0.55D));
            burst(level, random, ParticleTypes.ASH, entry.x(), centerY, entry.z(), 28, spread, entry.height() * 0.35D, spread, 0.035D);
            burst(level, random, ParticleTypes.CLOUD, entry.x(), centerY, entry.z(), 10, spread * 0.65D, entry.height() * 0.25D,
                    spread * 0.65D, 0.02D);
            if (entry.sound() >= 0 && entry.sound() < sources.length) {
                level.playLocalSound(Math.floor(entry.x()) + 0.5D, Math.floor(entry.y()) + 0.5D, Math.floor(entry.z()) + 0.5D,
                        SoundEvents.SOUL_ESCAPE.value(), sources[entry.sound()], 0.7F, 0.75F, false);
            }
        }
    }

    private static void burst(ClientLevel level, RandomSource random, ParticleOptions particle, double x, double y, double z, int count,
                              double spreadX, double spreadY, double spreadZ, double speed) {
        for (int index = 0; index < count; index++) {
            level.addParticle(particle, false, x + random.nextGaussian() * spreadX, y + random.nextGaussian() * spreadY,
                    z + random.nextGaussian() * spreadZ, random.nextGaussian() * speed, random.nextGaussian() * speed,
                    random.nextGaussian() * speed);
        }
    }

    private static void emit(ClientLevel level, RandomSource random, GauntletShapePayload payload, double x, double y, double z) {
        ParticleOptions particle = payload.particle();
        float spread = payload.spread();
        if (spread > 0.0F) {
            x += random.nextGaussian() * spread;
            y += random.nextGaussian() * spread;
            z += random.nextGaussian() * spread;
        }
        level.addParticle(particle, false, x, y, z, 0.0D, 0.0D, 0.0D);
    }
}
