package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.cleanup.Cleanup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(TagLoader.class)
public abstract class TagLoaderCleanupMixin {
    @Shadow
    @Final
    private String directory;

    private static final ResourceLocation RANDOMIUM_BLACKLIST = ResourceLocation.fromNamespaceAndPath("randomium", "randomium_blacklist");

    @Inject(method = "load", at = @At("RETURN"))
    private void integratedSuperheroes$dropRemovedItems(CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {
        if (!"tags/item".equals(this.directory)) {
            return;
        }
        Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags = cir.getReturnValue();
        List<TagLoader.EntryWithSource> randomium = tags.get(RANDOMIUM_BLACKLIST);
        for (List<TagLoader.EntryWithSource> entries : tags.values()) {
            if (entries == randomium) {
                continue;
            }
            List<TagLoader.EntryWithSource> removals = new ArrayList<>();
            for (TagLoader.EntryWithSource entry : entries) {
                if (!entry.remove() && !entry.entry().isTag() && Cleanup.isGone(entry.entry().getId())) {
                    removals.add(new TagLoader.EntryWithSource(TagEntry.optionalElement(entry.entry().getId()), "integrated_superheroes", true));
                }
            }
            entries.addAll(removals);
        }
        if (!net.neoforged.fml.ModList.get().isLoaded("randomium")) {
            return;
        }
        if (randomium == null) {
            randomium = new ArrayList<>();
            tags.put(RANDOMIUM_BLACKLIST, randomium);
        }
        for (ResourceLocation id : Cleanup.randomExclusions()) {
            randomium.add(new TagLoader.EntryWithSource(TagEntry.optionalElement(id), "integrated_superheroes", false));
        }
    }
}
