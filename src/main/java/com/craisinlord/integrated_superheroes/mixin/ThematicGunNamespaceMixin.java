package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.thematic.ThematicFixes;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.content.gun.GunContent", remap = false)
public abstract class ThematicGunNamespaceMixin {
    @Inject(method = "read", remap = false, cancellable = true, at = @At("HEAD"))
    private static void integratedSuperheroes$skipForeignGuns(ResourceLocation id, Path file, String kind, String folder, Map<ResourceLocation, ?> out, CallbackInfo ci) {
        if (ThematicFixes.foreignGunFile(id)) {
            ci.cancel();
        }
    }

    @Inject(method = "discoverAndRegister", remap = false, at = @At("RETURN"))
    private static void integratedSuperheroes$reportForeignGuns(CallbackInfo ci) {
        ThematicFixes.reportGunFiles();
    }
}
