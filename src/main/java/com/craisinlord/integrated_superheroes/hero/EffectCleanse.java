package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.mixin.LivingEntityEffectInvoker;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Hero cleanses (Deadpool's healing factor, Hulk's Last Stand, the Ancestral Plane) without the two problems of
 * {@link LivingEntity#removeEffectsCuredBy} / {@link LivingEntity#removeAllEffects}:
 * <ul>
 *     <li>Those iterate the live effect map, so any mod whose removal hook adds or removes an effect throws a
 *     ConcurrentModificationException. Here the effects are snapshotted first and removed one by one.</li>
 *     <li>They strip environment effects such as the Underside's gravity flip, which the dimension reapplies the next
 *     tick, flipping the player's screen and gravity over and over. Effects in
 *     {@code integrated_superheroes:cleanse_immune} are never touched.</li>
 * </ul>
 */
public final class EffectCleanse {
    public static final TagKey<MobEffect> IMMUNE = TagKey.create(Registries.MOB_EFFECT, IntegratedSuperheroes.id("cleanse_immune"));

    private EffectCleanse() {
    }

    public static boolean immune(MobEffectInstance instance) {
        return instance.getEffect().is(IMMUNE);
    }

    /** Removes every effect the given cure cures, skipping cleanse-immune effects. */
    public static boolean cure(LivingEntity entity, EffectCure cure) {
        return remove(entity, cure);
    }

    /** Removes every effect except cleanse-immune ones. */
    public static boolean removeAll(LivingEntity entity) {
        return remove(entity, null);
    }

    private static boolean remove(LivingEntity entity, @Nullable EffectCure cure) {
        if (entity.level().isClientSide()) {
            return false;
        }
        boolean removed = false;
        for (MobEffectInstance instance : List.copyOf(entity.getActiveEffects())) {
            Holder<MobEffect> effect = instance.getEffect();
            if (entity.getEffect(effect) != instance || immune(instance)) {
                continue;
            }
            if (cure != null && !instance.getCures().contains(cure)) {
                continue;
            }
            if (EventHooks.onEffectRemoved(entity, instance, cure)) {
                continue;
            }
            MobEffectInstance gone = entity.removeEffectNoUpdate(effect);
            if (gone != null) {
                ((LivingEntityEffectInvoker) entity).integratedSuperheroes$onEffectRemoved(gone);
                removed = true;
            }
        }
        return removed;
    }
}
