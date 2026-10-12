package com.craisinlord.integrated_superheroes.villain;

import com.craisinlord.integrated_superheroes.hero.ColossusPower;
import com.craisinlord.integrated_superheroes.hero.DaredevilPower;
import com.craisinlord.integrated_superheroes.hero.HeroPower;
import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import com.craisinlord.integrated_superheroes.hero.HulkPower;
import com.craisinlord.integrated_superheroes.hero.IronManEnergy;
import com.craisinlord.integrated_superheroes.hero.KittyPower;
import com.craisinlord.integrated_superheroes.hero.MagnetoPower;
import com.craisinlord.integrated_superheroes.hero.SuperSoldierPower;
import com.craisinlord.integrated_superheroes.registry.GauntletContent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class Loadout {
    private Loadout() {
    }

    public enum Kind {
        SERUM, SUIT, HONEST
    }

    public static Kind kind(LivingEntity entity) {
        if (entity instanceof Player player) {
            if (suit(player)) {
                return Kind.SUIT;
            }
            if (serum(player)) {
                return Kind.SERUM;
            }
        }
        return Kind.HONEST;
    }

    public static boolean serum(Player player) {
        try {
            String power = HeroPowers.data(player).getPrimaryPower();
            return power != null && !power.isEmpty();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static boolean hero(LivingEntity entity, String id) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        try {
            HeroPower hero = HeroPowers.activeHero(player);
            return hero != null && id.equals(hero.id());
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static boolean superSoldier(LivingEntity entity) {
        return hero(entity, SuperSoldierPower.ID);
    }

    public static boolean magneto(LivingEntity entity) {
        return hero(entity, MagnetoPower.ID);
    }

    public static boolean suit(LivingEntity entity) {
        return IronManEnergy.hasEnergy(entity.getItemBySlot(EquipmentSlot.CHEST));
    }

    public static boolean heavy(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        try {
            return HulkPower.isHulk(player) || ColossusPower.isSteel(player);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static boolean phasing(LivingEntity entity) {
        try {
            return entity instanceof Player player && KittyPower.isPhasing(player);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static boolean radar(LivingEntity entity) {
        try {
            return entity instanceof Player player && DaredevilPower.isRadarOn(player);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static boolean spaceStone(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        Item stone;
        try {
            stone = GauntletContent.SPACE_STONE.get();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(stone)) {
                return true;
            }
        }
        return player.getOffhandItem().is(stone);
    }

    public static void drainSuit(LivingEntity entity, double fraction) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }
            ItemStack stack = entity.getItemBySlot(slot);
            if (IronManEnergy.hasEnergy(stack)) {
                IronManEnergy.setFE(stack, IronManEnergy.getFE(stack) * (1.0D - fraction));
            }
        }
    }
}
