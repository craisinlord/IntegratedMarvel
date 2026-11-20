package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.util.ThematicHelper;
import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicFixes;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicOptions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Player.class, priority = 1500)
public abstract class ThematicPlayerFixMixin {
    @Unique
    private Entity integratedSuperheroes$checkedTarget;

    @TargetHandler(mixin = "bond.thematic.api.mixin.core.PlayerEntityMixin", name = "thematic$overrideAttack")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$attackEvent(Entity target, CallbackInfo outer, CallbackInfo ci) {
        this.integratedSuperheroes$checkedTarget = null;
        Player player = (Player) (Object) this;
        if (target == null || !ThematicOptions.on(ThematicOptions.ATTACK_EVENTS)
                || ThematicHelper.getArmor(player) == null || ThematicHelper.getArmorStack(player) == null) {
            return;
        }
        if (!CommonHooks.onPlayerAttackTarget(player, target)) {
            outer.cancel();
            ci.cancel();
            return;
        }
        this.integratedSuperheroes$checkedTarget = target;
    }

    @TargetHandler(mixin = "bond.thematic.api.mixin.core.PlayerEntityMixin", name = "thematic$overrideAttack")
    @Inject(method = "@MixinSquared:Handler", at = @At("RETURN"), remap = false)
    private void integratedSuperheroes$attackHandled(Entity target, CallbackInfo outer, CallbackInfo ci) {
        if (outer.isCancelled()) {
            this.integratedSuperheroes$checkedTarget = null;
        }
    }

    @WrapOperation(method = "attack", remap = false, at = @At(value = "INVOKE", remap = false,
            target = "Lnet/neoforged/neoforge/common/CommonHooks;onPlayerAttackTarget(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean integratedSuperheroes$attackEventOnce(Player player, Entity target, Operation<Boolean> original) {
        Entity checked = this.integratedSuperheroes$checkedTarget;
        this.integratedSuperheroes$checkedTarget = null;
        if (checked != null && checked == target) {
            return true;
        }
        return original.call(player, target);
    }

    @TargetHandler(mixin = "bond.thematic.api.mixin.core.PlayerEntityMixin", name = "thematic$damageCustomShield")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$otherShields(float amount, CallbackInfo outer, CallbackInfo ci) {
        if (ThematicOptions.on(ThematicOptions.OTHER_SHIELDS) && !ThematicFixes.thematicItem(((Player) (Object) this).getUseItem())) {
            ci.cancel();
        }
    }
}
