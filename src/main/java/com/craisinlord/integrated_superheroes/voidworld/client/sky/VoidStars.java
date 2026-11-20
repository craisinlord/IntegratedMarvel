package com.craisinlord.integrated_superheroes.voidworld.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

final class VoidStars {
    private static final int STARS = 700;
    private static final int GROUPS = 3;
    private static final long SEED = 0x57A125L;
    private static final float RADIUS = 99.0F;
    private static final float SPIN = Mth.TWO_PI / 4500.0F;
    private static final float[] TWINKLE_FREQ = {0.53F, 0.81F, 1.17F};
    private static final float[] TWINKLE_PHASE = {0.0F, 2.1F, 4.3F};
    private static final float AXIS_X = 0.0F;
    private static final float AXIS_Y = Mth.sin(TimelineRing.CENTER_ELEVATION);
    private static final float AXIS_Z = -Mth.cos(TimelineRing.CENTER_ELEVATION);
    private static final Matrix4f MATRIX = new Matrix4f();

    private static VertexBuffer[] groups;

    private VoidStars() {
    }

    private static void ensure() {
        if (groups != null) {
            return;
        }
        Random r = new Random(SEED);
        groups = new VertexBuffer[GROUPS];
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(STARS * 16 * 16 / GROUPS + 4096)) {
            for (int g = 0; g < GROUPS; g++) {
                BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                int placed = 0;
                int attempts = 0;
                while (placed < STARS / GROUPS && attempts < STARS * 40) {
                    attempts++;
                    float x = r.nextFloat() * 2.0F - 1.0F;
                    float y = r.nextFloat() * 2.0F - 1.0F;
                    float z = r.nextFloat() * 2.0F - 1.0F;
                    float len = (float) Math.sqrt(x * x + y * y + z * z);
                    if (len < 0.05F || len > 1.0F) {
                        continue;
                    }
                    x /= len;
                    y /= len;
                    z /= len;
                    float angle = (float) Math.acos(Mth.clamp(x * AXIS_X + y * AXIS_Y + z * AXIS_Z, -1.0F, 1.0F));
                    float edge = TimelineRing.smooth((TimelineRing.RADIUS - 0.06F - angle) / 0.3F);
                    float low = TimelineRing.smooth(((float) Math.asin(y) - 0.24F) / 0.3F);
                    float fade = edge * low;
                    if (fade < 0.02F || r.nextFloat() > 0.3F + 0.7F * fade) {
                        continue;
                    }
                    float mag = r.nextFloat();
                    float bright = (0.18F + 0.82F * mag * mag * mag) * fade;
                    float size = 0.0022F + 0.0045F * mag * mag;
                    float tint = r.nextFloat();
                    float cr;
                    float cg;
                    float cb;
                    if (tint < 0.12F) {
                        cr = 1.0F;
                        cg = 0.82F;
                        cb = 0.55F;
                    } else if (tint < 0.55F) {
                        cr = 0.78F;
                        cg = 0.7F;
                        cb = 1.0F;
                    } else {
                        cr = 0.93F;
                        cg = 0.9F;
                        cb = 1.0F;
                    }
                    star(b, x, y, z, size, cr * bright, cg * bright, cb * bright);
                    placed++;
                }
                VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                buffer.bind();
                buffer.upload(b.buildOrThrow());
                groups[g] = buffer;
            }
            VertexBuffer.unbind();
        }
    }

    private static void star(BufferBuilder b, float x, float y, float z, float size, float r, float g, float bl) {
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
        float cx = x * RADIUS;
        float cy = y * RADIUS;
        float cz = z * RADIUS;
        float s = size * RADIUS;
        for (int k = 0; k < 4; k++) {
            float a0 = k * Mth.HALF_PI;
            float a1 = (k + 1) * Mth.HALF_PI;
            float c0 = Mth.cos(a0) * s;
            float s0 = Mth.sin(a0) * s;
            float c1 = Mth.cos(a1) * s;
            float s1 = Mth.sin(a1) * s;
            b.addVertex(cx, cy, cz).setColor(r, g, bl, 1.0F);
            b.addVertex(cx + ex * c0 + fx * s0, cy + fy * s0, cz + ez * c0 + fz * s0).setColor(r, g, bl, 0.0F);
            b.addVertex(cx + ex * c0 + fx * s0, cy + fy * s0, cz + ez * c0 + fz * s0).setColor(r, g, bl, 0.0F);
            b.addVertex(cx + ex * c1 + fx * s1, cy + fy * s1, cz + ez * c1 + fz * s1).setColor(r, g, bl, 0.0F);
        }
    }

    static void render(Matrix4f modelView, Matrix4f projection, double cameraY) {
        if (!VoidSkyConfig.stars()) {
            return;
        }
        float storm = TimelineSky.stormLevel();
        float level = (1.0F - TimelineRing.smooth((storm - 0.25F) / 0.35F)) * TimelineSky.abyss(cameraY) * VoidSkyConfig.starBrightness();
        if (level <= 0.005F) {
            return;
        }
        ShaderInstance shader = GameRenderer.getPositionColorShader();
        if (shader == null) {
            return;
        }
        ensure();
        double now = Util.getMillis() / 1000.0D;
        MATRIX.set(modelView).rotate((float) (now * SPIN % Mth.TWO_PI), AXIS_X, AXIS_Y, AXIS_Z);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        for (int g = 0; g < GROUPS; g++) {
            float twinkle = 0.72F + 0.28F * Mth.sin((float) (now * TWINKLE_FREQ[g] % 1000.0D) + TWINKLE_PHASE[g]);
            float k = level * twinkle;
            RenderSystem.setShaderColor(k, k, k, 1.0F);
            groups[g].bind();
            groups[g].drawWithShader(MATRIX, projection, shader);
        }
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
