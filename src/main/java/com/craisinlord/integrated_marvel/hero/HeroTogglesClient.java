package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.stxllarplanes.marvellegacy.client.gui.overlay.wheel.WheelOverlayHandler;
import com.stxllarplanes.marvellegacy.client.gui.overlay.wheel.WheelSlice;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.network.packet.PowerSwapPacket;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = IntegratedMarvel.MODID, value = Dist.CLIENT)
public final class HeroTogglesClient {
    private static final String WHEEL_STATE = "WheelOpen_HeroAbilities";
    private static final ResourceLocation FALLBACK_ICON = ResourceLocation.fromNamespaceAndPath("marvellegacy", "textures/gui/overlay/super_jump_power.png");

    private HeroTogglesClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        HeroPower hero = HeroPowers.activeHero(player);
        if (hero == null) {
            return;
        }
        PlayerData data = HeroPowers.data(player);
        KeyMapping[] keys = HeroSlotsClient.allKeys();
        for (int slot = 0; slot < keys.length; slot++) {
            Power power = data.getActivePower(slot);
            if (power == null) {
                continue;
            }
            if (HeroToggles.WHEEL_ABILITY.equals(power.getName())) {
                if (!hero.toggles().isEmpty()) {
                    setOpen(player, hero, data, keys[slot].isDown());
                }
                continue;
            }
            HeroWheels.Wheel wheel = HeroWheels.get(power.getName());
            if (wheel != null) {
                setWheelOpen(player, wheel, data, keys[slot].isDown());
            }
        }
    }

    private static void setWheelOpen(LocalPlayer player, HeroWheels.Wheel wheel, PlayerData data, boolean held) {
        String state = "WheelOpen_" + wheel.ability();
        boolean open = held && Minecraft.getInstance().screen == null;
        if (open && !data.hasState(state)) {
            WheelOverlayHandler.openWheel(wheel, wheelSlices(player, wheel), true);
            data.addState(state);
        } else if (!open && data.hasState(state)) {
            WheelOverlayHandler.openWheel(wheel, wheelSlices(player, wheel), false);
            data.removeState(state);
        }
    }

    private static List<WheelSlice> wheelSlices(LocalPlayer player, HeroWheels.Wheel wheel) {
        return wheel.options().stream().map(option -> {
            boolean available = wheel.available().test(player, option);
            return new WheelSlice(
                    FALLBACK_ICON,
                    new ItemStack(option.icon().get()),
                    true,
                    Component.literal((available ? "" : "§7") + option.label()),
                    () -> PacketDistributor.sendToServer(new HeroWheelChoicePayload(wheel.ability(), option.id()))
            );
        }).toList();
    }

    private static void setOpen(LocalPlayer player, HeroPower hero, PlayerData data, boolean held) {
        boolean open = held && Minecraft.getInstance().screen == null;
        if (open && !data.hasState(WHEEL_STATE)) {
            WheelOverlayHandler.openWheel(HeroTogglesClient.class, slices(player, hero), true);
            data.addState(WHEEL_STATE);
        } else if (!open && data.hasState(WHEEL_STATE)) {
            WheelOverlayHandler.openWheel(HeroTogglesClient.class, slices(player, hero), false);
            data.removeState(WHEEL_STATE);
        }
    }

    private static List<WheelSlice> slices(LocalPlayer player, HeroPower hero) {
        return hero.toggles().stream().map(toggle -> new WheelSlice(
                FALLBACK_ICON,
                new ItemStack(toggle.icon().get()),
                true,
                Component.literal((HeroToggles.isOn(player, toggle) ? "Disable " : "Enable ") + toggle.label()),
                () -> PacketDistributor.sendToServer(new PowerSwapPacket(toggle.flag(), "toggle", List.of()))
        )).toList();
    }
}
