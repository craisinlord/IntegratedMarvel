package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.craisinlord.integrated_marvel.gauntlet.InfinityGauntletStones;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = IntegratedMarvel.MODID, value = Dist.CLIENT)
public final class InfinityGauntletTooltipEvents {
    private static final int[] ABILITY_COLORS = {0x55FFFF, 0xAA55FF, 0xFFAA00};

    private InfinityGauntletTooltipEvents() {
    }

    @SubscribeEvent
    public static void addTooltips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (InfinityGauntletStones.isGauntlet(stack)) {
            event.getToolTip().add(Component.translatable("tooltip.integrated_marvel.gauntlet.install")
                    .withStyle(Style.EMPTY.withColor(0xBBBBBB)));
            event.getToolTip().add(Component.translatable("tooltip.integrated_marvel.gauntlet.use")
                    .withStyle(Style.EMPTY.withColor(0xBBBBBB)));
            return;
        }
        InfinityGauntletStones.stoneKey(stack).ifPresent(stone -> {
            event.getToolTip().add(Component.translatable("tooltip.integrated_marvel.gauntlet.installed_stone")
                    .withStyle(Style.EMPTY.withColor(0x888888)));
            int abilityCount = stone.equals("power") || stone.equals("space") ? 2 : 3;
            for (int ability = 1; ability <= abilityCount; ability++) {
                Component line = Component.literal(roman(ability) + " ")
                        .withStyle(Style.EMPTY.withColor(ABILITY_COLORS[ability - 1]).withBold(true))
                        .append(Component.translatable("tooltip.integrated_marvel.gauntlet.ability." + stone + "." + ability)
                                .withStyle(Style.EMPTY.withColor(0xFFFFFF).withBold(true)))
                        .append(Component.literal(" — ").withStyle(Style.EMPTY.withColor(0x888888)))
                        .append(Component.translatable("tooltip.integrated_marvel.gauntlet.description." + stone + "." + ability)
                                .withStyle(Style.EMPTY.withColor(0xBBBBBB)));
                event.getToolTip().add(line);
            }
        });
    }

    private static String roman(int ability) {
        return switch (ability) {
            case 1 -> "I";
            case 2 -> "II";
            default -> "III";
        };
    }
}
