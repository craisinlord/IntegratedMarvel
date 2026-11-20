package com.craisinlord.integrated_superheroes.voidrealm.variant;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicVariants;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.VariantMoves;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

final class VariantTracker {
    private static final ResourceLocation SCALE = IntegratedSuperheroes.id("void_variant_tier");
    private static final Map<Entity, Tracked> TRACKED = new WeakHashMap<>();

    private VariantTracker() {
    }

    static final class Tracked {
        final Mob mob;
        final VariantProfile profile;
        final List<String> fallback = new ArrayList<>();
        long nextMove;
        int lonelyTicks;
        final Map<java.util.UUID, Long> grudges = new java.util.HashMap<>();

        Tracked(Mob mob, VariantProfile profile) {
            this.mob = mob;
            this.profile = profile;
            for (String move : profile.moves()) {
                if (VariantMoves.isFallback(move)) {
                    fallback.add(move);
                }
            }
        }
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(VariantTracker::onJoin);
        NeoForge.EVENT_BUS.addListener(VariantTracker::onLeave);
        NeoForge.EVENT_BUS.addListener(VariantTracker::onLevelTick);
        NeoForge.EVENT_BUS.addListener(VariantTracker::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(VariantTracker::onHurt);
        NeoForge.EVENT_BUS.addListener(VariantTracker::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(VariantTracker::onServerStopped);
        NeoForge.EVENT_BUS.addListener(VariantDrops::onDrops);
        NeoForge.EVENT_BUS.addListener(VariantCommand::onRegister);
        VariantTears.register();
    }

    static List<Mob> loaded(ServerLevel level) {
        List<Mob> out = new ArrayList<>();
        for (Tracked tracked : TRACKED.values()) {
            if (tracked.mob.level() == level && tracked.mob.isAlive()) {
                out.add(tracked.mob);
            }
        }
        return out;
    }

    static VariantAlignment alignment(Entity entity) {
        Tracked tracked = TRACKED.get(entity);
        if (tracked != null) {
            return tracked.profile.alignment();
        }
        if (!VoidVariants.isVariant(entity)) {
            return null;
        }
        VariantProfile profile = VariantProfile.of(entity);
        return profile == null ? null : profile.alignment();
    }

    private static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            VariantPool.markDirty();
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        TRACKED.clear();
        VariantPool.clear();
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || !VoidVariants.isVariant(mob) || TRACKED.containsKey(mob)) {
            return;
        }
        VariantProfile profile = VariantProfile.of(mob);
        if (profile == null) {
            return;
        }
        scale(mob, VariantTiers.power(profile.tier()));
        if (profile.kind() != VariantKind.MARVEL && VoidVariants.thematicHost(mob)) {
            String look = VoidVariants.wornLook(profile);
            if (!look.isEmpty()) {
                ThematicVariants.restore(mob, look);
            }
        }
        TRACKED.put(mob, new Tracked(mob, profile));
    }

    private static void scale(Mob mob, int tier) {
        if (!VariantConfig.ready()) {
            return;
        }
        modify(mob.getAttribute(Attributes.MAX_HEALTH), VariantConfig.HEALTH_PER_TIER.get() * tier);
        modify(mob.getAttribute(Attributes.ATTACK_DAMAGE), VariantConfig.DAMAGE_PER_TIER.get() * tier);
    }

    private static void modify(AttributeInstance instance, double amount) {
        if (instance == null) {
            return;
        }
        instance.removeModifier(SCALE);
        if (amount > 0.0D) {
            instance.addTransientModifier(new AttributeModifier(SCALE, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            TRACKED.remove(event.getEntity());
        }
    }

    private static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && VoidVariants.isVariant(event.getEntity()) && VoidVariants.allied(event.getEntity(), target)) {
            event.setCanceled(true);
            return;
        }
        if (target instanceof Player player && event.getEntity() instanceof Mob mob && !willFight(mob, player)) {
            event.setCanceled(true);
        }
    }

