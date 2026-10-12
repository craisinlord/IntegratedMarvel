package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.registry.ModEffects;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID)
public final class SoulSeverSync {
    private SoulSeverSync() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAdded(MobEffectEvent.Added event) {
        MobEffectInstance instance = event.getEffectInstance();
        LivingEntity entity = event.getEntity();
        if (instance != null && instance.is(ModEffects.soulSevered()) && entity.level() instanceof ServerLevel level) {
            level.getChunkSource().broadcast(entity, new ClientboundUpdateMobEffectPacket(entity.getId(), instance, false));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRemoved(MobEffectEvent.Remove event) {
        if (!event.isCanceled() && event.getEffect().is(ModEffects.soulSevered())) {
            clear(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().is(ModEffects.soulSevered())) {
            clear(event.getEntity());
        }
    }

    private static void clear(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel level) {
            level.getChunkSource().broadcast(entity, new ClientboundRemoveMobEffectPacket(entity.getId(), ModEffects.soulSevered()));
        }
    }
}
