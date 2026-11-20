package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.component.ComponentKey;
import bond.thematic.api.content.entity.body.OwnerCopyEntity;
import bond.thematic.api.util.ThematicHelper;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicOptions;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "bond.thematic.api.registries.data.bars.SpeedComponent", remap = false)
public abstract class ThematicSpeedSyncMixin {
    @Shadow(remap = false)
    @Final
    private LivingEntity provider;

    @WrapWithCondition(method = "serverTick", remap = false, at = @At(value = "INVOKE", remap = false,
            target = "Lbond/thematic/api/registries/data/sync/ComponentSyncQueue;request(Lbond/thematic/api/component/ComponentKey;Ljava/lang/Object;)V"))
    private boolean integratedSuperheroes$syncOnlyRelevant(ComponentKey<?> key, Object holder) {
        if (!ThematicOptions.on(ThematicOptions.TRIM_MOB_SYNC)) {
            return true;
        }
        return this.provider instanceof Player || this.provider instanceof OwnerCopyEntity || ThematicHelper.getArmor(this.provider) != null;
    }
}
