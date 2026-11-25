package com.craisinlord.integrated_superheroes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.baranhan.viltrumitecore.entity.MeteorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MeteorEntity.class, remap = false)
public abstract class ViltrumiteMeteorGriefMixin {
    @WrapOperation(method = "onHitBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"))
    private Explosion integratedSuperheroes$blast(Level level, Entity source, double x, double y, double z, float power,
                                                  Level.ExplosionInteraction interaction, Operation<Explosion> original) {
        boolean grief = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        return original.call(level, source, x, y, z, power, grief ? interaction : Level.ExplosionInteraction.NONE);
    }

    @WrapOperation(method = "onHitBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean integratedSuperheroes$crater(Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && original.call(level, pos, state, flags);
    }
}
