package com.craisinlord.integrated_superheroes.compat.thematic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

final class LanternShards {
    private static final float RAGE_HEALTH = 0.25F;
    private static final float RAGE_CHANCE = 0.3F;
    private static final float LOVE_CHANCE = 0.1F;
    private static final float GREED_CHANCE = 0.1F;
    private static final float COMPASSION_CHANCE = 0.2F;
    private static final float WITHER_SKELETON_DEATH_CHANCE = 0.03F;

    private LanternShards() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(LanternShards::onConversion);
        NeoForge.EVENT_BUS.addListener(LanternShards::onBaby);
        NeoForge.EVENT_BUS.addListener(LanternShards::onDeath);
        NeoForge.EVENT_BUS.addListener(LanternShards::onTrade);
        NeoForge.EVENT_BUS.addListener(LanternShards::onTame);
        NeoForge.EVENT_BUS.addListener(LanternShards::onTotem);
    }

    private static Item shard(String name) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thematic", name + "_shard"));
        return item == Items.AIR ? null : item;
    }

    private static void drop(Entity at, String name, int count) {
        Item item = shard(name);
        if (item == null || at.level().isClientSide() || count <= 0) {
            return;
        }
        ItemEntity entity = new ItemEntity(at.level(), at.getX(), at.getY() + 0.5D, at.getZ(), new ItemStack(item, count));
        entity.setDefaultPickUpDelay();
        at.level().addFreshEntity(entity);
    }

    private static void give(ServerPlayer player, String name) {
        Item item = shard(name);
        if (item == null) {
            return;
        }
        ItemStack stack = new ItemStack(item);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void onConversion(LivingConversionEvent.Post event) {
        if (event.getEntity() instanceof ZombieVillager && event.getOutcome() instanceof Villager villager) {
            drop(villager, "hope", 1);
        }
    }

    private static void onBaby(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() != null && event.getParentA().getRandom().nextFloat() < LOVE_CHANCE) {
            drop(event.getParentA(), "love", 1);
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var victim = event.getEntity();
        if (victim.getType() == EntityType.WITHER) {
            drop(victim, "death", 2);
        } else if (victim.getType() == EntityType.WITHER_SKELETON && victim.getRandom().nextFloat() < WITHER_SKELETON_DEATH_CHANCE) {
            drop(victim, "death", 1);
        }
        if (victim instanceof Enemy && player.getHealth() <= player.getMaxHealth() * RAGE_HEALTH
                && victim.getRandom().nextFloat() < RAGE_CHANCE) {
            drop(victim, "rage", 1);
        }
    }

    private static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getRandom().nextFloat() < GREED_CHANCE) {
            give(player, "greed");
        }
    }

    private static void onTame(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer player && player.getRandom().nextFloat() < COMPASSION_CHANCE) {
            give(player, "compassion");
        }
    }

    private static void onTotem(LivingUseTotemEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            give(player, "life");
        }
    }
}
