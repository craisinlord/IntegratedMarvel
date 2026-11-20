package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.content.ability.state.NumberSpec;
import bond.thematic.api.content.action.ActionContext;
import bond.thematic.api.content.action.Actions;
import bond.thematic.api.content.action.ContentAction;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.google.gson.JsonObject;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

public final class ApocalypseActions {
    private static final ResourceLocation SUIT = ResourceLocation.fromNamespaceAndPath("thematic", "apocalypse");
    private static final ResourceLocation MUTATION = IntegratedSuperheroes.id("apocalypse_mutation");
    private static final String GATE = "integrated_superheroes:apocalypse_";
    private static final String REGEN_AT = GATE + "regen_at";
    private static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.25F, 0.45F, 1.0F), 1.5F);
    private static final Map<UUID, Evolution> EVOLUTIONS = new HashMap<>();
    private static boolean registered;

    private static final class Evolution {
        ResourceKey<Level> dimension;
        String adaptedType = "";
        int adaptations;
        long adaptedUntil;
        long leapUntil;
        int leapAge;
        float leapDamage;
        double leapRadius;
        long mutationUntil;
        long mutationCooldown;
        double mutationMultiplier;
    }

    private ApocalypseActions() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        action("apocalypse_fist", ApocalypseActions::fist);
        action("apocalypse_leap", ApocalypseActions::leap);
        action("apocalypse_mutation", ApocalypseActions::mutation);
        action("apocalypse_end_mutation", json -> context -> {
            if (!context.isClient() && context.owner() instanceof ServerPlayer player) {
                endMutation(player, EVOLUTIONS.get(player.getUUID()));
            }
        });
        NeoForge.EVENT_BUS.addListener(ApocalypseActions::onTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ApocalypseActions::onDamage);
        NeoForge.EVENT_BUS.addListener(ApocalypseActions::onLogout);
        NeoForge.EVENT_BUS.addListener(ApocalypseActions::onDimension);
        NeoForge.EVENT_BUS.addListener(ApocalypseActions::onDeath);
        NeoForge.EVENT_BUS.addListener(ApocalypseActions::onClone);
        NeoForge.EVENT_BUS.addListener(ApocalypseActions::onStop);
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

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    private static boolean equipped(ServerPlayer player) {
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return player.isAlive() && !player.isSpectator() && BuiltInRegistries.ITEM.getKey(chest.getItem()).equals(SUIT)
                && SuitPieces.complete(player, chest.getItem());
    }

    private static Evolution evolution(ServerPlayer player) {
        Evolution evolution = EVOLUTIONS.get(player.getUUID());
        if (evolution == null || evolution.dimension != player.level().dimension()) {
            if (evolution != null) {
                endMutation(player, evolution);
            }
            evolution = new Evolution();
            evolution.dimension = player.level().dimension();
            EVOLUTIONS.put(player.getUUID(), evolution);
        }
        return evolution;
    }

    private static long cooldown(ActionContext context, double fallback) {
        return Math.max(1, Math.round(context.ability() == null ? fallback * 20 : context.ability().cooldown(context.owner())));
    }

    private static boolean ready(ServerPlayer player, String move, long ticks) {
        String key = GATE + move;
        if (player.getPersistentData().getLong(key) > now(player)) {
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.apocalypse_recharging"), true);
            return false;
        }
        player.getPersistentData().putLong(key, now(player) + ticks);
        return true;
    }

    private static ContentAction fist(JsonObject json) {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !equipped(player)
                    || !ready(player, "fist", cooldown(context, 5))) {
                return;
            }
            Evolution evolution = evolution(player);
            double radius = Math.clamp(number(context, json, "range", 9), 2, 12);
            float damage = (float) number(context, json, "damage", 110);
            if (evolution.mutationUntil > now(player)) {
                radius = Math.min(14, radius + 2);
                damage *= (float) evolution.mutationMultiplier;
            }
            Vec3 facing = Vec3.directionFromRotation(0, player.getYRot());
            for (LivingEntity target : targets(player, radius, 8, facing)) {
                if (target.hurt(player.serverLevel().damageSources().playerAttack(player), damage)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false), player);
                    shove(player, target, ThematicTargets.boss(target) ? 0.15 : 1.1);
                }
            }
            Vec3 hand = player.position().add(0, 1, 0);
            Vec3 end = hand.add(facing.scale(radius));
            for (int i = 0; i <= 20; i++) {
                Vec3 at = hand.lerp(end, i / 20.0);
                player.serverLevel().sendParticles(BLUE, at.x, at.y, at.z, 2, 0.16, 0.16, 0.16, 0);
            }
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.4F, 0.5F);
        };
    }

    private static ContentAction leap(JsonObject json) {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !equipped(player)
                    || player.isPassenger() || !ready(player, "leap", cooldown(context, 10))) {
                return;
            }
            Evolution evolution = evolution(player);
            evolution.leapUntil = now(player) + 100;
            evolution.leapAge = 0;
            evolution.leapDamage = (float) number(context, json, "damage", 100);
            evolution.leapRadius = Math.clamp(number(context, json, "range", 7), 2, 10);
            Vec3 forward = Vec3.directionFromRotation(0, player.getYRot());
            Vec3 movement = player.onGround() ? forward.scale(1.4).add(0, 0.95, 0) : forward.scale(0.65).add(0, -1.6, 0);
            player.setDeltaMovement(movement);
            player.fallDistance = 0;
            player.hurtMarked = true;
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.2F, 0.65F);
        };
    }

    private static ContentAction mutation(JsonObject json) {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !equipped(player)) {
                return;
            }
            long duration = Math.max(1, Math.round(number(context, json, "duration", 12) * 20));
            long recharge = cooldown(context, 110);
            if (!ready(player, "mutation", duration + recharge)) {
                return;
            }
            Evolution evolution = evolution(player);
            evolution.mutationUntil = now(player) + duration;
            evolution.mutationCooldown = recharge;
            evolution.mutationMultiplier = Math.clamp(number(context, json, "damage_multiplier", 1.65), 1, 2.5);
            modifier(player, Attributes.ATTACK_DAMAGE, 12, AttributeModifier.Operation.ADD_VALUE);
            modifier(player, Attributes.ENTITY_INTERACTION_RANGE, 2, AttributeModifier.Operation.ADD_VALUE);
            AABB body = player.getBoundingBox();
            Vec3 center = body.getCenter();
            AABB grown = new AABB(center.x - body.getXsize() * 0.6, body.minY, center.z - body.getZsize() * 0.6,
                    center.x + body.getXsize() * 0.6, body.minY + body.getYsize() * 1.2, center.z + body.getZsize() * 0.6);
            if (player.level().noCollision(player, grown)) {
                modifier(player, Attributes.SCALE, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            }
            impact(player, 6, 80);
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.apocalypse_evolved"), true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.5F, 0.65F);
        };
    }

    private static void modifier(ServerPlayer player, Holder<Attribute> attribute, double value, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(MUTATION);
            instance.addTransientModifier(new AttributeModifier(MUTATION, value, operation));
        }
    }

    private static void removeModifiers(ServerPlayer player) {
        for (Holder<Attribute> attribute : List.of(Attributes.ATTACK_DAMAGE, Attributes.ENTITY_INTERACTION_RANGE, Attributes.SCALE)) {
            var instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(MUTATION);
            }
        }
    }

    private static List<LivingEntity> targets(ServerPlayer player, double radius, int maximum) {
        return targets(player, radius, maximum, null);
    }

    private static List<LivingEntity> targets(ServerPlayer player, double radius, int maximum, Vec3 facing) {
        List<LivingEntity> targets = player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius, 5, radius), target -> !ThematicTargets.spared(player, target)
                        && target.distanceToSqr(player) <= radius * radius && player.hasLineOfSight(target)
                        && (facing == null || target.position().subtract(player.position()).multiply(1, 0, 1).normalize().dot(facing) >= 0.45));
        targets.sort(Comparator.comparingDouble(target -> target.distanceToSqr(player)));
        return targets.stream().limit(maximum).toList();
    }

    private static void shove(ServerPlayer player, LivingEntity target, double strength) {
        Vec3 direction = target.position().subtract(player.position()).multiply(1, 0, 1).normalize();
        target.setDeltaMovement(target.getDeltaMovement().add(direction.scale(strength)).add(0, strength * 0.25, 0));
        target.hurtMarked = true;
    }

    private static void impact(ServerPlayer player, double radius, float damage) {
        for (LivingEntity target : targets(player, radius, 10)) {
            if (target.hurt(player.serverLevel().damageSources().playerAttack(player), damage)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1, false, false), player);
                shove(player, target, ThematicTargets.boss(target) ? 0.15 : 1.4);
            }
        }
        for (int i = 0; i < 32; i++) {
            double angle = Math.PI * 2 * i / 32;
            player.serverLevel().sendParticles(BLUE, player.getX() + Math.cos(angle) * radius, player.getY() + 0.2,
                    player.getZ() + Math.sin(angle) * radius, 2, 0, 0.2, 0, 0);
        }
        player.serverLevel().sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2, player.getZ(), 2, 0.5, 0.1, 0.5, 0);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.5F);
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!equipped(player)) {
            clear(player);
            return;
        }
        Evolution evolution = evolution(player);
        long time = now(player);
        if (time >= evolution.adaptedUntil) {
            evolution.adaptations = 0;
            evolution.adaptedType = "";
        }
        if (evolution.leapUntil > time) {
            evolution.leapAge++;
            player.fallDistance = 0;
            if (evolution.leapAge > 3 && player.onGround()) {
                float damage = evolution.leapDamage;
                double radius = evolution.leapRadius;
                if (evolution.mutationUntil > time) {
                    damage *= (float) evolution.mutationMultiplier;
                    radius = Math.min(12, radius + 2);
                }
                evolution.leapUntil = 0;
                impact(player, radius, damage);
            }
        }
        if (evolution.mutationUntil > time) {
            if (player.tickCount % 20 == 0) {
                player.heal(2);
                player.serverLevel().sendParticles(BLUE, player.getX(), player.getY() + 1, player.getZ(), 10, 0.7, 0.8, 0.7, 0);
            }
        } else if (evolution.mutationUntil > 0) {
            endMutation(player, evolution);
        } else if (player.tickCount % 40 == 0 && time >= player.getPersistentData().getLong(REGEN_AT)) {
            player.heal(1);
        }
    }

    private static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !equipped(player) || event.getAmount() <= 0) {
            return;
        }
        Evolution evolution = evolution(player);
        if (event.getSource().is(DamageTypeTags.IS_FALL) && evolution.leapUntil > now(player)) {
            event.setCanceled(true);
            return;
        }
        player.getPersistentData().putLong(REGEN_AT, now(player) + 100);
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        String type = event.getSource().getMsgId();
        if (now(player) < evolution.adaptedUntil && evolution.adaptedType.equals(type)) {
            evolution.adaptations = Math.min(3, evolution.adaptations + 1);
        } else {
            evolution.adaptations = 0;
            evolution.adaptedType = type;
        }
        evolution.adaptedUntil = now(player) + 160;
        int adaptations = evolution.mutationUntil > now(player) ? 3 : evolution.adaptations;
        event.setAmount(event.getAmount() * (1 - adaptations * 0.12F));
    }

    private static void endMutation(ServerPlayer player, Evolution evolution) {
        removeModifiers(player);
        if (evolution != null && evolution.mutationUntil > 0) {
            player.getPersistentData().putLong(GATE + "mutation", now(player) + evolution.mutationCooldown);
            evolution.mutationUntil = 0;
        }
    }

    private static void clear(ServerPlayer player) {
        Evolution evolution = EVOLUTIONS.remove(player.getUUID());
        if (evolution != null) {
            endMutation(player, evolution);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    private static void onClone(PlayerEvent.Clone event) {
        for (String name : List.of("fist", "leap", "mutation", "regen_at")) {
            String key = GATE + name;
            event.getEntity().getPersistentData().putLong(key, event.getOriginal().getPersistentData().getLong(key));
        }
    }

    private static void onStop(ServerStoppedEvent event) {
        EVOLUTIONS.clear();
    }
}
