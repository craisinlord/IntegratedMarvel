package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.world.level.Level;
import com.craisinlord.integrated_superheroes.gauntlet.GauntletFx;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.Power;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tintankgames.marvel.core.particles.EmissiveDustParticleOptions;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class HeroAbilities {
    private static final TagKey<EntityType<?>> NO_CONTROL = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("integrated_superheroes", "no_control"));
    private static final String LIVING_VILLAIN_KEY = "integrated_superheroes_living_villain";
    private static final int MAX_LINE_POINTS = 256;
    private static ResourceManager bossSource;
    private static Set<ResourceLocation> dataBosses = Set.of();
    private static final Map<UUID, Dash> DASHES = ServerState.map(new HashMap<>());
    private static final ThreadLocal<Boolean> ABILITY_DAMAGE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> SUIT_DAMAGE = ThreadLocal.withInitial(() -> false);

    static {
        ServerState.onStop(() -> bossSource = null);
    }

    private HeroAbilities() {
    }

    public static void runAsAbilityDamage(Runnable action) {
        boolean previous = ABILITY_DAMAGE.get();
        ABILITY_DAMAGE.set(true);
        try {
            action.run();
        } finally {
            ABILITY_DAMAGE.set(previous);
        }
    }

    public static void runAsSuitDamage(Runnable action) {
        boolean previous = SUIT_DAMAGE.get();
        SUIT_DAMAGE.set(true);
        try {
            runAsAbilityDamage(action);
        } finally {
            SUIT_DAMAGE.set(previous);
        }
    }

    public static boolean isSuitDamage() {
        return SUIT_DAMAGE.get();
    }

    public static boolean isAbilityDamage() {
        return ABILITY_DAMAGE.get();
    }

    public static void startDash(LivingEntity player, Vec3 velocity, int ticks, double damage, double knockback) {
        startDash(player, velocity, ticks, damage, knockback, entity -> true);
    }

    public static void startDash(LivingEntity player, Vec3 velocity, int ticks, double damage, double knockback, Predicate<LivingEntity> canHit) {
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        DASHES.put(player.getUUID(), new Dash(ticks, damage, knockback, new HashSet<>(), canHit, HeroAdvancements.current(), isSuitDamage()));
    }

    public static void tickDash(LivingEntity player) {
        Dash dash = DASHES.get(player.getUUID());
        if (dash == null) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(0.8D),
                entity -> HeroCaster.foe(player, entity) && !dash.hit().contains(entity.getUUID()) && dash.canHit().test(entity))) {
            dash.hit().add(target.getUUID());
            HeroAdvancements.within(dash.cast(), () -> {
                if (dash.suit()) {
                    runAsSuitDamage(() -> HeroHits.hurt(player, target, player.damageSources().mobAttack(player), (float) dash.damage()));
                } else {
                    HeroHits.hurt(player, target, player.damageSources().mobAttack(player), (float) dash.damage());
                }
            });
            Vec3 push = motion.lengthSqr() > 1.0E-4D ? motion.normalize() : player.getLookAngle();
            target.knockback(dash.knockback(), -push.x, -push.z);
        }
        if (dash.ticksLeft() <= 1) {
            DASHES.remove(player.getUUID());
        } else {
            DASHES.put(player.getUUID(), new Dash(dash.ticksLeft() - 1, dash.damage(), dash.knockback(), dash.hit(), dash.canHit(), dash.cast(), dash.suit()));
        }
    }

    public static void clearDash(LivingEntity player) {
        DASHES.remove(player.getUUID());
    }

    public static boolean canControl(LivingEntity entity) {
        if (entity.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES) || entity.getType().is(NO_CONTROL)
                || entity.getPersistentData().contains(LIVING_VILLAIN_KEY) || dataBoss(entity)) {
            return false;
        }
        return !(entity instanceof net.minecraft.world.entity.player.Player player) || !player.isCreative() && !player.isSpectator();
    }

    public static boolean isBoss(LivingEntity entity) {
        return entity.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)
                || entity.getPersistentData().contains(LIVING_VILLAIN_KEY)
                || entity.getPersistentData().getBoolean(com.craisinlord.integrated_superheroes.villain.VillainBrain.MANAGED)
                && !(entity instanceof net.tintankgames.marvel.world.entity.HydraAgent)
                || dataBoss(entity);
    }

    private static boolean dataBoss(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!"thematic".equals(id.getNamespace())) {
            return false;
        }
        ResourceManager manager = level.getServer().getResourceManager();
        if (manager != bossSource) {
            bossSource = manager;
            Set<ResourceLocation> found = new HashSet<>();
            for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("entities",
                    file -> "thematic".equals(file.getNamespace()) && file.getPath().endsWith(".json")).entrySet()) {
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement json = JsonParser.parseReader(reader);
                    if (json.isJsonObject() && json.getAsJsonObject().has("boss") && json.getAsJsonObject().get("boss").getAsBoolean()) {
                        String path = entry.getKey().getPath();
                        found.add(ResourceLocation.fromNamespaceAndPath(entry.getKey().getNamespace(),
                                path.substring("entities/".length(), path.length() - ".json".length())));
                    }
                } catch (Exception ignored) {
                }
            }
            dataBosses = found;
        }
        return dataBosses.contains(id);
    }

    public static List<LivingEntity> cone(LivingEntity player, double range, double halfAngleDegrees) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double minDot = Math.cos(Math.toRadians(halfAngleDegrees));
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), entity -> {
            if (!HeroCaster.foe(player, entity)) {
                return false;
            }
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            double distance = to.length();
            return distance <= range && (distance < 1.0E-3D || to.normalize().dot(look) >= minDot) && player.hasLineOfSight(entity);
        });
    }

    public static List<LivingEntity> sphere(LivingEntity player, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                entity -> HeroCaster.foe(player, entity) && entity.distanceToSqr(player) <= radius * radius);
    }

    public static Beam beam(LivingEntity player, Vec3 direction, double range, int maxTargets) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(direction.scale(range));
        HitResult blockHit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        Vec3 finalEnd = end;
        AABB area = new AABB(start, end).inflate(1.0D);
        List<Entity> candidates = new ArrayList<>(player.level().getEntities(player, area,
                entity -> HeroCaster.foe(player, entity)
                        && entity.getBoundingBox().inflate(0.3D).clip(start, finalEnd).isPresent()));
        candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(start)));
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : candidates) {
            if (targets.size() >= maxTargets) {
                break;
            }
            targets.add((LivingEntity) entity);
        }
        Vec3 beamEnd = !targets.isEmpty() && maxTargets == 1 ? targets.get(0).getBoundingBox().getCenter() : finalEnd;
        return new Beam(start, beamEnd, targets, blockHit.getType() != HitResult.Type.MISS);
    }

    public static void particleLine(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double spacing) {
        double length = from.distanceTo(to);
        if (length < 1.0E-4D) {
            return;
        }
        GauntletFx.line(level, particle, from, to, Math.min(MAX_LINE_POINTS, (int) (length / spacing)), 0.0F);
    }

    public static boolean gone(ServerLevel level, int entityId) {
        if (level.dimension() != Level.OVERWORLD) {
            return false;
        }
        for (ServerLevel other : level.getServer().getAllLevels()) {
            if (other.getEntity(entityId) != null) {
                return false;
            }
        }
        return true;
    }

    public static ParticleOptions emissive(int color, float size) {
        return new EmissiveDustParticleOptions(color, size);
    }

    public static Power slotPower(ServerPlayer player, String abilityName) {
        for (Power power : HeroPowers.data(player).getActivePowers()) {
            if (power != null && abilityName.equals(power.getName())) {
                return power;
            }
        }
        return null;
    }

    public static void setCooldown(ServerPlayer player, String abilityName, double seconds) {
        Power power = slotPower(player, abilityName);
        if (power != null) {
            power.setCooldown(seconds);
            PlayerData.sync(player);
        }
    }

    public static void setActive(ServerPlayer player, String abilityName, boolean active) {
        Power power = slotPower(player, abilityName);
        if (power != null) {
            power.setActive(active);
            PlayerData.sync(player);
        }
    }

    public static void clearActive(ServerPlayer player, String abilityName) {
        Power power = slotPower(player, abilityName);
        if (power != null && power.isActive()) {
            power.setActive(false);
            PlayerData.sync(player);
        }
    }

    public record Beam(Vec3 start, Vec3 end, List<LivingEntity> targets, boolean hitBlock) {
    }

    private record Dash(int ticksLeft, double damage, double knockback, Set<UUID> hit, Predicate<LivingEntity> canHit,
                        HeroAdvancements.Cast cast, boolean suit) {
    }
}
