package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class SwarmSquirrel extends TamableAnimal implements GeoEntity {
    private static final EntityDataAccessor<Boolean> GLIDING = SynchedEntityData.defineId(SwarmSquirrel.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CLIMBING = SynchedEntityData.defineId(SwarmSquirrel.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PICKING_UP_NUT = SynchedEntityData.defineId(SwarmSquirrel.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TEXTURE_VARIANT = SynchedEntityData.defineId(SwarmSquirrel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> QUIRK_TICKS = SynchedEntityData.defineId(SwarmSquirrel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> QUIRK_VARIANT = SynchedEntityData.defineId(SwarmSquirrel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> SHOULDER_PLAYER = SynchedEntityData.defineId(SwarmSquirrel.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final String TEXTURE_VARIANT_KEY = "TextureVariant";
    private static final String[] TEXTURE_VARIANT_NAMES = {"regular", "brown", "albino"};
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation CLIMB_ANIM = RawAnimation.begin().thenLoop("climbing");
    private static final RawAnimation GLIDE_ANIM = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation PICKUP_ANIM = RawAnimation.begin().thenPlay("item_pickup");
    private static final RawAnimation QUIRK_1_ANIM = RawAnimation.begin().thenPlay("quirk1");
    private static final RawAnimation QUIRK_2_ANIM = RawAnimation.begin().thenPlay("quirk2");
    private static final String QUIRK_CONTROLLER = "quirk_controller";
    private static final String QUIRK_1_TRIGGER = "quirk1";
    private static final String QUIRK_2_TRIGGER = "quirk2";
    private static final int QUIRK_NONE = 0;
    private static final int QUIRK_1 = 1;
    private static final int QUIRK_2 = 2;
    private static final int QUIRK_1_DURATION_TICKS = 40;
    private static final int QUIRK_2_DURATION_TICKS = 30;
    private static final int MIN_QUIRK_COOLDOWN_TICKS = 260;
    private static final int MAX_QUIRK_COOLDOWN_TICKS = 420;
    private static final int MAX_GLIDE_TICKS = 160;
    private static final int PICKUP_ANIM_TICKS = 60;
    private static final double MIN_GLIDE_DISTANCE = 10.0D;
    private static final double MAX_GLIDE_DISTANCE = 64.0D;
    private static final int MIN_CLIMB_HEIGHT = 5;
    private static final int MAX_CLIMB_HEIGHT = 18;
    private static final int GROUND_ESCAPE_MIN_CLIMB_HEIGHT = 12;
    private static final float FALL_GLIDE_TRIGGER_DISTANCE = 0.75F;
    private static final double FALL_GLIDE_TRIGGER_SPEED = -0.08D;
    private static final int EMERGENCY_GLIDE_REPEAT_COOLDOWN = 60;
    private static final int RECENT_GLIDE_REPEAT_COOLDOWN = 80;
    private static final double CLIMB_START_DISTANCE_SQR = 4.0D;
    private static final int CLIMB_APPROACH_TIMEOUT_TICKS = 120;
    private static final int CLIMB_NO_PROGRESS_LIMIT = 25;
    private static final int FAILED_CLIMB_TARGET_COOLDOWN = 80;
    private static final int GROUND_STALL_FORCE_WANDER_TICKS = 100;
    private static final double IDLE_OWNER_RANGE = 10.0D;
    private static final int SHOULDER_CLIMB_TICKS = 20;
    private static final int SHOULDER_SNEAK_GRACE_TICKS = 10;
    private static final double SHOULDER_FORWARD_OFFSET = 0.0475D;
    private static final double SHOULDER_HEAD_Y_OFFSET = 0.26D;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int wanderCooldown;
    @Nullable
    private BlockPos glideTarget;
    @Nullable
    private Vec3 glideDirection;
    @Nullable
    private BlockPos glideLaunch;
    private boolean emergencyGlide;
    private int glideTicks;
    private int glideRecoveryTicks;
    private int glideSoundCooldown;
    @Nullable
    private BlockPos recentEmergencyGlideTarget;
    private int recentEmergencyGlideCooldown;
    @Nullable
    private BlockPos recentGlideLaunch;
    @Nullable
    private BlockPos recentGlideTarget;
    private int recentGlideCooldown;
    private int nextFallGlideScanTick;
    @Nullable
    private BlockPos cachedFallGlideTarget;
    private int nextBestGlideScanTick;
    @Nullable
    private BlockPos cachedBestGlideTarget;
    private int idleStallTicks;
    @Nullable
    private BlockPos recentFailedClimbTarget;
    private int recentFailedClimbCooldown;
    private int quirkCooldownTicks;
    private int lastTriggeredQuirkVariant = QUIRK_NONE;
    private UUID shoulderClimbPlayer;
    private Vec3 shoulderClimbStart;
    private int shoulderClimbTicks;
    private int shoulderSneakGraceTicks;
    private boolean tippy;
    private float biteDamage = 1.0F;
    private ItemStack carried = ItemStack.EMPTY;
    private ItemEntity forageTarget;
    private int pickupTicks;
    private LivingEntity pileTarget;
    private int pileTicks;
    private int pileIndex;
    private float pileDamage;
    private BlockPos guardPos;
    private int idleTicks;

    public SwarmSquirrel(EntityType<? extends SwarmSquirrel> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPathfindingMalus(PathType.LEAVES, -2.0F);
        this.quirkCooldownTicks = this.nextQuirkCooldown();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    public void setup(ServerPlayer owner, boolean tippy, float biteDamage, double health) {
        this.tippy = tippy;
        this.biteDamage = biteDamage;
        this.setTame(true, false);
        this.setOwnerUUID(owner.getUUID());
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        this.setHealth((float) health);
        int variant = tippy ? 2 : this.random.nextFloat() < 0.2F ? 1 : 0;
        this.setTextureVariant(variant);
        this.wanderCooldown = 40 + this.random.nextInt(60);
        if (tippy) {
            this.setCustomName(net.minecraft.network.chat.Component.literal("Tippy-Toe"));
        }
    }

    public boolean isTippy() {
        return this.tippy;
    }

    public boolean ownedBy(Player player) {
        UUID owner = this.getOwnerUUID();
        return owner != null && owner.equals(player.getUUID());
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new SwarmSquirrelNavigation(this, level);
    }

    @Override
    public boolean onClimbable() {
        return this.isClimbingTrunk() || this.horizontalCollision && !this.isOnShoulder() && !this.isClimbingToShoulder();
    }

    private boolean free() {
        return !this.isOnShoulder() && !this.isClimbingToShoulder() && this.pileTarget == null && !this.isOrderedToSit();
    }

    private boolean idle() {
        return this.free() && this.guardPos == null && this.getTarget() == null && this.pickupTicks <= 0
                && !this.isGliding() && !this.hasActiveQuirkAnimation() && this.ownerNearby(this.position(), IDLE_OWNER_RANGE);
    }

    private boolean ownerNearby(Vec3 pos, double range) {
        LivingEntity owner = this.getOwner();
        if (owner == null) {
            return false;
        }
        double dx = owner.getX() - pos.x;
        double dz = owner.getZ() - pos.z;
        return dx * dx + dz * dz <= range * range;
    }

    private String order() {
        if (this.tippy || !(this.getOwner() instanceof Player owner)) {
            return SquirrelGirlPower.DEFEND;
        }
        return SquirrelGirlPower.order(owner);
    }

    private boolean defending() {
        String order = this.order();
        return SquirrelGirlPower.DEFEND.equals(order) || this.guardPos != null;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new HoldGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.5D, true) {
            @Override
            public boolean canUse() {
                return SwarmSquirrel.this.free() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return SwarmSquirrel.this.free() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(4, new GuardGoal());
        this.goalSelector.addGoal(5, new ForageGoal());
        this.goalSelector.addGoal(6, new GnawGoal());
        this.goalSelector.addGoal(7, new FollowOwnerGoal(this, 1.3D, 10.0F, 5.0F) {
            @Override
            public boolean canUse() {
                return SwarmSquirrel.this.free() && SwarmSquirrel.this.guardPos == null && !SwarmSquirrel.this.isClimbingTrunk() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return SwarmSquirrel.this.free() && SwarmSquirrel.this.guardPos == null && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(8, new ClimbGoal());
        this.goalSelector.addGoal(9, new ArborealRandomStrollGoal());
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return SwarmSquirrel.this.free() && super.canUse();
            }
        });
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this) {
            @Override
            public boolean canUse() {
                return SwarmSquirrel.this.free() && SwarmSquirrel.this.defending() && super.canUse();
            }
        });
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return SwarmSquirrel.this.free() && super.canUse();
            }
        });
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, this::threat) {
            @Override
            public boolean canUse() {
                return SwarmSquirrel.this.free() && SwarmSquirrel.this.defending() && super.canUse();
            }
        });
    }

    private boolean threat(LivingEntity entity) {
        if (!(entity instanceof Mob mob) || entity instanceof SwarmSquirrel) {
            return false;
        }
        if (this.guardPos != null) {
            return entity instanceof Enemy && entity.distanceToSqr(Vec3.atCenterOf(this.guardPos)) < 144.0D;
        }
        return this.getOwner() != null && mob.getTarget() == this.getOwner();
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target == owner || target instanceof SwarmSquirrel) {
            return false;
        }
        if (target instanceof TamableAnimal tamable && tamable.isTame() && owner.getUUID().equals(tamable.getOwnerUUID())) {
            return false;
        }
        return !(target instanceof Player player) || !(owner instanceof Player ownerPlayer) || ownerPlayer.canHarmPlayer(player);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        LivingEntity owner = this.getOwner();
        return super.canAttack(target) && (owner == null || this.wantsToAttack(target, owner));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (target != null && !this.level().isClientSide) {
            this.stopQuirkAnimation();
            if (this.isClimbingTrunk() && !this.isClimbingToShoulder()) {
                this.setClimbingTrunk(false);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = this.bite(target, this.biteDamage);
        if (hit) {
            this.swing(InteractionHand.MAIN_HAND);
            if (this.random.nextInt(3) == 0) {
                this.playSound(ModSounds.SWARM_SQUIRREL_BEG.get(), 0.5F, 1.3F + this.random.nextFloat() * 0.3F);
            }
        }
        return hit;
    }

    private boolean bite(Entity target, float damage) {
        Vec3 motion = target.getDeltaMovement();
        target.invulnerableTime = 0;
        boolean hit = target.hurt(this.damageSources().mobAttack(this), damage);
        target.setDeltaMovement(motion);
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        Entity attacker = source.getEntity();
        UUID owner = this.getOwnerUUID();
        if (owner != null && attacker != null && (owner.equals(attacker.getUUID())
                || attacker instanceof SwarmSquirrel squirrel && owner.equals(squirrel.getOwnerUUID()))) {
            return false;
        }
        if ((this.isOnShoulder() || this.isClimbingToShoulder()) && !this.level().isClientSide) {
            this.dismountShoulder();
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide && this.tippy && this.getOwner() instanceof ServerPlayer owner) {
            SquirrelGirlPower.tippyLost(owner);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason != RemovalReason.UNLOADED_TO_CHUNK && reason != RemovalReason.UNLOADED_WITH_PLAYER && !this.carried.isEmpty()) {
            this.spawnAtLocation(this.carried);
            this.carried = ItemStack.EMPTY;
        }
        super.remove(reason);
    }

    public void vanish() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.3D, this.getZ(), 6, 0.2D, 0.2D, 0.2D, 0.02D);
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        this.tickTimers();
        Vec3 motion = this.getDeltaMovement();
        double horizontalSpeed = motion.horizontalDistanceSqr();
        if (!this.isGliding() && !this.isClimbingTrunk() && !this.isClimbingToShoulder() && !this.isOnShoulder()
                && this.pileTarget == null && horizontalSpeed > 1.0E-4D) {
            float moveYaw = (float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) - 90.0F;
            this.setYRot(Mth.approachDegrees(this.getYRot(), moveYaw, 10.0F));
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
        }
        this.tickSquirrel(horizontalSpeed);
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount % 10 == 0 && !this.ownerValid()) {
            this.vanish();
            return;
        }
        if (this.pileTarget != null) {
            this.tickPile();
        }
        if (this.tippy) {
            this.tickTippy();
        }
    }

    private void tickTimers() {
        if (this.wanderCooldown > 0) {
            this.wanderCooldown--;
        }
        if (this.shoulderSneakGraceTicks > 0) {
            this.shoulderSneakGraceTicks--;
        }
        if (this.glideRecoveryTicks > 0) {
            this.glideRecoveryTicks--;
        }
        if (this.recentEmergencyGlideCooldown > 0 && --this.recentEmergencyGlideCooldown == 0) {
            this.recentEmergencyGlideTarget = null;
        }
        if (this.recentGlideCooldown > 0 && --this.recentGlideCooldown == 0) {
            this.recentGlideLaunch = null;
            this.recentGlideTarget = null;
        }
        if (this.recentFailedClimbCooldown > 0 && --this.recentFailedClimbCooldown == 0) {
            this.recentFailedClimbTarget = null;
        }
    }

    private boolean ownerValid() {
        if (!(this.getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || owner.level() != this.level()
                || this.distanceToSqr(owner) > 48.0D * 48.0D || !SquirrelGirlPower.has(owner)) {
            return false;
        }
        return this.tippy ? SquirrelGirlPower.isTippy(owner, this) : SquirrelGirlPower.swarmOut(owner);
    }

    private void tickTippy() {
        if (this.isOnShoulder() || this.isClimbingToShoulder() || !(this.getOwner() instanceof ServerPlayer owner)) {
            this.idleTicks = 0;
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() || this.pileTarget != null || this.isGliding()) {
            this.idleTicks = 0;
            return;
        }
        this.idleTicks++;
        if (this.idleTicks < 60) {
            return;
        }
        if (this.distanceToSqr(owner) < 6.0D && this.onGround() && !this.isClimbingTrunk()) {
            this.mountShoulder(owner);
            this.idleTicks = 0;
        } else if (this.getNavigation().isDone()) {
            this.getNavigation().moveTo(owner, 1.4D);
        }
    }

    public void defend(LivingEntity attacker) {
        if (this.isOnShoulder() || this.isClimbingToShoulder()) {
            this.dismountShoulder();
            this.setDeltaMovement(attacker.position().subtract(this.position()).normalize().scale(0.6D).add(0.0D, 0.35D, 0.0D));
        }
        this.setOrderedToSit(false);
        this.setTarget(attacker);
        this.idleTicks = 0;
    }

    public void focus(LivingEntity target) {
        this.guardPos = null;
        this.setTarget(target);
    }

    public void guard(BlockPos pos) {
        this.guardPos = pos;
        this.setTarget(null);
        this.cancelTravel();
    }

    public void clearGuard() {
        this.guardPos = null;
    }

    public void pile(LivingEntity target, int ticks, float damage, int index) {
        this.pileTarget = target;
        this.pileTicks = ticks;
        this.pileDamage = damage;
        this.pileIndex = index;
        this.stopQuirkAnimation();
        this.setClimbingTrunk(false);
        if (this.isGliding()) {
            this.stopGliding();
        }
        this.cancelTravel();
    }

    private void tickPile() {
        LivingEntity target = this.pileTarget;
        if (target == null || !target.isAlive() || target.level() != this.level() || this.pileTicks-- <= 0) {
            this.pileTarget = null;
            this.setNoGravity(false);
            this.setDeltaMovement(this.random.nextGaussian() * 0.2D, 0.3D, this.random.nextGaussian() * 0.2D);
            if (target != null && target.isAlive()) {
                this.setTarget(target);
            }
            return;
        }
        double angle = this.pileIndex * 2.39996D + this.tickCount * 0.05D;
        double radius = target.getBbWidth() * 0.45D;
        double height = target.getBbHeight() * (0.35D + (this.pileIndex % 4) * 0.2D);
        Vec3 spot = target.position().add(Math.cos(angle) * radius, Math.min(height, target.getBbHeight()), Math.sin(angle) * radius);
        Vec3 step = spot.subtract(this.position());
        if (step.length() > 0.8D) {
            spot = this.position().add(step.normalize().scale(0.8D));
        }
        this.setPos(spot.x, spot.y, spot.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.fallDistance = 0.0F;
        this.lookAt(target, 30.0F, 30.0F);
        if (this.pileTicks % 10 == 0 && this.distanceToSqr(target) < 9.0D) {
            this.bite(target, this.pileDamage);
            this.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !this.isOnShoulder() && super.isPushable();
    }

    @Override
    public void push(Entity entity) {
        if (!this.isOnShoulder()) {
            super.push(entity);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GLIDING, false);
        builder.define(CLIMBING, false);
        builder.define(PICKING_UP_NUT, false);
        builder.define(TEXTURE_VARIANT, 0);
        builder.define(QUIRK_TICKS, 0);
        builder.define(QUIRK_VARIANT, QUIRK_NONE);
        builder.define(SHOULDER_PLAYER, Optional.empty());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TEXTURE_VARIANT_KEY, this.entityData.get(TEXTURE_VARIANT));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setTextureVariant(tag.getInt(TEXTURE_VARIANT_KEY));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, this::animate));
        controllers.add(new AnimationController<>(this, QUIRK_CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim(QUIRK_1_TRIGGER, QUIRK_1_ANIM)
                .triggerableAnim(QUIRK_2_TRIGGER, QUIRK_2_ANIM));
    }

    private PlayState animate(AnimationState<SwarmSquirrel> state) {
        if (this.hasActiveQuirkAnimation()) {
            return PlayState.STOP;
        }
        if (this.isPickingUpNut() || this.isOnShoulder()) {
            return state.setAndContinue(PICKUP_ANIM);
        }
        if (this.isGliding()) {
            state.getController().setAnimationSpeed(1.1D);
            return state.setAndContinue(GLIDE_ANIM);
        }
        if (this.isClimbingToShoulder() || this.isClimbingTrunk()) {
            return state.setAndContinue(CLIMB_ANIM);
        }
        if (this.getDeltaMovement().horizontalDistanceSqr() > 0.001D && this.onGround()) {
            state.getController().setAnimationSpeed(1.35D);
            return state.setAndContinue(WALK_ANIM);
        }
        state.getController().setAnimationSpeed(1.0D);
        return state.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    public boolean isGliding() {
        return this.entityData.get(GLIDING);
    }

    public boolean isClimbingTrunk() {
        return this.entityData.get(CLIMBING);
    }

    private void setClimbingTrunk(boolean climbing) {
        this.entityData.set(CLIMBING, climbing);
    }

    public boolean isPickingUpNut() {
        return this.entityData.get(PICKING_UP_NUT);
    }

    public void setPickingUpNut(boolean pickingUpNut) {
        this.entityData.set(PICKING_UP_NUT, pickingUpNut);
    }

    public String getTextureVariantName() {
        return TEXTURE_VARIANT_NAMES[Mth.clamp(this.entityData.get(TEXTURE_VARIANT), 0, TEXTURE_VARIANT_NAMES.length - 1)];
    }

    private void setTextureVariant(int variant) {
        this.entityData.set(TEXTURE_VARIANT, Mth.clamp(variant, 0, TEXTURE_VARIANT_NAMES.length - 1));
    }

    private int getQuirkTicks() {
        return this.entityData.get(QUIRK_TICKS);
    }

    private void setQuirkTicks(int ticks) {
        this.entityData.set(QUIRK_TICKS, Math.max(0, ticks));
    }

    private int getQuirkVariant() {
        return this.entityData.get(QUIRK_VARIANT);
    }

    private void setQuirkVariant(int variant) {
        this.entityData.set(QUIRK_VARIANT, Mth.clamp(variant, QUIRK_NONE, QUIRK_2));
    }

    private boolean hasActiveQuirkAnimation() {
        return this.getQuirkVariant() != QUIRK_NONE && this.getQuirkTicks() > 0;
    }

    private int nextQuirkCooldown() {
        return MIN_QUIRK_COOLDOWN_TICKS + this.random.nextInt(MAX_QUIRK_COOLDOWN_TICKS - MIN_QUIRK_COOLDOWN_TICKS + 1);
    }

    private boolean canUseQuirkAnimation() {
        return this.onGround()
                && !this.isInWaterOrBubble()
                && !this.isOrderedToSit()
                && !this.isPickingUpNut()
                && !this.isGliding()
                && !this.isClimbingTrunk()
                && !this.isClimbingToShoulder()
                && !this.isOnShoulder()
                && this.pileTarget == null
                && this.getTarget() == null
                && this.getNavigation().isDone()
                && this.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D;
    }

    private void tickQuirkAnimation() {
        if (this.getQuirkTicks() > 0) {
            if (this.canUseQuirkAnimation()) {
                this.setQuirkTicks(this.getQuirkTicks() - 1);
            } else {
                this.stopQuirkAnimation();
                this.quirkCooldownTicks = this.nextQuirkCooldown();
            }
            return;
        }
        if (this.getQuirkVariant() != QUIRK_NONE) {
            this.setQuirkVariant(QUIRK_NONE);
        }
        if (this.quirkCooldownTicks > 0) {
            this.quirkCooldownTicks--;
            return;
        }
        if (!this.canUseQuirkAnimation()) {
            return;
        }
        int variant = this.random.nextBoolean() ? QUIRK_1 : QUIRK_2;
        this.setQuirkVariant(variant);
        this.setQuirkTicks(variant == QUIRK_1 ? QUIRK_1_DURATION_TICKS : QUIRK_2_DURATION_TICKS);
        this.quirkCooldownTicks = this.nextQuirkCooldown();
    }

    private void stopQuirkAnimation() {
        this.setQuirkTicks(0);
        this.setQuirkVariant(QUIRK_NONE);
    }

    private void syncQuirkAnimationTrigger() {
        int variant = this.getQuirkVariant();
        if (variant != QUIRK_NONE && this.getQuirkTicks() > 0 && this.lastTriggeredQuirkVariant != variant) {
            this.triggerAnim(QUIRK_CONTROLLER, variant == QUIRK_2 ? QUIRK_2_TRIGGER : QUIRK_1_TRIGGER);
            this.lastTriggeredQuirkVariant = variant;
            return;
        }
        if (variant == QUIRK_NONE) {
            this.lastTriggeredQuirkVariant = QUIRK_NONE;
        }
    }

    private void startPickupAnimation() {
        this.pickupTicks = PICKUP_ANIM_TICKS;
        this.setPickingUpNut(true);
        this.cancelTravel();
        this.setDeltaMovement(Vec3.ZERO);
    }

    private void tickPickupAnimation() {
        if (this.pickupTicks <= 0) {
            return;
        }
        this.cancelTravel();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        if (--this.pickupTicks <= 0) {
            this.pickupTicks = 0;
            this.setPickingUpNut(false);
        }
    }

    public boolean isOnShoulder() {
        return this.entityData.get(SHOULDER_PLAYER).isPresent();
    }

    public boolean isClimbingToShoulder() {
        return this.shoulderClimbTicks > 0 && this.shoulderClimbPlayer != null;
    }

    @Nullable
    private Player shoulderPlayer() {
        return this.entityData.get(SHOULDER_PLAYER).map(id -> this.level().getPlayerByUUID(id)).orElse(null);
    }

    public void mountShoulder(Player player) {
        if (player.isSpectator()) {
            return;
        }
        this.shoulderClimbPlayer = player.getUUID();
        this.shoulderClimbStart = this.position();
        this.shoulderClimbTicks = SHOULDER_CLIMB_TICKS;
        this.shoulderSneakGraceTicks = SHOULDER_SNEAK_GRACE_TICKS;
        this.setOrderedToSit(false);
        this.setClimbingTrunk(false);
        this.stopQuirkAnimation();
        if (this.isGliding()) {
            this.stopGliding();
        }
        this.setNoGravity(true);
        this.cancelTravel();
        this.setTarget(null);
        this.stopRiding();
    }

    public void dismountShoulder() {
        Player player = this.shoulderPlayer();
        if (player != null) {
            Vec3 pos = player.position().add(0.0D, 0.1D, 0.0D);
            this.setPos(pos.x, pos.y, pos.z);
        }
        this.clearShoulder();
        this.setNoGravity(false);
        this.wanderCooldown = 20 + this.random.nextInt(30);
    }

    private void clearShoulder() {
        this.entityData.set(SHOULDER_PLAYER, Optional.empty());
        this.shoulderClimbPlayer = null;
        this.shoulderClimbStart = null;
        this.shoulderClimbTicks = 0;
        this.shoulderSneakGraceTicks = 0;
        this.setClimbingTrunk(false);
    }

    private void glideOffShoulder(Player player) {
        Vec3 launch = this.shoulderRidePos(player);
        this.clearShoulder();
        this.setPos(launch.x, launch.y, launch.z);
        this.fallDistance = 0.0F;
        BlockPos landing = this.findFallGlideTarget();
        if (landing == null) {
            landing = this.findBestGlideTarget();
        }
        if (landing == null) {
            landing = this.findForcedFallLanding();
        }
        if (landing != null) {
            this.startGliding(landing, false);
            return;
        }
        this.setNoGravity(false);
        this.wanderCooldown = 20 + this.random.nextInt(30);
    }

    private Vec3 shoulderRidePos(Player player) {
        float yaw = player.getYHeadRot();
        Vec3 horizontal = Vec3.directionFromRotation(0.0F, yaw).normalize();
        Vec3 head = Vec3.directionFromRotation(player.getXRot(), yaw).normalize();
        Vec3 side = new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();
        Vec3 up = side.cross(head).normalize();
        return player.position().add(0.0D, player.getEyeHeight(), 0.0D).add(head.scale(SHOULDER_FORWARD_OFFSET)).add(up.scale(SHOULDER_HEAD_Y_OFFSET));
    }

    private Vec3 shoulderClimbMidpoint(Player player) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getYHeadRot()).normalize();
        return player.position().add(forward.scale(0.08D)).add(0.0D, player.getBbHeight() * 0.32D, 0.0D);
    }

    private void tickShoulderClimb(Player player) {
        if (this.shoulderClimbStart == null) {
            this.shoulderClimbStart = this.position();
        }
        this.cancelTravel();
        this.setTarget(null);
        this.setClimbingTrunk(true);
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0F;
        int remaining = this.shoulderClimbTicks - 1;
        double progress = 1.0D - (double) Math.max(remaining, 0) / SHOULDER_CLIMB_TICKS;
        Vec3 middle = this.shoulderClimbMidpoint(player);
        Vec3 target = this.shoulderRidePos(player);
        Vec3 next = progress < 0.65D ? this.shoulderClimbStart.lerp(middle, progress / 0.65D) : middle.lerp(target, (progress - 0.65D) / 0.35D);
        this.setPos(next.x, next.y, next.z);
        float yaw = player.getYHeadRot() + 180.0F;
        this.setYRot(yaw);
        this.setXRot(player.getXRot());
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.shoulderClimbTicks = remaining;
        if (this.shoulderClimbTicks <= 0) {
            this.shoulderClimbTicks = 0;
            this.shoulderClimbStart = null;
            this.shoulderClimbPlayer = null;
            this.shoulderSneakGraceTicks = SHOULDER_SNEAK_GRACE_TICKS;
            this.setClimbingTrunk(false);
            this.entityData.set(SHOULDER_PLAYER, Optional.of(player.getUUID()));
        }
    }

    private void followShoulder(Player player) {
        Vec3 pos = this.shoulderRidePos(player);
        this.setPos(pos.x, pos.y, pos.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setYRot(player.getYHeadRot());
        this.setXRot(player.getXRot());
        this.yBodyRot = player.getYHeadRot();
        this.yHeadRot = player.getYHeadRot();
        this.fallDistance = 0.0F;
    }

    public void startGliding(BlockPos target) {
        this.startGliding(target, false);
    }

    void startGliding(BlockPos target, boolean emergency) {
        this.glideLaunch = this.blockPosition().immutable();
        this.glideTarget = target.immutable();
        this.emergencyGlide = emergency;
        this.glideTicks = 0;
        this.glideSoundCooldown = 0;
        this.entityData.set(GLIDING, true);
        this.setClimbingTrunk(false);
        this.stopQuirkAnimation();
        this.cancelTravel();
        this.setNoGravity(true);
        Vec3 launch = Vec3.atBottomCenterOf(target).subtract(this.position());
        Vec3 horizontal = new Vec3(launch.x, 0.0D, launch.z);
        if (horizontal.lengthSqr() > 1.0E-4D) {
            this.glideDirection = horizontal.normalize();
        } else {
            float yaw = this.getYRot() * Mth.DEG_TO_RAD;
            this.glideDirection = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw)).normalize();
        }
        this.setDeltaMovement(this.glideDirection.x * 0.38D, -0.06D, this.glideDirection.z * 0.38D);
        float yaw = (float) (Mth.atan2(this.glideDirection.z, this.glideDirection.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.hasImpulse = true;
    }

    private void stopGliding() {
        if (this.isGliding() && this.glideTarget != null) {
            if (this.glideLaunch != null) {
                this.recentGlideLaunch = this.glideLaunch;
                this.recentGlideTarget = this.glideTarget;
                this.recentGlideCooldown = RECENT_GLIDE_REPEAT_COOLDOWN;
            }
            if (this.emergencyGlide) {
                this.recentEmergencyGlideTarget = this.glideTarget;
                this.recentEmergencyGlideCooldown = EMERGENCY_GLIDE_REPEAT_COOLDOWN;
            }
        }
        this.entityData.set(GLIDING, false);
        this.glideTarget = null;
        this.glideDirection = null;
        this.glideLaunch = null;
        this.emergencyGlide = false;
        this.glideTicks = 0;
        this.glideSoundCooldown = 0;
        this.glideRecoveryTicks = 16;
    }

    private void tickGliding() {
        if (this.glideSoundCooldown-- <= 0) {
            this.playSound(ModSounds.SWARM_SQUIRREL_GLIDE_LOOP.get(), 0.2F, 0.92F + this.random.nextFloat() * 0.1F);
            this.glideSoundCooldown = 8 + this.random.nextInt(6);
        }
        if (this.glideTarget == null || this.glideDirection == null || this.isInWaterOrBubble()
                || this.onGround() && this.glideTicks > 1 || ++this.glideTicks > MAX_GLIDE_TICKS) {
            this.stopGliding();
            return;
        }
        Vec3 toTarget = Vec3.atBottomCenterOf(this.glideTarget).add(0.0D, 0.08D, 0.0D).subtract(this.position());
        double distance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        if (distance < 0.85D && Math.abs(toTarget.y) <= 1.0D) {
            this.stopGliding();
            return;
        }
        Vec3 flat = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (flat.lengthSqr() > 1.0E-4D) {
            this.glideDirection = this.glideDirection.scale(0.92D).add(flat.normalize().scale(0.08D)).normalize();
        }
        double horizontalSpeed = Mth.clamp(distance * 0.05D, 0.30D, 0.48D);
        double verticalSpeed = Mth.clamp(toTarget.y * 0.04D, -0.20D, -0.02D);
        this.setDeltaMovement(this.glideDirection.x * horizontalSpeed, verticalSpeed, this.glideDirection.z * horizontalSpeed);
        this.hasImpulse = true;
        float yaw = (float) (Mth.atan2(this.glideDirection.z, this.glideDirection.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 4.0F));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private void tickSquirrel(double horizontalSpeed) {
        if (this.level().isClientSide) {
            this.syncQuirkAnimationTrigger();
            Player player = this.isOnShoulder() ? this.shoulderPlayer() : null;
            if (player != null) {
                this.followShoulder(player);
            }
            return;
        }
        this.tickPickupAnimation();
        this.tickQuirkAnimation();
        this.syncQuirkAnimationTrigger();
        if (this.hasActiveQuirkAnimation()) {
            this.cancelTravel();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        }
        if (this.isClimbingToShoulder()) {
            Player player = this.level().getPlayerByUUID(this.shoulderClimbPlayer);
            if (player == null || !player.isAlive() || player.isSpectator()) {
                this.dismountShoulder();
            } else {
                this.tickShoulderClimb(player);
            }
            this.setNoGravity(true);
            return;
        }
        if (this.isOnShoulder()) {
            Player player = this.shoulderPlayer();
            if (player == null || !player.isAlive() || player.isSpectator()) {
                this.dismountShoulder();
            } else if (this.shoulderSneakGraceTicks <= 0 && player.isShiftKeyDown()) {
                this.glideOffShoulder(player);
            } else {
                this.followShoulder(player);
                this.setNoGravity(true);
                return;
            }
        }
        if (!this.isGliding() && !this.onGround() && !this.isInWaterOrBubble() && !this.isClimbingTrunk() && this.pileTarget == null
                && this.glideRecoveryTicks <= 0 && this.fallDistance > FALL_GLIDE_TRIGGER_DISTANCE
                && this.getDeltaMovement().y < FALL_GLIDE_TRIGGER_SPEED) {
            this.tryEmergencyGlide();
        }
        if (this.isGliding()) {
            this.tickGliding();
            this.fallDistance = 0.0F;
        } else {
            this.glideTicks = 0;
            this.glideTarget = null;
            if (this.isClimbingTrunk()) {
                this.fallDistance = 0.0F;
            }
        }
        this.tickIdleStall(horizontalSpeed);
        if (this.pileTarget == null) {
            this.setNoGravity(this.isGliding() || this.isClimbingTrunk());
        }
    }

    private void tryEmergencyGlide() {
        if (this.tickCount >= this.nextFallGlideScanTick) {
            this.nextFallGlideScanTick = this.tickCount + 10;
            this.cachedFallGlideTarget = this.findFallGlideTarget();
        }
        BlockPos rescue = this.cachedFallGlideTarget;
        if (rescue == null) {
            if (this.tickCount >= this.nextBestGlideScanTick) {
                this.nextBestGlideScanTick = this.tickCount + 10;
                this.cachedBestGlideTarget = this.findBestGlideTarget();
            }
            rescue = this.cachedBestGlideTarget;
        }
        if (rescue == null) {
            rescue = this.findForcedFallLanding();
        }
        if (rescue != null && this.isRecentEmergencyGlideTarget(rescue)) {
            rescue = null;
        }
        if (rescue != null) {
            this.cachedFallGlideTarget = null;
            this.cachedBestGlideTarget = null;
            this.startGliding(rescue, true);
        }
    }

    private void tickIdleStall(double horizontalSpeed) {
        if (!this.idle() || this.isClimbingTrunk()) {
            this.idleStallTicks = 0;
            return;
        }
        boolean still = horizontalSpeed < 1.0E-4D && Math.abs(this.getDeltaMovement().y) < 0.02D;
        if (!this.onGround() || !still || !this.getNavigation().isDone()) {
            this.idleStallTicks = 0;
            return;
        }
        if (++this.idleStallTicks == GROUND_STALL_FORCE_WANDER_TICKS) {
            this.forceGroundStallWander();
        }
    }

    private void forceGroundStallWander() {
        BlockPos origin = this.blockPosition();
        for (int attempt = 0; attempt < 16; attempt++) {
            int dx = this.random.nextIntBetweenInclusive(-8, 8);
            int dz = this.random.nextIntBetweenInclusive(-8, 8);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos candidate = origin.offset(dx, dy, dz);
                if (isOpenStandPos(this.level(), candidate) && this.ownerNearby(Vec3.atBottomCenterOf(candidate), IDLE_OWNER_RANGE)) {
                    this.wanderCooldown = 0;
                    this.requestTravelTo(candidate, 1.0D);
                    this.idleStallTicks = 0;
                    return;
                }
            }
        }
        this.idleStallTicks = GROUND_STALL_FORCE_WANDER_TICKS - 40;
    }

    private boolean requestTravelTo(BlockPos target, double speed) {
        if (this.getNavigation() instanceof SwarmSquirrelNavigation navigation) {
            return navigation.requestTravelTo(target, speed);
        }
        return this.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
    }

    private void cancelTravel() {
        if (this.getNavigation() instanceof SwarmSquirrelNavigation navigation) {
            navigation.cancelTraversal();
            return;
        }
        this.getNavigation().stop();
    }

    private boolean isRecentEmergencyGlideTarget(BlockPos target) {
        return this.recentEmergencyGlideTarget != null && this.recentEmergencyGlideCooldown > 0 && this.recentEmergencyGlideTarget.closerThan(target, 4.0D);
    }

    boolean isRecentGlidePath(Vec3 from, BlockPos target) {
        if (this.recentGlideCooldown <= 0 || this.recentGlideLaunch == null || this.recentGlideTarget == null) {
            return false;
        }
        return this.recentGlideLaunch.closerThan(BlockPos.containing(from), 4.0D) && this.recentGlideTarget.closerThan(target, 4.0D);
    }

    boolean isAtStandPos(BlockPos standPos) {
        return Vec3.atBottomCenterOf(standPos).distanceToSqr(this.position()) <= 1.45D;
    }

    double scoreGlideTargetFrom(Vec3 from, BlockPos standPos) {
        Vec3 to = Vec3.atBottomCenterOf(standPos);
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double drop = from.y - to.y;
        if (horizontal < MIN_GLIDE_DISTANCE || horizontal > MAX_GLIDE_DISTANCE || drop <= MIN_GLIDE_DISTANCE || drop > 32.0D) {
            return Double.NEGATIVE_INFINITY;
        }
        return horizontal * 1.5D + drop * 2.0D;
    }

    private Vec3 preferredGlideDirection() {
        Vec3 motion = this.getDeltaMovement();
        Vec3 flat = new Vec3(motion.x, 0.0D, motion.z);
        if (flat.lengthSqr() > 1.0E-4D) {
            return flat.normalize();
        }
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw)).normalize();
    }

    @Nullable
    private BlockPos findFallGlideTarget() {
        BlockPos origin = this.blockPosition();
        Vec3 preferred = this.preferredGlideDirection();
        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                for (int dy = -28; dy <= -2; dy++) {
                    BlockPos support = origin.offset(dx, dy, dz);
                    BlockState supportState = this.level().getBlockState(support);
                    if (!isStandableSurface(supportState)) {
                        continue;
                    }
                    BlockPos stand = support.above();
                    if (!isEmergencyLandingPos(this.level(), stand) || this.isRecentGlidePath(this.position(), stand)) {
                        continue;
                    }
                    Vec3 center = Vec3.atBottomCenterOf(stand);
                    double offX = center.x - this.getX();
                    double offZ = center.z - this.getZ();
                    double horizontal = Math.sqrt(offX * offX + offZ * offZ);
                    if (horizontal < 3.0D || horizontal > 28.0D) {
                        continue;
                    }
                    double score = horizontal * 1.6D + Math.min(origin.getY() - stand.getY(), 18.0D) * 0.8D;
                    score += preferred.dot(new Vec3(offX, 0.0D, offZ).normalize()) * 10.0D;
                    if (isAdjacentToLog(this.level(), stand)) {
                        score += 4.0D;
                    }
                    if (supportState.is(BlockTags.LEAVES)) {
                        score += 6.0D;
                    } else if (supportState.blocksMotion()) {
                        score += 2.0D;
                    }
                    if (score > bestScore) {
                        bestScore = score;
                        best = stand.immutable();
                    }
                }
            }
        }
        return best != null ? best : this.findFallbackFallLanding(preferred);
    }

    @Nullable
    private BlockPos findBestGlideTarget() {
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int dx = -24; dx <= 24; dx++) {
            for (int dz = -24; dz <= 24; dz++) {
                for (int dy = -32; dy <= -1; dy++) {
                    BlockPos support = origin.offset(dx, dy, dz);
                    if (!isStandableSurface(this.level().getBlockState(support))) {
                        continue;
                    }
                    BlockPos stand = support.above();
                    if (!isOpenStandPos(this.level(), stand) || this.isRecentGlidePath(this.position(), stand)) {
                        continue;
                    }
                    double score = this.scoreGlideTargetFrom(this.position(), stand);
                    if (score > bestScore) {
                        bestScore = score;
                        best = stand.immutable();
                    }
                }
            }
        }
        return best;
    }

    @Nullable
    private BlockPos findFallbackFallLanding(Vec3 preferred) {
        BlockPos origin = this.blockPosition();
        int minY = Math.max(this.level().getMinBuildHeight() + 1, origin.getY() - 40);
        for (int step = 4; step <= 18; step += 2) {
            int x = Mth.floor(this.getX() + preferred.x * step);
            int z = Mth.floor(this.getZ() + preferred.z * step);
            BlockPos landing = this.findLandingInColumn(x, z, origin.getY() - 2, minY);
            if (landing != null) {
                return landing;
            }
        }
        return null;
    }

    @Nullable
    private BlockPos findForcedFallLanding() {
        Vec3 preferred = this.preferredGlideDirection();
        Vec3 side = new Vec3(-preferred.z, 0.0D, preferred.x);
        BlockPos origin = this.blockPosition();
        int minY = Math.max(this.level().getMinBuildHeight() + 1, origin.getY() - 96);
        for (int forward = 4; forward <= 20; forward += 2) {
            double sampleX = this.getX() + preferred.x * forward;
            double sampleZ = this.getZ() + preferred.z * forward;
            BlockPos landing = this.usableLanding(Mth.floor(sampleX), Mth.floor(sampleZ), origin.getY() - 1, minY);
            if (landing != null) {
                return landing;
            }
            for (int lateral = 2; lateral <= 4; lateral += 2) {
                landing = this.usableLanding(Mth.floor(sampleX + side.x * lateral), Mth.floor(sampleZ + side.z * lateral), origin.getY() - 1, minY);
                if (landing != null) {
                    return landing;
                }
                landing = this.usableLanding(Mth.floor(sampleX - side.x * lateral), Mth.floor(sampleZ - side.z * lateral), origin.getY() - 1, minY);
                if (landing != null) {
                    return landing;
                }
            }
        }
        return this.usableLanding(origin.getX(), origin.getZ(), origin.getY() - 1, minY);
    }

    @Nullable
    private BlockPos usableLanding(int x, int z, int startY, int minY) {
        BlockPos landing = this.findLandingInColumn(x, z, startY, minY);
        return landing != null && !this.isRecentGlidePath(this.position(), landing) ? landing : null;
    }

    @Nullable
    private BlockPos findLandingInColumn(int x, int z, int startY, int minY) {
        if (!this.level().hasChunkAt(new BlockPos(x, startY, z))) {
            return null;
        }
        for (int y = startY; y >= minY; y--) {
            BlockPos support = new BlockPos(x, y, z);
            if (isStandableSurface(this.level().getBlockState(support)) && isEmergencyLandingPos(this.level(), support.above())) {
                return support.above().immutable();
            }
        }
        return null;
    }

    static boolean hasNearbyCanopy(BlockGetter level, BlockPos pos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (isCanopySupport(level.getBlockState(pos.offset(dx, dy, dz)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isCanopySupport(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS);
    }

    static boolean isStandableSurface(BlockState state) {
        return state.blocksMotion() || isCanopySupport(state);
    }

    private static boolean isClimbPassable(BlockState state) {
        return state.isAir() || state.is(BlockTags.LEAVES);
    }

    static boolean isOpenStandPos(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir() && isStandableSurface(level.getBlockState(pos.below()));
    }

    private static boolean isEmergencyLandingPos(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && isStandableSurface(level.getBlockState(pos.below()));
    }

    static boolean isOpenAir(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    private static boolean isOpenClimbPos(BlockGetter level, BlockPos pos) {
        return isClimbPassable(level.getBlockState(pos)) && isClimbPassable(level.getBlockState(pos.above()));
    }

    static boolean isAdjacentToLog(BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).is(BlockTags.LOGS) || level.getBlockState(pos.above().relative(direction)).is(BlockTags.LOGS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGroundEscapeSurface(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MOSS_CARPET);
    }

    private boolean shouldFavorClimbingFromGround() {
        if (!this.onGround() || this.getY() >= 200.0D || !isGroundEscapeSurface(this.level().getBlockState(this.blockPosition().below()))) {
            return false;
        }
        return Math.min(1.0D, Mth.clampedMap(this.getY(), 80.0D, 180.0D, 1.0D, 0.0D) + 0.20D) > 0.08D;
    }

    private void rememberFailedClimbTarget(BlockPos top) {
        this.recentFailedClimbTarget = top.immutable();
        this.recentFailedClimbCooldown = FAILED_CLIMB_TARGET_COOLDOWN;
    }

    @Nullable
    private ClimbTarget findBestClimbTarget() {
        BlockPos origin = this.blockPosition();
        boolean favorClimbing = this.shouldFavorClimbingFromGround();
        int range = favorClimbing ? 12 : 8;
        int currentY = Mth.floor(this.getY());
        int minimumY = currentY + (favorClimbing ? GROUND_ESCAPE_MIN_CLIMB_HEIGHT : MIN_CLIMB_HEIGHT);
        int maximumY = currentY + MAX_CLIMB_HEIGHT;
        ClimbTarget best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (BlockPos base : BlockPos.betweenClosed(origin.offset(-range, -1, -range), origin.offset(range, 1, range))) {
            if (!isOpenStandPos(this.level(), base) || !this.ownerNearby(Vec3.atBottomCenterOf(base), IDLE_OWNER_RANGE)) {
                continue;
            }
            Direction attach = this.findClimbAttachmentDirection(base, null);
            if (attach == null || !this.isValidClimbColumnPos(base, attach)) {
                continue;
            }
            int reachableTop = this.findHighestReachableClimbY(base, maximumY, attach);
            if (reachableTop < minimumY) {
                continue;
            }
            BlockPos top = this.findBestClimbPerch(base, minimumY, reachableTop + 2, attach);
            if (top == null || this.recentFailedClimbTarget != null && this.recentFailedClimbCooldown > 0 && this.recentFailedClimbTarget.closerThan(top, 4.0D)) {
                continue;
            }
            int height = top.getY() - currentY;
            if (height < MIN_CLIMB_HEIGHT || height > MAX_CLIMB_HEIGHT) {
                continue;
            }
            double walk = Vec3.atBottomCenterOf(base).distanceTo(this.position());
            double perchOffset = Vec3.atBottomCenterOf(top).distanceTo(Vec3.atBottomCenterOf(base));
            double score = height * (favorClimbing ? 10.0D : 4.5D) - walk - perchOffset * 0.6D + (favorClimbing ? 20.0D : 0.0D);
            if (score > bestScore) {
                bestScore = score;
                best = new ClimbTarget(base.immutable(), top.immutable(), attach);
            }
        }
        return best;
    }

    private int findHighestReachableClimbY(BlockPos base, int maxY, Direction preferred) {
        int highest = base.getY();
        for (int y = base.getY(); y <= maxY; y++) {
            if (!this.isValidClimbColumnPos(new BlockPos(base.getX(), y, base.getZ()), preferred)) {
                break;
            }
            highest = y;
        }
        return highest;
    }

    private boolean isValidClimbColumnPos(BlockPos pos, @Nullable Direction preferred) {
        return isOpenClimbPos(this.level(), pos) && this.findClimbAttachmentDirection(pos, preferred) != null;
    }

    private Vec3 getClimbAttachPoint(BlockPos column, @Nullable Direction preferred) {
        Direction attach = this.findClimbAttachmentDirection(column, preferred);
        double x = column.getX() + 0.5D;
        double z = column.getZ() + 0.5D;
        if (attach != null) {
            double offset = Mth.clamp(0.5D - this.getBbWidth() * 0.5D + 0.04D, 0.16D, 0.30D);
            x += attach.getStepX() * offset;
            z += attach.getStepZ() * offset;
        }
        return new Vec3(x, column.getY(), z);
    }

    @Nullable
    private Direction findClimbAttachmentDirection(BlockPos pos, @Nullable Direction preferred) {
        if (preferred != null && preferred.getAxis().isHorizontal()) {
            BlockState state = this.level().getBlockState(pos.relative(preferred));
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.blocksMotion()) {
                return preferred;
            }
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (this.level().getBlockState(pos.relative(direction)).is(BlockTags.LOGS)) {
                return direction;
            }
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (this.level().getBlockState(pos.relative(direction)).blocksMotion()) {
                return direction;
            }
        }
        return null;
    }

    @Nullable
    private BlockPos findBestClimbPerch(BlockPos base, int minimumY, int maximumY, Direction preferred) {
        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int y = maximumY; y >= minimumY; y--) {
            BlockPos column = new BlockPos(base.getX(), y, base.getZ());
            if (!this.isValidClimbColumnPos(column, preferred)) {
                continue;
            }
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > 3) {
                        continue;
                    }
                    BlockPos stand = new BlockPos(base.getX() + dx, y, base.getZ() + dz);
                    if (!isOpenStandPos(this.level(), stand)
                            || this.findClimbAttachmentDirection(stand, null) == null && !isStandableSurface(this.level().getBlockState(stand.above()))) {
                        continue;
                    }
                    double score = y * 4.0D - Math.sqrt(dx * dx + dz * dz) * 1.5D + (isAdjacentToLog(this.level(), stand) ? 2.0D : 0.0D);
                    if (score > bestScore) {
                        bestScore = score;
                        best = stand.immutable();
                    }
                }
            }
        }
        return best;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SWARM_SQUIRREL_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SWARM_SQUIRREL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SWARM_SQUIRREL_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.35F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(state.getSoundType().getStepSound(), 0.12F, 1.15F);
    }

    private record ClimbTarget(BlockPos base, BlockPos top, Direction attach) {
    }

    private final class HoldGoal extends Goal {
        private HoldGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return SwarmSquirrel.this.pileTarget != null || SwarmSquirrel.this.isClimbingToShoulder() || SwarmSquirrel.this.isOnShoulder()
                    || SwarmSquirrel.this.pickupTicks > 0 || SwarmSquirrel.this.hasActiveQuirkAnimation();
        }

        @Override
        public void start() {
            SwarmSquirrel.this.cancelTravel();
        }
    }

    private final class GuardGoal extends Goal {
        private GuardGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            BlockPos pos = SwarmSquirrel.this.guardPos;
            return pos != null && SwarmSquirrel.this.free() && SwarmSquirrel.this.getTarget() == null
                    && SwarmSquirrel.this.distanceToSqr(Vec3.atBottomCenterOf(pos)) > 9.0D;
        }

        @Override
        public void start() {
            BlockPos pos = SwarmSquirrel.this.guardPos;
            SwarmSquirrel.this.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 1.3D);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() && !SwarmSquirrel.this.getNavigation().isDone();
        }
    }

    private final class ForageGoal extends Goal {
        private int repath;

        private ForageGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!SwarmSquirrel.this.free() || SwarmSquirrel.this.tippy || SwarmSquirrel.this.getTarget() != null
                    || !SquirrelGirlPower.FORAGE.equals(SwarmSquirrel.this.order())) {
                return false;
            }
            if (!SwarmSquirrel.this.carried.isEmpty()) {
                return true;
            }
            SwarmSquirrel.this.forageTarget = this.find();
            return SwarmSquirrel.this.forageTarget != null;
        }

        @Override
        public boolean canContinueToUse() {
            return SwarmSquirrel.this.free() && SwarmSquirrel.this.getTarget() == null
                    && SquirrelGirlPower.FORAGE.equals(SwarmSquirrel.this.order())
                    && (!SwarmSquirrel.this.carried.isEmpty() || SwarmSquirrel.this.forageTarget != null && SwarmSquirrel.this.forageTarget.isAlive());
        }

        @Override
        public void stop() {
            SwarmSquirrel.this.forageTarget = null;
        }

        @Override
        public void tick() {
            if (SwarmSquirrel.this.pickupTicks > 0 || !(SwarmSquirrel.this.getOwner() instanceof Player owner)) {
                return;
            }
            if (!SwarmSquirrel.this.carried.isEmpty()) {
                if (SwarmSquirrel.this.distanceToSqr(owner) < 4.0D) {
                    ItemHandlerHelper.giveItemToPlayer(owner, SwarmSquirrel.this.carried);
                    SwarmSquirrel.this.carried = ItemStack.EMPTY;
                    SwarmSquirrel.this.playSound(ModSounds.SWARM_SQUIRREL_NUT.get(), 0.35F, 0.95F + SwarmSquirrel.this.random.nextFloat() * 0.2F);
                } else if (--this.repath <= 0) {
                    this.repath = 10;
                    SwarmSquirrel.this.getNavigation().moveTo(owner, 1.4D);
                }
                return;
            }
            ItemEntity item = SwarmSquirrel.this.forageTarget;
            if (item == null || !item.isAlive()) {
                return;
            }
            SwarmSquirrel.this.getLookControl().setLookAt(item, 30.0F, 30.0F);
            if (SwarmSquirrel.this.distanceToSqr(item) <= 2.1D) {
                SwarmSquirrel.this.carried = item.getItem().copy();
                item.discard();
                SwarmSquirrel.this.forageTarget = null;
                SwarmSquirrel.this.startPickupAnimation();
                SwarmSquirrel.this.playSound(ModSounds.SWARM_SQUIRREL_NUT.get(), 0.35F, 0.95F + SwarmSquirrel.this.random.nextFloat() * 0.2F);
            } else if (--this.repath <= 0) {
                this.repath = 10;
                SwarmSquirrel.this.requestTravelTo(item.blockPosition(), 1.15D);
            }
        }

        private ItemEntity find() {
            if (!(SwarmSquirrel.this.getOwner() instanceof Player owner)) {
                return null;
            }
            double range = MarvelBalanceConfig.SWARM_FORAGE_RANGE.get();
            ItemEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (ItemEntity item : SwarmSquirrel.this.level().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(range),
                    entity -> entity.isAlive() && !entity.hasPickUpDelay())) {
                if (SwarmSquirrel.this.level().getEntitiesOfClass(SwarmSquirrel.class, item.getBoundingBox().inflate(range),
                        other -> other != SwarmSquirrel.this && other.forageTarget == item).isEmpty()) {
                    double distance = SwarmSquirrel.this.distanceToSqr(item);
                    if (distance < bestDistance) {
                        best = item;
                        bestDistance = distance;
                    }
                }
            }
            return best;
        }
    }

    private final class GnawGoal extends Goal {
        private int repath;

        private GnawGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private BlockPos block() {
            return SwarmSquirrel.this.getOwner() instanceof Player owner ? SquirrelGirlPower.gnawTarget(owner) : null;
        }

        @Override
        public boolean canUse() {
            return SwarmSquirrel.this.free() && !SwarmSquirrel.this.tippy && SwarmSquirrel.this.getTarget() == null
                    && SquirrelGirlPower.GNAW.equals(SwarmSquirrel.this.order()) && this.block() != null;
        }

        @Override
        public void tick() {
            BlockPos pos = this.block();
            if (pos == null) {
                return;
            }
            Vec3 center = Vec3.atCenterOf(pos);
            SwarmSquirrel.this.getLookControl().setLookAt(center.x, center.y, center.z);
            if (SwarmSquirrel.this.distanceToSqr(center) < SquirrelGirlPower.GNAW_REACH * SquirrelGirlPower.GNAW_REACH) {
                SwarmSquirrel.this.cancelTravel();
            } else if (--this.repath <= 0) {
                this.repath = 10;
                SwarmSquirrel.this.getNavigation().moveTo(center.x, center.y, center.z, 1.3D);
            }
        }
    }

    private final class ArborealRandomStrollGoal extends WaterAvoidingRandomStrollGoal {
        private ArborealRandomStrollGoal() {
            super(SwarmSquirrel.this, 1.0D, 80);
        }

        @Override
        public boolean canUse() {
            SwarmSquirrel squirrel = SwarmSquirrel.this;
            if (!squirrel.idle() || squirrel.isClimbingTrunk() || squirrel.wanderCooldown > 0) {
                return false;
            }
            if (!squirrel.onGround() && !squirrel.isAtStandPos(squirrel.blockPosition()) && !hasNearbyCanopy(squirrel.level(), squirrel.blockPosition())) {
                return false;
            }
            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return SwarmSquirrel.this.idle() && super.canContinueToUse();
        }

        @Override
        @Nullable
        protected Vec3 getPosition() {
            BlockPos target = this.findBestArborealDestination();
            if (target != null) {
                return Vec3.atBottomCenterOf(target);
            }
            Vec3 fallback = super.getPosition();
            return fallback != null && SwarmSquirrel.this.ownerNearby(fallback, IDLE_OWNER_RANGE) ? fallback : null;
        }

        @Override
        public void start() {
            SwarmSquirrel.this.requestTravelTo(BlockPos.containing(this.wantedX, this.wantedY, this.wantedZ), this.speedModifier);
        }

        @Override
        public void stop() {
            super.stop();
            if (!SwarmSquirrel.this.isGliding()) {
                SwarmSquirrel.this.wanderCooldown = 25 + SwarmSquirrel.this.random.nextInt(35);
            }
        }

        @Nullable
        private BlockPos findBestArborealDestination() {
            SwarmSquirrel squirrel = SwarmSquirrel.this;
            BlockPos origin = squirrel.blockPosition();
            double heightBias = this.getHeightBias(origin);
            int verticalDown = Mth.floor(Mth.lerp(heightBias, 8.0D, 4.0D));
            int verticalUp = Mth.floor(Mth.lerp(heightBias, 16.0D, 20.0D));
            int range = (int) IDLE_OWNER_RANGE;
            BlockPos best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-range, -verticalDown, -range), origin.offset(range, verticalUp, range))) {
                if (!isOpenStandPos(squirrel.level(), candidate) || squirrel.isAtStandPos(candidate)
                        || !squirrel.ownerNearby(Vec3.atBottomCenterOf(candidate), IDLE_OWNER_RANGE)) {
                    continue;
                }
                boolean nearCanopy = hasNearbyCanopy(squirrel.level(), candidate);
                boolean nearLog = isAdjacentToLog(squirrel.level(), candidate);
                if (!nearCanopy && !nearLog) {
                    continue;
                }
                double dx = candidate.getX() + 0.5D - squirrel.getX();
                double dy = candidate.getY() - squirrel.getY();
                double dz = candidate.getZ() + 0.5D - squirrel.getZ();
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                if (horizontal < 4.0D || heightBias > 0.15D && dy < Mth.lerp(heightBias, 0.0D, 8.0D)) {
                    continue;
                }
                double score = (nearCanopy ? 10.0D : 0.0D) + (nearLog ? 8.0D : 0.0D);
                score += Math.max(dy, 0.0D) * Mth.lerp(heightBias, 3.0D, 5.5D);
                score += Math.min(horizontal, 24.0D) * Mth.lerp(heightBias, 1.1D, 0.9D);
                if (dy <= -4.0D && horizontal >= 10.0D) {
                    score += 18.0D * (1.0D - heightBias);
                }
                if (dy >= 12.0D) {
                    score += Mth.lerp(heightBias, 10.0D, 18.0D);
                }
                if (dy < -2.0D) {
                    score += dy * Mth.lerp(heightBias, 0.75D, 0.15D);
                }
                score += squirrel.random.nextDouble() * 6.0D;
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate.immutable();
                }
            }
            return best;
        }

        private double getHeightBias(BlockPos origin) {
            double bias = origin.getY() >= 190 ? 0.0D : Mth.clampedMap(origin.getY(), 80.0D, 170.0D, 1.0D, 0.0D);
            if (isGroundEscapeSurface(SwarmSquirrel.this.level().getBlockState(origin.below()))) {
                bias = Math.min(1.0D, bias + 0.15D);
            }
            return bias;
        }
    }

    private final class ClimbGoal extends Goal {
        @Nullable
        private ClimbTarget target;
        @Nullable
        private Direction attach;
        private int repathDelay;
        private int climbTicks;
        private int approachTicks;
        private int searchCooldown;
        private int noProgressTicks;
        private double lastDistanceToBase;
        private boolean climbing;
        private int attachRefreshTick;

        private ClimbGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            SwarmSquirrel squirrel = SwarmSquirrel.this;
            if (this.searchCooldown > 0) {
                this.searchCooldown--;
                return false;
            }
            boolean favorClimbing = squirrel.shouldFavorClimbingFromGround();
            if (!squirrel.idle() || !squirrel.onGround() || !favorClimbing && squirrel.wanderCooldown > 30) {
                return false;
            }
            this.target = squirrel.findBestClimbTarget();
            if (this.target != null) {
                return true;
            }
            this.searchCooldown = favorClimbing ? 10 : 20;
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            SwarmSquirrel squirrel = SwarmSquirrel.this;
            if (this.target == null || squirrel.isGliding() || !squirrel.free() || squirrel.guardPos != null || squirrel.getTarget() != null) {
                return false;
            }
            return this.climbing ? this.climbTicks < 200 : !squirrel.isAtStandPos(this.target.top());
        }

        @Override
        public void start() {
            this.repathDelay = 0;
            this.climbTicks = 0;
            this.approachTicks = 0;
            this.searchCooldown = 0;
            this.noProgressTicks = 0;
            this.lastDistanceToBase = Double.MAX_VALUE;
            this.climbing = false;
            this.attach = null;
            this.attachRefreshTick = 0;
        }

        @Override
        public void stop() {
            if (this.climbing && !SwarmSquirrel.this.isClimbingToShoulder()) {
                SwarmSquirrel.this.setClimbingTrunk(false);
                SwarmSquirrel.this.setNoGravity(false);
            }
            this.target = null;
            this.climbing = false;
            this.attach = null;
            this.searchCooldown = 10;
            if (!SwarmSquirrel.this.isGliding()) {
                SwarmSquirrel.this.getNavigation().stop();
            }
        }

        @Override
        public void tick() {
            SwarmSquirrel squirrel = SwarmSquirrel.this;
            ClimbTarget target = this.target;
            if (target == null) {
                return;
            }
            if (!this.climbing) {
                squirrel.getLookControl().setLookAt(target.top().getX() + 0.5D, target.top().getY(), target.top().getZ() + 0.5D, 20.0F, 20.0F);
                this.approachTicks++;
                Vec3 base = Vec3.atBottomCenterOf(target.base());
                double distance = base.distanceTo(squirrel.position());
                if (squirrel.isAtStandPos(target.base()) || squirrel.position().distanceToSqr(base) <= CLIMB_START_DISTANCE_SQR) {
                    this.climbing = true;
                    this.climbTicks = 0;
                    this.attach = target.attach();
                    squirrel.setClimbingTrunk(true);
                    return;
                }
                if (distance + 0.15D < this.lastDistanceToBase) {
                    this.noProgressTicks = 0;
                    this.lastDistanceToBase = distance;
                } else if (++this.noProgressTicks > CLIMB_NO_PROGRESS_LIMIT || this.approachTicks > CLIMB_APPROACH_TIMEOUT_TICKS) {
                    squirrel.rememberFailedClimbTarget(target.top());
                    this.target = null;
                    return;
                }
                if (--this.repathDelay <= 0) {
                    this.repathDelay = 10;
                    squirrel.requestTravelTo(target.base(), 1.0D);
                }
                return;
            }
            if (++this.climbTicks > 200) {
                this.abort(target);
                return;
            }
            squirrel.setClimbingTrunk(true);
            squirrel.setNoGravity(true);
            squirrel.fallDistance = 0.0F;
            double nextY = Math.min(squirrel.getY() + 0.24D, target.top().getY());
            BlockPos column = new BlockPos(target.base().getX(), Mth.floor(nextY), target.base().getZ());
            if (!squirrel.isValidClimbColumnPos(column, target.attach())) {
                this.abort(target);
                return;
            }
            Vec3 attachPoint = squirrel.getClimbAttachPoint(column, this.attach);
            squirrel.setPos(Mth.lerp(0.45D, squirrel.getX(), attachPoint.x), nextY, Mth.lerp(0.45D, squirrel.getZ(), attachPoint.z));
            squirrel.setDeltaMovement(Vec3.ZERO);
            squirrel.hasImpulse = true;
            if (this.climbTicks >= this.attachRefreshTick) {
                this.attachRefreshTick = this.climbTicks + 5;
                Direction refreshed = squirrel.findClimbAttachmentDirection(column, this.attach);
                if (refreshed != null) {
                    this.attach = refreshed;
                }
            }
            if (this.attach != null) {
                float yaw = this.attach.toYRot();
                squirrel.setYRot(yaw);
                squirrel.yRotO = yaw;
                squirrel.yBodyRot = yaw;
                squirrel.yBodyRotO = yaw;
                squirrel.yHeadRot = yaw;
                squirrel.yHeadRotO = yaw;
            }
            if (squirrel.getY() >= target.top().getY() - 0.15D) {
                squirrel.setPos(target.top().getX() + 0.5D, target.top().getY(), target.top().getZ() + 0.5D);
                squirrel.setDeltaMovement(Vec3.ZERO);
                squirrel.setClimbingTrunk(false);
                squirrel.setNoGravity(false);
                this.target = null;
                this.climbing = false;
                squirrel.wanderCooldown = 20 + squirrel.random.nextInt(25);
            }
        }

        private void abort(ClimbTarget target) {
            SwarmSquirrel.this.rememberFailedClimbTarget(target.top());
            SwarmSquirrel.this.setClimbingTrunk(false);
            SwarmSquirrel.this.setNoGravity(false);
            this.target = null;
            this.climbing = false;
        }
    }
}
