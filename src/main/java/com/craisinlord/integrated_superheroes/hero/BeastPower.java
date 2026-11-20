package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.api.RogueAbsorption;
import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.hero.skin.SerumCostumes;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class BeastPower {
    public static final String ID = "acrobatics_hank_mccoy";
    public static final String POUNCE = "beast_pounce";
    public static final String SWEEP = "beast_weighted_sweep";
    public static final HeroPower.Toggle LOW_LIGHT = new HeroPower.Toggle("integrated_superheroes_off_senses", "Low-Light Vision", () -> Items.ENDER_EYE);
    private static final int IDLE = 0;
    private static final int POUNCING = 1;
    private static final int WINDOW = 2;
    private static final int KICKING = 3;
    private static final int POUNCE_TICKS = 24;
    private static final int KICK_TICKS = 12;
    private static final int FALL_GUARD_TICKS = 200;
    private static final int WALL_MEMORY_TICKS = 60;
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Map<UUID, Long> STUMBLE_IMMUNE = new HashMap<>();
    private static RogueMemory rogue;
    private static boolean registered;

    private static final class State {
        private int phase = IDLE;
        private long started;
        private long until;
        private Vec3 direction = Vec3.ZERO;
        private UUID first;
        private boolean fromWall;
        private long fallGuardUntil;
        private boolean leftGround;
    }

    private BeastPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(BeastPower::onLogout);
            NeoForge.EVENT_BUS.addListener(BeastPower::onChangeDimension);
            NeoForge.EVENT_BUS.addListener(BeastPower::onStopping);
            SurfaceCling.register(player -> SerumKit.is(player, ID) && HeroToggles.isOn(player, SurfaceCling.TOGGLE),
                    new SurfaceCling.Style(BeastPower::clingLateral, 0.0D, "claw_climb",
                            "thematic:spiderman_wall_bump_1", true));
            SerumCostumes.register(ID, "thematic:beast");
            RogueAbsorption.remember(rogueMemory());
        }
        return new HeroPower(
                ID,
                "Acrobatic Mutation (Hank McCoy)",
                syringe,
                BeastConfig.BEAST,
                List.of("agility"),
                List.of(
                        new HeroPower.Ability(POUNCE, "Pounce", BeastPower::pounce),
                        new HeroPower.Ability(SWEEP, "Weighted Sweep", BeastPower::sweep),
                        HeroToggles.WHEEL
                ),
                List.of(SurfaceCling.TOGGLE, HeroToggles.SUPER_LEAP, LOW_LIGHT),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (player.tickCount % 20 == 0) {
                            Map<Holder<MobEffect>, Integer> effects = Map.of(MobEffects.NIGHT_VISION, 0,
                                    MobEffects.JUMP, BeastConfig.LEAP_JUMP_LEVEL.get() - 1);
                            Map<Holder<MobEffect>, HeroPower.Toggle> toggles = Map.of(MobEffects.NIGHT_VISION, LOW_LIGHT,
                                    MobEffects.JUMP, HeroToggles.SUPER_LEAP);
                            HeroToggles.applyEffects(player, effects, toggles);
                        }
                        tickState(player);
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        STATES.remove(player.getUUID());
                        HeroAbilities.clearDash(player);
                        SurfaceCling.clear(player);
                        SerumKit.dropEffect(player, MobEffects.NIGHT_VISION);
                        SerumKit.dropEffect(player, MobEffects.JUMP);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (!event.getSource().is(DamageTypeTags.IS_FALL)) {
                            return;
                        }
                        State state = STATES.get(player.getUUID());
                        if (state != null && state.fallGuardUntil > player.level().getGameTime()) {
                            state.fallGuardUntil = 0L;
                            event.setCanceled(true);
                        }
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        clear(player);
                    }

                    @Override
                    public void jump(Player player) {
                        if (!player.isShiftKeyDown() || !HeroToggles.isOn(player, HeroToggles.SUPER_LEAP)) {
                            return;
                        }
                        HeroAnimations.playLeapFlip(player);
                        Vec3 motion = player.getDeltaMovement();
                        double horizontal = player.isSprinting() ? BeastConfig.LEAP_HORIZONTAL.get() : 1.0D;
                        player.setDeltaMovement(motion.x * horizontal, motion.y * BeastConfig.LEAP_MULTIPLIER.get(), motion.z * horizontal);
                        player.hurtMarked = true;
                    }
                }
        );
    }

    public static double clingLateral() {
        return BeastConfig.CLING_LATERAL.get();
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
    }

    private static boolean target(ServerPlayer player, LivingEntity entity) {
        return !(entity instanceof ArmorStand) && KineticBurst.affects(player, entity) && player.hasLineOfSight(entity);
    }

    private static float pounceDamage(double multiplier) {
        return (float) (MarvelBalanceConfig.POUNCE_DAMAGE.get() * multiplier);
    }

    private static double pounce(ServerPlayer player, int slot) {
        State state = state(player);
        long now = player.level().getGameTime();
        if (state.phase == WINDOW && now < state.until) {
            return rebound(player, state, now);
        }
        if (state.phase != IDLE) {
            return 0.0D;
        }
        int up = HeroGravity.up(player);
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            flat = Vec3.directionFromRotation(0.0F, player.getYRot());
        }
        flat = flat.normalize();
        double speed = BeastConfig.POUNCE_SPEED.get();
        double lift = Math.min(1.0D, Math.max(0.45D, look.y * up + 0.3D));
        player.setDeltaMovement(flat.x * speed, lift * up, flat.z * speed);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        SurfaceCling.release(player);
        state.phase = POUNCING;
        state.started = now;
        state.until = now + POUNCE_TICKS;
        state.direction = flat;
        state.first = null;
        state.fromWall = SurfaceCling.clungWithin(player, WALL_MEMORY_TICKS);
        state.fallGuardUntil = now + FALL_GUARD_TICKS;
        state.leftGround = false;
        HeroAnimations.play(player, "flip_both_1");
        HeroFx.play(player, SoundEvents.GOAT_LONG_JUMP, 1.0F, 0.8F);
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1D, player.getZ(), 6, 0.3D, 0.05D, 0.3D, 0.02D);
        return 0.0D;
    }

    private static double rebound(ServerPlayer player, State state, long now) {
        double range = BeastConfig.REBOUND_RANGE.get();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        UUID first = state.first;
        LivingEntity next = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), entity -> {
                    if (entity == player || !entity.isAlive() || entity.getUUID().equals(first) || !target(player, entity)) {
                        return false;
                    }
                    Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
                    return to.length() <= range && to.normalize().dot(look) >= 0.6D;
                }).stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .orElse(null);
        int up = HeroGravity.up(player);
        if (next != null) {
            Vec3 path = next.getBoundingBox().getCenter().subtract(player.position());
            Vec3 dir = path.normalize();
            double speed = Math.min(1.6D, 0.6D + path.length() * 0.15D);
            player.setDeltaMovement(dir.x * speed, Math.max(0.2D, dir.y * up * speed + 0.15D) * up, dir.z * speed);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            state.phase = KICKING;
            state.started = now;
            state.until = now + KICK_TICKS;
            state.direction = new Vec3(dir.x, 0.0D, dir.z).lengthSqr() < 1.0E-4D ? state.direction : new Vec3(dir.x, 0.0D, dir.z).normalize();
            state.fallGuardUntil = now + FALL_GUARD_TICKS;
            state.leftGround = false;
            HeroAnimations.play(player, "kick");
            HeroFx.play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.3F);
            return 0.0D;
        }
        Vec3 normal = surface(player);
        if (normal == null) {
            player.displayClientMessage(Component.literal("§7Nothing close enough to kick off."), true);
            return 0.0D;
        }
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize();
        Vec3 push = normal.scale(0.8D).add(flat.scale(0.4D));
        player.setDeltaMovement(push.x, 0.65D * up, push.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        SurfaceCling.release(player);
        state.phase = IDLE;
        state.first = null;
        state.fallGuardUntil = now + FALL_GUARD_TICKS;
        state.leftGround = false;
        HeroAnimations.play(player, "kick");
        HeroFx.cue(player, "thematic:spiderman_wall_whack_1", 0.9F, 1.0F);
        player.serverLevel().sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0D, player.getZ(), 6, 0.2D, 0.3D, 0.2D, 0.02D);
        return BeastConfig.POUNCE_COOLDOWN.get();
    }

    private static Vec3 surface(ServerPlayer player) {
        Direction wall = SurfaceCling.wallFace(player);
        if (wall != null) {
            return new Vec3(-wall.getStepX(), 0.0D, -wall.getStepZ());
        }
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(3.0D)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection().getAxis() == Direction.Axis.Y) {
            return null;
        }
        Direction face = hit.getDirection();
        return new Vec3(face.getStepX(), 0.0D, face.getStepZ());
    }

    private static void tickState(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (!player.onGround()) {
            state.leftGround = true;
        } else if (state.leftGround && state.phase != POUNCING && state.phase != KICKING) {
            state.fallGuardUntil = 0L;
        }
        if (player.isInWater() || now >= state.fallGuardUntil) {
            state.fallGuardUntil = 0L;
        }
        if (state.phase == POUNCING || state.phase == KICKING) {
            if (VillainWorld.suppressed(player) || HealingFactor.isDowned(player)) {
                finish(player, state, BeastConfig.POUNCE_COOLDOWN.get());
                return;
            }
            LivingEntity hit = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(0.8D),
                            entity -> entity != player && entity.isAlive() && !entity.getUUID().equals(state.first) && target(player, entity))
                    .stream()
                    .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                    .orElse(null);
            if (hit != null) {
                if (state.phase == POUNCING) {
                    landPounce(player, state, hit, now);
                } else {
                    landKick(player, state, hit);
                }
                return;
            }
            boolean landed = now - state.started >= 4L && player.onGround();
            if (landed || now >= state.until) {
                finish(player, state, BeastConfig.POUNCE_COOLDOWN.get());
            }
            return;
        }
        if (state.phase == WINDOW && now >= state.until) {
            finish(player, state, BeastConfig.POUNCE_COOLDOWN.get());
            return;
        }
        if (state.phase == IDLE && state.fallGuardUntil == 0L) {
            STATES.remove(player.getUUID());
        }
    }

    private static void landPounce(ServerPlayer player, State state, LivingEntity hit, long now) {
        float damage = pounceDamage(BeastConfig.POUNCE_DAMAGE_MULTIPLIER.get());
        HeroAbilities.runAsAbilityDamage(() -> hit.hurt(player.damageSources().playerAttack(player), damage));
        Vec3 dir = state.direction;
        hit.knockback(0.8D, -dir.x, -dir.z);
        int up = HeroGravity.up(player);
        player.setDeltaMovement(-dir.x * 0.3D, 0.45D * up, -dir.z * 0.3D);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        state.phase = WINDOW;
        state.first = hit.getUUID();
        state.until = now + BeastConfig.REBOUND_WINDOW_TICKS.get();
        ServerLevel level = player.serverLevel();
        Vec3 at = hit.getBoundingBox().getCenter();
        HeroFx.vfx(level, at, "punch_pressure_wave", 0, () ->
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 12, 0.3D, 0.3D, 0.3D, 0.2D));
        HeroFx.play(player, HeroFx.sound("thematic:beast_bench_burst", SoundEvents.PLAYER_ATTACK_STRONG), 0.9F, 1.1F);
        player.displayClientMessage(Component.literal("§bRebound ready"), true);
    }

    private static void landKick(ServerPlayer player, State state, LivingEntity hit) {
        float damage = pounceDamage(BeastConfig.REBOUND_DAMAGE_MULTIPLIER.get());
        HeroAbilities.runAsAbilityDamage(() -> hit.hurt(player.damageSources().playerAttack(player), damage));
        Vec3 dir = state.direction;
        hit.knockback(1.0D, -dir.x, -dir.z);
        int up = HeroGravity.up(player);
        player.setDeltaMovement(-dir.x * 0.5D, 0.5D * up, -dir.z * 0.5D);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        ServerLevel level = player.serverLevel();
        Vec3 at = hit.getBoundingBox().getCenter();
        HeroFx.vfx(level, at, "impact_starburst", 0, () ->
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 16, 0.3D, 0.3D, 0.3D, 0.25D));
        HeroFx.play(player, HeroFx.sound("thematic:beast_kick", SoundEvents.PLAYER_ATTACK_KNOCKBACK), 1.0F, 1.0F);
        if (state.fromWall) {
            HeroAdvancements.award(player, "x_men/beast/bank_shot");
        }
        double cooldown = BeastConfig.POUNCE_COOLDOWN.get() * (1.0D - BeastConfig.REBOUND_REFUND.get());
        finish(player, state, cooldown);
    }

    private static void finish(ServerPlayer player, State state, double cooldown) {
        state.phase = IDLE;
        state.first = null;
        state.fromWall = false;
        if (cooldown > 0.0D) {
            HeroAbilities.setCooldown(player, POUNCE, cooldown);
        }
    }

    private static double sweep(ServerPlayer player, int slot) {
        double range = BeastConfig.SWEEP_RANGE.get();
        float damage = (float) (MarvelBalanceConfig.CLAW_SWIPE_DAMAGE.get() * BeastConfig.SWEEP_DAMAGE_MULTIPLIER.get());
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
        Vec3 forward = flat;
        double minDot = Math.cos(Math.toRadians(70.0D));
        int up = HeroGravity.up(player);
        long now = player.level().getGameTime();
        int stumble = BeastConfig.SWEEP_STUMBLE_TICKS.get();
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range, 1.5D, range), entity -> {
            if (entity == player || !target(player, entity)) {
                return false;
            }
            Vec3 to = entity.position().subtract(player.position());
            double height = to.y * up;
            Vec3 across = new Vec3(to.x, 0.0D, to.z);
            double distance = across.length();
            return height > -1.0D && height < 1.5D && distance <= range && (distance < 0.5D || across.normalize().dot(forward) >= minDot);
        });
        for (LivingEntity target : targets) {
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
            target.knockback(0.4D, -forward.x, -forward.z);
            if (stumble > 0 && HeroAbilities.canControl(target) && !HeroAbilities.isBoss(target)) {
                Long immune = STUMBLE_IMMUNE.get(target.getUUID());
                if (immune == null || now >= immune) {
                    HeroStun.stun(target, stumble);
                    STUMBLE_IMMUNE.put(target.getUUID(), now + stumble + BeastConfig.SWEEP_STUMBLE_IMMUNITY_TICKS.get());
                }
            }
        }
        if (STUMBLE_IMMUNE.size() > 256) {
            STUMBLE_IMMUNE.values().removeIf(until -> until < now);
        }
        ServerLevel level = player.serverLevel();
        for (int i = -1; i <= 1; i++) {
            double angle = Math.toRadians(i * 45.0D);
            double x = forward.x * Math.cos(angle) - forward.z * Math.sin(angle);
            double z = forward.x * Math.sin(angle) + forward.z * Math.cos(angle);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + x * 1.8D, player.getY() + 0.4D * up, player.getZ() + z * 1.8D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.sendParticles(ParticleTypes.POOF, player.getX() + forward.x, player.getY() + 0.1D * up, player.getZ() + forward.z, 8, 1.0D, 0.05D, 1.0D, 0.01D);
        HeroFx.play(player, HeroFx.sound("thematic:beast_sweep", SoundEvents.PLAYER_ATTACK_SWEEP), 1.0F, 0.9F);
        HeroAnimations.play(player, ModCompat.BETTER_COMBAT ? "bettercombat:two_handed_slash_horizontal_right" : "kick");
        return BeastConfig.SWEEP_COOLDOWN.get();
    }

    public static RogueMemory rogueMemory() {
        if (rogue == null) {
            rogue = RogueKit.memory("beast", "Beast")
                    .active("Rebound Kick", BeastPower::rogueLeapStrike)
                    .passives(RogueKit.effects(RogueKit.fx(MobEffects.JUMP, 0)), new RogueMemory.Passive() {
                        @Override
                        public void end(ServerPlayer player) {
                            HeroAbilities.clearDash(player);
                        }
                    })
                    .build();
        }
        return rogue;
    }

    private static double rogueLeapStrike(ServerPlayer player) {
        int up = HeroGravity.up(player);
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
        double lift = Math.min(0.8D, Math.max(0.4D, look.y * up + 0.25D));
        HeroAbilities.startDash(player, new Vec3(flat.x * 1.1D, lift * up, flat.z * 1.1D), 16,
                RogueKit.dmg(MarvelBalanceConfig.POUNCE_DAMAGE.get() * BeastConfig.REBOUND_DAMAGE_MULTIPLIER.get()), 1.0D,
                entity -> !(entity instanceof ArmorStand) && RogueKit.affects(player, entity) && player.hasLineOfSight(entity));
        player.fallDistance = 0.0F;
        HeroAnimations.play(player, "kick");
        HeroFx.play(player, HeroFx.sound("thematic:beast_kick", SoundEvents.PLAYER_ATTACK_KNOCKBACK), 1.0F, 1.1F);
        return BeastConfig.POUNCE_COOLDOWN.get();
    }

    private static void clear(ServerPlayer player) {
        State state = STATES.remove(player.getUUID());
        if (state != null && state.phase != IDLE && SerumKit.is(player, ID)) {
            HeroAbilities.setCooldown(player, POUNCE, BeastConfig.POUNCE_COOLDOWN.get());
        }
        SurfaceCling.clear(player);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    private static void onStopping(ServerStoppingEvent event) {
        STATES.clear();
        STUMBLE_IMMUNE.clear();
    }
}
