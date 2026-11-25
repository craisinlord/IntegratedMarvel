package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SurferBoardController {
    static final ResourceLocation FLIGHT_ID = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "silver_surfer_board");
    private static final float DEFAULT_FLY_SPEED = 0.05F;
    private static final int BOARD_COLOR = 0xE6EEF8;
    private static final double LONG_RIDE = 300.0D;
    private static final double TELEPORT_JUMP = 8.0D;
    private static final Map<UUID, Ride> RIDES = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> GRACE = ServerState.map(new HashMap<>());

    private SurferBoardController() {
    }

    private static final class Ride {
        ResourceKey<Level> level;
        Vec3 last;
        int overspeed;
        double airDistance;
        boolean longRideAwarded;
        int sentFlags = -1;
    }

    static boolean riding(ServerPlayer player) {
        return RIDES.containsKey(player.getUUID());
    }

    static double press(ServerPlayer player) {
        if (riding(player)) {
            dismount(player, false);
            return SilverSurferConfig.SUMMON_COOLDOWN.get();
        }
        if (blocked(player)) {
            player.displayClientMessage(Component.literal("§7You can't call your board right now."), true);
            return 0.0D;
        }
        if (blockedDimension(player)) {
            player.displayClientMessage(Component.literal("§7Your board won't answer here."), true);
            return 0.0D;
        }
        Ride ride = new Ride();
        ride.level = player.level().dimension();
        ride.last = player.position();
        RIDES.put(player.getUUID(), ride);
        GRACE.remove(player.getUUID());
        applyFlight(player, true);
        player.fallDistance = 0.0F;
        HeroAbilities.setActive(player, SilverSurferPower.BOARD, true);
        ServerLevel level = player.serverLevel();
        Vec3 feet = player.position();
        SerumKit.ring(level, feet.add(0.0D, 0.1D, 0.0D), SerumKit.glow(player, BOARD_COLOR, 1.0F), 1.1D, 18);
        level.sendParticles(ParticleTypes.END_ROD, feet.x, feet.y + 0.2D, feet.z, 10, 0.5D, 0.05D, 0.5D, 0.02D);
        HeroFx.play(player, SoundEvents.BEACON_ACTIVATE, 0.7F, 1.8F);
        sync(player, ride, true);
        return 0.0D;
    }

    static void tick(ServerPlayer player) {
        Ride ride = RIDES.get(player.getUUID());
        if (ride == null) {
            return;
        }
        if (ride.level != player.level().dimension()) {
            dismount(player, true);
            return;
        }
        if (blocked(player) || blockedDimension(player)) {
            dismount(player, true);
            player.displayClientMessage(Component.literal("§7Your board slips out from under you."), true);
            return;
        }
        applyFlight(player, true);
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0F;
        Vec3 position = player.position();
        Vec3 moved = ride.last == null ? Vec3.ZERO : position.subtract(ride.last);
        ride.last = position;
        double distance = moved.length();
        if (distance < TELEPORT_JUMP) {
            double allowed = SilverSurferConfig.CRUISE_MAX_SPEED.get() * 1.5D + 0.35D;
            ride.overspeed = distance > allowed ? ride.overspeed + 1 : 0;
            if (ride.overspeed >= SilverSurferConfig.OVERSPEED_TICKS.get()) {
                dismount(player, true);
                return;
            }
            if (player.onGround()) {
                ride.airDistance = 0.0D;
            } else {
                ride.airDistance += Math.sqrt(moved.x * moved.x + moved.z * moved.z);
                if (!ride.longRideAwarded && ride.airDistance >= LONG_RIDE) {
                    ride.longRideAwarded = true;
                    HeroAdvancements.award(player, "fantastic_four/silver_surfer/long_ride");
                }
            }
        }
        sync(player, ride, false);
    }

    static void dismount(ServerPlayer player, boolean forced) {
        Ride ride = RIDES.remove(player.getUUID());
        if (ride == null) {
            return;
        }
        applyFlight(player, false);
        player.fallDistance = 0.0F;
        HeroAbilities.setActive(player, SilverSurferPower.BOARD, false);
        if (forced) {
            int grace = SilverSurferConfig.LANDING_GRACE_TICKS.get();
            if (grace > 0) {
                GRACE.put(player.getUUID(), player.serverLevel().getGameTime() + grace);
            }
            HeroAbilities.setCooldown(player, SilverSurferPower.BOARD, SilverSurferConfig.FORCED_COOLDOWN.get());
        }
        if (player.isAlive()) {
            ServerLevel level = player.serverLevel();
            Vec3 feet = player.position();
            level.sendParticles(ParticleTypes.END_ROD, feet.x, feet.y + 0.1D, feet.z, 12, 0.5D, 0.05D, 0.5D, 0.04D);
            HeroFx.play(player, SoundEvents.BEACON_DEACTIVATE, 0.6F, 1.8F);
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SurferBoardPayload(player.getId(), 0, 0.0F, 0.0F, 0.0F));
    }

    static void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_FALL)) {
            return;
        }
        if (riding(player)) {
            event.setCanceled(true);
            return;
        }
        Long until = GRACE.remove(player.getUUID());
        if (until != null && player.serverLevel().getGameTime() <= until) {
            event.setCanceled(true);
        }
    }

    static void clear(ServerPlayer player) {
        dismount(player, false);
        GRACE.remove(player.getUUID());
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null && flight.hasModifier(FLIGHT_ID)) {
            applyFlight(player, false);
        }
    }

    static void logout(ServerPlayer player) {
        RIDES.remove(player.getUUID());
        GRACE.remove(player.getUUID());
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null) {
            flight.removeModifier(FLIGHT_ID);
        }
    }

    static void sendTo(ServerPlayer surfer, ServerPlayer viewer) {
        if (riding(surfer)) {
            PacketDistributor.sendToPlayer(viewer, payload(surfer));
        }
    }

    static void forget(ServerPlayer surfer, ServerPlayer viewer) {
        PacketDistributor.sendToPlayer(viewer, new SurferBoardPayload(surfer.getId(), 0, 0.0F, 0.0F, 0.0F));
    }

    private static int flags(ServerPlayer player) {
        int flags = SurferBoardPayload.RIDING;
        if (HeroToggles.isOn(player, SilverSurferPower.PRECISION)) {
            flags |= SurferBoardPayload.PRECISION;
        }
        if (HeroToggles.isOn(player, SilverSurferPower.TRAIL)) {
            flags |= SurferBoardPayload.TRAIL;
        }
        return flags;
    }

    private static SurferBoardPayload payload(ServerPlayer player) {
        int flags = flags(player);
        boolean precision = (flags & SurferBoardPayload.PRECISION) != 0;
        double speed = precision ? SilverSurferConfig.PRECISION_MAX_SPEED.get() : SilverSurferConfig.CRUISE_MAX_SPEED.get();
        double acceleration = precision ? SilverSurferConfig.PRECISION_ACCELERATION.get() : SilverSurferConfig.CRUISE_ACCELERATION.get();
        double turn = precision ? SilverSurferConfig.PRECISION_TURN_DEGREES.get() : SilverSurferConfig.CRUISE_TURN_DEGREES.get();
        return new SurferBoardPayload(player.getId(), flags, (float) speed, (float) acceleration, (float) turn);
    }

    private static void sync(ServerPlayer player, Ride ride, boolean force) {
        int flags = flags(player);
        if (force || flags != ride.sentFlags || player.tickCount % 200 == 0) {
            ride.sentFlags = flags;
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload(player));
        }
    }

    private static boolean blocked(ServerPlayer player) {
        return !player.isAlive() || player.isPassenger() || player.isSpectator() || player.isFallFlying() || player.isSleeping()
                || HealingFactor.isDowned(player) || VillainWorld.suppressed(player);
    }

    private static boolean blockedDimension(ServerPlayer player) {
        return SilverSurferConfig.BLOCKED_DIMENSIONS.get().contains(player.level().dimension().location().toString());
    }

    private static boolean otherFlight(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null) {
            return player.getAbilities().mayfly;
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
}
