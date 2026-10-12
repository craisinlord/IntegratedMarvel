package com.craisinlord.integrated_marvel.registry;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.craisinlord.integrated_marvel.gauntlet.SoulSeveredMobEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, IntegratedMarvel.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> SOUL_SEVERED = EFFECTS.register("soul_severed", SoulSeveredMobEffect::new);

    private ModEffects() {
    }

    public static Holder<MobEffect> soulSevered() {
        return SOUL_SEVERED;
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
