package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.vfx.VfxAPI;
import bond.thematic.api.vfx.VfxRegistry;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class ThematicVisuals {
    private static boolean failed;

    private ThematicVisuals() {
    }

    public static boolean enabled() {
        return !failed && ThematicOptions.on(ThematicOptions.VISUALS);
    }

    public static boolean vfx(ServerLevel level, Vec3 at, String id, int argb) {
        return vfx(level, at, id, argb, 0.0F);
    }

    public static boolean vfx(ServerLevel level, Vec3 at, String id, int argb, float radius) {
        if (!enabled()) {
            return false;
        }
        try {
            if (VfxRegistry.get(id) == null) {
                return false;
            }
            if (argb == 0 && radius <= 0.0F) {
                VfxAPI.triggerVFX(level, at, id);
            } else {
                VfxAPI.triggerVFX(level, at, id, builder -> {
                    if (argb != 0) {
                        builder.color(argb);
                    }
                    if (radius > 0.0F) {
                        builder.radius(radius);
                    }
                });
            }
            return true;
        } catch (Throwable error) {
            return fail(error);
        }
    }

    public static boolean beam(ServerLevel level, Vec3 from, Vec3 to, String id, int argb) {
        if (!enabled()) {
            return false;
        }
        try {
            if (VfxRegistry.get(id) == null) {
                return false;
            }
            VfxAPI.triggerVFX(level, from, id, builder -> {
                builder.towards(from, to);
                if (argb != 0) {
                    builder.color(argb);
                }
            });
            return true;
        } catch (Throwable error) {
            return fail(error);
        }
    }

    public static boolean follow(ServerLevel level, Entity entity, String id, String session) {
        if (!enabled()) {
            return false;
        }
        try {
            VfxAPI.playVFX(level, entity.position(), id, builder -> {
            }, session, Vec3.ZERO, entity);
            return true;
        } catch (Throwable error) {
            return fail(error);
        }
    }

    public static void stop(ServerLevel level, Vec3 at, String session) {
        if (failed) {
            return;
        }
        try {
            VfxAPI.stopVFX(level, at, session);
        } catch (Throwable error) {
            fail(error);
        }
    }

    private static boolean fail(Throwable error) {
        failed = true;
        IntegratedSuperheroes.LOGGER.warn("Thematic effects are off for this session; this Thematic version may have changed them", error);
        return false;
    }
}
