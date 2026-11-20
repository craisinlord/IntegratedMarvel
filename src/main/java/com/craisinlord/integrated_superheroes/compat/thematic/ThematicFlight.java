package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.content.entity.ai.move.HoverFlier;
import bond.thematic.api.content.entity.body.DataMobEntity;
import bond.thematic.api.registries.armors.armor.ArmorRegistry;
import bond.thematic.api.registries.armors.armor.CharacterItem;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ThematicFlight {
    private static final Map<String, Boolean> FLYERS = new ConcurrentHashMap<>();
    private static final Map<Mob, Flight> FLIGHTS = new WeakHashMap<>();
    private static final Map<Mob, Boolean> SUIT_VARIANTS = new WeakHashMap<>();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final int GROUNDED = 0;
    private static final int FLYING = 1;
    private static final int LANDING = 2;
    private static final int MAX_FLIGHT_TICKS = 600;
    private static final int LANDING_COOLDOWN = 60;
    private static final int GROUND_SCAN = 48;

    private ThematicFlight() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            NeoForge.EVENT_BUS.addListener(ThematicFlight::onTick);
        }
    }

    public static void clearCache() {
        FLYERS.clear();
    }

    private static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof DataMobEntity mob && !mob.level().isClientSide() && ThematicVariants.isHost(mob) && suitVariant(mob)) {
            tick(mob);
        }
    }

    private static boolean suitVariant(Mob mob) {
        synchronized (SUIT_VARIANTS) {
            return SUIT_VARIANTS.computeIfAbsent(mob, key -> {
                com.craisinlord.integrated_superheroes.voidrealm.variant.VariantProfile profile =
                        com.craisinlord.integrated_superheroes.voidrealm.variant.VariantProfile.of(key);
                return profile == null || profile.kind() == com.craisinlord.integrated_superheroes.voidrealm.variant.VariantKind.THEMATIC;
            });
        }
    }

    public static boolean flyer(CharacterItem suit) {
        if (suit == null) {
            return false;
        }
        return FLYERS.computeIfAbsent(suit.getArmorId(), id -> {
            try {
                JsonObject json = ArmorRegistry.resolvedDefinition(suit.getIdentifier());
                return json != null && (json.has("flight_type") || json.has("flight_profile"));
            } catch (RuntimeException ignored) {
                return false;
            }
        });
    }

    public static void tick(Mob mob) {
        if (!(mob instanceof DataMobEntity data) || mob.level().isClientSide()) {
            return;
        }
        Flight flight;
        synchronized (FLIGHTS) {
            flight = FLIGHTS.get(mob);
        }
        CharacterItem suit = data.suitId().isEmpty() ? null : ArmorRegistry.getArmor(data.suitId());
        if (!mob.isAlive() || !flyer(suit) || mob.isPassenger()) {
            if (flight != null) {
                if (flight.state != GROUNDED) {
                    ground(data);
                }
                synchronized (FLIGHTS) {
                    FLIGHTS.remove(mob);
                }
            }
            return;
        }
        if (flight == null) {
            flight = new Flight();
            if (mob.isNoGravity() || data.getFlightState() != HoverFlier.GROUNDED) {
                flight.state = LANDING;
            }
            synchronized (FLIGHTS) {
                FLIGHTS.put(mob, flight);
            }
        }
        LivingEntity target = data.attackTarget();
        if (target != null && (!target.isAlive() || target.level() != mob.level() || target.isRemoved())) {
            target = null;
        }
        switch (flight.state) {
            case FLYING -> fly(data, flight, target);
            case LANDING -> land(data, flight);
            default -> grounded(data, flight, target);
        }
    }

    private static void grounded(DataMobEntity mob, Flight flight, LivingEntity target) {
        if (flight.cooldown > 0) {
            flight.cooldown--;
            return;
        }
        if (target == null || mob.isInWater() && !target.isInWater()) {
            return;
        }
        double gap = target.getY() - mob.getY();
        double horizontal = horizontal(mob, target.position());
        boolean airborne = !target.onGround() && gap > 1.5D;
        boolean far = horizontal > 10.0D;
        boolean stuck = mob.getNavigation().isDone() && horizontal > 4.0D;
        boolean whim = horizontal > 5.0D && mob.getRandom().nextInt(80) == 0;
        if (gap > 2.5D || airborne || far || stuck || whim) {
            flight.state = FLYING;
            flight.ticks = 0;
            flight.home = mob.onGround() ? mob.blockPosition() : flight.home;
            mob.setNoGravity(true);
            mob.setFlightState(HoverFlier.ASCENDING);
            mob.setDeltaMovement(mob.getDeltaMovement().add(0.0D, 0.45D, 0.0D));
            mob.hasImpulse = true;
        }
    }

    private static void fly(DataMobEntity mob, Flight flight, LivingEntity target) {
        flight.ticks++;
        mob.setNoGravity(true);
        mob.fallDistance = 0.0F;
        if (target == null || flight.ticks > MAX_FLIGHT_TICKS || mob.isInWater() && flight.ticks > 20) {
            flight.state = LANDING;
            return;
        }
        double horizontal = horizontal(mob, target.position());
        double gap = target.getY() - mob.getY();
        boolean busy = mob.abilityController().isBusy();
        double height = target.onGround() ? (horizontal > 6.0D ? 2.5D : 0.6D) : 0.3D;
        Vec3 aim = target.position().add(0.0D, height, 0.0D);
        if (horizontal < 1.6D && Math.abs(gap) < 2.5D) {
            aim = new Vec3(mob.getX(), aim.y, mob.getZ());
        }
        Vec3 desired = aim.subtract(mob.position());
        double speed = busy ? 0.08D : horizontal > 14.0D ? 0.7D : horizontal > 6.0D ? 0.45D : 0.28D;
        Vec3 wanted = desired.lengthSqr() > 1.0E-4D ? desired.normalize().scale(Math.min(speed, desired.length() * 0.5D)) : Vec3.ZERO;
        Vec3 velocity = mob.getDeltaMovement();
        double lerp = 0.25D;
        mob.setDeltaMovement(velocity.add(wanted.subtract(velocity).scale(lerp)));
        mob.hasImpulse = true;
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        float yaw = (float) (Math.atan2(desired.z, desired.x) * (180.0D / Math.PI)) - 90.0F;
        if (horizontal > 2.0D) {
            mob.setYRot(yaw);
            mob.yBodyRot = yaw;
        }
        mob.setFlightState(HoverFlier.HOVERING);
        if (flight.ticks > 40 && !busy && target.onGround() && horizontal < 3.0D && Math.abs(gap) < 2.0D
                && mob.getRandom().nextInt(6) == 0) {
            flight.state = LANDING;
        }
    }

    private static void land(DataMobEntity mob, Flight flight) {
        mob.fallDistance = 0.0F;
        if (mob.onGround() || mob.isInWater() || mob.isInLava()) {
            ground(mob);
            flight.state = GROUNDED;
            flight.cooldown = LANDING_COOLDOWN;
            flight.ticks = 0;
            return;
        }
        mob.setFlightState(HoverFlier.DESCENDING);
        if (groundBelow(mob.level(), mob.blockPosition())) {
            mob.setNoGravity(false);
            Vec3 velocity = mob.getDeltaMovement();
            mob.setDeltaMovement(velocity.x * 0.8D, Math.max(velocity.y - 0.04D, -0.9D), velocity.z * 0.8D);
            return;
        }
        mob.setNoGravity(true);
        Vec3 velocity = mob.getDeltaMovement();
        if (flight.home != null) {
            Vec3 home = Vec3.atBottomCenterOf(flight.home).add(0.0D, 3.0D, 0.0D);
            Vec3 desired = home.subtract(mob.position());
            Vec3 wanted = desired.lengthSqr() > 1.0E-4D ? desired.normalize().scale(Math.min(0.45D, desired.length() * 0.5D)) : Vec3.ZERO;
            mob.setDeltaMovement(velocity.add(wanted.subtract(velocity).scale(0.25D)));
            mob.getNavigation().stop();
        } else {
            mob.setDeltaMovement(velocity.scale(0.6D));
        }
        mob.hasImpulse = true;
    }

    private static void ground(DataMobEntity mob) {
        mob.setNoGravity(false);
        mob.setFlightState(HoverFlier.GROUNDED);
        mob.fallDistance = 0.0F;
    }

    private static boolean groundBelow(Level level, BlockPos from) {
        BlockPos.MutableBlockPos cursor = from.mutable();
        int floor = level.getMinBuildHeight();
        for (int i = 0; i < GROUND_SCAN && cursor.getY() > floor; i++) {
            cursor.move(0, -1, 0);
            BlockState state = level.getBlockState(cursor);
            if (!state.getCollisionShape(level, cursor).isEmpty() || !state.getFluidState().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static double horizontal(Mob mob, Vec3 at) {
        double dx = at.x - mob.getX();
        double dz = at.z - mob.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static final class Flight {
        int state = GROUNDED;
        int ticks;
        int cooldown;
        BlockPos home;
    }
}
