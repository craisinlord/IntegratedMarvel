package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class LegacyGauntletListeners {
    private static final List<String> DISABLED = List.of(
            "net.redgitreds.infinitygauntlet.procedures.TimeStopProcProcedure",
            "net.redgitreds.infinitygauntlet.procedures.BaseUIProcProcedure",
            "net.redgitreds.infinitygauntlet.procedures.GauntletSlottingProcedure");

    private LegacyGauntletListeners() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (String name : DISABLED) {
                try {
                    NeoForge.EVENT_BUS.unregister(Class.forName(name, false, LegacyGauntletListeners.class.getClassLoader()));
                } catch (ClassNotFoundException | IllegalArgumentException ignored) {
                }
            }
        });
    }
}
