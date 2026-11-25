package com.craisinlord.integrated_superheroes.compat.thematic;

import com.craisinlord.integrated_superheroes.hero.ServerState;
import bond.thematic.api.content.action.ContentAction;
import bond.thematic.api.registries.entity.AbstractVehicleEntity;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

final class BatmobileOrders {
    private static final ResourceLocation BATMOBILE = ResourceLocation.fromNamespaceAndPath("thematic", "batmobile");
    private static final ResourceLocation GUNFIRE = ResourceLocation.fromNamespaceAndPath("thematic", "ee3carbine_fire");
    private static final ResourceLocation REV = ResourceLocation.fromNamespaceAndPath("thematic", "ultron.sentinel_dash");
    private static final ResourceLocation CRASH = ResourceLocation.fromNamespaceAndPath("thematic", "spiderman_land_car");
    private static final double SEARCH = 160.0D;
    private static final int CALL_TIMEOUT = 600;
    private static final int ATTACK_TIMEOUT = 400;
    private static final int PASS_TICKS = 22;
    private static final int SHOT_GAP = 5;
    private static final double GUN_RANGE = 28.0D;
    private static final Map<UUID, Order> ORDERS = ServerState.map(new HashMap<>());

    private BatmobileOrders() {
    }

    private enum Stage { CALL, ATTACK, PASS }

    private static final class Order {
        private final UUID player;
        private final ServerLevel level;
        private UUID target;
        private Stage stage;
        private int ticks;
        private float ram;
        private float gun;
        private double push;
        private final Set<UUID> rammed = new HashSet<>();

        private Order(UUID player, ServerLevel level) {
            this.player = player;
            this.level = level;
        }
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(BatmobileOrders::onServerTick);
    }

