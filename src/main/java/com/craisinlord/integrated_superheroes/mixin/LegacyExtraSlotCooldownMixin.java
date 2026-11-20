package com.craisinlord.integrated_superheroes.mixin;

import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.event.WorldTickEvent;
import com.stxllarplanes.marvellegacy.init.AttachmentsRegistry;
import com.stxllarplanes.marvellegacy.power.Power;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// Legacy only counts down slots 0-4; LegacyTenSlotsMixin widens the bar to ten, so
// anything pushed past slot 5 (suit abilities next to a serum) froze on cooldown.
@Mixin(value = WorldTickEvent.class, remap = false)
public abstract class LegacyExtraSlotCooldownMixin {
    private static final int LEGACY_TICKED_SLOTS = 5;

    @Inject(method = "cooldownTick", at = @At("TAIL"))
    private static void integratedSuperheroes$tickExtraSlots(PlayerTickEvent.Pre event, CallbackInfo ci) {
        PlayerData data = event.getEntity().getData(AttachmentsRegistry.PLAYER_DATA.get());
        List<Power> active = data.getActivePowers();
        for (int i = LEGACY_TICKED_SLOTS; i < active.size(); i++) {
            Power power = active.get(i);
            if (power != null && power.getCooldown() > 0.0D) {
                power.setCooldown(power.getCooldown() - 0.05D);
            }
        }
    }
}
