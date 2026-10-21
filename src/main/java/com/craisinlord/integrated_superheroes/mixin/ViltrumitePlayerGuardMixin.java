package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteFlightLedger;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class, priority = 1500)
public abstract class ViltrumitePlayerGuardMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.PlayerEntityCoreMixin", name = "onTick")
    @WrapWithCondition(method = "@MixinSquared:Handler", at = @At(value = "INVOKE",
            target = "Ldev/baranhan/viltrumitecore/util/PunchImpactManager;tickMeteorPhysics(Lnet/minecraft/server/level/ServerLevel;)V"), remap = false)
    private boolean integratedSuperheroes$meteorsOncePerTick(ServerLevel level) {
        return ViltrumiteGuards.firstThisTick(ViltrumiteGuards.METEORS, level);
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.PlayerEntityCoreMixin", name = "onTick")
    @WrapWithCondition(method = "@MixinSquared:Handler", at = @At(value = "INVOKE",
            target = "Ldev/baranhan/viltrumitecore/util/ChopImpactManager;tickBleeding(Lnet/minecraft/server/level/ServerLevel;)V"), remap = false)
    private boolean integratedSuperheroes$bleedingOncePerTick(ServerLevel level) {
        return ViltrumiteGuards.firstThisTick(ViltrumiteGuards.BLEEDING, level);
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.PlayerEntityCoreMixin", name = "onTick")
    @WrapWithCondition(method = "@MixinSquared:Handler", at = @At(value = "INVOKE",
            target = "Ldev/baranhan/viltrumitecore/util/ThunderClapManager;tickThunderclaps(Lnet/minecraft/server/level/ServerLevel;)V"), remap = false)
    private boolean integratedSuperheroes$clapsOncePerTick(ServerLevel level) {
        return ViltrumiteGuards.firstThisTick(ViltrumiteGuards.CLAPS, level);
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.PlayerEntityCoreMixin", name = "tickGrab")
    @WrapOperation(method = "@MixinSquared:Handler", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;mayfly:Z",
            opcode = Opcodes.PUTFIELD), remap = false)
    private void integratedSuperheroes$saveGrabbedFlight(Abilities abilities, boolean value, Operation<Void> original) {
        if (ViltrumiteGuards.flight()) {
            ViltrumiteGuards.ledger(abilities).integratedSuperheroes$saveBeforeGrab();
        }
        original.call(abilities, value);
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.PlayerEntityCoreMixin", name = "releaseTarget")
    @WrapOperation(method = "@MixinSquared:Handler", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;mayfly:Z",
            opcode = Opcodes.PUTFIELD), remap = false)
    private void integratedSuperheroes$restoreGrabbedMayfly(Abilities abilities, boolean value, Operation<Void> original) {
        ViltrumiteFlightLedger ledger = ViltrumiteGuards.ledger(abilities);
        original.call(abilities, ViltrumiteGuards.flight() && ledger.integratedSuperheroes$hasGrabSave() ? ledger.integratedSuperheroes$grabSavedMayfly() : value);
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.PlayerEntityCoreMixin", name = "releaseTarget")
    @WrapOperation(method = "@MixinSquared:Handler", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z",
            opcode = Opcodes.PUTFIELD), remap = false)
    private void integratedSuperheroes$restoreGrabbedFlying(Abilities abilities, boolean value, Operation<Void> original) {
        ViltrumiteFlightLedger ledger = ViltrumiteGuards.ledger(abilities);
        if (!ViltrumiteGuards.flight() || !ledger.integratedSuperheroes$hasGrabSave()) {
            original.call(abilities, value);
            return;
        }
        original.call(abilities, abilities.mayfly && ledger.integratedSuperheroes$grabSavedFlying());
        ledger.integratedSuperheroes$clearGrabSave();
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.PlayerStatsMixin", name = "onTick")
    @WrapOperation(method = "@MixinSquared:Handler", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;mayfly:Z",
            opcode = Opcodes.PUTFIELD), remap = false)
    private void integratedSuperheroes$markViltrumiteFlight(Abilities abilities, boolean value, Operation<Void> original) {
        original.call(abilities, value);
        if (value) {
            ViltrumiteGuards.ledger(abilities).integratedSuperheroes$setViltrumiteOwned(true);
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.PlayerStatsMixin", name = "ignoreWeakDamage")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$bypassIgnore(DamageSource source, float amount, CallbackInfoReturnable<Boolean> original, CallbackInfo ci) {
        if (ViltrumiteGuards.bypassesReduction(source)) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.PlayerStatsMixin", name = "reduceIncomingDamage")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$bypassReduction(float amount, DamageSource source, CallbackInfoReturnable<Float> cir) {
        if (ViltrumiteGuards.bypassesReduction(source)) {
            cir.setReturnValue(amount);
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.BlockDamageMixin", name = "applyViltrumiteBlock")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$bypassBlock(float amount, DamageSource source, CallbackInfoReturnable<Float> cir) {
        if (ViltrumiteGuards.bypassesBlock(source)) {
            cir.setReturnValue(amount);
        }
    }
}
