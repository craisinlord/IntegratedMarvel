package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.gauntlet.GauntletGrief;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.baranhan.viltrumiteflight.network.packet.FlightToggleC2SPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tintankgames.marvel.world.level.MarvelGameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The crouch + double-jump takeoff (Viltrumites and Captain Marvel) throws every block in a 3-block radius up as
 * falling debris. It ignored claims, spawn protection and the superpowerGriefing game rule, and turned containers into
 * falling blocks that dropped their contents into the void. It now follows the same rules as Hulk's rampage: the game
 * rule and the player's build rights gate it, and each block must pass the usual break checks and hold no block entity.
 */
@Mixin(value = FlightToggleC2SPacket.class, remap = false)
public abstract class ViltrumiteTakeoffGriefMixin {
    @ModifyExpressionValue(method = "handle", remap = false, at = @At(value = "FIELD",
            target = "Ldev/baranhan/viltrumiteflight/config/ViltrumiteConfig;breakBlocksOnTakeoff:Z"))
    private static boolean integratedSuperheroes$takeoffGriefAllowed(boolean breakBlocks, FlightToggleC2SPacket packet, IPayloadContext context) {
        return breakBlocks && context.player() instanceof ServerPlayer player && player.mayBuild()
                && player.level().getGameRules().getBoolean(MarvelGameRules.RULE_SUPERPOWERGRIEFING);
    }

    @WrapOperation(method = "handle", remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroySpeed(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private static float integratedSuperheroes$protectedTakeoffBlocks(BlockState state, BlockGetter getter, BlockPos pos,
                                                                      Operation<Float> original, FlightToggleC2SPacket packet, IPayloadContext context) {
        if (getter instanceof ServerLevel level && context.player() instanceof ServerPlayer player
                && (level.getBlockEntity(pos) != null || !GauntletGrief.canEdit(player, level, pos))) {
            return -1.0F;
        }
        return original.call(state, getter, pos);
    }
}
