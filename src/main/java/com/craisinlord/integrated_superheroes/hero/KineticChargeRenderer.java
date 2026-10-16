package com.craisinlord.integrated_superheroes.hero;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class KineticChargeRenderer extends EntityRenderer<KineticCharge> {
    private final ItemRenderer itemRenderer;

    public KineticChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(KineticCharge charge, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        ItemStack stack = charge.getItem().copy();
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        float age = charge.tickCount + partialTicks;
        float heading = Mth.lerp(partialTicks, charge.yRotO, charge.getYRot());
        poseStack.pushPose();
        poseStack.translate(0.0D, charge.getBbHeight() * 0.5D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(heading));
        if (charge.isCard()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * 45.0F));
            poseStack.scale(0.7F, 0.7F, 0.7F);
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 30.0F));
            float scale = stack.getItem() instanceof BlockItem ? 0.55F : 0.75F;
            poseStack.scale(scale, scale, scale);
        }
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                poseStack, buffers, charge.level(), charge.getId());
        poseStack.popPose();
        super.render(charge, yaw, partialTicks, poseStack, buffers, light);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(KineticCharge charge) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
