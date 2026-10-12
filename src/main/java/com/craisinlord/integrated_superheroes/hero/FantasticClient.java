package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class FantasticClient {
    private static final float ARM_LENGTH = 0.586F;
    private static final float MAX_SCALE = 30.0F;
    private static final double NECK_HEIGHT = 5.0D;
    private static final float[] PIVOTS = new float[18];
    private static final double BLEND_TICKS = 8.0D;
    private static final Map<UUID, Blend> BLENDS = new HashMap<>();
    private static boolean saved;

    private FantasticClient() {
    }

    public static boolean hidden(net.minecraft.world.entity.Entity entity) {
        return entity instanceof Player player && player != Minecraft.getInstance().player && InvisibleWomanPower.isInvisible(player);
    }

    private static ModelPart[] pivotParts(PlayerModel<?> model) {
        return new ModelPart[]{model.rightArm, model.leftArm, model.rightSleeve, model.leftSleeve, model.head, model.hat};
    }

    private static void save(PlayerModel<?> model) {
        if (saved) {
            return;
        }
        ModelPart[] parts = pivotParts(model);
        for (int i = 0; i < parts.length; i++) {
            PIVOTS[i * 3] = parts[i].x;
            PIVOTS[i * 3 + 1] = parts[i].y;
            PIVOTS[i * 3 + 2] = parts[i].z;
        }
        saved = true;
    }

    private static void stretchArm(ModelPart arm, ModelPart sleeve, float scale, float width, float depth) {
        arm.yScale = scale;
        arm.xScale = width;
        arm.zScale = depth;
        Vector3f shift = new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot).transform(new Vector3f(0.0F, 2.0F * (scale - 1.0F), 0.0F));
        arm.x += shift.x;
        arm.y += shift.y;
        arm.z += shift.z;
        sleeve.copyFrom(arm);
    }

    private static void setLeg(ModelPart leg, ModelPart pants, float xRot, float zRot, float yScale, float width, float depth) {
        leg.xRot = xRot;
        leg.yRot = 0.0F;
        leg.zRot = zRot;
        leg.yScale = yScale;
        leg.xScale = width;
        leg.zScale = depth;
        pants.copyFrom(leg);
    }

    private static void setArm(ModelPart arm, float xRot, float zRot) {
        arm.xRot = xRot;
        arm.yRot = 0.0F;
        arm.zRot = zRot;
    }

    private static float scaleFor(float length) {
        return Mth.clamp((length * 16.0F / 0.9375F + 2.0F) / 12.0F, 1.0F, MAX_SCALE);
    }

    private static long now(Player player) {
        return player.level().getGameTime();
    }

    private static float progress(MrFantasticPower.Stretch stretch, long now, float partial) {
        return Mth.clamp((now - stretch.start() + partial) / Math.max(1, stretch.ticks()), 0.0F, 1.0F);
    }

    private static Vec3 shoulder(Player player, float partial, boolean right) {
        float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 side = new Vec3(-Mth.cos(yaw), 0.0D, -Mth.sin(yaw)).scale(right ? 0.33D : -0.33D);
        return player.getPosition(partial).add(0.0D, player.getBbHeight() * 0.76D, 0.0D).add(side);
    }

    private static float factor(MrFantasticPower.Stretch stretch, long now, float partial) {
        float t = progress(stretch, now, partial);
        return switch (stretch.mode()) {
            case MrFantasticPower.PUNCH, MrFantasticPower.SWEEP -> Mth.sin(t * Mth.PI);
            case MrFantasticPower.SLING -> t < 0.7F ? t / 0.7F : Math.max(0.0F, 1.0F - (t - 0.7F) / 0.15F);
            default -> 1.0F;
        };
    }

    private static float width(MrFantasticPower.Stretch stretch, float factor) {
        return 1.0F + 0.8F * factor;
    }

    private static Vec3 hammerReach(Player player, float partial, Vec3 from, Vec3 point, float t) {
        Vec3 look = player.getViewVector(partial);
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
        Vec3 overhead = new Vec3(0.0D, 1.0D, 0.0D).subtract(flat.scale(0.35D)).normalize();
        Vec3 delta = point.subtract(from);
        double full = Math.max(ARM_LENGTH, delta.length());
        Vec3 aim = delta.lengthSqr() < 1.0E-4D ? flat : delta.normalize();
        float raise = 0.3F;
        float strike = (float) MrFantasticPower.HAMMER_IMPACT / MrFantasticPower.HAMMER_TICKS;
        Vec3 direction = aim;
        double length = full;
        if (t < raise) {
            direction = overhead;
            length = ARM_LENGTH + (2.5D - ARM_LENGTH) * (t / raise);
        } else if (t < strike) {
            float swing = (t - raise) / (strike - raise);
            Vec3 blended = overhead.lerp(aim, swing);
            direction = blended.lengthSqr() < 1.0E-4D ? aim : blended.normalize();
            length = Mth.lerp(swing, 2.5D, full);
        } else if (t >= 0.7F) {
            length = Mth.lerp((t - 0.7F) / 0.3F, full, ARM_LENGTH);
        }
        return from.add(direction.scale(length));
    }

    private static float hammerWidth(float t) {
        float swell = t < 0.3F ? t / 0.3F : t < 0.7F ? 1.0F : 1.0F - (t - 0.7F) / 0.3F;
        return 1.0F + 1.8F * swell;
    }

    private static Vec3 slingAnchor(Player player, float partial, boolean right, float factor) {
        Vec3 look = player.getViewVector(partial);
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0.0D, flat.x).scale(right ? 1.3D : -1.3D);
        return player.getEyePosition(partial).add(look.scale(0.6D + 3.4D * factor)).add(side.scale(0.4D + 0.6D * factor));
    }

    private static void aimAt(ModelPart arm, ModelPart sleeve, Player player, float partial, Vec3 from, Vec3 to, float length, float width) {
        Vec3 delta = to.subtract(from);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yawTo = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitchTo = (float) (-(Mth.atan2(delta.y, horizontal) * Mth.RAD_TO_DEG));
        float body = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        arm.yRot = Mth.wrapDegrees(yawTo - body) * Mth.DEG_TO_RAD;
        arm.xRot = -Mth.HALF_PI + pitchTo * Mth.DEG_TO_RAD;
        arm.zRot = 0.0F;
        stretchArm(arm, sleeve, scaleFor(length), width, width);
    }

    private static final class Blend {
        private String form;
        private double base;
        private double start;
        private boolean rising;

        private Blend(String form, double start) {
            this.form = form;
            this.start = start;
            this.rising = true;
        }

        private double value(double time) {
            return Mth.clamp(this.base + (this.rising ? 1.0D : -1.0D) * (time - this.start) / BLEND_TICKS, 0.0D, 1.0D);
        }

        private void turn(double time, boolean rising) {
            this.base = this.value(time);
            this.start = time;
            this.rising = rising;
        }
    }

    private static String activeForm(Player player) {
        if (!MrFantasticPower.isReed(player)) {
            return null;
        }
        FantasticSeat seat = ReedShapes.seat(player);
        if (seat != null) {
            return seat.isSofa() ? ReedShapes.SOFA : ReedShapes.BOAT;
        }
        if (ReedShapes.has(player, ReedShapes.TRAMPOLINE_STATE)) {
            return ReedShapes.TRAMPOLINE;
        }
        if (ReedShapes.has(player, ReedShapes.BALL_STATE)) {
            return ReedShapes.BALL;
        }
        if (ReedShapes.has(player, ReedShapes.NECK_STATE)) {
            return ReedShapes.NECK;
        }
        if (ReedShapes.has(player, ReedShapes.POGO_STATE)) {
            return ReedShapes.POGO;
        }
        if (ReedShapes.has(player, ReedShapes.UMBRELLA_STATE)) {
            return ReedShapes.UMBRELLA;
        }
        return MrFantasticPower.gliding(player) ? ReedShapes.PARACHUTE : null;
    }

    private static Blend blend(Player player, double time) {
        String active = activeForm(player);
        Blend blend = BLENDS.get(player.getUUID());
        if (blend == null) {
            if (active == null) {
                return null;
            }
            blend = new Blend(active, time);
            BLENDS.put(player.getUUID(), blend);
            return blend;
        }
        double value = blend.value(time);
        if (active != null && active.equals(blend.form)) {
            if (!blend.rising) {
                blend.turn(time, true);
            }
        } else if (active != null) {
            if (value <= 0.0D) {
                blend.form = active;
                blend.base = 0.0D;
                blend.start = time;
                blend.rising = true;
            } else if (blend.rising) {
                blend.turn(time, false);
            }
        } else if (blend.rising) {
            blend.turn(time, false);
        } else if (value <= 0.0D) {
            BLENDS.remove(player.getUUID());
            return null;
        }
        return blend;
    }

    private static float amount(Player player, String form, float partial) {
        double time = now(player) + partial;
        Blend blend = blend(player, time);
        return blend != null && form.equals(blend.form) ? (float) blend.value(time) : 0.0F;
    }

    private static float ease(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static void blendArm(ModelPart arm, ModelPart sleeve, float xRot, float zRot, float scale, float width, float depth, float b) {
        arm.xRot = Mth.lerp(b, arm.xRot, xRot);
        arm.yRot = Mth.lerp(b, arm.yRot, 0.0F);
        arm.zRot = Mth.lerp(b, arm.zRot, zRot);
        stretchArm(arm, sleeve, Mth.lerp(b, 1.0F, scale), Mth.lerp(b, 1.0F, width), Mth.lerp(b, 1.0F, depth));
    }

    private static void blendLeg(ModelPart leg, ModelPart pants, float xRot, float zRot, float scale, float width, float depth, float b) {
        setLeg(leg, pants, Mth.lerp(b, leg.xRot, xRot), Mth.lerp(b, leg.zRot, zRot), Mth.lerp(b, 1.0F, scale),
                Mth.lerp(b, 1.0F, width), Mth.lerp(b, 1.0F, depth));
    }

    private static void lie(PoseStack pose, float wide, float longer, float thick, float drop, float b) {
        pose.translate(0.0F, 0.75F + drop * b, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F * b));
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F * b));
        pose.scale(Mth.lerp(b, 1.0F, wide), Mth.lerp(b, 1.0F, longer), Mth.lerp(b, 1.0F, thick));
        pose.translate(0.0F, -0.75F, 0.0F);
    }

    public static double neckHeight(Player player, float partial) {
        return ease(amount(player, ReedShapes.NECK, partial)) * NECK_HEIGHT;
    }

    public static void pose(Player player, PlayerModel<?> model, float partial, PoseStack pose) {
        long now = now(player);
        MrFantasticPower.Stretch stretch = MrFantasticPower.stretch(player, now);
        double time = now + partial;
        Blend blend = blend(player, time);
        String form = blend == null ? null : blend.form;
        float b = blend == null ? 0.0F : ease((float) blend.value(time));
        if (stretch == null && b <= 0.0F) {
            return;
        }
        save(model);
        float clock = player.tickCount + partial;
        if (b > 0.0F && ReedShapes.BOAT.equals(form)) {
            lie(pose, 2.0F, 1.5F, 1.0F, 0.55F, b);
            Entity vehicle = player.getVehicle();
            float paddle = vehicle != null && vehicle.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3D ? Mth.sin(clock * 0.45F) * 0.35F : 0.0F;
            blendArm(model.rightArm, model.rightSleeve, paddle, 0.12F, 1.8F, 1.2F, 3.0F, b);
            blendArm(model.leftArm, model.leftSleeve, -paddle, -0.12F, 1.8F, 1.2F, 3.0F, b);
            blendLeg(model.rightLeg, model.rightPants, 0.0F, 0.1F, 1.3F, 1.2F, 2.6F, b);
            blendLeg(model.leftLeg, model.leftPants, 0.0F, -0.1F, 1.3F, 1.2F, 2.6F, b);
            return;
        }
        if (b > 0.0F && ReedShapes.SOFA.equals(form)) {
            pose.translate(0.0F, 0.75F, 0.0F);
            pose.scale(Mth.lerp(b, 1.0F, 2.2F), 1.0F, Mth.lerp(b, 1.0F, 1.4F));
            pose.translate(0.0F, -0.75F, 0.0F);
            blendArm(model.rightArm, model.rightSleeve, -Mth.HALF_PI + 0.15F, 0.0F, 1.2F, 1.4F, 2.4F, b);
            blendArm(model.leftArm, model.leftSleeve, -Mth.HALF_PI + 0.15F, 0.0F, 1.2F, 1.4F, 2.4F, b);
            blendLeg(model.rightLeg, model.rightPants, -1.4F, 0.0F, 1.1F, 1.8F, 1.8F, b);
            blendLeg(model.leftLeg, model.leftPants, -1.4F, 0.0F, 1.1F, 1.8F, 1.8F, b);
            return;
        }
        if (b > 0.0F && ReedShapes.TRAMPOLINE.equals(form)) {
            lie(pose, 2.6F, 1.3F, 0.35F, 0.62F, b);
            blendArm(model.rightArm, model.rightSleeve, 0.0F, 1.3F, 1.4F, 1.2F, 1.2F, b);
            blendArm(model.leftArm, model.leftSleeve, 0.0F, -1.3F, 1.4F, 1.2F, 1.2F, b);
            blendLeg(model.rightLeg, model.rightPants, 0.0F, 0.6F, 1.2F, 1.2F, 1.2F, b);
            blendLeg(model.leftLeg, model.leftPants, 0.0F, -0.6F, 1.2F, 1.2F, 1.2F, b);
            return;
        }
        if (b > 0.0F && ReedShapes.BALL.equals(form)) {
            pose.translate(0.0F, 0.75F + 0.3F * b, 0.0F);
            pose.mulPose(Axis.XP.rotation(clock * 0.7F * b));
            pose.scale(Mth.lerp(b, 1.0F, 1.25F), Mth.lerp(b, 1.0F, 0.95F), Mth.lerp(b, 1.0F, 1.25F));
            pose.translate(0.0F, -0.75F, 0.0F);
            model.head.xRot = Mth.lerp(b, model.head.xRot, 0.8F);
            model.hat.copyFrom(model.head);
            blendArm(model.rightArm, model.rightSleeve, -2.3F, 0.3F, 1.0F, 1.3F, 1.3F, b);
            blendArm(model.leftArm, model.leftSleeve, -2.3F, -0.3F, 1.0F, 1.3F, 1.3F, b);
            blendLeg(model.rightLeg, model.rightPants, -1.7F, 0.0F, 0.8F, 1.3F, 1.3F, b);
            blendLeg(model.leftLeg, model.leftPants, -1.7F, 0.0F, 0.8F, 1.3F, 1.3F, b);
            return;
        }
        if (b > 0.0F && ReedShapes.POGO.equals(form)) {
            float spring = 1.7F + Mth.clamp((float) (player.getY() - player.yo) * 1.5F, -0.5F, 0.5F);
            pose.translate(0.0F, -(Mth.lerp(b, 1.0F, spring) - 1.0F) * 0.75F, 0.0F);
            blendArm(model.rightArm, model.rightSleeve, -0.6F, -0.15F, 1.1F, 1.0F, 1.0F, b);
            blendArm(model.leftArm, model.leftSleeve, -0.6F, 0.15F, 1.1F, 1.0F, 1.0F, b);
            blendLeg(model.rightLeg, model.rightPants, 0.0F, -0.06F, spring, 0.8F, 0.8F, b);
            blendLeg(model.leftLeg, model.leftPants, 0.0F, 0.06F, spring, 0.8F, 0.8F, b);
            return;
        }
        if (b > 0.0F && ReedShapes.UMBRELLA.equals(form)) {
            blendArm(model.rightArm, model.rightSleeve, -Mth.PI, 0.12F, 1.6F, 1.0F, 1.0F, b);
            blendArm(model.leftArm, model.leftSleeve, -Mth.PI, -0.12F, 1.6F, 1.0F, 1.0F, b);
            return;
        }
        if (b > 0.0F && ReedShapes.NECK.equals(form)) {
            model.head.y -= (float) (b * NECK_HEIGHT * 16.0D / 0.9375D);
            model.hat.copyFrom(model.head);
        }
        if (stretch != null) {
            float factor = factor(stretch, now, partial);
            float width = width(stretch, factor);
            if (stretch.mode() == MrFantasticPower.SWEEP) {
                float scale = scaleFor((float) stretch.point().x * factor);
                float spin = progress(stretch, now, partial) * Mth.TWO_PI * 2.0F;
                setArm(model.rightArm, spin, Mth.HALF_PI);
                setArm(model.leftArm, -spin, -Mth.HALF_PI);
                stretchArm(model.rightArm, model.rightSleeve, scale, width, width);
                stretchArm(model.leftArm, model.leftSleeve, scale, width, width);
            } else if (stretch.mode() == MrFantasticPower.SLING) {
                for (boolean right : new boolean[]{true, false}) {
                    Vec3 from = shoulder(player, partial, right);
                    Vec3 to = slingAnchor(player, partial, right, factor);
                    aimAt(right ? model.rightArm : model.leftArm, right ? model.rightSleeve : model.leftSleeve, player, partial, from, to,
                            (float) from.distanceTo(to), 1.0F + 0.6F * factor);
                }
            } else if (stretch.mode() == MrFantasticPower.HAMMER) {
                float t = progress(stretch, now, partial);
                Vec3 from = shoulder(player, partial, true);
                Vec3 to = hammerReach(player, partial, from, stretch.point(), t);
                aimAt(model.rightArm, model.rightSleeve, player, partial, from, to, (float) from.distanceTo(to), hammerWidth(t));
            } else {
                Vec3 from = shoulder(player, partial, true);
                float full = (float) from.distanceTo(stretch.point());
                float length = ARM_LENGTH + (full - ARM_LENGTH) * factor;
                aimAt(model.rightArm, model.rightSleeve, player, partial, from, stretch.point(), length, width);
            }
            return;
        }
        if (b <= 0.0F || !ReedShapes.PARACHUTE.equals(form)) {
            return;
        }
        blendArm(model.rightArm, model.rightSleeve, 0.0F, 1.45F, 2.4F, 1.2F, 3.0F, b);
        blendArm(model.leftArm, model.leftSleeve, 0.0F, -1.45F, 2.4F, 1.2F, 3.0F, b);
        model.body.xScale = Mth.lerp(b, 1.0F, 1.5F);
        model.body.zScale = Mth.lerp(b, 1.0F, 0.7F);
        model.jacket.copyFrom(model.body);
        blendLeg(model.rightLeg, model.rightPants, 0.0F, 0.45F, 1.5F, 1.0F, 2.0F, b);
        blendLeg(model.leftLeg, model.leftPants, 0.0F, -0.45F, 1.5F, 1.0F, 2.0F, b);
    }

    public static void renderNeck(Player player, PlayerModel<?> model, PoseStack pose, VertexConsumer consumer, int light, int overlay, int color) {
        if (!MrFantasticPower.isReed(player)) {
            return;
        }
        double neck = neckHeight(player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (neck <= 0.0D) {
            return;
        }
        float px = (float) (neck * 16.0D / 0.9375D);
        float scale = px / 12.0F;
        ModelPart arm = model.rightArm;
        float[] state = store(arm);
        arm.x = 1.0F;
        arm.y = -10.0F * scale;
        arm.z = 0.0F;
        arm.xRot = 0.0F;
        arm.yRot = 0.0F;
        arm.zRot = 0.0F;
        arm.xScale = 0.8F;
        arm.yScale = scale;
        arm.zScale = 0.8F;
        arm.visible = true;
        arm.render(pose, consumer, light, overlay, color);
        restore(arm, state);
    }

    public static void renderUmbrella(Player player, PlayerModel<?> model, PoseStack pose, VertexConsumer consumer, int light, int overlay, int color) {
        if (!MrFantasticPower.isReed(player)) {
            return;
        }
        float b = ease(amount(player, ReedShapes.UMBRELLA, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)));
        if (b <= 0.0F) {
            return;
        }
        ModelPart arm = model.rightArm;
        float[] state = store(arm);
        float bottom = Mth.lerp(b, -8.0F, -15.0F);
        canopy(arm, pose, consumer, light, overlay, color, 10.0F * b, 0.15F, bottom);
        canopy(arm, pose, consumer, light, overlay, color, 6.0F * b, 0.15F, bottom - 1.8F);
        canopy(arm, pose, consumer, light, overlay, color, 2.5F * b, 0.2F, bottom - 3.6F);
        restore(arm, state);
    }

    private static void canopy(ModelPart arm, PoseStack pose, VertexConsumer consumer, int light, int overlay, int color, float wide, float thick, float bottom) {
        arm.x = wide;
        arm.y = bottom - 10.0F * thick;
        arm.z = 0.0F;
        arm.xRot = 0.0F;
        arm.yRot = 0.0F;
        arm.zRot = 0.0F;
        arm.xScale = wide;
        arm.yScale = thick;
        arm.zScale = wide;
        arm.visible = true;
        arm.render(pose, consumer, light, overlay, color);
    }

    public static void reset(PlayerModel<?> model) {
        if (saved) {
            ModelPart[] parts = pivotParts(model);
            for (int i = 0; i < parts.length; i++) {
                parts[i].x = PIVOTS[i * 3];
                parts[i].y = PIVOTS[i * 3 + 1];
                parts[i].z = PIVOTS[i * 3 + 2];
            }
            saved = false;
        }
        for (ModelPart part : new ModelPart[]{model.rightArm, model.leftArm, model.rightSleeve, model.leftSleeve,
                model.rightLeg, model.leftLeg, model.rightPants, model.leftPants, model.body, model.jacket}) {
            part.xScale = 1.0F;
            part.yScale = 1.0F;
            part.zScale = 1.0F;
        }
    }

    public static double cameraOffset(Player player, float partial) {
        if (!MrFantasticPower.isReed(player)) {
            return 0.0D;
        }
        double time = now(player) + partial;
        Blend blend = blend(player, time);
        if (blend == null) {
            return 0.0D;
        }
        float b = ease((float) blend.value(time));
        switch (blend.form) {
            case ReedShapes.TRAMPOLINE:
                return -1.2D * b;
            case ReedShapes.BALL:
                return -0.9D * b;
            case ReedShapes.BOAT:
                return -0.35D * b;
            case ReedShapes.NECK:
                break;
            default:
                return 0.0D;
        }
        double neck = b * NECK_HEIGHT;
        Vec3 eye = player.getEyePosition(partial);
        HitResult hit = player.level().clip(new ClipContext(eye, eye.add(0.0D, neck + 0.3D, 0.0D), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? neck : Math.max(0.0D, hit.getLocation().y - eye.y - 0.3D);
    }

    private static boolean firstPerson(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        return player == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
    }

    public static boolean hidesHand(Player player, boolean rightArm) {
        if (!firstPerson(player)) {
            return false;
        }
        if (ReedShapes.has(player, ReedShapes.TRAMPOLINE_STATE) || ReedShapes.has(player, ReedShapes.BALL_STATE)
                || ReedShapes.has(player, ReedShapes.UMBRELLA_STATE)) {
            return true;
        }
        MrFantasticPower.Stretch stretch = MrFantasticPower.stretch(player, now(player));
        if (stretch != null) {
            return rightArm || stretch.mode() == MrFantasticPower.SWEEP || stretch.mode() == MrFantasticPower.SLING;
        }
        return MrFantasticPower.gliding(player);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !firstPerson(player)) {
            return;
        }
        long now = now(player);
        MrFantasticPower.Stretch stretch = MrFantasticPower.stretch(player, now);
        boolean gliding = stretch == null && MrFantasticPower.gliding(player);
        boolean umbrella = stretch == null && !gliding && ReedShapes.has(player, ReedShapes.UMBRELLA_STATE);
        if (stretch == null && !gliding && !umbrella) {
            return;
        }
        EntityRenderer<? super LocalPlayer> renderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        Vec3 look = player.getViewVector(partial);
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0D, flat.x);
        Vec3 up = right.cross(look).normalize();
        Vec3 rightStart = camera.add(look.scale(0.15D)).add(right.scale(0.38D)).subtract(up.scale(0.32D));
        Vec3 leftStart = camera.add(look.scale(0.15D)).subtract(right.scale(0.38D)).subtract(up.scale(0.32D));
        PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        ArmPass pass = new ArmPass(event.getPoseStack(), buffers, playerRenderer, player, camera, partial);
        if (umbrella) {
            Vec3 top = camera.add(0.0D, 1.2D, 0.0D).add(flat.scale(0.2D));
            drawArm(pass, model.rightArm, model.rightSleeve, rightStart, top.add(right.scale(0.12D)), 1.0F, 1.0F, true);
            drawArm(pass, model.leftArm, model.leftSleeve, leftStart, top.subtract(right.scale(0.12D)), 1.0F, 1.0F, false);
        } else if (stretch == null) {
            Vec3 outRight = right.scale(0.85D).add(flat.scale(0.45D)).add(0.0D, -0.2D, 0.0D).normalize();
            Vec3 outLeft = right.scale(-0.85D).add(flat.scale(0.45D)).add(0.0D, -0.2D, 0.0D).normalize();
            drawArm(pass, model.rightArm, model.rightSleeve, rightStart, rightStart.add(outRight.scale(2.4D)), 1.3F, 3.0F, true);
            drawArm(pass, model.leftArm, model.leftSleeve, leftStart, leftStart.add(outLeft.scale(2.4D)), 1.3F, 3.0F, false);
        } else {
            float factor = factor(stretch, now, partial);
            float width = width(stretch, factor);
            if (stretch.mode() == MrFantasticPower.SWEEP) {
                float spin = progress(stretch, now, partial) * Mth.TWO_PI * 2.0F;
                double length = Math.max(ARM_LENGTH, stretch.point().x * factor);
                Vec3 direction = right.scale(Mth.cos(spin)).add(flat.scale(Mth.sin(spin))).add(0.0D, -0.1D, 0.0D).normalize();
                drawArm(pass, model.rightArm, model.rightSleeve, rightStart, rightStart.add(direction.scale(length)), width, width, true);
                drawArm(pass, model.leftArm, model.leftSleeve, leftStart, leftStart.subtract(direction.scale(length)), width, width, false);
            } else if (stretch.mode() == MrFantasticPower.SLING) {
                float sling = 1.0F + 0.6F * factor;
                drawArm(pass, model.rightArm, model.rightSleeve, rightStart, slingAnchor(player, partial, true, factor), sling, sling, true);
                drawArm(pass, model.leftArm, model.leftSleeve, leftStart, slingAnchor(player, partial, false, factor), sling, sling, false);
            } else if (stretch.mode() == MrFantasticPower.HAMMER) {
                float t = progress(stretch, now, partial);
                float swell = hammerWidth(t);
                drawArm(pass, model.rightArm, model.rightSleeve, rightStart, hammerReach(player, partial, rightStart, stretch.point(), t), swell, swell, true);
            } else {
                double full = rightStart.distanceTo(stretch.point());
                double length = ARM_LENGTH + (full - ARM_LENGTH) * factor;
                Vec3 direction = stretch.point().subtract(rightStart).normalize();
                drawArm(pass, model.rightArm, model.rightSleeve, rightStart, rightStart.add(direction.scale(length)), width, width, true);
            }
        }
        buffers.endBatch();
    }

    private record ArmPass(PoseStack pose, MultiBufferSource.BufferSource buffers, PlayerRenderer renderer, LocalPlayer player,
                           Vec3 camera, float partial) {
    }

    private static void drawArm(ArmPass pass, ModelPart arm, ModelPart sleeve, Vec3 from, Vec3 to, float width, float depth, boolean rightArm) {
        PoseStack pose = pass.pose();
        LocalPlayer player = pass.player();
        Vec3 camera = pass.camera();
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-3D) {
            return;
        }
        Vec3 direction = delta.scale(1.0D / length);
        float yScale = (float) (length * 16.0D / (12.0D * 0.9375D));
        float[] armState = store(arm);
        float[] sleeveState = store(sleeve);
        for (ModelPart part : new ModelPart[]{arm, sleeve}) {
            part.x = 0.0F;
            part.y = 0.0F;
            part.z = 0.0F;
            part.xRot = 0.0F;
            part.yRot = 0.0F;
            part.zRot = 0.0F;
            part.xScale = width;
            part.yScale = yScale;
            part.zScale = depth;
        }
        arm.visible = true;
        int light = LevelRenderer.getLightColor(player.level(), BlockPos.containing(from.add(delta.scale(0.5D))));
        pose.pushPose();
        pose.translate(from.x - camera.x, from.y - camera.y, from.z - camera.z);
        pose.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) direction.x, (float) direction.y, (float) direction.z));
        pose.scale(0.9375F, 0.9375F, 0.9375F);
        pose.translate((rightArm ? 1.0F : -1.0F) * width / 16.0F, 2.0F * yScale / 16.0F, 0.0F);
        VertexConsumer consumer = pass.buffers().getBuffer(RenderType.entityTranslucent(player.getSkin().texture()));
        arm.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        sleeve.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        drawArmor(pass, arm, light);
        pose.popPose();
        restore(arm, armState);
        restore(sleeve, sleeveState);
    }

    private static void drawArmor(ArmPass pass, ModelPart arm, int light) {
        PlayerModel<AbstractClientPlayer> model = pass.renderer().getModel();
        ModelPart[] others = {model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
        float[][] states = new float[others.length][];
        for (int i = 0; i < others.length; i++) {
            states[i] = store(others[i]);
            if (others[i] != arm) {
                others[i].xScale = 0.0F;
                others[i].yScale = 0.0F;
                others[i].zScale = 0.0F;
            }
        }
        float age = pass.player().tickCount + pass.partial();
        for (RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> layer : pass.renderer().layers) {
            if (layer instanceof HumanoidArmorLayer<?, ?, ?>) {
                layer.render(pass.pose(), pass.buffers(), light, pass.player(), 0.0F, 0.0F, pass.partial(), age, 0.0F, 0.0F);
            }
        }
        for (int i = 0; i < others.length; i++) {
            if (others[i] != arm) {
                restore(others[i], states[i]);
            }
        }
    }

    private static float[] store(ModelPart part) {
        return new float[]{part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.xScale, part.yScale, part.zScale, part.visible ? 1.0F : 0.0F};
    }

    private static void restore(ModelPart part, float[] state) {
        part.x = state[0];
        part.y = state[1];
        part.z = state[2];
        part.xRot = state[3];
        part.yRot = state[4];
        part.zRot = state[5];
        part.xScale = state[6];
        part.yScale = state[7];
        part.zScale = state[8];
        part.visible = state[9] > 0.5F;
    }
}
