package com.craisinlord.integrated_superheroes.mixin;

import com.google.common.collect.Table;
import net.minecraft.world.level.timers.TimerQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TimerQueue.class)
public interface TimerQueueAccessor<T> {
    @Accessor("events")
    Table<String, Long, TimerQueue.Event<T>> integratedSuperheroes$events();
}
