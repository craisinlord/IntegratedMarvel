package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GrootRoots {
    public static final String WHEEL = "groot_root_wheel";
    public static final String GROW = "groot_grow";
    public static final String SKEWER = "skewer";
    public static final String GRAPPLE = "root_grapple";
    public static final String TRUNK = "trunk_surge";
    public static final String TAKE_ROOT = "take_root";
    public static final String WALL = "branch_wall";
    public static final String SHELTER = "shelter";
    public static final String THORN_BURST = "thorn_burst";
    public static final String SPORES = "spore_glow";
    public static final HeroPower.Toggle PHOTOSYNTHESIS = new HeroPower.Toggle("integrated_superheroes_off_groot_sun", "Photosynthesis", () -> Items.SUNFLOWER);
    private static final ResourceLocation ROOT_SPEED = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "groot_rooted_speed");
    private static final ResourceLocation ROOT_JUMP = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "groot_rooted_jump");
    private static final ResourceLocation ROOT_KNOCKBACK = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "groot_rooted_knockback");
    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState LEAVES = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    private static final BlockState AZALEA = Blocks.AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    private static final List<HeroWheels.Option> OPTIONS = List.of(
            new HeroWheels.Option(SKEWER, "Skewer", () -> Items.POINTED_DRIPSTONE),
            new HeroWheels.Option(GRAPPLE, "Root Grapple", () -> Items.LEAD),
            new HeroWheels.Option(TRUNK, "Trunk Surge", () -> Items.OAK_LOG),
            new HeroWheels.Option(TAKE_ROOT, "Take Root", () -> Items.HANGING_ROOTS),
            new HeroWheels.Option(WALL, "Branch Wall", () -> Items.OAK_FENCE),
            new HeroWheels.Option(SHELTER, "Shelter", () -> Items.AZALEA_LEAVES),
            new HeroWheels.Option(THORN_BURST, "Thorn Burst", () -> Items.SWEET_BERRIES),
            new HeroWheels.Option(SPORES, "Spore Glow", () -> Items.GLOW_BERRIES)
    );
    private static final Map<UUID, String> SELECTED = ServerState.map(new HashMap<>());
    private static final Map<UUID, Map<String, Long>> READY = ServerState.map(new HashMap<>());
    private static final Map<UUID, Rooted> ROOTED = ServerState.map(new HashMap<>());
    private static final Map<UUID, Skewered> SKEWERS = ServerState.map(new HashMap<>());
    private static final Map<UUID, Grapple> GRAPPLES = ServerState.map(new HashMap<>());
    private static final Map<UUID, Surge> SURGES = ServerState.map(new HashMap<>());
    private static final Map<UUID, Shelter> SHELTERS = ServerState.map(new HashMap<>());
    private static final Map<UUID, List<Placed>> PLACED = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> SOFT_LANDING = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> LAST_HURT = ServerState.map(new HashMap<>());

    private static final class Rooted {
        private long until;
        private float absorbed;
    }

    private record Skewered(List<UUID> targets, Vec3 tip, long retractAt) {
    }

    private record Grapple(Vec3 point, UUID entity, long until) {
    }

    private static final class Surge {
        private BlockPos base;
        private boolean self;
        private int placed;
        private int height;
        private int wait;
        private final Set<UUID> launched = new HashSet<>();
    }

    private record Shelter(Vec3 center, long until) {
    }

    private record Placed(ServerLevel level, BlockPos pos, long expires) {
    }

    private GrootRoots() {
    }

    public static HeroWheels.Wheel wheel() {
        return new HeroWheels.Wheel(WHEEL, OPTIONS, (player, option) -> true, GrootRoots::select);
    }

    private static String label(String option) {
        return OPTIONS.stream().filter(entry -> entry.id().equals(option)).map(HeroWheels.Option::label).findFirst().orElse(option);
    }

    private static String selected(LivingEntity player) {
        return SELECTED.getOrDefault(player.getUUID(), SKEWER);
    }

    private static double remaining(ServerPlayer player, String form) {
        Map<String, Long> ready = READY.get(player.getUUID());
        Long at = ready == null ? null : ready.get(form);
        if (at == null) {
            return 0.0D;
        }
        return Math.max(0.0D, (at - player.level().getGameTime()) / 20.0D);
    }

    private static double startCooldown(LivingEntity player, String form, double seconds) {
        if (!(player instanceof ServerPlayer user)) {
            return seconds;
        }
        READY.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                .put(form, player.level().getGameTime() + Math.round(seconds * 20.0D));
        if (form.equals(selected(player))) {
            HeroAbilities.setCooldown(user, GROW, seconds);
        }
        return seconds;
    }

    private static void select(ServerPlayer player, String option) {
        SELECTED.put(player.getUUID(), option);
        HeroAbilities.setCooldown(player, GROW, remaining(player, option));
        player.displayClientMessage(Component.literal("§aRoot form: " + label(option)), true);
        HeroFx.play(player, SoundEvents.AZALEA_LEAVES_PLACE, 0.8F, 1.2F);
    }

    private static void tell(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal("§7" + text), true);
    }

    private static float damage(double multiplier) {
        return (float) (GrootConfig.ABILITY_DAMAGE.get() * multiplier);
    }

    public static float markBonus(LivingEntity target) {
        return target.hasEffect(MobEffects.GLOWING) ? GrootConfig.SPORE_MARK_BONUS.get().floatValue() : 1.0F;
    }

    private static void hurt(LivingEntity player, LivingEntity target, float amount) {
        float total = amount * markBonus(target);
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player instanceof Player user ? player.damageSources().playerAttack(user)
                : player.damageSources().mobAttack(player), total));
    }

    private static boolean ordinary(LivingEntity target) {
        return !(target instanceof Player) && HeroAbilities.canControl(target) && !HeroAbilities.isBoss(target);
    }

    private static boolean foe(LivingEntity player, LivingEntity target) {
        return !(target instanceof ArmorStand) && KineticBurst.affects(player, target);
    }

    public static double grow(ServerPlayer player, int slot) {
        UUID id = player.getUUID();
        if (ROOTED.containsKey(id)) {
            endRoot(player, false);
            HeroFx.play(player, SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 1.0F);
            tell(player, "You pull your roots up early.");
            return remaining(player, selected(player));
        }
        if (GRAPPLES.containsKey(id)) {
            endGrapple(player);
            return remaining(player, selected(player));
        }
        String form = selected(player);
        double left = remaining(player, form);
        if (left > 0.0D) {
            return left;
        }
        return switch (form) {
            case SKEWER -> skewer(player);
            case GRAPPLE -> grapple(player);
            case TRUNK -> trunk(player);
            case TAKE_ROOT -> takeRoot(player);
            case WALL -> wall(player);
            case SHELTER -> shelter(player);
            case THORN_BURST -> thornBurst(player);
            case SPORES -> spores(player);
            default -> 0.0D;
        };
    }

    public static boolean rooted(LivingEntity player) {
        return ROOTED.containsKey(player.getUUID());
    }

    private static void morph(LivingEntity player, int kind, int ticks, Vec3 point, Vec3 aux, int... targets) {
        if (player instanceof ServerPlayer user) {
            GrootMorphs.send(user, kind, ticks, point, aux, targets);
        }
    }

    public static void tickCaster(LivingEntity caster) {
        tickSkewer(caster);
    }

    public static void clearCaster(LivingEntity caster) {
        SKEWERS.remove(caster.getUUID());
    }

    static double skewer(LivingEntity player) {
        double range = GrootConfig.SKEWER_RANGE.get();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, GrootConfig.SKEWER_MAX_TARGETS.get());
        Vec3 tip = beam.end();
        ServerLevel level = HeroCaster.level(player);
        GrootPower.woodLine(player, GrootPower.hand(player), tip);
        player.swing(InteractionHand.MAIN_HAND, true);
        HeroFx.play(player, SoundEvents.WOOD_BREAK, 1.2F, 0.6F);
        List<UUID> pinned = new ArrayList<>();
        int pinTicks = GrootConfig.SKEWER_PIN_TICKS.get();
        float amount = damage(GrootConfig.SKEWER_DAMAGE_MULTIPLIER.get());
        for (LivingEntity target : beam.targets()) {
            if (!foe(player, target)) {
                continue;
            }
            hurt(player, target, amount);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, LOG), target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    10, 0.2D, 0.3D, 0.2D, 0.1D);
            HeroFx.play(target, SoundEvents.THORNS_HIT, 1.0F, 0.6F);
            if (target.isAlive() && ordinary(target)) {
                HeroStun.stun(target, pinTicks);
                pinned.add(target.getUUID());
            }
        }
        int[] hit = GrootMorphs.ids(beam.targets());
        if (beam.targets().isEmpty()) {
            morph(player, GrootMorphs.SKEWER, 12, tip, null, hit);
            return startCooldown(player, SKEWER, GrootConfig.SKEWER_MISS_COOLDOWN.get());
        }
        if (pinned.isEmpty()) {
            morph(player, GrootMorphs.SKEWER, 12, tip, null, hit);
        } else {
            morph(player, GrootMorphs.SKEWER, pinTicks + 8, tip, null, hit);
            SKEWERS.put(player.getUUID(), new Skewered(pinned, tip, level.getGameTime() + pinTicks));
        }
        return startCooldown(player, SKEWER, GrootConfig.SKEWER_COOLDOWN.get());
    }

    private static void tickSkewer(LivingEntity player) {
        Skewered skewered = SKEWERS.get(player.getUUID());
        if (skewered == null || player.level().getGameTime() < skewered.retractAt()) {
            return;
        }
        SKEWERS.remove(player.getUUID());
        ServerLevel level = HeroCaster.level(player);
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
        Vec3 clump = player.position().add(flat.scale(2.0D));
        double reach = GrootConfig.SKEWER_RANGE.get() + 3.0D;
        for (UUID id : skewered.targets()) {
            Entity found = level.getEntity(id);
            if (!(found instanceof LivingEntity target) || !target.isAlive()) {
                continue;
            }
            HeroStun.clear(target);
            if (target.distanceTo(player) > reach) {
                continue;
            }
            Vec3 to = clump.subtract(target.position());
            Vec3 horizontal = new Vec3(to.x, 0.0D, to.z);
            double distance = horizontal.length();
            if (distance < 0.8D) {
                continue;
            }
            double speed = Math.min(GrootConfig.PULL_SPEED.get(), 0.3D + distance * 0.12D);
            Vec3 push = horizontal.normalize().scale(speed);
            target.setDeltaMovement(push.x, 0.3D + Math.max(-0.1D, Math.min(0.5D, to.y * 0.1D)), push.z);
            target.hurtMarked = true;
        }
        HeroFx.play(player, SoundEvents.WOOD_PLACE, 1.2F, 0.5F);
        HeroFx.play(player, SoundEvents.VINE_STEP, 1.2F, 0.7F);
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    private static double grapple(ServerPlayer player) {
        double range = GrootConfig.GRAPPLE_RANGE.get();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 1);
        LivingEntity target = beam.targets().isEmpty() ? null : beam.targets().get(0);
        if (target == null && !beam.hitBlock()) {
            tell(player, "Nothing to grab there.");
            return 0.0D;
        }
        Vec3 point = target == null ? beam.end() : target.getBoundingBox().getCenter();
        long until = player.level().getGameTime() + GrootConfig.GRAPPLE_TICKS.get();
        GRAPPLES.put(player.getUUID(), new Grapple(point, target == null ? null : target.getUUID(), until));
        SOFT_LANDING.put(player.getUUID(), 0L);
        GrootPower.woodLine(player, GrootPower.hand(player), point);
        if (target == null) {
            GrootMorphs.send(player, GrootMorphs.GRAPPLE, GrootConfig.GRAPPLE_TICKS.get() + 4, point);
        } else {
            GrootMorphs.send(player, GrootMorphs.GRAPPLE, GrootConfig.GRAPPLE_TICKS.get() + 4, point, null, target.getId());
        }
        HeroFx.play(player, SoundEvents.WOOD_HIT, 1.0F, 0.6F);
        HeroFx.play(player, SoundEvents.VINE_STEP, 1.2F, 0.7F);
        return startCooldown(player, GRAPPLE, GrootConfig.GRAPPLE_COOLDOWN.get());
    }

    private static void tickGrapple(ServerPlayer player) {
        Grapple grapple = GRAPPLES.get(player.getUUID());
        if (grapple == null) {
            Long landed = SOFT_LANDING.get(player.getUUID());
            if (landed != null) {
                long now = player.level().getGameTime();
                if (landed == 0L && (player.onGround() || player.isInWater())) {
                    SOFT_LANDING.put(player.getUUID(), now + 5L);
                } else if (landed > 0L && now >= landed) {
                    SOFT_LANDING.remove(player.getUUID());
                }
            }
            return;
        }
        Vec3 point = grapple.point();
        if (grapple.entity() != null) {
            Entity found = player.serverLevel().getEntity(grapple.entity());
            if (found instanceof LivingEntity living && living.isAlive()) {
                point = living.getBoundingBox().getCenter();
            }
        }
        Vec3 to = point.subtract(player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D));
        if (player.isShiftKeyDown() || player.level().getGameTime() >= grapple.until() || to.length() < 1.6D
                || VillainWorld.suppressed(player) || HealingFactor.isDowned(player)) {
            endGrapple(player);
            return;
        }
        Vec3 motion = to.normalize().scale(GrootConfig.GRAPPLE_SPEED.get());
        player.setDeltaMovement(motion.x, motion.y + 0.08D, motion.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
    }

    private static void endGrapple(ServerPlayer player) {
        if (GRAPPLES.remove(player.getUUID()) != null) {
            GrootMorphs.end(player, GrootMorphs.GRAPPLE);
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.scale(0.4D));
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            HeroFx.play(player, SoundEvents.WOOD_PLACE, 0.8F, 0.9F);
        }
    }

    private static double trunk(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        int height = GrootConfig.TRUNK_HEIGHT.get();
        Surge surge = new Surge();
        surge.height = height;
        if (player.getXRot() > 70.0F) {
            if (!player.onGround()) {
                tell(player, "You need ground under you.");
                return 0.0D;
            }
            surge.self = true;
            surge.base = player.blockPosition();
        } else {
            Vec3 eye = player.getEyePosition();
            Vec3 end = eye.add(player.getLookAngle().scale(GrootConfig.TRUNK_RANGE.get()));
            BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) {
                tell(player, "Aim at the ground.");
                return 0.0D;
            }
            surge.base = hit.getBlockPos().above();
            if (!level.getBlockState(surge.base).canBeReplaced()) {
                tell(player, "No room for a trunk there.");
                return 0.0D;
            }
        }
        SURGES.put(player.getUUID(), surge);
        if (surge.self) {
            GrootMorphs.send(player, GrootMorphs.TRUNK_LIFT, 60, Vec3.atBottomCenterOf(surge.base));
        } else {
            surge.wait = 5;
            GrootMorphs.send(player, GrootMorphs.TRUNK_SLAM, 14, Vec3.atBottomCenterOf(surge.base));
            player.swing(InteractionHand.MAIN_HAND, true);
        }
        BlockState ground = level.getBlockState(surge.base.below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), surge.base.getX() + 0.5D, surge.base.getY() + 0.1D,
                    surge.base.getZ() + 0.5D, 20, 0.6D, 0.05D, 0.6D, 0.1D);
        }
        HeroFx.play(player, SoundEvents.ROOTED_DIRT_BREAK, 1.2F, 0.6F);
        HeroFx.play(player, SoundEvents.WOOD_PLACE, 1.2F, 0.5F);
        return startCooldown(player, TRUNK, GrootConfig.TRUNK_COOLDOWN.get());
    }

    private static void tickSurge(ServerPlayer player) {
        Surge surge = SURGES.get(player.getUUID());
        if (surge == null) {
            return;
        }
        if (surge.wait > 0) {
            surge.wait--;
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = surge.base.above(surge.placed);
        int ticks = GrootConfig.TRUNK_SECONDS.get() * 20;
        if (surge.self) {
            int clearance = (int) Math.ceil(player.getBbHeight());
            for (int up = 1; up <= clearance + 1; up++) {
                BlockPos above = pos.above(up);
                if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                    SURGES.remove(player.getUUID());
                    return;
                }
            }
            player.connection.teleport(player.getX(), pos.getY() + 1.0D, player.getZ(), player.getYRot(), player.getXRot());
            player.fallDistance = 0.0F;
            place(player, level, pos, LOG, ticks);
        } else {
            AABB cell = new AABB(pos).expandTowards(0.0D, 1.0D, 0.0D).inflate(0.3D, 0.0D, 0.3D);
            float amount = damage(GrootConfig.TRUNK_DAMAGE_MULTIPLIER.get());
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, cell, entity -> entity != player && entity.isAlive())) {
                if (!foe(player, target) || !surge.launched.add(target.getUUID())) {
                    continue;
                }
                hurt(player, target, amount);
                if (ordinary(target)) {
                    Vec3 motion = target.getDeltaMovement();
                    target.setDeltaMovement(motion.x, GrootConfig.TRUNK_LAUNCH.get(), motion.z);
                    target.hurtMarked = true;
                }
                HeroFx.play(target, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.6F);
            }
            if (!place(player, level, pos, LOG, ticks)) {
                finishSurge(player, surge);
                return;
            }
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, LOG), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                8, 0.4D, 0.4D, 0.4D, 0.05D);
        HeroFx.play(player, SoundEvents.WOOD_PLACE, 0.8F, 0.6F + surge.placed * 0.1F);
        surge.placed++;
        if (surge.placed >= surge.height) {
            finishSurge(player, surge);
        }
    }

    private static void finishSurge(ServerPlayer player, Surge surge) {
        SURGES.remove(player.getUUID());
        if (!surge.self) {
            ServerLevel level = player.serverLevel();
            BlockPos top = surge.base.above(Math.max(0, surge.placed));
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, LEAVES), top.getX() + 0.5D, top.getY() + 0.3D, top.getZ() + 0.5D,
                    12, 0.6D, 0.2D, 0.6D, 0.0D);
            if (surge.launched.size() >= 3) {
                HeroAdvancements.award(player, "avengers/groot/timber");
            }
        }
    }

    private static boolean place(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        if (!TempBlocks.place(level, player, pos, state, ticks)) {
            return false;
        }
        PLACED.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>())
                .add(new Placed(level, pos.immutable(), level.getGameTime() + ticks));
        return true;
    }

    private static double wall(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0.0D, flat.x);
        Vec3 center = player.position().add(flat.scale(GrootConfig.WALL_DISTANCE.get()));
        int half = GrootConfig.WALL_WIDTH.get() / 2;
        int height = GrootConfig.WALL_HEIGHT.get();
        int ticks = GrootConfig.WALL_SECONDS.get() * 20;
        int placed = 0;
        Set<BlockPos> columns = new HashSet<>();
        for (int i = -half; i <= half; i++) {
            BlockPos column = BlockPos.containing(center.add(side.scale(i)));
            if (!columns.add(column.atY(0))) {
                continue;
            }
            BlockPos base = floor(level, column);
            if (base == null) {
                continue;
            }
            for (int y = 0; y < height; y++) {
                if (place(player, level, base.above(y), y == height - 1 ? LEAVES : LOG, ticks)) {
                    placed++;
                }
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, LOG), base.getX() + 0.5D, base.getY() + 0.2D, base.getZ() + 0.5D,
                    6, 0.3D, 0.1D, 0.3D, 0.1D);
        }
        if (placed == 0) {
            tell(player, "No room for a wall there.");
            return 0.0D;
        }
        Vec3 floor = new Vec3(center.x, player.getY(), center.z);
        GrootMorphs.send(player, GrootMorphs.WALL, 14, floor.add(side.scale(half + 0.5D)), floor.subtract(side.scale(half + 0.5D)));
        HeroFx.play(player, SoundEvents.ROOTED_DIRT_PLACE, 1.2F, 0.6F);
        HeroFx.play(player, SoundEvents.WOOD_PLACE, 1.2F, 0.5F);
        HeroFx.play(player, SoundEvents.AZALEA_LEAVES_PLACE, 1.0F, 0.8F);
        player.swing(InteractionHand.MAIN_HAND, true);
        return startCooldown(player, WALL, GrootConfig.WALL_COOLDOWN.get());
    }

    private static BlockPos floor(ServerLevel level, BlockPos column) {
        for (int dy = 1; dy >= -2; dy--) {
            BlockPos pos = column.above(dy);
            BlockState below = level.getBlockState(pos.below());
            if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos).getFluidState().isEmpty()
                    && !below.getCollisionShape(level, pos.below()).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    private static double shelter(ServerPlayer player) {
        if (!player.onGround()) {
            tell(player, "You need solid ground to grow a shelter.");
            return 0.0D;
        }
        ServerLevel level = player.serverLevel();
        double radius = GrootConfig.SHELTER_RADIUS.get();
        int ticks = GrootConfig.SHELTER_SECONDS.get() * 20;
        BlockPos feet = player.blockPosition();
        Vec3 center = Vec3.atBottomCenterOf(feet);
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            if (foe(player, target) && ordinary(target)) {
                Vec3 away = target.position().subtract(center);
                Vec3 flat = new Vec3(away.x, 0.0D, away.z);
                flat = flat.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
                target.setDeltaMovement(flat.x * 1.2D, 0.4D, flat.z * 1.2D);
                target.hurtMarked = true;
            }
        }
        int r = (int) Math.ceil(radius) + 1;
        int placed = 0;
        for (int x = -r; x <= r; x++) {
            for (int y = 0; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance < radius - 0.5D || distance > radius + 0.5D) {
                        continue;
                    }
                    BlockState state = y <= 1 ? LOG : (x + y + z) % 3 == 0 ? AZALEA : LEAVES;
                    if (place(player, level, feet.offset(x, y, z), state, ticks)) {
                        placed++;
                    }
                }
            }
        }
        if (placed == 0) {
            tell(player, "No room to grow a shelter here.");
            return 0.0D;
        }
        BlockPos light = feet.above(2);
        place(player, level, light, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12), ticks);
        SHELTERS.put(player.getUUID(), new Shelter(center, level.getGameTime() + ticks));
        GrootMorphs.send(player, GrootMorphs.SHELTER, ticks, center, new Vec3(radius, 0.0D, 0.0D));
        HeroFx.play(player, SoundEvents.AZALEA_LEAVES_PLACE, 1.4F, 0.6F);
        HeroFx.play(player, SoundEvents.WOOD_PLACE, 1.2F, 0.5F);
        HeroFx.play(player, SoundEvents.ROOTED_DIRT_PLACE, 1.2F, 0.7F);
        return startCooldown(player, SHELTER, GrootConfig.SHELTER_COOLDOWN.get());
    }

    private static void tickShelter(ServerPlayer player) {
        Shelter shelter = SHELTERS.get(player.getUUID());
        if (shelter == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.getGameTime() >= shelter.until()) {
            SHELTERS.remove(player.getUUID());
            return;
        }
        double radius = GrootConfig.SHELTER_RADIUS.get();
        if (player.tickCount % 20 == 0) {
            GrootMorphs.send(player, GrootMorphs.SHELTER, (int) (shelter.until() - level.getGameTime()), shelter.center(), new Vec3(radius, 0.0D, 0.0D));
        }
        if (player.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, shelter.center().x, shelter.center().y + 1.5D, shelter.center().z,
                    3, radius * 0.4D, 0.8D, radius * 0.4D, 0.0D);
            level.sendParticles(ParticleTypes.GLOW, shelter.center().x, shelter.center().y + 1.5D, shelter.center().z,
                    1, radius * 0.4D, 0.8D, radius * 0.4D, 0.0D);
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        AABB inside = new AABB(shelter.center(), shelter.center()).inflate(radius, 0.0D, radius).expandTowards(0.0D, radius, 0.0D);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, inside, LivingEntity::isAlive)) {
            if (entity.position().distanceTo(shelter.center()) > radius) {
                continue;
            }
            if (entity == player || entity instanceof Player || !KineticBurst.affects(player, entity) && !(entity instanceof Enemy)) {
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, true), player);
            }
        }
    }

    private static double spores(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double radius = GrootConfig.SPORE_RADIUS.get();
        int glow = GrootConfig.SPORE_GLOW_TICKS.get();
        int marked = 0;
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            if (target instanceof Enemy && foe(player, target)) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, glow, 0, false, false), player);
                marked++;
            }
        }
        int lightTicks = GrootConfig.SPORE_LIGHT_SECONDS.get() * 20;
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, GrootConfig.SPORE_LIGHT_LEVEL.get());
        int lights = 0;
        int spread = (int) Math.min(radius, 10.0D);
        for (int attempt = 0; attempt < 48 && lights < GrootConfig.SPORE_LIGHTS.get(); attempt++) {
            BlockPos pos = player.blockPosition().offset(player.getRandom().nextInt(spread * 2 + 1) - spread,
                    player.getRandom().nextInt(5) - 1, player.getRandom().nextInt(spread * 2 + 1) - spread);
            if (!level.getBlockState(pos).isAir() || level.getBrightness(LightLayer.BLOCK, pos) >= 10) {
                continue;
            }
            if (place(player, level, pos, light, lightTicks)) {
                lights++;
                level.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 4, 0.3D, 0.3D, 0.3D, 0.0D);
            }
        }
        Vec3 at = player.position().add(0.0D, player.getBbHeight() * 0.8D, 0.0D);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, at.x, at.y, at.z, 60, radius * 0.4D, 1.5D, radius * 0.4D, 0.02D);
        level.sendParticles(ParticleTypes.GLOW, at.x, at.y, at.z, 30, radius * 0.4D, 1.5D, radius * 0.4D, 0.02D);
        GrootMorphs.send(player, GrootMorphs.BLOOM, 100, null);
        HeroFx.play(player, SoundEvents.SPORE_BLOSSOM_PLACE, 1.2F, 0.7F);
        HeroFx.play(player, SoundEvents.AZALEA_LEAVES_PLACE, 1.0F, 1.3F);
        if (marked == 0 && lights == 0) {
            tell(player, "The spores drift off.");
        }
        return startCooldown(player, SPORES, GrootConfig.SPORE_COOLDOWN.get());
    }

    private static double takeRoot(ServerPlayer player) {
        if (!player.onGround()) {
            tell(player, "You need solid ground under your feet to put roots down.");
            return 0.0D;
        }
        Rooted rooted = new Rooted();
        rooted.until = player.level().getGameTime() + GrootConfig.TAKE_ROOT_TICKS.get();
        ROOTED.put(player.getUUID(), rooted);
        root(player);
        GrootMorphs.send(player, GrootMorphs.TAKE_ROOT, GrootConfig.TAKE_ROOT_TICKS.get() + 10, null);
        ServerLevel level = player.serverLevel();
        BlockState ground = level.getBlockState(BlockPos.containing(player.getX(), player.getY() - 0.1D, player.getZ()));
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), player.getX(), player.getY() + 0.1D, player.getZ(),
                    24, 0.6D, 0.05D, 0.6D, 0.1D);
        }
        SerumKit.ring(level, player.position().add(0.0D, 0.1D, 0.0D), new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()),
                player.getBbWidth() + 0.4D, 12);
        HeroFx.play(player, SoundEvents.ROOTED_DIRT_PLACE, 1.2F, 0.7F);
        HeroFx.play(player, SoundEvents.WOOD_PLACE, 1.0F, 0.5F);
        return 0.0D;
    }

    static double thornBurst(LivingEntity player) {
        ServerLevel level = HeroCaster.level(player);
        double radius = rooted(player) ? GrootConfig.THORN_BURST_ROOTED_RADIUS.get() : GrootConfig.THORN_BURST_RADIUS.get();
        float amount = damage(GrootConfig.THORN_BURST_DAMAGE_MULTIPLIER.get());
        double push = GrootConfig.THORN_BURST_KNOCKBACK.get();
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            if (!foe(player, target) || !player.hasLineOfSight(target)) {
                continue;
            }
            float total = amount * markBonus(target);
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().thorns(player), total));
            Vec3 away = target.position().subtract(player.position());
            Vec3 flat = new Vec3(away.x, 0.0D, away.z);
            flat = flat.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
            if (ordinary(target)) {
                target.setDeltaMovement(flat.x * push, 0.4D, flat.z * push);
                target.hurtMarked = true;
            } else {
                target.knockback(0.5D, -flat.x, -flat.z);
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState()),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 6, 0.2D, 0.3D, 0.2D, 0.1D);
        }
        for (int ring = 1; ring <= 3; ring++) {
            SerumKit.ring(level, player.position().add(0.0D, 0.2D + ring * 0.4D, 0.0D),
                    new BlockParticleOption(ParticleTypes.BLOCK, LOG), radius * ring / 3.0D, 10 + ring * 6);
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState()),
                center.x, center.y, center.z, 30, radius * 0.4D, 0.6D, radius * 0.4D, 0.15D);
        if (player instanceof ServerPlayer user) {
            GrootMorphs.send(user, GrootMorphs.THORN, 8, null);
        }
        HeroFx.play(player, SoundEvents.SWEET_BERRY_BUSH_PLACE, 1.2F, 0.6F);
        HeroFx.play(player, SoundEvents.THORNS_HIT, 1.0F, 0.7F);
        HeroFx.play(player, SoundEvents.WOOD_BREAK, 1.0F, 0.5F);
        return startCooldown(player, THORN_BURST, GrootConfig.THORN_BURST_COOLDOWN.get());
    }

    public static void tick(ServerPlayer player) {
        tickSkewer(player);
        tickGrapple(player);
        tickSurge(player);
        tickShelter(player);
        tickPlaced(player);
        tickSun(player);
        tickRoot(player);
    }

    private static void tickPlaced(ServerPlayer player) {
        List<Placed> placed = PLACED.get(player.getUUID());
        if (placed == null || player.tickCount % 20 != 0) {
            return;
        }
        placed.removeIf(entry -> entry.level().getGameTime() >= entry.expires());
        if (placed.isEmpty()) {
            PLACED.remove(player.getUUID());
        }
    }

    private static void tickSun(ServerPlayer player) {
        if (player.tickCount % 40 != 0 || !HeroToggles.isOn(player, PHOTOSYNTHESIS) || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        long now = player.level().getGameTime();
        Long hurt = LAST_HURT.get(player.getUUID());
        if (hurt != null && now - hurt < GrootConfig.SUN_CALM_TICKS.get()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos head = BlockPos.containing(player.getEyePosition());
        if (!level.canSeeSky(head) || !(level.isDay() || level.isRainingAt(head))) {
            return;
        }
        player.heal(GrootConfig.SUN_HEAL.get().floatValue());
        GrootMorphs.send(player, GrootMorphs.SPROUT, 50, null);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + player.getBbHeight() * 0.8D, player.getZ(),
                2, 0.3D, 0.3D, 0.3D, 0.0D);
    }

    private static void tickRoot(ServerPlayer player) {
        Rooted rooted = ROOTED.get(player.getUUID());
        if (rooted == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (VillainWorld.suppressed(player) || HealingFactor.isDowned(player)) {
            endRoot(player, false);
            return;
        }
        if (now >= rooted.until) {
            endRoot(player, true);
            return;
        }
        root(player);
        if (player.tickCount % 20 == 0) {
            GrootMorphs.send(player, GrootMorphs.TAKE_ROOT, (int) (rooted.until - now) + 10, null);
        }
        if (player.tickCount % 5 == 0) {
            player.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HANGING_ROOTS.defaultBlockState()),
                    player.getX(), player.getY() + 0.2D, player.getZ(), 3, player.getBbWidth() * 0.6D, 0.1D, player.getBbWidth() * 0.6D, 0.0D);
        }
        int interval = GrootConfig.TAKE_ROOT_LASH_INTERVAL_TICKS.get();
        if ((rooted.until - now) % interval == 0) {
            lash(player);
        }
    }

    private static void lash(ServerPlayer player) {
        double radius = GrootConfig.TAKE_ROOT_LASH_RADIUS.get();
        LivingEntity target = HeroAbilities.sphere(player, radius).stream()
                .filter(entity -> entity instanceof Enemy && foe(player, entity) && player.hasLineOfSight(entity))
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .orElse(null);
        if (target == null) {
            return;
        }
        hurt(player, target, damage(GrootConfig.TAKE_ROOT_LASH_DAMAGE_MULTIPLIER.get()));
        if (target.isAlive() && ordinary(target)) {
            Vec3 to = player.position().subtract(target.position());
            Vec3 flat = new Vec3(to.x, 0.0D, to.z);
            if (flat.lengthSqr() > 4.0D) {
                flat = flat.normalize().scale(0.45D);
                target.setDeltaMovement(flat.x, 0.15D, flat.z);
                target.hurtMarked = true;
            }
        }
        Vec3 point = target.getBoundingBox().getCenter();
        GrootPower.woodLine(player, point, point);
        GrootMorphs.send(player, GrootMorphs.LASH, 12, null, null, target.getId());
        HeroFx.play(target, SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 0.8F);
        HeroFx.play(target, SoundEvents.WOOD_HIT, 1.0F, 0.7F);
        HeroFx.play(player, SoundEvents.VINE_STEP, 1.0F, 0.8F);
    }

    public static void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FALL) && SOFT_LANDING.remove(player.getUUID()) != null) {
            event.setCanceled(true);
            return;
        }
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * GrootConfig.FIRE_DAMAGE_MULTIPLIER.get().floatValue());
        }
        if (event.getAmount() > 0.0F) {
            LAST_HURT.put(player.getUUID(), player.level().getGameTime());
        }
        Rooted rooted = ROOTED.get(player.getUUID());
        if (rooted == null || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float before = event.getAmount();
        float after = before * GrootConfig.TAKE_ROOT_DAMAGE_TAKEN.get().floatValue();
        event.setAmount(after);
        rooted.absorbed += before - after;
        if (before - after > 0.5F) {
            player.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, LOG),
                    player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(), 6, 0.3D, 0.4D, 0.3D, 0.05D);
        }
    }

    private static void endRoot(ServerPlayer player, boolean finished) {
        Rooted rooted = ROOTED.remove(player.getUUID());
        unroot(player);
        if (rooted == null) {
            return;
        }
        GrootMorphs.end(player, GrootMorphs.TAKE_ROOT);
        if (SerumKit.is(player, GrootPower.ID)) {
            startCooldown(player, TAKE_ROOT, GrootConfig.TAKE_ROOT_COOLDOWN.get());
        }
        if (!finished) {
            return;
        }
        HeroFx.play(player, SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 1.0F);
        float heal = GrootConfig.TAKE_ROOT_HEAL.get().floatValue();
        if (heal > 0.0F) {
            player.heal(heal);
            ServerLevel level = player.serverLevel();
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + player.getBbHeight() * 0.6D, player.getZ(),
                    10, 0.4D, 0.6D, 0.4D, 0.0D);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, LEAVES),
                    player.getX(), player.getY() + player.getBbHeight(), player.getZ(), 8, 0.5D, 0.2D, 0.5D, 0.0D);
            HeroFx.play(player, SoundEvents.AZALEA_LEAVES_PLACE, 1.0F, 0.9F);
        }
        if (rooted.absorbed >= 30.0F) {
            HeroAdvancements.award(player, "avengers/groot/thick_bark");
        }
    }

    private static void root(ServerPlayer player) {
        modifier(player, Attributes.MOVEMENT_SPEED, ROOT_SPEED, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(player, Attributes.JUMP_STRENGTH, ROOT_JUMP, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(player, Attributes.KNOCKBACK_RESISTANCE, ROOT_KNOCKBACK, 1.0D, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void modifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && !instance.hasModifier(id)) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void unroot(ServerPlayer player) {
        for (Map.Entry<Holder<Attribute>, ResourceLocation> entry : Map.of(Attributes.MOVEMENT_SPEED, ROOT_SPEED,
                Attributes.JUMP_STRENGTH, ROOT_JUMP, Attributes.KNOCKBACK_RESISTANCE, ROOT_KNOCKBACK).entrySet()) {
            AttributeInstance instance = player.getAttribute(entry.getKey());
            if (instance != null) {
                instance.removeModifier(entry.getValue());
            }
        }
    }

    private static void releaseAll(ServerPlayer player) {
        List<Placed> placed = PLACED.remove(player.getUUID());
        if (placed != null) {
            placed.forEach(entry -> TempBlocks.release(entry.level(), entry.pos()));
        }
        Skewered skewered = SKEWERS.remove(player.getUUID());
        if (skewered != null) {
            for (UUID id : skewered.targets()) {
                Entity found = player.serverLevel().getEntity(id);
                if (found instanceof LivingEntity living) {
                    HeroStun.clear(living);
                }
            }
        }
        GRAPPLES.remove(player.getUUID());
        SURGES.remove(player.getUUID());
        SHELTERS.remove(player.getUUID());
        SOFT_LANDING.remove(player.getUUID());
        GrootMorphs.endAll(player);
    }

    public static void stop(ServerPlayer player) {
        if (ROOTED.containsKey(player.getUUID())) {
            endRoot(player, false);
        }
        releaseAll(player);
        LAST_HURT.remove(player.getUUID());
    }

    public static void clear(ServerPlayer player) {
        ROOTED.remove(player.getUUID());
        unroot(player);
        releaseAll(player);
        LAST_HURT.remove(player.getUUID());
        SELECTED.remove(player.getUUID());
        READY.remove(player.getUUID());
    }

    public static void clearAll() {
        ROOTED.clear();
        SKEWERS.clear();
        GRAPPLES.clear();
        SURGES.clear();
        SHELTERS.clear();
        PLACED.clear();
        SOFT_LANDING.clear();
        LAST_HURT.clear();
        SELECTED.clear();
        READY.clear();
    }
}
