package com.craisinlord.integrated_superheroes.boss;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WitherstormRenderer extends GeoEntityRenderer<WitherstormEntity> {
    private static final float FOG_START = 220.0F;
    private static final float FOG_END = 1400.0F;
    private static final int DEBRIS = 64;
    private static final float SPIN_PER_TICK = (float) (Math.PI * 2.0D / 80.0D);

    private final MultiBufferSource.BufferSource ownBuffer = MultiBufferSource.immediate(new ByteBufferBuilder(1 << 21));

    public WitherstormRenderer(EntityRendererProvider.Context context) {
        super(context, new WitherstormModel());
        this.shadowRadius = 1.5F;
    }

    @Override
    public void render(WitherstormEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float scale = WitherstormEntity.renderScale(entity.getStage());
        this.scaleWidth = scale;
        this.scaleHeight = scale;
        if (entity.isSmallForm()) {
            this.shadowRadius = 1.5F;
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }
        this.shadowRadius = 0.0F;
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(Math.max(fogStart, FOG_START));
        RenderSystem.setShaderFogEnd(Math.max(fogEnd, FOG_END));
        try {
            super.render(entity, entityYaw, partialTick, poseStack, this.ownBuffer, packedLight);
            this.renderDebris(entity, partialTick, poseStack, packedLight, scale);
            this.ownBuffer.endBatch();
        } finally {
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(fogEnd);
        }
    }

    private void renderDebris(WitherstormEntity entity, float partialTick, PoseStack poseStack, int packedLight, float s) {
        Minecraft minecraft = Minecraft.getInstance();
        double distance = minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(entity.position());
        int debris = distance > 192.0D * 192.0D ? 8 : distance > 96.0D * 96.0D ? 24 : DEBRIS;
        debris = switch (minecraft.options.particles().get()) {
            case MINIMAL -> debris / 4;
            case DECREASED -> debris / 2;
            default -> debris;
        };
        BlockState[] states = entity.eatenStates();
        if (states.length == 0) return;
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        float time = entity.tickCount + partialTick;
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.translate(0.0F, 0.0F, -1.5625F * s);
        for (int i = 0; i < debris; i++) {
            float a = hash(i, 1);
            float b = hash(i, 2);
            float c = hash(i, 3);
            float height = 0.15F + 0.85F * a;
            float radius = (2.5F + 9.0F * height) * s;
            float angle = b * Mth.TWO_PI - time * SPIN_PER_TICK * (0.7F + 0.6F * c);
            float bob = Mth.sin(time * 0.05F + c * 10.0F) * 0.6F * s;
            float y = (-25.0F + 33.0F * height) * s + bob;
            float size = s * (0.35F + 0.45F * c);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(angle) * radius, y, Mth.sin(angle) * radius);
            poseStack.mulPose(Axis.XP.rotation(time * 0.03F * (0.5F + b)));
            poseStack.mulPose(Axis.ZP.rotation(time * 0.02F * (0.5F + a)));
            poseStack.scale(size, size, size);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            blocks.renderSingleBlock(states[i % states.length], poseStack, this.ownBuffer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static float hash(int index, int salt) {
        int h = index * 0x9E3779B1 + salt * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    @Override
    protected float getDeathMaxRotation(WitherstormEntity animatable) {
        return 0.0F;
    }
}
