package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.content.ability.state.NumberSpec;
import bond.thematic.api.content.action.ActionContext;
import bond.thematic.api.content.action.Actions;
import bond.thematic.api.content.action.ContentAction;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.HeroTeleportSafety;
import com.google.gson.JsonObject;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

public final class DarkseidActions {
    private static final ResourceLocation SUIT = ResourceLocation.fromNamespaceAndPath("thematic", "darkseid");
    private static final String GATE = "integrated_superheroes:darkseid_";
    private static final DustParticleOptions OMEGA = new DustParticleOptions(new Vector3f(1.0F, 0.08F, 0.015F), 1.3F);
    private static final Map<UUID, Focus> FOCUS = new HashMap<>();
    private static boolean registered;

    private record Focus(UUID victim, ResourceKey<Level> dimension, long until) {
    }

    private DarkseidActions() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        action("darkseid_omega_beams", DarkseidActions::omegaBeams);
        action("darkseid_boom_tube", DarkseidActions::boomTube);
        action("darkseid_omega_sanction", DarkseidActions::omegaSanction);
        NeoForge.EVENT_BUS.addListener(DarkseidActions::onTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, DarkseidActions::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(DarkseidActions::onLogout);
        NeoForge.EVENT_BUS.addListener(DarkseidActions::onDimension);
        NeoForge.EVENT_BUS.addListener(DarkseidActions::onDeath);
        NeoForge.EVENT_BUS.addListener(DarkseidActions::onClone);
        NeoForge.EVENT_BUS.addListener(DarkseidActions::onStop);
    }

    private static void action(String name, Function<JsonObject, ContentAction> factory) {
        ResourceLocation id = IntegratedSuperheroes.id(name);
        if (!Actions.TYPES.has(id)) {
            Actions.TYPES.register(id, factory::apply);
        }
    }

    private static double number(ActionContext context, JsonObject json, String key, double fallback) {
        return NumberSpec.parse(json, key, NumberSpec.constant(fallback)).resolve(context.inputs());
    }

    private static boolean equipped(ServerPlayer player) {
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return player.isAlive() && !player.isSpectator() && BuiltInRegistries.ITEM.getKey(chest.getItem()).equals(SUIT)
                && SuitPieces.complete(player, chest.getItem());
    }

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    private static boolean ready(ServerPlayer player, ActionContext context, String move, double fallback) {
        String key = GATE + move;
        long time = now(player);
        if (player.getPersistentData().getLong(key) > time) {
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.darkseid_recharging"), true);
            return false;
        }
        double ticks = context.ability() == null ? fallback * 20 : context.ability().cooldown(player);
        player.getPersistentData().putLong(key, time + Math.max(1, Math.round(ticks)));
        return true;
    }

    private static boolean loadedPath(ServerLevel level, Vec3 from, Vec3 to) {
        return level.hasChunksAt((int) Math.floor(Math.min(from.x, to.x)) - 3,
                (int) Math.floor(Math.min(from.z, to.z)) - 3,
                (int) Math.floor(Math.max(from.x, to.x)) + 3,
                (int) Math.floor(Math.max(from.z, to.z)) + 3);
    }

