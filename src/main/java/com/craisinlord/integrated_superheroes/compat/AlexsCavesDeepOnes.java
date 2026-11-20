package com.craisinlord.integrated_superheroes.compat;

import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.living.DeepOneBaseEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

public final class AlexsCavesDeepOnes {
    private static final ResourceKey<Biome> ABYSSAL_CHASM = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("alexscaves", "abyssal_chasm"));

    private AlexsCavesDeepOnes() {
    }

    public static boolean inChasm(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(ABYSSAL_CHASM);
    }

    public static Mob summon(ServerLevel level, LivingEntity owner, Vec3 at, boolean mage, int ticks) {
        EntityType<? extends DeepOneBaseEntity> type = mage ? ACEntityRegistry.DEEP_ONE_MAGE.get() : ACEntityRegistry.DEEP_ONE_KNIGHT.get();
        DeepOneBaseEntity deepOne = type.create(level);
        if (deepOne == null) {
            return null;
        }
        deepOne.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
        deepOne.setSummonedBy(owner, ticks);
        return deepOne;
    }
}
