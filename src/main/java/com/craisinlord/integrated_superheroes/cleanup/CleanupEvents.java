package com.craisinlord.integrated_superheroes.cleanup;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.XGenesLootModifier;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class CleanupEvents {
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, IntegratedSuperheroes.MODID);
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, IntegratedSuperheroes.MODID);
    private static final ResourceLocation EMPTY_SYRINGE = ResourceLocation.parse("marvellegacy:syringe_empty");
    private static final ResourceLocation BLOOD_SAMPLE = ResourceLocation.parse("marvel:villager_blood_sample");

    static {
        LOOT_MODIFIERS.register("cleanup", () -> CleanupLootModifier.CODEC);
        LOOT_MODIFIERS.register("x_genes", () -> XGenesLootModifier.CODEC);
        BIOME_MODIFIERS.register("cleanup", () -> CleanupBiomeModifier.CODEC);
    }

    private CleanupEvents() {
    }

    public static void register(IEventBus modEventBus) {
        LOOT_MODIFIERS.register(modEventBus);
        BIOME_MODIFIERS.register(modEventBus);
        modEventBus.addListener(UnifiedCreativeTab::onBuildContents);
        NeoForge.EVENT_BUS.addListener(CleanupEvents::onItemJoin);
        NeoForge.EVENT_BUS.addListener(CleanupEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(CleanupEvents::onContainerOpen);
        NeoForge.EVENT_BUS.addListener(CleanupEvents::onEntityInteract);
    }

    private static void onItemJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity item) || !Cleanup.needsConversion(item.getItem())) {
            return;
        }
        ItemStack converted = Cleanup.convert(item.getItem());
        if (converted.isEmpty()) {
            event.setCanceled(true);
        } else {
            item.setItem(converted);
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (Cleanup.needsConversion(stack)) {
                inventory.setItem(slot, Cleanup.convert(stack));
            }
        }
    }

    private static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        for (Slot slot : event.getContainer().slots) {
            ItemStack stack = slot.getItem();
            if (Cleanup.needsConversion(stack)) {
                slot.set(Cleanup.convert(stack));
            }
        }
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!CleanupConfig.on(CleanupConfig.LEGACY_MERGE)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!EMPTY_SYRINGE.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                || !(event.getTarget() instanceof LivingEntity target)
                || !(target.getType() == EntityType.VILLAGER || target.getType() == EntityType.WANDERING_TRADER)) {
            return;
        }
        if (event.getLevel().isClientSide() || target.isBaby() || target.hasEffect(MobEffects.CONFUSION)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 2400));
        event.getLevel().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!event.getEntity().getAbilities().instabuild) {
            stack.shrink(1);
        }
        BuiltInRegistries.ITEM.getOptional(BLOOD_SAMPLE).ifPresent(sample -> {
            ItemStack blood = new ItemStack(sample);
            if (!event.getEntity().getInventory().add(blood)) {
                event.getEntity().drop(blood, false);
            }
        });
        event.getEntity().swing(event.getHand(), true);
    }
}
