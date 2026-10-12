package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = IntegratedMarvel.MODID, value = Dist.CLIENT)
public final class DeadpoolClient {
    public static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "deadpool");
    private static final ResourceLocation DIRT = ResourceLocation.withDefaultNamespace("textures/block/dirt.png");
    private static final ResourceLocation CRACK = ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "textures/gui/rage_quit_crack.png");
    private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
    private static final int YELLOW = 0xFFF7D51D;
    private static final int INK = 0xFF111111;
    private static final int MAX_WIDTH = 150;
    private static final int LINE_HEIGHT = 12;
    private static final int SLIDE_TICKS = 6;
    private static final int WORLD_LIFE = 100;
    private static final Deque<Caption> HUD = new ArrayDeque<>();
    private static final Map<Integer, Caption> WORLD = new HashMap<>();
    private static long clock;
    private static int loadingTicks;
    private static int pauseTicks;
    private static int breakTicks;
    private static int breakTotal;
    private static int retconTicks;

    private DeadpoolClient() {
    }

    private record Caption(Component text, long start) {
        private int life() {
            return 70 + text.getString().length() * 2;
        }
    }

    private static Component styled(String text) {
        return Component.literal(text).withStyle(Style.EMPTY.withFont(FONT).withColor(INK & 0xFFFFFF));
    }

    public static void caption(int entityId, String key) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Caption caption = new Caption(styled(Component.translatable(key).getString()), clock);
        if (entityId == player.getId()) {
            HUD.addLast(caption);
            while (HUD.size() > 2) {
                HUD.removeFirst();
            }
        } else {
            WORLD.put(entityId, caption);
        }
    }

    public static void downed(int state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (state == DeadpoolDownedPayload.UP) {
            if (minecraft.screen instanceof DeadpoolDeathScreen) {
                minecraft.setScreen(null);
            }
            return;
        }
        DeadpoolDeathScreen screen = minecraft.screen instanceof DeadpoolDeathScreen open ? open : new DeadpoolDeathScreen();
        if (minecraft.screen != screen) {
            minecraft.setScreen(screen);
        }
        if (state == DeadpoolDownedPayload.READY) {
            screen.ready();
        }
    }

    public static void fx(int kind, int ticks) {
        switch (kind) {
            case DeadpoolFxPayload.LOADING -> loadingTicks = ticks;
            case DeadpoolFxPayload.PAUSE -> pauseTicks = ticks;
            case DeadpoolFxPayload.BREAK -> {
                breakTicks = ticks;
                breakTotal = ticks;
            }
            case DeadpoolFxPayload.RETCON -> retconTicks = ticks;
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        clock++;
        loadingTicks = Math.max(0, loadingTicks - 1);
        pauseTicks = Math.max(0, pauseTicks - 1);
        breakTicks = Math.max(0, breakTicks - 1);
        retconTicks = Math.max(0, retconTicks - 1);
        while (!HUD.isEmpty() && clock - HUD.peekFirst().start() > HUD.peekFirst().life()) {
            HUD.removeFirst();
        }
        WORLD.values().removeIf(caption -> clock - caption.start() > Math.max(WORLD_LIFE, caption.life()));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HUD.clear();
        WORLD.clear();
        loadingTicks = 0;
        pauseTicks = 0;
        breakTicks = 0;
        retconTicks = 0;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (retconTicks > 0) {
            int alpha = (int) (110.0F * retconTicks / 10.0F);
            graphics.fill(0, 0, width, height, alpha << 24 | 0x5A2D82);
        }
        if (pauseTicks > 0) {
            drawPauseMenu(graphics, minecraft.font, width, height);
        }
        if (breakTicks > 0) {
            drawCrack(graphics, partial, width, height);
        }
        if (loadingTicks > 0) {
            graphics.setColor(0.25F, 0.25F, 0.25F, 1.0F);
            graphics.blit(DIRT, 0, 0, 0, 0.0F, 0.0F, width, height, 32, 32);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.drawCenteredString(minecraft.font, Component.translatable("multiplayer.downloadingTerrain"), width / 2, height / 2 - 20, 0xFFFFFF);
        }
        if (minecraft.options.hideGui) {
            return;
        }
        int y = height / 3;
        for (Caption caption : HUD) {
            y += drawCaption(graphics, minecraft.font, caption, partial, width, y) + 10;
        }
    }

    private static void drawPauseMenu(GuiGraphics graphics, Font font, int width, int height) {
        graphics.fill(0, 0, width, height, 0x50909090);
        int center = width / 2;
        graphics.drawCenteredString(font, Component.translatable("menu.game"), center, 8, 0xFFFFFF);
        graphics.blitSprite(BUTTON, center - 102, 22, 98, 20);
        graphics.blitSprite(BUTTON, center + 4, 22, 98, 20);
        graphics.drawCenteredString(font, Component.translatable("menu.returnToGame"), center - 53, 28, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("menu.options"), center + 53, 28, 0xFFFFFF);
    }

    private static void drawCrack(GuiGraphics graphics, float partial, int width, int height) {
        float remaining = breakTicks - partial;
        float elapsed = breakTotal - remaining;
        float alpha = Mth.clamp(remaining / 20.0F, 0.0F, 1.0F);
        if (elapsed < 3.0F) {
            graphics.fill(0, 0, width, height, (int) (160.0F * (1.0F - elapsed / 3.0F)) << 24 | 0xFFFFFF);
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        if (elapsed < 6.0F) {
            float jolt = (6.0F - elapsed) * 0.8F;
            pose.translate(Mth.sin(elapsed * 7.0F) * jolt, Mth.cos(elapsed * 9.0F) * jolt, 0.0F);
        }
        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(CRACK, -8, -8, width + 16, height + 16, 0.0F, 0.0F, 1024, 576, 1024, 576);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        pose.popPose();
    }

    private static float ease(float t) {
        float clamped = Mth.clamp(t, 0.0F, 1.0F);
        return 1.0F - (1.0F - clamped) * (1.0F - clamped) * (1.0F - clamped);
    }

    private static int drawCaption(GuiGraphics graphics, Font font, Caption caption, float partial, int width, int y) {
        String full = caption.text().getString();
        float age = clock - caption.start() + partial;
        int shown = Mth.clamp((int) ((age - SLIDE_TICKS) * 1.5F), 0, full.length());
        List<FormattedCharSequence> layout = font.split(caption.text(), MAX_WIDTH);
        List<FormattedCharSequence> lines = font.split(styled(full.substring(0, shown)), MAX_WIDTH);
        int boxWidth = layout.stream().mapToInt(font::width).max().orElse(0) + 14;
        int boxHeight = Math.max(1, layout.size()) * LINE_HEIGHT + 8;
        float visible = Math.min(ease(age / SLIDE_TICKS), ease((caption.life() - age) / SLIDE_TICKS));
        float x = width - boxWidth - 12 + (1.0F - visible) * (boxWidth + 30);
        float tilt = (caption.start() & 1L) == 0L ? -3.0F : 2.0F;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + boxWidth / 2.0F, y + boxHeight / 2.0F, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(tilt));
        pose.translate(-boxWidth / 2.0F, -boxHeight / 2.0F, 0.0F);
        graphics.fill(3, 3, boxWidth + 5, boxHeight + 5, 0x80000000);
        graphics.fill(-2, -2, boxWidth + 2, boxHeight + 2, INK);
        graphics.fill(0, 0, boxWidth, boxHeight, YELLOW);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), 7, 5 + i * LINE_HEIGHT, INK, false);
        }
        pose.popPose();
        return boxHeight;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Caption caption = WORLD.get(player.getId());
        if (caption == null || player == Minecraft.getInstance().player) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        List<FormattedCharSequence> lines = font.split(caption.text(), MAX_WIDTH);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0.0D, player.getBbHeight() + 0.9D, 0.0D);
        pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.025F, -0.025F, 0.025F);
        Matrix4f matrix = pose.last().pose();
        float top = -lines.size() * (float) LINE_HEIGHT;
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            font.drawInBatch(line, -font.width(line) / 2.0F, top + i * LINE_HEIGHT, INK, false, matrix,
                    event.getMultiBufferSource(), Font.DisplayMode.NORMAL, YELLOW, event.getPackedLight());
        }
        pose.popPose();
    }
}
