package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.anthology.portal.PortalEntity;
import com.craisinlord.anthology.portal.PortalShape;
import com.craisinlord.integrated_superheroes.portal.MarvelPortal;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.craisinlord.anthology.portal.client.PortalViewRenderer", remap = false)
abstract class MarvelPortalApertureMixin {
    @Inject(method = "apertureOutline", at = @At("HEAD"), cancellable = true, require = 0)
    private static void integratedSuperheroes$shape(PortalEntity portal, float partialTick,
                                                    CallbackInfoReturnable<Vec3[]> callback) {
        if (!(portal instanceof MarvelPortal marvel)) {
            return;
        }
        PortalShape shape = marvel.getWorldPortalShape();
        double radius = MarvelPortal.APERTURE_RADIUS * marvel.getPortalVisualScale(partialTick);
        int segments = marvel.style() == MarvelPortal.SPIDERVERSE ? 6 : 40;
        callback.setReturnValue(shape.getOutline(0.02, segments, radius, radius));
    }
}
