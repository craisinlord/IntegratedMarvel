package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.hero.HeroAnimations;
import com.craisinlord.integrated_superheroes.hero.MarvelBalanceConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ViltrumiteAnimations {
    public static final int THUNDERCLAP = 0;
    public static final int PUNCH = 1;
    public static final int CHOP = 2;

    private ViltrumiteAnimations() {
    }

    public static boolean play(ServerPlayer player, int move) {
        if (!ModCompat.VILTRUMITE || !MarvelBalanceConfig.VILTRUMITE_ANIMATIONS.get() || ViltrumiteBridge.isViltrumite(player)) {
            return false;
        }
        HeroAnimations.stop(player);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ViltrumiteAnimationPayload(player.getId(), move, player.getRandom().nextInt(4)));
        return true;
    }
}
