package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.voidworld.client.sky.ShaderPacks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class EyeBeamClient {
    private static final Map<Long, Beam> BEAMS = new HashMap<>();
    private static final Map<Integer, Vec3[]> EYES = new HashMap<>();
    private static Drawer drawer;

    private EyeBeamClient() {
    }

    @FunctionalInterface
    public interface Drawer {
        void draw(PoseStack poses, MultiBufferSource.BufferSource buffers, Vec3 start, Vec3 end, int core, int glow, float scale,
                  float opacity, float time);
    }

    public static void setDrawer(Drawer thematic) {
        drawer = thematic;
    }

    public static void accept(EyeBeamPayload payload) {
        long key = (long) payload.entityId() << 8 | payload.channel() & 0xFF;
        if (!payload.active()) {
            BEAMS.remove(key);
            return;
        }
        BEAMS.put(key, new Beam(payload.entityId(), payload.channel(), new Vec3(payload.endX(), payload.endY(), payload.endZ()),
                payload.core(), payload.glow(), payload.scale(), payload.life()));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            BEAMS.clear();
            return;
        }
        if (!minecraft.isPaused()) {
            BEAMS.values().removeIf(beam -> --beam.life < 0);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        BEAMS.clear();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (BEAMS.isEmpty() || event.getStage() != ShaderPacks.glowStage()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partialTick;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        for (Beam beam : BEAMS.values()) {
            if (!(level.getEntity(beam.entityId) instanceof LivingEntity owner) || owner.isRemoved()) {
                continue;
            }
            boolean firstPerson = owner == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
            float yaw = owner.getViewYRot(partialTick) * Mth.DEG_TO_RAD;
            Vec3 look = owner.getViewVector(partialTick);
            Vec3 right = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
            Vec3 up = right.cross(look).normalize();
            double side = beam.channel % 2 == 0 ? -1.0D : 1.0D;
            Vec3 eye = owner.getEyePosition(partialTick);
            Vec3 start = firstPerson
                    ? eye.add(right.scale(0.2D * side)).add(up.scale(0.08D)).subtract(look.scale(0.15D))
                    : capturedEye(owner.getId(), side, look, eye.add(look.scale(0.25D)).add(right.scale(0.06D * side)));
            Vec3 from = start.subtract(camera);
            Vec3 to = beam.end.subtract(camera);
            float opacity = firstPerson ? 0.5F : 1.0F;
            if (drawer != null) {
                try {
                    drawer.draw(poses, buffers, from, to, beam.core, beam.glow, beam.scale, opacity, time);
                    continue;
                } catch (Throwable error) {
                    drawer = null;
                    IntegratedSuperheroes.LOGGER.warn("Eye beams fell back to the plain look; this Thematic version may have changed its beam", error);
                }
            }
            drawPlain(poses, buffers, from, to, beam.core, beam.glow, beam.scale, opacity);
        }
        EYES.clear();
    }

    public static boolean wantsEyes(int entityId) {
        if (BEAMS.isEmpty()) {
            return false;
        }
        for (Beam beam : BEAMS.values()) {
            if (beam.entityId == entityId) {
                return true;
            }
        }
        return false;
    }

    public static void captureEyes(int entityId, Vec3 left, Vec3 right) {
        EYES.put(entityId, new Vec3[]{left, right});
    }

    private static Vec3 capturedEye(int entityId, double side, Vec3 look, Vec3 fallback) {
        Vec3[] eyes = EYES.get(entityId);
        if (eyes == null) {
            return fallback;
        }
        return (side < 0.0D ? eyes[0] : eyes[1]).add(look.scale(0.05D));
    }

    private static void drawPlain(PoseStack poses, MultiBufferSource.BufferSource buffers, Vec3 from, Vec3 to, int core, int glow,
                                  float scale, float opacity) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.05D) {
            return;
        }
        Vec3 direction = delta.scale(1.0D / length);
        RenderType type = RenderType.lightning();
        VertexConsumer vertices = buffers.getBuffer(type);
        poses.pushPose();
        poses.translate(from.x, from.y, from.z);
        poses.mulPose(Axis.YP.rotation((float) Math.atan2(direction.x, direction.z)));
        poses.mulPose(Axis.XP.rotation((float) -Math.asin(Mth.clamp(direction.y, -1.0D, 1.0D))));
        Matrix4f pose = poses.last().pose();
        float l = (float) length;
        plane(vertices, pose, 0.025F * scale, l, core, 1.0F);
        poses.mulPose(Axis.ZP.rotationDegrees(90.0F));
        plane(vertices, poses.last().pose(), 0.025F * scale, l, core, 1.0F);
        for (int i = 0; i < 4; i++) {
            poses.mulPose(Axis.ZP.rotationDegrees(45.0F));
            plane(vertices, poses.last().pose(), 0.06F * scale + 0.04F * scale * i, l, glow, opacity * (0.5F - i * 0.1F));
        }
        poses.popPose();
        buffers.endBatch(type);
    }

    private static void plane(VertexConsumer vertices, Matrix4f pose, float width, float length, int color, float alpha) {
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        vertex(vertices, pose, -width, 0.0F, red, green, blue, alpha);
        vertex(vertices, pose, -width, length, red, green, blue, 0.0F);
        vertex(vertices, pose, width, length, red, green, blue, 0.0F);
        vertex(vertices, pose, width, 0.0F, red, green, blue, alpha);
        vertex(vertices, pose, width, 0.0F, red, green, blue, alpha);
        vertex(vertices, pose, width, length, red, green, blue, 0.0F);
        vertex(vertices, pose, -width, length, red, green, blue, 0.0F);
        vertex(vertices, pose, -width, 0.0F, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float z, float red, float green, float blue, float alpha) {
        vertices.addVertex(pose, x, 0.0F, z).setColor(red, green, blue, alpha).setUv(0.0F, 0.0F)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
    }

    private static final class Beam {
        private final int entityId;
        private final int channel;
        private final Vec3 end;
        private final int core;
        private final int glow;
        private final float scale;
        private int life;

        private Beam(int entityId, int channel, Vec3 end, int core, int glow, float scale, int life) {
            this.entityId = entityId;
            this.channel = channel;
            this.end = end;
            this.core = core;
            this.glow = glow;
            this.scale = scale;
            this.life = life;
        }
    }
}
