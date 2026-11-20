package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.morph.api.MorphApi;
import com.craisinlord.morph.api.event.MorphEvent;
import com.craisinlord.morph.api.morph.MorphInfo;
import com.craisinlord.morph.api.morph.MorphVariant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class MorphShapeshift {
    private MorphShapeshift() {
    }

    public static boolean isMorphed(ServerPlayer player) {
        try {
            MorphInfo info = MorphApi.getApiImpl().getMorphInfo(player);
            return info != null && info.isMorphed();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static ResourceLocation formId(ServerPlayer player) {
        try {
            MorphInfo info = MorphApi.getApiImpl().getMorphInfo(player);
            if (info == null || !info.isMorphed() || info.nextState == null || info.nextState.variant == null) {
                return null;
            }
            return info.nextState.variant.id;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    public static boolean morph(ServerPlayer player, LivingEntity target) {
        try {
            MorphVariant variant = MorphApi.getApiImpl().createVariant(target);
            return variant != null && !MorphEvent.CanAcquire.post(player, variant).isCanceled()
                    && MorphApi.getApiImpl().morphTo(player, variant);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static boolean demorph(ServerPlayer player) {
        try {
            return MorphApi.getApiImpl().demorph(player);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }
}
