package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.cleanup.Cleanup;
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

    @Inject(method = "load", at = @At("RETURN"))
    private void integratedMarvel$dropRemovedItems(CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {
        if (!"tags/item".equals(this.directory)) {
            return;
        }
        for (List<TagLoader.EntryWithSource> entries : cir.getReturnValue().values()) {
            List<TagLoader.EntryWithSource> removals = new ArrayList<>();
            for (TagLoader.EntryWithSource entry : entries) {
                if (!entry.remove() && !entry.entry().isTag() && Cleanup.isGone(entry.entry().getId())) {
                    removals.add(new TagLoader.EntryWithSource(TagEntry.optionalElement(entry.entry().getId()), "integrated_marvel", true));
                }
            }
            entries.addAll(removals);
        }
    }
}
