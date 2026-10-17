package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class HulkRubbleRenderer {
    private static final long LIFETIME = 800L;
    private static final int MAX_GLOWS = 4000;
    private static final double MAX_DISTANCE_SQR = 96.0D * 96.0D;
    private static final Map<BlockPos, Glow> GLOWS = new HashMap<>();

    private HulkRubbleRenderer() {
    }

    private record Glow(long start, int tier) {
    }

    public static void add(List<BlockPos> blocks, int tier) {
        long now = Util.getMillis();
        for (BlockPos pos : blocks) {
            if (GLOWS.size() >= MAX_GLOWS && !GLOWS.containsKey(pos)) {
                break;
            }
            GLOWS.put(pos.immutable(), new Glow(now, tier));
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        GLOWS.clear();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || GLOWS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            GLOWS.clear();
            return;
        }
        long now = Util.getMillis();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        Matrix4f pose = event.getPoseStack().last().pose();
        Iterator<Map.Entry<BlockPos, Glow>> iterator = GLOWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Glow> entry = iterator.next();
            long age = now - entry.getValue().start();
            if (age > LIFETIME) {
                iterator.remove();
                continue;
            }
            BlockPos pos = entry.getKey();
            if (pos.distToCenterSqr(camera) > MAX_DISTANCE_SQR) {
                continue;
            }
            float life = 1.0F - age / (float) LIFETIME;
            boolean whiteHot = entry.getValue().tier() >= HulkRampage.UNSTOPPABLE;
            float red = whiteHot ? 0.55F + life * 0.35F : 0.2F + life * 0.15F;
            float green = 1.0F;
            float blue = whiteHot ? 0.35F + life * 0.4F : 0.1F + life * 0.1F;
            float alpha = life * life * 0.75F;
            float grow = 0.01F + (1.0F - life) * 0.04F;
            box(vertices, pose,
                    (float) (pos.getX() - camera.x) - grow, (float) (pos.getY() - camera.y) - grow, (float) (pos.getZ() - camera.z) - grow,
                    (float) (pos.getX() + 1 - camera.x) + grow, (float) (pos.getY() + 1 - camera.y) + grow, (float) (pos.getZ() + 1 - camera.z) + grow,
                    red, green, blue, alpha);
        }
        buffers.endBatch(RenderType.lightning());
    }

    private static void box(VertexConsumer v, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1,
                            float r, float g, float b, float a) {
        quad(v, m, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a);
        quad(v, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
        quad(v, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
        quad(v, m, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a);
        quad(v, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
        quad(v, m, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
    }

    private static void quad(VertexConsumer v, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float r, float g, float b, float a) {
        v.addVertex(m, ax, ay, az).setColor(r, g, b, a);
        v.addVertex(m, bx, by, bz).setColor(r, g, b, a);
        v.addVertex(m, cx, cy, cz).setColor(r, g, b, a);
        v.addVertex(m, dx, dy, dz).setColor(r, g, b, a);
        v.addVertex(m, ax, ay, az).setColor(r, g, b, a);
        v.addVertex(m, dx, dy, dz).setColor(r, g, b, a);
        v.addVertex(m, cx, cy, cz).setColor(r, g, b, a);
        v.addVertex(m, bx, by, bz).setColor(r, g, b, a);
    }
}
