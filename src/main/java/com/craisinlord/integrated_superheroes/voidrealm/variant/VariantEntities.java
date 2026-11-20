package com.craisinlord.integrated_superheroes.voidrealm.variant;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.skin.SerumSkin;
import com.craisinlord.integrated_superheroes.hero.skin.SerumSkins;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class VariantEntities {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, IntegratedSuperheroes.MODID);
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};

    public static final DeferredHolder<EntityType<?>, EntityType<VariantEntity>> VARIANT = TYPES.register("variant",
            () -> EntityType.Builder.<VariantEntity>of(VariantEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("variant"));

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IntegratedSuperheroes.MODID);

    public static final net.neoforged.neoforge.registries.DeferredItem<Item> SPAWN_EGG = ITEMS.register("variant_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(VARIANT, 0x3A2E1F, 0xD98A2B, new Item.Properties()));

    private VariantEntities() {
    }

    static void register(IEventBus modBus) {
        TYPES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(VariantEntities::attributes);
        modBus.addListener(VariantEntities::creativeTabs);
    }

    private static void creativeTabs(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS) event.accept(SPAWN_EGG.get());
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(VARIANT.get(), VariantEntity.createAttributes().build());
    }

    static List<Item> pieces(String set) {
        List<Item> out = new ArrayList<>();
        ResourceLocation base = ResourceLocation.tryParse(set.contains(":") ? set : "marvel:" + set);
        if (base == null) {
            return out;
        }
        for (String piece : PIECES) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "_" + piece);
            Item item = BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : Items.AIR;
            out.add(item);
        }
        return out;
    }

    static VariantEntity create(ServerLevel level, Vec3 at, VariantProfile profile, String marvelSet) {
        VariantEntity mob = VARIANT.get().create(level);
        if (mob == null) {
            return null;
        }
        mob.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        if (marvelSet != null && !marvelSet.isEmpty()) {
            List<Item> items = pieces(marvelSet);
            for (int i = 0; i < ARMOR.length; i++) {
                if (items.get(i) != Items.AIR) {
                    mob.setItemSlot(ARMOR[i], new ItemStack(items.get(i)));
                }
            }
        } else if (profile.kind() == VariantKind.SERUM || profile.kind() == VariantKind.VILTRUMITE) {
            SerumSkin skin = SerumSkins.byId(profile.look()).orElse(null);
            if (skin != null) {
                mob.setColors(skin.primary(), skin.secondary());
            }
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0.0F);
        }
        return mob;
    }
}
