package com.craisinlord.integrated_superheroes.voidworld.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

final class VoidClouds {
    private static final int W = 1024;
    private static final int H = 256;
    private static final int CELL_U = 64;
    private static final int CELL_V = 32;
    private static final int SEED = 0x51A7E;
    private static final int AZ = 64;
    private static final float[] ELEV = {-12F, -6F, -2F, 1F, 4F, 8F, 13F, 19F, 26F, 34F, 44F, 56F, 70F, 90F};
    private static final float EL_MIN = -12F;
    private static final float EL_SPAN = 102F;
    private static final float[] LAYER_RADIUS = {1.0F, 0.985F, 0.97F};
    private static final int[] LAYER_U_SCALE = {1, 2, 1};
    private static final float[] LAYER_V_SCALE = {1.0F, 1.7F, 0.8F};
    private static final float[] LAYER_V_OFFSET = {0.0F, 0.37F, 0.71F};
    private static final float[] LAYER_SPEED = {0.3F, 0.5F, -0.2F};
    private static final float[] LAYER_ALPHA = {0.92F, 0.7F, 0.55F};
    private static final float RIM_WIDTH = 0.09F;
    private static final float LIGHT_SPREAD = 1.0F - Mth.cos(25.0F * Mth.DEG_TO_RAD);

    private static final float[][][] DIRS = new float[ELEV.length][AZ + 1][3];
    private static final float[][] RIM = new float[ELEV.length][AZ + 1];
    private static final double[] PHASE = new double[LAYER_RADIUS.length];
    private static final float[] FOG = new float[3];
    private static final float[] BASE = new float[3];
    private static final float[] HUE = new float[3];
    private static final float[] FLASH = new float[3];
    private static final float[] PROFILE = new float[ELEV.length];
    private static final float[] SHADE = new float[ELEV.length * 3];
    private static final Matrix4f MATRIX = new Matrix4f();
    private static final Random RANDOM = new Random();
    private static final int MAX_LIGHTS = 2;
    private static final float[] LIGHT_DIR = new float[MAX_LIGHTS * 3];
    private static final float[] LIGHT_RGB = new float[MAX_LIGHTS * 3];

    private static DynamicTexture texture;
    private static float[] raw;
    private static float[] sorted;
    private static CompletableFuture<NativeImage> generating;
    private static CompletableFuture<Void> baking;
    private static float bakedStorm = -1.0F;
    private static double lastBake;
    private static final double REBUILD_SECONDS = 0.05D;

    private static double lastFrame = -1.0D;
    private static double lastBuild = -1.0D;
    private static boolean builtFancy;
    private static boolean farBuilt;
    private static boolean nearBuilt;

    private static VertexBuffer farMesh;
    private static VertexBuffer nearMesh;
    private static VertexBuffer lightMesh;
    private static VertexBuffer boltMesh;
    private static ByteBufferBuilder farBytes;
    private static ByteBufferBuilder nearBytes;
    private static ByteBufferBuilder lightBytes;
    private static ByteBufferBuilder boltBytes;

    private static int boltSerial = -1;
    private static final List<float[]> BOLT = new ArrayList<>();
    private static double flickerStart = -100.0D;
    private static double nextFlicker;
    private static final float[] FLICKER_DIR = new float[3];

    static {
        float ce = Mth.cos(TimelineRing.CENTER_ELEVATION);
        float cx = 0.0F;
        float cy = Mth.sin(TimelineRing.CENTER_ELEVATION);
        float cz = -ce;
        for (int r = 0; r < ELEV.length; r++) {
            float el = ELEV[r] * Mth.DEG_TO_RAD;
            for (int s = 0; s <= AZ; s++) {
                float az = s * Mth.TWO_PI / AZ;
                direction(DIRS[r][s], az, el);
                float[] d = DIRS[r][s];
                float dot = Mth.clamp(d[0] * cx + d[1] * cy + d[2] * cz, -1.0F, 1.0F);
                float off = ((float) Math.acos(dot) - TimelineRing.RADIUS) / RIM_WIDTH;
                RIM[r][s] = ELEV[r] > -2F ? (float) Math.exp(-off * off) : 0.0F;
            }
        }
    }

    private VoidClouds() {
    }

    private static void direction(float[] out, float az, float el) {
        float ce = Mth.cos(el);
        out[0] = ce * Mth.sin(az);
        out[1] = Mth.sin(el);
        out[2] = -ce * Mth.cos(az);
    }

