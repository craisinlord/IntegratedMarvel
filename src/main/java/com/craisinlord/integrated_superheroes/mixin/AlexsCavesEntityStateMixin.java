package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.AlexsCavesMagnetCache;
import com.craisinlord.integrated_superheroes.compat.AlexsCavesMovingBlocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class AlexsCavesEntityStateMixin implements AlexsCavesMagnetCache {
    @Unique
    private long integratedSuperheroes$magnetCheckTick = Long.MIN_VALUE;
    @Unique
    private boolean integratedSuperheroes$magnetPulled;

    @Inject(method = "setLevelCallback", at = @At("TAIL"))
    private void integratedSuperheroes$trackMovingBlocks(EntityInLevelCallback callback, CallbackInfo ci) {
        AlexsCavesMovingBlocks.onLevelCallback((Entity) (Object) this, callback != EntityInLevelCallback.NULL);
    }

    @Override
    public long integratedSuperheroes$magnetCheckTick() {
        return this.integratedSuperheroes$magnetCheckTick;
    }

    @Override
    public boolean integratedSuperheroes$magnetPulled() {
        return this.integratedSuperheroes$magnetPulled;
    }

    @Override
    public void integratedSuperheroes$setMagnetCheck(long tick, boolean pulled) {
        this.integratedSuperheroes$magnetCheckTick = tick;
        this.integratedSuperheroes$magnetPulled = pulled;
    }
}
