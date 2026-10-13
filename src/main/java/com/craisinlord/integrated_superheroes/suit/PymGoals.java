package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.compat.AntarchyColony;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.EnumSet;
import java.util.List;

final class PymGoals {
    private PymGoals() {
    }

    static void install(PymColony.Member member) {
        PathfinderMob mob = member.mob;
        boolean ant = member.kind == PymHelmets.Kind.ANT;
        goal(member, -7, new RiddenGoal(member));
        goal(member, -6, ant ? new OverrunGoal(member) : new DiveGoal(member));
        goal(member, -5, new StayGoal(member));
        goal(member, -4, new StrikeGoal(member));
        goal(member, -3, new GuardGoal(member));
        if (ant && !member.anthony()) {
            goal(member, -3, new ForageGoal(member));
            if (AntarchyColony.isTermite(mob)) {
                goal(member, -3, new ChewGoal(member));
            }
        } else if (!ant) {
            goal(member, -3, new ScreenGoal(member));
        }
        goal(member, -2, new HeelGoal(member));
        Goal target = new CommandTargetGoal(member);
        member.targetGoals.add(target);
        mob.targetSelector.addGoal(-1, target);
    }

    static void uninstall(PymColony.Member member) {
        member.goals.forEach(member.mob.goalSelector::removeGoal);
        member.targetGoals.forEach(member.mob.targetSelector::removeGoal);
        member.goals.clear();
        member.targetGoals.clear();
    }

    private static void goal(PymColony.Member member, int priority, Goal goal) {
        member.goals.add(goal);
        member.mob.goalSelector.addGoal(priority, goal);
    }

    private static ServerPlayer ready(PymColony.Member member) {
        return member.bound() ? member.owner() : null;
    }

    private static boolean free(PymColony.Member member) {
        return ready(member) != null && !member.busy();
    }

