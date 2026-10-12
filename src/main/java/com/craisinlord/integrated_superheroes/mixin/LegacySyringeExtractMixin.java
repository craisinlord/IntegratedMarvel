package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.HeroPower;
import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.stxllarplanes.marvellegacy.item.Syringes$Syringe", remap = false)
public abstract class LegacySyringeExtractMixin {
    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$extractHeroPower(ItemStack stack, Level level, LivingEntity entity, int remainingUseTicks, CallbackInfo ci) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (((Item) (Object) this).getUseDuration(stack, entity) - remainingUseTicks < 20 || player.getMainHandItem() != stack) {
            return;
        }
        HeroPower hero = HeroPowers.activeHero(player);
        if (hero != null) {
            HeroPowers.extract(player, hero);
            ci.cancel();
        }
    }
}
