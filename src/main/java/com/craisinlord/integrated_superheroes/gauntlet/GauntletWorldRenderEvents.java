package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;
import java.util.OptionalDouble;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class GauntletWorldRenderEvents {
    private GauntletWorldRenderEvents() {
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        GauntletClientState.reset();
        GauntletConfig.clearSync();
        MindControlClientState.setControlling(false);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        PoseStack poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        if (GauntletClientState.mindSightActive()) renderMindSight(minecraft, buffers, poses, camera, event);
        renderClipboardPreview(minecraft, buffers, poses, camera);
    }

    private static void renderMindSight(Minecraft minecraft, MultiBufferSource.BufferSource buffers, PoseStack poses, Vec3 camera,
                                        RenderLevelStageEvent event) {
        VertexConsumer lines = buffers.getBuffer(XRayRenderTypes.LINES);
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (int id : GauntletClientState.sightEntities()) {
            Entity entity = minecraft.level.getEntity(id);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;
            Vec3 interpolated = entity.getPosition(partial).subtract(entity.position());
            AABB box = entity.getBoundingBox().move(interpolated).move(-camera.x, -camera.y, -camera.z);
            boolean hostile = entity instanceof Enemy;
            LevelRenderer.renderLineBox(poses, lines, box, 1.0F, hostile ? 0.3F : 0.88F, hostile ? 0.25F : 0.2F, 0.9F);
        }
        for (BlockPos pos : GauntletClientState.sightOres()) {
            if (pos.distToCenterSqr(camera) > 48.0D * 48.0D) continue;
            AABB box = new AABB(pos).deflate(0.02D).move(-camera.x, -camera.y, -camera.z);
            int color = oreColor(minecraft, pos);
            LevelRenderer.renderLineBox(poses, lines, box, (color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F,
                    (color & 255) / 255.0F, 0.8F);
        }
        buffers.endBatch(XRayRenderTypes.LINES);
    }

    private static int oreColor(Minecraft minecraft, BlockPos pos) {
        int color = minecraft.level.getBlockState(pos).getMapColor(minecraft.level, pos).col;
        return color == 0 ? 0xFFFFFF : color;
    }

    private static void renderClipboardPreview(Minecraft minecraft, MultiBufferSource.BufferSource buffers, PoseStack poses, Vec3 camera) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(minecraft.player);
        if (!GauntletAbilities.isSelected(gauntlet, "reality", "realityClipboard")) return;
        GauntletHudPayload hud = GauntletClientState.hud();
        if (hud.clipLabel().isEmpty() || hud.clipEntity() || minecraft.player.isShiftKeyDown()) return;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        List<Integer> sizes = hud.clipSize();
        Vec3i size = new Vec3i(sizes.get(0), sizes.get(1), sizes.get(2));
        Rotation rotation = RealityClipboard.rotationFor(Direction.from2DDataValue(hud.clipFacing()),
                minecraft.player.getDirection(), hud.clipRotation());
        BlockPos origin;
        Vec3i rotated;
        if (size.equals(new Vec3i(1, 1, 1))) {
            origin = minecraft.level.getBlockState(hit.getBlockPos()).canBeReplaced() ? hit.getBlockPos()
                    : hit.getBlockPos().relative(hit.getDirection());
            rotated = size;
        } else {
            origin = hit.getBlockPos().relative(hit.getDirection());
            rotated = RealityClipboard.rotatedSize(size, rotation);
        }
        AABB box = new AABB(Vec3.atLowerCornerOf(origin), Vec3.atLowerCornerOf(origin.offset(rotated)))
                .move(-camera.x, -camera.y, -camera.z);
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poses, lines, box, 0.9F, 0.1F, 0.15F, 1.0F);
        buffers.endBatch(RenderType.lines());
    }

    private static final class XRayRenderTypes extends RenderType {
        private static final RenderType LINES = create("integrated_superheroes_xray_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES, 1536, false, false, CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.of(2.0D)))
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .createCompositeState(false));

        private XRayRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                                boolean sortOnUpload, Runnable setup, Runnable clear) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
        }
    }
}
