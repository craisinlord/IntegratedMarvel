package com.craisinlord.integrated_superheroes.boss;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.voidworld.VoidWorld;
import com.craisinlord.integrated_superheroes.voidworld.client.VoidSkyClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class WitherstormPressureClient {
    private static final double NEAR = 48.0D;
    private static final double FAR = 320.0D;
    private static final float MAX = 0.45F;

    private static float target;
    private static float current;

    private WitherstormPressureClient() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || !VoidWorld.isVoid(level)) {
            if (current != 0.0F) {
                current = 0.0F;
                target = 0.0F;
                VoidSkyClient.setStormPressure(0.0F);
            }
            return;
        }
        if (level.getGameTime() % 10L == 0L) {
            double best = Double.MAX_VALUE;
            for (Entity entity : level.entitiesForRendering()) {
                if (!(entity instanceof WitherstormEntity storm) || storm.isSmallForm() || !storm.isAlive()) continue;
                double dx = storm.getX() - player.getX();
                double dz = storm.getZ() - player.getZ();
                best = Math.min(best, Math.sqrt(dx * dx + dz * dz));
            }
            target = best == Double.MAX_VALUE ? 0.0F : (float) Mth.clamp((FAR - best) / (FAR - NEAR), 0.0D, 1.0D) * MAX;
        }
        float next = Mth.approach(current, target, 0.005F);
        if (next != current) {
            current = next;
            VoidSkyClient.setStormPressure(current);
        }
    }
}
