package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.AlexsCavesMagnetCache;
import com.craisinlord.integrated_superheroes.compat.AlexsCavesMovingBlocks;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.util.MagnetUtil", remap = false)
public abstract class AlexsCavesMagnetUtilMixin {
    @WrapMethod(method = "getMovingBlockCollisions", remap = false)
    private static List<VoxelShape> integratedSuperheroes$skipWithoutMovingBlocks(Entity entity, AABB box, Operation<List<VoxelShape>> original) {
        if (!AlexsCavesMovingBlocks.anyExist()) {
            return List.of();
        }
        return original.call(entity, box);
    }

    @WrapMethod(method = "isPulledByMagnets", remap = false)
    private static boolean integratedSuperheroes$cachePerTick(Entity entity, Operation<Boolean> original) {
        if (!(entity instanceof LivingEntity) || entity.level() == null) {
            return original.call(entity);
        }
        AlexsCavesMagnetCache cache = (AlexsCavesMagnetCache) entity;
        long tick = entity.level().getGameTime();
        if (cache.integratedSuperheroes$magnetCheckTick() == tick) {
            return cache.integratedSuperheroes$magnetPulled();
        }
        boolean pulled = original.call(entity);
        cache.integratedSuperheroes$setMagnetCheck(tick, pulled);
        return pulled;
    }
}
