package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.content.entity.body.MobSuitEquipper;
import bond.thematic.api.registries.armors.armor.ArmorRegistry;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicVariants;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MobSuitEquipper.class, remap = false)
public abstract class ThematicMobSuitRefusalMixin {
    @WrapOperation(method = "equip", at = @At(value = "INVOKE",
            target = "Lbond/thematic/api/content/entity/body/MobSuitEquipper;refusalFor(Ljava/lang/String;)Ljava/lang/String;"), remap = false)
    private static String integratedSuperheroes$variantSuits(String suitId, Operation<String> original, @Local(argsOnly = true) Mob mob) {
        String refusal = original.call(suitId);
        if (refusal != null && ThematicVariants.isHost(mob) && ArmorRegistry.getArmor(suitId) != null) {
            return null;
        }
        return refusal;
    }
}
