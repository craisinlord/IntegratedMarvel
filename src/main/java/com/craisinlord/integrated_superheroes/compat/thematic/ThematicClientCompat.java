package com.craisinlord.integrated_superheroes.compat.thematic;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.EyeBeamClient;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.SeparateTransformsModel;

final class ThematicClientCompat {
    private static final ResourceLocation LEGACY_SEPARATE_TRANSFORMS = ResourceLocation.fromNamespaceAndPath("forge", "separate_transforms");

    private ThematicClientCompat() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener(ThematicClientCompat::onRegisterGeometryLoaders);
        modBus.addListener(ThematicClientCompat::onClientSetup);
        modBus.addListener(SuitPieceIcons::onRegisterExtensions);
        EyeBeamClient.setDrawer(ThematicEyeBeams::draw);
        ThematicAbilityKeys.register();
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ThematicAbilityHud::init);
    }

    private static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        if (!ThematicOptions.on(ThematicOptions.LEGACY_GUN_MODELS)) {
            return;
        }
        try {
            event.register(LEGACY_SEPARATE_TRANSFORMS, SeparateTransformsModel.Loader.INSTANCE);
        } catch (IllegalArgumentException e) {
            IntegratedSuperheroes.LOGGER.debug("{} is already registered", LEGACY_SEPARATE_TRANSFORMS);
        }
    }
}
