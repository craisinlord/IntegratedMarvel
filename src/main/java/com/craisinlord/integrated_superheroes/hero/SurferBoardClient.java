package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class SurferBoardClient {
    private static final int TRAIL_COLOR = 0xDDE7F4;
    private static final float MAX_BANK = 32.0F;
    private static final Map<Integer, Pose> POSES = new HashMap<>();
    private static Vec3 velocity;

    private SurferBoardClient() {
    }

    private static final class Pose {
        float heading;
        float prevHeading;
        float bank;
        float prevBank;
        boolean started;
    }

    public record View(float heading, float bank) {
    }

    public static View view(LivingEntity living, float partialTick) {
        if (!(living instanceof Player)) {
            return null;
        }
        SurferBoardPayload state = SurferBoardPayload.client(living.getId());
        Pose pose = POSES.get(living.getId());
        if (state == null || pose == null || !pose.started) {
            return null;
        }
        return new View(Mth.rotLerp(partialTick, pose.prevHeading, pose.heading), Mth.lerp(partialTick, pose.prevBank, pose.bank));
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.isPaused()) {
            return;
        }
        SurferBoardPayload state = SurferBoardPayload.client(player.getId());
        if (state == null || player.isPassenger() || player.isSpectator() || player.isFallFlying() || !player.isAlive()) {
            velocity = null;
            return;
        }
        steer(player, state);
    }

    private static void steer(LocalPlayer player, SurferBoardPayload state) {
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        Vec3 current = player.getDeltaMovement();
        if (velocity == null) {
            velocity = new Vec3(current.x, player.onGround() ? 0.35D : Math.max(0.0D, current.y), current.z);
        }
        double vx = velocity.x;
        double vy = velocity.y;
        double vz = velocity.z;
        if (player.horizontalCollision) {
            vx = player.getX() - player.xo;
            vz = player.getZ() - player.zo;
        }
        if (player.verticalCollision && (vy < 0.0D && player.onGround() || vy > 0.0D && !player.onGround())) {
            vy = 0.0D;
        }
        float forward = Math.signum(player.input.forwardImpulse);
        float strafe = Math.signum(player.input.leftImpulse);
        boolean up = player.input.jumping;
        boolean down = player.input.shiftKeyDown;
        double maxPitch = SilverSurferConfig.MAX_PITCH_DEGREES.get();
        float pitch = (float) Mth.clamp(player.getXRot(), -maxPitch, maxPitch);
        float yaw = player.getYRot();
        Vec3 aim = Vec3.directionFromRotation(pitch, yaw);
        double acceleration = state.acceleration();
        double horizontal = Math.sqrt(vx * vx + vz * vz);
        if (horizontal > 1.0E-3D && forward >= 0.0F) {
            float heading = (float) (Mth.atan2(-vx, vz) * Mth.RAD_TO_DEG);
            float turned = heading + Mth.clamp(Mth.wrapDegrees(yaw - heading), -state.turn(), state.turn());
            vx = -Mth.sin(turned * Mth.DEG_TO_RAD) * horizontal;
            vz = Mth.cos(turned * Mth.DEG_TO_RAD) * horizontal;
        }
        if (forward > 0.0F) {
            vx += aim.x * acceleration * forward;
            vy += aim.y * acceleration * forward;
            vz += aim.z * acceleration * forward;
        } else if (forward < 0.0F) {
            vx *= 0.86D;
            vy *= 0.86D;
            vz *= 0.86D;
        }
        if (strafe != 0.0F) {
            double side = acceleration * 0.4D * strafe;
            vx += Mth.cos(yaw * Mth.DEG_TO_RAD) * side;
            vz += Mth.sin(yaw * Mth.DEG_TO_RAD) * side;
        }
        if (up) {
            vy += acceleration;
        }
        if (down) {
            vy -= acceleration;
        }
        boolean precision = state.precision();
        double coast = precision ? SilverSurferConfig.PRECISION_COAST_DRAG.get() : SilverSurferConfig.CRUISE_COAST_DRAG.get();
        double drag = forward > 0.0F || strafe != 0.0F ? 0.99D : coast;
        vx *= drag;
        vz *= drag;
        if (!up && !down) {
            vy *= forward > 0.0F ? 0.9D : 0.8D;
        }
        vy = Mth.clamp(vy, -SilverSurferConfig.MAX_DESCENT.get(), SilverSurferConfig.MAX_CLIMB.get());
        Vec3 next = new Vec3(vx, vy, vz);
        double max = state.maxSpeed();
        if (next.lengthSqr() > max * max) {
            next = next.normalize().scale(max);
        }
        if (next.lengthSqr() < 1.0E-6D) {
            next = Vec3.ZERO;
        }
        velocity = next;
        player.setDeltaMovement(next);
        player.fallDistance = 0.0F;
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            POSES.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        POSES.keySet().removeIf(id -> SurferBoardPayload.client(id) == null);
        for (SurferBoardPayload state : SurferBoardPayload.clientAll()) {
            Entity entity = level.getEntity(state.entityId());
            if (!(entity instanceof Player rider)) {
                POSES.remove(state.entityId());
                continue;
            }
            Pose pose = POSES.computeIfAbsent(state.entityId(), ignored -> new Pose());
            track(rider, pose);
            if (state.trail()) {
                trail(level, rider, pose);
            }
        }
    }

    private static void track(Player rider, Pose pose) {
        double dx = rider.getX() - rider.xo;
        double dz = rider.getZ() - rider.zo;
        double speed = Math.sqrt(dx * dx + dz * dz);
        if (!pose.started) {
            pose.started = true;
            pose.heading = pose.prevHeading = rider.getYRot();
            pose.bank = pose.prevBank = 0.0F;
        }
        pose.prevHeading = pose.heading;
        pose.prevBank = pose.bank;
        float target = speed > 0.06D && speed < 8.0D ? (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG) : rider.getYRot();
        float limit = speed > 0.06D ? 20.0F : 6.0F;
        float delta = Mth.clamp(Mth.wrapDegrees(target - pose.heading), -limit, limit);
        pose.heading = Mth.wrapDegrees(pose.heading + delta);
        float bankTarget = speed > 0.12D ? Mth.clamp(delta * 3.2F, -MAX_BANK, MAX_BANK) : 0.0F;
        pose.bank += (bankTarget - pose.bank) * 0.25F;
        if (pose.prevHeading - pose.heading > 180.0F) {
            pose.prevHeading -= 360.0F;
        } else if (pose.heading - pose.prevHeading > 180.0F) {
            pose.prevHeading += 360.0F;
        }
        rider.yBodyRotO = pose.prevHeading;
        rider.yBodyRot = pose.heading;
    }

    private static void trail(ClientLevel level, Player rider, Pose pose) {
        double dx = rider.getX() - rider.xo;
        double dy = rider.getY() - rider.yo;
        double dz = rider.getZ() - rider.zo;
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (speed < 0.2D || speed > 8.0D || rider.isInvisible()) {
            return;
        }
        float rad = pose.heading * Mth.DEG_TO_RAD;
        double backX = Mth.sin(rad) * 0.9D;
        double backZ = -Mth.cos(rad) * 0.9D;
        double x = rider.getX() + backX;
        double y = rider.getY() + 0.03D;
        double z = rider.getZ() + backZ;
        level.addParticle(HeroAbilities.emissive(TRAIL_COLOR, 0.9F), x, y, z, 0.0D, 0.0D, 0.0D);
        if (rider.tickCount % 3 == 0) {
            level.addParticle(ParticleTypes.END_ROD, x, y, z, -dx * 0.05D, 0.005D, -dz * 0.05D);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !minecraft.options.getCameraType().isFirstPerson()
                || minecraft.getCameraEntity() != player || player.isInvisible()) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        View view = view(player, partialTick);
        if (view == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        double x = Mth.lerp(partialTick, player.xo, player.getX());
        double y = Mth.lerp(partialTick, player.yo, player.getY());
        double z = Mth.lerp(partialTick, player.zo, player.getZ());
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(x - camera.x, y - camera.y, z - camera.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-view.heading()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(view.bank()));
        poseStack.translate(0.0F, SurferBoardRenderer.RIDE_HEIGHT, 0.0F);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        int light = LevelRenderer.getLightColor(minecraft.level, BlockPos.containing(x, y + 0.2D, z));
        SurferBoardRenderer.draw(poseStack, buffers, light, OverlayTexture.NO_OVERLAY);
        buffers.endBatch();
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SurferBoardPayload.clearClient();
        POSES.clear();
        velocity = null;
    }
}
