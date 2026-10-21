package com.craisinlord.integrated_superheroes.villain;

import com.craisinlord.integrated_superheroes.gauntlet.GauntletEntityLocks;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.hero.HeroStun;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.tintankgames.marvel.world.entity.BaronZemo;
import net.tintankgames.marvel.world.entity.RedSkull;
import net.tintankgames.marvel.world.entity.WinterSoldier;
import net.tintankgames.marvel.world.entity.projectile.ThrownVibraniumShield;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class VillainBrain extends Goal {
    public static final String MANAGED = "integrated_superheroes_villain";
    private static final String KEY_INIT = "integrated_superheroes_villain_init";
    private static final String KEY_ENGAGED = "integrated_superheroes_villain_engaged";
    private static final String KEY_PHASE = "integrated_superheroes_villain_phase";
    private static final String KEY_SCALE = "integrated_superheroes_villain_scale";
    private static final String KEY_ANCHOR = "integrated_superheroes_villain_anchor";
    private static final String KEY_DEATH_AT = "integrated_superheroes_villain_death_at";
    private static final String KEY_TRANSITION_AT = "integrated_superheroes_villain_transition_at";
    private static final List<String> FIGHT_PREFIXES = List.of("integrated_superheroes_villain_", "integrated_superheroes_zemo_",
            "integrated_superheroes_skull_", "integrated_superheroes_ws_ally");

    protected final PathfinderMob mob;
    protected final VillainConfig.Boss config;
    protected final RandomSource random;
    protected final Map<String, Long> readyAt = new HashMap<>();
    private final Deque<Long> damageTimes = new ArrayDeque<>();
    private final Deque<Float> damageAmounts = new ArrayDeque<>();

    protected boolean engaged;
    protected int phase;
    protected double healthScale = 1.0D;
    protected Vec3 anchor;
    protected LivingEntity target;
    protected Move current;
    protected int windupLeft;
    protected int recoveryLeft;
    protected int transitionLeft;
    protected long transitionEndsAt;
    protected boolean dying;
    protected long deathAt;
    protected long staggerUntil;
    protected long staggerImmuneUntil;
    protected long openUntil;
    protected float poise;
    protected long nextBigAt;
    protected int unreachableTicks;
    protected int attackCooldown;
    protected int pathTimer;
    protected UUID lastPlayer;
    protected boolean finishing;
    private boolean finished;
    private int finishAttempts;
    protected Vec3 lockedSpot;
    protected boolean alarmed;

    protected VillainBrain(PathfinderMob mob, VillainConfig.Boss config) {
        this.mob = mob;
        this.config = config;
        this.random = mob.getRandom();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        CompoundTag data = mob.getPersistentData();
        if (!data.getBoolean(KEY_INIT)) {
            data.putBoolean(KEY_INIT, true);
            setBase(Attributes.MAX_HEALTH, config.health.get());
            setBase(Attributes.ARMOR, config.armor.get());
            initEquipment();
            mob.setHealth(mob.getMaxHealth());
        }
        this.engaged = data.getBoolean(KEY_ENGAGED);
        this.phase = data.getInt(KEY_PHASE);
        this.healthScale = data.contains(KEY_SCALE) ? data.getDouble(KEY_SCALE) : 1.0D;
        if (data.contains(KEY_ANCHOR)) {
            CompoundTag a = data.getCompound(KEY_ANCHOR);
            this.anchor = new Vec3(a.getDouble("x"), a.getDouble("y"), a.getDouble("z"));
        }
        long now = mob.level().getGameTime();
        if (data.contains(KEY_DEATH_AT)) {
            this.dying = true;
            this.deathAt = Math.min(data.getLong(KEY_DEATH_AT), now + deathTicks());
        } else if (data.contains(KEY_TRANSITION_AT)) {
            this.transitionEndsAt = Math.min(data.getLong(KEY_TRANSITION_AT), now + 50L);
            this.transitionLeft = (int) Math.max(1L, transitionEndsAt - now);
        }
    }

    public static void resetFight(CompoundTag entityTag) {
        if (!entityTag.contains("NeoForgeData")) {
            return;
        }
        CompoundTag data = entityTag.getCompound("NeoForgeData");
        for (String key : List.copyOf(data.getAllKeys())) {
            if (key.equals(MANAGED) || key.equals(KEY_INIT)) {
                continue;
            }
            for (String prefix : FIGHT_PREFIXES) {
                if (key.startsWith(prefix)) {
                    data.remove(key);
                    break;
                }
            }
        }
    }

    protected record Move(String id, int cooldown, int windup, int recovery, boolean big, boolean priority,
                          Predicate<LivingEntity> ready, Consumer<LivingEntity> begin,
                          BiConsumer<LivingEntity, Integer> telegraph, Consumer<LivingEntity> fire) {
    }

    protected static Move big(String id, int cooldown, int windup, int recovery, Predicate<LivingEntity> ready,
                              Consumer<LivingEntity> begin, BiConsumer<LivingEntity, Integer> telegraph, Consumer<LivingEntity> fire) {
        return new Move(id, cooldown, windup, recovery, true, false, ready, begin, telegraph, fire);
    }

    protected static Move priority(String id, int cooldown, int windup, int recovery, Predicate<LivingEntity> ready,
                                   Consumer<LivingEntity> begin, BiConsumer<LivingEntity, Integer> telegraph, Consumer<LivingEntity> fire) {
        return new Move(id, cooldown, windup, recovery, true, true, ready, begin, telegraph, fire);
    }

    protected static Move small(String id, int cooldown, int windup, int recovery, Predicate<LivingEntity> ready,
                                Consumer<LivingEntity> begin, BiConsumer<LivingEntity, Integer> telegraph, Consumer<LivingEntity> fire) {
        return new Move(id, cooldown, windup, recovery, false, false, ready, begin, telegraph, fire);
    }

    protected static final Consumer<LivingEntity> NONE = t -> {
    };
    protected static final BiConsumer<LivingEntity, Integer> QUIET = (t, left) -> {
    };

    protected abstract void initEquipment();

    protected abstract float[] thresholds();

    protected abstract List<Move> moves();

    protected abstract String key();

    protected abstract ChatFormatting color();

    protected double engageRange() {
        return 8.0D;
    }

    protected int deathTicks() {
        return 40;
    }

    protected double chaseSpeed() {
        return 1.1D;
    }

    protected int meleeCooldown() {
        return 20;
    }

    protected void onEngage(ServerLevel level, LivingEntity target) {
    }

    protected void onTransition(ServerLevel level, int newPhase) {
    }

    protected void transitionTick(ServerLevel level, int left) {
    }

    protected void onPhaseStart(ServerLevel level, int newPhase) {
    }

    protected void dormantTick(ServerLevel level) {
    }

    protected void passiveTick(ServerLevel level, long now) {
    }

    protected boolean stanceTick(ServerLevel level, long now, LivingEntity target) {
        return false;
    }

    protected void onStagger(ServerLevel level) {
    }

    protected void onDeathStart(ServerLevel level) {
    }

    protected void deathTick(ServerLevel level, int left) {
    }

    protected void onFinished(ServerLevel level) {
    }

    protected void onDamaged(ServerLevel level, DamageSource source, float amount) {
    }

    protected boolean bypassPipeline() {
        return false;
    }

    public boolean incoming(LivingIncomingDamageEvent event) {
        if (finishing || bypassPipeline()) {
            return false;
        }
        return dying || transitionLeft > 0;
    }

    public void onKilled(ServerLevel level, DamageSource source) {
    }

    @Override
    public boolean canUse() {
        return mob.isAlive() && !mob.level().isClientSide;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    protected void alwaysTick(ServerLevel level, long now) {
    }

    public void serverTick(ServerLevel level) {
        if (finished || !mob.isAlive()) {
            return;
        }
        long now = level.getGameTime();
        alwaysTick(level, now);
        if (!mob.isAlive()) {
            return;
        }
        if (mob.tickCount % 5 == 0) {
            VillainFx.bossBarProgress(mob, mob.getHealth() / mob.getMaxHealth());
        }
        if (!dying && engaged && !bypassPipeline() && phase >= thresholds().length && mob.getHealth() <= 1.0F) {
            beginDeath(level);
        }
        if (dying) {
            mob.getNavigation().stop();
            if (finishing) {
                finish(level);
                return;
            }
            int left = (int) Math.max(0L, deathAt - now);
            if (left > 0) {
                deathTick(level, left);
            } else {
                finish(level);
            }
            return;
        }
        if (transitionEndsAt > 0L) {
            mob.getNavigation().stop();
            transitionLeft = (int) Math.max(0L, transitionEndsAt - now);
            if (transitionLeft > 0) {
                transitionTick(level, transitionLeft);
            } else {
                transitionEndsAt = 0L;
                save();
                onPhaseStart(level, phase);
                resetCooldowns(now);
            }
        }
    }

    @Override
    public void tick() {
        ServerLevel level = (ServerLevel) mob.level();
        long now = level.getGameTime();
        if (dying || transitionLeft > 0) {
            mob.getNavigation().stop();
            return;
        }
        if (!engaged) {
            mob.getNavigation().stop();
            setMarvelActivated(false);
            dormantTick(level);
            LivingEntity found = acquire(level);
            if (found != null) {
                engage(level, found);
            }
            return;
        }
        LivingEntity t = pickTarget(level);
        target = t;
        if (t == null) {
            current = null;
            returnToAnchor(level);
            return;
        }
        if (anchor != null && mob.position().distanceTo(anchor) > VillainConfig.LEASH_RADIUS.get()) {
            current = null;
            returnToAnchor(level);
            if (now % 20L == 0L) {
                mob.heal(mob.getMaxHealth() * 0.01F);
            }
            return;
        }
        updateReach(t);
        passiveTick(level, now);
        if (current != null) {
            Move move = current;
            if (move.windup() > 0) {
                mob.getNavigation().stop();
            }
            mob.getLookControl().setLookAt(t, 30.0F, 30.0F);
            move.telegraph().accept(t, windupLeft);
            if (windupLeft-- <= 0) {
                current = null;
                recoveryLeft = move.recovery();
                move.fire().accept(t);
            }
            return;
        }
        if (recoveryLeft > 0) {
            recoveryLeft--;
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(t, 20.0F, 20.0F);
            return;
        }
        if (stanceTick(level, now, t)) {
            return;
        }
        Move next = choose(now, t);
        if (next != null) {
            start(next, now, t);
        } else {
            basicTick(level, t);
        }
    }

    protected void start(Move move, long now, LivingEntity t) {
        current = move;
        windupLeft = move.windup();
        readyAt.put(move.id(), now + move.cooldown());
        if (move.big()) {
            nextBigAt = now + 50L;
        }
        move.begin().accept(t);
    }

    protected void force(String id, LivingEntity t) {
        if (dying || transitionLeft > 0 || !mob.isAlive()) {
            return;
        }
        for (Move move : moves()) {
            if (move.id().equals(id)) {
                start(move, mob.level().getGameTime(), t);
                return;
            }
        }
    }

    private Move choose(long now, LivingEntity t) {
        List<Move> ready = new ArrayList<>();
        for (Move move : moves()) {
            if (readyAt.getOrDefault(move.id(), now) > now) {
                continue;
            }
            if (move.big() && now < nextBigAt) {
                continue;
            }
            if (!move.ready().test(t)) {
                continue;
            }
            if (move.priority()) {
                return move;
            }
            ready.add(move);
        }
        return ready.isEmpty() ? null : ready.get(random.nextInt(ready.size()));
    }

    protected void resetCooldowns(long now) {
        for (Move move : moves()) {
            readyAt.put(move.id(), now + 20L + random.nextInt(Math.max(1, move.cooldown() / 2)));
        }
        nextBigAt = now + 30L;
    }

    protected void basicTick(ServerLevel level, LivingEntity t) {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        mob.getLookControl().setLookAt(t, 30.0F, 30.0F);
        if (--pathTimer <= 0) {
            pathTimer = 8;
            mob.getNavigation().moveTo(t, chaseSpeed());
        }
        if (attackCooldown <= 0 && mob.isWithinMeleeAttackRange(t) && mob.hasLineOfSight(t)) {
            attackCooldown = meleeCooldown();
            melee(level, t);
        }
    }

    protected void melee(ServerLevel level, LivingEntity t) {
        mob.swing(InteractionHand.MAIN_HAND);
        mob.doHurtTarget(t);
    }

    private void returnToAnchor(ServerLevel level) {
        if (anchor == null) {
            mob.getNavigation().stop();
            return;
        }
        if (mob.position().distanceTo(anchor) > 2.0D && --pathTimer <= 0) {
            pathTimer = 20;
            mob.getNavigation().moveTo(anchor.x, anchor.y, anchor.z, 1.0D);
        }
    }

    private void updateReach(LivingEntity t) {
        if (mob.tickCount % 20 != 0) {
            return;
        }
        boolean flying = !t.onGround() && t.getY() - mob.getY() > 2.5D && !t.isInWater();
        Path path = mob.getNavigation().createPath(t, 0);
        boolean reachable = !flying && path != null && path.canReach();
        unreachableTicks = reachable ? 0 : unreachableTicks + 20;
    }

    protected boolean unreachable() {
        return unreachableTicks >= 80;
    }

    private LivingEntity acquire(ServerLevel level) {
        if (alarmed) {
            Player near = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), VillainConfig.ALARM_RADIUS.get(), Villains::validTarget);
            if (near != null) {
                return near;
            }
        }
        if (mob.getLastHurtByMob() instanceof Player hurter && Villains.validTarget(hurter)) {
            return hurter;
        }
        Player near = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), engageRange(), Villains::validTarget);
        if (near != null && mob.hasLineOfSight(near)) {
            return near;
        }
        return null;
    }

    protected LivingEntity pickTarget(ServerLevel level) {
        LivingEntity t = mob.getTarget();
        if (t != null && t.isAlive() && (!(t instanceof Player p) || Villains.validTarget(p)) && t.distanceTo(mob) < 40.0D) {
            return t;
        }
        Player near = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), 32.0D, Villains::validTarget);
        if (near != null) {
            mob.setTarget(near);
        }
        return near;
    }

    public void alarm(ServerLevel level) {
        alarmed = true;
    }

    public boolean engaged() {
        return engaged;
    }

    protected void engage(ServerLevel level, LivingEntity t) {
        engaged = true;
        if (anchor == null) {
            anchor = mob.position();
        }
        int players = Math.max(1, VillainFx.near(level, mob.position(), 32.0D).size());
        healthScale = 1.0D + (players - 1) * VillainConfig.HEALTH_PER_EXTRA_PLAYER.get();
        setBase(Attributes.MAX_HEALTH, config.health.get() * healthScale);
        mob.setHealth(mob.getMaxHealth());
        save();
        mob.setTarget(t);
        setMarvelActivated(true);
        resetCooldowns(level.getGameTime());
        updateBar();
        onEngage(level, t);
    }

    protected void save() {
        CompoundTag data = mob.getPersistentData();
        data.putBoolean(KEY_ENGAGED, engaged);
        data.putInt(KEY_PHASE, phase);
        data.putDouble(KEY_SCALE, healthScale);
        if (anchor != null) {
            CompoundTag a = new CompoundTag();
            a.putDouble("x", anchor.x);
            a.putDouble("y", anchor.y);
            a.putDouble("z", anchor.z);
            data.put(KEY_ANCHOR, a);
        }
        if (dying) {
            data.putLong(KEY_DEATH_AT, deathAt);
        } else {
            data.remove(KEY_DEATH_AT);
        }
        if (transitionEndsAt > 0L) {
            data.putLong(KEY_TRANSITION_AT, transitionEndsAt);
        } else {
            data.remove(KEY_TRANSITION_AT);
        }
    }

    protected void updateBar() {
        VillainFx.bossBarName(mob, Component.translatable("integrated_superheroes.villains." + key() + ".name")
                .append(" - ")
                .append(Component.translatable("integrated_superheroes.villains." + key() + ".phase." + phase)));
    }

    protected void setMarvelActivated(boolean active) {
        if (mob instanceof BaronZemo zemo) {
            zemo.setActivated(active);
        } else if (mob instanceof WinterSoldier soldier) {
            soldier.setActivated(active);
        } else if (mob instanceof RedSkull skull) {
            skull.setActivated(active);
        }
    }

    public float process(DamageSource source, float amount) {
        if (finishing || bypassPipeline()) {
            return amount;
        }
        if (dying || transitionLeft > 0) {
            return 0.0F;
        }
        ServerLevel level = (ServerLevel) mob.level();
        long now = level.getGameTime();
        if (source.getEntity() instanceof Player player) {
            lastPlayer = player.getUUID();
            if (!engaged && Villains.validTarget(player)) {
                engage(level, player);
            }
        }
        boolean staggered = now < staggerUntil;
        float raw = amount;
        if (staggered) {
            amount *= 1.5F;
        }
        if (now < openUntil) {
            amount *= 1.25F;
        }
        amount = Math.min(amount, (float) (config.perHitCap.get() * (staggered ? 1.5D : 1.0D)));
        while (!damageTimes.isEmpty() && now - damageTimes.peekFirst() > 100L) {
            damageTimes.pollFirst();
            damageAmounts.pollFirst();
        }
        float sum = 0.0F;
        for (float value : damageAmounts) {
            sum += value;
        }
        float budget = (float) (config.dpsCap.get() * 5.0D * healthScale * (staggered ? 1.5D : 1.0D));
        if (sum >= budget) {
            amount *= 0.1F;
            VillainFx.tink(level, mob);
        } else if (sum + amount > budget) {
            float over = sum + amount - budget;
            amount = amount - over + over * 0.1F;
        }
        float health = mob.getHealth();
        float[] lines = thresholds();
        if (phase < lines.length) {
            float line = lines[phase] * mob.getMaxHealth();
            if (health - amount <= line) {
                amount = Math.max(0.0F, health - line);
                beginTransition(level);
            }
        } else if (health - amount <= 0.5F) {
            amount = Math.max(0.0F, health - 1.0F);
            beginDeath(level);
        }
        damageTimes.addLast(now);
        damageAmounts.addLast(amount);
        addPoise(level, now, source, amount, raw);
        onDamaged(level, source, amount);
        return amount;
    }

    private void addPoise(ServerLevel level, long now, DamageSource source, float amount, float raw) {
        if (dying || transitionLeft > 0) {
            return;
        }
        float factor = 1.0F;
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (attacker instanceof Player player && direct == attacker && player.fallDistance > 0.0F && !player.onGround()) {
            factor += 0.5F;
        }
        if (attacker instanceof LivingEntity living && direct == attacker && Loadout.heavy(living)) {
            factor += 1.0F;
        }
        if (direct instanceof ThrownVibraniumShield && !(mob instanceof WinterSoldier)) {
            factor += 1.0F;
        }
        if (direct != attacker && raw >= 15.0F) {
            factor += 0.25F;
        }
        poise += amount * factor;
        if (poise >= mob.getMaxHealth() * 0.15F) {
            stagger(level);
        }
    }

    public void stagger(ServerLevel level) {
        long now = level.getGameTime();
        if (now < staggerImmuneUntil || dying || transitionLeft > 0) {
            poise = 0.0F;
            return;
        }
        poise = 0.0F;
        staggerUntil = now + 50L;
        staggerImmuneUntil = now + 250L;
        current = null;
        recoveryLeft = 0;
        HeroStun.stun(mob, 50);
        VillainFx.sound(mob, SoundEvents.SHIELD_BREAK, 1.2F, 0.6F);
        level.sendParticles(ParticleTypes.CRIT, mob.getX(), mob.getY(1.0D), mob.getZ(), 20, 0.4D, 0.4D, 0.4D, 0.3D);
        onStagger(level);
    }

    public boolean staggered() {
        return mob.level().getGameTime() < staggerUntil;
    }

    private void beginTransition(ServerLevel level) {
        phase++;
        transitionLeft = 50;
        transitionEndsAt = level.getGameTime() + 50L;
        current = null;
        recoveryLeft = 0;
        poise = 0.0F;
        save();
        updateBar();
        HydraSquad.salute(level, mob, 16.0D);
        onTransition(level, phase);
    }

    private void beginDeath(ServerLevel level) {
        dying = true;
        deathAt = level.getGameTime() + deathTicks();
        transitionLeft = 0;
        transitionEndsAt = 0L;
        current = null;
        recoveryLeft = 0;
        poise = 0.0F;
        releaseHolds();
        mob.getNavigation().stop();
        save();
        onDeathStart(level);
    }

    private void releaseHolds() {
        HeroStun.clear(mob);
        GauntletEntityLocks.forget(mob);
        mob.setNoAi(false);
        mob.setInvulnerable(false);
    }

    private void finish(ServerLevel level) {
        finishing = true;
        finishAttempts++;
        releaseHolds();
        DamageSource source = mob.damageSources().genericKill();
        if (lastPlayer != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(lastPlayer);
            if (player != null && player.level() == level) {
                source = mob.damageSources().playerAttack(player);
                mob.setLastHurtByPlayer(player);
            }
        }
        mob.invulnerableTime = 0;
        mob.hurt(source, Float.MAX_VALUE);
        if (mob.isAlive()) {
            mob.invulnerableTime = 0;
            mob.kill();
        }
        if (mob.isAlive()) {
            mob.setHealth(0.0F);
            mob.die(source);
        }
        if (mob.isAlive() && finishAttempts > 40) {
            mob.remove(Entity.RemovalReason.KILLED);
        }
        if (!mob.isAlive()) {
            finished = true;
            onFinished(level);
        }
    }

    protected void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    protected ServerLevel level() {
        return (ServerLevel) mob.level();
    }

    protected boolean friendly(LivingEntity entity) {
        return entity == mob || Villains.isVillainSide(entity);
    }

    protected void hit(LivingEntity t, float damage) {
        if (friendly(t)) {
            return;
        }
        t.hurt(mob.damageSources().mobAttack(mob), damage);
    }

    protected void hitPiercing(LivingEntity t, float damage) {
        if (friendly(t)) {
            return;
        }
        t.hurt(mob.damageSources().indirectMagic(mob, mob), damage);
    }

    protected boolean inFront(Entity other, double maxDegrees) {
        Vec3 look = Vec3.directionFromRotation(0.0F, mob.getYHeadRot());
        Vec3 to = other.position().subtract(mob.position()).multiply(1.0D, 0.0D, 1.0D);
        if (to.lengthSqr() < 1.0E-4D) {
            return true;
        }
        double dot = look.normalize().dot(to.normalize());
        return dot >= Math.cos(Math.toRadians(maxDegrees));
    }

    protected boolean blinded() {
        return mob.hasEffect(MobEffects.BLINDNESS);
    }

    protected void say(String line) {
        VillainFx.line(level(), mob.position(), 40.0D, "integrated_superheroes.villains." + key() + ".line." + line, color());
    }

    protected void title(String subtitle) {
        VillainFx.title(level(), mob.position(), 48.0D,
                Component.translatable("integrated_superheroes.villains." + key() + ".name").withStyle(color(), ChatFormatting.BOLD),
                Component.translatable("integrated_superheroes.villains." + key() + ".subtitle." + subtitle).withStyle(ChatFormatting.GRAY));
    }

    protected Vec3 groundSpot(Vec3 around, double minDistance, double maxDistance) {
        ServerLevel level = level();
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
            int x = Mth.floor(around.x + Math.cos(angle) * distance);
            int z = Mth.floor(around.z + Math.sin(angle) * distance);
            for (int dy = 3; dy >= -4; dy--) {
                BlockPos pos = BlockPos.containing(x, around.y + dy, z);
                if (level.getBlockState(pos.below()).blocksMotion()
                        && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                    Vec3 spot = Vec3.atBottomCenterOf(pos);
                    if (anchor == null || spot.distanceTo(anchor) <= VillainConfig.LEASH_RADIUS.get()) {
                        return spot;
                    }
                }
            }
        }
        return null;
    }

    protected void lob(Vec3 to, String itemId) {
        net.minecraft.world.entity.projectile.Snowball ball = new net.minecraft.world.entity.projectile.Snowball(level(), mob);
        ball.setItem(new net.minecraft.world.item.ItemStack(HeroFx.item(itemId)));
        ball.addTag(Villains.COSMETIC_TAG);
        Vec3 from = ball.position();
        double ticks = 14.0D;
        Vec3 delta = to.subtract(from);
        ball.setDeltaMovement(delta.x / ticks, delta.y / ticks + 0.5D * 0.03D * ticks, delta.z / ticks);
        level().addFreshEntity(ball);
    }

    protected static double horizontal(Entity a, Vec3 b) {
        return Math.sqrt(a.position().subtract(b).horizontalDistanceSqr());
    }
}
