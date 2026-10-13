package com.craisinlord.integrated_superheroes.villain;

import com.craisinlord.integrated_superheroes.hero.HeroFx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tintankgames.marvel.core.particles.MarvelParticleTypes;
import net.tintankgames.marvel.core.particles.SpaceStoneParticleOptions;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VillainFx {
    public static final Vector3f PURPLE = new Vector3f(0.55F, 0.2F, 0.75F);
    public static final Vector3f WHITE = new Vector3f(0.9F, 0.95F, 1.0F);
    public static final Vector3f RED = new Vector3f(0.85F, 0.08F, 0.08F);
    public static final Vector3f CYAN = new Vector3f(0.25F, 0.85F, 1.0F);
    public static final Vector3f ORANGE = new Vector3f(1.0F, 0.55F, 0.1F);
    private static final Map<Class<?>, Field> BOSS_EVENT_FIELDS = new ConcurrentHashMap<>();

    private VillainFx() {
    }

    public static List<ServerPlayer> near(ServerLevel level, Vec3 center, double radius) {
        return level.getPlayers(player -> player.isAlive() && !player.isSpectator() && player.position().distanceToSqr(center) <= radius * radius);
    }

    public static void title(ServerLevel level, Vec3 center, double radius, Component title, Component subtitle) {
        for (ServerPlayer player : near(level, center, radius)) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
    }

    public static void line(ServerLevel level, Vec3 center, double radius, String key, ChatFormatting color) {
        Component text = Component.translatable(key).withStyle(color);
        for (ServerPlayer player : near(level, center, radius)) {
            player.displayClientMessage(text, true);
        }
    }

    public static void line(ServerPlayer player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), true);
    }

    public static void sound(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    public static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    public static void soundTo(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.playNotifySound(sound, SoundSource.HOSTILE, volume, pitch);
    }

    public static void glint(ServerLevel level, LivingEntity mob) {
        Vec3 look = mob.getLookAngle();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize().scale(0.4D);
        Vec3 hand = mob.position().add(0.0D, mob.getBbHeight() * 0.6D, 0.0D).add(side).add(look.scale(0.3D));
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, hand.x, hand.y, hand.z, 4, 0.08D, 0.08D, 0.08D, 0.02D);
    }

    public static void ring(ServerLevel level, Vec3 center, double radius, Vector3f color) {
        int points = Math.max(12, (int) (radius * 10.0D));
        DustParticleOptions dust = new DustParticleOptions(color, 1.2F);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            level.sendParticles(dust, center.x + Math.cos(angle) * radius, center.y + 0.1D, center.z + Math.sin(angle) * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    public static void disc(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int count) {
        level.sendParticles(particle, center.x, center.y + 0.2D, center.z, count, radius * 0.5D, 0.1D, radius * 0.5D, 0.01D);
    }

    public static void lineBetween(ServerLevel level, Vec3 from, Vec3 to, Vector3f color) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.01D) {
            return;
        }
        Vec3 step = delta.scale(0.5D / length);
        DustParticleOptions dust = new DustParticleOptions(color, 0.6F);
        Vec3 point = from;
        for (double d = 0.0D; d < length; d += 0.5D) {
            level.sendParticles(dust, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            point = point.add(step);
        }
    }

    public static void smoke(ServerLevel level, Vec3 at, double radius) {
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 1.0D, at.z, (int) (radius * 14), radius * 0.5D, 0.8D, radius * 0.5D, 0.02D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.0D, at.z, (int) (radius * 10), radius * 0.4D, 0.6D, radius * 0.4D, 0.02D);
    }

    public static void sparks(ServerLevel level, Vec3 at, int count) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, count, 0.3D, 0.3D, 0.3D, 0.2D);
    }

    public static void spaceBurst(ServerLevel level, Vec3 at, float scale, float yaw, boolean reverse) {
        SpaceStoneParticleOptions options = new SpaceStoneParticleOptions(
                reverse ? MarvelParticleTypes.REVERSE_SPACE_STONE_EMITTER.get() : MarvelParticleTypes.SPACE_STONE_EMITTER.get(), scale, yaw);
        level.sendParticles(options, at.x, at.y, at.z, 0, 0.0D, 0.0D, 0.0D, 1.0D);
    }

    public static void tink(ServerLevel level, LivingEntity mob) {
        level.sendParticles(ParticleTypes.CRIT, mob.getX(), mob.getY(0.6D), mob.getZ(), 6, 0.3D, 0.3D, 0.3D, 0.1D);
        sound(mob, HeroFx.sound("minecraft:block.anvil.land", net.minecraft.sounds.SoundEvents.ANVIL_LAND), 0.3F, 2.0F);
    }

    public static void bossBarName(Entity mob, Component name) {
        ServerBossEvent event = bossEvent(mob);
        if (event != null) {
            event.setName(name);
        }
    }

    public static void bossBarVisible(Entity mob, boolean visible) {
        ServerBossEvent event = bossEvent(mob);
        if (event != null) {
            event.setVisible(visible);
        }
    }

    public static boolean hasBossBar(Entity mob) {
        return bossEvent(mob) != null;
    }

    private static ServerBossEvent bossEvent(Entity mob) {
        Field field = BOSS_EVENT_FIELDS.computeIfAbsent(mob.getClass(), VillainFx::findField);
        if (field == null) {
            return null;
        }
        try {
            return (ServerBossEvent) field.get(mob);
        } catch (IllegalAccessException | ClassCastException e) {
            return null;
        }
    }

    private static Field findField(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField("bossEvent");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
