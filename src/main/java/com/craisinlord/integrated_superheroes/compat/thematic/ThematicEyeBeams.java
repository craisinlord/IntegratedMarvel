package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.ability.effect.client.HeatVisionUtil;
import bond.thematic.api.rendering.geometry.Geometry;
import bond.thematic.api.rendering.geometry.GeometrySink;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class ThematicEyeBeams {
    private static final float CORE_WIDTH = 0.025F;
    private static final float GLOW_SIZE = 0.25F;
    private static final int HALO_LAYERS = 8;

    private ThematicEyeBeams() {
    }

    static void draw(PoseStack poses, MultiBufferSource.BufferSource buffers, Vec3 from, Vec3 to, int core, int glow, float scale,
                     float opacity, float time) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.05D) {
            return;
        }
        Vec3 direction = delta.scale(1.0D / length);
        RenderType type = HeatVisionUtil.getWaterSafeLightning();
        GeometrySink sink = Geometry.sink(buffers.getBuffer(type));
        if (sink == null) {
            throw new IllegalStateException("Thematic has no geometry backend");
        }
        poses.pushPose();
        try {
            poses.translate(from.x, from.y, from.z);
            poses.mulPose(Axis.YP.rotation((float) Math.atan2(direction.x, direction.z)));
            poses.mulPose(Axis.XP.rotation((float) -Math.asin(Mth.clamp(direction.y, -1.0D, 1.0D))));
            float[] coreRgba = HeatVisionUtil.colorToFloat(0xFF000000 | core & 0xFFFFFF);
            float[] glowRgb = HeatVisionUtil.colorToFloat(0xFF000000 | glow & 0xFFFFFF);
            HeatVisionUtil.drawLayeredBeam(poses, sink, (float) length, CORE_WIDTH * scale, GLOW_SIZE * scale, HALO_LAYERS, coreRgba, glowRgb,
                    opacity, 0.01F, 0.005F, time);
        } finally {
            poses.popPose();
        }
        buffers.endBatch(type);
    }
}
