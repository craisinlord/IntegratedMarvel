package com.craisinlord.integrated_superheroes.compat.thematic;

import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

final class CuriosRings {
    private CuriosRings() {
    }

    static ItemStack find(LivingEntity entity, Predicate<ItemStack> test) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(inventory -> inventory.findFirstCurio(test))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }
}
