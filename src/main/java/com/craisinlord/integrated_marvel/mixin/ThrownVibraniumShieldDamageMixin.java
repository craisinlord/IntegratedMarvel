package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.MarvelWeaponConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.tintankgames.marvel.world.entity.projectile.ThrownVibraniumShield;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.tintankgames.marvel.world.entity.projectile.ThrownVibraniumShield", remap = false)
public abstract class ThrownVibraniumShieldDamageMixin {
    private static final ResourceLocation VIBRANIUM_SHIELD_ID = ResourceLocation.parse("marvel:vibranium_shield");
    private static final ResourceLocation PROTO_ADAMANTIUM_SHIELD_ID = ResourceLocation.parse("marvel:proto_adamantium_shield");

    @Redirect(
            method = "onHitEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/tintankgames/marvel/world/entity/projectile/ThrownVibraniumShield;getBaseDamage()D",
                    remap = false
            ),
            remap = false
    )
    private double integratedMarvel$setThrownShieldDamage(ThrownVibraniumShield projectile) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(projectile.getWeaponItem().getItem());
        if (VIBRANIUM_SHIELD_ID.equals(itemId)) return MarvelWeaponConfig.or(MarvelWeaponConfig.VIBRANIUM_SHIELD_DAMAGE, projectile.getBaseDamage());
        if (PROTO_ADAMANTIUM_SHIELD_ID.equals(itemId)) return MarvelWeaponConfig.or(MarvelWeaponConfig.PROTO_ADAMANTIUM_SHIELD_DAMAGE, projectile.getBaseDamage());
        return projectile.getBaseDamage();
    }
}
