package com.craisinlord.integrated_superheroes.voidrealm.variant;

import com.craisinlord.integrated_superheroes.portal.MarvelPortal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class VariantEntity extends Monster {
    private static final EntityDataAccessor<Integer> PRIMARY = SynchedEntityData.defineId(VariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SECONDARY = SynchedEntityData.defineId(VariantEntity.class, EntityDataSerializers.INT);
    private boolean rolled;

    public VariantEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PRIMARY, -1);
        builder.define(SECONDARY, -1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.rolled && this.level() instanceof net.minecraft.server.level.ServerLevel server && !this.isRemoved()) {
            this.rolled = true;
            if (VariantProfile.of(this) == null) {
                int tier = com.craisinlord.integrated_superheroes.voidworld.VoidTier.at(server, this.blockPosition());
                if (MarvelPortal.enabled() && com.craisinlord.integrated_superheroes.voidworld.VoidWorld.isVoid(server)
                        ? VariantTears.arrive(server, position(), tier, null)
                        : VoidVariants.spawn(server, this.blockPosition(), tier) != null) {
                    this.discard();
                }
            }
        }
    }

    public void setColors(int primary, int secondary) {
        this.entityData.set(PRIMARY, primary);
        this.entityData.set(SECONDARY, secondary);
    }

    public int primaryColor() {
        return this.entityData.get(PRIMARY);
    }

    public int secondaryColor() {
        return this.entityData.get(SECONDARY);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VariantPrimary", primaryColor());
        tag.putInt("VariantSecondary", secondaryColor());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("VariantPrimary")) {
            setColors(tag.getInt("VariantPrimary"), tag.getInt("VariantSecondary"));
        }
    }
}
