package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class StormArcRenderer {
    private static final int MAX_ARCS = 240;
    private static final List<Arc> ARCS = new ArrayList<>();

    private StormArcRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            ARCS.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        ARCS.removeIf(arc -> --arc.life <= 0);
        RandomSource random = level.getRandom();
        for (int id : StormCharge.clientCharged()) {
            if (!(level.getEntity(id) instanceof Player player) || player.isRemoved() || player.isInvisible()) {
                continue;
            }
            boolean firstPerson = player == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
            int count = 1 + (random.nextFloat() < 0.6F ? 1 : 0);
            for (int i = 0; i < count && ARCS.size() < MAX_ARCS; i++) {
                ARCS.add(bodyArc(player, random, firstPerson));
            }
            if (!firstPerson && random.nextInt(10) == 0 && ARCS.size() < MAX_ARCS) {
                ARCS.add(groundArc(player, random));
            }
            if (random.nextFloat() < 0.7F) {
                double radius = player.getBbWidth() * 0.5D + 0.4D;
                double angle = random.nextDouble() * Math.PI * 2.0D;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        player.getX() + Math.cos(angle) * radius,
                        player.getY() + random.nextDouble() * player.getBbHeight(),
                        player.getZ() + Math.sin(angle) * radius,
                        (random.nextDouble() - 0.5D) * 0.4D, random.nextDouble() * 0.2D, (random.nextDouble() - 0.5D) * 0.4D);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ARCS.clear();
        StormCharge.clearClient();
    }

    private static Arc bodyArc(Player player, RandomSource random, boolean firstPerson) {
        double height = player.getBbHeight();
        double width = player.getBbWidth() * 0.5D + 0.15D;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double y = firstPerson ? height * (0.05D + random.nextDouble() * 0.45D) : height * (0.05D + random.nextDouble() * 0.9D);
        Vec3 start = new Vec3(Math.cos(angle) * width, y, Math.sin(angle) * width);
        Vec3 outward = new Vec3(Math.cos(angle), (random.nextDouble() - 0.5D) * 1.2D, Math.sin(angle));
        Vec3 direction = outward.add(randomVector(random).scale(0.6D)).normalize();
        double length = 0.5D + random.nextDouble() * 0.8D;
        return new Arc(player.getId(), jagged(start, start.add(direction.scale(length)), 6, 0.16D, random), 2 + random.nextInt(3));
    }

    private static Arc groundArc(Player player, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double reach = 1.5D + random.nextDouble() * 1.5D;
        Vec3 start = new Vec3(0.0D, player.getBbHeight() * (0.4D + random.nextDouble() * 0.4D), 0.0D);
        Vec3 end = new Vec3(Math.cos(angle) * reach, -0.2D - random.nextDouble() * 0.6D, Math.sin(angle) * reach);
        return new Arc(player.getId(), jagged(start, end, 9, 0.32D, random), 3);
    }

    private static Vec3[] jagged(Vec3 start, Vec3 end, int count, double jitter, RandomSource random) {
        Vec3[] points = new Vec3[count];
        for (int i = 0; i < count; i++) {
            double t = (double) i / (count - 1);
            Vec3 point = start.lerp(end, t);
            if (i > 0 && i < count - 1) {
                point = point.add(randomVector(random).scale(jitter));
            }
            points[i] = point;
        }
        return points;
    }

    private static Vec3 randomVector(RandomSource random) {
        return new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D, random.nextDouble() - 0.5D).scale(2.0D);
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ARCS.isEmpty()) {
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
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        Matrix4f pose = event.getPoseStack().last().pose();
        Iterator<Arc> iterator = ARCS.iterator();
        while (iterator.hasNext()) {
            Arc arc = iterator.next();
            Entity entity = level.getEntity(arc.entityId);
            if (entity == null) {
                iterator.remove();
                continue;
            }
            Vec3 base = entity.getPosition(partialTick).subtract(camera);
            float fade = Math.min(1.0F, arc.life / 2.0F);
            for (int i = 0; i < arc.points.length - 1; i++) {
                Vec3 from = base.add(arc.points[i]);
                Vec3 to = base.add(arc.points[i + 1]);
                ribbon(vertices, pose, from, to, 0.07F, 0.35F, 0.55F, 1.0F, 0.32F * fade);
                ribbon(vertices, pose, from, to, 0.022F, 0.9F, 0.95F, 1.0F, 0.95F * fade);
            }
        }
        buffers.endBatch(RenderType.lightning());
    }

    private static void ribbon(VertexConsumer vertices, Matrix4f pose, Vec3 from, Vec3 to, float width,
                               float red, float green, float blue, float alpha) {
        Vec3 along = to.subtract(from);
        Vec3 side = along.cross(from.add(to).scale(0.5D));
        if (side.lengthSqr() < 1.0E-8D) {
            return;
        }
        side = side.normalize().scale(width);
        Vec3 a = from.add(side);
        Vec3 b = from.subtract(side);
        Vec3 c = to.subtract(side);
        Vec3 d = to.add(side);
        quad(vertices, pose, a, b, c, d, red, green, blue, alpha);
        quad(vertices, pose, d, c, b, a, red, green, blue, alpha);
    }

    private static void quad(VertexConsumer vertices, Matrix4f pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             float red, float green, float blue, float alpha) {
        vertex(vertices, pose, a, red, green, blue, alpha);
        vertex(vertices, pose, b, red, green, blue, alpha);
        vertex(vertices, pose, c, red, green, blue, alpha);
        vertex(vertices, pose, d, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose, Vec3 point, float red, float green, float blue, float alpha) {
        vertices.addVertex(pose, (float) point.x, (float) point.y, (float) point.z).setColor(red, green, blue, alpha);
    }

    private static final class Arc {
        private final int entityId;
        private final Vec3[] points;
        private int life;

        private Arc(int entityId, Vec3[] points, int life) {
            this.entityId = entityId;
            this.points = points;
            this.life = life;
        }
    }
}
