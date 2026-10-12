package com.craisinlord.integrated_superheroes;

import com.craisinlord.integrated_superheroes.cleanup.CleanupConfig;
import com.craisinlord.integrated_superheroes.cleanup.CleanupEvents;
import com.craisinlord.integrated_superheroes.compat.InfinityGauntletTestEntityBlocker;
import com.craisinlord.integrated_superheroes.hero.HeroContent;
import com.craisinlord.integrated_superheroes.hero.IronManEnergy;
import com.craisinlord.integrated_superheroes.hero.MarvelBalanceConfig;
import com.craisinlord.integrated_superheroes.hero.MarvelWeaponConfig;
import com.craisinlord.integrated_superheroes.registry.GauntletContent;
import com.craisinlord.integrated_superheroes.registry.HeroEntities;
import com.craisinlord.integrated_superheroes.registry.ModCreativeTab;
import com.craisinlord.integrated_superheroes.registry.ModEffects;
import com.craisinlord.integrated_superheroes.registry.SuitEntities;
import com.craisinlord.integrated_superheroes.villain.VillainConfig;
import com.craisinlord.integrated_superheroes.villain.Villains;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(IntegratedSuperheroes.MODID)
public final class IntegratedSuperheroes {
    public static final String MODID = "integrated_superheroes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public IntegratedSuperheroes(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, MarvelBalanceConfig.SPEC, "integrated_superheroes-heroes.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, VillainConfig.SPEC, "integrated_superheroes-villains.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, MarvelWeaponConfig.SPEC, "integrated_superheroes-weapons.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, CleanupConfig.SPEC, "integrated_superheroes-cleanup.toml");
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
