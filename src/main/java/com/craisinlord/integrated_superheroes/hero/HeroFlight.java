package com.craisinlord.integrated_superheroes.hero;

import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForgeMod;

public final class HeroFlight {
    private static final float DEFAULT_FLY_SPEED = 0.05F;

    private HeroFlight() {
    }

    public static void tick(ServerPlayer player, ResourceLocation id, boolean enabled, float speed) {
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
        player.getAbilities().setFlyingSpeed(DEFAULT_FLY_SPEED);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
        HeroPowers.data(player).removeState(BinaryPower.FLYING_STATE);
    }
}