    private static double now() {
        return Util.getMillis() / 1000.0D;
    }

    static boolean enabled() {
        return VoidSkyConfig.clouds() && Minecraft.getInstance().options.getCloudsType() != CloudStatus.OFF;
    }

    static void release() {
        if (baking != null) {
            baking.join();
            baking = null;
        }
        if (generating != null) {
            CompletableFuture<NativeImage> pending = generating;
            generating = null;
            pending.thenAccept(NativeImage::close);
        }
        if (texture != null) {
            texture.close();
            texture = null;
        }
        raw = null;
        sorted = null;
        bakedStorm = -1.0F;
        BOLT.clear();
        boltSerial = -1;
        lastFrame = -1.0D;
    }

    private static void ensureBuffers() {
        if (farMesh != null) {
            return;
        }
        farBytes = new ByteBufferBuilder(65536);
        nearBytes = new ByteBufferBuilder(131072);
        lightBytes = new ByteBufferBuilder(65536);
        boltBytes = new ByteBufferBuilder(16384);
        farMesh = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        nearMesh = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        lightMesh = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        boltMesh = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    }

    private static boolean ensureTexture(float storm, double now) {
        if (texture == null) {
            if (generating == null) {
                float target = storm;
                generating = CompletableFuture.supplyAsync(() -> generate(target), Util.backgroundExecutor());
                return false;
            }
            if (!generating.isDone()) {
                return false;
            }
            NativeImage image = generating.join();
            generating = null;
            texture = new DynamicTexture(image);
            texture.setFilter(true, false);
            texture.bind();
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            bakedStorm = storm;
            lastBake = now;
            return true;
        }
        if (baking != null) {
            if (baking.isDone()) {
                baking.join();
                baking = null;
                texture.upload();
                lastBake = now;
            }
        } else if (Math.abs(storm - bakedStorm) > 0.05F && now - lastBake > 0.5D) {
            float target = storm;
            NativeImage image = texture.getPixels();
            bakedStorm = target;
            baking = CompletableFuture.runAsync(() -> threshold(image, raw, sorted, target), Util.backgroundExecutor());
        }
        return true;
    }

    private static NativeImage generate(float storm) {
        float[] field = new float[W * H];
        int px = W / CELL_U;
        int py = H / CELL_V;
        for (int y = 0; y < H; y++) {
            float v = (float) y / CELL_V;
            for (int x = 0; x < W; x++) {
                float u = (float) x / CELL_U;
                float warp = VoidCloudNoise.fbm(u * 0.5F, v * 0.5F, px / 2, py / 2, 2, SEED + 7);
                float u2 = u + (warp - 0.5F) * 1.6F;
                float billow = VoidCloudNoise.fbm(u2, v, px, py, 4, SEED);
                float streak = VoidCloudNoise.value(u2 * 0.5F, v * 4.0F, px / 2, py * 4, SEED + 3);
                float mass = VoidCloudNoise.value(u * 0.25F, v * 0.5F, px / 4, py / 2, SEED + 5);
                field[y * W + x] = 0.6F * billow + 0.15F * streak + 0.35F * mass;
            }
        }
        float[] sample = new float[field.length / 16];
        for (int i = 0; i < sample.length; i++) {
            sample[i] = field[i * 16];
        }
        Arrays.sort(sample);
        raw = field;
        sorted = sample;
        NativeImage image = new NativeImage(W, H, false);
        threshold(image, field, sample, storm);
        return image;
    }

