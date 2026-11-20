package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.cleanup.CosmeticMerges;
import com.craisinlord.integrated_superheroes.hero.skin.SkinData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class AngelWingLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation ELYTRA_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/elytra.png");
    private static final Map<Integer, float[]> POSES = new ConcurrentHashMap<>();
    private static volatile Map<String, Wings> wings = Map.of();

    private final ElytraModel<AbstractClientPlayer> elytra;
    private final ModelPart elytraLeft;
    private final ModelPart elytraRight;

    private record Wings(CosmeticMerges.WingProfile profile, List<GeoBone> mountPath, GeoBone left, GeoBone right,
                         float lx, float ly, float lz, float rx, float ry, float rz) {
    }

    private AngelWingLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        ModelPart root = models.bakeLayer(ModelLayers.ELYTRA);
        this.elytra = new ElytraModel<>(root);
        this.elytraLeft = root.getChild("left_wing");
        this.elytraRight = root.getChild("right_wing");
    }

    public static boolean replacesThematicWings(Player player) {
        return WingedFlightPayload.client(player.getId()) != null;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        WingedFlightPayload state = WingedFlightPayload.client(player.getId());
        if (state == null || !state.wings() || player.isInvisible() || wearsOtherWings(player)) {
            return;
        }
        float[] pose = pose(player, state.mode(), ageInTicks);
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        Wings model = wings.get(CosmeticMerges.wingProfile(SkinData.costume(player)).id());
        if (model != null) {
            renderFeathered(poseStack, buffers, light, overlay, model, pose);
        } else {
            renderElytra(poseStack, buffers, light, overlay, player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, state.mode(), pose);
        }
    }

    private static boolean wearsOtherWings(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) {
            return false;
        }
        if (chest.getItem() instanceof ElytraItem) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(chest.getItem());
        return "thematic".equals(id.getNamespace()) && (id.getPath().startsWith("hawkgirl") || id.getPath().startsWith("hawkman"));
    }

    private static float[] pose(Player player, int mode, float ageInTicks) {
        float spread;
        float raise;
        if (mode == WingedFlightPayload.POWERED) {
            float speed = player.isSprinting() ? 0.9F : 0.6F;
            spread = 35.0F;
            raise = 5.0F + Mth.sin(ageInTicks * speed) * 20.0F;
        } else if (mode == WingedFlightPayload.GLIDE) {
            spread = 38.0F;
            raise = 4.0F + Mth.sin(ageInTicks * 0.08F) * 2.0F;
        } else if (mode == WingedFlightPayload.DIVE) {
            spread = -30.0F;
            raise = 0.0F;
        } else {
            spread = player.isCrouching() ? -5.0F : -10.0F;
            raise = Mth.sin(ageInTicks * 0.05F) * 1.5F;
        }
        float[] current = POSES.computeIfAbsent(player.getId(), ignored -> new float[]{spread, raise, 0.0F});
        float now = (float) (System.nanoTime() / 1.0E9D);
        float elapsed = current[2] == 0.0F ? 0.05F : Mth.clamp(now - current[2], 0.0F, 0.25F);
        current[2] = now;
        float blend = Mth.clamp(elapsed * 10.0F, 0.0F, 1.0F);
        current[0] += (spread - current[0]) * blend;
        current[1] = mode == WingedFlightPayload.POWERED ? raise : current[1] + (raise - current[1]) * blend;
        return current;
    }

    private void renderFeathered(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, Wings model, float[] pose) {
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(model.profile().texture()));
        poseStack.pushPose();
        for (GeoBone parent : model.mountPath()) RenderUtil.prepMatrixForBone(poseStack, parent);
        float spread = pose[0] * Mth.DEG_TO_RAD;
        float raise = pose[1] * Mth.DEG_TO_RAD;
        model.left().updateRotation(model.lx(), model.ly() + spread, model.lz() + raise);
        model.right().updateRotation(model.rx(), model.ry() - spread, model.rz() - raise);
        renderBone(poseStack, model.left(), consumer, light, overlay);
        renderBone(poseStack, model.right(), consumer, light, overlay);
        model.left().updateRotation(model.lx(), model.ly(), model.lz());
        model.right().updateRotation(model.rx(), model.ry(), model.rz());
        poseStack.popPose();
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

    private void renderElytra(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, AbstractClientPlayer player, float limbSwing,
                              float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, int mode, float[] pose) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);
        this.getParentModel().copyPropertiesTo(this.elytra);
        this.elytra.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        ModelPart left = this.elytraLeft;
        ModelPart right = this.elytraRight;
        float open = Mth.clamp((pose[0] + 10.0F) / 48.0F, 0.0F, 1.0F);
        float x = mode == WingedFlightPayload.DIVE ? 0.9F : Mth.lerp(open, 0.2618F, 0.35F);
        float z = -Mth.lerp(open, 0.2618F, 1.45F) - pose[1] * Mth.DEG_TO_RAD * open;
        left.xRot = x;
        left.yRot = 0.0F;
        left.zRot = z;
        right.xRot = x;
        right.yRot = 0.0F;
        right.zRot = -z;
        VertexConsumer consumer = buffers.getBuffer(RenderType.armorCutoutNoCull(ELYTRA_TEXTURE));
        this.elytra.renderToBuffer(poseStack, consumer, light, overlay);
        poseStack.popPose();
    }

    public static List<String> replacedWingBones(Player player) {
        var profile = CosmeticMerges.wingProfile(SkinData.costume(player));
        return List.of(profile.left(), profile.right());
    }

    private static Wings load(ResourceManager manager, CosmeticMerges.WingProfile profile) {
        try {
            if (manager.getResource(profile.geo()).isEmpty() || manager.getResource(profile.texture()).isEmpty()) return null;
            BakedGeoModel model = BakedModelFactory.getForNamespace(profile.geo().getNamespace())
                    .constructGeoModel(GeometryTree.fromModel(FileLoader.loadModelFile(profile.geo(), manager)));
            GeoBone body = model.getBone("armorBody").orElse(null);
            GeoBone left = model.getBone(profile.left()).orElse(null);
            GeoBone right = model.getBone(profile.right()).orElse(null);
            if (body == null || left == null || right == null || left.getParent() == null || left.getParent() != right.getParent()) return null;
            List<GeoBone> path = new ArrayList<>();
            for (GeoBone bone = left.getParent(); bone != null; bone = bone.getParent()) {
                path.addFirst(bone);
                if (bone == body) break;
            }
            if (path.isEmpty() || path.getFirst() != body) return null;
            return new Wings(profile, List.copyOf(path), left, right, left.getRotX(), left.getRotY(), left.getRotZ(),
                    right.getRotX(), right.getRotY(), right.getRotZ());
        } catch (Throwable error) {
            IntegratedSuperheroes.LOGGER.warn("Wing appearance {} will use the simple wing model", profile.id(), error);
            return null;
        }
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void addLayers(EntityRenderersEvent.AddLayers event) {
            for (var skin : event.getSkins()) {
                add(event.getSkin(skin), event.getEntityModels());
            }
        }

        @SuppressWarnings("unchecked")
        private static void add(EntityRenderer<?> renderer, EntityModelSet models) {
            if (renderer instanceof LivingEntityRenderer<?, ?> living && living.getModel() instanceof PlayerModel<?>) {
                LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> player =
                        (LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) living;
                player.addLayer(new AngelWingLayer(player, models));
            }
        }

        @SubscribeEvent
        public static void reloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> {
                Map<String, Wings> loaded = new LinkedHashMap<>();
                CosmeticMerges.wingProfiles().forEach((id, profile) -> {
                    Wings model = load(manager, profile);
                    if (model != null) loaded.put(id, model);
                });
                wings = Map.copyOf(loaded);
                POSES.clear();
            });
        }
    }

    static void clearPoses() {
        POSES.clear();
    }
}