    private static void teleportNear(PymColony.Member member, ServerPlayer owner) {
        ServerLevel level = owner.serverLevel();
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos pos = owner.blockPosition().offset(level.random.nextInt(5) - 2, member.kind == PymHelmets.Kind.WASP ? 1 : 0, level.random.nextInt(5) - 2);
            if (level.isEmptyBlock(pos) && (member.kind == PymHelmets.Kind.WASP || !level.isEmptyBlock(pos.below()))) {
                member.mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, member.mob.getYRot(), member.mob.getXRot());
                member.mob.getNavigation().stop();
                return;
            }
        }
    }

    private static final class RiddenGoal extends Goal {
        private final PymColony.Member member;

        private RiddenGoal(PymColony.Member member) {
            this.member = member;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return this.member.bound() && this.member.mob.isVehicle();
        }

        @Override
        public void start() {
            this.member.mob.getNavigation().stop();
        }
    }

    private static final class StayGoal extends Goal {
        private final PymColony.Member member;

        private StayGoal(PymColony.Member member) {
            this.member = member;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return free(this.member) && PymColony.STAY.equals(this.member.order()) && this.member.focus == null
                    && this.member.guard == null && this.member.mob.getTarget() == null;
        }

        @Override
        public void start() {
            this.member.mob.getNavigation().stop();
        }
    }

    private static final class StrikeGoal extends MeleeAttackGoal {
        private final PymColony.Member member;

        private StrikeGoal(PymColony.Member member) {
            super(member.mob, 1.35D, true);
            this.member = member;
        }

        @Override
        public boolean canUse() {
            return free(this.member) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return free(this.member) && super.canContinueToUse();
        }
    }

    private static final class GuardGoal extends Goal {
        private final PymColony.Member member;
        private int repath;

        private GuardGoal(PymColony.Member member) {
            this.member = member;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return free(this.member) && this.member.guard != null && this.member.mob.getTarget() == null;
        }

        @Override
        public void tick() {
            BlockPos pos = this.member.guard;
            if (pos == null) {
                return;
            }
            Vec3 spot = Vec3.atBottomCenterOf(pos);
            if (this.member.mob.distanceToSqr(spot) > 6.25D) {
                if (--this.repath <= 0) {
                    this.repath = 10;
                    this.member.mob.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.25D);
                }
            } else {
                this.member.mob.getNavigation().stop();
            }
        }
    }

    private static final class HeelGoal extends Goal {
        private final PymColony.Member member;
        private int repath;

        private HeelGoal(PymColony.Member member) {
            this.member = member;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return free(this.member) && this.member.guard == null;
        }

        @Override
        public void tick() {
            ServerPlayer owner = this.member.owner();
            if (owner == null) {
                return;
            }
            PathfinderMob mob = this.member.mob;
            double distance = mob.distanceToSqr(owner);
            double leash = PymConfig.LEASH_RANGE.get();
            if (distance > leash * leash) {
                teleportNear(this.member, owner);
            } else if (distance > (this.member.anthony() ? 9.0D : 12.0D)) {
                if (--this.repath <= 0) {
                    this.repath = 10;
                    mob.getNavigation().moveTo(owner, this.member.anthony() ? 1.3D : 1.2D);
                }
            } else {
                mob.getNavigation().stop();
                if (mob.getRandom().nextInt(40) == 0) {
                    mob.getLookControl().setLookAt(owner, 30.0F, 30.0F);
                }
            }
        }
    }

    private static final class ForageGoal extends Goal {
        private final PymColony.Member member;
        private int repath;

        private ForageGoal(PymColony.Member member) {
            this.member = member;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean foraging() {
            return free(this.member) && PymColony.FORAGE.equals(this.member.order()) && this.member.mob.getTarget() == null
                    && this.member.guard == null;
        }

        @Override
        public boolean canUse() {
            if (!this.foraging()) {
                return false;
            }
            if (!this.member.carried.isEmpty()) {
                return true;
            }
            this.member.forage = this.find();
            return this.member.forage != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.foraging() && (!this.member.carried.isEmpty() || this.member.forage != null && this.member.forage.isAlive());
        }

        @Override
        public void stop() {
            this.member.forage = null;
        }

        @Override
        public void tick() {
            ServerPlayer owner = this.member.owner();
            PathfinderMob mob = this.member.mob;
            if (owner == null) {
                return;
            }
            if (!this.member.carried.isEmpty()) {
                if (mob.distanceToSqr(owner) < 4.0D) {
                    ItemHandlerHelper.giveItemToPlayer(owner, this.member.carried);
                    this.member.carried = ItemStack.EMPTY;
                    mob.playSound(SoundEvents.ITEM_PICKUP, 0.3F, 1.4F + mob.getRandom().nextFloat() * 0.3F);
                } else if (--this.repath <= 0) {
                    this.repath = 10;
                    mob.getNavigation().moveTo(owner, 1.3D);
                }
                return;
            }
            ItemEntity item = this.member.forage;
            if (item == null || !item.isAlive()) {
                return;
            }
            mob.getLookControl().setLookAt(item, 30.0F, 30.0F);
            if (mob.distanceToSqr(item) <= 2.0D) {
                this.member.carried = item.getItem().copy();
                item.discard();
                this.member.forage = null;
                mob.playSound(SoundEvents.ITEM_PICKUP, 0.3F, 1.6F);
                this.repath = 0;
            } else if (--this.repath <= 0) {
                this.repath = 10;
                mob.getNavigation().moveTo(item.getX(), item.getY(), item.getZ(), 1.2D);
            }
        }

        private ItemEntity find() {
            ServerPlayer owner = this.member.owner();
            if (owner == null) {
                return null;
            }
            double range = PymConfig.FORAGE_RANGE.get();
            List<PymColony.Member> others = PymColony.members(owner, false);
            ItemEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (ItemEntity item : owner.level().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(range),
                    entity -> entity.isAlive() && !entity.hasPickUpDelay())) {
                if (others.stream().anyMatch(other -> other != this.member && other.forage == item)) {
                    continue;
                }
                double distance = this.member.mob.distanceToSqr(item);
                if (distance < bestDistance) {
                    best = item;
                    bestDistance = distance;
                }
            }
            return best;
        }
    }

    private static final class ChewGoal extends Goal {
        private final PymColony.Member member;
        private int repath;

        private ChewGoal(PymColony.Member member) {
            this.member = member;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return free(this.member) && PymColony.CHEW.equals(this.member.order()) && this.member.mob.getTarget() == null
                    && this.member.guard == null && PymColony.chewTarget(this.member.owner) != null;
        }

        @Override
        public void tick() {
            BlockPos pos = PymColony.chewTarget(this.member.owner);
            if (pos == null) {
                return;
            }
            PathfinderMob mob = this.member.mob;
            Vec3 center = Vec3.atCenterOf(pos);
            mob.getLookControl().setLookAt(center.x, center.y, center.z);
            if (mob.distanceToSqr(center) < (PymColony.CHEW_REACH - 0.5D) * (PymColony.CHEW_REACH - 0.5D)) {
                mob.getNavigation().stop();
                if (mob.getRandom().nextInt(12) == 0) {
                    mob.swing(InteractionHand.MAIN_HAND);
                }
            } else if (--this.repath <= 0) {
                this.repath = 10;
                mob.getNavigation().moveTo(center.x, center.y, center.z, 1.25D);
            }
        }
    }

    private static final class ScreenGoal extends Goal {
        private final PymColony.Member member;

        private ScreenGoal(PymColony.Member member) {
            this.member = member;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return free(this.member) && PymColony.SCREEN.equals(this.member.order()) && this.member.mob.getTarget() == null
                    && this.member.guard == null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ServerPlayer owner = this.member.owner();
            if (owner == null) {
                return;
            }
            PathfinderMob mob = this.member.mob;
            if (mob.distanceToSqr(owner) > 64.0D) {
                mob.getNavigation().moveTo(owner, 1.4D);
                return;
            }
            double angle = this.member.screenIndex * Math.PI * 2.0D / Math.max(1, this.member.screenCount) + owner.tickCount * 0.12D;
            double radius = 1.2D + owner.getBbWidth();
            double height = owner.getY() + owner.getBbHeight() * 0.7D + Math.sin(owner.tickCount * 0.2D + this.member.screenIndex) * 0.25D;
            mob.getNavigation().stop();
            mob.getMoveControl().setWantedPosition(owner.getX() + Math.cos(angle) * radius, height, owner.getZ() + Math.sin(angle) * radius, 1.4D);
        }
    }

    private static final class OverrunGoal extends Goal {
        private final PymColony.Member member;

        private OverrunGoal(PymColony.Member member) {
            this.member = member;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return this.member.bound() && this.member.overrun != null && !this.member.mob.isVehicle();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.member.mob.getNavigation().stop();
        }

        @Override
        public void stop() {
            this.member.mob.setNoGravity(false);
            this.member.overrun = null;
        }

        @Override
        public void tick() {
            PathfinderMob mob = this.member.mob;
            LivingEntity target = this.member.overrun;
            if (target == null || !target.isAlive() || target.level() != mob.level() || this.member.overrunTicks-- <= 0) {
                this.member.overrun = null;
                mob.setNoGravity(false);
                mob.setDeltaMovement(mob.getRandom().nextGaussian() * 0.15D, 0.3D, mob.getRandom().nextGaussian() * 0.15D);
                if (target != null && PymColony.allowed(this.member, target)) {
                    this.member.focus = target;
                    PymColony.command(this.member, target);
                }
                return;
            }
            double angle = this.member.overrunIndex * 2.39996D + mob.tickCount * 0.05D;
            double radius = target.getBbWidth() * 0.5D + mob.getBbWidth() * 0.3D;
            double height = target.getBbHeight() * (0.2D + (this.member.overrunIndex % 4) * 0.2D);
            Vec3 spot = target.position().add(Math.cos(angle) * radius, Math.min(height, target.getBbHeight()), Math.sin(angle) * radius);
            Vec3 step = spot.subtract(mob.position());
            if (step.length() > 0.8D) {
                spot = mob.position().add(step.normalize().scale(0.8D));
            }
            mob.setPos(spot.x, spot.y, spot.z);
            mob.setDeltaMovement(Vec3.ZERO);
            mob.setNoGravity(true);
            mob.fallDistance = 0.0F;
            mob.lookAt(target, 30.0F, 30.0F);
            if (this.member.overrunTicks % 10 == 0 && mob.distanceToSqr(target) < 9.0D) {
                Vec3 motion = target.getDeltaMovement();
                target.invulnerableTime = 0;
                target.hurt(mob.damageSources().mobAttack(mob), PymConfig.OVERRUN_BITE_DAMAGE.get().floatValue());
                target.setDeltaMovement(motion);
                mob.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    private static final class DiveGoal extends Goal {
        private final PymColony.Member member;

        private DiveGoal(PymColony.Member member) {
            this.member = member;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return this.member.bound() && this.member.dive != null && !this.member.mob.isVehicle();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.member.mob.getNavigation().stop();
        }

        @Override
        public void stop() {
            this.member.dive = null;
        }

        private void finish(LivingEntity target) {
            this.member.dive = null;
            this.member.diveTicks = 0;
            if (target != null && PymColony.allowed(this.member, target)) {
                this.member.focus = target;
                PymColony.command(this.member, target);
            }
        }

        @Override
        public void tick() {
            PathfinderMob mob = this.member.mob;
            LivingEntity target = this.member.dive;
            if (target == null || !target.isAlive() || target.level() != mob.level() || this.member.diveTicks > 80) {
                this.finish(null);
                return;
            }
            Vec3 aim = target.getBoundingBox().getCenter();
            mob.getLookControl().setLookAt(aim.x, aim.y, aim.z);
            if (this.member.diveDelay > 0) {
                this.member.diveDelay--;
                mob.getNavigation().stop();
                return;
            }
            this.member.diveTicks++;
            Vec3 toward = aim.subtract(mob.position());
            mob.setDeltaMovement(toward.normalize().scale(0.85D));
            if (mob.getBoundingBox().inflate(0.4D).intersects(target.getBoundingBox()) || toward.lengthSqr() < 1.2D) {
                double attack = mob.getAttribute(Attributes.ATTACK_DAMAGE) == null ? 0.0D : mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
                target.invulnerableTime = 0;
                target.hurt(mob.damageSources().mobAttack(mob), (float) (attack + PymConfig.STING_STORM_DAMAGE.get()));
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), mob);
                mob.swing(InteractionHand.MAIN_HAND);
                mob.playSound(SoundEvents.BEE_STING, 0.8F, 1.2F);
                mob.setDeltaMovement(toward.normalize().scale(-0.3D).add(0.0D, 0.6D, 0.0D));
                this.finish(target);
            }
        }
    }

    private static final class CommandTargetGoal extends Goal {
        private final PymColony.Member member;
        private LivingEntity chosen;
        private int cadence;

        private CommandTargetGoal(PymColony.Member member) {
            this.member = member;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (--this.cadence > 0) {
                return false;
            }
            this.cadence = 5;
            ServerPlayer owner = ready(this.member);
            if (owner == null || this.member.mob.isVehicle()) {
                return false;
            }
            this.chosen = PymColony.pickTarget(this.member, owner);
            return this.chosen != null && this.chosen != this.member.mob.getTarget();
        }

        @Override
        public void start() {
            PymColony.command(this.member, this.chosen);
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer owner = ready(this.member);
            LivingEntity target = this.member.mob.getTarget();
            if (owner == null || target == null || !PymColony.allowed(this.member, target)) {
                return false;
            }
            double leash = PymConfig.LEASH_RANGE.get();
            if (target.distanceToSqr(owner) > leash * leash) {
                return false;
            }
            if (this.member.focus == null && --this.cadence <= 0) {
                this.cadence = 10;
                LivingEntity better = PymColony.pickTarget(this.member, owner);
                if (better != null && better != target) {
                    PymColony.command(this.member, better);
                }
            }
            return true;
        }

        @Override
        public void stop() {
            LivingEntity target = this.member.mob.getTarget();
            if (target != null && !PymColony.allowed(this.member, target) || this.member.focus == null) {
                this.member.sanctioned = null;
                this.member.mob.setTarget(null);
            }
            this.chosen = null;
        }
    }
}
