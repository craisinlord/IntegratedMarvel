package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.FlightClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class HeroFlightCamera {
    private static final float MAX_ROLL = 22.0F;
    private static final float TURN_ROLL = 1.4F;
    private static final float STRAFE_ROLL = 18.0F;
    private static final float RESPONSE = 0.18F;
    private static float roll;
    private static float prevRoll;
    private static float lastYaw;
    private static boolean tracking;

    private HeroFlightCamera() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        prevRoll = roll;
        if (player == null || minecraft.isPaused()) {
            tracking = false;
            roll = 0.0F;
            return;
        }
        float yaw = player.getYRot();
        float turn = tracking ? Mth.wrapDegrees(yaw - lastYaw) : 0.0F;
        lastYaw = yaw;
        tracking = true;
        float scale = FlightClientConfig.heroCameraRoll();
        float target = 0.0F;
        if (scale > 0.0F && player.getAbilities().flying && BinaryFlight.isFlying(player)) {
            Vec3 motion = player.getDeltaMovement();
            Vec3 local = motion.yRot(player.getYRot() * Mth.DEG_TO_RAD);
            float strafe = (float) Mth.clamp(local.x, -1.0D, 1.0D);
            target = Mth.clamp(turn * TURN_ROLL - strafe * STRAFE_ROLL, -MAX_ROLL, MAX_ROLL) * scale;
        }
        roll += (target - roll) * RESPONSE;
        if (Math.abs(roll) < 0.01F && target == 0.0F) {
            roll = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (roll == 0.0F && prevRoll == 0.0F) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getCamera().getEntity() != minecraft.player || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        event.setRoll(event.getRoll() + Mth.lerp((float) event.getPartialTick(), prevRoll, roll));
    }
}