    static ContentAction order(JsonObject json) {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player)) {
                return;
            }
            LivingEntity target = context.hitEntity() instanceof LivingEntity living && living != player && living.isAlive() ? living : null;
            AbstractVehicleEntity vehicle = find(player);
            if (vehicle == null) {
                vehicle = summon(player);
            }
            if (vehicle == null) {
                return;
            }
            Order order = new Order(player.getUUID(), player.serverLevel());
            order.ram = (float) BatmanActions.number(context, json, "ram", 40.0D);
            order.gun = (float) BatmanActions.number(context, json, "gun", 4.0D);
            order.push = BatmanActions.number(context, json, "push", 2.0D);
            vehicle.getEntityData().set(AbstractVehicleEntity.AUTONOMOUS, false);
            if (target != null) {
                order.stage = Stage.ATTACK;
                order.target = target.getUUID();
                vehicle.setPotentialOwner(target.getUUID());
            } else {
                order.stage = Stage.CALL;
                vehicle.setPotentialOwner(player.getUUID());
            }
            ORDERS.put(vehicle.getUUID(), order);
            order.level.playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(), BatmanActions.sound(REV, SoundEvents.FIREWORK_ROCKET_LAUNCH), SoundSource.NEUTRAL, 2.0F, 0.6F);
        };
    }

    private static AbstractVehicleEntity find(ServerPlayer player) {
        AbstractVehicleEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (AbstractVehicleEntity vehicle : player.serverLevel().getEntitiesOfClass(AbstractVehicleEntity.class,
                player.getBoundingBox().inflate(SEARCH, SEARCH * 0.4D, SEARCH),
                vehicle -> vehicle.isAlive() && !vehicle.isVehicle() && BATMOBILE.equals(BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType())))) {
            double distance = vehicle.distanceToSqr(player);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = vehicle;
            }
        }
        return best;
    }

    private static AbstractVehicleEntity summon(ServerPlayer player) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(BATMOBILE).orElse(null);
        if (type == null) {
            return null;
        }
        Entity created = type.create(player.serverLevel());
        if (!(created instanceof AbstractVehicleEntity vehicle)) {
            if (created != null) {
                created.discard();
            }
            return null;
        }
        return vehicle.callVehicleToPlayer(player) ? vehicle : null;
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (ORDERS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, Order>> iterator = ORDERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Order> entry = iterator.next();
            Order order = entry.getValue();
            Entity found = order.level.getEntity(entry.getKey());
            ServerPlayer player = server.getPlayerList().getPlayer(order.player);
            if (!(found instanceof AbstractVehicleEntity vehicle) || !vehicle.isAlive()) {
                iterator.remove();
                continue;
            }
            if (player == null || player.level() != order.level || vehicle.isVehicle()) {
                park(vehicle);
                iterator.remove();
                continue;
            }
            order.ticks++;
            if (order.ticks % 30 == 0 && vehicle.getVehicleSpeed() > 0.2F) {
                order.level.playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(), SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, 0.8F, 0.55F);
            }
            if (!step(order, vehicle, player)) {
                iterator.remove();
            }
        }
    }

    private static boolean step(Order order, AbstractVehicleEntity vehicle, ServerPlayer player) {
        double mount = vehicle.getConfig().mountingDistance;
        switch (order.stage) {
            case CALL -> {
                if (vehicle.distanceTo(player) <= mount + 3.0D || order.ticks > CALL_TIMEOUT) {
                    park(vehicle);
                    return false;
                }
                vehicle.setPotentialOwner(player.getUUID());
                return true;
            }
            case ATTACK -> {
                Entity found = order.level.getEntity(order.target);
                if (!(found instanceof LivingEntity target) || !target.isAlive() || order.ticks > ATTACK_TIMEOUT) {
                    home(order, vehicle, player);
                    return true;
                }
                vehicle.setPotentialOwner(target.getUUID());
                double distance = vehicle.distanceTo(target);
                if (distance <= GUN_RANGE && order.ticks % SHOT_GAP == 0 && vehicle.hasLineOfSight(target)) {
                    shoot(order, vehicle, player, target);
                }
                if (distance <= mount + 2.5D) {
                    order.stage = Stage.PASS;
                    order.ticks = 0;
                    vehicle.setPotentialOwner(null);
                    vehicle.setAutonomous();
                }
                return true;
            }
            case PASS -> {
                ram(order, vehicle, player);
                if (order.ticks > PASS_TICKS) {
                    vehicle.getEntityData().set(AbstractVehicleEntity.AUTONOMOUS, false);
                    home(order, vehicle, player);
                }
                return true;
            }
        }
        return false;
    }

    private static void home(Order order, AbstractVehicleEntity vehicle, ServerPlayer player) {
        order.stage = Stage.CALL;
        order.ticks = 0;
        order.target = null;
        vehicle.setPotentialOwner(player.getUUID());
    }

    private static void park(AbstractVehicleEntity vehicle) {
        vehicle.setPotentialOwner(null);
        vehicle.getEntityData().set(AbstractVehicleEntity.AUTONOMOUS, false);
        vehicle.getEntityData().set(AbstractVehicleEntity.IS_ACCELERATING, false);
        vehicle.setVehicleSpeed(0.0F);
        vehicle.setDeltaMovement(Vec3.ZERO);
    }

    private static Vec3 nose(AbstractVehicleEntity vehicle) {
        double yaw = Math.toRadians(vehicle.getYRot());
        return vehicle.position().add(-Math.sin(yaw) * 3.2D, 1.1D, Math.cos(yaw) * 3.2D);
    }

    private static void shoot(Order order, AbstractVehicleEntity vehicle, ServerPlayer player, LivingEntity target) {
        Vec3 from = nose(vehicle);
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
        ServerLevel level = order.level;
        level.sendParticles(ParticleTypes.FLASH, from.x, from.y, from.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 3, 0.1D, 0.1D, 0.1D, 0.02D);
        Vec3 step = to.subtract(from);
        int points = (int) Math.min(24, step.length() * 1.5D);
        for (int i = 1; i <= points; i++) {
            Vec3 at = from.add(step.scale(i / (double) points));
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.playSound(null, from.x, from.y, from.z, BatmanActions.sound(GUNFIRE, SoundEvents.CROSSBOW_SHOOT), SoundSource.PLAYERS, 1.2F, 0.9F + level.random.nextFloat() * 0.2F);
        if (!ThematicTargets.spared(player, target)) {
            DamageSource source = level.damageSources().mobProjectile(vehicle, player);
            target.hurt(source, order.gun);
        }
    }

    private static void ram(Order order, AbstractVehicleEntity vehicle, ServerPlayer player) {
        ServerLevel level = order.level;
        double yaw = Math.toRadians(vehicle.getYRot());
        Vec3 heading = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, vehicle.getBoundingBox().inflate(0.8D, 0.4D, 0.8D))) {
            if (victim == vehicle || order.rammed.contains(victim.getUUID()) || ThematicTargets.spared(player, victim)) {
                continue;
            }
            order.rammed.add(victim.getUUID());
            float damage = ThematicTargets.boss(victim) ? order.ram * 0.5F : order.ram;
            victim.hurt(level.damageSources().playerAttack(player), damage);
            double strength = order.push * (1.0D - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.6D);
            victim.setDeltaMovement(victim.getDeltaMovement().add(heading.scale(strength)).add(0.0D, 0.5D, 0.0D));
            victim.hurtMarked = true;
            victim.hasImpulse = true;
            level.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), BatmanActions.sound(CRASH, SoundEvents.GENERIC_EXPLODE.value()), SoundSource.PLAYERS, 1.6F, 1.0F);
        }
    }
}
