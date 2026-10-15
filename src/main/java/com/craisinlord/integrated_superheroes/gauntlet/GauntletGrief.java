package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class GauntletGrief {
    public static final TagKey<Block> IMMUNE = TagKey.create(Registries.BLOCK, IntegratedSuperheroes.id("gauntlet_immune"));
    public static final TagKey<Block> UNCOPYABLE = TagKey.create(Registries.BLOCK, IntegratedSuperheroes.id("reality_uncopyable"));

    private static final ThreadLocal<Boolean> PROBING = ThreadLocal.withInitial(() -> false);

    private GauntletGrief() {
    }

    public static boolean probing() {
        return PROBING.get();
    }

    public static boolean canEdit(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return canEdit(player, level, pos, false);
    }

    public static boolean canEdit(ServerPlayer player, ServerLevel level, BlockPos pos, boolean allowUnbreakable) {
        if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.is(IMMUNE)) return false;
        if (!allowUnbreakable && state.getDestroySpeed(level, pos) < 0.0F) return false;
        if (player == null) return true;
        if (!player.mayInteract(level, pos)) return false;
        if (state.isAir()) return true;
        PROBING.set(true);
        try {
            return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled();
        } finally {
            PROBING.set(false);
        }
    }

    public static boolean canEditQuietly(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.is(IMMUNE) || state.getDestroySpeed(level, pos) < 0.0F) return false;
        return player == null || player.mayInteract(level, pos);
    }

    public static boolean canEditQuietly(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(IMMUNE) || state.getDestroySpeed(level, pos) < 0.0F) return false;
        return player == null || player.mayInteract(level, pos);
    }

    public static boolean canDestroyTerrain(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return GauntletConfig.griefing() && canEdit(player, level, pos);
    }
}
