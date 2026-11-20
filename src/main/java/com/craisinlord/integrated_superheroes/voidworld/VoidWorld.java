package com.craisinlord.integrated_superheroes.voidworld;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class VoidWorld {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, IntegratedSuperheroes.id("the_void"));
    public static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, IntegratedSuperheroes.id("the_void"));
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, IntegratedSuperheroes.id("the_void"));
    public static final ResourceKey<StructureProcessorList> PRUNED_PROCESSORS = ResourceKey.create(Registries.PROCESSOR_LIST, IntegratedSuperheroes.id("void/pruned"));
    public static final TagKey<Biome> IS_THE_VOID = TagKey.create(Registries.BIOME, IntegratedSuperheroes.id("is_the_void"));
    public static final TagKey<Block> DEBRIS_STRIP = TagKey.create(Registries.BLOCK, IntegratedSuperheroes.id("void_debris_strip"));
    public static final TagKey<Block> DEBRIS_KEEP_ENTITY = TagKey.create(Registries.BLOCK, IntegratedSuperheroes.id("void_debris_keep_entity"));
    public static final TagKey<Block> DEBRIS_REJECT = TagKey.create(Registries.BLOCK, IntegratedSuperheroes.id("void_debris_reject"));

    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, IntegratedSuperheroes.MODID);
    private static final DeferredRegister<PlacementModifierType<?>> PLACEMENTS = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, IntegratedSuperheroes.MODID);
    private static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, IntegratedSuperheroes.MODID);
    private static final DeferredRegister<LootItemConditionType> CONDITIONS = DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, IntegratedSuperheroes.MODID);

    public static final DeferredHolder<Feature<?>, PrunedDebrisFeature> PRUNED_DEBRIS = FEATURES.register("pruned_debris", PrunedDebrisFeature::new);
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<VoidTierChance>> TIER_CHANCE = PLACEMENTS.register("void_tier_chance", () -> () -> VoidTierChance.CODEC);
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<PrunedDecayProcessor>> PRUNED_DECAY = PROCESSORS.register("pruned_decay", () -> () -> PrunedDecayProcessor.CODEC);
    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> TIER_CONDITION = CONDITIONS.register("void_tier", () -> new LootItemConditionType(VoidTierCondition.CODEC));

    private VoidWorld() {
    }

    public static boolean isVoid(Level level) {
        return level != null && level.dimension().equals(LEVEL);
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, VoidConfig.SPEC, "integrated_superheroes-void-world.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, com.craisinlord.integrated_superheroes.voidworld.client.sky.VoidSkyConfig.SPEC, "integrated_superheroes-void-sky-client.toml");
        FEATURES.register(modEventBus);
        PLACEMENTS.register(modEventBus);
        PROCESSORS.register(modEventBus);
        CONDITIONS.register(modEventBus);
        com.craisinlord.integrated_superheroes.voidworld.pruning.PruningEntities.register(modEventBus);
        modEventBus.addListener(VoidWorld::onConfig);
        modEventBus.addListener(VoidWorld::onConfigReload);
        modEventBus.addListener(VoidPalettePack::onAddPackFinders);
        NeoForge.EVENT_BUS.register(VoidEvents.class);
        com.craisinlord.integrated_superheroes.voidworld.vehicles.VoidVehicles.register();
    }

    private static void onConfig(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == VoidConfig.SPEC) {
            VoidTier.reloadEdges();
        }
    }

    private static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == VoidConfig.SPEC) {
            VoidTier.reloadEdges();
        }
    }
}
