package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.hero.HeroShockwaves;
import fuzs.mutantmonsters.world.level.SeismicWave;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class MutantMonstersSeismicWaves {
    private MutantMonstersSeismicWaves() {
    }

    public static List<HeroShockwaves.Point> line(ServerLevel level, int x1, int z1, int x2, int z2, int y) {
        List<SeismicWave> waves = new ArrayList<>();
        SeismicWave.createWaves(level, waves, x1, z1, x2, z2, y);
        List<HeroShockwaves.Point> points = new ArrayList<>(waves.size());
        for (SeismicWave wave : waves) {
            points.add(new Wrapped(wave));
        }
        return points;
    }

    private record Wrapped(SeismicWave wave) implements HeroShockwaves.Point {
        @Override
        public int x() {
            return wave.getX();
        }

        @Override
        public int y() {
            return wave.getY();
        }

        @Override
        public int z() {
            return wave.getZ();
        }

        @Override
        public void affect(ServerLevel level, ServerPlayer owner) {
            wave.affectBlocks(level, owner);
        }
    }
}
