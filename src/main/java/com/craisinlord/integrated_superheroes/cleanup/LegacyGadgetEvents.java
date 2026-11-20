package com.craisinlord.integrated_superheroes.cleanup;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID)
public final class LegacyGadgetEvents {
    private static final ResourceLocation BENCH = ResourceLocation.parse("thematic:gadget_bench");

    private LegacyGadgetEvents() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!CatalogCleanup.retiredLegacyGadget(BENCH)) return;
        boolean holdingBench = BENCH.equals(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()));
        boolean usingBench = BENCH.equals(BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock()));
        if (!holdingBench && !usingBench) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!event.getLevel().isClientSide()) {
            event.getEntity().displayClientMessage(Component.literal("The old Gadget Bench is retired. Use your suit's abilities."), true);
        }
    }
}
