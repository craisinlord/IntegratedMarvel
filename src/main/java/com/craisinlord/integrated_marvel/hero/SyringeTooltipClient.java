package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = IntegratedMarvel.MODID, value = Dist.CLIENT)
public final class SyringeTooltipClient {
    private SyringeTooltipClient() {
    }

    public static String key(ResourceLocation id) {
        return "tooltip." + id.getNamespace() + "." + id.getPath();
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!("marvellegacy".equals(id.getNamespace()) || com.craisinlord.integrated_marvel.IntegratedMarvel.MODID.equals(id.getNamespace())) || !id.getPath().startsWith("syringe_") || "syringe_empty".equals(id.getPath())) {
            return;
        }
        String key = key(id);
        if (!Language.getInstance().has(key)) {
            return;
        }
        List<Component> lines = event.getToolTip();
        lines.removeIf(line -> {
            String text = line.getString();
            return text.startsWith("§e") && text.contains("(") || line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable
                    && translatable.getKey().equals(key);
        });
        lines.add(Math.min(1, lines.size()), Component.translatable(key).withStyle(ChatFormatting.YELLOW));
    }
}
