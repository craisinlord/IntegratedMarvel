package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.craisinlord.integrated_marvel.gauntlet.InfinityGauntletStones;
import com.craisinlord.integrated_marvel.registry.GauntletContent;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = IntegratedMarvel.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GauntletItemProperties {
    private GauntletItemProperties() {
    }

    @SubscribeEvent
    public static void register(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation property = ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "installed_stones");
            ItemProperties.register(GauntletContent.INFINITY_GAUNTLET.get(), property,
                    (stack, level, entity, seed) -> InfinityGauntletStones.installedStoneModelIndex(stack));
            ItemProperties.register(GauntletContent.NANO_GAUNTLET.get(), property,
                    (stack, level, entity, seed) -> InfinityGauntletStones.installedStoneModelIndex(stack));
        });
    }
}
