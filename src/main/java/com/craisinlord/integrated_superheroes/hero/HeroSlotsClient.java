package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.ScorchedGunsHud;
import com.craisinlord.integrated_superheroes.gauntlet.InfinityGauntletStones;
import com.craisinlord.integrated_superheroes.suit.SuitAbilities;
import com.mojang.blaze3d.platform.InputConstants;
import com.stxllarplanes.marvellegacy.Config;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.init.ModKeyMappings;
import com.stxllarplanes.marvellegacy.network.packet.PowerHandlerPacket;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HeroSlotsClient {
    private static List<AbilityRow> cachedRows;
    private static java.lang.ref.WeakReference<LocalPlayer> cachedPlayer = new java.lang.ref.WeakReference<>(null);
    private static int cachedTick;
    private static final DecimalFormat COOLDOWN_FORMAT = new DecimalFormat("#.#");
    private static final int ICON_WIDTH = 10;
    public static final KeyMapping[] EXTRA_KEYS = {
            key(6, InputConstants.KEY_F),
            key(7, InputConstants.KEY_G),
            key(8, InputConstants.KEY_V),
            key(9, InputConstants.KEY_C),
            key(10, InputConstants.KEY_X)
    };
    public static final KeyMapping[] OVERFLOW_KEYS = {
            mouseKey(11, GLFW.GLFW_MOUSE_BUTTON_4),
            mouseKey(12, GLFW.GLFW_MOUSE_BUTTON_5)
    };
    private static final boolean[] DOWN = new boolean[EXTRA_KEYS.length];

    private HeroSlotsClient() {
    }

    private static KeyMapping key(int number, int key) {
        return new KeyMapping("key.integrated_superheroes.ability_" + number, KeyConflictContext.IN_GAME, KeyModifier.ALT,
                InputConstants.Type.KEYSYM, key, "key.categories.marvellegacy");
    }

    private static KeyMapping mouseKey(int number, int button) {
        return new KeyMapping("key.integrated_superheroes.ability_" + number, KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputConstants.Type.MOUSE, button, "key.categories.marvellegacy");
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        for (KeyMapping mapping : EXTRA_KEYS) {
            event.register(mapping);
        }
        for (KeyMapping mapping : OVERFLOW_KEYS) {
            event.register(mapping);
        }
    }

    public static KeyMapping[] allKeys() {
        return new KeyMapping[]{ModKeyMappings.ABILITY_1_KEY, ModKeyMappings.ABILITY_2_KEY, ModKeyMappings.ABILITY_3_KEY,
                ModKeyMappings.ABILITY_4_KEY, ModKeyMappings.ABILITY_5_KEY,
                EXTRA_KEYS[0], EXTRA_KEYS[1], EXTRA_KEYS[2], EXTRA_KEYS[3], EXTRA_KEYS[4]};
    }

    public static KeyMapping[] slotKeys() {
        return new KeyMapping[]{ModKeyMappings.ABILITY_1_KEY, ModKeyMappings.ABILITY_2_KEY, ModKeyMappings.ABILITY_3_KEY,
                ModKeyMappings.ABILITY_4_KEY, ModKeyMappings.ABILITY_5_KEY,
                EXTRA_KEYS[0], EXTRA_KEYS[1], EXTRA_KEYS[2], EXTRA_KEYS[3], EXTRA_KEYS[4],
                OVERFLOW_KEYS[0], OVERFLOW_KEYS[1]};
    }

    public static boolean handleKey(InputEvent.Key event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        PlayerData data = HeroPowers.data(player);
        InputConstants.Key input = InputConstants.getKey(event.getKey(), event.getScanCode());
        boolean handled = false;
        KeyMapping[] legacyKeys = allKeys();
        for (int slot = 0; slot < 5; slot++) {
            if (UnifiedSlots.suitOwned(slot) && legacyKeys[slot].matches(event.getKey(), event.getScanCode())) {
                handled = true;
            }
        }
        for (int i = 0; i < EXTRA_KEYS.length; i++) {
            int slot = 5 + i;
            if (event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null && EXTRA_KEYS[i].isActiveAndMatches(input)) {
                if (!UnifiedSlots.suitOwned(slot)) {
                    DOWN[i] = true;
                    PacketDistributor.sendToServer(new PowerHandlerPacket("press", data.getActivePower(slot).getName(), slot));
                }
                handled = true;
            } else if (event.getAction() == GLFW.GLFW_RELEASE && DOWN[i] && EXTRA_KEYS[i].getKey().equals(input)) {
                DOWN[i] = false;
                PacketDistributor.sendToServer(new PowerHandlerPacket("release", data.getActivePower(slot).getName(), slot));
                handled = true;
            }
        }
        return handled;
    }

    public static boolean hudShown() {
        Minecraft minecraft = Minecraft.getInstance();
        return !minecraft.options.hideGui && Config.RENDER_POWER_HUD.get() != Config.ToggleState.False && minecraft.player != null;
    }

    public static void renderOverlay(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!hudShown() || UnifiedSlots.hostedElsewhere()) {
            return;
        }
        Font font = minecraft.font;
        float scale = 0.95F;
        int width = (int) (graphics.guiWidth() / scale);
        int height = (int) (graphics.guiHeight() / scale);
        int padding = 5;
        List<AbilityRow> rows = rows(minecraft.player);
        if (rows.isEmpty()) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        int textWidth = 0;
        boolean icons = false;
        for (AbilityRow row : rows) {
            Component text = line(row);
            lines.add(text);
            icons |= !row.icon().isEmpty();
            textWidth = Math.max(textWidth, font.width(text));
        }
        int iconColumn = icons ? ICON_WIDTH : 0;
        int blockWidth = textWidth + iconColumn;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int blockHeight = lines.size() * 11 - 2;
        HeroHudClientConfig.Corner corner = HeroHudClientConfig.corner();
        boolean rightSide = corner == HeroHudClientConfig.Corner.BOTTOM_RIGHT || corner == HeroHudClientConfig.Corner.TOP_RIGHT;
        boolean bottom = corner == HeroHudClientConfig.Corner.BOTTOM_RIGHT || corner == HeroHudClientConfig.Corner.BOTTOM_LEFT;
        int right = (rightSide ? width - padding : padding + blockWidth) + HeroHudClientConfig.offsetX();
        int top = (bottom ? height - padding - blockHeight : padding) + HeroHudClientConfig.offsetY();
        if (corner == HeroHudClientConfig.Corner.BOTTOM_RIGHT && HeroHudClientConfig.avoidAmmoHud()
                && ScorchedGunsHud.showsAmmo(minecraft.player)) {
            int ammoLeft = (int) ((graphics.guiWidth() * 0.88D - 24) / scale);
            int ammoTop = (int) ((graphics.guiHeight() * 0.8D - 13) / scale);
            int ammoBottom = (int) ((graphics.guiHeight() * 0.8D + 19) / scale);
            if (overlaps(right - blockWidth, top, right, top + blockHeight, ammoLeft, ammoTop, width, ammoBottom)) {
                int above = ammoTop - blockHeight;
                if (above >= padding && !overlapsGauntlet(minecraft, graphics, scale, right - blockWidth, above, right, ammoTop)) {
                    top = above;
                } else {
                    right = ammoLeft;
                }
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            Component text = lines.get(i);
            int y = top + i * 11;
            int x = rightSide ? right - font.width(text) : right - textWidth;
            graphics.drawString(font, text, x, y, 0xFFFFFF, true);
            AbilityRow row = rows.get(i);
            if (!row.icon().isEmpty()) {
                int iconX = rightSide ? x - ICON_WIDTH : right - blockWidth;
                graphics.pose().pushPose();
                graphics.pose().translate(iconX, y - 0.5F, 0.0F);
                graphics.pose().scale(0.5F, 0.5F, 1.0F);
                graphics.renderItem(row.icon(), 0, 0);
                graphics.pose().popPose();
            }
        }
        graphics.pose().popPose();
    }

    public static List<AbilityRow> rows(LocalPlayer player) {
        if (cachedRows == null || cachedPlayer.get() != player || cachedTick != player.tickCount) {
            cachedRows = buildRows(player);
            cachedPlayer = new java.lang.ref.WeakReference<>(player);
            cachedTick = player.tickCount;
        }
        return cachedRows;
    }

    private static List<AbilityRow> buildRows(LocalPlayer player) {
        List<AbilityRow> rows = new ArrayList<>();
        PlayerData data = HeroPowers.data(player);
        KeyMapping[] keys = slotKeys();
        for (int slot = 0; slot < HeroSlots.SLOTS && slot < data.getActivePowers().size(); slot++) {
            Power power = data.getActivePower(slot);
            if (power == null || "none".equals(power.getName())) {
                continue;
            }
            rows.add(new AbilityRow(Component.literal(power.getDisplayName()), slot, keys[slot].getTranslatedKeyMessage(),
                    power.getCooldown(), power.isActive(), AbilityRow.Lock.NONE, Component.empty(),
                    ItemStack.EMPTY, SuitAbilities.isSuitAbility(power.getName()), false));
        }
        if (HeroHudClientConfig.unifiedSuitAbilities()) {
            rows.addAll(UnifiedSlots.suitRows(player, keys));
        }
        rows.sort(Comparator.comparingInt(row -> row.slot() < 0 ? Integer.MAX_VALUE : row.slot()));
        return rows;
    }

    private static boolean overlaps(int left, int top, int right, int bottom, int otherLeft, int otherTop, int otherRight, int otherBottom) {
        return left < otherRight && right > otherLeft && top < otherBottom && bottom > otherTop;
    }

    private static boolean overlapsGauntlet(Minecraft minecraft, GuiGraphics graphics, float scale, int left, int top, int right, int bottom) {
        if (InfinityGauntletStones.equippedGauntlet(minecraft.player).isEmpty()) {
            return false;
        }
        int panelLeft = (int) ((graphics.guiWidth() / 2 + 162) / scale);
        int panelTop = (int) ((graphics.guiHeight() / 2 + 59) / scale);
        int panelRight = (int) ((graphics.guiWidth() / 2 + 207) / scale);
        int panelBottom = (int) ((graphics.guiHeight() / 2 + 111) / scale);
        return overlaps(left, top, right, bottom, panelLeft, panelTop, panelRight, panelBottom);
    }

    private static Component line(AbilityRow row) {
        boolean unkeyed = row.slot() < 0;
        MutableComponent text = Component.empty();
        if (row.lock() != AbilityRow.Lock.NONE) {
            text.append(Component.literal("🔒 ").withStyle(row.lock() == AbilityRow.Lock.EXCLUSIVE ? ChatFormatting.YELLOW : ChatFormatting.RED));
        } else if (row.cooldown() > 0.0D && !row.active()) {
            text.append(Component.literal("(" + COOLDOWN_FORMAT.format(row.cooldown()) + ") ").withStyle(ChatFormatting.RED));
        }
        ChatFormatting color = unkeyed ? ChatFormatting.GRAY : (row.active() ? ChatFormatting.YELLOW : ChatFormatting.WHITE);
        MutableComponent body = Component.empty().withStyle(color);
        body.append(row.suit() ? Component.literal(row.name().getString()) : row.name());
        if (row.suffix() != null && !row.suffix().getString().isEmpty()) {
            body.append(row.suffix());
        }
        body.append(" - ");
        body.append(unkeyed ? Component.literal("?").withStyle(ChatFormatting.DARK_GRAY) : row.key());
        text.append(body);
        if (row.emphasised()) {
            return Component.empty().append(text).withStyle(style -> style.withBold(true).withColor(ChatFormatting.RED));
        }
        return text;
    }
}
