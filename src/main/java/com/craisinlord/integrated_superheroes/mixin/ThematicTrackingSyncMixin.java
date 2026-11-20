package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.component.core.SyncRecipient;
import bond.thematic.api.content.entity.body.OwnerCopyEntity;
import bond.thematic.api.util.ThematicHelper;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.component.core.ComponentSync", remap = false)
public abstract class ThematicTrackingSyncMixin {
    @Inject(method = "syncAll", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$skipPlainMobs(SyncRecipient recipient, Object holder, CallbackInfo ci) {
        if (!ThematicOptions.on(ThematicOptions.TRIM_MOB_SYNC) || !(holder instanceof LivingEntity living)
                || living instanceof Player || living instanceof OwnerCopyEntity) {
            return;
        }
        if ("thematic".equals(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).getNamespace())) {
            return;
        }
        if (ThematicHelper.getArmor(living) == null) {
            ci.cancel();
        }
    }
}
