package com.craisinlord.integrated_superheroes.compat.thematic;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public final class ThematicCompat {
    private ThematicCompat() {
    }

    public static void register(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ThematicOptions.SPEC, "integrated_superheroes-thematic.toml");
        modBus.addListener(ThematicCompat::onConfigLoad);
        modBus.addListener(ThematicCompat::onConfigReload);
        modBus.addListener(ThematicCompat::onLoadComplete);
        NeoForge.EVENT_BUS.addListener(ThematicCompat::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(ThematicCompat::onServerStarted);
        NeoForge.EVENT_BUS.addListener(ThematicCompat::onCommand);
        SuitPieces.register(modBus);
        ThematicVillains.register();
        ThematicQuantumDive.register();
        LanternCharge.register();
        LanternRings.register();
        LanternShards.register();
        ThematicScorchedGuns.register();
        BatmanActions.register();
        SithActions.register();
        DraculaActions.register();
        DoomActions.register();
        ApocalypseActions.register();
        FateActions.register();
        DarkseidActions.register();
        if (FMLEnvironment.dist.isClient()) {
            ThematicClientCompat.register(modBus);
        }
    }

    private static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ThematicOptions.SPEC) {
            ThematicBridge.applyConfig();
            ThematicLogging.apply();
        }
    }

    private static void onLoadComplete(FMLLoadCompleteEvent event) {
        ThematicLogging.apply();
    }

    private static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ThematicOptions.SPEC) {
            ThematicBridge.applyConfig();
        }
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        ThematicBridge.applyConfig();
        ThematicLogging.apply();
    }

    private static void onServerStarted(ServerStartedEvent event) {
        ThematicRules.apply(event.getServer());
    }

    private static void onCommand(CommandEvent event) {
        ThematicStarters.onCommand(event);
    }
}
