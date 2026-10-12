package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.antarchy.content.client.ContractionAfterimages;
import com.craisinlord.antarchy.content.time.TimeDilationApi;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.init.AttachmentsRegistry;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SpeedForceAfterimages {
    private static final int SPEED_FORCE_BLUE = 0x4FB8FF;

    private SpeedForceAfterimages() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        try {
            ContractionAfterimages.addActivator(SpeedForceAfterimages::isSpeeding);
            ContractionAfterimages.addColorProvider(living -> isQuicksilver(living) ? SPEED_FORCE_BLUE : null);
        } catch (LinkageError ignored) {
        }
    }

    private static boolean isSpeeding(LivingEntity living) {
        if (!isQuicksilver(living) || TimeDilationApi.getRate(living) <= 1.0D) {
            return false;
        }
        for (Power power : ((Player) living).getData(AttachmentsRegistry.PLAYER_DATA.get()).getActivePowers()) {
            if (power != null && QuicksilverPower.SPEED_FORCE.equals(power.getName()) && power.isActive()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isQuicksilver(LivingEntity living) {
        if (!(living instanceof Player player) || !player.hasData(AttachmentsRegistry.PLAYER_DATA.get())) {
            return false;
        }
        PlayerData data = player.getData(AttachmentsRegistry.PLAYER_DATA.get());
        return QuicksilverPower.ID.equals(data.getPrimaryPower());
    }
}
