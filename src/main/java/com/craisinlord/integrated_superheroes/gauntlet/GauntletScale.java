package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.AntarchySizeRay;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class GauntletScale {
    private static final ResourceLocation OWN_MODIFIER_ID = IntegratedSuperheroes.id("reality_scale");

    private GauntletScale() {
    }

    public static ResourceLocation modifierId() {
        return ModCompat.ANTARCHY ? AntarchySizeRay.modifierId() : OWN_MODIFIER_ID;
    }

    public static boolean has(AttributeInstance scale) {
        return scale.getModifier(modifierId()) != null;
    }

    public static double offset(AttributeInstance scale) {
        AttributeModifier existing = scale.getModifier(modifierId());
        return existing == null ? 0.0D : existing.amount();
    }

    public static void write(AttributeInstance scale, double offset) {
        if (ModCompat.ANTARCHY) {
            AntarchySizeRay.write(scale, offset);
            return;
        }
        scale.removeModifier(OWN_MODIFIER_ID);
        if (offset != 0.0D) {
            scale.addPermanentModifier(new AttributeModifier(OWN_MODIFIER_ID, offset, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