    private static void onHurt(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getSource().getEntity() instanceof Player player) || !VariantConfig.ready()) {
            return;
        }
        Tracked tracked = TRACKED.get(mob);
        if (tracked != null) {
            tracked.grudges.put(player.getUUID(), mob.level().getGameTime() + VariantConfig.GRUDGE_SECONDS.get() * 20L);
        }
    }

    private static boolean willFight(Mob mob, Player player) {
        if (!VariantConfig.ready() || !VariantConfig.NEUTRAL_HEROES.get()) {
            return true;
        }
        Tracked tracked = TRACKED.get(mob);
        if (tracked == null || tracked.profile.alignment() != VariantAlignment.HERO) {
            return true;
        }
        Long until = tracked.grudges.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (mob.level().getGameTime() > until) {
            tracked.grudges.remove(player.getUUID());
            return false;
        }
        return true;
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !VariantConfig.ready()) {
            return;
        }
        long now = level.getGameTime();
        if (now % 5L == 0L && !TRACKED.isEmpty()) {
            for (Tracked tracked : new ArrayList<>(TRACKED.values())) {
                if (tracked.mob.level() == level && tracked.mob.isAlive()) {
                    tick(level, tracked, now);
                }
            }
        }
        if (now % 20L == 0L) {
            VariantSpawner.tick(level, now);
        }
    }

    private static LivingEntity target(Mob mob) {
        return VoidVariants.thematicHost(mob) ? ThematicVariants.target(mob) : mob.getTarget();
    }

    private static void setTarget(Mob mob, LivingEntity target) {
        if (VoidVariants.thematicHost(mob)) {
            ThematicVariants.setTarget(mob, target);
        } else {
            mob.setTarget(target);
        }
    }

    private static boolean busy(Mob mob) {
        return VoidVariants.thematicHost(mob) && ThematicVariants.busy(mob);
    }

    private static void tick(ServerLevel level, Tracked tracked, long now) {
        Mob mob = tracked.mob;
        LivingEntity target = target(mob);
        if (target instanceof Player player && !willFight(mob, player)) {
            setTarget(mob, null);
            target = null;
        }
        com.craisinlord.integrated_superheroes.voidrealm.variant.moves.HeroMoves.tick(level, mob, tracked.profile, target);
        if (now % 20L == 0L) {
            if (target != null && VoidVariants.allied(mob, target)) {
                setTarget(mob, null);
                target = null;
            }
            target = pickRival(level, tracked, target);
            tracked.lonelyTicks = level.getNearestPlayer(mob, 128.0D) == null ? tracked.lonelyTicks + 20 : 0;
            if (tracked.lonelyTicks >= VariantConfig.LONELY_SECONDS.get() * 20) {
                mob.discard();
                return;
            }
        }
        if (tracked.fallback.isEmpty() || target == null || !target.isAlive() || now < tracked.nextMove || busy(mob)) {
            return;
        }
        if (VariantMoves.tryUse(level, mob, target, tracked.fallback, mob.getRandom())) {
            tracked.nextMove = now + VariantConfig.FALLBACK_GAP_TICKS.get() + mob.getRandom().nextInt(40);
        }
    }

    private static LivingEntity pickRival(ServerLevel level, Tracked tracked, LivingEntity current) {
        Mob mob = tracked.mob;
        Player close = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), VariantConfig.PLAYER_FOCUS_RADIUS.get(),
                entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator() && willFight(mob, player));
        if (close != null) {
            if (current != close && (current == null || VoidVariants.isVariant(current))) {
                setTarget(mob, close);
                return close;
            }
            return current;
        }
        if (!VariantConfig.FIGHT_EACH_OTHER.get() || current != null && current.isAlive() && VoidVariants.isVariant(current)) {
            return current;
        }
        double radius = VariantConfig.RIVAL_RADIUS.get();
        Mob rival = null;
        double best = radius * radius;
        for (Tracked other : TRACKED.values()) {
            if (other == tracked || other.mob.level() != level || !other.mob.isAlive()
                    || !tracked.profile.alignment().opposes(other.profile.alignment())) {
                continue;
            }
            double distance = other.mob.distanceToSqr(mob);
            if (distance < best) {
                best = distance;
                rival = other.mob;
            }
        }
        if (rival != null && (current == null || !current.isAlive() || current instanceof Player)) {
            setTarget(mob, rival);
            return rival;
        }
        return current;
    }
}
