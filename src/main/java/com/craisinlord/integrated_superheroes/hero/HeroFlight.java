package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteBridge;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteCompat;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HeroFlight {
    private static final float DEFAULT_FLY_SPEED = 0.05F;
    private static final Map<String, ResourceLocation> IDS = new ConcurrentHashMap<>();

    private HeroFlight() {
    }

    public static void register(String power, ResourceLocation id) {
        IDS.put(power, id);
    }

    public static boolean allowed(Player player) {
        String power = HeroPowers.data(player).getPrimaryPower();
        if (ShazamPower.ID.equals(power)) {
            return ShazamPower.isPowered(player);
        }
        if (KryptonianPower.ID.equals(power)) {
            return !KryptonianPower.weakened(player);
        }
        return true;
    }

    public static void grant(ServerPlayer player) {
        ResourceLocation id = IDS.get(HeroPowers.data(player).getPrimaryPower());
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (id != null && flight != null && !flight.hasModifier(id)) {
            flight.addTransientModifier(new AttributeModifier(id, 1.0D, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static void tick(ServerPlayer player, ResourceLocation id, boolean enabled, float speed) {
        String power = HeroPowers.data(player).getPrimaryPower();
        if (power != null) {
            IDS.putIfAbsent(power, id);
        }
        if (ModCompat.VILTRUMITE && ViltrumiteCompat.listed(player)) {
            boolean inFlight = ViltrumiteBridge.inFlight(player);
            if (!enabled && inFlight && !ViltrumiteBridge.isViltrumite(player)) {
                ViltrumiteBridge.stopFlight(player);
            }
            enabled = enabled && inFlight;
        }
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (enabled) {
            if (flight != null && !flight.hasModifier(id)) {
                flight.addTransientModifier(new AttributeModifier(id, 1.0D, AttributeModifier.Operation.ADD_VALUE));
            }
            if (player.getAbilities().getFlyingSpeed() != speed) {
                player.getAbilities().setFlyingSpeed(speed);
                player.onUpdateAbilities();
            }
        } else if (flight != null && flight.hasModifier(id)) {
            remove(player, id);
        }
        boolean flying = player.getAbilities().flying;
        PlayerData data = HeroPowers.data(player);
        if (flying != data.hasState(BinaryPower.FLYING_STATE)) {
            if (flying) {
                data.addState(BinaryPower.FLYING_STATE);
            } else {
                data.removeState(BinaryPower.FLYING_STATE);
            }
            PlayerData.sync(player);
        }
    }

    public static void remove(ServerPlayer player, ResourceLocation id) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null) {
            flight.removeModifier(id);
        }
        if (ModCompat.VILTRUMITE && ViltrumiteBridge.inFlight(player) && !ViltrumiteBridge.isViltrumite(player)) {
            ViltrumiteBridge.stopFlight(player);
        }
        player.getAbilities().setFlyingSpeed(DEFAULT_FLY_SPEED);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
        HeroPowers.data(player).removeState(BinaryPower.FLYING_STATE);
    }
}
