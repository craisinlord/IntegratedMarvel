package com.craisinlord.integrated_marvel.villain;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tintankgames.marvel.world.entity.HydraAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class HydraAlarm {
    private static final Map<ResourceKey<Level>, List<Alarm>> ALARMS = new HashMap<>();

    private HydraAlarm() {
    }

    public static final class Alarm {
        final Vec3 center;
        final long started;
        long until;
        int spawned;

        Alarm(Vec3 center, long started) {
            this.center = center;
            this.started = started;
            this.until = started + 1200L;
        }
    }

    public static Alarm active(ServerLevel level, Vec3 at) {
        List<Alarm> alarms = ALARMS.get(level.dimension());
        if (alarms == null) {
            return null;
        }
        double radius = VillainConfig.ALARM_RADIUS.get();
        for (Alarm alarm : alarms) {
            if (alarm.center.distanceToSqr(at) <= radius * radius) {
                return alarm;
            }
        }
        return null;
    }

    public static void raise(ServerLevel level, Vec3 at, LivingEntity intruder) {
        long now = level.getGameTime();
        Alarm existing = active(level, at);
        if (existing != null) {
            existing.until = now + 1200L;
            return;
        }
        Alarm alarm = new Alarm(at, now);
        ALARMS.computeIfAbsent(level.dimension(), k -> new ArrayList<>()).add(alarm);
        double radius = VillainConfig.ALARM_RADIUS.get();
        HydraSquad.alert(level, at, radius, intruder);
        for (HydraAgent agent : level.getEntitiesOfClass(HydraAgent.class, new AABB(at, at).inflate(radius), LivingEntity::isAlive)) {
            agent.setTarget(intruder);
        }
        for (VillainBrain brain : Villains.brains()) {
            if (brain.mob.level() == level && brain.mob.position().distanceToSqr(at) <= radius * radius) {
                brain.alarm(level);
            }
        }
        VillainFx.line(level, at, radius, "integrated_marvel.villains.hydra.alarm", ChatFormatting.RED);
    }

    public static void tick(ServerLevel level) {
        List<Alarm> alarms = ALARMS.get(level.dimension());
        if (alarms == null || alarms.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Alarm> iterator = alarms.iterator();
        while (iterator.hasNext()) {
            Alarm alarm = iterator.next();
            if (now >= alarm.until) {
                iterator.remove();
                continue;
            }
            long age = now - alarm.started;
            if (age < 600L && age % 80L == 0L) {
                VillainFx.sound(level, alarm.center, SoundEvents.BELL_RESONATE, 4.0F, 0.6F);
                VillainFx.sound(level, alarm.center, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 3.0F, 0.5F);
            }
        }
    }
}
