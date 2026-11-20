package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.RenderUtil;

import java.util.List;

public final class SurferBoardRenderer {
    public static final float RIDE_HEIGHT = 0.06F;
    private static final ResourceLocation BOARD_GEO = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "geo/entity/silver_surfer_board.geo.json");
    private static final ResourceLocation BOARD_TEXTURE = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "textures/entity/silver_surfer_board.png");
    private static final ResourceLocation FALLBACK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");
    private static final float SCALE = 2.0F;
    private static final float DECK_TOP = 1.3F / 16.0F;
    private static volatile List<GeoBone> bones;

    private SurferBoardRenderer() {
    }

    public static void draw(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        List<GeoBone> model = bones;
        poseStack.pushPose();
        if (model != null) {
            poseStack.scale(SCALE, SCALE, SCALE);
            poseStack.translate(0.0F, -DECK_TOP, 0.0F);
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(BOARD_TEXTURE));
            for (GeoBone bone : model) {
                renderBone(poseStack, bone, consumer, light, overlay);
            }
        } else {
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(FALLBACK_TEXTURE));
            slab(poseStack, consumer, light, overlay, 0.28F, RIDE_HEIGHT, 0.95F);
        }
        poseStack.popPose();
    }

    private static void renderBone(PoseStack poseStack, GeoBone bone, VertexConsumer consumer, int light, int overlay) {
        if (bone.isHidden()) {
            return;
        }
        poseStack.pushPose();
        RenderUtil.prepMatrixForBone(poseStack, bone);
        for (GeoCube cube : bone.getCubes()) {
            poseStack.pushPose();
            RenderUtil.translateToPivotPoint(poseStack, cube);
            RenderUtil.rotateMatrixAroundCube(poseStack, cube);
            RenderUtil.translateAwayFromPivotPoint(poseStack, cube);
            Matrix3f normalPose = poseStack.last().normal();
            Matrix4f pose = poseStack.last().pose();
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) {
                    continue;
                }
                Vector3f normal = normalPose.transform(new Vector3f(quad.normal()));
                RenderUtil.fixInvertedFlatCube(cube, normal);
                for (GeoVertex vertex : quad.vertices()) {
                    Vector3f position = vertex.position();
                    Vector4f point = pose.transform(new Vector4f(position.x(), position.y(), position.z(), 1.0F));
                    consumer.addVertex(point.x(), point.y(), point.z(), 0xFFFFFFFF, vertex.texU(), vertex.texV(), overlay, light,
                            normal.x(), normal.y(), normal.z());
                }
            }
            poseStack.popPose();
        }
        if (!bone.isHidingChildren()) {
            for (GeoBone child : bone.getChildBones()) {
                renderBone(poseStack, child, consumer, light, overlay);
            }
        }
        poseStack.popPose();
    }

    private static void slab(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float halfWidth, float height, float halfLength) {
        PoseStack.Pose pose = poseStack.last();
        float x0 = -halfWidth;
        float x1 = halfWidth;
        float y0 = -height;
        float y1 = 0.0F;
        float z0 = -halfLength;
        float z1 = halfLength;
        face(consumer, pose, light, overlay, 0.0F, 1.0F, 0.0F, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        face(consumer, pose, light, overlay, 0.0F, -1.0F, 0.0F, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        face(consumer, pose, light, overlay, 0.0F, 0.0F, -1.0F, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        face(consumer, pose, light, overlay, 0.0F, 0.0F, 1.0F, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        face(consumer, pose, light, overlay, -1.0F, 0.0F, 0.0F, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        face(consumer, pose, light, overlay, 1.0F, 0.0F, 0.0F, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void face(VertexConsumer consumer, PoseStack.Pose pose, int light, int overlay, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
        consumer.addVertex(pose, ax, ay, az).setColor(0xFFFFFFFF).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, bx, by, bz).setColor(0xFFFFFFFF).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, cx, cy, cz).setColor(0xFFFFFFFF).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, dx, dy, dz).setColor(0xFFFFFFFF).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }

    private static List<GeoBone> load(ResourceManager manager) {
        try {
            if (manager.getResource(BOARD_GEO).isEmpty() || manager.getResource(BOARD_TEXTURE).isEmpty()) {
                return null;
            }
            BakedGeoModel model = BakedModelFactory.getForNamespace(BOARD_GEO.getNamespace())
                    .constructGeoModel(GeometryTree.fromModel(FileLoader.loadModelFile(BOARD_GEO, manager)));
            List<GeoBone> top = model.topLevelBones();
            return top.isEmpty() ? null : List.copyOf(top);
        } catch (Throwable error) {
            IntegratedSuperheroes.LOGGER.warn("Silver Surfer board will use the plain board model", error);
            return null;
        }
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void reloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> bones = load(manager));
        }
    }
}
