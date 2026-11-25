package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.hero.HeroCaster;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VoidVariants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;

public final class SuitMobSupport {
    private static final String NO_GRIEF = "integrated_superheroes_no_grief";
    private static boolean registered;

    private SuitMobSupport() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(SuitMobSupport::onGriefing);
    }

    static void noGrief(LivingEntity caster, Entity spawned) {
        if (spawned != null && !HeroCaster.griefs(caster)) {
            spawned.getPersistentData().putBoolean(NO_GRIEF, true);
        }
    }

    private static void onGriefing(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();
        if (entity != null && (entity.getPersistentData().getBoolean(NO_GRIEF) || VoidVariants.isVariant(entity))) {
            event.setCanGrief(false);
        }
    }
}
