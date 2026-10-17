package com.craisinlord.integrated_superheroes.compat;

import dev.baranhan.viltrumitecore.config.ViltrumiteCoreConfig;
import dev.baranhan.viltrumitecore.effect.ViltrumiteEffects;
import dev.baranhan.viltrumitecore.entity.ViltrumiteFakePlayer;
import dev.baranhan.viltrumitecore.item.ViltrumiteItems;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumitecore.util.ViltrumiteCosmeticsPlayer;
import dev.baranhan.viltrumitecore.util.ViltrumiteStatHolder;
import dev.baranhan.viltrumiteflight.config.ViltrumiteConfig;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ViltrumiteBridge {
    public static final String SUIT_OFF = "off";

    private ViltrumiteBridge() {
    }

    public static boolean isViltrumite(Entity entity) {
        return entity instanceof ViltrumiteCorePlayer core && core.isViltrumite();
    }

    public static boolean inFlight(Entity entity) {
        return entity instanceof ViltrumiteFlightPlayer flight && flight.getFlightState() != FlightState.NONE;
    }

    public static boolean isNpc(Entity entity) {
        return entity instanceof ViltrumiteFakePlayer;
    }

    public static void grant(ServerPlayer player) {
        ViltrumiteCorePlayer core = (ViltrumiteCorePlayer) player;
        if (!core.hasChosenRace()) {
            core.setChosenRace(true);
        }
        if (!core.isViltrumite()) {
            core.setViltrumite(true);
        }
    }

    public static void revoke(ServerPlayer player) {
        ViltrumiteCorePlayer core = (ViltrumiteCorePlayer) player;
        if (!core.hasChosenRace()) {
            core.setChosenRace(true);
        }
        if (!core.isViltrumite()) {
            return;
        }
        if (core.isGrabbingAnything()) {
            core.releaseTarget();
        }
        core.setBlocking(false);
        core.setBarraging(false);
        core.setChargingPunch(false);
        core.setSuperSpeed(false);
        ViltrumiteFlightPlayer flight = (ViltrumiteFlightPlayer) player;
        if (flight.getFlightState() != FlightState.NONE) {
            flight.stopFlight();
        }
        core.setViltrumite(false);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public static void markChosen(ServerPlayer player) {
        ViltrumiteCorePlayer core = (ViltrumiteCorePlayer) player;
        if (!core.hasChosenRace()) {
            core.setChosenRace(true);
        }
    }

    public static void applyStats(Player player, float baseDamage, float reduction, float ignore, float heal, float maxFlight, float throttle) {
        ViltrumiteStatHolder stats = (ViltrumiteStatHolder) player;
        if (stats.getBaseDamage() != baseDamage) {
            stats.setBaseDamage(baseDamage);
        }
        if (stats.getDamageReduction() != reduction) {
            stats.setDamageReduction(reduction);
        }
        if (stats.getDamageIgnoreThreshold() != ignore) {
            stats.setDamageIgnoreThreshold(ignore);
        }
        if (stats.getHealFactor() != heal) {
            stats.setHealFactor(heal);
        }
        ViltrumiteFlightPlayer flight = (ViltrumiteFlightPlayer) player;
        if (flight.getMaxFlightSpeed() != maxFlight) {
            flight.setMaxFlightSpeed(maxFlight);
        }
        if (flight.getThrottleSpeed() != throttle) {
            flight.setThrottleSpeed(throttle);
        }
    }

    public static void scaleBaseDamage(LivingEntity entity, float multiplier) {
        if (entity instanceof ViltrumiteStatHolder stats) {
            stats.setBaseDamage(stats.getBaseDamage() * multiplier);
        }
    }

    public static void capReduction(LivingEntity entity, float max) {
        if (entity instanceof ViltrumiteStatHolder stats && stats.getDamageReduction() > max) {
            stats.setDamageReduction(max);
        }
    }

    public static void setSuit(ServerPlayer player, String skin, String cape) {
        ViltrumiteCosmeticsPlayer cosmetics = (ViltrumiteCosmeticsPlayer) player;
        if (!skin.equals(cosmetics.getViltrumiteSkin())) {
            cosmetics.setViltrumiteSkin(skin);
        }
        if (!cape.equals(cosmetics.getViltrumiteCape())) {
            cosmetics.setViltrumiteCape(cape);
        }
    }

    public static boolean wearsSuit(ServerPlayer player, String skin) {
        return skin.equals(((ViltrumiteCosmeticsPlayer) player).getViltrumiteSkin());
    }

    public static boolean hasScourge(LivingEntity entity) {
        return entity.hasEffect(ViltrumiteEffects.SCOURGE_VIRUS);
    }

    public static ItemStack bloodSample(int count) {
        return new ItemStack(ViltrumiteItems.VILTRUMITE_BLOOD_SAMPLE.get(), count);
    }

    public static void configure(boolean serumOnly, boolean takeoffBreaksBlocks, boolean humansBleed) {
        ViltrumiteCoreConfig core = ViltrumiteCoreConfig.INSTANCE;
        if (core != null && serumOnly) {
            core.shouldAskRace = false;
            core.isViltrumiteByDefault = false;
        }
        if (core != null) {
            core.shouldHumansBleed = humansBleed;
        }
        ViltrumiteConfig flight = ViltrumiteConfig.INSTANCE;
        if (flight != null) {
            flight.breakBlocksOnTakeoff = takeoffBreaksBlocks;
        }
    }
}
