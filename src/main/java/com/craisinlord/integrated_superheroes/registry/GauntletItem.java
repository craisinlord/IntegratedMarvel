package com.craisinlord.integrated_superheroes.registry;

import com.craisinlord.integrated_superheroes.gauntlet.InfinityGauntletStones;
import com.craisinlord.integrated_superheroes.gauntlet.GauntletAbilityController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public abstract class GauntletItem extends Item {
    private static final int HOLD_DURATION = 72000;

    protected GauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (InfinityGauntletStones.equippedGauntlet(player) != stack
                || InfinityGauntletStones.installedStoneKeys(stack).isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }
        if (GauntletAbilityController.usesHeldActivation(stack)) {
            if (player instanceof ServerPlayer serverPlayer) {
                GauntletAbilityController.handleInput(serverPlayer, GauntletAbilityController.ACTIVATE_START, 0.0D);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            GauntletAbilityController.activate(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (entity instanceof ServerPlayer serverPlayer) {
            GauntletAbilityController.handleInput(serverPlayer, GauntletAbilityController.ACTIVATE_RELEASE, 0.0D);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return HOLD_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }
}
