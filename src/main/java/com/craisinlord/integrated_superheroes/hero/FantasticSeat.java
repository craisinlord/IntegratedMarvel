package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FantasticSeat extends Entity {
    private static final EntityDataAccessor<Boolean> SOFA = SynchedEntityData.defineId(FantasticSeat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(FantasticSeat.class, EntityDataSerializers.INT);

    public FantasticSeat(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SOFA, false);
        builder.define(OWNER, -1);
    }

    public boolean isSofa() {
        return this.entityData.get(SOFA);
    }

    public void setSofa(boolean sofa) {
        this.entityData.set(SOFA, sofa);
    }

    public void setOwner(Player owner) {
        this.entityData.set(OWNER, owner.getId());
    }

    public boolean isOwner(Entity entity) {
        return entity.getId() == this.entityData.get(OWNER);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < 2 && !this.isOwner(passenger);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || this.isOwner(player) || !this.canAddPassenger(player)) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        Entity owner = this.level().getEntity(this.entityData.get(OWNER));
        if (!(owner instanceof Player player) || !player.isAlive()
                || !ReedShapes.has(player, this.isSofa() ? ReedShapes.SOFA_STATE : ReedShapes.BOAT_STATE)) {
            this.ejectPassengers();
            this.discard();
            return;
        }
        this.setPos(player.getX(), player.getY(), player.getZ());
        if (!this.isSofa()) {
            this.setYRot(player.getYRot());
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        int index = Math.max(0, this.getPassengers().indexOf(passenger));
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
        Vec3 offset;
        if (this.isSofa()) {
            Vec3 side = new Vec3(-forward.z, 0.0D, forward.x).scale(index == 0 ? 0.4D : -0.4D);
            offset = side.add(forward.scale(0.1D)).add(0.0D, 0.45D, 0.0D);
        } else {
            offset = forward.scale(-0.7D - 0.7D * index).add(0.0D, 0.45D, 0.0D);
        }
        Vec3 attach = passenger.getVehicleAttachmentPoint(this);
        move.accept(passenger, this.getX() + offset.x - attach.x, this.getY() + offset.y - attach.y, this.getZ() + offset.z - attach.z);
    }
}
