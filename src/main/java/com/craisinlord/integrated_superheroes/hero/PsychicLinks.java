package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class PsychicLinks {
    public static final String OWNER_KEY = "integrated_superheroes_psychic_owner";
    public static final String UNTIL_KEY = "integrated_superheroes_psychic_until";
    private static final int SCAN_INTERVAL = 5;
    private static final int TETHER_COLOR = 0xB57CFF;
    private static final Map<UUID, Link> LINKS = ServerState.map(new HashMap<>());
    private static final Map<UUID, Memory> STRUCK = ServerState.map(new HashMap<>());
    private static final Map<UUID, Memory> STRUCK_BY = ServerState.map(new HashMap<>());
    private static final List<Predicate<Entity>> OTHER_CONTROL = new ArrayList<>();
    private static boolean registered;

    private record Memory(UUID entity, long at) {
    }

    private static final class Link {
        private final UUID owner;
        private final UUID mob;
        private final ResourceKey<Level> dimension;
        private final boolean borrowed;
        private long expires;
        private Mob entity;
        private Goal targetGoal;
        private Goal followGoal;

        private Link(UUID owner, UUID mob, ResourceKey<Level> dimension, boolean borrowed, long expires) {
            this.owner = owner;
            this.mob = mob;
            this.dimension = dimension;
            this.borrowed = borrowed;
            this.expires = expires;
        }
    }

    private PsychicLinks() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(PsychicLinks::onServerTick);
        NeoForge.EVENT_BUS.addListener(PsychicLinks::onTarget);
        NeoForge.EVENT_BUS.addListener(PsychicLinks::onDamage);
        NeoForge.EVENT_BUS.addListener(PsychicLinks::onJoin);
        NeoForge.EVENT_BUS.addListener(PsychicLinks::onLeave);
        NeoForge.EVENT_BUS.addListener(PsychicLinks::onDeath);
        NeoForge.EVENT_BUS.addListener(PsychicLinks::onLogout);
        NeoForge.EVENT_BUS.addListener(PsychicLinks::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(PsychicLinks::onStopping);
    }

    public static void addOtherControl(Predicate<Entity> check) {
        OTHER_CONTROL.add(check);
    }

    public static boolean isLinked(Entity entity) {
        return entity != null && LINKS.containsKey(entity.getUUID());
    }

    public static UUID ownerOf(Entity entity) {
        Link link = entity == null ? null : LINKS.get(entity.getUUID());
        return link == null ? null : link.owner;
    }

    public static boolean isControlledElsewhere(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (HeroGrip.isHeld(entity) || MartianPower.isThrall(entity)) {
            return true;
        }
        for (Predicate<Entity> check : OTHER_CONTROL) {
            try {
                if (check.test(entity)) {
                    return true;
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return false;
    }

    public static int count(ServerPlayer owner, boolean borrowed) {
        int count = 0;
        for (Link link : LINKS.values()) {
            if (link.owner.equals(owner.getUUID()) && link.borrowed == borrowed) {
                count++;
            }
        }
        return count;
    }

    public static List<Mob> linked(ServerPlayer owner) {
        List<Mob> mobs = new ArrayList<>();
        for (Link link : LINKS.values()) {
            if (link.owner.equals(owner.getUUID()) && link.entity != null && link.entity.isAlive()) {
                mobs.add(link.entity);
            }
        }
        return mobs;
    }

    public static boolean eligible(ServerPlayer owner, LivingEntity target) {
        if (!(target instanceof Mob mob) || target instanceof Player || !mob.isAlive() || mob.isNoAi()) {
            return false;
        }
        if (!(mob instanceof Enemy) || mob instanceof AbstractVillager || mob instanceof Merchant || mob instanceof Npc) {
            return false;
        }
        if (!HeroAbilities.canControl(mob) || HeroAbilities.isBoss(mob)) {
            return false;
        }
        if (mob instanceof TamableAnimal tame && tame.isTame()) {
            return false;
        }
        if (mob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return false;
        }
        if (mob instanceof TraceableEntity traceable && traceable.getOwner() != null) {
            return false;
        }
        if (mob.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)) {
            return false;
        }
        if (owner != null && (owner.isAlliedTo(mob) || mob.isAlliedTo(owner))) {
            return false;
        }
        return !isControlledElsewhere(mob);
    }

    public static boolean canLink(ServerPlayer owner, LivingEntity target) {
        if (!eligible(owner, target)) {
            return false;
        }
        Link existing = LINKS.get(target.getUUID());
        return existing == null || existing.owner.equals(owner.getUUID()) && !existing.borrowed;
    }

    public static boolean link(ServerPlayer owner, Mob mob, int ticks, int cap, boolean borrowed) {
        if (!canLink(owner, mob)) {
            return false;
        }
        long now = owner.serverLevel().getGameTime();
        Link existing = LINKS.get(mob.getUUID());
        if (existing != null) {
            existing.expires = now + ticks;
            mark(mob, existing);
            return true;
        }
        while (count(owner, borrowed) >= cap) {
            Link oldest = null;
            for (Link link : LINKS.values()) {
                if (link.owner.equals(owner.getUUID()) && link.borrowed == borrowed && (oldest == null || link.expires < oldest.expires)) {
                    oldest = link;
                }
            }
            if (oldest == null) {
                break;
            }
            release(owner.getServer(), oldest);
        }
        Link link = new Link(owner.getUUID(), mob.getUUID(), mob.level().dimension(), borrowed, now + ticks);
        link.entity = mob;
        link.targetGoal = new LinkTargetGoal(link);
        link.followGoal = new LinkFollowGoal(link);
        mob.targetSelector.addGoal(-1, link.targetGoal);
        mob.goalSelector.addGoal(-1, link.followGoal);
        LINKS.put(mob.getUUID(), link);
        mark(mob, link);
        LivingEntity current = mob.getTarget();
        if (current != null && !allowedTarget(owner, link, current)) {
            mob.setTarget(null);
        }
        mob.getNavigation().stop();
        return true;
    }

    public static void releaseAll(ServerPlayer owner) {
        releaseWhere(owner.getServer(), link -> link.owner.equals(owner.getUUID()));
    }

    public static void releaseBorrowed(ServerPlayer owner) {
        releaseWhere(owner.getServer(), link -> link.owner.equals(owner.getUUID()) && link.borrowed);
    }

    public static void release(Entity entity) {
        Link link = entity == null ? null : LINKS.get(entity.getUUID());
        if (link != null && entity.level() instanceof ServerLevel level) {
            release(level.getServer(), link);
        }
    }

    private static void releaseWhere(MinecraftServer server, Predicate<Link> which) {
        List<Link> doomed = new ArrayList<>();
        for (Link link : LINKS.values()) {
            if (which.test(link)) {
                doomed.add(link);
            }
        }
        doomed.forEach(link -> release(server, link));
    }

    private static void release(MinecraftServer server, Link link) {
        LINKS.remove(link.mob);
        Mob mob = link.entity;
        if (mob == null && server != null) {
            ServerLevel level = server.getLevel(link.dimension);
            if (level != null && level.getEntity(link.mob) instanceof Mob found) {
                mob = found;
            }
        }
        if (mob == null) {
            return;
        }
        if (link.targetGoal != null) {
            mob.targetSelector.removeGoal(link.targetGoal);
        }
        if (link.followGoal != null) {
            mob.goalSelector.removeGoal(link.followGoal);
        }
        unmark(mob);
        if (mob.isAlive() && !mob.isRemoved()) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    private static void mark(Mob mob, Link link) {
        CompoundTag data = mob.getPersistentData();
        data.putUUID(OWNER_KEY, link.owner);
        data.putLong(UNTIL_KEY, link.expires);
    }

    private static void unmark(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        data.remove(OWNER_KEY);
        data.remove(UNTIL_KEY);
    }

    private static ServerPlayer owner(MinecraftServer server, Link link) {
        return server == null ? null : server.getPlayerList().getPlayer(link.owner);
    }

    private static boolean ownerValid(ServerPlayer owner, Link link) {
        if (owner == null || !owner.isAlive() || owner.isRemoved() || owner.isSpectator() && !AstralProjection.active(owner) || owner.hasDisconnected()) {
            return false;
        }
        if (!owner.level().dimension().equals(link.dimension)) {
            return false;
        }
        return link.borrowed || SerumKit.is(owner, ProfessorXPower.ID);
    }

    private static boolean stillEligible(ServerPlayer owner, Mob mob) {
        if (!mob.isAlive() || mob.isRemoved() || mob.isNoAi()) {
            return false;
        }
        if (!HeroAbilities.canControl(mob) || HeroAbilities.isBoss(mob)) {
            return false;
        }
        if (mob instanceof TamableAnimal tame && tame.isTame() || mob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return false;
        }
        if (owner.isAlliedTo(mob)) {
            return false;
        }
        return !isControlledElsewhere(mob);
    }

    static boolean allowedTarget(ServerPlayer owner, LivingEntity mob, LivingEntity target) {
        Link link = LINKS.get(mob.getUUID());
        return link != null && allowedTarget(owner, link, target);
    }

    private static boolean allowedTarget(ServerPlayer owner, Link link, LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved() || target == owner || target.getUUID().equals(link.mob)) {
            return false;
        }
        if (target.level() != owner.level()) {
            return false;
        }
        Link other = LINKS.get(target.getUUID());
        if (other != null && other.owner.equals(link.owner)) {
            return false;
        }
        if (owner.isAlliedTo(target) || target.isAlliedTo(owner)) {
            return false;
        }
        if (target instanceof AbstractVillager || target instanceof Merchant || target instanceof Npc) {
            return false;
        }
        if (target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            UUID petOwner = ownable.getOwnerUUID();
            if (petOwner.equals(owner.getUUID()) || !(target instanceof Mob pet && pet.getTarget() == owner)) {
                return false;
            }
        }
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator() || !owner.getServer().isPvpAllowed()) {
                return false;
            }
            Memory memory = STRUCK_BY.get(owner.getUUID());
            return memory != null && memory.entity.equals(player.getUUID())
                    && owner.serverLevel().getGameTime() - memory.at <= ProfessorXConfig.LINK_MEMORY_TICKS.get();
        }
        if (!(target instanceof Enemy)) {
            Memory memory = STRUCK_BY.get(owner.getUUID());
            boolean attacker = memory != null && memory.entity.equals(target.getUUID());
            boolean aggressor = target instanceof Mob hostile && hostile.getTarget() == owner;
            return attacker || aggressor;
        }
        return true;
    }

    private static LivingEntity remembered(ServerPlayer owner, Map<UUID, Memory> memories) {
        Memory memory = memories.get(owner.getUUID());
        if (memory == null) {
            return null;
        }
        if (owner.serverLevel().getGameTime() - memory.at > ProfessorXConfig.LINK_MEMORY_TICKS.get()) {
            memories.remove(owner.getUUID());
            return null;
        }
        return owner.serverLevel().getEntity(memory.entity) instanceof LivingEntity living ? living : null;
    }

    private static boolean inReach(ServerPlayer owner, LivingEntity target) {
        double range = ProfessorXConfig.LINK_TARGET_RANGE.get();
        return target.distanceToSqr(owner) <= range * range;
    }

    private static LivingEntity choose(ServerPlayer owner, Link link, List<LivingEntity> aggressors) {
        LivingEntity struck = remembered(owner, STRUCK);
        if (struck != null && inReach(owner, struck) && allowedTarget(owner, link, struck)) {
            return struck;
        }
        LivingEntity attacker = remembered(owner, STRUCK_BY);
        if (attacker != null && inReach(owner, attacker) && allowedTarget(owner, link, attacker)) {
            return attacker;
        }
        Mob self = link.entity;
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity aggressor : aggressors) {
            if (!allowedTarget(owner, link, aggressor)) {
                continue;
            }
            double distance = aggressor.distanceToSqr(self);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = aggressor;
            }
        }
        return best;
    }

    private static List<LivingEntity> aggressors(ServerPlayer owner) {
        double radius = ProfessorXConfig.LINK_DEFEND_RADIUS.get();
        if (radius <= 0.0D) {
            return List.of();
        }
        return owner.serverLevel().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(radius),
                entity -> entity instanceof Mob mob && mob.isAlive() && mob.getTarget() == owner && !isLinked(mob));
    }

    private static void steer(ServerPlayer owner, Link link, List<LivingEntity> aggressors) {
        Mob mob = link.entity;
        LivingEntity current = mob.getTarget();
        LivingEntity wanted = choose(owner, link, aggressors);
        if (wanted != null) {
            if (current != wanted) {
                mob.setTarget(wanted);
            }
            return;
        }
        if (current != null && (!allowedTarget(owner, link, current) || !inReach(owner, current))) {
            mob.setTarget(null);
        }
    }

    private static void tether(ServerPlayer owner, Link link) {
        if (!link.borrowed && !ProfessorXPower.tethersOn(owner)) {
            return;
        }
        ServerLevel level = owner.serverLevel();
        Mob mob = link.entity;
        ParticleOptions glow = SerumKit.glow(owner, TETHER_COLOR, 0.5F);
        Vec3 from = owner.getEyePosition().subtract(0.0D, 0.3D, 0.0D);
        Vec3 to = mob.getBoundingBox().getCenter();
        double length = from.distanceTo(to);
        double spacing = Math.max(0.7D, length / 24.0D);
        HeroAbilities.particleLine(level, from, to, glow, spacing);
        SerumKit.ring(level, new Vec3(mob.getX(), mob.getEyeY() + 0.45D, mob.getZ()), glow, Math.max(0.3D, mob.getBbWidth() * 0.5D), 8);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (LINKS.isEmpty()) {
            if (!STRUCK.isEmpty() || !STRUCK_BY.isEmpty()) {
                STRUCK.clear();
                STRUCK_BY.clear();
            }
            return;
        }
        if (server.getTickCount() % SCAN_INTERVAL != 0) {
            return;
        }
        Map<UUID, List<LivingEntity>> aggressorCache = new HashMap<>();
        Iterator<Map.Entry<UUID, Link>> iterator = new ArrayList<>(LINKS.entrySet()).iterator();
        while (iterator.hasNext()) {
            Link link = iterator.next().getValue();
            if (LINKS.get(link.mob) != link) {
                continue;
            }
            ServerPlayer owner = owner(server, link);
            if (!ownerValid(owner, link)) {
                release(server, link);
                continue;
            }
            ServerLevel level = server.getLevel(link.dimension);
            Entity entity = level == null ? null : level.getEntity(link.mob);
            if (!(entity instanceof Mob mob) || mob != link.entity) {
                if (entity instanceof Mob replaced) {
                    link.entity = replaced;
                }
                release(server, link);
                continue;
            }
            long now = level.getGameTime();
            double leash = ProfessorXConfig.LINK_LEASH.get();
            if (now >= link.expires || !stillEligible(owner, mob) || mob.distanceToSqr(owner) > leash * leash) {
                release(server, link);
                continue;
            }
            List<LivingEntity> aggressors = aggressorCache.computeIfAbsent(owner.getUUID(), ignored -> aggressors(owner));
            steer(owner, link, aggressors);
            tether(owner, link);
        }
    }

    private static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        Link link = LINKS.get(event.getEntity().getUUID());
        if (link == null || target == null) {
            return;
        }
        MinecraftServer server = event.getEntity().getServer();
        ServerPlayer owner = owner(server, link);
        if (owner == null || !allowedTarget(owner, link, target)) {
            event.setCanceled(true);
        }
    }

    private static void onDamage(LivingIncomingDamageEvent event) {
        if (LINKS.isEmpty()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        Entity source = event.getSource().getEntity();
        long now = victim.level().getGameTime();
        if (source instanceof ServerPlayer attacker && attacker != victim && owns(attacker)) {
            Link victimLink = LINKS.get(victim.getUUID());
            if (victimLink == null || !victimLink.owner.equals(attacker.getUUID())) {
                STRUCK.put(attacker.getUUID(), new Memory(victim.getUUID(), now));
            }
        }
        if (victim instanceof ServerPlayer owner && source instanceof LivingEntity attacker && attacker != owner && owns(owner)) {
            Link attackerLink = LINKS.get(attacker.getUUID());
            if (attackerLink == null || !attackerLink.owner.equals(owner.getUUID())) {
                STRUCK_BY.put(owner.getUUID(), new Memory(attacker.getUUID(), now));
            }
        }
    }

    private static boolean owns(ServerPlayer player) {
        for (Link link : LINKS.values()) {
            if (link.owner.equals(player.getUUID())) {
                return true;
            }
        }
        return false;
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(OWNER_KEY) || LINKS.containsKey(mob.getUUID())) {
            return;
        }
        unmark(mob);
        mob.setTarget(null);
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || LINKS.isEmpty()) {
            return;
        }
        Link link = LINKS.get(event.getEntity().getUUID());
        if (link != null && link.entity == event.getEntity()) {
            LINKS.remove(link.mob);
            Mob mob = link.entity;
            mob.targetSelector.removeGoal(link.targetGoal);
            mob.goalSelector.removeGoal(link.followGoal);
            unmark(mob);
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            releaseAll(player);
            STRUCK.remove(player.getUUID());
            STRUCK_BY.remove(player.getUUID());
        } else if (LINKS.containsKey(event.getEntity().getUUID())) {
            release(event.getEntity());
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            releaseAll(player);
            STRUCK.remove(player.getUUID());
            STRUCK_BY.remove(player.getUUID());
        }
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            releaseAll(player);
        }
    }

    private static void onStopping(ServerStoppingEvent event) {
        releaseWhere(event.getServer(), link -> true);
        LINKS.clear();
        STRUCK.clear();
        STRUCK_BY.clear();
    }

    private static boolean active(Link link) {
        return LINKS.get(link.mob) == link;
    }

    private static final class LinkTargetGoal extends Goal {
        private final Link link;

        private LinkTargetGoal(Link link) {
            this.link = link;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return active(this.link);
        }

        @Override
        public boolean canContinueToUse() {
            return active(this.link);
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }
    }

    private static final class LinkFollowGoal extends Goal {
        private static final double START = 7.0D;
        private static final double STOP = 3.5D;
        private final Link link;
        private int repath;

        private LinkFollowGoal(Link link) {
            this.link = link;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private ServerPlayer leader() {
            Mob mob = this.link.entity;
            return mob == null ? null : owner(mob.getServer(), this.link);
        }

        @Override
        public boolean canUse() {
            if (!active(this.link) || this.link.entity.getTarget() != null) {
                return false;
            }
            ServerPlayer owner = this.leader();
            return owner != null && owner.level() == this.link.entity.level() && this.link.entity.distanceToSqr(owner) > START * START;
        }

        @Override
        public boolean canContinueToUse() {
            if (!active(this.link) || this.link.entity.getTarget() != null) {
                return false;
            }
            ServerPlayer owner = this.leader();
            return owner != null && owner.level() == this.link.entity.level() && this.link.entity.distanceToSqr(owner) > STOP * STOP;
        }

        @Override
        public void start() {
            this.repath = 0;
        }

        @Override
        public void stop() {
            this.link.entity.getNavigation().stop();
        }

        @Override
        public void tick() {
            ServerPlayer owner = this.leader();
            if (owner == null) {
                return;
            }
            Mob mob = this.link.entity;
            mob.getLookControl().setLookAt(owner, 10.0F, mob.getMaxHeadXRot());
            if (--this.repath <= 0) {
                this.repath = 10;
                mob.getNavigation().moveTo(owner, 1.15D);
            }
        }
    }
}
