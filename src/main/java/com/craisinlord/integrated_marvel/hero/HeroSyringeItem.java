package com.craisinlord.integrated_marvel.hero;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

public class HeroSyringeItem extends Item {
    private final String heroId;

    public HeroSyringeItem(Properties properties, String heroId) {
        super(properties);
        this.heroId = heroId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseTicks) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (this.getUseDuration(stack, entity) - remainingUseTicks < 20 || player.getMainHandItem() != stack) {
            return;
        }
        HeroPower hero = HeroPowers.get(this.heroId);
        if (hero != null) {
            HeroPowers.inject(player, hero);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        HeroPower hero = HeroPowers.get(this.heroId);
        if (hero != null) {
            tooltip.add(Component.literal("§e" + hero.tooltip()));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
