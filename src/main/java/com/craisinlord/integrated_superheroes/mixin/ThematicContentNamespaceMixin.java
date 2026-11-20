package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.thematic.ThematicFixes;
import com.google.gson.JsonObject;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "bond.thematic.api.content.registry.RegisteredContentType", remap = false)
public abstract class ThematicContentNamespaceMixin {
    @Shadow(remap = false)
    @Final
    private String folder;

    @ModifyArg(method = "accept", remap = false, index = 0, at = @At(value = "INVOKE", remap = false,
            target = "Lbond/thematic/api/content/registry/RegisteredContentType;resolveParents(Ljava/util/Map;Ljava/util/List;)Ljava/util/Map;"))
    private Map<ResourceLocation, JsonObject> integratedSuperheroes$ownStartupContent(Map<ResourceLocation, JsonObject> found) {
        return com.craisinlord.integrated_superheroes.cleanup.CosmeticMerges.applySuitDefinitions(
                ThematicFixes.ownContent(found, this.folder, true), this.folder);
    }

    @ModifyArg(method = "reloaded", remap = false, index = 0, at = @At(value = "INVOKE", remap = false,
            target = "Lbond/thematic/api/content/registry/RegisteredContentType;resolveParents(Ljava/util/Map;Ljava/util/List;)Ljava/util/Map;"))
    private Map<ResourceLocation, JsonObject> integratedSuperheroes$ownReloadedContent(Map<ResourceLocation, JsonObject> found) {
        return com.craisinlord.integrated_superheroes.cleanup.CosmeticMerges.applySuitDefinitions(
                ThematicFixes.ownContent(found, this.folder, false), this.folder);
    }
}
