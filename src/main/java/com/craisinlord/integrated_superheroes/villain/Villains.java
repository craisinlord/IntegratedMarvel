package com.craisinlord.integrated_superheroes.villain;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tintankgames.marvel.world.entity.BaronZemo;
import net.tintankgames.marvel.world.entity.HydraAgent;
import net.tintankgames.marvel.world.entity.RedSkull;
import net.tintankgames.marvel.world.entity.WinterSoldier;
import net.tintankgames.marvel.world.entity.ai.goal.TimedMeleeAttackGoal;
import net.tintankgames.marvel.world.entity.projectile.ThrownVibraniumShield;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class Villains {
    public static final java.util.Set<String> ECHO_TAGS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static final String COSMETIC_TAG = "integrated_superheroes_villain_cosmetic";
    public static final String DART_TAG = "integrated_superheroes_zemo_dart";
    public static final String PORTAL_TAG = "integrated_superheroes_tesseract_portal";
    private static final ResourceKey<net.minecraft.world.damagesource.DamageType> TESSERACT_ARROW = ResourceKey.create(
            net.minecraft.core.registries.Registries.DAMAGE_TYPE, ResourceLocation.parse("marvel:tesseract_arrow"));
    private static final Map<Entity, VillainBrain> BRAINS = new WeakHashMap<>();

    private Villains() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Villains::onJoin);
        NeoForge.EVENT_BUS.addListener(Villains::onIncoming);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Villains::onDamage);
        NeoForge.EVENT_BUS.addListener(Villains::onDeath);
        NeoForge.EVENT_BUS.addListener(Villains::onImpact);
        NeoForge.EVENT_BUS.addListener(Villains::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(Villains::onAttack);
        NeoForge.EVENT_BUS.addListener(Villains::onTarget);
        NeoForge.EVENT_BUS.addListener(Villains::onLevelTick);
        NeoForge.EVENT_BUS.addListener(Villains::onPlayerTick);
    }

    public static boolean managed(Entity entity) {
        return entity != null && VillainConfig.enabled() && entity.getPersistentData().getBoolean(VillainBrain.MANAGED);
    }

    public static boolean blockWeaponGoal(Entity entity) {
        if (!managed(entity) || !(entity instanceof Mob mob)) {
            return false;
        }
        if (entity instanceof HydraAgent) {
            return VillainArms.gun(mob.getMainHandItem());
        }
        return true;
    }

    public static boolean validTarget(Entity entity) {
        return entity instanceof Player player && player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    public static boolean isVillainSide(LivingEntity entity) {
        if (entity instanceof WinterSoldier && BRAINS.get(entity) instanceof WinterSoldierBrain brain && brain.ally()) {
            return false;
        }
        return entity instanceof HydraAgent || entity instanceof BaronZemo || entity instanceof WinterSoldier || entity instanceof RedSkull;
    }

    public static VillainBrain brain(Entity entity) {
        return BRAINS.get(entity);
    }

    public static Collection<VillainBrain> brains() {
        return new ArrayList<>(BRAINS.values());
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !VillainConfig.enabled()) {
            return;
        }
        Entity entity = event.getEntity();
        if (event.loadedFromDisk() && (entity.getTags().contains(PORTAL_TAG)
                || entity instanceof WinterSoldier && entity.getPersistentData().getBoolean(WinterSoldierBrain.KEY_FREED))) {
            event.setCanceled(true);
            return;
        }
        if (entity instanceof HydraAgent agent) {
            HydraSquad.onJoin(agent);
            return;
        }
        if (!(entity instanceof PathfinderMob mob) || BRAINS.containsKey(entity)) {
            return;
        }
        boolean echo = entity.getTags().stream().anyMatch(ECHO_TAGS::contains);
        VillainBrain brain = null;
        if (entity instanceof BaronZemo && VillainConfig.ZEMO.enabled.get()) {
            mark(mob);
            brain = new ZemoBrain(mob, echo);
        } else if (entity instanceof WinterSoldier && VillainConfig.WINTER_SOLDIER.enabled.get()) {
            mark(mob);
            brain = new WinterSoldierBrain(mob, echo);
        } else if (entity instanceof RedSkull && VillainConfig.RED_SKULL.enabled.get()) {
            mark(mob);
            brain = new RedSkullBrain(mob, echo);
        }
        if (brain == null) {
            return;
        }
        stripCombatGoals(mob, true);
        mob.goalSelector.addGoal(1, brain);
        BRAINS.put(entity, brain);
        brain.updateBar();
    }

    private static void mark(Mob mob) {
        mob.getPersistentData().putBoolean(VillainBrain.MANAGED, true);
    }

    static void stripCombatGoals(Mob mob, boolean stroll) {
        List<Goal> remove = new ArrayList<>();
        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            Goal goal = wrapped.getGoal();
            if (goal instanceof MeleeAttackGoal || goal instanceof RangedAttackGoal || goal instanceof RangedCrossbowAttackGoal
                    || goal instanceof TimedMeleeAttackGoal || (stroll && goal instanceof RandomStrollGoal)) {
                remove.add(goal);
            }
        }
        remove.forEach(mob.goalSelector::removeGoal);
    }

    private static void onIncoming(LivingIncomingDamageEvent event) {
        if (!VillainConfig.enabled() || event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (source.is(TESSERACT_ARROW)) {
            if (attacker instanceof RedSkull) {
                event.setAmount((float) (event.getAmount() * VillainConfig.TESSERACT_CHARGE_MULTIPLIER.get()));
                Loadout.drainSuit(victim, 0.03D);
            } else if (attacker instanceof HydraAgent) {
                event.setAmount((float) (event.getAmount() * VillainConfig.AGENT_TESSERACT_CHARGE_MULTIPLIER.get()));
            }
        }
        if (attacker instanceof RedSkull && Loadout.superSoldier(victim)) {
            event.setAmount(event.getAmount() * 1.15F);
        }
        if (victim instanceof RedSkull && attacker instanceof LivingEntity living && Loadout.superSoldier(living)) {
            event.setAmount(event.getAmount() * 1.15F);
        }
        VillainBrain attackerBrain = attacker == null ? null : BRAINS.get(attacker);
        if (attackerBrain instanceof WinterSoldierBrain soldier && soldier.ally() && victim instanceof Player) {
            event.setCanceled(true);
            return;
        }
        VillainBrain brain = BRAINS.get(victim);
        if (brain != null) {
            if (brain instanceof WinterSoldierBrain soldier && soldier.ally() && (attacker instanceof Player || attacker == null && source.getDirectEntity() instanceof Projectile)) {
                event.setCanceled(true);
                return;
            }
            if (brain.incoming(event)) {
                event.setCanceled(true);
                return;
            }
        }
        if (victim instanceof HydraAgent agent && managed(agent)) {
            HydraSquad.onHurt(agent, source);
        }
        if (victim instanceof ServerPlayer player) {
            RedBook.onReaderHurt(player);
        }
    }

    private static void onDamage(LivingDamageEvent.Pre event) {
        if (!VillainConfig.enabled() || event.getEntity().level().isClientSide()) {
            return;
        }
        VillainBrain brain = BRAINS.get(event.getEntity());
        if (brain != null) {
            event.setNewDamage(brain.process(event.getSource(), event.getNewDamage()));
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!VillainConfig.enabled() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity entity = event.getEntity();
        VillainBrain brain = BRAINS.get(entity);
        if (brain != null && !brain.echo) {
            brain.onKilled(level, event.getSource());
        }
        if (entity instanceof HydraAgent agent && managed(agent)) {
            HydraSquad.onDeath(level, agent, event.getSource());
        }
    }

    private static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        if (projectile.getTags().contains(COSMETIC_TAG)) {
            event.setCanceled(true);
            projectile.discard();
            return;
        }
        HitResult result = event.getRayTraceResult();
        if (projectile.getTags().contains(DART_TAG)) {
            event.setCanceled(true);
            projectile.discard();
            if (result instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity victim && !isVillainSide(victim)) {
                ZemoBrain.dartHit(level, projectile, victim);
            }
            return;
        }
        if (projectile instanceof ThrownVibraniumShield shield && !shield.isFromWinterSoldier()
                && result instanceof EntityHitResult hit && BRAINS.get(hit.getEntity()) instanceof WinterSoldierBrain soldier) {
            if (soldier.catchShield(level, shield)) {
                event.setCanceled(true);
            }
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !VillainConfig.enabled()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && RedBook.isBook(event.getItemStack()) && RedBook.tryRead(player)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    private static void onAttack(AttackEntityEvent event) {
        if (event.getTarget().getTags().contains(PORTAL_TAG) && event.getTarget().level() instanceof ServerLevel level) {
            RedSkullBrain.hitPortal(level, event.getTarget());
        }
    }

    private static void onTarget(LivingChangeTargetEvent event) {
        if (BRAINS.get(event.getEntity()) instanceof WinterSoldierBrain soldier && soldier.ally()) {
            LivingEntity next = event.getNewAboutToBeSetTarget();
            if (next != null && !soldier.allowedAllyTarget(next)) {
                event.setCanceled(true);
            }
            return;
        }
        LivingEntity next = event.getNewAboutToBeSetTarget();
        if (next instanceof WinterSoldier && BRAINS.get(next) instanceof WinterSoldierBrain soldier && soldier.ally()
                && event.getEntity() instanceof Mob && !(event.getEntity() instanceof HydraAgent)) {
            event.setCanceled(true);
        }
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !VillainConfig.enabled()) {
            return;
        }
        VillainWorld.tick(level);
        HydraAlarm.tick(level);
        RedBook.tick(level);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VillainWorld.tickPlayer(player);
        }
    }

    public static void award(ServerPlayer player, String path) {
        award(player, ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, path));
    }

    public static void award(ServerPlayer player, ResourceLocation id) {
        AdvancementHolder advancement = player.server.getAdvancements().get(id);
        if (advancement == null) {
            return;
        }
        for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    public static void awardNear(ServerLevel level, Vec3 center, String path) {
        awardNear(level, center, ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, path));
    }

    public static void awardNear(ServerLevel level, Vec3 center, ResourceLocation id) {
        for (ServerPlayer player : VillainFx.near(level, center, 48.0D)) {
            award(player, id);
        }
    }

    public static boolean isType(Entity entity, String id) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(id);
    }
}
