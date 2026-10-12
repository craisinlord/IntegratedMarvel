package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = IntegratedMarvel.MODID, value = Dist.CLIENT)
public final class UniformTooltipClient {
    private static final Set<ResourceLocation> UNIFORM = Set.of(
            ResourceLocation.fromNamespaceAndPath("marvellegacy", "spiderman_bagman_chestplate"),
            ResourceLocation.fromNamespaceAndPath("marvellegacy", "spiderman_bagman_leggings"),
            ResourceLocation.fromNamespaceAndPath("marvellegacy", "spiderman_bagman_boots"));

    private UniformTooltipClient() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!UNIFORM.contains(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()))) {
            return;
        }
        List<Component> lines = event.getToolTip();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getString().contains("Bag-Man")) {
                lines.set(i, Component.literal("§e• [ Fantastic Four | E-616 ] • "));
            }
        }
    }
}
