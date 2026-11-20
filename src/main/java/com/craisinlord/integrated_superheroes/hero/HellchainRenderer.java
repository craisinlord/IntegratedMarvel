package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Map;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class HellchainRenderer {
    private static final ResourceLocation CHAIN = ResourceLocation.withDefaultNamespace("textures/block/chain.png");
    private static final float HALF_WIDTH = 0.09375F;
    private static final int MAX_SEGMENTS = 24;

    private HellchainRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            HellchainController.clearClient();
            return;
        }
        if (!minecraft.isPaused()) {
            HellchainController.tickClient();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HellchainController.clearClient();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Map<Integer, int[]> chains = HellchainController.clientChains();
        if (chains.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType type = RenderType.entityCutoutNoCull(CHAIN);
        VertexConsumer vertices = buffers.getBuffer(type);
        PoseStack.Pose pose = event.getPoseStack().last();
        boolean drew = false;
        for (Map.Entry<Integer, int[]> entry : chains.entrySet()) {
            Entity owner = level.getEntity(entry.getKey());
            Entity target = level.getEntity(entry.getValue()[0]);
            if (!(owner instanceof LivingEntity rider) || target == null || owner.isRemoved() || target.isRemoved()) {
                continue;
            }
            Vec3 from = hand(rider, partialTick, minecraft).subtract(camera);
            Vec3 to = target.getPosition(partialTick).add(0.0D, target.getBbHeight() * 0.55D, 0.0D).subtract(camera);
            draw(vertices, pose, from, to);
            drew = true;
        }
        if (drew) {
            buffers.endBatch(type);
        }
    }

    private static Vec3 hand(LivingEntity rider, float partialTick, Minecraft minecraft) {
        Vec3 base = rider.getPosition(partialTick);
        boolean firstPerson = rider == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
        float yaw = Mth.lerp(partialTick, rider.yBodyRotO, rider.yBodyRot) * Mth.DEG_TO_RAD;
        if (firstPerson) {
            Vec3 look = rider.getViewVector(partialTick);
            float headYaw = rider.getViewYRot(partialTick) * Mth.DEG_TO_RAD;
            Vec3 right = new Vec3(-Mth.cos(headYaw), 0.0D, -Mth.sin(headYaw));
            return rider.getEyePosition(partialTick).add(look.scale(0.6D)).add(right.scale(0.35D)).subtract(0.0D, 0.35D, 0.0D);
        }
        Vec3 right = new Vec3(-Mth.cos(yaw), 0.0D, -Mth.sin(yaw));
        return base.add(right.scale(0.38D)).add(0.0D, rider.getBbHeight() * 0.62D, 0.0D);
    }

    private static void draw(VertexConsumer vertices, PoseStack.Pose pose, Vec3 from, Vec3 to) {
        Vec3 along = to.subtract(from);
        double length = along.length();
        if (length < 0.05D) {
            return;
        }
        Vec3 axis = along.scale(1.0D / length);
        Vec3 up = Math.abs(axis.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 sideA = axis.cross(up).normalize().scale(HALF_WIDTH);
        Vec3 sideB = axis.cross(sideA).normalize().scale(HALF_WIDTH);
        int segments = Math.min(MAX_SEGMENTS, Math.max(1, (int) Math.ceil(length)));
        double sag = Math.min(0.6D, length * 0.04D);
        Vec3 previous = from;
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            Vec3 point = from.add(along.scale(t)).subtract(0.0D, sag * 4.0D * t * (1.0D - t), 0.0D);
            float v = (float) (point.distanceTo(previous));
            plane(vertices, pose, previous, point, sideA, 0.0F, 3.0F / 16.0F, v);
            plane(vertices, pose, previous, point, sideB, 3.0F / 16.0F, 6.0F / 16.0F, v);
            previous = point;
        }
    }

    private static void plane(VertexConsumer vertices, PoseStack.Pose pose, Vec3 from, Vec3 to, Vec3 side,
                              float u0, float u1, float v) {
        Matrix4f matrix = pose.pose();
        Vec3 normal = side.cross(to.subtract(from));
        if (normal.lengthSqr() < 1.0E-8D) {
            return;
        }
        normal = normal.normalize();
        float vEnd = Math.min(1.0F, v);
        vertex(vertices, matrix, pose, from.subtract(side), u0, 0.0F, normal);
        vertex(vertices, matrix, pose, from.add(side), u1, 0.0F, normal);
        vertex(vertices, matrix, pose, to.add(side), u1, vEnd, normal);
        vertex(vertices, matrix, pose, to.subtract(side), u0, vEnd, normal);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, PoseStack.Pose pose, Vec3 point, float u, float v, Vec3 normal) {
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .setColor(255, 190, 140, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
