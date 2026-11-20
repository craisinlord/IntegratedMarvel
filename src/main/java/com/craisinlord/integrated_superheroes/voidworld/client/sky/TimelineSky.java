package com.craisinlord.integrated_superheroes.voidworld.client.sky;

import com.craisinlord.integrated_superheroes.boss.WitherstormEntity;
import com.craisinlord.integrated_superheroes.voidworld.VoidPrunePayload;
import com.craisinlord.integrated_superheroes.voidworld.VoidTier;
import com.craisinlord.integrated_superheroes.voidworld.VoidWorld;
import com.craisinlord.integrated_superheroes.voidworld.client.VoidSkyClient;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TimelineSky {
    private static final long SEED = 0x5AC12ED7L;
    private static final float GLOW = 0.036F;
    private static final float CORE = 0.0026F;
    private static final float MIN_CORE = 0.0011F;
    private static final float PULSE_LENGTH = 0.13F;
    private static final int PULSES = 24;
    private static final int SPARKS = 128;
    private static final double REBUILD_SECONDS = 0.2D;
    private static final double FRAGMENT_SECONDS = 2.2D;
    private static final float FLARE_SECONDS = 0.6F;
    private static final float DOME_RADIUS = 100.0F;
    private static final float MAX_PRESSURE = 0.45F;
    private static final int MODE_BRANCH = 0;
    private static final int MODE_PULSE = 1;
    private static final int MODE_FLAT = 2;
    private static final int MODE_RING = 3;
    private static final float HALO = 0.11F;
    private static final float[] DOME_ELEVATIONS = {-90F, -25F, -8F, -2F, 0F, 3F, 8F, 16F, 28F, 45F, 65F, 90F};

    private static final TimelineRing TREE = new TimelineRing(SEED);
    private static final Random RANDOM = new Random();
    private static final float[] HUE = new float[3];
    private static final float[] FLASH_COLOUR = new float[3];
    private static final Matrix4f MATRIX = new Matrix4f();
    private static final Quaternionf ROTATION = new Quaternionf();
    private static final Quaternionf SPIN = new Quaternionf();
    private static final Vector3f VEC = new Vector3f();
    private static final float[] SX = new float[(TREE.maxSegs + 4) * 3];
    private static final float[] SS = new float[TREE.maxSegs + 4];
    private static final float[] FX = new float[(TREE.maxSegs + 4) * 3];

    private static final int[] PULSE_BRANCH = new int[PULSES];
    private static final double[] PULSE_START = new double[PULSES];
    private static final float[] PULSE_SPEED = new float[PULSES];

    private static final float[] SPARK_POS = new float[SPARKS * 3];
    private static final float[] SPARK_VEL = new float[SPARKS * 3];
    private static final float[] SPARK_LIFE = new float[SPARKS];
    private static final float[] SPARK_MAX = new float[SPARKS];

    private static final List<Prune> PRUNES = new ArrayList<>();
    private static final List<Fragment> FRAGMENTS = new ArrayList<>();

    private static VertexBuffer strands;
    private static VertexBuffer overlay;
    private static VertexBuffer dome;
    private static VertexBuffer blob;
    private static ByteBufferBuilder strandBytes;
    private static ByteBufferBuilder overlayBytes;
    private static boolean strandsReady;
    private static boolean dirty = true;
    private static double lastBuild = -1.0D;
    private static double lastFrame = -1.0D;
    private static boolean active;
    private static int nextSpark;

    private static boolean hasStorm;
    private static double stormX;
    private static double stormY;
    private static double stormZ;
    private static int scanCooldown;

    private static double flashStart = -100.0D;
    private static int flashSerial;
    private static double nextFlash;
    private static float flashAzimuth;
    private static float flashElevation;

    private TimelineSky() {
    }

    private record Prune(int branch, double start, double snapAt) {
    }

    private record Fragment(float[][] strips, float[] widths, float cx, float cy, float cz, float ax, float az, double start) {
    }

    private static double now() {
        return Util.getMillis() / 1000.0D;
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || !VoidWorld.isVoid(level)) {
            if (active) {
                deactivate();
            }
            return;
        }
        double now = now();
        if (!active) {
            activate(now);
        }
        if (--scanCooldown <= 0) {
            scanCooldown = 20;
            scanStorm(level, player.getX(), player.getZ());
        }
        if (now >= nextFlash) {
            float tier = TimelinePalette.level(player.getX(), player.getZ());
            if (VoidSkyConfig.lightning()) {
                startFlash(now, tier);
            }
            nextFlash = now + (20.0D + RANDOM.nextDouble() * 40.0D) * (1.0D - 0.1D * tier);
        }
    }

    private static void activate(double now) {
        active = true;
        TREE.reset(now);
        PRUNES.clear();
        FRAGMENTS.clear();
        Arrays.fill(SPARK_LIFE, 0.0F);
        for (int i = 0; i < PULSES; i++) {
            PULSE_BRANCH[i] = -1;
            PULSE_START[i] = now + 4.0D + RANDOM.nextDouble() * 8.0D;
            PULSE_SPEED[i] = 0.12F + RANDOM.nextFloat() * 0.18F;
        }
        nextFlash = now + 8.0D + RANDOM.nextDouble() * 20.0D;
        flashStart = -100.0D;
        lastFrame = now;
        scanCooldown = 0;
        hasStorm = false;
        dirty = true;
    }

    private static void deactivate() {
        active = false;
        VoidClouds.release();
        hasStorm = false;
        strandsReady = false;
        PRUNES.clear();
        FRAGMENTS.clear();
    }

    private static void scanStorm(ClientLevel level, double x, double z) {
        hasStorm = false;
        if (VoidSkyClient.stormPressure() <= 0.0F) {
            return;
        }
        double best = Double.MAX_VALUE;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof WitherstormEntity storm) || storm.isSmallForm() || !storm.isAlive()) {
                continue;
            }
            double dx = storm.getX() - x;
            double dz = storm.getZ() - z;
            double distance = dx * dx + dz * dz;
            if (distance < best) {
                best = distance;
                stormX = storm.getX();
                stormY = storm.getY();
                stormZ = storm.getZ();
                hasStorm = true;
            }
        }
    }

    private static boolean skyBlocked(Camera camera) {
        return camera.getEntity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS));
    }

    private static void startFlash(double now, float tier) {
        flashStart = now;
        flashSerial++;
        flashAzimuth = RANDOM.nextFloat() * Mth.TWO_PI;
        flashElevation = 0.02F + RANDOM.nextFloat() * 0.22F;
        if (RANDOM.nextFloat() < 0.25F + 0.1F * tier) {
            FLASH_COLOUR[0] = 1.0F;
            FLASH_COLOUR[1] = 0.55F;
            FLASH_COLOUR[2] = 0.25F;
        } else {
            FLASH_COLOUR[0] = 0.62F;
            FLASH_COLOUR[1] = 0.42F;
            FLASH_COLOUR[2] = 1.0F;
        }
    }

    private static float flashEnvelope(double now) {
        double t = now - flashStart;
        if (t < 0.0D || t > 1.4D) {
            return 0.0F;
        }
        double value = pulse(t, 0.0D, 0.09D) * 0.7D + pulse(t, 0.16D, 0.2D) + pulse(t, 0.5D, 0.3D) * 0.45D;
        return (float) Math.min(1.0D, value);
    }

    private static double pulse(double t, double at, double tau) {
        double d = t - at;
        if (d < 0.0D) {
            return 0.0D;
        }
        return Math.min(1.0D, d / 0.04D) * Math.exp(-d / tau);
    }

    public static float lightning(float[] colourOut) {
        if (!active || !VoidSkyConfig.lightning()) {
            return 0.0F;
        }
        float env = flashEnvelope(now()) * VoidSkyConfig.lightningBrightness();
        colourOut[0] = FLASH_COLOUR[0];
        colourOut[1] = FLASH_COLOUR[1];
        colourOut[2] = FLASH_COLOUR[2];
        return env;
    }

    static int flashSerial() {
        return flashSerial;
    }

    static float flashAzimuth() {
        return flashAzimuth;
    }

    static float flashElevation() {
        return flashElevation;
    }

    static float flashLevel(float[] colourOut) {
        return lightning(colourOut);
    }

    static float abyss(double y) {
        return 0.35F + 0.65F * (float) Mth.clamp((y - 24.0D) / 32.0D, 0.0D, 1.0D);
    }

    static float stormLevel() {
        return Mth.clamp(VoidSkyClient.stormPressure() / MAX_PRESSURE, 0.0F, 1.0F);
    }

    public static void onPrune(VoidPrunePayload payload) {
        if (!active || !VoidSkyConfig.strands() || !VoidSkyConfig.pruneSnaps()) {
            return;
        }
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx = payload.x() - camera.x;
        double dz = payload.z() - camera.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float tx = 0.0F;
        float ty = 1.0F;
        float tz = 0.0F;
        if (h > 1.0D) {
            float ce = (float) Math.cos(0.7D);
            tx = ce * (float) (dx / h);
            ty = (float) Math.sin(0.7D);
            tz = ce * (float) (dz / h);
        }
        int best = -1;
        float bestScore = -2.0F;
        for (TimelineRing.Branch b : TREE.branches) {
            if (b.visible < 0.35F || b.fade < 0.5F || !TREE.prunable(b)) {
                continue;
            }
            int last = (int) b.visibleIndex();
            float score = -2.0F;
            for (int i = 0; i <= last; i++) {
                score = Math.max(score, b.pts[i * 3] * tx + b.pts[i * 3 + 1] * ty + b.pts[i * 3 + 2] * tz);
            }
            if (b.depth == 2) {
                score += 0.03F;
            }
            if (score > bestScore) {
                bestScore = score;
                best = b.index;
            }
        }
        if (best < 0) {
            return;
        }
        TimelineRing.Branch branch = TREE.branches[best];
        Random seeded = new Random(payload.seed());
        branch.cutAt = Math.min(0.3F + 0.35F * seeded.nextFloat(), branch.visible * 0.75F);
        branch.prune = TimelineRing.PRUNE_WARN;
        double now = now();
        PRUNES.add(new Prune(best, now, now + Math.max(0, payload.warnTicks()) / 20.0D));
        if (PRUNES.size() > 6) {
            Prune oldest = PRUNES.get(0);
            PRUNES.set(0, new Prune(oldest.branch(), oldest.start(), now));
        }
    }

    public static boolean renderBackdrop(Matrix4f modelView, Matrix4f projection, Camera camera) {
        FogType fluid = camera.getFluidInCamera();
        if (fluid == FogType.LAVA || fluid == FogType.POWDER_SNOW) {
            return true;
        }
        ShaderInstance shader = GameRenderer.getPositionColorShader();
        if (shader == null) {
            return true;
        }
        ensureStatic();
        float[] fog = RenderSystem.getShaderFogColor();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(fog[0], fog[1], fog[2], 1.0F);
        dome.bind();
        dome.drawWithShader(modelView, projection, shader);
        if (active && fluid == FogType.NONE && !skyBlocked(camera)) {
            Vec3 pos = camera.getPosition();
            VoidStars.render(modelView, projection, pos.y);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            if (VoidSkyConfig.lightning()) {
                float env = flashEnvelope(now()) * VoidSkyConfig.lightningBrightness() * 0.35F * abyss(pos.y);
                if (env > 0.001F) {
                    RenderSystem.setShaderColor(FLASH_COLOUR[0] * env, FLASH_COLOUR[1] * env, FLASH_COLOUR[2] * env, 1.0F);
                    MATRIX.set(modelView).rotateY(-flashAzimuth).rotateX(flashElevation);
                    blob.bind();
                    blob.drawWithShader(MATRIX, projection, shader);
                }
            }
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        if (ShaderPacks.inUse()) {
            if (strandsVisible(camera)) {
                drawStrands(modelView, projection, camera, DOME_RADIUS, shader);
            }
            if (cloudsVisible(camera)) {
                VoidClouds.render(modelView, projection, camera.getPosition(), DOME_RADIUS * 0.95F);
            }
        }
        return true;
    }

    private static boolean skyVisible(Camera camera) {
        return active && camera.getFluidInCamera() == FogType.NONE && !skyBlocked(camera);
    }

    private static boolean strandsVisible(Camera camera) {
        return VoidSkyConfig.strands() && skyVisible(camera);
    }

    private static boolean cloudsVisible(Camera camera) {
        return VoidClouds.enabled() && skyVisible(camera);
    }

    public static void renderStrands(Matrix4f modelView, Matrix4f projection, Camera camera) {
        if (ShaderPacks.inUse() || !skyVisible(camera)) {
            return;
        }
        ShaderInstance shader = GameRenderer.getPositionColorShader();
        if (shader == null) {
            return;
        }
        float fogEnd = RenderSystem.getShaderFogEnd();
        float far = Minecraft.getInstance().gameRenderer.getDepthFar();
        float radius = Mth.clamp(Math.min(fogEnd * 0.96F, far * 0.9F), 8.0F, 4000.0F);
        if (VoidSkyConfig.strands()) {
            drawStrands(modelView, projection, camera, radius, shader);
        }
        if (VoidClouds.enabled()) {
            VoidClouds.render(modelView, projection, camera.getPosition(), radius * 0.95F);
        }
    }

    private static void drawStrands(Matrix4f modelView, Matrix4f projection, Camera camera, float radius, ShaderInstance shader) {
        ensureStatic();
        double now = now();
        float dt = (float) Mth.clamp(now - lastFrame, 0.0D, 0.1D);
        lastFrame = now;
        Vec3 pos = camera.getPosition();
        TREE.update(now, 0.25F + 0.75F * Mth.clamp(TimelinePalette.level(pos.x, pos.z) / VoidTier.MAX, 0.0F, 1.0F));
        processPrunes(now);
        if (dirty || now - lastBuild >= REBUILD_SECONDS || TREE.revealing(now)) {
            rebuild(now, pos);
        }
        TimelinePalette.hue(TimelinePalette.level(pos.x, pos.z), HUE);
        float storm = stormLevel();
        float flash = VoidSkyConfig.lightning() ? flashEnvelope(now) * VoidSkyConfig.lightningBrightness() : 0.0F;
        float bright = VoidSkyConfig.strandBrightness() * (1.0F - 0.55F * storm) * abyss(pos.y) * (1.0F + 0.2F * flash);
        float shimmer = 0.93F + 0.07F * (float) Math.sin(now * 0.9D);
        MATRIX.set(modelView).scale(radius);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        if (strandsReady) {
            float m = bright * shimmer;
            RenderSystem.setShaderColor(HUE[0] * m, HUE[1] * m, HUE[2] * m, 1.0F);
            strands.bind();
            strands.drawWithShader(MATRIX, projection, shader);
        }
        updateSparks(dt);
        MeshData mesh = buildOverlay(now);
        if (mesh != null) {
            RenderSystem.setShaderColor(bright, bright, bright, 1.0F);
            overlay.bind();
            overlay.upload(mesh);
            overlay.drawWithShader(MATRIX, projection, shader);
        }
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void ensureStatic() {
        if (dome != null) {
            return;
        }
        strandBytes = new ByteBufferBuilder(262144);
        overlayBytes = new ByteBufferBuilder(65536);
        strands = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        overlay = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        blob = new VertexBuffer(VertexBuffer.Usage.STATIC);
        VertexBuffer domeBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(65536)) {
            BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            buildDome(b);
            domeBuffer.bind();
            domeBuffer.upload(b.buildOrThrow());
            b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            buildBlob(b);
            blob.bind();
            blob.upload(b.buildOrThrow());
            VertexBuffer.unbind();
        }
        dome = domeBuffer;
    }

    private static void buildDome(BufferBuilder b) {
        int lon = 24;
        for (int i = 0; i < DOME_ELEVATIONS.length - 1; i++) {
            float e0 = DOME_ELEVATIONS[i] * Mth.DEG_TO_RAD;
            float e1 = DOME_ELEVATIONS[i + 1] * Mth.DEG_TO_RAD;
            for (int j = 0; j < lon; j++) {
                float a0 = j * Mth.TWO_PI / lon;
                float a1 = (j + 1) * Mth.TWO_PI / lon;
                domeVertex(b, e0, a0);
                domeVertex(b, e0, a1);
                domeVertex(b, e1, a1);
                domeVertex(b, e1, a0);
            }
        }
    }

    private static void domeVertex(BufferBuilder b, float elevation, float azimuth) {
        float ce = Mth.cos(elevation);
        float x = ce * Mth.sin(azimuth) * DOME_RADIUS;
        float y = Mth.sin(elevation) * DOME_RADIUS;
        float z = -ce * Mth.cos(azimuth) * DOME_RADIUS;
        float g = 1.0F;
        if (elevation > 0.0F) {
            g = 1.0F - 0.6F * (float) Math.pow(Mth.sin(elevation), 0.55D);
        }
        float k = 1.0F - g;
        b.addVertex(x, y, z).setColor(g - 0.05F * k, g - 0.08F * k, g, 1.0F);
    }

    private static void buildBlob(BufferBuilder b) {
        int segments = 20;
        float h = 0.8F;
        float v = 0.28F;
        for (int k = 0; k < segments; k++) {
            float a0 = k * Mth.TWO_PI / segments;
            float a1 = (k + 1) * Mth.TWO_PI / segments;
            blobVertex(b, 0.0F, 0.0F, 1.0F);
            blobVertex(b, Mth.cos(a0) * h * 0.4F, Mth.sin(a0) * v * 0.4F, 0.5F);
            blobVertex(b, Mth.cos(a0) * h * 0.4F, Mth.sin(a0) * v * 0.4F, 0.5F);
            blobVertex(b, Mth.cos(a1) * h * 0.4F, Mth.sin(a1) * v * 0.4F, 0.5F);
            blobVertex(b, Mth.cos(a0) * h * 0.4F, Mth.sin(a0) * v * 0.4F, 0.5F);
            blobVertex(b, Mth.cos(a0) * h, Mth.sin(a0) * v, 0.0F);
            blobVertex(b, Mth.cos(a1) * h, Mth.sin(a1) * v, 0.0F);
            blobVertex(b, Mth.cos(a1) * h * 0.4F, Mth.sin(a1) * v * 0.4F, 0.5F);
        }
    }

    private static void blobVertex(BufferBuilder b, float x, float y, float alpha) {
        float len = (float) Math.sqrt(x * x + y * y + 1.0F);
        b.addVertex(x / len * DOME_RADIUS, y / len * DOME_RADIUS, -1.0F / len * DOME_RADIUS).setColor(1.0F, 1.0F, 1.0F, alpha);
    }

    private static void rebuild(double now, Vec3 camera) {
        float dx = 0.0F;
        float dy = 0.0F;
        float dz = 0.0F;
        float bend = 0.0F;
        float storm = stormLevel();
        if (hasStorm && storm > 0.0F) {
            double ox = stormX - camera.x;
            double oz = stormZ - camera.z;
            double h = Math.sqrt(ox * ox + oz * oz);
            if (h > 1.0D) {
                float elevation = (float) Mth.clamp(Math.atan2(stormY - camera.y, h), 0.12D, 0.6D);
                float ce = Mth.cos(elevation);
                dx = ce * (float) (ox / h);
                dy = Mth.sin(elevation);
                dz = ce * (float) (oz / h);
                bend = storm;
            }
        }
        TREE.solve(now, dx, dy, dz, bend);
        BufferBuilder b = new BufferBuilder(strandBytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float braid = (float) (now * 0.35D % (Math.PI * 2.0D));
        for (TimelineRing.Branch branch : TREE.branches) {
            if (branch.visible <= 0.002F || branch.fade <= 0.002F) {
                continue;
            }
            float to = branch.visibleIndex();
            float i = branch.intensity * branch.fade;
            float ws = branch.width;
            int mode = branch.ring() && branch.visible >= 0.999F ? MODE_RING : MODE_BRANCH;
            if (branch.kind == TimelineRing.KIND_SPINE) {
                strip(b, branch.pts, branch.segs, 0.0F, to, HALO, 0.0F, true, 0.0F, 0.0F,
                        0.07F * i, 0.062F * i, 0.055F * i, mode);
            }
            strip(b, branch.pts, branch.segs, 0.0F, to, GLOW * branch.glow, branch.taper, true, 0.0F, 0.0F,
                    0.34F * i, 0.31F * i, 0.27F * i, mode);
            float core = Math.max(MIN_CORE, CORE * ws);
            float spacing = CORE * 3.2F * ws;
            for (int k = 0; k < branch.threads; k++) {
                float offset = spacing * (k - (branch.threads - 1) * 0.5F);
                strip(b, branch.pts, branch.segs, 0.0F, to, core, branch.taper * 0.9F, false, offset, branch.threadPhase + k * 1.7F + braid,
                        i, 0.96F * i, 0.88F * i, mode);
            }
            float scar = TREE.scarGlow(branch, now);
            if (scar > 0.01F) {
                sample(branch.pts, branch.segs, to, 0);
                dot(b, SX[0], SX[1], SX[2], 0.012F * Math.max(0.5F, ws), scar, 0.45F * scar, 0.25F * scar);
            }
        }
        MeshData mesh = b.build();
        if (mesh == null) {
            strandsReady = false;
        } else {
            strands.bind();
            strands.upload(mesh);
            VertexBuffer.unbind();
            strandsReady = true;
        }
        lastBuild = now;
        dirty = false;
    }

    private static MeshData buildOverlay(double now) {
        BufferBuilder b = new BufferBuilder(overlayBytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float pr = HUE[0] * 0.55F + 0.45F;
        float pg = HUE[1] * 0.55F + 0.45F;
        float pb = HUE[2] * 0.55F + 0.45F;
        for (int k = 0; k < PULSES; k++) {
            if (PULSE_BRANCH[k] < 0) {
                PULSE_BRANCH[k] = pickBranch();
                if (PULSE_BRANCH[k] < 0) {
                    continue;
                }
                PULSE_START[k] = Math.max(PULSE_START[k], now);
                PULSE_SPEED[k] = 0.12F + RANDOM.nextFloat() * 0.18F;
            }
            if (now < PULSE_START[k]) {
                continue;
            }
            TimelineRing.Branch branch = TREE.branches[PULSE_BRANCH[k]];
            float head = (float) (now - PULSE_START[k]) * PULSE_SPEED[k];
            float visibleLength = branch.visible * branch.length;
            if (branch.visible < 0.05F || branch.prune == TimelineRing.PRUNE_WARN || head - PULSE_LENGTH > visibleLength) {
                PULSE_BRANCH[k] = pickBranch();
                PULSE_START[k] = now + RANDOM.nextDouble() * 2.5D;
                PULSE_SPEED[k] = 0.12F + RANDOM.nextFloat() * 0.18F;
                continue;
            }
            float from = Math.max(0.0F, head - PULSE_LENGTH) / branch.length * branch.segs;
            float to = Math.min(head, visibleLength) / branch.length * branch.segs;
            float a = 0.5F * branch.fade * branch.intensity;
            strip(b, branch.pts, branch.segs, from, to, GLOW * branch.width * 0.4F, branch.taper, true, 0.0F, 0.0F,
                    pr * a, pg * a, pb * a, MODE_PULSE);
        }
        for (Prune prune : PRUNES) {
            TimelineRing.Branch branch = TREE.branches[prune.branch()];
            double span = Math.max(0.05D, prune.snapAt() - prune.start());
            float q = (float) Mth.clamp((now - prune.start()) / span, 0.0D, 1.0D);
            double t = now - prune.start();
            float beat = 0.55F + 0.45F * (float) Math.sin(Mth.TWO_PI * (1.5D * t + 3.5D * q * t));
            float level = (0.5F + 0.5F * q) * beat;
            float from = branch.cutAt * branch.segs;
            emitPruned(b, branch, from, level);
            sample(branch.pts, branch.segs, from, 0);
            dot(b, SX[0], SX[1], SX[2], 0.02F * (0.6F + 0.4F * q), level, 0.6F * level, 0.25F * level);
        }
        Iterator<Fragment> fragments = FRAGMENTS.iterator();
        while (fragments.hasNext()) {
            Fragment fragment = fragments.next();
            float age = (float) (now - fragment.start());
            if (age >= FRAGMENT_SECONDS) {
                fragments.remove();
                continue;
            }
            if (age < FLARE_SECONDS) {
                float flare = 1.0F - age / FLARE_SECONDS;
                float f2 = flare * flare;
                dot(b, fragment.cx(), fragment.cy(), fragment.cz(), 0.03F + 0.06F * (1.0F - flare), f2, 0.75F * f2, 0.45F * f2);
            }
            float life = 1.0F - age / (float) FRAGMENT_SECONDS;
            float alpha = life * (float) Math.sqrt(life);
            float theta = 0.05F * age + 0.11F * age * age;
            ROTATION.identity().rotateAxis(-theta, fragment.ax(), 0.0F, fragment.az());
            SPIN.identity().rotateAxis(0.18F * age, fragment.cx(), fragment.cy(), fragment.cz());
            ROTATION.mul(SPIN);
            float fr = Mth.lerp(1.0F - life, 1.0F, 0.8F) * alpha;
            float fg = Mth.lerp(1.0F - life, 0.55F, 0.2F) * alpha;
            float fb = Mth.lerp(1.0F - life, 0.15F, 0.08F) * alpha;
            for (int s = 0; s < fragment.strips().length; s++) {
                float[] pts = fragment.strips()[s];
                int n = pts.length / 3;
                for (int p = 0; p < n; p++) {
                    VEC.set(pts[p * 3], pts[p * 3 + 1], pts[p * 3 + 2]).rotate(ROTATION);
                    FX[p * 3] = VEC.x;
                    FX[p * 3 + 1] = VEC.y;
                    FX[p * 3 + 2] = VEC.z;
                }
                float w = fragment.widths()[s];
                strip(b, FX, n - 1, 0.0F, n - 1, GLOW * w * 1.2F, 0.4F, true, 0.0F, 0.0F, fr * 0.6F, fg * 0.6F, fb * 0.6F, MODE_FLAT);
                strip(b, FX, n - 1, 0.0F, n - 1, Math.max(MIN_CORE, CORE * w * 1.4F), 0.4F, false, 0.0F, 0.0F, fr, fg, fb, MODE_FLAT);
            }
        }
        for (int k = 0; k < SPARKS; k++) {
            float life = SPARK_LIFE[k];
            if (life <= 0.0F) {
                continue;
            }
            float f = life / SPARK_MAX[k];
            int i = k * 3;
            dot(b, SPARK_POS[i], SPARK_POS[i + 1], SPARK_POS[i + 2], 0.0045F * (0.5F + 0.5F * f),
                    f, (0.25F + 0.45F * f) * f, 0.25F * f * f);
        }
        return b.build();
    }

    private static void emitPruned(BufferBuilder b, TimelineRing.Branch branch, float from, float level) {
        float to = branch.visibleIndex();
        if (to > from) {
            strip(b, branch.pts, branch.segs, from, to, GLOW * branch.width * 1.6F, 0.4F, true, 0.0F, 0.0F,
                    0.9F * level, 0.45F * level, 0.1F * level, MODE_BRANCH);
            strip(b, branch.pts, branch.segs, from, to, Math.max(MIN_CORE, CORE * branch.width * 2.0F), 0.4F, false, 0.0F, 0.0F,
                    level, 0.72F * level, 0.38F * level, MODE_BRANCH);
        }
        float cut = from / branch.segs;
        for (int child : branch.children) {
            TimelineRing.Branch c = TREE.branches[child];
            if (c.attach >= cut && c.visible > 0.002F) {
                emitPruned(b, c, 0.0F, level);
            }
        }
    }

    private static void processPrunes(double now) {
        Iterator<Prune> it = PRUNES.iterator();
        while (it.hasNext()) {
            Prune prune = it.next();
            if (now < prune.snapAt()) {
                continue;
            }
            it.remove();
            snap(TREE.branches[prune.branch()], now);
        }
    }

    private static void snap(TimelineRing.Branch branch, double now) {
        float from = branch.cutAt * branch.segs;
        sample(branch.pts, branch.segs, from, 0);
        float cx = SX[0];
        float cy = SX[1];
        float cz = SX[2];
        List<float[]> strips = new ArrayList<>();
        List<Float> widths = new ArrayList<>();
        capture(branch, from, strips, widths);
        float ax = -cz;
        float az = cx;
        float len = (float) Math.sqrt(ax * ax + az * az);
        if (len < 1.0E-4F) {
            ax = 1.0F;
            az = 0.0F;
            len = 1.0F;
        }
        float[] w = new float[widths.size()];
        for (int i = 0; i < w.length; i++) {
            w[i] = widths.get(i);
        }
        FRAGMENTS.add(new Fragment(strips.toArray(new float[0][]), w, cx, cy, cz, ax / len, az / len, now));
        if (FRAGMENTS.size() > 8) {
            FRAGMENTS.remove(0);
        }
        spawnSparks(cx, cy, cz, 36);
        TREE.cut(branch, now, 150.0F + RANDOM.nextFloat() * 120.0F);
        dirty = true;
    }

    private static void capture(TimelineRing.Branch branch, float from, List<float[]> strips, List<Float> widths) {
        float to = branch.visibleIndex();
        if (to - from > 0.05F) {
            int n = 0;
            n = sample(branch.pts, branch.segs, from, n);
            for (int k = (int) Math.floor(from) + 1; k < to; k++) {
                n = sample(branch.pts, branch.segs, k, n);
            }
            n = sample(branch.pts, branch.segs, to, n);
            float[] copy = new float[n * 3];
            System.arraycopy(SX, 0, copy, 0, n * 3);
            strips.add(copy);
            widths.add(branch.width);
        }
        float cut = from / branch.segs;
        for (int child : branch.children) {
            TimelineRing.Branch c = TREE.branches[child];
            if (c.attach >= cut && c.visible > 0.002F) {
                capture(c, 0.0F, strips, widths);
            }
        }
    }

    private static void spawnSparks(float x, float y, float z, int count) {
        float ex = -z;
        float ez = x;
        float el = (float) Math.sqrt(ex * ex + ez * ez);
        if (el < 1.0E-4F) {
            ex = 1.0F;
            ez = 0.0F;
            el = 1.0F;
        }
        ex /= el;
        ez /= el;
        float fx = y * ez;
        float fy = z * ex - x * ez;
        float fz = -y * ex;
        for (int n = 0; n < count; n++) {
            int k = nextSpark;
            nextSpark = (nextSpark + 1) % SPARKS;
            float angle = RANDOM.nextFloat() * Mth.TWO_PI;
            float speed = 0.06F + RANDOM.nextFloat() * 0.22F;
            float c = Mth.cos(angle) * speed;
            float s = Mth.sin(angle) * speed;
            int i = k * 3;
            SPARK_POS[i] = x;
            SPARK_POS[i + 1] = y;
            SPARK_POS[i + 2] = z;
            SPARK_VEL[i] = ex * c + fx * s;
            SPARK_VEL[i + 1] = fy * s;
            SPARK_VEL[i + 2] = ez * c + fz * s;
            SPARK_MAX[k] = 0.7F + RANDOM.nextFloat() * 1.1F;
            SPARK_LIFE[k] = SPARK_MAX[k];
        }
    }

    private static void updateSparks(float dt) {
        if (dt <= 0.0F) {
            return;
        }
        float drag = (float) Math.pow(0.35D, dt);
        for (int k = 0; k < SPARKS; k++) {
            if (SPARK_LIFE[k] <= 0.0F) {
                continue;
            }
            SPARK_LIFE[k] -= dt;
            int i = k * 3;
            float x = SPARK_POS[i];
            float y = SPARK_POS[i + 1];
            float z = SPARK_POS[i + 2];
            float vx = SPARK_VEL[i];
            float vy = SPARK_VEL[i + 1] - 0.12F * dt * (1.0F - y * y);
            float vz = SPARK_VEL[i + 2];
            vx += x * y * 0.12F * dt;
            vz += z * y * 0.12F * dt;
            x += vx * dt;
            y += vy * dt;
            z += vz * dt;
            float len = (float) Math.sqrt(x * x + y * y + z * z);
            if (len < 1.0E-4F) {
                SPARK_LIFE[k] = 0.0F;
                continue;
            }
            x /= len;
            y /= len;
            z /= len;
            float d = vx * x + vy * y + vz * z;
            SPARK_POS[i] = x;
            SPARK_POS[i + 1] = y;
            SPARK_POS[i + 2] = z;
            SPARK_VEL[i] = (vx - x * d) * drag;
            SPARK_VEL[i + 1] = (vy - y * d) * drag;
            SPARK_VEL[i + 2] = (vz - z * d) * drag;
        }
    }

    private static int pickBranch() {
        float total = 0.0F;
        for (TimelineRing.Branch b : TREE.branches) {
            if (b.visible > 0.3F && b.fade > 0.5F) {
                total += b.length * (b.depth <= 1 ? 2.0F : 1.0F);
            }
        }
        if (total <= 0.0F) {
            return -1;
        }
        float pick = RANDOM.nextFloat() * total;
        for (TimelineRing.Branch b : TREE.branches) {
            if (b.visible > 0.3F && b.fade > 0.5F) {
                pick -= b.length * (b.depth <= 1 ? 2.0F : 1.0F);
                if (pick <= 0.0F) {
                    return b.index;
                }
            }
        }
        return -1;
    }

    private static int sample(float[] pts, int segs, float fi, int n) {
        float clamped = Mth.clamp(fi, 0.0F, segs);
        int i = Math.min((int) clamped, segs - 1);
        float f = clamped - i;
        int a = i * 3;
        int c = a + 3;
        int o = n * 3;
        SX[o] = pts[a] + (pts[c] - pts[a]) * f;
        SX[o + 1] = pts[a + 1] + (pts[c + 1] - pts[a + 1]) * f;
        SX[o + 2] = pts[a + 2] + (pts[c + 2] - pts[a + 2]) * f;
        SS[n] = clamped / segs;
        return n + 1;
    }

    private static float profile(int mode, float u) {
        return switch (mode) {
            case MODE_PULSE -> u < 0.8F ? (u / 0.8F) * (u / 0.8F) : (1.0F - u) / 0.2F;
            case MODE_FLAT -> Math.min(1.0F, Math.min(u / 0.06F, (1.0F - u) / 0.06F));
            case MODE_RING -> 1.0F;
            default -> Math.min(1.0F, (1.0F - u) / 0.12F);
        };
    }

    private static void strip(BufferBuilder b, float[] pts, int segs, float from, float to, float halfWidth, float taper, boolean soft,
                              float offset, float offPhase, float r, float g, float bl, int mode) {
        if (to - from < 0.02F || segs < 1) {
            return;
        }
        int n = sample(pts, segs, from, 0);
        for (int k = (int) Math.floor(from) + 1; k < to; k++) {
            n = sample(pts, segs, k, n);
        }
        n = sample(pts, segs, to, n);
        float span = SS[n - 1] - SS[0];
        if (span <= 0.0F) {
            return;
        }
        float sx = 0.0F;
        float sy = 0.0F;
        float sz = 0.0F;
        float plx = 0.0F;
        float ply = 0.0F;
        float plz = 0.0F;
        float pcx = 0.0F;
        float pcy = 0.0F;
        float pcz = 0.0F;
        float prx = 0.0F;
        float pry = 0.0F;
        float prz = 0.0F;
        float pa = 0.0F;
        for (int j = 0; j < n; j++) {
            int o = j * 3;
            int a0 = Math.max(0, j - 1) * 3;
            int a1 = Math.min(n - 1, j + 1) * 3;
            float px = SX[o];
            float py = SX[o + 1];
            float pz = SX[o + 2];
            float tx = SX[a1] - SX[a0];
            float ty = SX[a1 + 1] - SX[a0 + 1];
            float tz = SX[a1 + 2] - SX[a0 + 2];
            float cx = py * tz - pz * ty;
            float cy = pz * tx - px * tz;
            float cz = px * ty - py * tx;
            float len = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
            if (len > 1.0E-7F) {
                sx = cx / len;
                sy = cy / len;
                sz = cz / len;
            }
            float s = SS[j];
            float u = (s - SS[0]) / span;
            float w = halfWidth * (1.0F - taper * s);
            float alpha = Math.max(0.0F, profile(mode, u));
            float off = offset == 0.0F ? 0.0F : offset * (1.0F - 0.7F * s) * (0.65F + 0.35F * Mth.sin(s * 23.0F + offPhase));
            float mx = px + sx * off;
            float my = py + sy * off;
            float mz = pz + sz * off;
            float lx = mx - sx * w;
            float ly = my - sy * w;
            float lz = mz - sz * w;
            float rx = mx + sx * w;
            float ry = my + sy * w;
            float rz = mz + sz * w;
            if (j > 0) {
                if (soft) {
                    b.addVertex(plx, ply, plz).setColor(r, g, bl, 0.0F);
                    b.addVertex(pcx, pcy, pcz).setColor(r, g, bl, pa);
                    b.addVertex(mx, my, mz).setColor(r, g, bl, alpha);
                    b.addVertex(lx, ly, lz).setColor(r, g, bl, 0.0F);
                    b.addVertex(pcx, pcy, pcz).setColor(r, g, bl, pa);
                    b.addVertex(prx, pry, prz).setColor(r, g, bl, 0.0F);
                    b.addVertex(rx, ry, rz).setColor(r, g, bl, 0.0F);
                    b.addVertex(mx, my, mz).setColor(r, g, bl, alpha);
                } else {
                    b.addVertex(plx, ply, plz).setColor(r, g, bl, pa);
                    b.addVertex(prx, pry, prz).setColor(r, g, bl, pa);
                    b.addVertex(rx, ry, rz).setColor(r, g, bl, alpha);
                    b.addVertex(lx, ly, lz).setColor(r, g, bl, alpha);
                }
            }
            plx = lx;
            ply = ly;
            plz = lz;
            pcx = mx;
            pcy = my;
            pcz = mz;
            prx = rx;
            pry = ry;
            prz = rz;
            pa = alpha;
        }
    }

    private static void dot(BufferBuilder b, float x, float y, float z, float radius, float r, float g, float bl) {
        float ex = -z;
        float ez = x;
        float el = (float) Math.sqrt(ex * ex + ez * ez);
        if (el < 1.0E-4F) {
            ex = 1.0F;
            ez = 0.0F;
            el = 1.0F;
        }
        ex /= el;
        ez /= el;
        float fx = y * ez;
        float fy = z * ex - x * ez;
        float fz = -y * ex;
        int segments = 8;
        for (int k = 0; k < segments; k++) {
            float a0 = k * Mth.TWO_PI / segments;
            float a1 = (k + 1) * Mth.TWO_PI / segments;
            float c0 = Mth.cos(a0) * radius;
            float s0 = Mth.sin(a0) * radius;
            float c1 = Mth.cos(a1) * radius;
            float s1 = Mth.sin(a1) * radius;
            b.addVertex(x, y, z).setColor(r, g, bl, 1.0F);
            b.addVertex(x + ex * c0 + fx * s0, y + fy * s0, z + ez * c0 + fz * s0).setColor(r, g, bl, 0.0F);
            b.addVertex(x + ex * c0 + fx * s0, y + fy * s0, z + ez * c0 + fz * s0).setColor(r, g, bl, 0.0F);
            b.addVertex(x + ex * c1 + fx * s1, y + fy * s1, z + ez * c1 + fz * s1).setColor(r, g, bl, 0.0F);
        }
    }
}
