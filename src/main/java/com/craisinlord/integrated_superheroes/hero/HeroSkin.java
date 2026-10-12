package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class HeroSkin {
    private static int counter;

    public enum Material {
        ROCK("cobblestone", new int[]{96, 56, 28}, new int[]{255, 184, 110}, false),
        STEEL("iron_block", new int[]{70, 78, 94}, new int[]{232, 238, 248}, true),
        MAGMA("magma", new int[]{150, 46, 10}, new int[]{255, 176, 58}, true),
        ICE("packed_ice", new int[]{96, 150, 214}, new int[]{214, 238, 255}, true);

        private final ResourceLocation texture;
        private final int[] seam;
        private final int[] light;
        private final boolean normalize;
        private final Map<ResourceLocation, ResourceLocation> cache = new HashMap<>();
        private final Map<ResourceLocation, Long> retry = new HashMap<>();
        private NativeImage pattern;
        private double low;
        private double high;

        Material(String block, int[] seam, int[] light, boolean normalize) {
            this.texture = ResourceLocation.withDefaultNamespace("textures/block/" + block + ".png");
            this.seam = seam;
            this.light = light;
            this.normalize = normalize;
        }

        private double shadeOf(double luma) {
            if (!this.normalize) {
                return Math.min(1.0D, luma * 1.35D);
            }
            return this.high <= this.low ? 1.0D : Math.max(0.0D, Math.min(1.0D, (luma - this.low) / (this.high - this.low)));
        }
    }

    private HeroSkin() {
    }

    public static PlayerSkin apply(PlayerSkin skin, Material material) {
        ResourceLocation painted = painted(skin.texture(), material);
        if (painted == null) {
            return skin;
        }
        return new PlayerSkin(painted, skin.textureUrl(), skin.capeTexture(), skin.elytraTexture(), skin.model(), skin.secure());
    }

    private static ResourceLocation painted(ResourceLocation source, Material material) {
        ResourceLocation cached = material.cache.get(source);
        if (cached != null) {
            return cached;
        }
        if (!RenderSystem.isOnRenderThread()) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now < material.retry.getOrDefault(source, 0L)) {
            return null;
        }
        material.retry.put(source, now + 2000L);
        NativeImage skin = download(source);
        if (skin == null) {
            return null;
        }
        if (!loadPattern(material)) {
            skin.close();
            return null;
        }
        NativeImage out = paint(skin, material);
        skin.close();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID,
                "hero_skin/" + material.name().toLowerCase(Locale.ROOT) + "/" + counter++);
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(out));
        material.cache.put(source, id);
        material.retry.remove(source);
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

    private static boolean loadPattern(Material material) {
        if (material.pattern != null) {
            return true;
        }
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(material.texture);
        if (resource.isEmpty()) {
            return false;
        }
        NativeImage image;
        try (InputStream stream = resource.get().open()) {
            image = NativeImage.read(stream);
        } catch (Exception exception) {
            return false;
        }
        double low = 1.0D;
        double high = 0.0D;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                double luma = luma(pixel & 0xFF, pixel >> 8 & 0xFF, pixel >> 16 & 0xFF);
                low = Math.min(low, luma);
                high = Math.max(high, luma);
            }
        }
        material.low = low;
        material.high = high;
        material.pattern = image;
        return true;
    }

    private static double luma(int r, int g, int b) {
        return (0.299D * r + 0.587D * g + 0.114D * b) / 255.0D;
    }

    private static int mix(int a, int b, double t) {
        return (int) (a + (b - a) * t);
    }

    private static NativeImage paint(NativeImage skin, Material material) {
        NativeImage pattern = material.pattern;
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
                double t = material.shadeOf(stoneLuma);
                double shade = 0.88D + 0.35D * luma(r, g, b);
                int sx = x / unit;
                int sy = y / unit;
                boolean face = sx >= 8 && sx < 16 && sy >= 8 && sy < 16 || sx >= 40 && sx < 48 && sy >= 8 && sy < 16;
                boolean seam = material.normalize ? t < 0.25D : stoneLuma < 0.25D;
                double keep = face ? 0.45D : seam ? 0.05D : 0.12D;
                int[] rgb = new int[3];
                int[] skinRgb = {r, g, b};
                for (int i = 0; i < 3; i++) {
                    int base = (int) Math.min(255.0D, mix(material.seam[i], material.light[i], t) * shade);
                    rgb[i] = Math.max(0, Math.min(255, mix(base, skinRgb[i], keep)));
                }
                out.setPixelRGBA(x, y, alpha << 24 | rgb[2] << 16 | rgb[1] << 8 | rgb[0]);
            }
        }
        return out;
    }
}
