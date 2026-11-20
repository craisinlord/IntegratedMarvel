package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(targets = "bond.thematic.api.content.trait.ItemTraits", remap = false)
public abstract class ThematicItemTraitsMixin {
    @Unique
    private static final Map<ResourceLocation, String> integratedSuperheroes$IDS = new ConcurrentHashMap<>();

    @Redirect(method = "has(Lnet/minecraft/world/item/ItemStack;Ljava/lang/String;)Z", remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;toString()Ljava/lang/String;", remap = false))
    private static String integratedSuperheroes$cachedId(ResourceLocation id) {
        return integratedSuperheroes$IDS.computeIfAbsent(id, ResourceLocation::toString);
    }
}
