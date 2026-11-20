package com.craisinlord.integrated_superheroes.boss.command.client;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.boss.command.StormCommandBlocks;
import com.craisinlord.integrated_superheroes.boss.command.StormCommandOpenPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class StormCommandBlockClient {
    public static final int TINT = 0xFF9A5BFF;

    private StormCommandBlockClient() {
    }

    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? TINT : -1, StormCommandBlocks.BLOCK.get());
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? TINT : -1, StormCommandBlocks.ITEM.get());
    }

    public static void open(StormCommandOpenPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        minecraft.setScreen(new StormCommandBlockScreen(payload));
    }
}
