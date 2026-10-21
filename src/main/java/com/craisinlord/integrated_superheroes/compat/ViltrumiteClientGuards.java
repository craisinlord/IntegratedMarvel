package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.mixin.ViltrumiteFlightAnimAccessor;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.client.render.FlightAnimManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ViltrumiteClientGuards {
    private ViltrumiteClientGuards() {
    }

    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        Map<UUID, FlightAnimManager.AnimState> states = ViltrumiteFlightAnimAccessor.integratedSuperheroes$states();
        if (level == null) {
            ViltrumiteGuards.setClientGrabbed(Set.of());
            if (!states.isEmpty()) {
                states.clear();
            }
            return;
        }
        Set<Entity> grabbed = null;
        for (AbstractClientPlayer player : level.players()) {
            LivingEntity target = player instanceof ViltrumiteCorePlayer core ? core.getGrabbedTarget() : null;
            if (target != null) {
                if (grabbed == null) {
                    grabbed = Collections.newSetFromMap(new IdentityHashMap<>());
                }
                grabbed.add(target);
            }
        }
        ViltrumiteGuards.setClientGrabbed(grabbed == null ? Set.of() : grabbed);
        if (level.getGameTime() % 200L == 0L && !states.isEmpty() && ViltrumiteGuards.performance()) {
            Set<UUID> present = new HashSet<>();
            for (AbstractClientPlayer player : level.players()) {
                present.add(player.getUUID());
            }
            states.keySet().retainAll(present);
        }
    }
}
