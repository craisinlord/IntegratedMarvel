package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.content.ability.state.visuals.MarkSigilVisual;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicIdle;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.content.ability.client.visuals.MarkSigilRenderer", remap = false)
public abstract class ThematicMarkSigilGuardMixin {
    @Shadow(remap = false)
    @Final
    private static Map<String, MarkSigilVisual.Settings> INSTALLED;

    @Shadow(remap = false)
    private static int shownCount;

    @Shadow(remap = false)
    private static MarkSigilVisual.Settings hud;

    @Inject(method = "tick(Lnet/minecraft/client/Minecraft;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$skipUnmarkedWorld(Minecraft client, CallbackInfo ci) {
        if (client.level != null) {
            long now = client.level.getGameTime();
            for (MarkSigilVisual.Settings settings : INSTALLED.values()) {
                if (ThematicIdle.markLive(settings.key(), now)) {
                    return;
                }
            }
        }
        shownCount = 0;
        hud = null;
        ci.cancel();
    }
}
