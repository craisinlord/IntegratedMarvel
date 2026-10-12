package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.antarchy.content.client.renderer.RoyalBeamRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class MindBeamRenderEvents {
    private static final ResourceLocation OUTER = ResourceLocation.fromNamespaceAndPath("antarchy", "textures/entity/king/fire_beam_outer.png");
    private static final ResourceLocation INNER = ResourceLocation.fromNamespaceAndPath("antarchy", "textures/entity/king/fire_beam_inner.png");
    private static final ResourceLocation END_ONE = ResourceLocation.fromNamespaceAndPath("antarchy", "textures/entity/king/fire_beam_end_1.png");
    private static final ResourceLocation END_TWO = ResourceLocation.fromNamespaceAndPath("antarchy", "textures/entity/king/fire_beam_end_2.png");
    private static final ResourceLocation QUEEN_OUTER = ResourceLocation.fromNamespaceAndPath("antarchy", "textures/entity/queen/queen_purple_beam_outer.png");
    private static final ResourceLocation QUEEN_INNER = ResourceLocation.fromNamespaceAndPath("antarchy", "textures/entity/queen/queen_purple_beam_inner.png");
    private static final ResourceLocation QUEEN_END_ONE = ResourceLocation.fromNamespaceAndPath("antarchy", "textures/entity/queen/queen_purple_beam_end_1.png");
    private static final ResourceLocation QUEEN_END_TWO = ResourceLocation.fromNamespaceAndPath("antarchy", "textures/entity/queen/queen_purple_beam_end_2.png");
    private static final ResourceLocation QUEEN_RED_OUTER = ResourceLocation.fromNamespaceAndPath("antarchy", "textures/entity/queen/queen_red_beam_outer.png");
    private static final ResourceLocation QUEEN_RED_INNER = ResourceLocation.fromNamespaceAndPath("antarchy", "textures/entity/queen/queen_red_beam_inner.png");
    private static final ResourceLocation QUEEN_RED_END_ONE = ResourceLocation.fromNamespaceAndPath("antarchy", "textures/entity/queen/queen_red_beam_end_1.png");
    private static final ResourceLocation QUEEN_RED_END_TWO = ResourceLocation.fromNamespaceAndPath("antarchy", "textures/entity/queen/queen_red_beam_end_2.png");
    private static final ResourceLocation RAINBOW_OUTER = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "textures/entity/infinity_beam/outer.png");
    private static final ResourceLocation RAINBOW_INNER = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "textures/entity/infinity_beam/inner.png");
    private static final ResourceLocation RAINBOW_END_ONE = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "textures/entity/infinity_beam/end_1.png");
    private static final ResourceLocation RAINBOW_END_TWO = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "textures/entity/infinity_beam/end_2.png");
    public static final int QUEEN_RED_BEAM = 3;
    private static final int INFINITY_BEAM = 1;
    private static final double RAINBOW_SEGMENT_LENGTH = 0.5D;
    private static final int RAINBOW_MAX_SEGMENTS = 256;
    private static final float RAINBOW_BAND_LENGTH = 6.0F;
    private static final float RAINBOW_SPEED = 0.06F;

    private static final RenderType OUTER_TYPE = RoyalBeamRenderTypes.beam(OUTER);
    private static final RenderType INNER_TYPE = RoyalBeamRenderTypes.beam(INNER);
    private static final RenderType END_ONE_TYPE = RoyalBeamRenderTypes.beam(END_ONE);
    private static final RenderType END_TWO_TYPE = RoyalBeamRenderTypes.beam(END_TWO);
    private static final RenderType QUEEN_OUTER_TYPE = RoyalBeamRenderTypes.beam(QUEEN_OUTER);
    private static final RenderType QUEEN_INNER_TYPE = RoyalBeamRenderTypes.beam(QUEEN_INNER);
    private static final RenderType QUEEN_END_ONE_TYPE = RoyalBeamRenderTypes.beam(QUEEN_END_ONE);
    private static final RenderType QUEEN_END_TWO_TYPE = RoyalBeamRenderTypes.beam(QUEEN_END_TWO);
    private static final RenderType QUEEN_RED_OUTER_TYPE = RoyalBeamRenderTypes.beam(QUEEN_RED_OUTER);
    private static final RenderType QUEEN_RED_INNER_TYPE = RoyalBeamRenderTypes.beam(QUEEN_RED_INNER);
    private static final RenderType QUEEN_RED_END_ONE_TYPE = RoyalBeamRenderTypes.beam(QUEEN_RED_END_ONE);
    private static final RenderType QUEEN_RED_END_TWO_TYPE = RoyalBeamRenderTypes.beam(QUEEN_RED_END_TWO);
    private static final RenderType RAINBOW_OUTER_TYPE = RoyalBeamRenderTypes.beam(RAINBOW_OUTER);
    private static final RenderType RAINBOW_INNER_TYPE = RoyalBeamRenderTypes.beam(RAINBOW_INNER);
    private static final RenderType RAINBOW_END_ONE_TYPE = RoyalBeamRenderTypes.beam(RAINBOW_END_ONE);
    private static final RenderType RAINBOW_END_TWO_TYPE = RoyalBeamRenderTypes.beam(RAINBOW_END_TWO);

    private MindBeamRenderEvents() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || Minecraft.getInstance().level == null) {
            return;
        }
        var beams = MindBeamClientState.beams();
        if (beams.isEmpty()) {
            return;
        }
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        PoseStack poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        for (var beam : beams.values()) {
            Vec3 start = new Vec3(beam.startX(), beam.startY(), beam.startZ()).subtract(camera);
            Vec3 end = new Vec3(beam.endX(), beam.endY(), beam.endZ()).subtract(camera);
            Vec3 delta = end.subtract(start);
            double length = delta.length();
            if (length < 0.05D) {
                continue;
            }
            Vec3 direction = delta.scale(1.0D / length);
            boolean power = beam.kind() == 0;
            boolean red = beam.kind() == QUEEN_RED_BEAM;
            boolean rainbow = beam.kind() == INFINITY_BEAM;
            RenderType inner = rainbow ? RAINBOW_INNER_TYPE : red ? QUEEN_RED_INNER_TYPE : power ? QUEEN_INNER_TYPE : INNER_TYPE;
            RenderType outer = rainbow ? RAINBOW_OUTER_TYPE : red ? QUEEN_RED_OUTER_TYPE : power ? QUEEN_OUTER_TYPE : OUTER_TYPE;
            RenderType endOne = rainbow ? RAINBOW_END_ONE_TYPE : red ? QUEEN_RED_END_ONE_TYPE : power ? QUEEN_END_ONE_TYPE : END_ONE_TYPE;
            RenderType endTwo = rainbow ? RAINBOW_END_TWO_TYPE : red ? QUEEN_RED_END_TWO_TYPE : power ? QUEEN_END_TWO_TYPE : END_TWO_TYPE;
            float time = beam.age() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
            float scroll = -time * 0.25F;
            float yaw = (float) Math.atan2(direction.z, direction.x);
            float pitch = (float) Math.acos(direction.y);
            poses.pushPose();
            poses.translate(start.x, start.y, start.z);
            poses.mulPose(Axis.YP.rotation((float) (Math.PI / 2.0D) - yaw));
            poses.mulPose(Axis.XP.rotation((float) (-Math.PI / 2.0D) + pitch));
            poses.mulPose(Axis.ZP.rotationDegrees(45.0F));
            Matrix4f pose = poses.last().pose();
            if (rainbow) {
                drawRainbowBeam(buffers.getBuffer(inner), pose, length, 1.5F, 4, scroll * 0.5F, 0.5F, time, 0.55F);
                drawRainbowBeam(buffers.getBuffer(outer), pose, length, 1.75F, 8, scroll, 0.15F, time, 0.0F);
            } else {
                drawRoyalBeam(buffers.getBuffer(inner), pose, length, 1.5F, 4, scroll * 0.5F, 0.5F);
                drawRoyalBeam(buffers.getBuffer(outer), pose, length, 1.75F, 8, scroll, 0.15F);
            }
            poses.popPose();
            VertexConsumer impact = buffers.getBuffer(beam.age() / 2 % 2 == 0 ? endOne : endTwo);
            if (rainbow) {
                int color = rainbowColor((float) length, time, 0.25F);
                drawImpact(impact, poses, end, event.getCamera().rotation(), 1.3F,
                        (color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F);
            } else {
                drawImpact(impact, poses, end, event.getCamera().rotation(), 1.3F);
            }
        }
        buffers.endBatch(RAINBOW_END_ONE_TYPE);
        buffers.endBatch(RAINBOW_END_TWO_TYPE);
        buffers.endBatch(RAINBOW_INNER_TYPE);
        buffers.endBatch(RAINBOW_OUTER_TYPE);
        buffers.endBatch(END_ONE_TYPE);
        buffers.endBatch(END_TWO_TYPE);
        buffers.endBatch(INNER_TYPE);
        buffers.endBatch(OUTER_TYPE);
        buffers.endBatch(QUEEN_END_ONE_TYPE);
        buffers.endBatch(QUEEN_END_TWO_TYPE);
        buffers.endBatch(QUEEN_INNER_TYPE);
        buffers.endBatch(QUEEN_OUTER_TYPE);
        buffers.endBatch(QUEEN_RED_END_ONE_TYPE);
        buffers.endBatch(QUEEN_RED_END_TWO_TYPE);
        buffers.endBatch(QUEEN_RED_INNER_TYPE);
        buffers.endBatch(QUEEN_RED_OUTER_TYPE);
    }

    private static void drawRoyalBeam(VertexConsumer vertices, Matrix4f pose, double length,
                                      float radius, int sections, float scroll, float uvScale) {
        float endV = scroll + (float) length * uvScale;
        float previousX = -radius;
        float previousY = 0.0F;
        float previousU = 0.0F;
        for (int section = 0; section <= sections; section++) {
            float angle = (float) (Math.PI + Math.PI * 2.0D * section / sections);
            float x = (float) Math.cos(angle) * radius;
            float y = (float) Math.sin(angle) * radius;
            emitRoyalQuad(vertices, pose,
                    new Vec3(previousX * 0.55D, previousY * 0.55D, 0.0D),
                    new Vec3(previousX, previousY, length),
                    new Vec3(x, y, length),
                    new Vec3(x * 0.55D, y * 0.55D, 0.0D),
                    previousU, scroll, section + 1.0F, endV);
            previousX = x;
            previousY = y;
            previousU = section + 1.0F;
        }
    }

    private static void drawRainbowBeam(VertexConsumer vertices, Matrix4f pose, double length, float radius, int sections,
                                        float scroll, float uvScale, float time, float whiteMix) {
        int segments = Mth.clamp(Mth.ceil(length / RAINBOW_SEGMENT_LENGTH), 1, RAINBOW_MAX_SEGMENTS);
        float[] red = new float[segments + 1];
        float[] green = new float[segments + 1];
        float[] blue = new float[segments + 1];
        for (int segment = 0; segment <= segments; segment++) {
            int color = rainbowColor((float) (length * segment / segments), time, whiteMix);
            red[segment] = (color >> 16 & 255) / 255.0F;
            green[segment] = (color >> 8 & 255) / 255.0F;
            blue[segment] = (color & 255) / 255.0F;
        }
        float endV = scroll + (float) length * uvScale;
        float previousX = -radius;
        float previousY = 0.0F;
        float previousU = 0.0F;
        for (int section = 0; section <= sections; section++) {
            float angle = (float) (Math.PI + Math.PI * 2.0D * section / sections);
            float x = (float) Math.cos(angle) * radius;
            float y = (float) Math.sin(angle) * radius;
            float nextU = section + 1.0F;
            for (int segment = 0; segment < segments; segment++) {
                float near = (float) segment / segments;
                float far = (float) (segment + 1) / segments;
                float nearScale = 0.55F + 0.45F * near;
                float farScale = 0.55F + 0.45F * far;
                float nearZ = (float) length * near;
                float farZ = (float) length * far;
                float nearU = Mth.lerp(near, previousU, nextU);
                float farU = Mth.lerp(far, previousU, nextU);
                rainbowVertex(vertices, pose, previousX * nearScale, previousY * nearScale, nearZ, nearU, scroll,
                        red[segment], green[segment], blue[segment]);
                rainbowVertex(vertices, pose, previousX * farScale, previousY * farScale, farZ, farU, scroll,
                        red[segment + 1], green[segment + 1], blue[segment + 1]);
                rainbowVertex(vertices, pose, x * farScale, y * farScale, farZ, farU, endV,
                        red[segment + 1], green[segment + 1], blue[segment + 1]);
                rainbowVertex(vertices, pose, x * nearScale, y * nearScale, nearZ, nearU, endV,
                        red[segment], green[segment], blue[segment]);
            }
            previousX = x;
            previousY = y;
            previousU = nextU;
        }
    }

    private static int rainbowColor(float distance, float time, float whiteMix) {
        float hue = Mth.frac(distance / RAINBOW_BAND_LENGTH - time * RAINBOW_SPEED);
        return Mth.hsvToRgb(hue, 1.0F - whiteMix, 1.0F);
    }

    private static void rainbowVertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float u, float v,
                                      float red, float green, float blue) {
        vertices.addVertex(pose, x, y, z)
                .setColor(red, green, blue, 1.0F).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(240).setNormal(0.0F, -1.0F, 0.0F);
    }

    private static void emitRoyalQuad(VertexConsumer vertices, Matrix4f pose, Vec3 topLeft, Vec3 bottomLeft,
                                      Vec3 bottomRight, Vec3 topRight, float u0, float v0, float u1, float v1) {
        royalVertex(vertices, pose, topLeft, u0, v0);
        royalVertex(vertices, pose, bottomLeft, u1, v0);
        royalVertex(vertices, pose, bottomRight, u1, v1);
        royalVertex(vertices, pose, topRight, u0, v1);
    }

    private static void royalVertex(VertexConsumer vertices, Matrix4f pose, Vec3 point, float u, float v) {
        vertices.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(240).setNormal(0.0F, -1.0F, 0.0F);
    }

    private static void drawImpact(VertexConsumer vertices, PoseStack poses, Vec3 end,
                                   org.joml.Quaternionf cameraRotation, float size) {
        drawImpact(vertices, poses, end, cameraRotation, size, 1.0F, 1.0F, 1.0F);
    }

    private static void drawImpact(VertexConsumer vertices, PoseStack poses, Vec3 end,
                                   org.joml.Quaternionf cameraRotation, float size, float red, float green, float blue) {
        poses.pushPose();
        poses.translate(end.x, end.y, end.z);
        poses.mulPose(cameraRotation);
        Matrix4f pose = poses.last().pose();
        vertex(vertices, pose, new Vec3(-size, -size, 0.0D), 0.0F, 0.0F, 1.0F, red, green, blue);
        vertex(vertices, pose, new Vec3(size, -size, 0.0D), 1.0F, 0.0F, 1.0F, red, green, blue);
        vertex(vertices, pose, new Vec3(size, size, 0.0D), 1.0F, 1.0F, 1.0F, red, green, blue);
        vertex(vertices, pose, new Vec3(-size, size, 0.0D), 0.0F, 1.0F, 1.0F, red, green, blue);
        poses.popPose();
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose, Vec3 point, float u, float v, float alpha,
                               float red, float green, float blue) {
        vertices.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
