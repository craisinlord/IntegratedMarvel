package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.registries.armors.armor.CharacterItem;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class SuitPieceItem extends ArmorItem {
    public SuitPieceItem(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Item suit = SuitPieces.suit(stack);
        if (suit == null) {
            return super.getName(stack);
        }
        return Component.translatable(getDescriptionId() + ".named", suit.getDescription());
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        if (SuitPieces.suit(stack) instanceof CharacterItem suit && SuitPieces.costume(suit)) {
            return ItemAttributeModifiers.EMPTY;
        }
        return super.getDefaultAttributeModifiers(stack);
    }
}
