package com.craisinlord.integrated_superheroes.villain;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class VillainWorld {
    private static final ResourceLocation STUCK_JUMP = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "villain_adhesive_jump");
    private static final Map<ResourceKey<Level>, List<Delayed>> DELAYED = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<Hazard>> HAZARDS = new HashMap<>();
    private static final Map<UUID, Long> SUPPRESSED = new HashMap<>();
    private static final Map<UUID, Long> GROUNDED = new HashMap<>();
    private static final Map<UUID, Long> STUCK = new HashMap<>();
    private static final List<ShieldReturn> SHIELDS = new ArrayList<>();

    private VillainWorld() {
    }

    public interface Hazard {
        boolean tick(ServerLevel level, long now);
    }

    private record Delayed(long at, Runnable action) {
    }

    private record ShieldReturn(ResourceKey<Level> level, UUID projectile, UUID owner, ItemStack stack, long expires) {
    }

    public static void later(ServerLevel level, int ticks, Runnable action) {
        DELAYED.computeIfAbsent(level.dimension(), k -> new ArrayList<>()).add(new Delayed(level.getGameTime() + ticks, action));
    }

    public static void hazard(ServerLevel level, Hazard hazard) {
        HAZARDS.computeIfAbsent(level.dimension(), k -> new ArrayList<>()).add(hazard);
    }

    public static void suppress(Player player, int ticks) {
        SUPPRESSED.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    public static boolean suppressed(Player player) {
        Long until = SUPPRESSED.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    public static void ground(Player player, int ticks) {
        GROUNDED.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    public static void trackShield(ServerLevel level, Entity projectile, UUID owner, ItemStack stack) {
        SHIELDS.add(new ShieldReturn(level.dimension(), projectile.getUUID(), owner, stack, level.getGameTime() + 400L));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        List<Delayed> delayed = DELAYED.get(level.dimension());
        if (delayed != null && !delayed.isEmpty()) {
            List<Delayed> due = new ArrayList<>();
            Iterator<Delayed> iterator = delayed.iterator();
            while (iterator.hasNext()) {
                Delayed entry = iterator.next();
                if (now >= entry.at()) {
                    due.add(entry);
                    iterator.remove();
                }
            }
            for (Delayed entry : due) {
                entry.action().run();
            }
        }
        List<Hazard> hazards = HAZARDS.get(level.dimension());
        if (hazards != null && !hazards.isEmpty()) {
            List<Hazard> copy = new ArrayList<>(hazards);
            for (Hazard hazard : copy) {
                if (!hazard.tick(level, now)) {
                    hazards.remove(hazard);
                }
            }
        }
        tickShields(level, now);
        if (now % 5L == 0L) {
            releaseStuck(level, now);
        }
    }

    private static void tickShields(ServerLevel level, long now) {
        Iterator<ShieldReturn> iterator = SHIELDS.iterator();
        while (iterator.hasNext()) {
            ShieldReturn entry = iterator.next();
            if (!entry.level().equals(level.dimension())) {
                continue;
            }
            Entity projectile = level.getEntity(entry.projectile());
            if (projectile != null && !projectile.isRemoved() && now < entry.expires()) {
                continue;
            }
            if (projectile != null && !projectile.isRemoved()) {
                projectile.discard();
            }
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(entry.owner());
            if (owner != null) {
                if (!owner.getInventory().add(entry.stack().copy())) {
                    owner.drop(entry.stack().copy(), false);
                }
            } else if (projectile != null) {
                projectile.spawnAtLocation(entry.stack().copy());
            }
            iterator.remove();
        }
    }

    public static void tickPlayer(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long grounded = GROUNDED.get(player.getUUID());
        if (grounded != null) {
            if (now >= grounded) {
                GROUNDED.remove(player.getUUID());
            } else if (!player.onGround()) {
                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(motion.x * 0.8D, Math.min(motion.y, -0.35D), motion.z * 0.8D);
                player.hurtMarked = true;
                if (now % 4L == 0L) {
                    ((ServerLevel) player.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY(1.0D), player.getZ(),
                            2, 0.3D, 0.3D, 0.3D, 0.05D);
                }
            }
        }
        Long suppressed = SUPPRESSED.get(player.getUUID());
        if (suppressed != null) {
            if (now >= suppressed) {
                SUPPRESSED.remove(player.getUUID());
            } else if (now % 6L == 0L) {
                ((ServerLevel) player.level()).sendParticles(new DustParticleOptions(VillainFx.PURPLE, 0.9F),
                        player.getX(), player.getY(1.0D), player.getZ(), 2, 0.3D, 0.4D, 0.3D, 0.0D);
            }
        }
    }

    public static void stick(LivingEntity entity, int ticks) {
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 3, false, true, true));
        AttributeInstance jump = entity.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null && !jump.hasModifier(STUCK_JUMP)) {
            jump.addTransientModifier(new AttributeModifier(STUCK_JUMP, -10.0D, AttributeModifier.Operation.ADD_VALUE));
        }
        STUCK.put(entity.getUUID(), entity.level().getGameTime() + ticks);
    }

    public static boolean stuck(LivingEntity entity) {
        Long until = STUCK.get(entity.getUUID());
        return until != null && entity.level().getGameTime() < until;
    }

    private static void releaseStuck(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, Long>> iterator = STUCK.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (now < entry.getValue()) {
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null) {
                if (now > entry.getValue() + 200L) {
                    iterator.remove();
                }
                continue;
            }
            if (entity instanceof LivingEntity living) {
                AttributeInstance jump = living.getAttribute(Attributes.JUMP_STRENGTH);
                if (jump != null) {
                    jump.removeModifier(STUCK_JUMP);
                }
            }
            iterator.remove();
        }
    }

    public static List<LivingEntity> victims(ServerLevel level, Vec3 center, double radius, double height, Predicate<LivingEntity> filter) {
        AABB box = new AABB(center.x - radius, center.y - 0.5D, center.z - radius, center.x + radius, center.y + height, center.z + radius);
        return level.getEntitiesOfClass(LivingEntity.class, box, entity -> entity.isAlive()
                && entity.position().subtract(center).horizontalDistanceSqr() <= radius * radius && filter.test(entity));
    }

    public static void notice(ServerPlayer player, String key) {
        VillainFx.line(player, key, ChatFormatting.LIGHT_PURPLE);
    }
}
