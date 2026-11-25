package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TelekineticFormation {
    public static final String GRAVITY_TAG = "integrated_superheroes_tk_gravity";
    private static final int MAX_SLOTS = 3;
    private static final int BLOCKED_RELEASE_TICKS = 20;
    private static final int THROW_TICKS = 40;
    private static final Map<UUID, List<Hold>> OWNERS = ServerState.map(new HashMap<>());
    private static final Map<UUID, UUID> HELD = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> IMMUNE = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> FALL_GRACE = ServerState.map(new HashMap<>());
    private static final Map<UUID, Flight> THROWN = ServerState.map(new HashMap<>());
    private static final Map<UUID, LivingEntity> MOB_OWNERS = ServerState.map(new HashMap<>());
    private static final List<Runnable> CRUSHES = ServerState.all(new ArrayList<>());
    private static MinecraftServer server;
    private static boolean registered;

    public enum Result {
        HELD,
        BUSY,
        IMMUNE,
        FULL,
        INELIGIBLE
    }

    public record Style(int color, ParticleOptions sparkle) {
    }

    static final class Hold {
        final UUID target;
        final ResourceKey<Level> dimension;
        final boolean originalNoGravity;
        final long expiry;
        final boolean willing;
        final int slot;
        final float expireDamage;
        final float crush;
        final Style style;
        final long start;
        int blocked;
        boolean sneakLifted;

        Hold(UUID target, ResourceKey<Level> dimension, boolean originalNoGravity, long start, long expiry, boolean willing, int slot,
             float expireDamage, float crush, Style style) {
            this.target = target;
            this.dimension = dimension;
            this.originalNoGravity = originalNoGravity;
            this.start = start;
            this.expiry = expiry;
            this.willing = willing;
            this.slot = slot;
            this.expireDamage = expireDamage;
            this.crush = crush;
            this.style = style;
        }
    }

    private static final class Flight {
        final UUID owner;
        final ResourceKey<Level> dimension;
        final float damage;
        final boolean phoenix;
        final Set<UUID> hit = new HashSet<>();
        int age;

        Flight(UUID owner, ResourceKey<Level> dimension, float damage, boolean phoenix) {
            this.owner = owner;
            this.dimension = dimension;
            this.damage = damage;
            this.phoenix = phoenix;
        }
    }

    private TelekineticFormation() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(TelekineticFormation::onServerTick);
        NeoForge.EVENT_BUS.addListener(TelekineticFormation::onJoin);
        NeoForge.EVENT_BUS.addListener(TelekineticFormation::onFall);
        NeoForge.EVENT_BUS.addListener(TelekineticFormation::onStopping);
        PsychicLinks.addOtherControl(TelekineticFormation::isHeld);
    }

    public static boolean isHeld(Entity entity) {
        return entity != null && HELD.containsKey(entity.getUUID());
    }

    public static boolean claimedElsewhere(LivingEntity entity) {
        return isHeld(entity) || HeroGrip.isHeld(entity) || MartianPower.isThrall(entity) || PsychicLinks.isLinked(entity);
    }

    private static LivingEntity owner(UUID id) {
        ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(id);
        if (player != null) {
            return player;
        }
        LivingEntity mob = MOB_OWNERS.get(id);
        return mob == null || mob.isRemoved() ? null : mob;
    }

    private static DamageSource attack(LivingEntity owner) {
        return owner instanceof Player player ? owner.damageSources().playerAttack(player) : owner.damageSources().mobAttack(owner);
    }

    static boolean affects(LivingEntity owner, LivingEntity entity) {
        if (owner instanceof ServerPlayer player) {
            return KineticBurst.affects(player, entity);
        }
        return !entity.isSpectator() && HeroCaster.foe(owner, entity);
    }

    private static boolean suppressed(LivingEntity owner) {
        return owner instanceof Player player && (VillainWorld.suppressed(player) || HealingFactor.isDowned(player));
    }

    static int count(LivingEntity owner) {
        List<Hold> holds = OWNERS.get(owner.getUUID());
        return holds == null ? 0 : holds.size();
    }

    static boolean holding(LivingEntity owner, Entity entity) {
        return owner.getUUID().equals(HELD.get(entity.getUUID()));
    }

    private static long now() {
        return server == null ? 0L : server.getTickCount();
    }

    static boolean immune(Entity entity) {
        Long until = IMMUNE.get(entity.getUUID());
        return until != null && now() < until;
    }

    static Result hold(LivingEntity owner, LivingEntity target, int ticks, boolean willing, float expireDamage, int maxHeld, Style style) {
        return hold(owner, target, ticks, willing, expireDamage, 0.0F, maxHeld, style);
    }

    static Result hold(LivingEntity owner, LivingEntity target, int ticks, boolean willing, float expireDamage, float crush, int maxHeld, Style style) {
        if (server == null) {
            server = owner.getServer();
        }
        if (!(owner instanceof ServerPlayer)) {
            MOB_OWNERS.put(owner.getUUID(), owner);
        }
        if (target == owner || !target.isAlive() || target.isRemoved() || !HeroAbilities.canControl(target)) {
            return Result.INELIGIBLE;
        }
        if (claimedElsewhere(target)) {
            return Result.BUSY;
        }
        if (immune(target)) {
            return Result.IMMUNE;
        }
        List<Hold> holds = OWNERS.computeIfAbsent(owner.getUUID(), ignored -> new ArrayList<>());
        if (holds.size() >= Math.min(MAX_SLOTS, maxHeld)) {
            if (holds.isEmpty()) {
                OWNERS.remove(owner.getUUID());
            }
            return Result.FULL;
        }
        int slot = 0;
        while (slotTaken(holds, slot)) {
            slot++;
        }
        CompoundTag data = target.getPersistentData();
        boolean original = data.contains(GRAVITY_TAG) ? data.getBoolean(GRAVITY_TAG) : target.isNoGravity();
        data.putBoolean(GRAVITY_TAG, original);
        target.setNoGravity(true);
        target.fallDistance = 0.0F;
        holds.add(new Hold(target.getUUID(), owner.level().dimension(), original, now(), now() + ticks, willing, slot, expireDamage,
                willing ? 0.0F : crush, style));
        HELD.put(target.getUUID(), owner.getUUID());
        return Result.HELD;
    }

    private static boolean slotTaken(List<Hold> holds, int slot) {
        for (Hold hold : holds) {
            if (hold.slot == slot) {
                return true;
            }
        }
        return false;
    }

    static void setDownAll(LivingEntity owner) {
        List<Hold> holds = OWNERS.remove(owner.getUUID());
        if (holds == null) {
            return;
        }
        for (Hold hold : holds) {
            finish(hold, null);
        }
    }

    static int throwAll(LivingEntity owner, Vec3 direction, double speed, double perTarget, double cap) {
        List<Hold> holds = OWNERS.remove(owner.getUUID());
        if (holds == null) {
            return 0;
        }
        int hostile = 0;
        for (Hold hold : holds) {
            if (!hold.willing) {
                hostile++;
            }
        }
        float damage = hostile == 0 ? 0.0F : (float) Math.min(perTarget, cap / hostile);
        boolean phoenix = JeanGreyPower.ignited(owner);
        int thrown = 0;
        for (Hold hold : holds) {
            Entity entity = find(hold.target, hold.dimension);
            if (hold.willing || !(entity instanceof LivingEntity target) || !target.isAlive()) {
                finish(hold, null);
                continue;
            }
            HELD.remove(hold.target);
            restore(target);
            IMMUNE.put(hold.target, now() + JeanGreyConfig.CONTROL_IMMUNITY_TICKS.get());
            target.setDeltaMovement(direction.normalize().scale(speed).add(0.0D, 0.15D, 0.0D));
            target.hurtMarked = true;
            target.hasImpulse = true;
            THROWN.put(hold.target, new Flight(owner.getUUID(), hold.dimension, damage, phoenix));
            thrown++;
        }
        return thrown;
    }

    static void releaseOwner(UUID owner) {
        List<Hold> holds = OWNERS.remove(owner);
        if (holds == null) {
            return;
        }
        for (Hold hold : holds) {
            finish(hold, null);
        }
    }

    private static void finish(Hold hold, LivingEntity slam) {
        HELD.remove(hold.target);
        IMMUNE.put(hold.target, now() + JeanGreyConfig.CONTROL_IMMUNITY_TICKS.get());
        Entity entity = find(hold.target, hold.dimension);
        if (entity == null) {
            return;
        }
        restore(entity);
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * 0.2D, Math.min(motion.y, 0.0D) * 0.2D - 0.05D, motion.z * 0.2D);
        entity.hurtMarked = true;
        if (slam != null && hold.expireDamage > 0.0F && entity instanceof LivingEntity living && living.isAlive()) {
            entity.setDeltaMovement(0.0D, -1.2D, 0.0D);
            HeroAbilities.runAsAbilityDamage(() -> living.hurt(attack(slam), hold.expireDamage));
            if (entity.level() instanceof ServerLevel level) {
                Vec3 center = entity.getBoundingBox().getCenter();
                level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 10, 0.3D, 0.3D, 0.3D, 0.2D);
            }
            return;
        }
        FALL_GRACE.put(hold.target, now() + JeanGreyConfig.FALL_GRACE_TICKS.get());
        if (entity.level() instanceof ServerLevel level) {
            Vec3 center = entity.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 6, entity.getBbWidth() * 0.4D,
                    entity.getBbHeight() * 0.4D, entity.getBbWidth() * 0.4D, 0.01D);
        }
    }

    private static Entity find(UUID id, ResourceKey<Level> dimension) {
        if (server == null) {
            return null;
        }
        ServerLevel level = server.getLevel(dimension);
        Entity entity = level == null ? null : level.getEntity(id);
        if (entity != null) {
            return entity;
        }
        for (ServerLevel other : server.getAllLevels()) {
            entity = other.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static void restore(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.contains(GRAVITY_TAG)) {
            entity.setNoGravity(data.getBoolean(GRAVITY_TAG));
            data.remove(GRAVITY_TAG);
        }
        entity.fallDistance = 0.0F;
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        server = event.getServer();
        long now = now();
        if (!OWNERS.isEmpty()) {
            List<Hold> ended = new ArrayList<>();
            List<Hold> expired = new ArrayList<>();
            List<LivingEntity> slammers = new ArrayList<>();
            Iterator<Map.Entry<UUID, List<Hold>>> owners = OWNERS.entrySet().iterator();
            while (owners.hasNext()) {
                Map.Entry<UUID, List<Hold>> entry = owners.next();
                LivingEntity owner = owner(entry.getKey());
                boolean dropAll = owner == null || !owner.isAlive() || owner.isRemoved() || owner.isSpectator() || suppressed(owner);
                Iterator<Hold> holds = entry.getValue().iterator();
                while (holds.hasNext()) {
                    Hold hold = holds.next();
                    int verdict = dropAll ? 1 : tickHold(owner, hold, now);
                    if (verdict == 1) {
                        ended.add(hold);
                        holds.remove();
                    } else if (verdict == 2) {
                        expired.add(hold);
                        slammers.add(owner);
                        holds.remove();
                    }
                }
                if (entry.getValue().isEmpty()) {
                    owners.remove();
                }
            }
            List<Runnable> crushes = List.copyOf(CRUSHES);
            CRUSHES.clear();
            crushes.forEach(Runnable::run);
            for (int i = 0; i < expired.size(); i++) {
                finish(expired.get(i), slammers.get(i));
            }
            for (Hold hold : ended) {
                finish(hold, null);
            }
        }
        tickThrown(now);
        if (now % 200L == 0L) {
            IMMUNE.values().removeIf(until -> until < now);
            FALL_GRACE.values().removeIf(until -> until < now);
            MOB_OWNERS.entrySet().removeIf(entry -> (entry.getValue().isRemoved() || !entry.getValue().isAlive())
                    && !OWNERS.containsKey(entry.getKey()));
        }
    }

    private static int tickHold(LivingEntity owner, Hold hold, long now) {
        if (owner.level().dimension() != hold.dimension) {
            return 1;
        }
        ServerLevel level = (ServerLevel) owner.level();
        Entity entity = level.getEntity(hold.target);
        if (!(entity instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()) {
            return 1;
        }
        if (now >= hold.expiry) {
            return 2;
        }
        double leash = JeanGreyConfig.GRAB_RANGE.get() + 8.0D;
        if (target.distanceToSqr(owner) > leash * leash) {
            return 1;
        }
        if (hold.willing && target instanceof Player player) {
            if (!player.isShiftKeyDown()) {
                hold.sneakLifted = true;
            } else if (hold.sneakLifted) {
                return 1;
            }
            if (player.isCreative() || player.isSpectator()) {
                return 1;
            }
        }
        if (!level.noCollision(target, target.getBoundingBox().deflate(0.05D))) {
            return 1;
        }
        Vec3 goal = goal(owner, target, hold, now);
        if (goal == null) {
            hold.blocked++;
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;
            if (hold.blocked > BLOCKED_RELEASE_TICKS) {
                return 1;
            }
        } else {
            hold.blocked = 0;
            Vec3 pull = goal.subtract(target.position()).scale(0.35D);
            if (pull.length() > 0.9D) {
                pull = pull.normalize().scale(0.9D);
            }
            target.setDeltaMovement(pull);
            target.hurtMarked = true;
            target.hasImpulse = true;
        }
        if (!target.isNoGravity()) {
            target.setNoGravity(true);
        }
        target.fallDistance = 0.0F;
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        if (hold.crush > 0.0F && now > hold.start && (now - hold.start) % 20L == 0L) {
            boolean phoenix = JeanGreyPower.ignited(owner);
            float squeeze = phoenix ? hold.crush * 2.0F : hold.crush;
            if (phoenix) {
                target.igniteForSeconds(3.0F);
            }
            CRUSHES.add(() -> HeroAbilities.runAsAbilityDamage(() -> target.hurt(attack(owner), squeeze)));
            Vec3 center = target.getBoundingBox().getCenter();
            level.sendParticles(phoenix ? ParticleTypes.FLAME : ParticleTypes.CRIT, center.x, center.y, center.z, 6,
                    target.getBbWidth() * 0.4D, target.getBbHeight() * 0.4D, target.getBbWidth() * 0.4D, 0.05D);
        }
        if (owner.tickCount % 2 == 0) {
            Vec3 hand = owner.getEyePosition().add(owner.getLookAngle().scale(0.6D)).subtract(0.0D, 0.3D, 0.0D);
            Vec3 center = target.getBoundingBox().getCenter();
            HeroAbilities.particleLine(level, hand, center, HeroAbilities.emissive(hold.style.color(), 0.5F), 0.7D);
        }
        if (owner.tickCount % 4 == hold.slot) {
            Vec3 center = target.getBoundingBox().getCenter();
            level.sendParticles(hold.style.sparkle(), center.x, center.y, center.z, 2, target.getBbWidth() * 0.5D,
                    target.getBbHeight() * 0.5D, target.getBbWidth() * 0.5D, 0.0D);
        }
        return 0;
    }

    private static Vec3 goal(LivingEntity owner, LivingEntity target, Hold hold, long now) {
        Vec3 eye = owner.getEyePosition();
        Vec3 look = owner.getLookAngle();
        Vec3 reference = Math.abs(look.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = look.cross(reference).normalize();
        Vec3 up = right.cross(look).normalize();
        double halfWidths = owner.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D;
        double minReach = halfWidths + 1.0D;
        double reach = Math.max(minReach, 2.6D + halfWidths);
        double radius = 1.2D + target.getBbWidth() * 0.5D;
        double angle = now * 0.06D + hold.slot * (Math.PI * 2.0D / MAX_SLOTS);
        Vec3 ring = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
        double[][] options = {{reach, radius}, {reach, radius * 0.5D}, {minReach + 0.5D, radius * 0.5D}, {minReach + 0.5D, 0.0D}};
        AABB self = owner.getBoundingBox().inflate(0.3D);
        AABB box = target.getBoundingBox();
        for (double[] option : options) {
            Vec3 center = eye.add(look.scale(option[0])).add(ring.scale(option[1]));
            Vec3 position = center.subtract(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            AABB moved = box.move(position.subtract(target.position()));
            if (!moved.intersects(self) && owner.level().noCollision(target, moved) && clearOfOthers(owner, target, moved)) {
                return position;
            }
        }
        return null;
    }

    private static boolean clearOfOthers(LivingEntity owner, LivingEntity target, AABB moved) {
        List<Hold> holds = OWNERS.get(owner.getUUID());
        if (holds == null) {
            return true;
        }
        for (Hold other : holds) {
            if (other.target.equals(target.getUUID())) {
                continue;
            }
            Entity entity = ((ServerLevel) owner.level()).getEntity(other.target);
            if (entity != null && entity.getBoundingBox().intersects(moved)) {
                return false;
            }
        }
        return true;
    }

    private static void tickThrown(long now) {
        if (THROWN.isEmpty() || server == null) {
            return;
        }
        Iterator<Map.Entry<UUID, Flight>> iterator = THROWN.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Flight> entry = iterator.next();
            Flight flight = entry.getValue();
            flight.age++;
            ServerLevel level = server.getLevel(flight.dimension);
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            LivingEntity owner = owner(flight.owner);
            if (!(entity instanceof LivingEntity target) || !target.isAlive() || owner == null || flight.age > THROW_TICKS) {
                iterator.remove();
                continue;
            }
            boolean impact = target.horizontalCollision || target.verticalCollision && flight.age > 3;
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(0.4D),
                    candidate -> candidate != target && candidate != owner && candidate.isAlive() && !flight.hit.contains(candidate.getUUID())
                            && affects(owner, candidate))) {
                flight.hit.add(other.getUUID());
                HeroAbilities.runAsAbilityDamage(() -> other.hurt(attack(owner), flight.damage));
                impact = true;
            }
            if (impact) {
                HeroAbilities.runAsAbilityDamage(() -> target.hurt(attack(owner), flight.damage));
                Vec3 center = target.getBoundingBox().getCenter();
                level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 12, 0.3D, 0.3D, 0.3D, 0.25D);
                if (flight.phoenix) {
                    JeanGreyPower.phoenixBurst(owner, target, center, flight.damage);
                }
                iterator.remove();
            } else if (flight.age % 2 == 0) {
                Vec3 center = target.getBoundingBox().getCenter();
                level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
            }
        }
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide() || !entity.getPersistentData().contains(GRAVITY_TAG)) {
            return;
        }
        UUID owner = HELD.get(entity.getUUID());
        if (owner != null) {
            List<Hold> holds = OWNERS.get(owner);
            if (holds != null) {
                for (Hold hold : holds) {
                    if (hold.target.equals(entity.getUUID()) && hold.dimension == event.getLevel().dimension()) {
                        return;
                    }
                }
            }
        }
        restore(entity);
    }

    private static void onFall(LivingIncomingDamageEvent event) {
        if (FALL_GRACE.isEmpty() || !event.getSource().is(DamageTypeTags.IS_FALL)) {
            return;
        }
        Long until = FALL_GRACE.get(event.getEntity().getUUID());
        if (until != null && now() <= until) {
            FALL_GRACE.remove(event.getEntity().getUUID());
            event.setCanceled(true);
        }
    }

    private static void onStopping(ServerStoppingEvent event) {
        server = event.getServer();
        for (UUID owner : new ArrayList<>(OWNERS.keySet())) {
            releaseOwner(owner);
        }
        OWNERS.clear();
        HELD.clear();
        IMMUNE.clear();
        FALL_GRACE.clear();
        THROWN.clear();
        MOB_OWNERS.clear();
        server = null;
    }
}
