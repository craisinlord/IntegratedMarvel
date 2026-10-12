package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.antarchy.content.AntarchySoundEvents;
import com.craisinlord.antarchy.content.entity.flying_squirrel.FlyingSquirrelEntity;
import com.craisinlord.integrated_marvel.mixin.FlyingSquirrelInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

public class SwarmSquirrel extends FlyingSquirrelEntity {
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
        ((FlyingSquirrelInvoker) this).integratedMarvel$setTextureVariant(variant);
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
        return new WallClimberNavigation(this, level);
    }

    @Override
    public boolean onClimbable() {
        return super.onClimbable() || this.horizontalCollision && !this.isOnShoulder() && !this.isClimbingToShoulder();
    }

    private boolean free() {
        return !this.isOnShoulder() && !this.isClimbingToShoulder() && this.pileTarget == null && !this.isOrderedToSit();
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
        this.goalSelector.addGoal(7, new FollowOwnerGoal(this, 1.3D, 6.0F, 2.0F) {
            @Override
            public boolean canUse() {
                return SwarmSquirrel.this.free() && SwarmSquirrel.this.guardPos == null && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return SwarmSquirrel.this.free() && SwarmSquirrel.this.guardPos == null && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
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
    public boolean doHurtTarget(Entity target) {
        boolean hit = this.bite(target, this.biteDamage);
        if (hit) {
            this.swing(InteractionHand.MAIN_HAND);
            if (this.random.nextInt(3) == 0) {
                this.playSound(AntarchySoundEvents.FLYING_SQUIRREL_BEG.get(), 0.5F, 1.3F + this.random.nextFloat() * 0.3F);
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
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount % 10 == 0 && !this.ownerValid()) {
            this.vanish();
            return;
        }
        if (this.pickupTicks > 0) {
            this.pickupTicks--;
            this.getNavigation().stop();
            if (this.pickupTicks == 0) {
                this.setPickingUpNut(false);
            }
        }
        if (this.pileTarget != null) {
            this.tickPile();
        }
        if (this.tippy) {
            this.tickTippy();
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
        if (target != null && target.isAlive() || this.pileTarget != null) {
            this.idleTicks = 0;
            return;
        }
        this.idleTicks++;
        if (this.idleTicks < 60) {
            return;
        }
        if (this.distanceToSqr(owner) < 6.0D) {
            ((FlyingSquirrelInvoker) this).integratedMarvel$mountShoulder(owner);
            this.idleTicks = 0;
        } else if (this.getNavigation().isDone()) {
            this.getNavigation().moveTo(owner, 1.4D);
        }
    }

    public void defend(LivingEntity attacker) {
        if (this.isOnShoulder() || this.isClimbingToShoulder()) {
            ((FlyingSquirrelInvoker) this).integratedMarvel$dismountShoulder();
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
        this.getNavigation().stop();
    }

    public void clearGuard() {
        this.guardPos = null;
    }

    public void pile(LivingEntity target, int ticks, float damage, int index) {
        this.pileTarget = target;
        this.pileTicks = ticks;
        this.pileDamage = damage;
        this.pileIndex = index;
        this.getNavigation().stop();
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

    private final class HoldGoal extends Goal {
        private HoldGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return SwarmSquirrel.this.pileTarget != null || SwarmSquirrel.this.isClimbingToShoulder() || SwarmSquirrel.this.isOnShoulder()
                    || SwarmSquirrel.this.pickupTicks > 0;
        }

        @Override
        public void start() {
            SwarmSquirrel.this.getNavigation().stop();
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
                    SwarmSquirrel.this.playSound(AntarchySoundEvents.FLYING_SQUIRREL_NUT.get(), 0.5F, 1.1F);
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
            SwarmSquirrel.this.getLookControl().setLookAt(item);
            if (SwarmSquirrel.this.distanceToSqr(item) < 2.5D) {
                SwarmSquirrel.this.carried = item.getItem().copy();
                item.discard();
                SwarmSquirrel.this.forageTarget = null;
                SwarmSquirrel.this.pickupTicks = 15;
                SwarmSquirrel.this.setPickingUpNut(true);
                SwarmSquirrel.this.playSound(AntarchySoundEvents.FLYING_SQUIRREL_NUT.get(), 0.5F, 1.3F);
            } else if (--this.repath <= 0) {
                this.repath = 10;
                SwarmSquirrel.this.getNavigation().moveTo(item, 1.4D);
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
                SwarmSquirrel.this.getNavigation().stop();
            } else if (--this.repath <= 0) {
                this.repath = 10;
                SwarmSquirrel.this.getNavigation().moveTo(center.x, center.y, center.z, 1.3D);
            }
        }
    }
}
