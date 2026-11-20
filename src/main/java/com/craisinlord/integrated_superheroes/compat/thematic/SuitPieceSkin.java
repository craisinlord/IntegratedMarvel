package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.client.SuitModelProxies;
import bond.thematic.api.registries.armors.armor.CharacterItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

public final class SuitPieceSkin {
    private static boolean warned;

    private SuitPieceSkin() {
    }

    public static void cover(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model) {
        try {
            if (SuitModelProxies.renderArmor(player) != null) {
                return;
            }
            Item chest = player.getItemBySlot(EquipmentSlot.CHEST).getItem();
            if (chest instanceof CharacterItem) {
                model.body.visible = false;
                model.jacket.visible = false;
                model.rightArm.visible = false;
                model.leftArm.visible = false;
                model.rightSleeve.visible = false;
                model.leftSleeve.visible = false;
            }
            if (SuitPieces.suit(player.getItemBySlot(EquipmentSlot.HEAD)) != null || chest instanceof CharacterItem && SuitPieces.helmetless(chest)) {
                model.hat.visible = false;
                model.head.visible = false;
            }
            boolean legs = SuitPieces.suit(player.getItemBySlot(EquipmentSlot.LEGS)) != null;
            boolean feet = SuitPieces.suit(player.getItemBySlot(EquipmentSlot.FEET)) != null;
            if (legs && feet) {
                model.rightLeg.visible = false;
                model.leftLeg.visible = false;
            }
            if (legs || feet) {
                model.rightPants.visible = false;
                model.leftPants.visible = false;
            }
        } catch (Throwable error) {
            if (!warned) {
                warned = true;
                com.craisinlord.integrated_superheroes.IntegratedSuperheroes.LOGGER.warn("Couldn't hide the skin under suit pieces", error);
            }
        }
    }
}
