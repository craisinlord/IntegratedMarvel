package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.content.ability.state.NumberSpec;
import bond.thematic.api.content.action.ActionContext;
import bond.thematic.api.content.action.Actions;
import bond.thematic.api.content.action.ContentAction;
import bond.thematic.api.util.AbilityUtils;
import bond.thematic.api.vfx.VfxAPI;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.HeroGrief;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class BatmanActions {
    public static final String GEL_FLAG = "bat_gel_blew";
    private static final ResourceLocation GEL_BLOB = ResourceLocation.fromNamespaceAndPath("thematic", "bat_gel_blob");
    private static final ResourceLocation DETONATION = ResourceLocation.fromNamespaceAndPath("thematic", "major_detonation");
    private static final Map<UUID, LinkedHashMap<UUID, Scan>> SCANS = new HashMap<>();
    private static boolean registered;

    private BatmanActions() {
    }

    private record Scan(long until, float bonus) {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        try {
            action("scan_mark", BatmanActions::scanMark);
            action("gel_detonate", BatmanActions::gelDetonate);
            action("batmobile_order", BatmobileOrders::order);
        } catch (RuntimeException | LinkageError error) {
            IntegratedSuperheroes.LOGGER.warn("Couldn't register the Batman ability actions with Thematic", error);
            return;
        }
        NeoForge.EVENT_BUS.addListener(BatmanActions::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(BatmanActions::onLogout);
        BatmobileOrders.register();
    }

    private static void action(String name, Function<JsonObject, ContentAction> factory) {
        ResourceLocation id = IntegratedSuperheroes.id(name);
        if (!Actions.TYPES.has(id)) {
            Actions.TYPES.register(id, factory::apply);
        }
    }

    static double number(ActionContext context, JsonObject json, String field, double fallback) {
        return NumberSpec.parse(json, field, NumberSpec.constant(fallback)).resolve(context.inputs());
    }

    static SoundEvent sound(ResourceLocation id, SoundEvent fallback) {
        return BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse(fallback);
    }

    private static ContentAction scanMark(JsonObject json) {
        return context -> {
            if (context.isClient() || context.owner() == null || !(context.hitEntity() instanceof LivingEntity target)) {
                return;
            }
            double seconds = number(context, json, "seconds", 30.0D);
            int max = Math.max(1, (int) Math.round(number(context, json, "max", 3.0D)));
            float bonus = (float) (ThematicTargets.boss(target) ? number(context, json, "boss_bonus", 0.1D) : number(context, json, "bonus", 0.2D));
            long now = target.level().getGameTime();
            LinkedHashMap<UUID, Scan> scans = SCANS.computeIfAbsent(context.owner().getUUID(), id -> new LinkedHashMap<>());
            scans.values().removeIf(scan -> scan.until() < now);
            scans.remove(target.getUUID());
            Iterator<UUID> oldest = scans.keySet().iterator();
            while (scans.size() >= max && oldest.hasNext()) {
                oldest.next();
                oldest.remove();
            }
            scans.put(target.getUUID(), new Scan(now + Math.round(seconds * 20.0D), bonus));
        };
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (SCANS.isEmpty() || !(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        LinkedHashMap<UUID, Scan> scans = SCANS.get(attacker.getUUID());
        if (scans == null) {
            return;
        }
        LivingEntity victim = event.getEntity();
        Scan scan = scans.get(victim.getUUID());
        if (scan == null) {
            return;
        }
        if (scan.until() < victim.level().getGameTime()) {
            scans.remove(victim.getUUID());
            return;
        }
        event.setAmount(event.getAmount() * (1.0F + scan.bonus()));
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SCANS.remove(event.getEntity().getUUID());
    }

    private static ContentAction gelDetonate(JsonObject json) {
        return context -> {
            LivingEntity owner = context.owner();
            if (context.isClient() || owner == null || !(owner.level() instanceof ServerLevel level)) {
                return;
            }
            double range = number(context, json, "range", 64.0D);
            float radius = (float) number(context, json, "radius", 4.0D);
            float damage = (float) number(context, json, "damage", 20.0D);
            double push = number(context, json, "push", 1.2D);
            float hardness = (float) number(context, json, "hardness", 3.0D);
            double breakRadius = number(context, json, "break_radius", 1.6D);
            List<Entity> blobs = new ArrayList<>(level.getEntities((Entity) null, owner.getBoundingBox().inflate(range),
                    entity -> entity instanceof Projectile projectile && projectile.getOwner() == owner
                            && GEL_BLOB.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))));
            AbilityUtils.storeBoolean(owner, GEL_FLAG, !blobs.isEmpty());
            blobs.sort(Comparator.comparingDouble(owner::distanceToSqr));
            for (int i = 0; i < blobs.size(); i++) {
                Entity blob = blobs.get(i);
                Vec3 at = blob.position().add(0.0D, blob.getBbHeight() * 0.5D, 0.0D);
                blob.discard();
                Runnable blast = () -> blast(level, owner, at, radius, damage, push, hardness, breakRadius);
                if (i == 0) {
                    blast.run();
                } else {
                    VillainWorld.later(level, i * 4, blast);
                }
            }
        };
    }

    private static void blast(ServerLevel level, LivingEntity owner, Vec3 at, float radius, float damage, double push, float hardness, double breakRadius) {
        VfxAPI.triggerVFX(level, at, "thematic:chem_burst");
        VfxAPI.triggerVFX(level, at, "thematic:impact_fracture");
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 2, 0.4D, 0.4D, 0.4D, 0.0D);
        level.sendParticles(ParticleTypes.GLOW, at.x, at.y, at.z, 24, 0.6D, 0.6D, 0.6D, 0.15D);
        level.playSound(null, at.x, at.y, at.z, sound(DETONATION, SoundEvents.GENERIC_EXPLODE.value()), SoundSource.PLAYERS, 2.0F, 0.95F + level.random.nextFloat() * 0.1F);
        DamageSource source = level.damageSources().explosion(owner, owner);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius))) {
            double distance = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D).distanceTo(at);
            if (distance > radius || ThematicTargets.spared(owner, victim)) {
                continue;
            }
            float share = (float) (1.0D - 0.5D * distance / radius);
            victim.hurt(source, damage * share);
            Vec3 away = victim.position().subtract(at);
            Vec3 flat = new Vec3(away.x, 0.0D, away.z);
            Vec3 direction = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize();
            double strength = push * share * (1.0D - victim.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) * 0.6D);
            victim.setDeltaMovement(victim.getDeltaMovement().add(direction.scale(strength)).add(0.0D, 0.35D * share, 0.0D));
            victim.hurtMarked = true;
            victim.hasImpulse = true;
        }
        if (!(owner instanceof ServerPlayer player) || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) || breakRadius <= 0.0D) {
            return;
        }
        BlockPos center = BlockPos.containing(at);
        int reach = (int) Math.ceil(breakRadius);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -reach, -reach), center.offset(reach, reach, reach))) {
            if (pos.getCenter().distanceTo(at) > breakRadius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            float speed = state.getDestroySpeed(level, pos);
            if (speed < 0.0F || speed > hardness) {
                continue;
            }
            HeroGrief.tryBreak(player, level, pos.immutable(), true);
        }
    }
}
