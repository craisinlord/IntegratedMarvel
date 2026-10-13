package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.antarchy.content.entity.SizeRayProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

public final class AntarchySizeRay {
    private AntarchySizeRay() {
    }

    public static ResourceLocation modifierId() {
        return SizeRayProjectileEntity.SIZE_RAY_MODIFIER_ID;
    }

    public static void write(AttributeInstance scale, double offset) {
        SizeRayProjectileEntity.writeModifier(scale, offset);
    }
}