    private static void threshold(NativeImage image, float[] field, float[] sample, float storm) {
        if (field == null || sample == null) {
            return;
        }
        float coverage = 0.62F + 0.3F * storm;
        float t = sample[(int) ((1.0F - coverage) * (sample.length - 1))];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                float d = field[y * W + x];
                float a = smoothstep(t, t + 0.16F, d);
                float shade = 1.0F - 0.42F * smoothstep(t + 0.05F, t + 0.45F, d);
                int alpha = (int) (a * 255.0F);
                int grey = (int) (shade * 255.0F);
                image.setPixelRGBA(x, y, alpha << 24 | grey << 16 | grey << 8 | grey);
            }
        }
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        return TimelineRing.smooth((x - edge0) / (edge1 - edge0));
    }

    static void render(Matrix4f modelView, Matrix4f projection, Vec3 camera, float radius) {
        double now = now();
        float dt = lastFrame < 0.0D ? 0.0F : (float) Mth.clamp(now - lastFrame, 0.0D, 0.1D);
        lastFrame = now;
        float storm = TimelineSky.stormLevel();
        ensureBuffers();
        if (!ensureTexture(storm, now)) {
            return;
        }
        boolean fancy = Minecraft.getInstance().options.getCloudsType() == CloudStatus.FANCY;
        float speed = 1.0F + 1.5F * storm;
        for (int l = 0; l < PHASE.length; l++) {
            PHASE[l] = (PHASE[l] + dt * LAYER_SPEED[l] * speed / 360.0D) % 1.0D;
        }
        float abyss = TimelineSky.abyss(camera.y);
        prepareColours(storm, camera);
        int lights = gatherLights(now, storm);
        boolean rebuild = lastBuild < 0.0D || now - lastBuild >= REBUILD_SECONDS || fancy != builtFancy;
        MeshData far = rebuild ? buildLayers(farBytes, 0, 1) : null;
        MeshData near = rebuild && fancy ? buildLayers(nearBytes, 1, LAYER_RADIUS.length) : null;
        MeshData light = lights > 0 ? buildLights(lights, fancy ? 2 : 1) : null;
        MeshData bolt = buildBolt(now);

        MATRIX.set(modelView).scale(radius);
        ShaderInstance texShader = GameRenderer.getPositionTexColorShader();
        ShaderInstance colourShader = GameRenderer.getPositionColorShader();
        if (texShader == null || colourShader == null) {
            closeAll(far, near, light, bolt);
            return;
        }
        if (rebuild) {
            lastBuild = now;
            builtFancy = fancy;
            farBuilt = far != null;
            nearBuilt = near != null;
        }
        float k = (1.0F - 0.5F * storm) * abyss;
        float opacity = VoidSkyConfig.cloudOpacity();
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture.getId());
        RenderSystem.setShaderColor(k, k, k, opacity);
        if (farBuilt) {
            draw(farMesh, far, texShader, projection);
        }
        if (bolt != null) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            draw(boltMesh, bolt, colourShader, projection);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, texture.getId());
            RenderSystem.setShaderColor(k, k, k, opacity);
        }
        if (nearBuilt) {
            draw(nearMesh, near, texShader, projection);
        }
        if (light != null) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            float lift = 0.4F + 0.6F * abyss;
            RenderSystem.setShaderColor(lift, lift, lift, opacity);
            draw(lightMesh, light, texShader, projection);
        }
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void closeAll(MeshData... meshes) {
        for (MeshData mesh : meshes) {
            if (mesh != null) {
                mesh.close();
            }
        }
    }

    private static void draw(VertexBuffer buffer, MeshData mesh, ShaderInstance shader, Matrix4f projection) {
        buffer.bind();
        if (mesh != null) {
            buffer.upload(mesh);
        }
        buffer.drawWithShader(MATRIX, projection, shader);
    }

    private static void prepareColours(float storm, Vec3 camera) {
        float[] fog = RenderSystem.getShaderFogColor();
        FOG[0] = fog[0];
        FOG[1] = fog[1];
        FOG[2] = fog[2];
        TimelinePalette.hue(TimelinePalette.level(camera.x, camera.z), HUE);
        float lum = (FOG[0] + FOG[1] + FOG[2]) / 3.0F;
        for (int c = 0; c < 3; c++) {
            float lifted = FOG[c] * 0.82F + 0.01F;
            BASE[c] = Math.min(1.0F, lifted + (HUE[c] * lum * 2.0F - lifted) * 0.1F);
        }
        float top = 24.0F + 16.0F * storm;
        float overcast = TimelineRing.smooth((storm - 0.55F) / 0.45F);
        for (int r = 0; r < ELEV.length; r++) {
            float el = ELEV[r];
            float a = el <= top ? 1.0F : 1.0F - TimelineRing.smooth((el - top) / 12.0F);
            PROFILE[r] = Mth.lerp(overcast, a, 1.0F);
            float b = el < 0.0F ? 1.0F - 0.7F * TimelineRing.smooth(-el / 12.0F) : 0.8F + 0.25F * TimelineRing.smooth(el / 35.0F);
            float haze = 0.45F * (1.0F - TimelineRing.smooth(Math.abs(el) / 14.0F));
            for (int c = 0; c < 3; c++) {
                SHADE[r * 3 + c] = Mth.lerp(haze, BASE[c] * b, FOG[c]);
            }
        }
    }

    private static MeshData buildLayers(ByteBufferBuilder bytes, int from, int to) {
        BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float rim = VoidSkyConfig.cloudRingGlow() && VoidSkyConfig.strands()
                ? 0.35F * VoidSkyConfig.strandBrightness() * (1.0F - 0.55F * TimelineSky.stormLevel()) : 0.0F;
        for (int l = from; l < to; l++) {
            float radius = LAYER_RADIUS[l];
            float alpha = LAYER_ALPHA[l];
            for (int r = 0; r < ELEV.length - 1; r++) {
                if (PROFILE[r] <= 0.002F && PROFILE[r + 1] <= 0.002F) {
                    continue;
                }
                for (int s = 0; s < AZ; s++) {
                    vertex(b, l, r, s, radius, alpha, rim);
                    vertex(b, l, r, s + 1, radius, alpha, rim);
                    vertex(b, l, r + 1, s + 1, radius, alpha, rim);
                    vertex(b, l, r + 1, s, radius, alpha, rim);
                }
            }
        }
        return b.build();
    }

    private static void vertex(BufferBuilder b, int layer, int ring, int seg, float radius, float alpha, float rim) {
        float[] d = DIRS[ring][seg];
        float u = (float) seg / AZ * LAYER_U_SCALE[layer] + (float) PHASE[layer] * LAYER_U_SCALE[layer];
        float v = (ELEV[ring] - EL_MIN) / EL_SPAN * LAYER_V_SCALE[layer] + LAYER_V_OFFSET[layer];
        float glow = RIM[ring][seg] * rim;
        float cr = Math.min(1.0F, SHADE[ring * 3] + HUE[0] * glow);
        float cg = Math.min(1.0F, SHADE[ring * 3 + 1] + HUE[1] * glow);
        float cb = Math.min(1.0F, SHADE[ring * 3 + 2] + HUE[2] * glow);
        b.addVertex(d[0] * radius, d[1] * radius, d[2] * radius).setUv(u, v).setColor(cr, cg, cb, alpha * PROFILE[ring]);
    }

    private static int gatherLights(double now, float storm) {
        int count = 0;
        if (!VoidSkyConfig.cloudLightning()) {
            return 0;
        }
        float env = TimelineSky.flashLevel(FLASH);
        if (env > 0.01F) {
            direction(LIGHT_DIR, TimelineSky.flashAzimuth(), TimelineSky.flashElevation());
            LIGHT_RGB[0] = FLASH[0] * env;
            LIGHT_RGB[1] = FLASH[1] * env;
            LIGHT_RGB[2] = FLASH[2] * env;
            count++;
        }
        if (storm > 0.3F && now >= nextFlicker) {
            nextFlicker = now + (3.0D + RANDOM.nextDouble() * 5.0D) / (0.5D + storm);
            flickerStart = now;
            direction(FLICKER_DIR, RANDOM.nextFloat() * Mth.TWO_PI, (2.0F + RANDOM.nextFloat() * 14.0F) * Mth.DEG_TO_RAD);
        } else if (storm <= 0.3F) {
            nextFlicker = Math.max(nextFlicker, now + 2.0D);
        }
        double t = now - flickerStart;
        if (t >= 0.0D && t < 0.12D) {
            float f = (float) (Math.sin(t / 0.12D * Math.PI) * (0.6D + 0.4D * Math.sin(t * 90.0D))) * 0.7F * VoidSkyConfig.lightningBrightness();
            int o = count * 3;
            LIGHT_DIR[o] = FLICKER_DIR[0];
            LIGHT_DIR[o + 1] = FLICKER_DIR[1];
            LIGHT_DIR[o + 2] = FLICKER_DIR[2];
            LIGHT_RGB[o] = 0.62F * f;
            LIGHT_RGB[o + 1] = 0.45F * f;
            LIGHT_RGB[o + 2] = 1.0F * f;
            count++;
        }
        return count;
    }

    private static MeshData buildLights(int lights, int layers) {
        BufferBuilder b = new BufferBuilder(lightBytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int l = 0; l < layers; l++) {
            float radius = LAYER_RADIUS[l] * 0.999F;
            for (int r = 0; r < ELEV.length - 1; r++) {
                if (ELEV[r] > 40.0F) {
                    break;
                }
                for (int s = 0; s < AZ; s++) {
                    float g = Math.max(Math.max(light(r, s, lights), light(r, s + 1, lights)),
                            Math.max(light(r + 1, s, lights), light(r + 1, s + 1, lights)));
                    if (g < 0.01F) {
                        continue;
                    }
                    lightVertex(b, l, r, s, radius, lights);
                    lightVertex(b, l, r, s + 1, radius, lights);
                    lightVertex(b, l, r + 1, s + 1, radius, lights);
                    lightVertex(b, l, r + 1, s, radius, lights);
                }
            }
        }
        return b.build();
    }

    private static float light(int ring, int seg, int lights) {
        float[] d = DIRS[ring][seg];
        float best = 0.0F;
        for (int i = 0; i < lights; i++) {
            int o = i * 3;
            float dot = d[0] * LIGHT_DIR[o] + d[1] * LIGHT_DIR[o + 1] + d[2] * LIGHT_DIR[o + 2];
            best = Math.max(best, (float) Math.exp(-(1.0F - dot) / LIGHT_SPREAD));
        }
        return best;
    }

    private static void lightVertex(BufferBuilder b, int layer, int ring, int seg, float radius, int lights) {
        float[] d = DIRS[ring][seg];
        float cr = 0.0F;
        float cg = 0.0F;
        float cb = 0.0F;
        for (int i = 0; i < lights; i++) {
            int o = i * 3;
            float dot = d[0] * LIGHT_DIR[o] + d[1] * LIGHT_DIR[o + 1] + d[2] * LIGHT_DIR[o + 2];
            float g = (float) Math.exp(-(1.0F - dot) / LIGHT_SPREAD);
            cr += LIGHT_RGB[o] * g;
            cg += LIGHT_RGB[o + 1] * g;
            cb += LIGHT_RGB[o + 2] * g;
        }
        float under = ELEV[ring] < 6.0F ? 1.0F : 0.7F;
        float u = (float) seg / AZ * LAYER_U_SCALE[layer] + (float) PHASE[layer] * LAYER_U_SCALE[layer];
        float v = (ELEV[ring] - EL_MIN) / EL_SPAN * LAYER_V_SCALE[layer] + LAYER_V_OFFSET[layer];
        b.addVertex(d[0] * radius, d[1] * radius, d[2] * radius).setUv(u, v)
                .setColor(Math.min(1.0F, cr * under), Math.min(1.0F, cg * under), Math.min(1.0F, cb * under), LAYER_ALPHA[layer] * PROFILE[ring]);
    }

    private static MeshData buildBolt(double now) {
        if (!VoidSkyConfig.cloudLightning()) {
            return null;
        }
        int serial = TimelineSky.flashSerial();
        if (serial != boltSerial) {
            boltSerial = serial;
            BOLT.clear();
            if (serial >= 0 && RANDOM.nextFloat() < 0.4F + 0.35F * TimelineSky.stormLevel()) {
                makeBolt(TimelineSky.flashAzimuth());
            }
        }
        if (BOLT.isEmpty()) {
            return null;
        }
        float env = TimelineSky.flashLevel(FLASH);
        if (env < 0.02F) {
            return null;
        }
        BufferBuilder b = new BufferBuilder(boltBytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float gr = FLASH[0] * env * 0.35F;
        float gg = FLASH[1] * env * 0.35F;
        float gb = FLASH[2] * env * 0.35F;
        float wr = Mth.lerp(0.6F, FLASH[0], 1.0F) * env;
        float wg = Mth.lerp(0.6F, FLASH[1], 1.0F) * env;
        float wb = Mth.lerp(0.6F, FLASH[2], 1.0F) * env;
        for (int i = 0; i < BOLT.size(); i++) {
            float[] line = BOLT.get(i);
            float scale = i == 0 ? 1.0F : 0.6F;
            boltStrip(b, line, 0.012F * scale, gr, gg, gb, true);
            boltStrip(b, line, 0.0018F * scale, wr, wg, wb, false);
        }
        return b.build();
    }

    private static void makeBolt(float azimuth) {
        float top = (12.0F + RANDOM.nextFloat() * 6.0F) * Mth.DEG_TO_RAD;
        float bottom = -4.0F * Mth.DEG_TO_RAD;
        float[] main = displace(azimuth, top, azimuth + (RANDOM.nextFloat() - 0.5F) * 0.12F, bottom, 0.045F, 5);
        BOLT.add(toDirections(main));
        int forks = 1 + RANDOM.nextInt(2);
        int n = main.length / 2;
        for (int f = 0; f < forks; f++) {
            int at = n / 4 + RANDOM.nextInt(Math.max(1, n / 2));
            float az = main[at * 2];
            float el = main[at * 2 + 1];
            float side = RANDOM.nextBoolean() ? 1.0F : -1.0F;
            float endAz = az + side * (0.04F + RANDOM.nextFloat() * 0.06F);
            float endEl = el - (0.04F + RANDOM.nextFloat() * 0.08F);
            BOLT.add(toDirections(displace(az, el, endAz, endEl, 0.02F, 3)));
        }
    }

    private static float[] displace(float az0, float el0, float az1, float el1, float amount, int levels) {
        int n = (1 << levels) + 1;
        float[] p = new float[n * 2];
        p[0] = az0;
        p[1] = el0;
        p[(n - 1) * 2] = az1;
        p[(n - 1) * 2 + 1] = el1;
        float a = amount;
        for (int step = n - 1; step > 1; step /= 2) {
            for (int i = 0; i + step < n; i += step) {
                int m = i + step / 2;
                p[m * 2] = (p[i * 2] + p[(i + step) * 2]) * 0.5F + (RANDOM.nextFloat() - 0.5F) * a;
                p[m * 2 + 1] = (p[i * 2 + 1] + p[(i + step) * 2 + 1]) * 0.5F + (RANDOM.nextFloat() - 0.5F) * a * 0.3F;
            }
            a *= 0.55F;
        }
        return p;
    }

    private static float[] toDirections(float[] angles) {
        int n = angles.length / 2;
        float[] out = new float[n * 6];
        float[] d = new float[3];
        for (int i = 0; i < n; i++) {
            float az = angles[i * 2];
            direction(d, az, angles[i * 2 + 1]);
            out[i * 6] = d[0] * 0.992F;
            out[i * 6 + 1] = d[1] * 0.992F;
            out[i * 6 + 2] = d[2] * 0.992F;
            out[i * 6 + 3] = Mth.cos(az);
            out[i * 6 + 4] = 0.0F;
            out[i * 6 + 5] = Mth.sin(az);
        }
        return out;
    }

    private static void boltStrip(BufferBuilder b, float[] line, float width, float r, float g, float bl, boolean soft) {
        int n = line.length / 6;
        for (int i = 0; i < n - 1; i++) {
            int a = i * 6;
            int c = a + 6;
            float w0 = width * (1.0F - 0.5F * i / (n - 1));
            float w1 = width * (1.0F - 0.5F * (i + 1) / (n - 1));
            float ax = line[a];
            float ay = line[a + 1];
            float az = line[a + 2];
            float cx = line[c];
            float cy = line[c + 1];
            float cz = line[c + 2];
            float sax = line[a + 3] * w0;
            float saz = line[a + 5] * w0;
            float scx = line[c + 3] * w1;
            float scz = line[c + 5] * w1;
            if (soft) {
                b.addVertex(ax - sax, ay, az - saz).setColor(r, g, bl, 0.0F);
                b.addVertex(ax, ay, az).setColor(r, g, bl, 1.0F);
                b.addVertex(cx, cy, cz).setColor(r, g, bl, 1.0F);
                b.addVertex(cx - scx, cy, cz - scz).setColor(r, g, bl, 0.0F);
                b.addVertex(ax, ay, az).setColor(r, g, bl, 1.0F);
                b.addVertex(ax + sax, ay, az + saz).setColor(r, g, bl, 0.0F);
                b.addVertex(cx + scx, cy, cz + scz).setColor(r, g, bl, 0.0F);
                b.addVertex(cx, cy, cz).setColor(r, g, bl, 1.0F);
            } else {
                b.addVertex(ax - sax, ay, az - saz).setColor(r, g, bl, 1.0F);
                b.addVertex(ax + sax, ay, az + saz).setColor(r, g, bl, 1.0F);
                b.addVertex(cx + scx, cy, cz + scz).setColor(r, g, bl, 1.0F);
                b.addVertex(cx - scx, cy, cz - scz).setColor(r, g, bl, 1.0F);
            }
        }
    }
}
