package com.craisinlord.integrated_superheroes.hero;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public final class XGenesLootModifier extends LootModifier {
    public static final MapCodec<XGenesLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, XGenesLootModifier::new));
    private static final ResourceLocation X_GENES = ResourceLocation.parse("marvel:x_genes");
    private static final ResourceLocation HYDRA_BASE = ResourceLocation.parse("marvel:chests/hydra_base");
    private static final ResourceLocation HYDRA_BASE_MAP = ResourceLocation.parse("marvel:chests/hydra_base_map");
    private static final ResourceLocation HYDRA_OUTPOST = ResourceLocation.parse("marvel:chests/hydra_outpost");
    private static final ResourceLocation WITCH = ResourceLocation.parse("minecraft:entities/witch");

    public XGenesLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        double chance = chance(context);
        if (chance > 0.0D && context.getRandom().nextDouble() < chance) {
            BuiltInRegistries.ITEM.getOptional(X_GENES).ifPresent(item -> generatedLoot.add(new ItemStack(item)));
        }
        return generatedLoot;
    }

    private static double chance(LootContext context) {
        ResourceLocation table = context.getQueriedLootTableId();
        if (HYDRA_BASE.equals(table) || HYDRA_BASE_MAP.equals(table)) {
            return MarvelBalanceConfig.X_GENES_HYDRA_BASE_CHANCE.get();
        }
        if (HYDRA_OUTPOST.equals(table)) {
            return MarvelBalanceConfig.X_GENES_HYDRA_OUTPOST_CHANCE.get();
        }
        if (WITCH.equals(table) && context.hasParam(LootContextParams.LAST_DAMAGE_PLAYER)) {
            return MarvelBalanceConfig.X_GENES_WITCH_CHANCE.get();
        }
        return 0.0D;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
