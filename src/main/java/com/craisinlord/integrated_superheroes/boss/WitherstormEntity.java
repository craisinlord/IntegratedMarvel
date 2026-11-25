package com.craisinlord.integrated_superheroes.boss;

import com.craisinlord.integrated_superheroes.hero.ServerState;
import java.util.Set;
import java.util.Collections;
import com.google.common.collect.MapMaker;
import com.craisinlord.anthology.multipart.MultipartEntityOwner;
import com.craisinlord.anthology.multipart.MultipartFramework;
import com.craisinlord.anthology.multipart.MultipartLayout;
import com.craisinlord.anthology.neoforge.multipart.MultipartPartEntity;
import com.craisinlord.anthology.neoforge.multipart.MultipartPartLevelRegistry;
import com.craisinlord.anthology.shake.CameraShakeSync;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.voidworld.VoidConfig;
import com.craisinlord.integrated_superheroes.voidworld.VoidStorm;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.craisinlord.integrated_superheroes.voidworld.VoidWorld;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.levelgen.Heightmap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WitherstormEntity extends Monster implements GeoEntity, MultipartEntityOwner {
    public static final int MAX_STAGE = 5;

    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(WitherstormEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CORE_OPEN = SynchedEntityData.defineId(WitherstormEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<CompoundTag> EATEN = SynchedEntityData.defineId(WitherstormEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final ResourceLocation STAGE_HEALTH = IntegratedSuperheroes.id("witherstorm_stage_health");
    private static final int EATEN_RING = 16;
    private static final BlockState[] NO_STATES = new BlockState[0];

    private static final String SMALL = "animation.the_storm_small.";
    private static final String BIG = "animation.the_storm.";
    private static final RawAnimation SMALL_FLY = RawAnimation.begin().thenLoop(SMALL + "fly");
    private static final RawAnimation SMALL_DEATH = RawAnimation.begin().thenPlayAndHold(SMALL + "death");
    private static final RawAnimation BIG_IDLE = RawAnimation.begin().thenLoop(BIG + "idle");
    private static final RawAnimation BIG_MOVE = RawAnimation.begin().thenLoop(BIG + "move");
    private static final RawAnimation BIG_CORE = RawAnimation.begin().thenLoop(BIG + "core_exposed");
    private static final RawAnimation BIG_DEATH = RawAnimation.begin().thenPlayAndHold(BIG + "death");
    private static final String[] SMALL_TRIGGERS = {"shoot", "hit", "roar", "devour", "grow"};
    private static final String[] BIG_TRIGGERS = {"shoot", "hurt", "roar", "devour", "grow", "core_hit", "spit", "attackall", "enrage", "tractor_beam"};

    private static final String SKULL_BARRAGE = "skull_barrage";
    private static final String DEVOURING_LUNGE = "devouring_lunge";
    private static final String SHOCKWAVE_ROAR = "shockwave_roar";
    private static final String[] STAGE_ONE_ATTACKS = {SKULL_BARRAGE, DEVOURING_LUNGE, SHOCKWAVE_ROAR};

    private static final float SHIELDED_SHARE = 0.1F;
    private static final float HEAD_STUN_SHARE = 0.02F;
    private static final int CORE_OPEN_TICKS = 220;
    private static final int INTRO_TICKS = 70;
    private static final int GROUNDED_TICKS = 100;
    private static final double ROAR_RADIUS = 32.0D;
    private static final double PRESENCE_RADIUS = 24.0D;
    private static final int PLACED_LIMIT = 4096;
    private static final long KILL_CREDIT_TICKS = 1200L;
    private static final List<Integer> DEFAULT_COMMAND_BLOCK_DROPS = List.of(0, 0, 1, 2, 4);
    private static final DustParticleOptions TELL_DUST = new DustParticleOptions(new Vector3f(0.55F, 0.1F, 0.75F), 2.0F);
    private static final DustParticleOptions BEAM_DUST = new DustParticleOptions(new Vector3f(0.42F, 0.12F, 0.62F), 4.0F);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent;
    private final WitherstormAttackScheduler scheduler = new WitherstormAttackScheduler();
    private final float[] headStun = new float[WitherstormLayout.HEAD_COUNT];
    private final LongOpenHashSet placedBlocks = new LongOpenHashSet();
    private static final Set<WitherstormEntity> LIVE = ServerState.all(Collections.newSetFromMap(new MapMaker().weakKeys().makeMap()));
    private final Map<UUID, Long> grounded = new HashMap<>();
    @Nullable
    private Entity[] multipartParts;
    private int coreTicks;
    private int introTicks;
    private double hunger;
    private int attackGap = 40;
    private long lastDeflect;
    @Nullable
    private Vec3 lungePoint;
    private boolean lunging;
    private boolean lungeLanded;
    private boolean roaming;
    @Nullable
    private Vec3 roamGoal;
    private double roamSpeed = 0.12D;
    private int groundY = Integer.MIN_VALUE;
    private final int[] eatenIds = new int[EATEN_RING];
    private int eatenCount;
    private int eatenNext;
    private boolean eatenDirty;
    private BlockState[] eatenStates = NO_STATES;
    @Nullable
    private WitherstormDevour devour;
    private final Map<UUID, Long> damagers = new HashMap<>();
    @Nullable
    private UUID lastDamager;
    private boolean rewarded;

    public WitherstormEntity(EntityType<? extends WitherstormEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 500;
        this.setPersistenceRequired();
        this.bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12000.0D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.6D);
    }

    public static float renderScale(int stage) {
        return switch (stage) {
            case 1 -> 3.0F;
            case 2 -> 2.4F;
            case 3 -> 4.8F;
            case 4 -> 7.0F;
            default -> 10.0F;
        };
    }

    public static double healthMultiplier(int stage) {
        return switch (stage) {
            case 2 -> 2.0D;
            case 3 -> 4.0D;
            case 4 -> 7.0D;
            case 5 -> 12.0D;
            default -> 1.0D;
        };
    }

    public float scale() {
        return renderScale(this.getStage());
    }

    public float attackScale() {
        return this.isSmallForm() ? 1.0F : Mth.clamp(this.scale() * 0.6F, 1.5F, 6.0F);
    }

    public double hoverOffset() {
        return this.isSmallForm() ? 6.0D : 6.0D * this.scale();
    }

    public double engageRange() {
        return 32.0D + 10.0D * this.scale();
    }

    private double followRange() {
        if (this.roaming) return this.engageRange();
        return this.isSmallForm() ? 80.0D : Math.min(256.0D, 80.0D * this.attackScale());
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, false, null) {
            @Override
            protected AABB getTargetSearchArea(double distance) {
                return this.mob.getBoundingBox().inflate(distance, distance, distance);
            }
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STAGE, 1);
        builder.define(CORE_OPEN, false);
        builder.define(EATEN, new CompoundTag());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STAGE.equals(key)) {
            this.noPhysics = !this.isSmallForm();
            this.refreshDimensions();
            this.rebuildParts();
        } else if (EATEN.equals(key)) {
            int[] ids = this.entityData.get(EATEN).getIntArray("B");
            List<BlockState> states = new ArrayList<>(ids.length);
            for (int id : ids) {
                BlockState state = Block.stateById(id);
                if (state.getRenderShape() == RenderShape.MODEL) states.add(state);
            }
            this.eatenStates = states.toArray(NO_STATES);
        }
    }

    public BlockState[] eatenStates() {
        return this.eatenStates;
    }

    void rememberEaten(BlockState state) {
        int id = Block.getId(state);
        for (int i = 0; i < this.eatenCount; i++) {
            if (this.eatenIds[i] == id) return;
        }
        this.eatenIds[this.eatenNext] = id;
        this.eatenNext = (this.eatenNext + 1) % EATEN_RING;
        this.eatenCount = Math.min(EATEN_RING, this.eatenCount + 1);
        this.eatenDirty = true;
    }

    private void syncEaten() {
        if (!this.eatenDirty) return;
        this.eatenDirty = false;
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("B", java.util.Arrays.copyOf(this.eatenIds, this.eatenCount));
        this.entityData.set(EATEN, tag);
    }

    public int getStage() {
        return this.entityData.get(STAGE);
    }

    public void setStage(int stage) {
        int clamped = Mth.clamp(stage, 1, MAX_STAGE);
        this.entityData.set(STAGE, clamped);
        this.noPhysics = clamped != 1;
        if (!this.level().isClientSide) this.applyStageStats(clamped);
    }

    private void applyStageStats(int stage) {
        AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            double amount = healthMultiplier(stage) - 1.0D;
            AttributeModifier current = health.getModifier(STAGE_HEALTH);
            double had = current == null ? 0.0D : current.amount();
            if (had != amount) {
                float fraction = this.getMaxHealth() > 0.0F ? this.getHealth() / this.getMaxHealth() : 1.0F;
                health.removeModifier(STAGE_HEALTH);
                if (amount != 0.0D) {
                    health.addPermanentModifier(new AttributeModifier(STAGE_HEALTH, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
                this.setHealth(fraction * this.getMaxHealth());
            }
        }
        AttributeInstance follow = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(this.followRange());
    }

    public double getHunger() {
        return this.hunger;
    }

    public void setHunger(double hunger) {
        this.hunger = Math.max(0.0D, hunger);
    }

    void feed(double amount) {
        int stage = this.getStage();
        if (stage < 2 || stage >= MAX_STAGE || !(this.level() instanceof ServerLevel level)) return;
        int perStage = com.craisinlord.integrated_superheroes.voidworld.VoidConfig.SPEC.isLoaded()
                ? com.craisinlord.integrated_superheroes.voidworld.VoidConfig.STORM_GROWTH.get() : 150000;
        if (perStage <= 0) return;
        this.hunger += amount;
        double need = (double) perStage * stage;
        if (this.hunger < need) return;
        this.hunger -= need;
        this.setStage(stage + 1);
        this.playAnim("grow");
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 12.0F, 0.5F);
    }

    public boolean isRoaming() {
        return this.roaming;
    }

    public void setRoaming(boolean roaming) {
        this.roaming = roaming;
        AttributeInstance follow = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(this.followRange());
    }

    public void setRoamGoal(@Nullable Vec3 goal, double speed) {
        this.roamGoal = goal;
        this.roamSpeed = speed;
    }

    public Vec3 mouth() {
        return this.partCenter(WitherstormLayout.HEAD_CENTER);
    }

    public int groundY() {
        if (this.groundY == Integer.MIN_VALUE) this.refreshGround();
        return this.groundY;
    }

    private void refreshGround() {
        BlockPos at = this.blockPosition();
        int top = this.level().isLoaded(at) ? this.level().getHeight(Heightmap.Types.WORLD_SURFACE, at.getX(), at.getZ()) : 0;
        this.groundY = Math.max(top, 64);
    }

    @Override
    public boolean shouldBeSaved() {
        return !this.roaming && super.shouldBeSaved();
    }

    @Override
    protected void pushEntities() {
        if (this.isSmallForm()) super.pushEntities();
    }

    @Override
    protected boolean updateInWaterStateAndDoFluidPushing() {
        if (this.isSmallForm()) return super.updateInWaterStateAndDoFluidPushing();
        this.wasTouchingWater = false;
        return false;
    }

    @Override
    public void tick() {
        this.noPhysics = !this.isSmallForm();
        super.tick();
    }

    public boolean isCoreOpen() {
        return this.entityData.get(CORE_OPEN);
    }

    public boolean isSmallForm() {
        return this.getStage() == 1;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        if (this.isSmallForm()) return super.getDefaultDimensions(pose);
        float scale = this.scale();
        return EntityDimensions.scalable(6.0F * scale, 8.0F * scale).withEyeHeight(9.0F * scale);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        if (this.isSmallForm()) return this.getBoundingBox().inflate(4.0D);
        double s = this.scale();
        return new AABB(this.getX() - 22.0D * s, this.getY() - 27.0D * s, this.getZ() - 22.0D * s,
                this.getX() + 22.0D * s, this.getY() + 18.0D * s, this.getZ() + 22.0D * s);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = this.isSmallForm() ? 256.0D : 1536.0D;
        return distance < range * range;
    }

    @Override
    public MultipartLayout anthology$getMultipartLayout() {
        return WitherstormLayout.forStage(this.getStage());
    }

    @Override
    @Nullable
    public Entity[] anthology$getMultipartParts() {
        return this.multipartParts;
    }

    @Override
    public void anthology$setMultipartParts(@Nullable Entity[] parts) {
        this.multipartParts = parts;
    }

    private void rebuildParts() {
        if (this.multipartParts == null) return;
        MultipartPartLevelRegistry.unregister(this);
        MultipartFramework.discardMultipartParts(this);
        MultipartFramework.spawnMultipartParts(this);
        MultipartPartLevelRegistry.register(this);
    }

    private Vec3 partCenter(int index) {
        Entity[] parts = this.multipartParts;
        if (parts != null && index < parts.length && parts[index] != null) {
            Entity part = parts[index];
            return part.position().add(0.0D, part.getBbHeight() * 0.5D, 0.0D);
        }
        var spec = this.anthology$getMultipartLayout().part(index);
        return this.anthology$getMultipartLayout().getPartPosition(this, spec).add(0.0D, spec.height() * 0.5D, 0.0D);
    }

    @Override
    public boolean anthology$hurtMultipartPart(Entity part, DamageSource source, float amount) {
        int index = part instanceof MultipartPartEntity piece ? piece.getPartIndex() : WitherstormLayout.TORSO;
        return this.damageRegion(index, source, amount, part.position().add(0.0D, part.getBbHeight() * 0.5D, 0.0D));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return this.damageRegion(WitherstormLayout.TORSO, source, amount, null);
    }

    private boolean rejects(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker == this) return true;
        if (attacker instanceof PartEntity<?> piece && piece.getParent() == this) return true;
        if (attacker == null && source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        return source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FLY_INTO_WALL) || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.DROWN) || source.is(DamageTypes.WITHER);
    }

    private boolean damageRegion(int region, DamageSource source, float amount, @Nullable Vec3 at) {
        if (this.level().isClientSide || this.isDeadOrDying() || this.isRemoved()) return false;
        Vec3 hit = at != null ? at : this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        if (this.introTicks > 0 || this.rejects(source)) {
            this.deflect(hit);
            return false;
        }
        boolean head = region >= WitherstormLayout.HEAD_CENTER && region < WitherstormLayout.HEAD_CENTER + WitherstormLayout.HEAD_COUNT;
        boolean exposed = region == WitherstormLayout.CORE && this.isCoreOpen();
        float dealt = exposed ? amount : amount * SHIELDED_SHARE;
        boolean landed = super.hurt(source, dealt);
        if (!landed) return false;
        this.creditDamage(source);
        if (exposed) {
            this.playAnim(this.isSmallForm() ? "hit" : "core_hit");
            this.playSound(SoundEvents.WITHER_HURT, 6.0F, 0.5F);
        } else {
            this.deflect(hit);
        }
        if (head) this.stunHead(region - WitherstormLayout.HEAD_CENTER, amount);
        return true;
    }

    private void deflect(Vec3 at) {
        if (!(this.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (now - this.lastDeflect < 4) return;
        this.lastDeflect = now;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 14, 0.4D, 0.4D, 0.4D, 0.5D);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.3D, 0.3D, 0.3D, 0.3D);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.5F, 1.8F);
    }

    private float stunThreshold() {
        return this.getMaxHealth() * HEAD_STUN_SHARE;
    }

    private boolean headStunned(int head) {
        return this.headStun[head] >= this.stunThreshold();
    }

    private void stunHead(int head, float amount) {
        if (this.isCoreOpen() || this.headStunned(head)) return;
        float threshold = this.stunThreshold();
        this.headStun[head] = Math.min(threshold, this.headStun[head] + Math.min(amount, threshold / 3.0F));
        if (!this.headStunned(head) || !(this.level() instanceof ServerLevel level)) return;
        Vec3 at = this.partCenter(WitherstormLayout.HEAD_CENTER + head);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 20, 0.4D, 0.4D, 0.4D, 0.05D);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 6.0F, 0.4F);
        for (int i = 0; i < WitherstormLayout.HEAD_COUNT; i++) {
            if (!this.headStunned(i)) return;
        }
        this.openCore(level);
    }

    private void openCore(ServerLevel level) {
        this.scheduler.cancelAll();
        if (this.devour != null) this.devour.release();
        this.lunging = false;
        this.coreTicks = CORE_OPEN_TICKS;
        this.entityData.set(CORE_OPEN, true);
        Vec3 core = this.partCenter(WitherstormLayout.CORE);
        level.playSound(null, core.x, core.y, core.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, core.x, core.y, core.z, SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 6.0F, 1.6F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, core.x, core.y, core.z, 60, 0.6D, 0.6D, 0.6D, 0.15D);
    }

    private void closeCore(ServerLevel level) {
        this.entityData.set(CORE_OPEN, false);
        java.util.Arrays.fill(this.headStun, 0.0F);
        this.attackGap = 20;
        Vec3 core = this.partCenter(WitherstormLayout.CORE);
        level.playSound(null, core.x, core.y, core.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0F, 0.5F);
        this.playAnim("roar");
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide) LIVE.add(this);
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        LIVE.remove(this);
    }

    static List<WitherstormEntity> live(ServerLevel level) {
        return LIVE.isEmpty() ? List.of() : LIVE.stream().filter(storm -> storm.level() == level).toList();
    }

    @Override
    public void onBelowWorld() {
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.devour != null) this.devour.release();
        super.remove(reason);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.WITHER) && !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        this.beginIntro();
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    public void beginIntro() {
        this.introTicks = INTRO_TICKS;
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!this.roaming) this.bossEvent.addPlayer(player);
    }

    private void updateRoamingBossBar(ServerLevel level) {
        double range = this.engageRange() + 16.0D;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            if (dx * dx + dz * dz <= range * range || this.getTarget() == player) this.bossEvent.addPlayer(player);
            else this.bossEvent.removePlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 8.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stage", this.getStage());
        tag.putInt("Intro", this.introTicks);
        tag.putInt("CoreTicks", this.coreTicks);
        tag.putDouble("Hunger", this.hunger);
        ListTag stun = new ListTag();
        for (float value : this.headStun) stun.add(FloatTag.valueOf(value));
        tag.put("HeadStun", stun);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Stage")) this.setStage(tag.getInt("Stage"));
        this.introTicks = tag.getInt("Intro");
        this.coreTicks = tag.getInt("CoreTicks");
        this.hunger = tag.getDouble("Hunger");
        this.entityData.set(CORE_OPEN, this.coreTicks > 0);
        ListTag stun = tag.getList("HeadStun", Tag.TAG_FLOAT);
        for (int i = 0; i < Math.min(stun.size(), this.headStun.length); i++) this.headStun[i] = stun.getFloat(i);
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }

    public void recordPlaced(BlockPos pos) {
        if (this.placedBlocks.size() < PLACED_LIMIT) this.placedBlocks.add(pos.asLong());
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide) this.steer();
        super.aiStep();
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private void roam(Vec3 motion) {
        if (this.roamGoal == null) {
            this.setDeltaMovement(motion.scale(0.7D));
            return;
        }
        double desiredY = this.groundY() + this.hoverOffset();
        double vy = Mth.clamp((desiredY - this.getY()) * 0.04D, -0.3D, 0.3D);
        Vec3 flat = new Vec3(this.roamGoal.x - this.getX(), 0.0D, this.roamGoal.z - this.getZ());
        double distance = flat.length();
        Vec3 desired = distance > 1.0D ? flat.scale(Math.min(this.roamSpeed, distance * 0.05D) / distance) : Vec3.ZERO;
        this.setDeltaMovement(Mth.lerp(0.05D, motion.x, desired.x), vy, Mth.lerp(0.05D, motion.z, desired.z));
        if (distance > 4.0D) this.face(this.roamGoal);
    }

    private void steer() {
        Vec3 motion = this.getDeltaMovement();
        if (this.lunging && this.lungePoint != null) {
            Vec3 to = this.lungePoint.add(0.0D, 0.5D, 0.0D).subtract(this.position());
            double distance = to.length();
            this.setDeltaMovement(distance > 0.5D ? to.scale(Math.min(1.6D, distance) / distance) : Vec3.ZERO);
            this.face(this.lungePoint);
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null && this.roaming && this.introTicks <= 0 && !this.isCoreOpen()) {
            this.roam(motion);
            return;
        }
        if (this.introTicks > 0 || this.isCoreOpen() || target == null) {
            Vec3 drift = motion.scale(0.7D);
            if (this.isCoreOpen() && !this.isSmallForm() && target instanceof Player player
                    && !this.airborne(player) && this.distanceToSqr(player) < this.engageRange() * this.engageRange()) {
                double desiredCoreY = Math.max(this.groundY(), player.getY()) + 5.0D;
                double descent = Mth.clamp((desiredCoreY - this.partCenter(WitherstormLayout.CORE).y) * 0.1D, -0.65D, 0.0D);
                if (this.getY() > this.groundY() + 4.0D) drift = new Vec3(drift.x, descent, drift.z);
            }
            this.setDeltaMovement(drift);
            if (target != null) this.face(target.position());
            return;
        }
        double reach = this.attackScale();
        double speed = this.isSmallForm() ? 0.32D : Math.min(0.8D, 0.25D + 0.05D * this.scale());
        double desiredY = target.getY() + this.hoverOffset();
        double vy = Mth.clamp((desiredY - this.getY()) * 0.08D, -0.25D * reach, 0.3D * reach);
        Vec3 flat = new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
        double distance = flat.length();
        Vec3 desired = Vec3.ZERO;
        if (distance > 0.01D) {
            Vec3 dir = flat.scale(1.0D / distance);
            if (distance > 16.0D * reach) desired = dir.scale(speed);
            else if (distance < 8.0D * reach) desired = dir.scale(-0.2D);
            else desired = new Vec3(-dir.z, 0.0D, dir.x).scale(0.06D);
        }
        double vx = Mth.lerp(0.15D, motion.x, desired.x);
        double vz = Mth.lerp(0.15D, motion.z, desired.z);
        this.setDeltaMovement(vx, vy, vz);
        this.face(target.position());
    }

    private void face(Vec3 point) {
        double dx = point.x - this.getX();
        double dz = point.z - this.getZ();
        if (dx * dx + dz * dz < 0.01D) return;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 6.0F));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel level)) return;
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.tickCount % 10 == 0) this.updateFightStatus();
        if (!this.isSmallForm() && this.tickCount % 20 == 0) this.refreshGround();
        if (this.roaming && this.tickCount % 20 == 0) this.updateRoamingBossBar(level);
        if (!this.isSmallForm() && VoidWorld.isVoid(level)) {
            if (this.devour == null) this.devour = new WitherstormDevour(this);
            if (!this.isCoreOpen() && this.introTicks <= 0) this.devour.tick(level);
            if (this.tickCount % 40 == 0) this.syncEaten();
        }
        if (this.introTicks > 0) {
            this.tickIntro(level);
            return;
        }
        this.scheduler.tick();
        this.tickCore(level);
        this.tickGrounded(level);
        if (!this.isCoreOpen() && this.tickCount % 40 == 0) this.witheringPresence(level);
        if (this.tickCount % 10 == 0) this.stunnedHeadSmoke(level);
        if (this.isCoreOpen() || this.scheduler.anyBusy()) return;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;
        if (this.attackGap > 0) {
            this.attackGap--;
            return;
        }
        this.chooseAttack(level, target);
    }

    private void updateFightStatus() {
        Component name = this.getDisplayName();
        if (this.isCoreOpen()) {
            name = name.copy().append(" | Core exposed: " + ((this.coreTicks + 19) / 20) + "s");
        }
        this.bossEvent.setName(name);
    }

    private void tickIntro(ServerLevel level) {
        if (this.introTicks == INTRO_TICKS) {
            this.playAnim("roar");
            level.globalLevelEvent(1023, this.blockPosition(), 0);
        }
        this.introTicks--;
        Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.5D + 1.5D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 6, 1.5D, 1.5D, 1.5D, 0.02D);
        if (this.introTicks == 0) this.attackGap = 30;
    }

    private void tickCore(ServerLevel level) {
        if (this.coreTicks <= 0) return;
        this.coreTicks--;
        if (this.coreTicks % 4 == 0) {
            Vec3 core = this.partCenter(WitherstormLayout.CORE);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, core.x, core.y, core.z, 6, 0.5D, 0.5D, 0.5D, 0.03D);
        }
        if (this.coreTicks == 0) this.closeCore(level);
    }

    private void stunnedHeadSmoke(ServerLevel level) {
        for (int i = 0; i < WitherstormLayout.HEAD_COUNT; i++) {
            if (!this.headStunned(i)) continue;
            Vec3 at = this.partCenter(WitherstormLayout.HEAD_CENTER + i);
            level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 4, 0.3D, 0.3D, 0.3D, 0.01D);
        }
    }

    private void witheringPresence(ServerLevel level) {
        for (ServerPlayer player : this.fightersWithin(level, PRESENCE_RADIUS * this.attackScale())) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), this);
        }
    }

    private List<ServerPlayer> fightersWithin(ServerLevel level, double radius) {
        AABB box = this.getBoundingBox().inflate(radius);
        return level.getPlayers(player -> this.fighting(player) && box.intersects(player.getBoundingBox()));
    }

    private boolean fighting(Player player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private boolean airborne(Player player) {
        if (player.isFallFlying() || player.getAbilities().flying) return true;
        if (player.onGround() || player.isInWater()) return false;
        BlockPos below = player.blockPosition();
        for (int i = 0; i < 4; i++) {
            below = below.below();
            if (!this.level().getBlockState(below).isAir()) return false;
        }
        return true;
    }

    private void chooseAttack(ServerLevel level, LivingEntity target) {
        double reach = this.attackScale();
        double roar = this.roarRadius();
        List<ServerPlayer> nearby = this.fightersWithin(level, roar);
        boolean anyAirborne = nearby.stream().anyMatch(this::airborne);
        boolean builds = this.hasPlacedNear(roar);
        boolean headsReady = false;
        for (int i = 0; i < WitherstormLayout.HEAD_COUNT; i++) headsReady |= !this.headStunned(i);
        boolean canBarrage = headsReady;
        double distance = this.distanceTo(target);
        String choice = this.scheduler.chooseWeighted(STAGE_ONE_ATTACKS, id -> switch (id) {
            case SKULL_BARRAGE -> canBarrage ? 6 : 0;
            case DEVOURING_LUNGE -> distance < 40.0D * reach && this.hasLineOfSight(target) ? 3 + this.getStage() : 0;
            case SHOCKWAVE_ROAR -> 2 + (anyAirborne ? 8 : 0) + (builds ? 5 : 0) + (distance < 10.0D * reach ? 3 : 0);
            default -> 0;
        }, this.random);
        if (choice == null) return;
        switch (choice) {
            case SKULL_BARRAGE -> this.startSkullBarrage(level);
            case DEVOURING_LUNGE -> this.startDevouringLunge(level, target);
            case SHOCKWAVE_ROAR -> this.startShockwaveRoar(level);
            default -> {
            }
        }
    }

    float attackDamage() {
        return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private double roarRadius() {
        return ROAR_RADIUS * this.attackScale();
    }

    private void startSkullBarrage(ServerLevel level) {
        int windup = this.getStage() >= 4 ? 20 : 25;
        this.scheduler.start(SKULL_BARRAGE, WitherstormAttackScheduler.Lane.HEADS, 140, windup, 48, 20, new WitherstormAttackScheduler.Action() {
            @Override
            public void onWindup(int elapsed, int remaining) {
                for (int i = 0; i < WitherstormLayout.HEAD_COUNT; i++) {
                    if (headStunned(i)) continue;
                    Vec3 at = partCenter(WitherstormLayout.HEAD_CENTER + i);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, 3, 0.25D, 0.25D, 0.25D, 0.01D);
                }
                if (elapsed % 5 == 0) {
                    playSound(SoundEvents.BEACON_AMBIENT, 3.0F, 0.6F + elapsed * 0.05F);
                }
            }

            @Override
            public void onActive(int elapsed) {
                if (elapsed % 12 != 0) return;
                playAnim("shoot");
                volley(level);
            }

            @Override
            public void onComplete() {
                attackGap = 30;
            }
        });
    }

    private void volley(ServerLevel level) {
        LivingEntity main = this.getTarget();
        if (main == null) return;
        List<ServerPlayer> others = this.fightersWithin(level, 48.0D * this.attackScale()).stream()
                .filter(player -> player != main && this.hasLineOfSight(player)).toList();
        boolean fired = false;
        for (int i = 0; i < WitherstormLayout.HEAD_COUNT; i++) {
            if (this.headStunned(i)) continue;
            LivingEntity aim = i == 0 || others.isEmpty() ? main : others.get(this.random.nextInt(others.size()));
            Vec3 origin = this.partCenter(WitherstormLayout.HEAD_CENTER + i);
            Vec3 aimAt = aim.position().add(0.0D, aim.getBbHeight() * 0.5D, 0.0D);
            double travel = origin.distanceTo(aimAt);
            aimAt = aimAt.add(aim.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).scale(travel * 0.6D));
            Vec3 direction = aimAt.subtract(origin).normalize();
            boolean blue = this.random.nextInt(5) == 0;
            WitherstormSkull skull = new WitherstormSkull(level, this, direction, this.attackDamage() * 0.4F, blue);
            skull.setPos(origin.x, origin.y, origin.z);
            level.addFreshEntity(skull);
            fired = true;
        }
        if (fired) this.playSound(SoundEvents.WITHER_SHOOT, 6.0F, 0.7F);
    }

    private void startDevouringLunge(ServerLevel level, LivingEntity target) {
        this.lungePoint = this.groundBelow(level, target.position());
        this.lungeLanded = false;
        this.scheduler.start(DEVOURING_LUNGE, WitherstormAttackScheduler.Lane.BODY, 220, 32, 24, 30, new WitherstormAttackScheduler.Action() {
            @Override
            public void onStart() {
                playAnim("devour");
                playSound(SoundEvents.WITHER_SPAWN, 10.0F, 0.5F);
            }

            @Override
            public void onWindup(int elapsed, int remaining) {
                LivingEntity current = getTarget();
                if (elapsed <= 16 && current != null && current.isAlive()) {
                    lungePoint = groundBelow(level, current.position());
                }
                if (elapsed % 3 == 0 && lungePoint != null) {
                    ring(level, lungePoint.add(0.0D, 0.2D, 0.0D), 3.5D * attackScale(), (int) Math.min(96, 24 * attackScale()), TELL_DUST);
                }
                if (remaining == 10) {
                    playSound(SoundEvents.WITHER_AMBIENT, 8.0F, 0.6F);
                }
            }

            @Override
            public void onActive(int elapsed) {
                if (elapsed == 0) {
                    lunging = isSmallForm();
                    playSound(SoundEvents.WITHER_SHOOT, 8.0F, 0.4F);
                    if (!isSmallForm() && lungePoint != null) {
                        playAnim("tractor_beam");
                        beam(level, mouth(), lungePoint, 40);
                        if (getStage() < 5) bite(level, lungePoint);
                    }
                }
                if (lungeLanded || lungePoint == null) return;
                if (getStage() >= 5) {
                    if (elapsed % 4 == 0) {
                        beam(level, mouth(), lungePoint, 16);
                        LivingEntity current = getTarget();
                        if (current instanceof ServerPlayer player && fighting(player) && hasLineOfSight(player)
                                && player.distanceToSqr(lungePoint) < Mth.square(3.5D * attackScale())) {
                            Vec3 pull = lungePoint.subtract(player.position()).multiply(1.0D, 0.0D, 1.0D).normalize().scale(0.35D);
                            player.push(pull.x, 0.0D, pull.z);
                            player.hurtMarked = true;
                        }
                    }
                    if (elapsed >= 23) bite(level, lungePoint);
                    return;
                }
                if (position().distanceTo(lungePoint) < 3.0D || elapsed >= 23) {
                    bite(level, lungePoint);
                }
            }

            @Override
            public void onComplete() {
                lunging = false;
                lungePoint = null;
                attackGap = 30;
            }
        });
    }

    private Vec3 groundBelow(ServerLevel level, Vec3 from) {
        BlockPos.MutableBlockPos cursor = BlockPos.containing(from).mutable();
        for (int i = 0; i < 24 && cursor.getY() > level.getMinBuildHeight(); i++) {
            if (!level.getBlockState(cursor.below()).getCollisionShape(level, cursor.below()).isEmpty()) break;
            cursor.move(0, -1, 0);
        }
        return new Vec3(from.x, cursor.getY(), from.z);
    }

    private void bite(ServerLevel level, Vec3 center) {
        this.lungeLanded = true;
        this.lunging = false;
        this.setDeltaMovement(Vec3.ZERO);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y + 1.0D, center.z, 40, 1.5D, 1.0D, 1.5D, 0.05D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 4.0F, 0.5F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 0.7F);
        double reach = this.attackScale();
        CameraShakeSync.sendNearby(level, center, 1.0F, 16, (float) (40.0D * reach));
        this.breakSphere(level, center, 3.0D * reach, (int) Math.min(600, 120 * reach));
        DamageSource source = this.damageSources().mobAttack(this);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(3.5D * reach), this::canHurt)) {
            if (victim.position().distanceTo(center) > 4.0D * reach) continue;
            if (victim.hurt(source, this.attackDamage() * 1.25F)) {
                victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1), this);
            }
        }
    }

    private boolean canHurt(Entity entity) {
        if (entity == this || !entity.isAlive()) return false;
        if (entity instanceof Player player) return this.fighting(player);
        return !(entity instanceof WitherstormEntity);
    }

    private boolean griefing(ServerLevel level) {
        return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    boolean breakable(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0.0F
                && !state.is(BlockTags.WITHER_IMMUNE) && state.getBlock().getExplosionResistance() <= 1200.0F
                && level.getWorldBorder().isWithinBounds(pos);
    }

    private void breakSphere(ServerLevel level, Vec3 center, double radius, int maxBlocks) {
        if (!this.griefing(level)) return;
        double radiusSqr = radius * radius;
        int broken = 0;
        for (BlockPos cursor : BlockPos.betweenClosed(BlockPos.containing(center.subtract(radius, radius, radius)), BlockPos.containing(center.add(radius, radius, radius)))) {
            if (broken >= maxBlocks) return;
            if (cursor.distToCenterSqr(center.x, center.y, center.z) > radiusSqr) continue;
            BlockState state = level.getBlockState(cursor);
            if (!this.breakable(level, cursor, state)) continue;
            BlockPos pos = cursor.immutable();
            if (this.random.nextFloat() < 0.05F) {
                level.destroyBlock(pos, true, this);
            } else {
                if (broken % 8 == 0) level.levelEvent(2001, pos, Block.getId(state));
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            this.placedBlocks.remove(pos.asLong());
            broken++;
        }
    }

    private void startShockwaveRoar(ServerLevel level) {
        this.scheduler.start(SHOCKWAVE_ROAR, WitherstormAttackScheduler.Lane.BODY, 300, 34, 1, 40, new WitherstormAttackScheduler.Action() {
            @Override
            public void onStart() {
                playAnim("roar");
                playSound(SoundEvents.WITHER_SPAWN, 10.0F, 0.6F);
                skyFlash(level);
            }

            @Override
            public void onWindup(int elapsed, int remaining) {
                if (elapsed % 2 == 0) {
                    double reach = attackScale();
                    Vec3 center = position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 12, 2.0D * reach, 2.0D * reach, 2.0D * reach, 0.1D);
                    ring(level, position(), (4.0D + elapsed * 0.4D) * reach, (int) Math.min(80, 20 * reach), ParticleTypes.SMOKE);
                }
                if (remaining == 12) {
                    skyFlash(level);
                }
            }

            @Override
            public void onActive(int elapsed) {
                shockwave(level);
            }

            @Override
            public void onComplete() {
                attackGap = 40;
            }
        });
    }

    private void skyFlash(ServerLevel level) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(this.getX(), this.getY() + this.getBbHeight() + 6.0D, this.getZ());
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    private void shockwave(ServerLevel level) {
        Vec3 origin = this.position();
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 5.0F, 0.6F);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.5F);
        double radius = this.roarRadius();
        double step = 4.0D * this.attackScale();
        for (double r = step; r <= radius; r += step) {
            this.ring(level, origin.add(0.0D, 0.5D, 0.0D), r, (int) Math.min(160, 12 + r * 2), ParticleTypes.SWEEP_ATTACK);
        }
        CameraShakeSync.sendNearby(level, origin, 1.4F, 30, (float) (radius * 1.5D));
        DamageSource source = this.damageSources().mobAttack(this);
        long until = level.getGameTime() + GROUNDED_TICKS;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius), this::canHurt)) {
            Vec3 away = victim.position().subtract(origin);
            double distance = away.length();
            if (distance > radius) continue;
            Vec3 flat = new Vec3(away.x, 0.0D, away.z);
            Vec3 push = flat.lengthSqr() < 0.01D ? Vec3.ZERO : flat.normalize().scale(0.8D + 2.2D * (1.0D - distance / radius));
            victim.hurt(source, this.attackDamage() * 0.75F);
            victim.setDeltaMovement(push.x, 0.6D, push.z);
            victim.hurtMarked = true;
            if (victim instanceof ServerPlayer player) {
                this.grounded.put(player.getUUID(), until);
                this.ground(player);
            }
        }
        this.breakPlaced(level, radius, 256);
    }

    void beam(ServerLevel level, Vec3 from, Vec3 to, int points) {
        Vec3 span = to.subtract(from);
        for (ServerPlayer player : level.players()) {
            for (int i = 0; i < points; i++) {
                Vec3 at = from.add(span.scale(this.random.nextDouble()));
                level.sendParticles(player, BEAM_DUST, true, at.x, at.y, at.z, 1, 0.6D, 0.6D, 0.6D, 0.0D);
            }
        }
    }

    public void roarFromAfar() {
        if (!this.isSmallForm()) this.playAnim("roar");
    }

    private boolean hasPlacedNear(double radius) {
        double radiusSqr = radius * radius;
        LongIterator iterator = this.placedBlocks.iterator();
        while (iterator.hasNext()) {
            if (BlockPos.of(iterator.nextLong()).distToCenterSqr(this.position()) <= radiusSqr) return true;
        }
        return false;
    }

    private void breakPlaced(ServerLevel level, double radius, int maxBlocks) {
        if (this.placedBlocks.isEmpty()) return;
        boolean grief = this.griefing(level);
        double radiusSqr = radius * radius;
        double forgetSqr = Math.max(128.0D * 128.0D, radiusSqr * 4.0D);
        List<BlockPos> hits = new ArrayList<>();
        LongIterator iterator = this.placedBlocks.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = BlockPos.of(iterator.nextLong());
            double distance = pos.distToCenterSqr(this.position());
            if (!level.isLoaded(pos) || distance > forgetSqr) {
                iterator.remove();
                continue;
            }
            if (distance > radiusSqr) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                iterator.remove();
                continue;
            }
            if (grief && hits.size() < maxBlocks && this.breakable(level, pos, state)) {
                hits.add(pos);
                iterator.remove();
            }
        }
        for (BlockPos pos : hits) level.destroyBlock(pos, true, this);
    }

    private void tickGrounded(ServerLevel level) {
        if (this.grounded.isEmpty()) return;
        long now = level.getGameTime();
        this.grounded.values().removeIf(until -> until < now);
        for (UUID id : this.grounded.keySet()) {
            if (level.getPlayerByUUID(id) instanceof ServerPlayer player) this.ground(player);
        }
    }

    private void ground(ServerPlayer player) {
        if (!this.fighting(player)) return;
        if (player.isFallFlying()) player.stopFallFlying();
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private void ring(ServerLevel level, Vec3 center, double radius, int points, ParticleOptions particle) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius, 1, 0.0D, 0.05D, 0.0D, 0.0D);
        }
    }

    void playAnim(String name) {
        String prefix = this.isSmallForm() ? "small_" : "big_";
        String resolved = this.isSmallForm() && name.equals("core_hit") ? "hit" : !this.isSmallForm() && name.equals("hit") ? "hurt" : name;
        this.triggerAnim("attack", prefix + resolved);
    }

    @Override
    public void die(DamageSource source) {
        boolean first = !this.dead && !this.isRemoved();
        super.die(source);
        if (!this.dead || !(this.level() instanceof ServerLevel level)) return;
        if (first) this.giveReward(level, source);
        if (this.roaming) VoidStorm.recordDefeat(level, this);
        if (this.devour != null) this.devour.release();
    }

    @Nullable
    private static UUID playerBehind(@Nullable Entity attacker) {
        if (attacker instanceof Player player && !player.isSpectator()) return player.getUUID();
        if (attacker instanceof OwnableEntity owned && owned.getOwner() instanceof Player owner) return owner.getUUID();
        return null;
    }

    private void creditDamage(DamageSource source) {
        UUID id = playerBehind(source.getEntity());
        if (id == null) return;
        this.damagers.put(id, this.level().getGameTime());
        this.lastDamager = id;
    }

    @Nullable
    private UUID killCredit(DamageSource source) {
        UUID direct = playerBehind(source.getEntity());
        if (direct != null) return direct;
        if (this.lastHurtByPlayer != null) return this.lastHurtByPlayer.getUUID();
        if (this.lastDamager == null) return null;
        Long at = this.damagers.get(this.lastDamager);
        return at != null && this.level().getGameTime() - at <= KILL_CREDIT_TICKS ? this.lastDamager : null;
    }

    private static int commandBlockDrops(int stage) {
        List<? extends Integer> drops = VoidConfig.SPEC.isLoaded() ? VoidConfig.STORM_COMMAND_BLOCK_DROPS.get() : DEFAULT_COMMAND_BLOCK_DROPS;
        int index = stage - 1;
        return index >= 0 && index < drops.size() ? drops.get(index) : 0;
    }

    @Nullable
    private ServerPlayer usable(ServerLevel level, UUID id) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
        return player != null && player.isAlive() && !player.isSpectator() ? player : null;
    }

    @Nullable
    private ServerPlayer rewardRecipient(ServerLevel level, UUID killer) {
        ServerPlayer direct = this.usable(level, killer);
        if (direct != null && direct.level() == level) return direct;
        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (UUID id : this.damagers.keySet()) {
            ServerPlayer player = this.usable(level, id);
            if (player == null || player.level() != level) continue;
            double distance = player.distanceToSqr(this);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best != null ? best : direct;
    }

    private void giveReward(ServerLevel level, DamageSource source) {
        if (this.rewarded) return;
        this.rewarded = true;
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
        int count = commandBlockDrops(this.getStage());
        if (count <= 0) return;
        Item item = BuiltInRegistries.ITEM.get(IntegratedSuperheroes.id("storm_command_block"));
        if (item == Items.AIR) return;
        UUID killer = this.killCredit(source);
        if (killer == null) return;
        ServerPlayer recipient = this.rewardRecipient(level, killer);
        ItemStack stack = new ItemStack(item, count);
        if (recipient != null) {
            recipient.getInventory().add(stack);
            recipient.containerMenu.broadcastChanges();
            if (!stack.isEmpty()) safeDrop((ServerLevel) recipient.level(), recipient.position(), stack);
            return;
        }
        int x = Mth.floor(this.getX());
        int z = Mth.floor(this.getZ());
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        double y = ground > level.getMinBuildHeight() ? ground : level.getMinBuildHeight() + 80;
        safeDrop(level, new Vec3(this.getX(), y + 0.5D, this.getZ()), stack);
    }

    private static void safeDrop(ServerLevel level, Vec3 at, ItemStack stack) {
        ItemEntity drop = new ItemEntity(level, at.x, at.y, at.z, stack, 0.0D, 0.0D, 0.0D);
        drop.setUnlimitedLifetime();
        drop.setInvulnerable(true);
        drop.setNoGravity(level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(at.x), Mth.floor(at.z)) <= level.getMinBuildHeight());
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        this.lunging = false;
        if (this.level() instanceof ServerLevel level && this.deathTime % 4 == 0) {
            Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.5D + 1.5D, 0.0D);
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 3, 2.0D, 2.0D, 2.0D, 0.0D);
        }
        if (this.deathTime >= 50 && !this.level().isClientSide && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 6, this::bodyState));
        AnimationController<WitherstormEntity> attack = new AnimationController<>(this, "attack", 2, state -> PlayState.STOP);
        for (String name : SMALL_TRIGGERS) attack.triggerableAnim("small_" + name, RawAnimation.begin().thenPlay(SMALL + name));
        for (String name : BIG_TRIGGERS) attack.triggerableAnim("big_" + name, RawAnimation.begin().thenPlay(BIG + name));
        controllers.add(attack);
    }

    private PlayState bodyState(AnimationState<WitherstormEntity> state) {
        boolean small = this.isSmallForm();
        if (this.isDeadOrDying()) return state.setAndContinue(small ? SMALL_DEATH : BIG_DEATH);
        if (small) return state.setAndContinue(SMALL_FLY);
        if (this.isCoreOpen()) return state.setAndContinue(BIG_CORE);
        return state.setAndContinue(state.isMoving() ? BIG_MOVE : BIG_IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
