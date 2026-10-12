package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.compat.AlexsCavesMagnetCache;
import com.craisinlord.integrated_marvel.compat.AlexsCavesMovingBlocks;
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
    private long integratedMarvel$magnetCheckTick = Long.MIN_VALUE;
    @Unique
    private boolean integratedMarvel$magnetPulled;

    @Inject(method = "setLevelCallback", at = @At("TAIL"))
    private void integratedMarvel$trackMovingBlocks(EntityInLevelCallback callback, CallbackInfo ci) {
        AlexsCavesMovingBlocks.onLevelCallback((Entity) (Object) this, callback != EntityInLevelCallback.NULL);
    }

    @Override
    public long integratedMarvel$magnetCheckTick() {
        return this.integratedMarvel$magnetCheckTick;
    }

    @Override
    public boolean integratedMarvel$magnetPulled() {
        return this.integratedMarvel$magnetPulled;
    }

    @Override
    public void integratedMarvel$setMagnetCheck(long tick, boolean pulled) {
        this.integratedMarvel$magnetCheckTick = tick;
        this.integratedMarvel$magnetPulled = pulled;
    }
}
