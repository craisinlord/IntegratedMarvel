package com.craisinlord.integrated_superheroes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.baranhan.viltrumitecore.network.packet.HandPosSyncC2SPacket;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = HandPosSyncC2SPacket.class, remap = false)
public abstract class ViltrumiteHandPositionMixin {
    @WrapOperation(method = "lambda$handle$0", at = @At(value = "INVOKE",
            target = "Ldev/baranhan/viltrumitecore/util/ViltrumiteCorePlayer;setServerHandPos(Lnet/minecraft/world/phys/Vec3;)V"))
    private void integratedSuperheroes$withinReach(ViltrumiteCorePlayer core, Vec3 hand, Operation<Void> original) {
        original.call(core, core instanceof Player player && !integratedSuperheroes$reachable(player, hand) ? null : hand);
    }

    @Unique
    private static boolean integratedSuperheroes$reachable(Player player, Vec3 hand) {
        double distance = player.getEyePosition().distanceTo(hand);
        double speed = Math.max(player.getDeltaMovement().length(), player.getKnownMovement().length());
        double reach = (3.0D + Math.max(3.0D, player.entityInteractionRange())) * Math.max(1.0D, player.getScale()) + 10.0D * speed;
        return Double.isFinite(distance) && distance <= reach;
    }
}
