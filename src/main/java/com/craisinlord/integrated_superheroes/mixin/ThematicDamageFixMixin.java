package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicFixes;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicOptions;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = 1500)
public abstract class ThematicDamageFixMixin {
    @Shadow
    protected abstract void hurtArmor(DamageSource source, float amount);

    @TargetHandler(mixin = "bond.thematic.api.mixin.core.DefenseStatMixin", name = "thematic$applyArmorToDamage")
    @ModifyVariable(method = "@MixinSquared:Handler", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private float integratedSuperheroes$vanillaArmor(float amount, DamageSource source) {
        if (source == null || source.is(DamageTypeTags.BYPASSES_ARMOR) || !ThematicOptions.on(ThematicOptions.VANILLA_ARMOR)) {
            return amount;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        this.hurtArmor(source, amount);
        return CombatRules.getDamageAfterAbsorb(self, amount, source, (float) self.getArmorValue(), (float) self.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }

    @TargetHandler(mixin = "bond.thematic.api.mixin.core.GlideMixin", name = "thematic$injectElytraTick")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$skipElytraTick(CallbackInfo outer, CallbackInfo ci) {
        if (ThematicFixes.skipElytraTick((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "bond.thematic.api.mixin.core.StatusEffectEventMixin", name = "broadcastStatusEffectToAll")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$trimEffectAdded(MobEffectInstance effect, Entity source, CallbackInfo outer, CallbackInfo ci) {
        if (!ThematicFixes.broadcastEffect(effect)) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "bond.thematic.api.mixin.core.StatusEffectEventMixin", name = "broadcastStatusEffectRemovalFirst")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$trimEffectRemoved(MobEffectInstance effect, CallbackInfo outer, CallbackInfo ci) {
        if (!ThematicFixes.broadcastEffect(effect)) {
            ci.cancel();
        }
    }
}
