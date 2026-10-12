package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.opengl.GL11;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ThingSkin {
    private static final ResourceLocation COBBLE = ResourceLocation.withDefaultNamespace("textures/block/cobblestone.png");
    private static final int[] SEAM = {96, 56, 28};
    private static final int[] LIGHT = {255, 184, 110};
    private static final Map<ResourceLocation, ResourceLocation> CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Long> RETRY = new HashMap<>();
    private static NativeImage cobble;
    private static int counter;

    private ThingSkin() {
    }

    public static PlayerSkin apply(PlayerSkin skin) {
        ResourceLocation rock = rock(skin.texture());
        if (rock == null) {
            return skin;
        }
        return new PlayerSkin(rock, skin.textureUrl(), skin.capeTexture(), skin.elytraTexture(), skin.model(), skin.secure());
    }

    private static ResourceLocation rock(ResourceLocation source) {
        ResourceLocation cached = CACHE.get(source);
        if (cached != null) {
            return cached;
        }
        if (!RenderSystem.isOnRenderThread()) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now < RETRY.getOrDefault(source, 0L)) {
            return null;
        }
        RETRY.put(source, now + 2000L);
        NativeImage skin = download(source);
        if (skin == null) {
            return null;
        }
        NativeImage pattern = cobble();
        if (pattern == null) {
            skin.close();
            return null;
        }
        NativeImage out = build(skin, pattern);
        skin.close();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "thing_skin/" + counter++);
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(out));
        CACHE.put(source, id);
        RETRY.remove(source);
        return id;
    }

    private static NativeImage download(ResourceLocation source) {
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        AbstractTexture texture = manager.getTexture(source, MissingTextureAtlasSprite.getTexture());
        if (texture == MissingTextureAtlasSprite.getTexture()) {
            return null;
        }
        RenderSystem.bindTexture(texture.getId());
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        if (width < 64 || height < 64 || width != height) {
            return null;
        }
        NativeImage image = new NativeImage(width, height, false);
        image.downloadTexture(0, false);
        boolean any = false;
        for (int y = 0; y < height && !any; y++) {
            for (int x = 0; x < width && !any; x++) {
                any = (image.getPixelRGBA(x, y) >>> 24) != 0;
            }
        }
        if (!any) {
            image.close();
            return null;
        }
        return image;
    }

    private static NativeImage cobble() {
        if (cobble != null) {
            return cobble;
        }
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(COBBLE);
        if (resource.isEmpty()) {
            return null;
        }
        try (InputStream stream = resource.get().open()) {
            cobble = NativeImage.read(stream);
        } catch (Exception exception) {
            return null;
        }
        return cobble;
    }

    private static double luma(int r, int g, int b) {
        return (0.299D * r + 0.587D * g + 0.114D * b) / 255.0D;
    }

    private static int mix(int a, int b, double t) {
        return (int) (a + (b - a) * t);
    }

    private static NativeImage build(NativeImage skin, NativeImage pattern) {
        int size = skin.getWidth();
        int unit = size / 64;
        NativeImage out = new NativeImage(size, size, false);
        int pw = pattern.getWidth();
        int ph = pattern.getHeight();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int pixel = skin.getPixelRGBA(x, y);
                int alpha = pixel >>> 24;
                if (alpha == 0) {
                    out.setPixelRGBA(x, y, 0);
                    continue;
                }
                int r = pixel & 0xFF;
                int g = pixel >> 8 & 0xFF;
                int b = pixel >> 16 & 0xFF;
                int stone = pattern.getPixelRGBA((x / unit) % pw, (y / unit) % ph);
                double stoneLuma = luma(stone & 0xFF, stone >> 8 & 0xFF, stone >> 16 & 0xFF);
                double t = Math.min(1.0D, stoneLuma * 1.35D);
                double shade = 0.88D + 0.35D * luma(r, g, b);
                int sx = x / unit;
                int sy = y / unit;
                boolean face = sx >= 8 && sx < 16 && sy >= 8 && sy < 16 || sx >= 40 && sx < 48 && sy >= 8 && sy < 16;
                double keep = face ? 0.45D : stoneLuma < 0.25D ? 0.05D : 0.12D;
                int[] rgb = new int[3];
                int[] skinRgb = {r, g, b};
                for (int i = 0; i < 3; i++) {
                    int rock = (int) Math.min(255.0D, mix(SEAM[i], LIGHT[i], t) * shade);
                    rgb[i] = Math.max(0, Math.min(255, mix(rock, skinRgb[i], keep)));
                }
                out.setPixelRGBA(x, y, alpha << 24 | rgb[2] << 16 | rgb[1] << 8 | rgb[0]);
            }
        }
        return out;
    }
}
