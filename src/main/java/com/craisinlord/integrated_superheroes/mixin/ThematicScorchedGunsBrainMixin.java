package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.content.criminal.DataCriminalEntity;
import bond.thematic.api.content.criminal.GunnerBrain;
import bond.thematic.api.content.criminal.GunnerStats;
import bond.thematic.api.util.ThematicFactions;
import com.craisinlord.integrated_superheroes.compat.ScorchedGunsVillains;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicScorchedGuns;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunnerBrain.class, remap = false)
public abstract class ThematicScorchedGunsBrainMixin {
    @Shadow
    @Final
    private DataCriminalEntity entity;

    @Unique
    private long integratedSuperheroes$nextShot;

    @Unique
    private ItemStack integratedSuperheroes$prepared;

    @Inject(method = "fireVolleyAt", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$fireScorchedGun(GunnerStats stats, LivingEntity target, float spread, CallbackInfo ci) {
        ItemStack gun = entity.getEquippedGun();
        if (!ThematicScorchedGuns.isGun(gun)) {
            return;
        }
        ci.cancel();
        long now = entity.level().getGameTime();
        if (now < integratedSuperheroes$nextShot) {
            return;
        }
        if (stats.aoeSafetyRadius() > 0.0D && !ThematicFactions.isAoeLaneClear(entity, target.position(), stats.aoeSafetyRadius())) {
            return;
        }
        if (integratedSuperheroes$prepared != gun) {
            ScorchedGunsVillains.prepareGunner(gun, entity.getPersistentData(), 2);
            integratedSuperheroes$prepared = gun;
        }
        integratedSuperheroes$nextShot = now + ScorchedGunsVillains.fireDelay(gun);
        ThematicScorchedGuns.fire(entity, gun, target);
    }
}
