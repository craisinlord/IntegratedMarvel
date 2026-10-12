package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.IronManSuits;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

import java.util.Map;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class MarvelSuitArmorEvents {
    private static final Map<String, double[]> SUIT_ARMOR = Map.ofEntries(
            Map.entry("iron_man_iron", new double[]{3, 7, 6, 3, 1.0}),
            Map.entry("iron_man_iron_diamond", new double[]{4, 9, 7, 4, 2.5}),
            Map.entry("iron_man_diamond", new double[]{4, 10, 8, 4, 3.0}),
            Map.entry("reinforced_leather", new double[]{3, 8, 6, 3, 2.0}),
            Map.entry("reinforced_leather_no_helmet", new double[]{0, 9, 7, 4, 2.0}),
            Map.entry("upgraded_reinforced_leather", new double[]{4, 9, 7, 4, 2.5}),
            Map.entry("vibranium_weave", new double[]{4, 9, 7, 4, 2.5}),
            Map.entry("vibranium_nanite", new double[]{4, 10, 8, 4, 3.0}),
            Map.entry("thor", new double[]{4, 10, 8, 4, 3.0}),
            Map.entry("captain_marvel", new double[]{4, 10, 8, 4, 3.0})
    );

    private static final double[] COSTUME_ARMOR = {3, 6, 5, 2, 1.0};
    private static final double[] PANTHER_ARMOR = {5, 10, 8, 5, 3.5};
    private static final java.util.Set<String> PANTHER_MATERIALS = java.util.Set.of("vibranium_weave", "vibranium_nanite");

    private MarvelSuitArmorEvents() {
    }

    @SubscribeEvent
    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        if (!(event.getItemStack().getItem() instanceof ArmorItem armor)) {
            return;
        }
        boolean costume = MarvelCostumes.isCostume(armor);
        ResourceLocation material = armor.getMaterial().unwrapKey().map(key -> key.location()).orElse(null);
        boolean marvelMaterial = material != null && "marvel".equals(material.getNamespace());
        IronManSuits.Stats ironMan = IronManSuits.isLoaded() ? IronManSuits.forItem(armor) : null;
        double[] stats;
        if (ironMan != null) {
            stats = null;
        } else if (marvelMaterial && PANTHER_MATERIALS.contains(material.getPath())) {
            stats = PANTHER_ARMOR;
        } else if (costume) {
            stats = COSTUME_ARMOR;
        } else {
            if (!marvelMaterial) {
                return;
            }
            stats = SUIT_ARMOR.get(material.getPath());
            if (stats == null) {
                return;
            }
        }

        ArmorItem.Type type = armor.getType();
        int index = switch (type) {
            case HELMET -> 0;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 2;
            case BOOTS -> 3;
            default -> -1;
        };
        if (index < 0) {
            return;
        }

        ResourceLocation id = ResourceLocation.withDefaultNamespace("armor." + type.getName());
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());
        event.removeAllModifiersFor(Attributes.ARMOR);
        event.removeAllModifiersFor(Attributes.ARMOR_TOUGHNESS);
        if (costume) {
            event.removeAllModifiersFor(Attributes.ATTACK_DAMAGE);
            event.removeAllModifiersFor(Attributes.KNOCKBACK_RESISTANCE);
        }
        double armorValue = ironMan != null ? ironMan.armor(index) : stats[index];
        double toughness = ironMan != null ? ironMan.toughness().get() : stats[4];
        event.addModifier(Attributes.ARMOR, new AttributeModifier(id, armorValue, AttributeModifier.Operation.ADD_VALUE), slot);
        event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, toughness, AttributeModifier.Operation.ADD_VALUE), slot);
    }
}
