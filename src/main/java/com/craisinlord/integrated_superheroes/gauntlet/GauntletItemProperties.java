package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.gauntlet.InfinityGauntletStones;
import com.craisinlord.integrated_superheroes.registry.GauntletContent;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GauntletItemProperties {
    private GauntletItemProperties() {
    }

    @SubscribeEvent
    public static void register(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation property = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "installed_stones");
            ItemProperties.register(GauntletContent.INFINITY_GAUNTLET.get(), property,
                    (stack, level, entity, seed) -> InfinityGauntletStones.installedStoneModelIndex(stack));
            ItemProperties.register(GauntletContent.NANO_GAUNTLET.get(), property,
                    (stack, level, entity, seed) -> InfinityGauntletStones.installedStoneModelIndex(stack));
        });
    }
}
