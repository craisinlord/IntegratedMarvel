package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.MutantMonstersSeismicWaves;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class HeroShockwaves {
    private static final List<Wave> ACTIVE = new ArrayList<>();

    private HeroShockwaves() {
    }

    public interface Point {
        int x();

        int y();

        int z();

        void affect(ServerLevel level, ServerPlayer owner);
    }

    private record GroundPoint(BlockPos pos) implements Point {
        @Override
        public int x() {
            return pos.getX();
        }

        @Override
        public int y() {
            return pos.getY();
        }

        @Override
        public int z() {
            return pos.getZ();
        }

        @Override
        public void affect(ServerLevel level, ServerPlayer owner) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return;
            }
            level.levelEvent(2001, pos, Block.getId(state));
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                    12, 0.3D, 0.2D, 0.3D, 0.15D);
            level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 2, 0.2D, 0.1D, 0.2D, 0.02D);
        }
    }

    private static List<Point> vanillaLine(ServerLevel level, int x1, int z1, int x2, int z2, int y) {
        List<Point> line = new ArrayList<>();
        int dx = x2 - x1;
        int dz = z2 - z1;
        int steps = Math.max(Math.abs(dx), Math.abs(dz));
        for (int i = 0; i <= steps; i++) {
            int x = steps == 0 ? x1 : x1 + Math.round((float) dx * i / steps);
            int z = steps == 0 ? z1 : z1 + Math.round((float) dz * i / steps);
            BlockPos ground = ground(level, x, y, z);
            if (ground != null) {
                line.add(new GroundPoint(ground));
            }
        }
        return line;
    }

    private static BlockPos ground(ServerLevel level, int x, int y, int z) {
        for (int dy = 1; dy >= -3; dy--) {
            BlockPos pos = new BlockPos(x, y + dy - 1, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.isFaceSturdy(level, pos, Direction.UP)
                    && !level.getBlockState(pos.above()).isSuffocating(level, pos.above())) {
                return pos;
            }
        }
        return null;
    }

    private static final class Wave {
        private final ServerLevel level;
        private final UUID owner;
        private final List<List<Point>> lines;
        private final float damage;
        private final Set<Integer> hit = new HashSet<>();
        private final HeroAdvancements.Cast cast = HeroAdvancements.current();
        private int step;

        private Wave(ServerLevel level, UUID owner, List<List<Point>> lines, float damage) {
            this.level = level;
            this.owner = owner;
            this.lines = lines;
            this.damage = damage;
        }
    }

    public static void ring(ServerPlayer player, int directions, int length, float damage) {
        List<Float> yaws = new ArrayList<>();
        for (int i = 0; i < directions; i++) {
            yaws.add(360.0F / directions * i);
        }
        launch(player, yaws, 1.0D, length, damage);
    }

    public static void fan(ServerPlayer player, int lines, float spread, int length, float damage) {
        List<Float> yaws = new ArrayList<>();
        for (int i = 0; i < lines; i++) {
            float offset = lines == 1 ? 0.0F : -spread + 2.0F * spread * i / (lines - 1);
            yaws.add(player.getYRot() + offset);
        }
        launch(player, yaws, 1.5D, length, damage);
    }

    private static void launch(ServerPlayer player, List<Float> yaws, double start, int length, float damage) {
        ServerLevel level = player.serverLevel();
        int y = Mth.floor(player.getBoundingBox().minY);
        List<List<Point>> lines = new ArrayList<>();
        for (float yaw : yaws) {
            Vec3 direction = Vec3.directionFromRotation(0.0F, yaw);
            int x1 = Mth.floor(player.getX() + direction.x * start);
            int z1 = Mth.floor(player.getZ() + direction.z * start);
            int x2 = Mth.floor(player.getX() + direction.x * (start + length));
            int z2 = Mth.floor(player.getZ() + direction.z * (start + length));
            lines.add(ModCompat.MUTANT_MONSTERS
                    ? MutantMonstersSeismicWaves.line(level, x1, z1, x2, z2, y)
                    : vanillaLine(level, x1, z1, x2, z2, y));
        }
        ACTIVE.add(new Wave(level, player.getUUID(), lines, damage));
    }

    public static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Wave> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            ServerPlayer owner = wave.level.getServer().getPlayerList().getPlayer(wave.owner);
            if (owner == null || owner.level() != wave.level) {
                iterator.remove();
                continue;
            }
            boolean any = false;
            for (List<Point> line : wave.lines) {
                for (int i = wave.step * 2; i < Math.min(line.size(), wave.step * 2 + 2); i++) {
                    any = true;
                    Point point = line.get(i);
                    point.affect(wave.level, owner);
                    AABB box = new AABB(point.x(), point.y() + 1.0D, point.z(), point.x() + 1.0D, point.y() + 3.0D, point.z() + 1.0D).inflate(0.5D);
                    for (LivingEntity target : wave.level.getEntitiesOfClass(LivingEntity.class, box,
                            entity -> entity != owner && entity.isAlive() && !wave.hit.contains(entity.getId()))) {
                        wave.hit.add(target.getId());
                        HeroAdvancements.within(wave.cast, () -> target.hurt(owner.damageSources().mobAttack(owner), wave.damage));
                        Vec3 push = new Vec3(target.getX() - owner.getX(), 0.0D, target.getZ() - owner.getZ());
                        Vec3 away = push.lengthSqr() < 1.0E-4D ? Vec3.ZERO : push.normalize().scale(ModCompat.MUTANT_MONSTERS ? 0.0D : 0.5D);
                        target.setDeltaMovement(target.getDeltaMovement().add(away.x, 0.6D, away.z));
                        target.hurtMarked = true;
                    }
                }
            }
            wave.step++;
            if (!any) {
                iterator.remove();
            }
        }
    }
}
