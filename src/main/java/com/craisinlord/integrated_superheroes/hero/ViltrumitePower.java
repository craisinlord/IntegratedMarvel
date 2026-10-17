package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;
import java.util.function.Supplier;

public final class ViltrumitePower {
    public static final String ID = "viltrumite_blood_omni_man";

    private ViltrumitePower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Viltrumite Blood (Omni-Man)",
                syringe,
                MarvelBalanceConfig.VILTRUMITE,
                List.of(),
                List.of(),
                List.of(),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        ViltrumiteBridge.grant(player);
                        if (player.tickCount % 20 == 0 && MarvelBalanceConfig.VILTRUMITE_SUIT.get()) {
                            ViltrumiteBridge.setSuit(player, MarvelBalanceConfig.VILTRUMITE_SUIT_SKIN.get(), MarvelBalanceConfig.VILTRUMITE_SUIT_CAPE.get());
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        ViltrumiteBridge.revoke(player);
                        if (ViltrumiteBridge.wearsSuit(player, MarvelBalanceConfig.VILTRUMITE_SUIT_SKIN.get())) {
                            ViltrumiteBridge.setSuit(player, ViltrumiteBridge.SUIT_OFF, ViltrumiteBridge.SUIT_OFF);
                        }
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        if (ViltrumiteBridge.hasScourge(player)) {
                            HeroPowers.strip(player, HeroPowers.get(ID));
                            player.displayClientMessage(Component.translatable("integrated_superheroes.viltrumite.scourged"), false);
                        }
                    }
                }
        );
    }

    public static void applyStats(ServerPlayer player) {
        ViltrumiteBridge.applyStats(player,
                MarvelBalanceConfig.VILTRUMITE_BASE_DAMAGE.get().floatValue(),
                MarvelBalanceConfig.VILTRUMITE_DAMAGE_REDUCTION.get().floatValue(),
                MarvelBalanceConfig.VILTRUMITE_DAMAGE_IGNORE.get().floatValue(),
                MarvelBalanceConfig.VILTRUMITE_HEAL_FACTOR.get().floatValue(),
                MarvelBalanceConfig.VILTRUMITE_MAX_FLIGHT_SPEED.get().floatValue(),
                MarvelBalanceConfig.VILTRUMITE_THROTTLE.get().floatValue());
    }
}
