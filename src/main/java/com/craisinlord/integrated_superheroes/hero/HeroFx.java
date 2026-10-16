package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

public final class HeroFx {
    private HeroFx() {
    }

    public static SoundEvent sound(String id, SoundEvent fallback) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(id));
        return sound == null ? fallback : sound;
    }

    public static net.minecraft.core.particles.ParticleOptions particle(String id, net.minecraft.core.particles.ParticleOptions fallback) {
        return BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(id)) instanceof net.minecraft.core.particles.SimpleParticleType type ? type : fallback;
    }

    public static Item item(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == null ? Items.BARRIER : item;
    }

    public static Item item(String id, Item fallback) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == null || item == Items.AIR ? fallback : item;
    }

    public static Optional<Holder.Reference<MobEffect>> effect(String id) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id));
    }

    public static void play(Entity entity, SoundEvent sound, float volume, float pitch) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void shake(ServerLevel level, Vec3 center, double radius, float intensity, int ticks) {
        for (ServerPlayer viewer : level.players()) {
            double distance = viewer.position().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            float scaled = (float) (intensity * (1.0D - distance / radius * 0.7D));
            if (scaled > 0.01F) {
                PacketDistributor.sendToPlayer(viewer, new ScreenShakePayload(scaled, ticks));
            }
        }
    }
}
