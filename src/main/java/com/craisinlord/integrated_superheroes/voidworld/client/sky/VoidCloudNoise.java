package com.craisinlord.integrated_superheroes.voidworld.client.sky;

final class VoidCloudNoise {
    private VoidCloudNoise() {
    }

    private static float lattice(int x, int y, int periodX, int periodY, int seed) {
        int ix = Math.floorMod(x, periodX);
        int iy = Math.floorMod(y, periodY);
        int h = ix * 374761393 + iy * 668265263 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    static float value(float x, float y, int periodX, int periodY, int seed) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        float fx = fade(x - x0);
        float fy = fade(y - y0);
        float a = lattice(x0, y0, periodX, periodY, seed);
        float b = lattice(x0 + 1, y0, periodX, periodY, seed);
        float c = lattice(x0, y0 + 1, periodX, periodY, seed);
        float d = lattice(x0 + 1, y0 + 1, periodX, periodY, seed);
        float top = a + (b - a) * fx;
        float bottom = c + (d - c) * fx;
        return top + (bottom - top) * fy;
    }

    static float fbm(float x, float y, int periodX, int periodY, int octaves, int seed) {
        float sum = 0.0F;
        float amp = 0.5F;
        float norm = 0.0F;
        float scale = 1.0F;
        for (int o = 0; o < octaves; o++) {
            sum += amp * value(x * scale, y * scale, periodX * (int) scale, periodY * (int) scale, seed + o * 31);
            norm += amp;
            amp *= 0.5F;
            scale *= 2.0F;
        }
        return sum / norm;
    }
}
