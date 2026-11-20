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
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

public final class DoomActions {
    private static final ResourceLocation SUIT = ResourceLocation.fromNamespaceAndPath("thematic", "dr_doom");
    private static final String GATE = "integrated_superheroes:doom_";
    private static final String REPAIR_AT = GATE + "repair_at";
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.15F, 1.0F, 0.25F), 1.2F);
    private static final Map<UUID, Systems> SYSTEMS = new HashMap<>();
    private static boolean registered;

    private static final class Systems {
        ResourceKey<Level> dimension;
        Vec3 sigil;
        long sigilUntil;
        float sigilDamage;
        double sigilRadius;
        Vec3 anchor;
        float yaw;
        float pitch;
        long wardUntil;
        float shield;
        float projectileCost;
        long wardCooldown;
        long ascendantUntil;
        float stormDamage;
        double stormRadius;
        int stormTargets;
        double beamMultiplier;
        long ascendantCooldown;
    }

    private DoomActions() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        action("doom_gauntlet", DoomActions::gauntlet);
        action("doom_aegis", DoomActions::aegis);
        action("doom_recall", json -> DoomActions::recall);
        action("doom_end_aegis", json -> context -> {
            if (!context.isClient() && context.owner() != null) {
                Systems systems = SYSTEMS.get(context.owner().getUUID());
                if (systems != null) {
                    if (context.owner() instanceof ServerPlayer player && systems.wardUntil > 0) {
                        player.getPersistentData().putLong(GATE + "aegis", now(player) + systems.wardCooldown);
                    }
                    systems.anchor = null;
                    systems.shield = 0;
                    systems.wardUntil = 0;
                }
            }
        });
        action("doom_ascendant", DoomActions::ascendant);
        action("doom_end_ascendant", json -> context -> {
            if (!context.isClient() && context.owner() != null) {
                Systems systems = SYSTEMS.get(context.owner().getUUID());
                if (systems != null) {
                    if (context.owner() instanceof ServerPlayer player && systems.ascendantUntil > 0) {
                        player.getPersistentData().putLong(GATE + "ascendant", now(player) + systems.ascendantCooldown);
                    }
                    systems.ascendantUntil = 0;
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(DoomActions::onTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, DoomActions::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(DoomActions::onDamageDealt);
        NeoForge.EVENT_BUS.addListener(DoomActions::onLogout);
        NeoForge.EVENT_BUS.addListener(DoomActions::onDimension);
        NeoForge.EVENT_BUS.addListener(DoomActions::onDeath);
        NeoForge.EVENT_BUS.addListener(DoomActions::onClone);
        NeoForge.EVENT_BUS.addListener(DoomActions::onStop);
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

    private static double cooldown(ActionContext context, double fallback) {
        return context.ability() == null ? fallback : context.ability().cooldown(context.owner()) / 20.0;
    }

    private static boolean equipped(ServerPlayer player) {
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(chest.getItem()).equals(SUIT)
                && player.isAlive() && !player.isSpectator() && SuitPieces.complete(player, chest.getItem());
    }

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    private static Systems systems(ServerPlayer player) {
        Systems current = SYSTEMS.get(player.getUUID());
        if (current == null || current.dimension != player.level().dimension()) {
            current = new Systems();
            current.dimension = player.level().dimension();
            SYSTEMS.put(player.getUUID(), current);
        }
        return current;
    }

    private static boolean ready(ServerPlayer player, String move, double seconds) {
        String key = GATE + move;
        long time = now(player);
        if (player.getPersistentData().getLong(key) > time) {
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.doom_recharging"), true);
            return false;
        }
        player.getPersistentData().putLong(key, time + Math.max(1, Math.round(seconds * 20)));
        return true;
    }

    private static void inCombat(ServerPlayer player) {
        player.getPersistentData().putLong(REPAIR_AT, now(player) + 200);
    }

    private static ContentAction gauntlet(JsonObject json) {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !equipped(player)
                    || !ready(player, "gauntlet", cooldown(context, 4))) {
                return;
            }
            ServerLevel level = player.serverLevel();
            Systems systems = systems(player);
            Vec3 start = player.getEyePosition();
            double range = Math.clamp(number(context, json, "range", 48), 1, 64);
            Vec3 end = start.add(player.getLookAngle().scale(range));
            HitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (block.getType() != HitResult.Type.MISS) {
                end = block.getLocation();
            }
            Vec3 finish = end;
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1.2),
                    target -> !ThematicTargets.spared(player, target) && player.hasLineOfSight(target)
                            && target.getBoundingBox().inflate(0.45).clip(start, finish).isPresent());
            targets.sort(Comparator.comparingDouble(target -> target.distanceToSqr(player)));
            float damage = (float) number(context, json, "damage", 80);
            if (systems.ascendantUntil > now(player)) {
                damage *= (float) systems.beamMultiplier;
            }
            Vec3 impact = targets.isEmpty() ? end : targets.getFirst().position();
            for (LivingEntity target : targets.stream().limit(3).toList()) {
                target.hurt(level.damageSources().playerAttack(player), damage);
                level.sendParticles(GREEN, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.5, 0.3, 0);
            }
            beam(level, start, end, 48);
            systems.sigil = impact;
            systems.sigilUntil = now(player) + Math.round(number(context, json, "sigil_duration", 6) * 20);
            systems.sigilDamage = (float) number(context, json, "sigil_damage", 12);
            systems.sigilRadius = Math.clamp(number(context, json, "sigil_radius", 4), 1, 8);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9F, 0.6F);
            inCombat(player);
        };
    }

    private static ContentAction aegis(JsonObject json) {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !equipped(player)
                    || !ready(player, "aegis", cooldown(context, 25) + number(context, json, "duration", 8))) {
                return;
            }
            Systems systems = systems(player);
            systems.anchor = HeroTeleportSafety.canStart(player) && HeroTeleportSafety.fits(player, player.position(), false)
                    ? player.position() : null;
            systems.yaw = player.getYRot();
            systems.pitch = player.getXRot();
            systems.wardUntil = now(player) + Math.round(number(context, json, "duration", 8) * 20);
            systems.shield = (float) Math.clamp(number(context, json, "shield", 180), 1, 1000);
            systems.projectileCost = (float) Math.max(1, number(context, json, "projectile_cost", 8));
            systems.wardCooldown = Math.round(cooldown(context, 25) * 20);
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.doom_aegis"), true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1, 0.7F);
        };
    }

    private static void recall(ActionContext context) {
        if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !equipped(player)) {
            return;
        }
        Systems systems = SYSTEMS.get(player.getUUID());
        if (systems == null || systems.dimension != player.level().dimension() || systems.anchor == null
                || systems.wardUntil < now(player) || systems.anchor.distanceToSqr(player.position()) > 128 * 128
                || !HeroTeleportSafety.fits(player, systems.anchor, false)) {
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.doom_recall_failed"), true);
            return;
        }
        HeroTeleportSafety.Policy policy = new HeroTeleportSafety.Policy(128, 0, 0, true, false, 0, 10);
        Vec3 from = player.position();
        if (!HeroTeleportSafety.move(player, systems.anchor, systems.yaw, systems.pitch, policy)) {
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.doom_recall_failed"), true);
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false));
        beam(player.serverLevel(), from.add(0, 1, 0), player.position().add(0, 1, 0), 24);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1, 0.7F);
    }

    private static ContentAction ascendant(JsonObject json) {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !equipped(player)
                    || !ready(player, "ascendant", cooldown(context, 120) + number(context, json, "duration", 8))) {
                return;
            }
            Systems systems = systems(player);
            systems.ascendantUntil = now(player) + Math.round(number(context, json, "duration", 8) * 20);
            systems.stormDamage = (float) number(context, json, "damage", 40);
            systems.stormRadius = Math.clamp(number(context, json, "range", 16), 1, 24);
            systems.stormTargets = (int) Math.clamp(number(context, json, "max_targets", 8), 1, 12);
            systems.beamMultiplier = Math.clamp(number(context, json, "beam_multiplier", 1.75), 1, 3);
            systems.ascendantCooldown = Math.round(cooldown(context, 120) * 20);
            storm(player, systems);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1, 0.6F);
            inCombat(player);
        };
    }

    private static List<LivingEntity> targets(ServerPlayer player, Vec3 center, double radius, int maximum) {
        List<LivingEntity> targets = player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius), target -> !ThematicTargets.spared(player, target)
                        && target.position().distanceToSqr(center) <= radius * radius && player.hasLineOfSight(target));
        targets.sort(Comparator.comparingDouble(target -> target.position().distanceToSqr(center)));
        return targets.stream().limit(maximum).toList();
    }

    private static void storm(ServerPlayer player, Systems systems) {
        for (LivingEntity target : targets(player, player.position(), systems.stormRadius, systems.stormTargets)) {
            target.hurt(player.serverLevel().damageSources().playerAttack(player), systems.stormDamage);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false), player);
            Vec3 at = target.position().add(0, 1, 0);
            beam(player.serverLevel(), at.add(0, 5, 0), at, 12);
        }
    }

    private static void beam(ServerLevel level, Vec3 start, Vec3 end, int count) {
        for (int i = 0; i <= count; i++) {
            Vec3 at = start.lerp(end, i / (double) count);
            level.sendParticles(GREEN, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            level.sendParticles(GREEN, center.x + Math.cos(angle) * radius, center.y + 0.1,
                    center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    private static void intercept(ServerPlayer player, Systems systems) {
        for (Projectile projectile : player.serverLevel().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(2.2))
                .stream().limit(16).toList()) {
            if (systems.shield <= 0) {
                break;
            }
            if (projectile.getOwner() == player || projectile.getOwner() instanceof LivingEntity owner
                    && ThematicTargets.spared(player, owner)
                    || projectile.getDeltaMovement().dot(projectile.position().subtract(player.getBoundingBox().getCenter())) >= 0) {
                continue;
            }
            systems.shield = Math.max(0, systems.shield - systems.projectileCost);
            player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, projectile.getX(), projectile.getY(), projectile.getZ(), 4, 0.1, 0.1, 0.1, 0.02);
            projectile.discard();
            if (systems.shield <= 0) {
                shieldBroken(player);
            }
        }
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!equipped(player)) {
            clear(player);
            return;
        }
        long time = now(player);
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 30, 0, false, false));
            player.removeEffect(MobEffects.POISON);
            if (player.tickCount % 40 == 0 && time >= player.getPersistentData().getLong(REPAIR_AT)) {
                player.heal(1);
            }
        }
        Systems systems = SYSTEMS.get(player.getUUID());
        if (systems == null) {
            return;
        }
        if (systems.dimension != player.level().dimension()) {
            clear(player);
            return;
        }
        if (systems.wardUntil > time && systems.shield > 0) {
            if (player.tickCount % 2 == 0) {
                intercept(player, systems);
            }
            if (player.tickCount % 5 == 0) {
                ring(player.serverLevel(), player.position().add(0, 0.8, 0), 1.3, 12);
            }
        }
        if (systems.sigil != null && systems.sigilUntil > time && player.tickCount % 20 == 0
                && player.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(systems.sigil))) {
            for (LivingEntity target : targets(player, systems.sigil, systems.sigilRadius, 8)) {
                target.hurt(player.serverLevel().damageSources().indirectMagic(player, player), systems.sigilDamage);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false), player);
            }
            ring(player.serverLevel(), systems.sigil, systems.sigilRadius, 20);
        }
        if (systems.ascendantUntil > time) {
            if (player.tickCount % 20 == 0) {
                storm(player, systems);
                inCombat(player);
            }
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2, 1, false, false));
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !equipped(player) || event.getAmount() <= 0) {
            return;
        }
        inCombat(player);
        Systems systems = SYSTEMS.get(player.getUUID());
        if (systems == null || systems.dimension != player.level().dimension() || systems.wardUntil <= now(player)
                || systems.shield <= 0 || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float absorbed = Math.min(systems.shield, event.getAmount());
        systems.shield -= absorbed;
        event.setAmount(event.getAmount() - absorbed);
        if (event.getAmount() <= 0) {
            event.setCanceled(true);
        }
        if (systems.shield <= 0) {
            shieldBroken(player);
        }
    }

    private static void shieldBroken(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.integrated_superheroes.doom_ward_broken"), true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1, 0.6F);
    }

    private static void onDamageDealt(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && equipped(player) && event.getNewDamage() > 0) {
            inCombat(player);
        }
    }

    private static void clear(ServerPlayer player) {
        Systems systems = SYSTEMS.remove(player.getUUID());
        if (systems == null) {
            return;
        }
        if (systems.wardUntil > 0) {
            player.getPersistentData().putLong(GATE + "aegis", now(player) + systems.wardCooldown);
        }
        if (systems.ascendantUntil > 0) {
            player.getPersistentData().putLong(GATE + "ascendant", now(player) + systems.ascendantCooldown);
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
        for (String move : List.of("gauntlet", "aegis", "ascendant", "repair_at")) {
            String key = GATE + move;
            event.getEntity().getPersistentData().putLong(key, event.getOriginal().getPersistentData().getLong(key));
        }
    }

    private static void onStop(ServerStoppedEvent event) {
        SYSTEMS.clear();
    }
}
