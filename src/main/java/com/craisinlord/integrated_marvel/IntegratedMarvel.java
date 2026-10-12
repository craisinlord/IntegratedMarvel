package com.craisinlord.integrated_marvel;

import com.craisinlord.integrated_marvel.cleanup.CleanupConfig;
import com.craisinlord.integrated_marvel.cleanup.CleanupEvents;
import com.craisinlord.integrated_marvel.compat.InfinityGauntletTestEntityBlocker;
import com.craisinlord.integrated_marvel.hero.HeroContent;
import com.craisinlord.integrated_marvel.hero.IronManEnergy;
import com.craisinlord.integrated_marvel.hero.MarvelBalanceConfig;
import com.craisinlord.integrated_marvel.hero.MarvelWeaponConfig;
import com.craisinlord.integrated_marvel.registry.GauntletContent;
import com.craisinlord.integrated_marvel.registry.HeroEntities;
import com.craisinlord.integrated_marvel.registry.ModCreativeTab;
import com.craisinlord.integrated_marvel.registry.ModEffects;
import com.craisinlord.integrated_marvel.registry.SuitEntities;
import com.craisinlord.integrated_marvel.villain.VillainConfig;
import com.craisinlord.integrated_marvel.villain.Villains;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(IntegratedMarvel.MODID)
public final class IntegratedMarvel {
    public static final String MODID = "integrated_marvel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public IntegratedMarvel(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, MarvelBalanceConfig.SPEC, "integrated_marvel-heroes.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, VillainConfig.SPEC, "integrated_marvel-villains.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, MarvelWeaponConfig.SPEC, "integrated_marvel-weapons.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, CleanupConfig.SPEC, "integrated_marvel-cleanup.toml");
        ModEffects.register(modEventBus);
        ModCreativeTab.register(modEventBus);
        HeroContent.register(modEventBus);
        SuitEntities.register(modEventBus);
        HeroEntities.register(modEventBus);
        GauntletContent.register(modEventBus);
        IronManEnergy.register(modEventBus);
        CleanupEvents.register(modEventBus);
        NeoForge.EVENT_BUS.register(InfinityGauntletTestEntityBlocker.class);
        Villains.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
