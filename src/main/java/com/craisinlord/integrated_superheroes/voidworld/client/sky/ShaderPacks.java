package com.craisinlord.integrated_superheroes.voidworld.client.sky;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class ShaderPacks {
    private static boolean checked;
    private static MethodHandle inUse;

    private ShaderPacks() {
    }

    public static RenderLevelStageEvent.Stage glowStage() {
        return inUse() ? RenderLevelStageEvent.Stage.AFTER_ENTITIES : RenderLevelStageEvent.Stage.AFTER_PARTICLES;
    }

    public static boolean inUse() {
        if (!checked) {
            checked = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object instance = api.getMethod("getInstance").invoke(null);
                inUse = MethodHandles.publicLookup()
                        .findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class))
                        .bindTo(instance);
            } catch (Throwable ignored) {
                inUse = null;
            }
        }
        if (inUse == null) {
            return false;
        }
        try {
            return (boolean) inUse.invokeExact();
        } catch (Throwable ignored) {
            inUse = null;
            return false;
        }
    }
}
