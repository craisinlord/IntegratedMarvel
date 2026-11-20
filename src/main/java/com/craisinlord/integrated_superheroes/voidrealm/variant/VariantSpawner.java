package com.craisinlord.integrated_superheroes.voidrealm.variant;

import com.craisinlord.integrated_superheroes.portal.MarvelPortal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class VariantSpawner {
    private VariantSpawner() {
    }

    static void tick(ServerLevel level, long now) {
        if (!VariantConfig.ENABLED.get() || level.players().isEmpty() || !VariantTiers.spawnsIn(level)) {
            return;
        }
        long period = Math.max(1L, VariantConfig.CHECK_TICKS.get() / 20L);
        for (ServerPlayer player : level.players()) {
            if ((now / 20L + player.getId()) % period != 0L || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            check(level, player);
        }
    }

    private static void check(ServerLevel level, ServerPlayer player) {
        if (VariantTears.tryNexus(level, player)) {
            return;
        }
        List<Mob> loaded = VoidVariants.loaded(level);
        if (loaded.size() + VariantTears.pending(level) >= VariantConfig.MAX_LOADED.get()) {
            return;
        }
        double radius = VariantConfig.NEAR_PLAYER_RADIUS.get();
        int near = 0;
        for (Mob mob : loaded) {
            if (mob.distanceToSqr(player) <= radius * radius) {
                near++;
            }
        }
        if (near >= VariantConfig.MAX_NEAR_PLAYER.get()) {
            return;
        }
        int tier = VariantTiers.forPlayer(player);
        double chance = VariantConfig.SPAWN_CHANCE.get() + VariantConfig.SPAWN_CHANCE_PER_TIER.get() * tier;
        if (chance <= 0.0D || level.random.nextDouble() >= chance) {
            return;
        }
        double min = VariantConfig.SPAWN_MIN_DISTANCE.get();
        double max = Math.max(min + 1.0D, VariantConfig.SPAWN_MAX_DISTANCE.get());
        Vec3 at = surfaceSpot(level, player.position(), min, max);
        if (at == null) {
            return;
        }
        if (MarvelPortal.enabled() || level.random.nextDouble() < VariantConfig.TEAR_CHANCE.get()) {
            VariantTears.arrive(level, at, tier, player.position());
        } else {
            VoidVariants.spawn(level, BlockPos.containing(at), tier);
        }
    }

    static Vec3 surfaceSpot(ServerLevel level, Vec3 around, double min, double max) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = min + level.random.nextDouble() * (max - min);
            int x = Mth.floor(around.x + Math.cos(angle) * distance);
            int z = Mth.floor(around.z + Math.sin(angle) * distance);
            if (!level.isPositionEntityTicking(new BlockPos(x, Mth.floor(around.y), z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (y > level.getMinBuildHeight() && level.getBlockState(pos.below()).getFluidState().isEmpty()
                    && !level.getBlockState(pos.below()).isAir() && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
                return Vec3.atBottomCenterOf(pos);
            }
        }
        return null;
    }
}
