package com.craisinlord.integrated_superheroes.voidworld;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddPackFindersEvent;

public final class VoidPalettePack {
    private VoidPalettePack() {
    }

    static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || !ModList.get().isLoaded("antarchy")) {
            return;
        }
        event.addPackFinders(IntegratedSuperheroes.id("resourcepacks/void_antarchy"), PackType.SERVER_DATA,
                Component.literal("Void palette"), PackSource.BUILT_IN, true, Pack.Position.TOP);
    }
}
