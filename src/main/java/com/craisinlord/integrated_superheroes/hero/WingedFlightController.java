package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WingedFlightController {
    static final ResourceLocation FLIGHT_ID = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "angel_wingbeat");
    private static final float DEFAULT_FLY_SPEED = 0.05F;
    private static final double DIVE_MIN_DROP = 0.35D;
    private static final double LONG_GLIDE = 150.0D;
    private static final Map<UUID, State> STATES = new HashMap<>();

    private WingedFlightController() {
    }

    private static final class State {
        double lift = AngelConfig.LIFT_MAX.get();
        boolean wingbeat;
        boolean takeoffPaid;
        boolean wasFlying;
        boolean gliding;
        int groundTicks;
        Vec3 last;
        ResourceKey<Level> level;
        Dive dive;
        int sentMode = -1;
        int sentLift = -1;
        boolean sentWings;
        double glideDistance;
        boolean glideClimbed;
    }

    private static final class Dive {
        int ticks;
        double speed;
        double measured;
        double peakY;
        Vec3 last;
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
    }

    static boolean wingbeat(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.wingbeat;
    }

    static boolean diving(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.dive != null;
    }

    static boolean gliding(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.gliding;
    }

    static double toggleWingbeat(ServerPlayer player) {
        State state = state(player);
        if (state.wingbeat) {
            state.wingbeat = false;
            applyFlight(player, false);
            HeroAbilities.setActive(player, AngelPower.WINGBEAT, false);
            HeroFx.play(player, SoundEvents.PHANTOM_FLAP, 0.6F, 1.3F);
            sync(player, state, true);
            return 0.5D;
        }
        if (blocked(player)) {
            player.displayClientMessage(Component.literal("§7You can't spread your wings right now."), true);
            return 0.0D;
        }
        boolean airborne = !player.onGround();
        double cost = state.takeoffPaid ? 0.0D : AngelConfig.TAKEOFF_COST.get();
        if (state.lift <= 0.0D || state.lift < cost) {
            player.displayClientMessage(Component.literal("§7Your wings are spent. Rest on solid ground first."), true);
            return 0.0D;
        }
        state.wingbeat = true;
        HeroAbilities.setActive(player, AngelPower.WINGBEAT, true);
        if (state.dive == null) {
            applyFlight(player, true);
            if (!player.getAbilities().flying) {
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
            if (!airborne) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.5D, player.getDeltaMovement().z);
                player.hurtMarked = true;
            }
            takeoff(player, state);
        }
        sync(player, state, true);
        return 0.5D;
    }

    static void tick(ServerPlayer player) {
        State state = state(player);
        ResourceKey<Level> level = player.level().dimension();
        if (state.level != level) {
            state.level = level;
            state.last = null;
            state.sentMode = -1;
            if (state.dive != null) {
                endDive(player, state, false);
            }
        }
        Vec3 position = player.position();
        Vec3 motion = state.last == null ? Vec3.ZERO : position.subtract(state.last);
        state.last = position;
        if (motion.lengthSqr() > 400.0D) {
            motion = Vec3.ZERO;
        }
        if (state.dive != null) {
            tickDive(player, state);
        }
        boolean blocked = blocked(player);
        boolean grounded = player.onGround() && !player.getAbilities().flying;
        state.groundTicks = grounded ? state.groundTicks + 1 : 0;
        if (state.groundTicks >= AngelConfig.GROUND_SETTLE_TICKS.get()) {
            state.takeoffPaid = false;
            double max = AngelConfig.LIFT_MAX.get();
            if (state.lift < max) {
                state.lift = Math.min(max, state.lift + AngelConfig.GROUND_REGAIN.get() / 20.0D);
            }
        }
        boolean grant = state.wingbeat && !blocked && state.lift > 0.0D && state.dive == null;
        applyFlight(player, grant);
        boolean flying = player.getAbilities().flying;
        boolean own = flying && ownFlight(player);
        if (own && !state.wasFlying) {
            takeoff(player, state);
            own = player.getAbilities().flying;
        }
        state.wasFlying = own;
        if (own) {
            drain(player, state, motion);
        }
        updateGlide(player, state, motion, blocked);
        if (state.gliding) {
            player.fallDistance = 0.0F;
        }
        sync(player, state, false);
    }

    private static void takeoff(ServerPlayer player, State state) {
        if (state.takeoffPaid) {
            return;
        }
        double cost = AngelConfig.TAKEOFF_COST.get();
        if (state.lift < cost) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.displayClientMessage(Component.literal("§7Not enough Lift to take off."), true);
            return;
        }
        state.lift -= cost;
        state.takeoffPaid = true;
        HeroFx.play(player, SoundEvents.ENDER_DRAGON_FLAP, 0.5F, 1.6F);
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2D, player.getZ(), 10, 0.6D, 0.1D, 0.6D, 0.02D);
    }

    private static void drain(ServerPlayer player, State state, Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        boolean sprint = player.isSprinting();
        double rate;
        if (motion.y > 0.05D) {
            rate = sprint ? AngelConfig.SPRINT_CLIMB_DRAIN.get() : AngelConfig.HOVER_DRAIN.get();
        } else if (horizontal < 0.08D && Math.abs(motion.y) <= 0.05D) {
            rate = AngelConfig.HOVER_DRAIN.get();
        } else {
            rate = sprint ? AngelConfig.SPRINT_DRAIN.get() : AngelConfig.GLIDE_DRAIN.get();
        }
        if (player.isInWaterOrRain()) {
            rate *= AngelConfig.WET_MULTIPLIER.get();
        }
        state.lift = Math.max(0.0D, state.lift - rate / 20.0D);
        if (player.tickCount % 12 == 0) {
            HeroFx.play(player, SoundEvents.PHANTOM_FLAP, 0.35F, sprint ? 1.25F : 1.05F);
        }
        if (state.lift <= 0.0D) {
            player.displayClientMessage(Component.literal("§7Your wings give out. Glide down and rest."), true);
        }
    }

    private static void updateGlide(ServerPlayer player, State state, Vec3 motion, boolean blocked) {
        boolean airborne = !player.onGround() && !player.getAbilities().flying && !player.isInWater() && !player.isInLava()
                && !player.isPassenger() && !player.isFallFlying() && !player.onClimbable() && !player.isSpectator();
        boolean open = airborne && !blocked && state.dive == null && !player.isShiftKeyDown();
        if (open && !state.gliding) {
            boolean spent = state.wingbeat && state.lift <= 0.0D;
            boolean assist = HeroToggles.isOn(player, AngelPower.GLIDE_ASSIST) && motion.y < -AngelConfig.ASSIST_FALL_SPEED.get();
            open = spent || assist;
            if (open) {
                state.glideDistance = 0.0D;
                state.glideClimbed = false;
            }
        }
        if (state.gliding && open) {
            state.glideDistance += Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            if (motion.y > 0.05D) {
                state.glideClimbed = true;
            }
            if (!state.glideClimbed && state.glideDistance >= LONG_GLIDE) {
                state.glideClimbed = true;
                HeroAdvancements.award(player, "x_men/angel/long_glide");
            }
        }
        state.gliding = open;
    }

    static double skyfall(ServerPlayer player) {
        State state = state(player);
        if (state.dive != null) {
            if (state.dive.ticks < AngelConfig.SKYFALL_PULL_OUT_TICKS.get()) {
                return 0.0D;
            }
            pullOut(player, state);
            return 0.0D;
        }
        if (player.onGround() || player.isInWater() || player.isInLava() || player.isPassenger() || player.isFallFlying()) {
            player.displayClientMessage(Component.literal("§7You need open air below you to dive."), true);
            return 0.0D;
        }
        Dive dive = new Dive();
        Vec3 motion = player.getDeltaMovement();
        dive.speed = Math.max(AngelConfig.SKYFALL_START_SPEED.get(), Math.min(AngelConfig.SKYFALL_MAX_SPEED.get(), motion.length()));
        dive.measured = 0.0D;
        dive.peakY = player.getY();
        dive.last = player.position();
        state.dive = dive;
        state.gliding = false;
        applyFlight(player, false);
        if (player.getAbilities().flying && ownFlight(player)) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        player.setDeltaMovement(diveDirection(player).scale(dive.speed));
        player.hurtMarked = true;
        HeroAbilities.setActive(player, AngelPower.SKYFALL, true);
        HeroFx.play(player, SoundEvents.PHANTOM_SWOOP, 1.0F, 0.8F);
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0D, player.getZ(), 12, 0.5D, 0.3D, 0.5D, 0.05D);
        sync(player, state, true);
        return 0.0D;
    }

    private static Vec3 diveDirection(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        if (look.y <= -DIVE_MIN_DROP) {
            return look.normalize();
        }
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        double scale = Math.sqrt(1.0D - DIVE_MIN_DROP * DIVE_MIN_DROP);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize().scale(scale);
        return new Vec3(flat.x, flat.lengthSqr() < 1.0E-4D ? -1.0D : -DIVE_MIN_DROP, flat.z);
    }

    private static void tickDive(ServerPlayer player, State state) {
        Dive dive = state.dive;
        dive.ticks++;
        Vec3 from = dive.last;
        Vec3 to = player.position();
        dive.last = to;
        double moved = Math.min(AngelConfig.SKYFALL_MAX_SPEED.get(), from.distanceTo(to));
        dive.measured = dive.ticks <= 1 ? moved : dive.measured * 0.5D + moved * 0.5D;
        if (!player.isAlive() || player.isInWater() || player.isInLava() || player.isPassenger() || player.isFallFlying() || blocked(player)) {
            endDive(player, state, true);
            return;
        }
        if (player.getAbilities().flying) {
            if (dive.ticks >= AngelConfig.SKYFALL_PULL_OUT_TICKS.get()) {
                pullOut(player, state);
                return;
            }
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        LivingEntity target = sweep(player, from, to);
        if (target != null) {
            strike(player, state, target, to);
            return;
        }
        if (player.onGround() || dive.ticks > 3 && moved < 0.05D) {
            impact(player, state);
            return;
        }
        if (player.horizontalCollision && moved < 0.3D) {
            endDive(player, state, true);
            return;
        }
        if (dive.ticks >= AngelConfig.SKYFALL_MAX_TICKS.get()) {
            pullOut(player, state);
            return;
        }
        dive.speed = Math.min(AngelConfig.SKYFALL_MAX_SPEED.get(), dive.speed + AngelConfig.SKYFALL_ACCELERATION.get());
        player.setDeltaMovement(diveDirection(player).scale(dive.speed));
        player.hurtMarked = true;
        player.fallDistance = (float) Math.max(0.0D, dive.peakY - player.getY() - AngelConfig.SKYFALL_CUSHION.get());
        if (dive.ticks % 2 == 0) {
            player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0D, player.getZ(), 2, 0.3D, 0.3D, 0.3D, 0.0D);
        }
    }

    private static LivingEntity sweep(ServerPlayer player, Vec3 from, Vec3 to) {
        double height = player.getBbHeight() * 0.5D;
        Vec3 start = from.add(0.0D, height, 0.0D);
        Vec3 end = to.add(0.0D, height, 0.0D);
        AABB area = new AABB(from, to).expandTowards(0.0D, player.getBbHeight(), 0.0D).inflate(player.getBbWidth() + 0.8D);
        double reach = player.getBbWidth() * 0.5D + 0.3D;
        return player.level().getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player && KineticBurst.affects(player, entity)
                        && (entity.getBoundingBox().inflate(reach).clip(start, end).isPresent()
                        || entity.getBoundingBox().inflate(0.3D).intersects(player.getBoundingBox())))
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(start)))
                .orElse(null);
    }

    private static float damage(Dive dive) {
        double start = AngelConfig.SKYFALL_START_SPEED.get();
        double max = AngelConfig.SKYFALL_MAX_SPEED.get();
        double speed = Math.min(max, Math.min(dive.speed, Math.max(dive.measured, start)));
        double fraction = max <= start ? 1.0D : Math.max(0.0D, Math.min(1.0D, (speed - start) / (max - start)));
        double floor = AngelConfig.SKYFALL_MIN_DAMAGE_FRACTION.get();
        double top = MarvelBalanceConfig.POUNCE_DAMAGE.get() * AngelConfig.SKYFALL_POUNCE_MULTIPLIER.get();
        return (float) (top * (floor + (1.0D - floor) * fraction));
    }

    private static void strike(ServerPlayer player, State state, LivingEntity target, Vec3 at) {
        Dive dive = state.dive;
        float amount = damage(dive);
        Vec3 direction = diveDirection(player);
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().mobAttack(player), amount));
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (flat.lengthSqr() > 1.0E-4D) {
            target.knockback(AngelConfig.SKYFALL_KNOCKBACK.get(), -flat.x, -flat.z);
        }
        ServerLevel level = player.serverLevel();
        Vec3 center = target.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 20, 0.4D, 0.4D, 0.4D, 0.3D);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 12, 0.5D, 0.5D, 0.5D, 0.08D);
        HeroFx.play(player, SoundEvents.MACE_SMASH_AIR, 1.0F, 1.0F);
        player.setDeltaMovement(direction.x * 0.25D, 0.6D, direction.z * 0.25D);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        endDive(player, state, true);
    }

    private static void impact(ServerPlayer player, State state) {
        Dive dive = state.dive;
        float amount = damage(dive);
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position();
        double radius = AngelConfig.SKYFALL_IMPACT_RADIUS.get();
        LivingEntity target = radius <= 0.0D ? null : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                        entity -> entity != player && KineticBurst.affects(player, entity) && entity.distanceToSqr(at) <= radius * radius)
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(at)))
                .orElse(null);
        if (target != null) {
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().mobAttack(player), amount));
            target.knockback(AngelConfig.SKYFALL_KNOCKBACK.get() * 0.7D, at.x - target.getX(), at.z - target.getZ());
        }
        BlockPos below = BlockPos.containing(at.x, at.y - 0.2D, at.z);
        BlockState ground = level.getBlockState(below);
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1D, at.z, 30, 1.0D, 0.1D, 1.0D, 0.15D);
        }
        SerumKit.ring(level, at.add(0.0D, 0.15D, 0.0D), ParticleTypes.CLOUD, 1.5D, 20);
        HeroFx.play(player, SoundEvents.MACE_SMASH_GROUND, 1.0F, 0.9F);
        HeroFx.shake(level, at, 8.0D, 0.4F, 6);
        endDive(player, state, true);
    }

    private static void pullOut(ServerPlayer player, State state) {
        Dive dive = state.dive;
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize();
        double speed = Math.min(AngelConfig.GLIDE_SPEED.get() * 2.0D, dive.speed * 0.6D);
        player.setDeltaMovement(flat.x * speed, 0.25D, flat.z * speed);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        HeroFx.play(player, SoundEvents.PHANTOM_FLAP, 0.8F, 0.9F);
        endDive(player, state, true);
        state.gliding = !player.onGround();
        state.glideDistance = 0.0D;
        state.glideClimbed = true;
    }

    private static void endDive(ServerPlayer player, State state, boolean cooldown) {
        state.dive = null;
        HeroAbilities.setActive(player, AngelPower.SKYFALL, false);
        if (cooldown) {
            HeroAbilities.setCooldown(player, AngelPower.SKYFALL, AngelConfig.SKYFALL_COOLDOWN.get());
        }
        sync(player, state, true);
    }

    private static boolean blocked(ServerPlayer player) {
        return player.isPassenger() || player.isSpectator() || player.isFallFlying() || HealingFactor.isDowned(player) || VillainWorld.suppressed(player);
    }

    private static boolean ownFlight(ServerPlayer player) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        return flight != null && flight.hasModifier(FLIGHT_ID) && !otherFlight(player);
    }

    private static boolean otherFlight(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator() || player.getAbilities().mayfly) {
            return true;
        }
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null) {
            return false;
        }
        return flight.getValue() - (flight.hasModifier(FLIGHT_ID) ? 1.0D : 0.0D) > 0.0D;
    }

    private static void applyFlight(ServerPlayer player, boolean enabled) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null) {
            return;
        }
        if (enabled) {
            if (!flight.hasModifier(FLIGHT_ID)) {
                flight.addTransientModifier(new AttributeModifier(FLIGHT_ID, 1.0D, AttributeModifier.Operation.ADD_VALUE));
            }
            float speed = AngelConfig.FLY_SPEED.get().floatValue();
            if (!otherFlight(player) && player.getAbilities().getFlyingSpeed() != speed) {
                player.getAbilities().setFlyingSpeed(speed);
                player.onUpdateAbilities();
            }
            return;
        }
        if (!flight.hasModifier(FLIGHT_ID)) {
            return;
        }
        flight.removeModifier(FLIGHT_ID);
        if (!otherFlight(player)) {
            player.getAbilities().flying = false;
            player.getAbilities().setFlyingSpeed(DEFAULT_FLY_SPEED);
            player.onUpdateAbilities();
        }
    }

    static boolean wingsShown(ServerPlayer player) {
        return HeroToggles.isOn(player, AngelPower.SHOW_WINGS);
    }

    private static int mode(ServerPlayer player, State state) {
        if (state.dive != null) {
            return WingedFlightPayload.DIVE;
        }
        if (player.getAbilities().flying) {
            return ownFlight(player) ? WingedFlightPayload.POWERED : WingedFlightPayload.GLIDE;
        }
        return state.gliding ? WingedFlightPayload.GLIDE : WingedFlightPayload.FOLDED;
    }

    private static void sync(ServerPlayer player, State state, boolean force) {
        int mode = mode(player, state);
        boolean wings = wingsShown(player);
        int lift = (int) Math.round(state.lift / AngelConfig.LIFT_MAX.get() * 100.0D);
        boolean shared = force || mode != state.sentMode || wings != state.sentWings || player.tickCount % 200 == 0;
        if (shared) {
            state.sentMode = mode;
            state.sentWings = wings;
            state.sentLift = lift;
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new WingedFlightPayload(player.getId(), mode, lift, wings));
        } else if (lift != state.sentLift) {
            state.sentLift = lift;
            PacketDistributor.sendToPlayer(player, new WingedFlightPayload(player.getId(), mode, lift, wings));
        }
    }

    static void sendTo(ServerPlayer angel, ServerPlayer viewer) {
        State state = STATES.get(angel.getUUID());
        if (state == null || !AngelPower.isAngel(angel)) {
            return;
        }
        int lift = (int) Math.round(state.lift / AngelConfig.LIFT_MAX.get() * 100.0D);
        PacketDistributor.sendToPlayer(viewer, new WingedFlightPayload(angel.getId(), mode(angel, state), lift, wingsShown(angel)));
    }

    static void forget(ServerPlayer angel, ServerPlayer viewer) {
        PacketDistributor.sendToPlayer(viewer, new WingedFlightPayload(angel.getId(), WingedFlightPayload.NONE, 0, false));
    }

    static void interrupt(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state != null && state.dive != null) {
            endDive(player, state, true);
        }
    }

    static void clear(ServerPlayer player) {
        State state = STATES.remove(player.getUUID());
        applyFlight(player, false);
        if (state != null && state.dive != null) {
            HeroAbilities.setActive(player, AngelPower.SKYFALL, false);
        }
        HeroAbilities.setActive(player, AngelPower.WINGBEAT, false);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new WingedFlightPayload(player.getId(), WingedFlightPayload.NONE, 0, false));
    }

    static void logout(ServerPlayer player) {
        STATES.remove(player.getUUID());
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null) {
            flight.removeModifier(FLIGHT_ID);
        }
    }
}
