package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FantasticSeat extends Boat {
    private static final EntityDataAccessor<Boolean> SOFA = SynchedEntityData.defineId(FantasticSeat.class, EntityDataSerializers.BOOLEAN);

    public FantasticSeat(EntityType<? extends Boat> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SOFA, false);
    }

    public boolean isSofa() {
        return this.entityData.get(SOFA);
    }

    public void setSofa(boolean sofa) {
        this.entityData.set(SOFA, sofa);
    }

    @Override
    public Item getDropItem() {
        return Items.AIR;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return this.isSofa() ? null : super.getControllingPassenger();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && (!(this.getFirstPassenger() instanceof Player player) || !MrFantasticPower.isReed(player))) {
            this.ejectPassengers();
            this.discard();
            return;
        }
        Vec3 motion = this.getDeltaMovement();
        if (this.isSofa()) {
            this.setDeltaMovement(0.0D, motion.y, 0.0D);
        } else if (this.isUnderWater()) {
            this.setDeltaMovement(motion.x, Math.max(motion.y, 0.12D), motion.z);
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        Entity first = this.getFirstPassenger();
        if (this.isSofa() && first != null && passenger != first) {
            Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot()).scale(0.35D);
            move.accept(passenger, first.getX() + forward.x, first.getY() + 0.55D, first.getZ() + forward.z);
            return;
        }
        super.positionRider(passenger, move);
    }
}
