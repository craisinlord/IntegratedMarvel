package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class HeroStun {
    public static final String STUN_TAG = "integrated_superheroes_stunned";
    public static final String SINK_TAG = "integrated_superheroes_sunk";
    private static final Map<ResourceKey<Level>, Map<UUID, Stun>> STUNNED = new HashMap<>();

    private HeroStun() {
    }

    private record Stun(long until, double riseTo) {
    }

    public static void stun(LivingEntity target, int ticks) {
        stun(target, ticks, Double.NaN);
    }

    public static void stun(LivingEntity target, int ticks, double riseTo) {
        if (!(target.level() instanceof ServerLevel level) || ticks <= 0) {
            return;
        }
        if (target instanceof Mob mob) {
            if (!mob.isNoAi() || mob.getTags().contains(STUN_TAG)) {
                mob.setNoAi(true);
                mob.addTag(STUN_TAG);
            }
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2, false, true, true));
        }
        if (!Double.isNaN(riseTo)) {
            target.addTag(SINK_TAG);
        }
        Map<UUID, Stun> stunned = STUNNED.computeIfAbsent(level.dimension(), ignored -> new HashMap<>());
        Stun previous = stunned.get(target.getUUID());
        double rise = !Double.isNaN(riseTo) ? riseTo : previous != null ? previous.riseTo() : Double.NaN;
        stunned.put(target.getUUID(), new Stun(level.getGameTime() + ticks, rise));
    }

    public static void tick(ServerLevel level) {
        Map<UUID, Stun> stunned = STUNNED.get(level.dimension());
        if (stunned == null || stunned.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Stun>> iterator = stunned.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Stun> entry = iterator.next();
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null) {
                if (now > entry.getValue().until() + 200L) {
                    iterator.remove();
                }
                continue;
            }
            if (now < entry.getValue().until()) {
                continue;
            }
            release(entity, entry.getValue().riseTo());
            iterator.remove();
        }
    }

    private static void release(Entity entity, double riseTo) {
        if (entity instanceof Mob mob && mob.getTags().contains(STUN_TAG)) {
            mob.setNoAi(false);
        }
        entity.removeTag(STUN_TAG);
        if (entity.getTags().contains(SINK_TAG)) {
            entity.removeTag(SINK_TAG);
            double y = Double.isNaN(riseTo) ? entity.getY() + 1.0D : riseTo;
            entity.teleportTo(entity.getX(), y + 0.05D, entity.getZ());
        }
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && !event.getLevel().isClientSide
                && (event.getEntity().getTags().contains(STUN_TAG) || event.getEntity().getTags().contains(SINK_TAG))) {
            release(event.getEntity(), Double.NaN);
        }
    }
}
