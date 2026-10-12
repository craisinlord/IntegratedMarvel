package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.MarvelBalanceConfig;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.stxllarplanes.marvellegacy.power.wolverineattacks.WolverineAttacks", remap = false)
public abstract class LegacyWolverineDamageMixin {
    @Redirect(
            method = {"berserkerBarrage", "WolverineAttacksTick", "lambda$WolverineAttacksTick$7"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = false),
            remap = false,
            require = 0
    )
    private static boolean integratedSuperheroes$scaleClawDamage(LivingEntity target, DamageSource source, float amount) {
        float scaled = amount;
        if (source.getEntity() instanceof Player player) {
            scaled = (float) (MarvelBalanceConfig.LOGAN_CLAW_DAMAGE.get() + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        boolean[] result = new boolean[1];
        float finalDamage = scaled;
        HeroAbilities.runAsAbilityDamage(() -> result[0] = target.hurt(source, finalDamage));
        return result[0];
    }
}
