package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.gauntlet.GauntletGrief;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tintankgames.marvel.world.level.MarvelGameRules;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID)
public final class JuggernautCharge {
    public static final TagKey<Block> BREACHABLE = TagKey.create(Registries.BLOCK, IntegratedSuperheroes.id("juggernaut_breachable"));
    private static final ResourceLocation BRACE_KNOCKBACK = IntegratedSuperheroes.id("juggernaut_brace_knockback");
    private static final String READY_TAG = "integrated_superheroes_juggernaut";
    private static final double SUBSTEP = 0.25D;
    private static final int AIRBORNE_LIMIT = 12;
    private static final int LEVEL_EFFECTS_PER_TICK = 6;
    private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.62F, 0.04F, 0.06F), 1.6F);
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final List<Wave> WAVES = new ArrayList<>();
    private static final Map<ResourceKey<Level>, long[]> LEVEL_EFFECTS = new HashMap<>();

    private JuggernautCharge() {
    }

    private enum Phase {
        CHARGING, BRAKING, BRACED, COASTING
    }

    private static final class State {
        private Phase phase = Phase.CHARGING;
        private int age;
        private int phaseTicks;
        private float heading;
        private double speed;
        private float momentum;
        private Vec3 last;
        private int airborne;
        private int stalled;
        private double dealt;
        private int broken;
        private long lastCrunch;
        private Vec3 brace = Vec3.ZERO;
        private final Set<UUID> hit = new HashSet<>();
    }

    private record Sweep(double clear, boolean wall) {
    }

    private record Broken(BlockPos pos, BlockState state) {
    }

    private static final class Wave {
        private final ServerLevel level;
        private final UUID owner;
        private final Vec3 origin;
        private final Vec3 direction;
        private final int length;
        private final float damage;
        private final double knockback;
        private final Set<UUID> hit = new HashSet<>();
        private int y;
        private int step;

        private Wave(ServerLevel level, UUID owner, Vec3 origin, Vec3 direction, int length, float damage, double knockback, int y) {
            this.level = level;
            this.owner = owner;
            this.origin = origin;
            this.direction = direction;
            this.length = length;
            this.damage = damage;
            this.knockback = knockback;
            this.y = y;
        }
    }

    public static boolean charging(Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.phase == Phase.CHARGING;
    }

    static boolean busy(Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && (state.phase == Phase.BRAKING || state.phase == Phase.BRACED);
    }

    static float momentum(Player player) {
        State state = STATES.get(player.getUUID());
        return state == null ? 0.0F : state.momentum;
    }

    static boolean start(ServerPlayer player) {
        if (interrupted(player)) {
            return false;
        }
        if (!player.onGround()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§7You need solid ground under your feet."), true);
            return false;
        }
        Vec3 look = player.getLookAngle();
        if (look.x * look.x + look.z * look.z < 1.0E-4D) {
            return false;
        }
        State state = new State();
        state.heading = player.getYRot();
        state.last = player.position();
        STATES.put(player.getUUID(), state);
        brace(player, true);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2F, 0.75F);
        BlockState ground = level.getBlockState(player.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), player.getX(), player.getY() + 0.1D, player.getZ(),
                    16, 0.5D, 0.05D, 0.5D, 0.2D);
        }
        if (JuggernautPower.shakeOn(player)) {
            HeroFx.shake(level, player.position(), 16.0D, 0.25F, 6);
        }
        return true;
    }

    static void digIn(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.phase != Phase.CHARGING) {
            return;
        }
        state.phase = Phase.BRAKING;
        state.phaseTicks = 0;
        state.momentum = 0.0F;
        state.brace = Vec3.directionFromRotation(0.0F, state.heading);
        startCooldown(player, JuggernautPower.CHARGE, JuggernautConfig.CHARGE_COOLDOWN.get());
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 1.0F, 0.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, 1.4F, 0.5F);
    }

    static float spend(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return 0.0F;
        }
        float spent = state.momentum;
        if (state.phase == Phase.CHARGING) {
            end(player, state);
        }
        state.momentum = 0.0F;
        if (state.phase == Phase.COASTING) {
            STATES.remove(player.getUUID());
        }
        return spent;
    }

    public static void clear(ServerPlayer player) {
        State state = STATES.remove(player.getUUID());
        brace(player, false);
        WAVES.removeIf(wave -> wave.owner.equals(player.getUUID()));
        if (state == null) {
            return;
        }
        if (state.phase == Phase.CHARGING) {
            persist(player, JuggernautPower.CHARGE, JuggernautConfig.CHARGE_COOLDOWN.get());
        }
        SerumKit.meter(player, -1.0F);
        SerumKit.clearMeter(player);
    }

    static boolean frontal(ServerPlayer player, DamageSource source) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.phase != Phase.BRACED) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return false;
        }
        Vec3 from = source.getSourcePosition();
        if (from == null) {
            return false;
        }
        Vec3 to = new Vec3(from.x - player.getX(), 0.0D, from.z - player.getZ());
        if (to.lengthSqr() < 1.0E-4D) {
            return false;
        }
        double minDot = Math.cos(Math.toRadians(JuggernautConfig.DIG_IN_ARC_DEGREES.get()));
        return to.normalize().dot(state.brace) >= minDot;
    }

    static void tick(ServerPlayer player) {
        if (player.tickCount % 20 == 0) {
            syncCooldowns(player);
        }
        State state = STATES.get(player.getUUID());
        if (state != null) {
            switch (state.phase) {
                case CHARGING -> tickCharge(player, state);
                case BRAKING -> tickBrake(player, state);
                case BRACED -> tickBrace(player, state);
                case COASTING -> tickCoast(player, state);
            }
        }
        State now = STATES.get(player.getUUID());
        SerumKit.meter(player, now == null || now.momentum <= 0.0F ? -1.0F : now.momentum / 100.0F);
    }

    private static boolean interrupted(ServerPlayer player) {
        return !player.isAlive() || player.isSpectator() || player.isPassenger() || player.isInWater() || player.isInLava()
                || player.isFallFlying() || player.getAbilities().flying || player.isSleeping()
                || VillainWorld.suppressed(player) || HealingFactor.isDowned(player);
    }

    private static void tickCharge(ServerPlayer player, State state) {
        state.age++;
        if (interrupted(player) || state.age > JuggernautConfig.CHARGE_MAX_TICKS.get()) {
            end(player, state);
            return;
        }
        boolean grounded = player.onGround();
        state.airborne = grounded ? 0 : state.airborne + 1;
        if (state.airborne > AIRBORNE_LIMIT) {
            end(player, state);
            return;
        }
        Vec3 now = player.position();
        Vec3 previous = state.last;
        double dx = now.x - previous.x;
        double dz = now.z - previous.z;
        double moved = Math.sqrt(dx * dx + dz * dz);
        float share = state.momentum / 100.0F;
        float maxTurn = (float) Mth.lerp(share, JuggernautConfig.CHARGE_TURN_DEGREES.get(), JuggernautConfig.CHARGE_FULL_MOMENTUM_TURN_DEGREES.get());
        float turn = Mth.clamp(Mth.wrapDegrees(player.getYRot() - state.heading), -maxTurn, maxTurn);
        state.heading = Mth.wrapDegrees(state.heading + turn);
        if (grounded && moved <= state.speed * 1.6D + 0.15D) {
            double gain = moved * JuggernautConfig.MOMENTUM_PER_BLOCK.get() * (Math.abs(turn) < 1.5F ? 1.25D : 1.0D);
            state.momentum = (float) Math.min(100.0D, state.momentum + gain);
        }
        state.momentum = Math.max(0.0F, state.momentum - Math.abs(turn) * 0.6F);
        state.last = now;
        double cap = JuggernautConfig.CHARGE_MAX_SPEED.get();
        int accel = JuggernautConfig.CHARGE_ACCELERATION_TICKS.get();
        double target = cap * Math.min(1.0D, state.age / (double) accel);
        if (state.age > accel) {
            if (state.momentum <= 0.0F) {
                end(player, state);
                return;
            }
            target = Math.min(target, cap * (0.4D + 0.6D * state.momentum / 100.0D));
        }
        state.speed = target;
        Vec3 heading = Vec3.directionFromRotation(0.0F, state.heading);
        if (hitEntities(player, state, previous.subtract(now), heading)) {
            impact(player, state, heading, true);
            return;
        }
        if (JuggernautPower.footstepsOn(player)) {
            HeroStride.tick(player, 1.6D, SoundEvents.RAVAGER_STEP, 1.3F, 0.7F, 8, JuggernautPower.shakeOn(player) ? 0.12F : 0.0F);
        }
        if (!grounded) {
            return;
        }
        Sweep sweep = sweep(player, state, heading, state.speed * 2.0D + 0.5D);
        double move = Math.min(state.speed, sweep.clear());
        if (sweep.wall() && sweep.clear() < state.speed) {
            push(player, heading, Math.max(0.0D, sweep.clear() - 0.05D));
            impact(player, state, heading, false);
            return;
        }
        if (move < state.speed) {
            state.momentum = Math.max(0.0F, state.momentum - 2.0F);
            state.stalled = move < 0.05D ? state.stalled + 1 : 0;
            if (state.stalled >= 3) {
                impact(player, state, heading, false);
                return;
            }
        } else {
            state.stalled = 0;
        }
        push(player, heading, move);
    }

    private static void push(ServerPlayer player, Vec3 heading, double distance) {
        player.setDeltaMovement(heading.x * distance, -0.08D, heading.z * distance);
        player.hurtMarked = true;
    }

    private static void tickBrake(ServerPlayer player, State state) {
        state.phaseTicks++;
        if (interrupted(player)) {
            finish(player);
            return;
        }
        state.speed *= 0.55D;
        if (player.onGround() && state.speed > 0.02D) {
            push(player, state.brace, state.speed);
        }
        if (state.phaseTicks < JuggernautConfig.DIG_IN_BRAKE_TICKS.get()) {
            return;
        }
        state.phase = Phase.BRACED;
        state.phaseTicks = 0;
        ServerLevel level = player.serverLevel();
        BlockState ground = level.getBlockState(player.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), player.getX(), player.getY() + 0.1D, player.getZ(),
                    24, 0.6D, 0.05D, 0.6D, 0.15D);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1.0F, 0.5F);
        if (JuggernautPower.shakeOn(player)) {
            HeroFx.shake(level, player.position(), 10.0D, 0.2F, 5);
        }
    }

    private static void tickBrace(ServerPlayer player, State state) {
        state.phaseTicks++;
        if (interrupted(player) || state.phaseTicks >= JuggernautConfig.DIG_IN_TICKS.get()) {
            finish(player);
            return;
        }
        if (state.phaseTicks % 6 == 0) {
            Vec3 front = player.position().add(state.brace.scale(0.7D)).add(0.0D, 1.2D, 0.0D);
            player.serverLevel().sendParticles(CRIMSON, front.x, front.y, front.z, 3, 0.3D, 0.4D, 0.3D, 0.0D);
        }
    }

    private static void tickCoast(ServerPlayer player, State state) {
        state.phaseTicks++;
        if (state.phaseTicks > 20) {
            state.momentum -= 5.0F;
        }
        if (state.momentum <= 0.0F) {
            STATES.remove(player.getUUID());
        }
    }

    private static void finish(ServerPlayer player) {
        STATES.remove(player.getUUID());
        brace(player, false);
    }

    private static void end(ServerPlayer player, State state) {
        brace(player, false);
        startCooldown(player, JuggernautPower.CHARGE, JuggernautConfig.CHARGE_COOLDOWN.get());
        state.phase = Phase.COASTING;
        state.phaseTicks = 0;
        if (state.momentum <= 0.0F) {
            STATES.remove(player.getUUID());
        }
    }

    private static void impact(ServerPlayer player, State state, Vec3 heading, boolean boss) {
        double cap = JuggernautConfig.CHARGE_MAX_SPEED.get();
        boolean hard = state.speed >= cap * JuggernautConfig.IMPACT_SPEED_FRACTION.get();
        state.momentum *= 0.5F;
        Set<UUID> spared = new HashSet<>(state.hit);
        end(player, state);
        ServerLevel level = player.serverLevel();
        if (!hard) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.PLAYERS, 0.8F, 0.6F);
            return;
        }
        Vec3 front = player.position().add(heading.scale(0.9D)).add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, front.x, front.y, front.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(CRIMSON, front.x, front.y, front.z, 16, 0.6D, 0.6D, 0.6D, 0.0D);
        level.playSound(null, front.x, front.y, front.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.3F, 0.5F);
        level.playSound(null, front.x, front.y, front.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 0.8F);
        float damage = JuggernautConfig.IMPACT_SHOCKWAVE_DAMAGE.get().floatValue();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(front, front).inflate(3.0D, 1.5D, 3.0D),
                entity -> !spared.contains(entity.getUUID()) && KineticBurst.affects(player, entity))) {
            if (damage > 0.0F) {
                HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().mobAttack(player), damage));
            }
            if (HeroAbilities.canControl(target)) {
                SerumKit.push(target, player.position(), 0.9D, 0.35D);
            }
        }
        if (!boss) {
            player.setDeltaMovement(-heading.x * 0.3D, 0.2D, -heading.z * 0.3D);
            player.hurtMarked = true;
        }
        HeroAnimations.play(player, "bettercombat:one_handed_slam");
        if (JuggernautPower.shakeOn(player)) {
            HeroFx.shake(level, player.position(), 20.0D, 0.6F, 10);
        }
    }

    private static boolean hitEntities(ServerPlayer player, State state, Vec3 back, Vec3 heading) {
        ServerLevel level = player.serverLevel();
        AABB box = player.getBoundingBox().expandTowards(back).expandTowards(heading.scale(0.75D)).inflate(0.4D);
        List<LivingEntity> targets = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && entity.isAlive() && !state.hit.contains(entity.getUUID()) && KineticBurst.affects(player, entity)));
        if (targets.isEmpty()) {
            return false;
        }
        targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
        int limit = JuggernautConfig.CHARGE_MAX_TARGETS_PER_TICK.get();
        double share = state.momentum / 100.0D;
        double base = MarvelBalanceConfig.STEEL_CHARGE_DAMAGE.get()
                * Mth.lerp(share, JuggernautConfig.CHARGE_MIN_DAMAGE_MULTIPLIER.get(), JuggernautConfig.CHARGE_MAX_DAMAGE_MULTIPLIER.get());
        double knockback = JuggernautConfig.CHARGE_KNOCKBACK.get() * (0.5D + share);
        boolean bossHit = false;
        int count = 0;
        for (LivingEntity target : targets) {
            if (count >= limit) {
                break;
            }
            count++;
            state.hit.add(target.getUUID());
            float damage = (float) Math.min(base, JuggernautConfig.CHARGE_MAX_DAMAGE_PER_CAST.get() - state.dealt);
            if (damage > 0.0F) {
                state.dealt += damage;
                HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().mobAttack(player), damage));
            }
            if (HeroAbilities.isBoss(target) || !HeroAbilities.canControl(target)) {
                bossHit = true;
                continue;
            }
            target.knockback(knockback, -heading.x, -heading.z);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.3D, 0.0D));
            target.hurtMarked = true;
            state.momentum = Math.max(0.0F, state.momentum - JuggernautConfig.MOMENTUM_PER_HIT.get().floatValue());
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.2F, 0.7F);
        Vec3 front = player.position().add(heading.scale(0.8D)).add(0.0D, 1.0D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT, front.x, front.y, front.z, 10, 0.4D, 0.4D, 0.4D, 0.3D);
        return bossHit;
    }

    private static boolean breaching(ServerPlayer player, State state) {
        return JuggernautPower.breachOn(player) && JuggernautConfig.BREACH_ALLOWED.get()
                && state.momentum >= JuggernautConfig.BREACH_MIN_MOMENTUM.get() && player.mayBuild()
                && player.level().getGameRules().getBoolean(MarvelGameRules.RULE_SUPERPOWERGRIEFING);
    }

    private static boolean eligible(BlockState state, ServerLevel level, BlockPos pos) {
        if (!state.is(BREACHABLE) || state.is(GauntletGrief.IMMUNE) || state.is(Tags.Blocks.ORES) || state.hasBlockEntity()
                || level.getBlockEntity(pos) != null || !state.getFluidState().isEmpty()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0.0F && hardness <= JuggernautConfig.BREACH_MAX_HARDNESS.get();
    }

    private static Sweep sweep(ServerPlayer player, State state, Vec3 heading, double lookahead) {
        ServerLevel level = player.serverLevel();
        AABB base = player.getBoundingBox();
        double feet = base.minY;
        double step = player.maxUpStep();
        boolean breach = breaching(player, state);
        int tickBudget = breach ? JuggernautConfig.BREACH_BLOCKS_PER_TICK.get() : 0;
        int chargeBudget = JuggernautConfig.BREACH_BLOCKS_PER_CHARGE.get();
        CollisionContext context = CollisionContext.of(player);
        Set<BlockPos> cleared = new HashSet<>();
        List<Broken> broken = new ArrayList<>();
        double clear = 0.0D;
        boolean wall = false;
        boolean stop = false;
        for (double d = SUBSTEP; d <= lookahead + 1.0E-6D && !stop; d += SUBSTEP) {
            AABB moved = base.move(heading.x * d, 0.0D, heading.z * d);
            AABB probe = new AABB(moved.minX + 0.001D, feet + 0.01D, moved.minZ + 0.001D, moved.maxX - 0.001D, moved.maxY - 0.001D, moved.maxZ - 0.001D);
            Set<BlockPos> blocking = new LinkedHashSet<>();
            for (BlockPos cursor : BlockPos.betweenClosed(Mth.floor(probe.minX), Mth.floor(probe.minY), Mth.floor(probe.minZ),
                    Mth.floor(probe.maxX), Mth.floor(probe.maxY), Mth.floor(probe.maxZ))) {
                if (cleared.contains(cursor)) {
                    continue;
                }
                if (!level.isInWorldBounds(cursor) || !level.isLoaded(cursor)) {
                    wall = true;
                    stop = true;
                    break;
                }
                BlockState block = level.getBlockState(cursor);
                VoxelShape shape = block.getCollisionShape(level, cursor, context);
                if (shape.isEmpty()) {
                    continue;
                }
                for (AABB part : shape.toAabbs()) {
                    AABB placed = part.move(cursor);
                    if (placed.intersects(probe) && placed.maxY > feet + step + 1.0E-3D) {
                        blocking.add(cursor.immutable());
                        break;
                    }
                }
            }
            if (stop) {
                break;
            }
            if (!blocking.isEmpty()) {
                if (!breach || state.broken + blocking.size() > chargeBudget) {
                    wall = true;
                    break;
                }
                if (broken.size() + blocking.size() > tickBudget) {
                    break;
                }
                List<Broken> batch = new ArrayList<>();
                for (BlockPos pos : blocking) {
                    BlockState block = level.getBlockState(pos);
                    if (!eligible(block, level, pos)) {
                        wall = true;
                        break;
                    }
                    batch.add(new Broken(pos, block));
                }
                if (wall) {
                    break;
                }
                for (Broken entry : batch) {
                    if (!HeroGrief.allowBreak(player, level, entry.pos())) {
                        wall = true;
                        break;
                    }
                }
                if (wall) {
                    break;
                }
                for (Broken entry : batch) {
                    level.removeBlock(entry.pos(), false);
                    level.gameEvent(GameEvent.BLOCK_DESTROY, entry.pos(), GameEvent.Context.of(player, entry.state()));
                    cleared.add(entry.pos());
                    broken.add(entry);
                    state.broken++;
                }
            }
            clear = d;
        }
        if (!broken.isEmpty()) {
            breachEffects(player, state, broken);
        }
        return new Sweep(clear, wall);
    }

    private static void breachEffects(ServerPlayer player, State state, List<Broken> broken) {
        ServerLevel level = player.serverLevel();
        int shown = 0;
        for (Broken entry : broken) {
            if (shown >= 2 || !levelEffect(level)) {
                break;
            }
            level.levelEvent(2001, entry.pos(), Block.getId(entry.state()));
            shown++;
        }
        long now = level.getGameTime();
        if (now - state.lastCrunch >= 4L) {
            state.lastCrunch = now;
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.PLAYERS,
                    1.1F, 0.5F + level.random.nextFloat() * 0.15F);
            if (JuggernautPower.shakeOn(player)) {
                HeroFx.shake(level, player.position(), 12.0D, 0.25F, 5);
            }
        }
    }

    private static boolean levelEffect(ServerLevel level) {
        long[] slot = LEVEL_EFFECTS.computeIfAbsent(level.dimension(), key -> new long[]{-1L, 0L});
        long now = level.getGameTime();
        if (slot[0] != now) {
            slot[0] = now;
            slot[1] = 0L;
        }
        if (slot[1] >= LEVEL_EFFECTS_PER_TICK) {
            return false;
        }
        slot[1]++;
        return true;
    }

    private static void brace(ServerPlayer player, boolean on) {
        AttributeInstance instance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (instance == null) {
            return;
        }
        if (instance.getModifier(BRACE_KNOCKBACK) != null) {
            instance.removeModifier(BRACE_KNOCKBACK);
        }
        if (on) {
            instance.addTransientModifier(new AttributeModifier(BRACE_KNOCKBACK, 1.0D, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    static void launchShock(ServerPlayer player, float spent) {
        ServerLevel level = player.serverLevel();
        double share = Math.min(1.0D, spent / 100.0D);
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0D, look.z);
        direction = direction.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, player.getYRot()) : direction.normalize();
        int length = Math.max(3, (int) Math.round(JuggernautConfig.SHOCK_LENGTH.get() * share));
        float damage = (float) (JuggernautConfig.SHOCK_DAMAGE.get() * share);
        double knockback = JuggernautConfig.SHOCK_KNOCKBACK.get() * (0.5D + share * 0.5D);
        WAVES.add(new Wave(level, player.getUUID(), player.position(), direction, length, damage, knockback, Mth.floor(player.getY())));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2F, 0.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 0.6F);
        level.sendParticles(CRIMSON, player.getX(), player.getY() + 0.3D, player.getZ(), 20, 0.8D, 0.1D, 0.8D, 0.0D);
        HeroAnimations.play(player, "bettercombat:two_handed_slam");
        if (JuggernautPower.shakeOn(player)) {
            HeroFx.shake(level, player.position(), 24.0D, 0.5F, 10);
        }
    }

    private static BlockPos ground(ServerLevel level, int x, int y, int z) {
        for (int dy = 1; dy >= -3; dy--) {
            BlockPos pos = new BlockPos(x, y + dy - 1, z);
            if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) {
                return null;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.isFaceSturdy(level, pos, Direction.UP) && !level.getBlockState(pos.above()).isSuffocating(level, pos.above())) {
                return pos;
            }
        }
        return null;
    }

    private static void tickWaves() {
        if (WAVES.isEmpty()) {
            return;
        }
        Iterator<Wave> iterator = WAVES.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            ServerPlayer owner = wave.level.getServer().getPlayerList().getPlayer(wave.owner);
            if (owner == null || owner.level() != wave.level || wave.step * 2 >= wave.length) {
                iterator.remove();
                continue;
            }
            boolean blocked = false;
            for (int d = wave.step * 2 + 1; d <= Math.min(wave.length, wave.step * 2 + 2) && !blocked; d++) {
                Vec3 at = wave.origin.add(wave.direction.scale(d));
                BlockPos floor = ground(wave.level, Mth.floor(at.x), wave.y, Mth.floor(at.z));
                if (floor == null) {
                    blocked = true;
                    break;
                }
                wave.y = floor.getY() + 1;
                BlockState state = wave.level.getBlockState(floor);
                Vec3 side = new Vec3(-wave.direction.z, 0.0D, wave.direction.x);
                for (int w = -1; w <= 1; w++) {
                    Vec3 point = at.add(side.scale(w));
                    wave.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), point.x, floor.getY() + 1.1D, point.z,
                            6, 0.3D, 0.2D, 0.3D, 0.15D);
                }
                wave.level.sendParticles(CRIMSON, at.x, floor.getY() + 1.3D, at.z, 4, 0.8D, 0.2D, 0.8D, 0.0D);
                AABB box = new AABB(at.x - 1.5D, floor.getY() + 1.0D, at.z - 1.5D, at.x + 1.5D, floor.getY() + 3.5D, at.z + 1.5D);
                for (LivingEntity target : wave.level.getEntitiesOfClass(LivingEntity.class, box,
                        entity -> !wave.hit.contains(entity.getUUID()) && KineticBurst.affects(owner, entity))) {
                    wave.hit.add(target.getUUID());
                    if (wave.damage > 0.0F) {
                        HeroAbilities.runAsAbilityDamage(() -> target.hurt(owner.damageSources().mobAttack(owner), wave.damage));
                    }
                    if (HeroAbilities.canControl(target)) {
                        target.setDeltaMovement(target.getDeltaMovement().add(wave.direction.x * wave.knockback, 0.45D, wave.direction.z * wave.knockback));
                        target.hurtMarked = true;
                    }
                }
            }
            wave.step++;
            if (blocked) {
                iterator.remove();
            }
        }
    }

    static double remaining(ServerPlayer player, String ability) {
        long until = ready(player).getLong(ability);
        long now = clock(player);
        return until > now ? (until - now) / 20.0D : 0.0D;
    }

    static void startCooldown(ServerPlayer player, String ability, double seconds) {
        HeroAbilities.setCooldown(player, ability, seconds);
        persist(player, ability, seconds);
    }

    static void persist(ServerPlayer player, String ability, double seconds) {
        ready(player).putLong(ability, clock(player) + (long) Math.ceil(seconds * 20.0D));
    }

    private static void syncCooldowns(ServerPlayer player) {
        boolean changed = false;
        for (String ability : List.of(JuggernautPower.CHARGE, JuggernautPower.SHOCK)) {
            if (ability.equals(JuggernautPower.CHARGE) && STATES.containsKey(player.getUUID())) {
                continue;
            }
            double left = remaining(player, ability);
            if (left <= 0.0D) {
                continue;
            }
            com.stxllarplanes.marvellegacy.power.Power power = HeroAbilities.slotPower(player, ability);
            if (power != null && power.getCooldown() + 1.0D < left) {
                power.setCooldown(left);
                changed = true;
            }
        }
        if (changed) {
            com.stxllarplanes.marvellegacy.data.PlayerData.sync(player);
        }
    }

    private static long clock(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    private static CompoundTag ready(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        CompoundTag kept = root.getCompound(Player.PERSISTED_NBT_TAG);
        root.put(Player.PERSISTED_NBT_TAG, kept);
        CompoundTag mine = kept.getCompound(READY_TAG);
        kept.put(READY_TAG, mine);
        return mine;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickWaves();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.clear();
        WAVES.clear();
        LEVEL_EFFECTS.clear();
    }
}
