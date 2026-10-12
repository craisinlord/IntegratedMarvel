package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.IronManSuits;
import com.craisinlord.integrated_superheroes.suit.SuitStates;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.tintankgames.marvel.world.item.UnibeamItem", remap = false)
public abstract class IronManUnibeamMixin {
    @ModifyVariable(method = "releaseUsing", at = @At("STORE"), ordinal = 0, remap = false)
    private HitResult integratedSuperheroes$unibeamDirectHit(HitResult hit, ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return hit;
        }

        double overcharge = player instanceof ServerPlayer serverPlayer ? SuitStates.consumeOvercharge(serverPlayer) : 1.0D;
        if (!(hit instanceof EntityHitResult entityHit)) {
            return hit;
        }
        IronManSuits.Stats stats = IronManSuits.isLoaded() ? IronManSuits.forWearer(player) : null;
        if (stats == null || stats.unibeamDamage().get() <= 0.0D || entityHit.getEntity() == player) {
            return hit;
        }
        if (entityHit.getEntity() instanceof LivingEntity living) {
            living.invulnerableTime = 0;
        }
        float damage = (float) (stats.unibeamDamage().get() * overcharge);
        HeroAbilities.runAsAbilityDamage(() -> entityHit.getEntity().hurt(player.damageSources().playerAttack(player), damage));
        return hit;
    }
}
