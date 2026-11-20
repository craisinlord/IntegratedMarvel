package com.craisinlord.integrated_superheroes.voidworld;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public record VoidTierCondition(int min, int max) implements LootItemCondition {
    public static final MapCodec<VoidTierCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, VoidTier.MAX).optionalFieldOf("min", 0).forGetter(VoidTierCondition::min),
            Codec.intRange(0, VoidTier.MAX).optionalFieldOf("max", VoidTier.MAX).forGetter(VoidTierCondition::max)
    ).apply(instance, VoidTierCondition::new));

    public static int tierFor(LootContext context) {
        Entity self = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (self instanceof Player player) {
            return VoidTier.forPlayer(player);
        }
        if (VoidTier.isStamped(self)) {
            return VoidTier.tierOf(self);
        }
        Player killer = context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
        if (killer != null) {
            return VoidTier.forPlayer(killer);
        }
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin != null) {
            return VoidTier.at(context.getLevel(), BlockPos.containing(origin));
        }
        return 0;
    }

    @Override
    public LootItemConditionType getType() {
        return VoidWorld.TIER_CONDITION.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of();
    }

    @Override
    public boolean test(LootContext context) {
        int tier = tierFor(context);
        return tier >= this.min && tier <= this.max;
    }
}
