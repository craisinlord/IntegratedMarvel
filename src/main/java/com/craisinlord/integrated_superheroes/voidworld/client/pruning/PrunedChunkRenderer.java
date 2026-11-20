package com.craisinlord.integrated_superheroes.voidworld.client.pruning;

import com.craisinlord.integrated_superheroes.voidworld.pruning.PrunedChunkEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

public class PrunedChunkRenderer extends EntityRenderer<PrunedChunkEntity> {
    public PrunedChunkRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(PrunedChunkEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        PrunedMeshCache.Mesh mesh = PrunedMeshCache.get(entity);
        if (!mesh.ready()) {
            return;
        }
        float sx = entity.sizeX();
        float sy = entity.sizeY();
        float sz = entity.sizeZ();
        float axis = entity.tumbleAxis();
        poseStack.pushPose();
        poseStack.translate(0.0F, sy * 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.spin()));
        poseStack.mulPose(Axis.of(new Vector3f(Mth.cos(axis), 0.0F, Mth.sin(axis))).rotationDegrees(entity.tumble(partialTick)));
        poseStack.translate(-sx * 0.5F, -sy * 0.5F, -sz * 0.5F);
        mesh.draw(poseStack, buffers, packedLight);
        if (!mesh.animated().isEmpty()) {
            BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
            for (PrunedMeshCache.Animated cell : mesh.animated()) {
                BlockState state = cell.state();
                poseStack.pushPose();
                poseStack.translate(cell.pos().getX() + 0.5F, cell.pos().getY(), cell.pos().getZ() + 0.5F);
                if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(-state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()));
                }
                poseStack.translate(-0.5F, 0.0F, -0.5F);
                try {
                    dispatcher.renderSingleBlock(state, poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
                } catch (RuntimeException ignored) {
                }
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(PrunedChunkEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