    private static List<LivingEntity> aimedTargets(ServerPlayer player, double range, double cosine, int maximum) {
        Vec3 look = player.getLookAngle();
        List<LivingEntity> candidates = player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), target -> !ThematicTargets.spared(player, target)
                        && player.distanceToSqr(target) <= range * range
                        && target.getBoundingBox().getCenter().subtract(player.getEyePosition()).normalize().dot(look) >= cosine);
        candidates.sort(Comparator.<LivingEntity>comparingDouble(target -> -target.getBoundingBox().getCenter()
                .subtract(player.getEyePosition()).normalize().dot(look)).thenComparingDouble(player::distanceToSqr));
        return candidates.stream().limit(32).filter(target -> loadedPath(player.serverLevel(), player.getEyePosition(), target.getEyePosition())
                && player.hasLineOfSight(target)).limit(maximum).toList();
    }

    private static ContentAction omegaBeams(JsonObject json) {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !equipped(player)
                    || !ready(player, context, "beams", 5)) {
                return;
            }
            double range = Math.clamp(number(context, json, "range", 48), 1, 64);
            List<LivingEntity> targets = aimedTargets(player, range, 0.94, 1);
            if (targets.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.integrated_superheroes.darkseid_no_target"), true);
                return;
            }
            LivingEntity first = targets.getFirst();
            ServerLevel level = player.serverLevel();
            float damage = (float) number(context, json, "damage", 110);
            boolean hit = first.hurt(level.damageSources().indirectMagic(player, player), damage);
            eyeBeams(level, player, first.getBoundingBox().getCenter(), 22);
            if (hit) {
                FOCUS.put(player.getUUID(), new Focus(first.getUUID(), level.dimension(), now(player) + 240));
                first.addEffect(new MobEffectInstance(MobEffects.GLOWING, 240, 0, false, false), player);
            }
            double chainRange = Math.clamp(number(context, json, "chain_range", 8), 1, 12);
            List<LivingEntity> chained = level.getEntitiesOfClass(LivingEntity.class, first.getBoundingBox().inflate(chainRange),
                    target -> target != first && !ThematicTargets.spared(player, target)
                            && target.distanceToSqr(first) <= chainRange * chainRange);
            chained.sort(Comparator.comparingDouble(first::distanceToSqr));
            for (LivingEntity target : chained.stream().limit(16).filter(target -> loadedPath(level, first.getEyePosition(), target.getEyePosition())
                    && first.hasLineOfSight(target) && player.hasLineOfSight(target)).limit(2).toList()) {
                curve(level, first.getEyePosition(), target.getBoundingBox().getCenter(), 1, 16);
                target.hurt(level.damageSources().indirectMagic(player, player), damage * (float) number(context, json, "chain_share", 0.6));
            }
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1, 0.5F);
        };
    }

    private static ContentAction boomTube(JsonObject json) {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !equipped(player)
                    || !ready(player, context, "boom_tube", 14)) {
                return;
            }
            double range = Math.clamp(number(context, json, "range", 48), 2, 64);
            Vec3 start = player.getEyePosition();
            while (range > 2 && !loadedPath(player.serverLevel(), start, start.add(player.getLookAngle().scale(range)))) {
                range -= 2;
            }
            if (!loadedPath(player.serverLevel(), start, start.add(player.getLookAngle().scale(range)))) {
                player.displayClientMessage(Component.translatable("message.integrated_superheroes.darkseid_tube_failed"), true);
                return;
            }
            HeroTeleportSafety.Policy policy = new HeroTeleportSafety.Policy(range, 1, 2, false, false, 4, 10);
            var destination = HeroTeleportSafety.aimed(player, policy);
            if (destination.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.integrated_superheroes.darkseid_tube_failed"), true);
                return;
            }
            Vec3 from = player.position();
            if (!HeroTeleportSafety.move(player, destination.get(), player.getYRot(), player.getXRot(), policy)) {
                player.displayClientMessage(Component.translatable("message.integrated_superheroes.darkseid_tube_failed"), true);
                return;
            }
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0;
            ring(player.serverLevel(), from, 1.8, 24);
            ring(player.serverLevel(), player.position(), 2.2, 32);
            double radius = Math.clamp(number(context, json, "shock_radius", 6), 1, 10);
            List<LivingEntity> targets = player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(radius), target -> !ThematicTargets.spared(player, target)
                            && target.distanceToSqr(player) <= radius * radius && player.hasLineOfSight(target));
            targets.sort(Comparator.comparingDouble(player::distanceToSqr));
            for (LivingEntity target : targets.stream().limit(8).toList()) {
                if (target.hurt(player.serverLevel().damageSources().playerAttack(player), (float) number(context, json, "damage", 65))) {
                    target.knockback(1.4, player.getX() - target.getX(), player.getZ() - target.getZ());
                }
            }
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1, 0.7F);
        };
    }

    private static ContentAction omegaSanction(JsonObject json) {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !equipped(player)
                    || !ready(player, context, "sanction", 100)) {
                return;
            }
            double range = Math.clamp(number(context, json, "range", 32), 1, 48);
            int maximum = (int) Math.clamp(number(context, json, "max_targets", 6), 1, 8);
            List<LivingEntity> targets = aimedTargets(player, range, 0.5, maximum);
            if (targets.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.integrated_superheroes.darkseid_no_target"), true);
                return;
            }
            Focus focus = FOCUS.remove(player.getUUID());
            for (LivingEntity target : targets) {
                float damage = (float) number(context, json, "damage", 240);
                if (focus != null && focus.until() >= now(player) && focus.dimension() == player.level().dimension()
                        && focus.victim().equals(target.getUUID())) {
                    damage *= (float) number(context, json, "marked_multiplier", 1.5);
                }
                eyeBeams(player.serverLevel(), player, target.getBoundingBox().getCenter(), 18);
                if (target.hurt(player.serverLevel().damageSources().indirectMagic(player, player), damage)) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, false), player);
                }
            }
            ring(player.serverLevel(), player.position(), 3, 40);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.4F, 0.45F);
        };
    }

    private static void eyeBeams(ServerLevel level, ServerPlayer player, Vec3 end, int count) {
        Vec3 right = player.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(0.18);
        curve(level, player.getEyePosition().add(right), end, 1, count);
        curve(level, player.getEyePosition().subtract(right), end, -1, count);
    }

    private static void curve(ServerLevel level, Vec3 start, Vec3 end, double side, int count) {
        Vec3 sideways = end.subtract(start).cross(new Vec3(0, 1, 0)).normalize();
        for (int i = 0; i <= count; i++) {
            double fraction = i / (double) count;
            Vec3 point = start.lerp(end, fraction).add(sideways.scale(Math.sin(fraction * Math.PI * 2) * side * 0.8));
            level.sendParticles(OMEGA, point.x, point.y, point.z, 1, 0.025, 0.025, 0.025, 0);
        }
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            level.sendParticles(OMEGA, center.x + Math.cos(angle) * radius, center.y + 0.8,
                    center.z + Math.sin(angle) * radius, 1, 0, 0.3, 0, 0);
        }
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!equipped(player)) {
            FOCUS.remove(player.getUUID());
            return;
        }
        Focus focus = FOCUS.get(player.getUUID());
        if (focus != null && (focus.until() < now(player) || focus.dimension() != player.level().dimension())) {
            FOCUS.remove(player.getUUID());
        }
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 30, 0, false, false));
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && equipped(player) && event.getAmount() > 0
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) {
            event.setAmount(event.getAmount() * 0.85F);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        FOCUS.remove(event.getEntity().getUUID());
    }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        FOCUS.remove(event.getEntity().getUUID());
    }

    private static void onDeath(LivingDeathEvent event) {
        FOCUS.remove(event.getEntity().getUUID());
    }

    private static void onClone(PlayerEvent.Clone event) {
        for (String move : List.of("beams", "boom_tube", "sanction")) {
            String key = GATE + move;
            event.getEntity().getPersistentData().putLong(key, event.getOriginal().getPersistentData().getLong(key));
        }
    }

    private static void onStop(ServerStoppedEvent event) {
        FOCUS.clear();
    }
}
