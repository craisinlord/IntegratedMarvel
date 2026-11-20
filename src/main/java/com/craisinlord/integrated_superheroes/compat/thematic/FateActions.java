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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
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

public final class FateActions {
    private static final ResourceLocation SUIT = ResourceLocation.fromNamespaceAndPath("thematic", "dr_fate");
    private static final String PREFIX = "integrated_superheroes:fate_";
    private static final String WARD = PREFIX + "mantle";
    private static final String COMBAT = PREFIX + "combat_until";
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.76F, 0.12F), 1.2F);
    private static final Map<UUID, Order> ORDERS = new HashMap<>();
    private static boolean registered;

    private static final class Order {
        ResourceKey<Level> dimension;
        UUID judged;
        long judgedUntil;
        Vec3 seal;
        long sealUntil;
        double sealRadius;
        float sealDamage;
    }

    private FateActions() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        action("fate_judgment", FateActions::judgment);
        action("fate_bonds", FateActions::bonds);
        action("fate_decree", FateActions::decree);
        NeoForge.EVENT_BUS.addListener(FateActions::onTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, FateActions::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(FateActions::onDamageDealt);
        NeoForge.EVENT_BUS.addListener(FateActions::onLogout);
        NeoForge.EVENT_BUS.addListener(FateActions::onDimension);
        NeoForge.EVENT_BUS.addListener(FateActions::onDeath);
        NeoForge.EVENT_BUS.addListener(FateActions::onClone);
        NeoForge.EVENT_BUS.addListener(FateActions::onStop);
    }

    private static void action(String name, Function<JsonObject, ContentAction> factory) {
        ResourceLocation id = IntegratedSuperheroes.id(name);
        if (!Actions.TYPES.has(id)) {
            Actions.TYPES.register(id, factory::apply);
        }
    }

    private static boolean equipped(ServerPlayer player) {
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(chest.getItem()).equals(SUIT)
                && player.isAlive() && !player.isSpectator() && SuitPieces.complete(player, chest.getItem());
    }

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    private static Order order(ServerPlayer player) {
        Order order = ORDERS.get(player.getUUID());
        if (order == null || order.dimension != player.level().dimension()) {
            order = new Order();
            order.dimension = player.level().dimension();
            ORDERS.put(player.getUUID(), order);
        }
        return order;
    }

    private static double number(ActionContext context, JsonObject json, String key, double fallback) {
        return NumberSpec.parse(json, key, NumberSpec.constant(fallback)).resolve(context.inputs());
    }

    private static boolean ready(ActionContext context, ServerPlayer player, String move, double fallback) {
        String key = PREFIX + move;
        long time = now(player);
        if (player.getPersistentData().getLong(key) > time) {
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.fate_recharging"), true);
            return false;
        }
        double ticks = context.ability() == null ? fallback * 20 : context.ability().cooldown(player);
        player.getPersistentData().putLong(key, time + Math.max(1, Math.round(ticks)));
        player.getPersistentData().putLong(COMBAT, time + 120);
        return true;
    }

    private static Vec3 aim(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start;
        for (int step = 1; step <= Math.ceil(range); step++) {
            Vec3 candidate = start.add(player.getLookAngle().scale(Math.min(step, range)));
            if (!player.serverLevel().hasChunkAt(BlockPos.containing(candidate))) {
                break;
            }
            end = candidate;
        }
        HitResult hit = player.serverLevel().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private static LivingEntity aimedTarget(ServerPlayer player, Vec3 end) {
        Vec3 start = player.getEyePosition();
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1.5),
                target -> !ThematicTargets.spared(player, target) && player.hasLineOfSight(target)
                        && (target.getBoundingBox().inflate(0.9).contains(start)
                        || target.getBoundingBox().inflate(0.9).clip(start, end).isPresent()))
                .stream().min(Comparator.comparingDouble(target -> target.distanceToSqr(player))).orElse(null);
    }

    private static ContentAction judgment(JsonObject json) {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !equipped(player)
                    || !ready(context, player, "judgment", 3)) {
                return;
            }
            Vec3 end = aim(player, Math.clamp(number(context, json, "range", 40), 1, 56));
            LivingEntity target = aimedTarget(player, end);
            if (target == null) {
                player.displayClientMessage(Component.translatable("message.integrated_superheroes.fate_no_target"), true);
                return;
            }
            Order order = order(player);
            float damage = (float) number(context, json, "damage", 96);
            if (target.getUUID().equals(order.judged) && order.judgedUntil > now(player)) {
                damage *= (float) number(context, json, "focus_multiplier", 1.4);
            }
            if (target.hurt(player.serverLevel().damageSources().indirectMagic(player, player), damage)) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false), player);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, false), player);
                order.judged = target.getUUID();
                order.judgedUntil = now(player) + 120;
            }
            line(player.serverLevel(), player.getEyePosition(), target.getBoundingBox().getCenter(), 20);
            ankh(player.serverLevel(), target.position().add(0, target.getBbHeight() * 0.5, 0), 1.1);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.1F, 0.7F);
        };
    }

    private static ContentAction bonds(JsonObject json) {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !equipped(player)
                    || !ready(context, player, "bonds", 16)) {
                return;
            }
            Vec3 end = aim(player, Math.clamp(number(context, json, "range", 28), 1, 40));
            LivingEntity target = aimedTarget(player, end);
            Order order = order(player);
            order.seal = target == null ? end : target.position();
            order.sealUntil = now(player) + Math.round(number(context, json, "duration", 6) * 20);
            order.sealRadius = Math.clamp(number(context, json, "seal_radius", 6), 1, 8);
            order.sealDamage = (float) number(context, json, "seal_damage", 18);
            for (LivingEntity bound : foes(player, order.seal, order.sealRadius, 8)) {
                bound.hurt(player.serverLevel().damageSources().indirectMagic(player, player), (float) number(context, json, "damage", 48));
                ankh(player.serverLevel(), bound.getBoundingBox().getCenter(), 0.8);
            }
            ring(player.serverLevel(), order.seal, order.sealRadius, 32);
            player.serverLevel().playSound(null, BlockPos.containing(order.seal), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.4F);
        };
    }

    private static ContentAction decree(JsonObject json) {
        return context -> {
            if (context.isClient() || !(context.owner() instanceof ServerPlayer player) || !equipped(player)
                    || !ready(context, player, "decree", 100)) {
                return;
            }
            double radius = Math.clamp(number(context, json, "range", 18), 1, 24);
            for (LivingEntity target : foes(player, player.position(), radius, 10)) {
                if (target.hurt(player.serverLevel().damageSources().indirectMagic(player, player), (float) number(context, json, "damage", 200))) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2, false, false), player);
                }
                ankh(player.serverLevel(), target.getBoundingBox().getCenter(), 1.8);
            }
            List<LivingEntity> allies = player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                    target -> target.isAlive() && target.distanceToSqr(player) <= radius * radius && player.hasLineOfSight(target)
                            && (target == player || player.isAlliedTo(target)
                            || target instanceof OwnableEntity pet && player.getUUID().equals(pet.getOwnerUUID())));
            allies.sort(Comparator.comparingDouble(target -> target.distanceToSqr(player)));
            for (LivingEntity ally : allies.stream().limit(10).toList()) {
                ally.heal((float) number(context, json, "heal", 12));
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2, false, false), player);
                ally.removeEffect(MobEffects.POISON);
                ally.removeEffect(MobEffects.WITHER);
            }
            player.getPersistentData().putFloat(WARD, 100);
            ankh(player.serverLevel(), player.position().add(0, 3, 0), 3.5);
            ring(player.serverLevel(), player.position(), radius, 48);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.4F, 0.65F);
        };
    }

    private static List<LivingEntity> foes(ServerPlayer player, Vec3 center, double radius, int max) {
        List<LivingEntity> targets = player.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                target -> !ThematicTargets.spared(player, target) && target.position().distanceToSqr(center) <= radius * radius
                        && player.hasLineOfSight(target));
        targets.sort(Comparator.comparingDouble(target -> target.position().distanceToSqr(center)));
        return targets.stream().limit(max).toList();
    }

    private static void line(ServerLevel level, Vec3 from, Vec3 to, int count) {
        for (int i = 0; i <= count; i++) {
            Vec3 at = from.lerp(to, i / (double) count);
            level.sendParticles(GOLD, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            level.sendParticles(GOLD, center.x + Math.cos(angle) * radius, center.y + 0.15,
                    center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    private static void ankh(ServerLevel level, Vec3 center, double scale) {
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2 * i / 16;
            level.sendParticles(GOLD, center.x + Math.cos(angle) * scale * 0.35, center.y + scale * (0.5 + Math.sin(angle) * 0.35), center.z, 1, 0, 0, 0, 0);
        }
        line(level, center.add(0, scale * 0.15, 0), center.add(0, -scale, 0), 10);
        line(level, center.add(-scale * 0.65, -scale * 0.15, 0), center.add(scale * 0.65, -scale * 0.15, 0), 10);
    }

    private static float ward(ServerPlayer player) {
        return player.getPersistentData().contains(WARD) ? Math.clamp(player.getPersistentData().getFloat(WARD), 0, 100) : 100;
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !equipped(player) || event.getAmount() <= 0) {
            return;
        }
        player.getPersistentData().putLong(COMBAT, now(player) + 120);
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float current = ward(player);
        float absorbed = Math.min(current, event.getAmount() * 0.35F);
        player.getPersistentData().putFloat(WARD, current - absorbed);
        event.setAmount(event.getAmount() - absorbed);
        if (absorbed > 0) {
            player.serverLevel().sendParticles(GOLD, player.getX(), player.getY() + 1, player.getZ(), 6, 0.6, 0.7, 0.6, 0);
            if (absorbed >= current) {
                player.displayClientMessage(Component.translatable("message.integrated_superheroes.fate_mantle_broken"), true);
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
            }
        }
    }

    private static void onDamageDealt(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && equipped(player) && event.getNewDamage() > 0) {
            player.getPersistentData().putLong(COMBAT, now(player) + 120);
        }
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!equipped(player)) {
            ORDERS.remove(player.getUUID());
            return;
        }
        long time = now(player);
        if (player.tickCount % 20 == 0) {
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.DARKNESS);
            player.removeEffect(MobEffects.POISON);
            if (time >= player.getPersistentData().getLong(COMBAT)) {
                player.getPersistentData().putFloat(WARD, Math.min(100, ward(player) + 2));
            }
        }
        Order order = ORDERS.get(player.getUUID());
        if (order == null || order.dimension != player.level().dimension()) {
            ORDERS.remove(player.getUUID());
            return;
        }
        if (order.seal != null && order.sealUntil > time && player.tickCount % 20 == 0
                && player.serverLevel().hasChunkAt(BlockPos.containing(order.seal))) {
            for (LivingEntity target : foes(player, order.seal, order.sealRadius, 8)) {
                if (!target.hurt(player.serverLevel().damageSources().indirectMagic(player, player), order.sealDamage)) {
                    continue;
                }
                boolean resistant = target instanceof Player || ThematicTargets.boss(target);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, resistant ? 0 : 2, false, false), player);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1, false, false), player);
                if (!resistant) {
                    Vec3 offset = order.seal.subtract(target.position());
                    if (offset.horizontalDistanceSqr() > 2.25) {
                        Vec3 tug = new Vec3(offset.x, 0, offset.z).normalize().scale(0.22);
                        target.setDeltaMovement(target.getDeltaMovement().multiply(0.5, 1, 0.5).add(tug));
                        target.hurtMarked = true;
                    }
                }
            }
            if (player.tickCount % 20 == 0) {
                ring(player.serverLevel(), order.seal, order.sealRadius, 24);
            }
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ORDERS.remove(event.getEntity().getUUID());
    }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ORDERS.remove(event.getEntity().getUUID());
    }

    private static void onDeath(LivingDeathEvent event) {
        ORDERS.remove(event.getEntity().getUUID());
    }

    private static void onClone(PlayerEvent.Clone event) {
        for (String move : List.of("judgment", "bonds", "decree", "combat_until")) {
            event.getEntity().getPersistentData().putLong(PREFIX + move, event.getOriginal().getPersistentData().getLong(PREFIX + move));
        }
        if (event.getOriginal().getPersistentData().contains(WARD)) {
            event.getEntity().getPersistentData().putFloat(WARD, event.getOriginal().getPersistentData().getFloat(WARD));
        }
    }

    private static void onStop(ServerStoppedEvent event) {
        ORDERS.clear();
    }
}
