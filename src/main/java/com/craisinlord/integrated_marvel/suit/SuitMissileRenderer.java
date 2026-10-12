package com.craisinlord.integrated_marvel.suit;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class SuitMissileRenderer extends EntityRenderer<SuitMissileEntity> {
    private static final ResourceLocation ROCKET = ResourceLocation.fromNamespaceAndPath("scguns", "rocket");
    private static final ResourceLocation GAUNTLET = ResourceLocation.fromNamespaceAndPath("marvel", "repulsor");
    private final ItemRenderer itemRenderer;
    private ItemStack rocket;
    private ItemStack gauntlet;

    public SuitMissileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SuitMissileEntity missile, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Vec3 motion = missile.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            motion = Vec3.directionFromRotation(missile.getXRot(), missile.getYRot());
        }
        Vec3 direction = motion.normalize();
        float scale = missile.scale();
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) direction.x, (float) direction.y, (float) direction.z));
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(missile.kind() == SuitMissileEntity.Kind.GAUNTLET ? gauntlet() : rocket(), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, poseStack, buffers, missile.level(), missile.getId());
        poseStack.popPose();
        super.render(missile, yaw, partialTicks, poseStack, buffers, light);
    }

    private ItemStack rocket() {
        if (rocket == null) {
            rocket = stack(ROCKET, Items.FIREWORK_ROCKET);
        }
        return rocket;
    }

    private ItemStack gauntlet() {
        if (gauntlet == null) {
            gauntlet = stack(GAUNTLET, Items.IRON_INGOT);
        }
        return gauntlet;
    }

    private static ItemStack stack(ResourceLocation id, net.minecraft.world.item.Item fallback) {
        var item = BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item == Items.AIR ? fallback : item);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(SuitMissileEntity missile) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
