package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.registry.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
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
        M model = getParentModel();
        float modelHeight = entity.getBbHeight() / Math.max(0.01F, entity.getScale());
        float bob = Mth.sin(ageInTicks * 0.12F) * 0.04F * modelHeight;
        float lift = modelHeight * 0.45F + bob;
        float drift = modelHeight * 0.08F;
        float pulse = 0.85F + 0.15F * Mth.sin(ageInTicks * 0.25F);

        RenderType ghost = RenderType.entityTranslucent(getTextureLocation(entity));
        renderGhost(poseStack, buffers.getBuffer(ghost), model, lift * 0.35F, drift * 0.35F, 1.0F, argb(0.18F, 0xFF7A1A));
        renderGhost(poseStack, buffers.getBuffer(ghost), model, lift * 0.7F, drift * 0.7F, 1.0F, argb(0.32F, 0xFF8A24));
        renderGhost(poseStack, buffers.getBuffer(ghost), model, lift, drift, 1.0F, argb(0.6F * pulse, 0xFFA040));
        renderGhost(poseStack, buffers.getBuffer(ghost), model, lift, drift, 1.08F, argb(0.22F * pulse, 0xFF6A00));
    }

    private void renderGhost(PoseStack poseStack, VertexConsumer consumer, M model,
                             float lift, float drift, float grow, int color) {
        poseStack.pushPose();
        poseStack.translate(0.0F, -lift, drift);
        poseStack.scale(grow, grow, grow);
        model.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
        poseStack.popPose();
    }

    private static int argb(float alpha, int rgb) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | (rgb & 0xFFFFFF);
    }
}
