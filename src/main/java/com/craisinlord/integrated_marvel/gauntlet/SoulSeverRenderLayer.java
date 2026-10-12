package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.integrated_marvel.registry.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

public final class SoulSeverRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public SoulSeverRenderLayer(LivingEntityRenderer<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!entity.hasEffect(ModEffects.soulSevered()) || entity.isInvisible() || entity.isSpectator()) {
            return;
        }
        M entityModel = getParentModel();
        RenderType renderType = RenderType.entityTranslucent(getTextureLocation(entity));
        VertexConsumer consumer = buffers.getBuffer(renderType);
        entityModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        renderProjection(poseStack, entityModel, consumer, overlay, entity.getBbHeight() + 0.35D, 0xA8B8EFFF);
        renderProjection(poseStack, entityModel, consumer, overlay, entity.getBbHeight() + 0.75D, 0x56B8EFFF);
        renderProjection(poseStack, entityModel, consumer, overlay, entity.getBbHeight() + 1.10D, 0x28B8EFFF);
        entityModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    private void renderProjection(PoseStack poseStack, M entityModel, VertexConsumer consumer, int overlay, double height, int color) {
        poseStack.pushPose();
        poseStack.translate(0.0D, height, 0.0D);
        entityModel.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT, overlay, color);
        poseStack.popPose();
    }
}
