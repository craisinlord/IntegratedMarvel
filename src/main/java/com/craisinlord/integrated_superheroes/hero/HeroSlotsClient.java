package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.platform.InputConstants;
import com.stxllarplanes.marvellegacy.Config;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.init.ModKeyMappings;
import com.stxllarplanes.marvellegacy.network.packet.PowerHandlerPacket;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HeroSlotsClient {
    private static final DecimalFormat COOLDOWN_FORMAT = new DecimalFormat("#.#");
    public static final KeyMapping[] EXTRA_KEYS = {
            key(6, InputConstants.KEY_R),
            key(7, InputConstants.KEY_G),
            key(8, InputConstants.KEY_X),
            key(9, InputConstants.KEY_C),
            key(10, InputConstants.KEY_V)
    };
    private static final boolean[] DOWN = new boolean[EXTRA_KEYS.length];

    private HeroSlotsClient() {
    }

    private static KeyMapping key(int number, int key) {
        return new KeyMapping("key.integrated_superheroes.ability_" + number, KeyConflictContext.IN_GAME, KeyModifier.ALT,
                InputConstants.Type.KEYSYM, key, "key.categories.marvellegacy");
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        for (KeyMapping mapping : EXTRA_KEYS) {
            event.register(mapping);
        }
    }

    public static KeyMapping[] allKeys() {
        return new KeyMapping[]{ModKeyMappings.ABILITY_1_KEY, ModKeyMappings.ABILITY_2_KEY, ModKeyMappings.ABILITY_3_KEY,
                ModKeyMappings.ABILITY_4_KEY, ModKeyMappings.ABILITY_5_KEY,
                EXTRA_KEYS[0], EXTRA_KEYS[1], EXTRA_KEYS[2], EXTRA_KEYS[3], EXTRA_KEYS[4]};
    }

    public static boolean handleKey(InputEvent.Key event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        PlayerData data = HeroPowers.data(player);
        InputConstants.Key input = InputConstants.getKey(event.getKey(), event.getScanCode());
        boolean handled = false;
        for (int i = 0; i < EXTRA_KEYS.length; i++) {
            int slot = 5 + i;
            if (event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null && EXTRA_KEYS[i].isActiveAndMatches(input)) {
                DOWN[i] = true;
                PacketDistributor.sendToServer(new PowerHandlerPacket("press", data.getActivePower(slot).getName(), slot));
                handled = true;
            } else if (event.getAction() == GLFW.GLFW_RELEASE && DOWN[i] && EXTRA_KEYS[i].getKey().equals(input)) {
                DOWN[i] = false;
                PacketDistributor.sendToServer(new PowerHandlerPacket("release", data.getActivePower(slot).getName(), slot));
                handled = true;
            }
        }
        return handled;
    }

    public static void renderOverlay(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || Config.RENDER_POWER_HUD.get() == Config.ToggleState.False || minecraft.player == null) {
            return;
        }
        Font font = minecraft.font;
        float scale = 0.95F;
        int width = (int) (graphics.guiWidth() / scale);
        int height = (int) (graphics.guiHeight() / scale);
        int padding = 5;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        PlayerData data = HeroPowers.data(minecraft.player);
        KeyMapping[] keys = allKeys();
        int row = 0;
        for (int slot = keys.length - 1; slot >= 0; slot--) {
            Power power = data.getActivePower(slot);
            if (power == null || "none".equals(power.getName())) {
                continue;
            }
            String text = line(power, keys[slot]);
            graphics.drawString(font, text, width - font.width(text) - padding, height - 9 - padding - row * 11, 0xFFFFFF, true);
            row++;
        }
        graphics.pose().popPose();
    }

    private static String line(Power power, KeyMapping key) {
        String prefix = power.getCooldown() > 0.0D ? "§c(" + COOLDOWN_FORMAT.format(power.getCooldown()) + ") " : (power.isActive() ? "§e" : "");
        return prefix + power.getDisplayName() + " - " + key.getTranslatedKeyMessage().getString();
    }
}
