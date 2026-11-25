package com.craisinlord.integrated_superheroes.compat.thematic;

import com.craisinlord.integrated_superheroes.hero.ServerState;
import bond.thematic.api.content.pocket.PocketDimension;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ThematicQuantumDive {
    private static final TagKey<Item> DIVERS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "quantum_divers"));
    private static final ResourceLocation REALM = ResourceLocation.parse("thematic:quantum_realm");
    private static final float MAX_SCALE = 0.15F;
    private static final Map<UUID, Integer> DIVING = ServerState.map(new ConcurrentHashMap<>());

    private ThematicQuantumDive() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(ThematicQuantumDive::onTick);
        NeoForge.EVENT_BUS.addListener(ThematicQuantumDive::onLogout);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DIVING.remove(event.getEntity().getUUID());
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.isCrouching() || !ThematicOptions.on(ThematicOptions.QUANTUM_DIVE) || !player.onGround()
                || player.getScale() > MAX_SCALE || !player.getItemBySlot(EquipmentSlot.CHEST).is(DIVERS) || PocketDimension.isInside(player)) {
            if (DIVING.remove(player.getUUID()) != null) {
                player.displayClientMessage(Component.empty(), true);
            }
            return;
        }
        int ticks = DIVING.merge(player.getUUID(), 1, Integer::sum);
        int needed = ThematicOptions.QUANTUM_DIVE_TICKS.get();
        if (ticks == 20) {
            player.displayClientMessage(Component.translatable("integrated_superheroes.quantum.shrinking").withStyle(ChatFormatting.AQUA), true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.6F, 1.8F);
        }
        if (ticks >= 20 && ticks % 4 == 0) {
            double spread = 0.6D * (1.0D - ticks / (double) needed) + 0.1D;
            player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.05D, player.getZ(), 3, spread, 0.05D, spread, 0.01D);
        }
        if (ticks == needed - 20) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false, false));
        }
        if (ticks >= needed) {
            DIVING.remove(player.getUUID());
            player.serverLevel().sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 0.1D, player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);
            player.displayClientMessage(Component.translatable("integrated_superheroes.quantum.entered").withStyle(ChatFormatting.AQUA), true);
            PocketDimension.enter(player, REALM);
        }
    }
}
