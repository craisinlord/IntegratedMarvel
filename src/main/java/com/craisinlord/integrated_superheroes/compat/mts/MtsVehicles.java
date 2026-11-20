package com.craisinlord.integrated_superheroes.compat.mts;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.voidworld.vehicles.VehicleState;
import mcinterface1211.WrapperEntity;
import mcinterface1211.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.entities.instances.PartPropeller;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.JSONCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class MtsVehicles {
    private static final List<String> REQUIRED_SLOTS = List.of("ground_", "seat", "engine_", "propeller");
    private static final String PONTOON = "ground_pontoon";
    private static final int MAX_DEPTH = 3;
    private static final int ATTEMPTS = 4;
    private static List<List<ItemVehicle>> groups;
    private static List<AItemPart> parts;
    private static final Set<String> BROKEN = new HashSet<>();

    private MtsVehicles() {
    }

    private static void index() {
        if (groups != null) {
            return;
        }
        Map<String, List<ItemVehicle>> found = new LinkedHashMap<>();
        List<AItemPart> foundParts = new ArrayList<>();
        for (AItemPack<?> item : PackParser.getAllPackItems()) {
            if (item instanceof ItemVehicle vehicle) {
                if (drivable(vehicle.definition)) {
                    found.computeIfAbsent(name(vehicle.definition), key -> new ArrayList<>()).add(vehicle);
                }
            } else if (item instanceof AItemPart part && part.definition.generic != null && part.definition.generic.type != null
                    && !part.definition.generic.type.equals(PONTOON)) {
                foundParts.add(part);
            }
        }
        if (found.isEmpty()) {
            return;
        }
        parts = foundParts;
        groups = new ArrayList<>(found.values());
    }

    private static String name(JSONVehicle definition) {
        return definition.packID + ":" + definition.systemName;
    }

    private static boolean drivable(JSONVehicle definition) {
        if (definition.motorized == null || definition.motorized.isTrailer || definition.parts == null) {
            return false;
        }
        boolean controller = false;
        boolean engine = false;
        boolean ground = definition.motorized.isBlimp;
        for (JSONPartDefinition slot : definition.parts) {
            if (slot.types == null) {
                continue;
            }
            for (String type : slot.types) {
                if (type.startsWith("seat") && slot.isController) {
                    controller = true;
                } else if (type.startsWith("engine_")) {
                    engine = true;
                } else if (type.startsWith("ground_") && !type.equals(PONTOON)) {
                    ground = true;
                }
            }
        }
        return controller && engine && ground;
    }

    @Nullable
    public static UUID spawn(ServerLevel level, Vec3 ground, Vec3 facing, double lift, RandomSource random, Predicate<List<String>> filter, boolean aircraft, double fuel) {
        List<List<ItemVehicle>> pool = pool(filter, aircraft);
        if (pool.isEmpty() && aircraft) {
            pool = pool(filter, false);
        }
        for (int attempt = 0; attempt < ATTEMPTS && !pool.isEmpty(); attempt++) {
            List<ItemVehicle> variants = pool.remove(random.nextInt(pool.size()));
            ItemVehicle item = variants.get(random.nextInt(variants.size()));
            EntityVehicleF_Physics vehicle = build(level, item, ground, facing, lift, random, fuel);
            if (vehicle != null) {
                return vehicle.uniqueUUID;
            }
        }
        return null;
    }

    private static List<List<ItemVehicle>> pool(Predicate<List<String>> filter, boolean aircraft) {
        index();
        List<List<ItemVehicle>> pool = new ArrayList<>();
        if (groups == null) {
            return pool;
        }
        for (List<ItemVehicle> group : groups) {
            JSONVehicle definition = group.get(0).definition;
            if (definition.motorized.isAircraft != aircraft || BROKEN.contains(name(definition))) {
                continue;
            }
            List<ItemVehicle> allowed = new ArrayList<>();
            for (ItemVehicle item : group) {
                if (filter.test(List.of(name(definition), "mts:" + item.getRegistrationName()))) {
                    allowed.add(item);
                }
            }
            if (!allowed.isEmpty()) {
                pool.add(allowed);
            }
        }
        return pool;
    }

    @Nullable
    private static EntityVehicleF_Physics build(ServerLevel level, ItemVehicle item, Vec3 ground, Vec3 facing, double lift, RandomSource random, double fuel) {
        EntityVehicleF_Physics vehicle;
        try {
            vehicle = new EntityVehicleF_Physics(WrapperWorld.getWrapperFor(level), null, item, null);
            vehicle.position.set(ground.x, ground.y, ground.z);
            vehicle.prevPosition.set(vehicle.position);
            vehicle.orientation.setToVector(new Point3D(facing.x, 0.0D, facing.z), true);
            vehicle.prevOrientation.set(vehicle.orientation);
            vehicle.motion.set(0.0D, 0.0D, 0.0D);
            vehicle.prevMotion.set(vehicle.motion);
            vehicle.world.spawnEntity(vehicle);
        } catch (Exception e) {
            IntegratedSuperheroes.LOGGER.warn("Couldn't create vehicle {}", item.getRegistrationName(), e);
            BROKEN.add(name(item.definition));
            return null;
        }
        try {
            vehicle.addPartsPostAddition(null, null);
            Map<String, AItemPart> picks = new HashMap<>();
            for (APart part : vehicle.allParts) {
                if (part instanceof PartEngine && part.cachedItem instanceof AItemPart engine) {
                    picks.putIfAbsent("engine", engine);
                }
            }
            fill(vehicle, random, picks, 0);
            if (!working(vehicle)) {
                BROKEN.add(name(item.definition));
                vehicle.remove();
                return null;
            }
            vehicle.position.y = ground.y + lift - lowestPoint(vehicle);
            vehicle.prevPosition.set(vehicle.position);
            if (!fits(level, vehicle, ground.y)) {
                vehicle.remove();
                return null;
            }
            refuel(vehicle, fuel);
            vehicle.brakeVar.setTo(0.0D, false);
            vehicle.parkingBrakeVar.setTo(1.0D, false);
        } catch (Exception e) {
            IntegratedSuperheroes.LOGGER.warn("Couldn't fit out vehicle {}", item.getRegistrationName(), e);
            BROKEN.add(name(item.definition));
            vehicle.remove();
            return null;
        }
        return vehicle;
    }

    private static double lowestPoint(EntityVehicleF_Physics vehicle) {
        double lowest = 0.0D;
        if (vehicle.definition.collisionGroups != null) {
            for (JSONCollisionGroup group : vehicle.definition.collisionGroups) {
                for (JSONCollisionBox box : group.collisions) {
                    lowest = Math.min(lowest, box.pos.y - box.height / 2.0D);
                }
            }
        }
        for (APart part : vehicle.allParts) {
            lowest = Math.min(lowest, part.placementDefinition.pos.y - part.getHeight() / 2.0D);
        }
        return lowest - 0.1D;
    }

    private static boolean fits(ServerLevel level, EntityVehicleF_Physics vehicle, double groundY) {
        if (vehicle.definition.collisionGroups != null) {
            for (JSONCollisionGroup group : vehicle.definition.collisionGroups) {
                if (group.collisionTypes == null || !group.collisionTypes.contains(JSONCollisionGroup.CollisionType.BLOCK)) {
                    continue;
                }
                for (JSONCollisionBox box : group.collisions) {
                    Point3D center = box.pos.copy().rotate(vehicle.orientation).add(vehicle.position);
                    double w = Math.max(0.0D, box.width / 2.0D - 0.1D);
                    double h = Math.max(0.0D, box.height / 2.0D - 0.1D);
                    AABB aabb = new AABB(center.x - w, center.y - h, center.z - w, center.x + w, center.y + h, center.z + w);
                    if (level.getBlockCollisions(null, aabb).iterator().hasNext()) {
                        return false;
                    }
                }
            }
        }
        int floor = Mth.floor(groundY);
        for (APart part : vehicle.parts) {
            if (!(part instanceof PartGroundDevice)) {
                continue;
            }
            Point3D at = part.placementDefinition.pos.copy().rotate(vehicle.orientation).add(vehicle.position);
            int x = Mth.floor(at.x);
            int z = Mth.floor(at.z);
            if (!level.hasChunkAt(new BlockPos(x, floor, z))) {
                return false;
            }
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(top - floor) > 1 || !level.getFluidState(new BlockPos(x, top - 1, z)).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean needed(JSONPartDefinition slot) {
        if (slot.isSpare || slot.types == null) {
            return false;
        }
        for (String type : slot.types) {
            if (type.equals(PONTOON)) {
                continue;
            }
            for (String prefix : REQUIRED_SLOTS) {
                if (type.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void fill(AEntityF_Multipart<?> entity, RandomSource random, Map<String, AItemPart> picks, int depth) {
        if (depth > MAX_DEPTH || entity.definition.parts == null) {
            return;
        }
        List<JSONPartDefinition> slots = entity.definition.parts;
        for (int i = 0; i < slots.size() && i < entity.partsInSlots.size(); i++) {
            JSONPartDefinition slot = slots.get(i);
            if (entity.partsInSlots.get(i) != null || !needed(slot)) {
                continue;
            }
            AItemPart pick = pick(entity, slot, random, picks);
            if (pick != null) {
                entity.addPartFromStack(pick.getNewStack(null), null, i, false, true);
            }
        }
        for (APart part : new ArrayList<>(entity.parts)) {
            fill(part, random, picks, depth + 1);
        }
    }

    @Nullable
    private static AItemPart pick(AEntityF_Multipart<?> entity, JSONPartDefinition slot, RandomSource random, Map<String, AItemPart> picks) {
        String key = slot.types + "|" + slot.customTypes + "|" + slot.minValue + "|" + slot.maxValue + "|" + slot.validSubNames;
        AItemPart previous = picks.get(key);
        if (previous != null && previous.isPartValidForPackDef(slot, entity.subDefinition, !slot.bypassSlotMinMax)) {
            return previous;
        }
        AItemPart engine = picks.get("engine");
        List<AItemPart> same = new ArrayList<>();
        List<AItemPart> other = new ArrayList<>();
        for (AItemPart part : parts) {
            if (!part.isPartValidForPackDef(slot, entity.subDefinition, !slot.bypassSlotMinMax)) {
                continue;
            }
            if (engine != null && part.definition.engine != null && !sameFuel(engine.definition.engine, part.definition.engine)) {
                continue;
            }
            (part.definition.packID.equals(entity.definition.packID) ? same : other).add(part);
        }
        List<AItemPart> from = same.isEmpty() ? other : same;
        if (from.isEmpty()) {
            return null;
        }
        AItemPart chosen = from.get(random.nextInt(from.size()));
        picks.put(key, chosen);
        if (chosen.definition.engine != null) {
            picks.putIfAbsent("engine", chosen);
        }
        return chosen;
    }

    private static boolean sameFuel(JSONPart.JSONPartEngine a, JSONPart.JSONPartEngine b) {
        return a.type == b.type && (a.type != JSONPart.EngineType.NORMAL || String.valueOf(a.fuelType).equals(String.valueOf(b.fuelType)));
    }

    private static boolean working(EntityVehicleF_Physics vehicle) {
        boolean engine = false;
        boolean controller = false;
        boolean ground = false;
        boolean thrust = false;
        for (APart part : vehicle.allParts) {
            if (part instanceof PartEngine) {
                engine = true;
                if (part.definition.engine != null && part.definition.engine.jetPowerFactor > 0.0F) {
                    thrust = true;
                }
            } else if (part instanceof PartSeat && part.placementDefinition.isController) {
                controller = true;
            } else if (part instanceof PartGroundDevice) {
                ground = true;
            } else if (part instanceof PartPropeller) {
                thrust = true;
            }
        }
        if (!engine || !controller) {
            return false;
        }
        return vehicle.definition.motorized.isAircraft ? thrust && (ground || vehicle.definition.motorized.isBlimp) : ground;
    }

    private static void refuel(EntityVehicleF_Physics vehicle, double fraction) {
        int capacity = vehicle.definition.motorized.fuelCapacity;
        if (capacity <= 0) {
            return;
        }
        for (APart part : vehicle.allParts) {
            if (!(part instanceof PartEngine) || part.definition.engine == null) {
                continue;
            }
            ResourceLocation fluid = switch (part.definition.engine.type) {
                case MAGIC -> null;
                case ELECTRIC -> ResourceLocation.fromNamespaceAndPath("wildcard", PartEngine.ELECTRICITY_FUEL);
                default -> fluid(part.definition.engine.fuelType);
            };
            if (fluid != null) {
                double amount = Math.max(1.0D, Mth.clamp(fraction, 0.0D, 1.0D) * capacity);
                vehicle.fuelTank.manuallySet(fluid.getPath(), fluid.getNamespace(), amount);
            }
            return;
        }
    }

    @Nullable
    private static ResourceLocation fluid(@Nullable String fuelType) {
        if (fuelType == null || ConfigSystem.settings == null || ConfigSystem.settings.fuel == null || ConfigSystem.settings.fuel.fuels == null) {
            return null;
        }
        Map<String, Double> fuels = ConfigSystem.settings.fuel.fuels.get(fuelType);
        if (fuels == null) {
            return null;
        }
        ResourceLocation best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (ResourceLocation id : BuiltInRegistries.FLUID.keySet()) {
            Double factor = fuels.get(id.getPath());
            if (factor == null || id.getPath().startsWith("flowing_")) {
                continue;
            }
            double score = factor - (id.getPath().contains("lava") ? 0.5D : 0.0D);
            if (score > bestScore) {
                bestScore = score;
                best = id;
            }
        }
        return best;
    }

    @Nullable
    private static EntityVehicleF_Physics find(ServerLevel level, UUID id) {
        AEntityA_Base entity = WrapperWorld.getWrapperFor(level).getEntity(id);
        return entity instanceof EntityVehicleF_Physics vehicle && vehicle.isValid ? vehicle : null;
    }

    @Nullable
    public static VehicleState state(ServerLevel level, UUID id) {
        EntityVehicleF_Physics vehicle = find(level, id);
        if (vehicle == null) {
            return null;
        }
        boolean player = false;
        boolean others = false;
        for (APart part : vehicle.allParts) {
            if (part.rider instanceof IWrapperPlayer) {
                player = true;
            } else if (part.rider != null) {
                others = true;
            }
        }
        double speed = vehicle.motion.length() * vehicle.speedFactor;
        return new VehicleState(new Vec3(vehicle.position.x, vehicle.position.y, vehicle.position.z), player, others, speed);
    }

    @Nullable
    public static String describe(ServerLevel level, UUID id) {
        EntityVehicleF_Physics vehicle = find(level, id);
        return vehicle == null ? null : "mts:" + vehicle.definition.packID + "." + vehicle.definition.systemName + vehicle.subDefinition.subName;
    }

    public static void push(ServerLevel level, UUID id, Vec3 blocksPerTick) {
        EntityVehicleF_Physics vehicle = find(level, id);
        if (vehicle == null || vehicle.speedFactor <= 0.0D) {
            return;
        }
        vehicle.parkingBrakeVar.setTo(0.0D, true);
        vehicle.motion.set(blocksPerTick.x / vehicle.speedFactor, blocksPerTick.y / vehicle.speedFactor, blocksPerTick.z / vehicle.speedFactor);
        vehicle.prevMotion.set(vehicle.motion);
    }

    public static void brake(ServerLevel level, UUID id, double amount) {
        EntityVehicleF_Physics vehicle = find(level, id);
        if (vehicle != null) {
            vehicle.brakeVar.setTo(amount, true);
        }
    }

    public static void park(ServerLevel level, UUID id) {
        EntityVehicleF_Physics vehicle = find(level, id);
        if (vehicle != null) {
            vehicle.brakeVar.setTo(0.0D, true);
            vehicle.parkingBrakeVar.setTo(1.0D, true);
        }
    }

    public static boolean seat(ServerLevel level, UUID id, Entity rider) {
        EntityVehicleF_Physics vehicle = find(level, id);
        if (vehicle == null) {
            return false;
        }
        PartSeat fallback = null;
        for (APart part : vehicle.allParts) {
            if (part instanceof PartSeat seat && seat.rider == null) {
                if (seat.placementDefinition.isController) {
                    return seat.setRider(WrapperEntity.getWrapperFor(rider), true);
                }
                if (fallback == null) {
                    fallback = seat;
                }
            }
        }
        return fallback != null && fallback.setRider(WrapperEntity.getWrapperFor(rider), true);
    }

    public static void eject(ServerLevel level, UUID id) {
        EntityVehicleF_Physics vehicle = find(level, id);
        if (vehicle == null) {
            return;
        }
        for (APart part : new ArrayList<>(vehicle.allParts)) {
            if (part.rider != null && !(part.rider instanceof IWrapperPlayer)) {
                part.removeRider();
            }
        }
    }

    public static void remove(ServerLevel level, UUID id) {
        EntityVehicleF_Physics vehicle = find(level, id);
        if (vehicle != null) {
            vehicle.remove();
        }
    }
}
