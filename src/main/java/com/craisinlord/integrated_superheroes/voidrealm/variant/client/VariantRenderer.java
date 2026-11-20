package com.craisinlord.integrated_superheroes.voidrealm.variant.client;

import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

public class VariantRenderer extends HumanoidMobRenderer<VariantEntity, VariantRenderer.TintedModel> {
    public VariantRenderer(EntityRendererProvider.Context context) {
        super(context, new TintedModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(VariantEntity entity) {
        return DefaultPlayerSkin.get(entity.getUUID()).texture();
    }

    public static final class TintedModel extends PlayerModel<VariantEntity> {
        private int tint = -1;

        TintedModel(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(VariantEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            int primary = entity.primaryColor();
            tint = primary == -1 ? -1 : 0xFF000000 | lighten(primary);
        }

        private static int lighten(int color) {
            int red = (color >> 16 & 0xFF) + (0xFF - (color >> 16 & 0xFF)) * 2 / 5;
            int green = (color >> 8 & 0xFF) + (0xFF - (color >> 8 & 0xFF)) * 2 / 5;
            int blue = (color & 0xFF) + (0xFF - (color & 0xFF)) * 2 / 5;
            return red << 16 | green << 8 | blue;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
            super.renderToBuffer(poseStack, buffer, light, overlay, tint == -1 ? color : FastColor.ARGB32.multiply(color, tint));
        }
    }
}
