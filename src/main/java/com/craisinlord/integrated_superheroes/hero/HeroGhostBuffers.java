package com.craisinlord.integrated_superheroes.hero;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class HeroGhostBuffers {
    private static final VertexConsumer NONE = new Fade(null, 0);

    private HeroGhostBuffers() {
    }

    public static MultiBufferSource wrap(MultiBufferSource delegate, LivingEntity entity) {
        int alpha = HeroTint.ghostAlpha(entity);
        if (alpha < 0) {
            return delegate;
        }
        return type -> {
            if (type.format() == DefaultVertexFormat.NEW_ENTITY && type instanceof RenderType.CompositeRenderType composite) {
                Optional<ResourceLocation> texture = composite.state().textureState.cutoutTexture();
                if (texture.isPresent()) {
                    return new Fade(delegate.getBuffer(RenderType.entityTranslucent(texture.get())), alpha);
                }
            }
            return NONE;
        };
    }

    private record Fade(VertexConsumer delegate, int alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (delegate != null) {
                delegate.addVertex(x, y, z);
            }
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            if (delegate != null) {
                delegate.setColor(red, green, blue, alpha * this.alpha / 255);
            }
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            if (delegate != null) {
                delegate.setUv(u, v);
            }
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            if (delegate != null) {
                delegate.setUv1(u, v);
            }
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            if (delegate != null) {
                delegate.setUv2(u, v);
            }
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            if (delegate != null) {
                delegate.setNormal(x, y, z);
            }
            return this;
        }
    }
}
