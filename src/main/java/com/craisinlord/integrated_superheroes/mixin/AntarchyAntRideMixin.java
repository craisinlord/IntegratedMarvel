package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.antarchy.content.entity.ant.BaseAntEntity;
import com.craisinlord.integrated_superheroes.suit.PymConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BaseAntEntity.class)
public abstract class AntarchyAntRideMixin extends Animal {
    protected AntarchyAntRideMixin(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        if (this.getFirstPassenger() instanceof Player player) {
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        this.setRot(player.getYRot(), player.getXRot() * 0.5F);
        this.yRotO = this.getYRot();
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
        if (this.horizontalCollision && this.onGround() && input.lengthSqr() > 0.0D) {
            this.jumpFromGround();
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        float forward = player.zza;
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }
        return new Vec3(player.xxa * 0.5F, 0.0D, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * PymConfig.ANTHONY_RIDE_SPEED.get());
    }
}
