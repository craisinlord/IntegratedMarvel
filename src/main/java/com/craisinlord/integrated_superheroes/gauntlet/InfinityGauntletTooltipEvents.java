package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class InfinityGauntletTooltipEvents {
    private InfinityGauntletTooltipEvents() {
    }

    @SubscribeEvent
    public static void addTooltips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        if (InfinityGauntletStones.isGauntlet(stack)) {
            tooltip.add(Component.translatable("tooltip.integrated_superheroes.gauntlet.install")
                    .withStyle(Style.EMPTY.withColor(0xBBBBBB)));
            tooltip.add(Component.translatable("tooltip.integrated_superheroes.gauntlet.use")
                    .withStyle(Style.EMPTY.withColor(0xBBBBBB)));
            if (GauntletAbilities.infinity(stack)) {
                tooltip.add(Component.translatable("tooltip.integrated_superheroes.gauntlet.infinity_tier")
                        .withStyle(Style.EMPTY.withColor(0xFFD700).withBold(true)));
            }
            if (Minecraft.getInstance().level != null && SnapStone.isScorched(stack, Minecraft.getInstance().level.getGameTime())) {
                tooltip.add(Component.translatable("tooltip.integrated_superheroes.gauntlet.scorched")
                        .withStyle(Style.EMPTY.withColor(0x777777).withItalic(true)));
            }
            return;
        }
        InfinityGauntletStones.stoneKey(stack).ifPresent(stone -> {
            tooltip.add(Component.translatable("tooltip.integrated_superheroes.gauntlet.installed_stone")
                    .withStyle(Style.EMPTY.withColor(0x888888)));
            for (GauntletAbility ability : GauntletAbilities.forStone(stone)) {
                Component line = Component.literal(GauntletAbilities.numeral(ability.index()) + " ")
                        .withStyle(Style.EMPTY.withColor(GauntletAbilities.numeralColor(ability.index())).withBold(true))
                        .append(Component.translatable("tooltip.integrated_superheroes.gauntlet.ability." + ability.translationSuffix())
                                .withStyle(Style.EMPTY.withColor(0xFFFFFF).withBold(true)))
                        .append(Component.literal(" — ").withStyle(Style.EMPTY.withColor(0x888888)))
                        .append(Component.translatable("tooltip.integrated_superheroes.gauntlet.description." + ability.translationSuffix())
                                .withStyle(Style.EMPTY.withColor(0xBBBBBB)));
                tooltip.add(line);
            }
        });
    }
}
