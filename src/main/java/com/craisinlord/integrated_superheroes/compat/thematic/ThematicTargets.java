package com.craisinlord.integrated_superheroes.compat.thematic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags;

final class ThematicTargets {
    static final float BOSS_HEALTH = 250.0F;

    private ThematicTargets() {
    }

    static boolean boss(LivingEntity entity) {
        return entity.getType().is(Tags.EntityTypes.BOSSES) || entity.getMaxHealth() >= BOSS_HEALTH;
    }

    static boolean spared(LivingEntity owner, LivingEntity victim) {
        if (victim == owner || !victim.isAlive() || victim.isSpectator() || owner.isAlliedTo(victim)) {
            return true;
        }
        if (victim instanceof OwnableEntity ownable && owner.getUUID().equals(ownable.getOwnerUUID())) {
            return true;
        }
        return owner instanceof Player attacker && victim instanceof Player player && (player.isCreative() || !attacker.canHarmPlayer(player));
    }
}
