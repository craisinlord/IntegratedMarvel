package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.gauntlet.GauntletGrief;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class HeroGrief {
    private HeroGrief() {
    }

    public static ServerPlayer responsiblePlayer(Entity entity) {
        if (entity instanceof ServerPlayer player) return player;
        if (entity instanceof OwnableEntity ownable && ownable.getOwner() instanceof ServerPlayer player) return player;
        return null;
    }

    public static boolean isPlayerOwned(Entity entity) {
        return entity instanceof ServerPlayer || entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null;
    }

    public static boolean canBreak(LivingEntity caster, ServerLevel level, BlockPos pos) {
        return caster instanceof ServerPlayer player && GauntletGrief.canEdit(player, level, pos);
    }

    public static boolean allowBreak(LivingEntity caster, ServerLevel level, BlockPos pos) {
        if (!(caster instanceof ServerPlayer player) || !GauntletGrief.canEditQuietly(player, level, pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled();
    }

    public static boolean tryBreak(LivingEntity caster, ServerLevel level, BlockPos pos, boolean drop) {
        if (!allowBreak(caster, level, pos)) return false;
        return level.destroyBlock(pos, drop, caster);
    }

    public static boolean canPlace(LivingEntity caster, ServerLevel level, BlockPos pos) {
        if (!(caster instanceof ServerPlayer player) || !level.isInWorldBounds(pos) || !level.isLoaded(pos) || !player.mayInteract(level, pos)) return false;
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        return GauntletGrief.probe(new BlockEvent.EntityPlaceEvent(snapshot, level.getBlockState(pos.below()), player));
    }
}
