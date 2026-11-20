package com.craisinlord.integrated_superheroes.hero;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class GrootClient {
    private GrootClient() {
    }

    public record Goal(Vec3 point, float factor, float thick) {
    }

    public static Vec3 shoulder(Player player, float partial, boolean right) {
        float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double scale = player.getScale();
        Vec3 side = new Vec3(-Mth.cos(yaw), 0.0D, -Mth.sin(yaw)).scale((right ? 0.33D : -0.33D) * scale);
        return player.getPosition(partial).add(0.0D, player.getBbHeight() * 0.76D, 0.0D).add(side);
    }

    public static Vec3 forward(Player player, float partial) {
        float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
    }

    static float hold(float t, float in, float out) {
        if (t < in) {
            return GrootMorphs.ease(t / in);
        }
        if (t > 1.0F - out) {
            return GrootMorphs.ease(Math.max(0.0F, (1.0F - t) / out));
        }
        return 1.0F;
    }

    static float skewerReach(GrootMorphs.Morph morph, long now, float partial) {
        float age = morph.age(now, partial);
        float total = Math.max(1.0F, morph.end() - morph.start());
        float grow = Mth.clamp(age / 4.0F, 0.0F, 1.0F);
        float retract = Mth.clamp((total - age) / 8.0F, 0.0F, 1.0F);
        return GrootMorphs.ease(Math.min(grow, retract));
    }

    static Vec3 entityCenter(Player player, int id, float partial, Vec3 fallback) {
        Entity entity = player.level().getEntity(id);
        if (entity != null && entity.isAlive()) {
            return entity.getPosition(partial).add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        }
        return fallback;
    }

    public static Goal arm(Player player, float partial, boolean right) {
        long now = player.level().getGameTime();
        GrootMorphs.Morph shelter = GrootMorphs.get(player, GrootMorphs.SHELTER);
        if (shelter != null) {
            float b = shelter.held(now, partial);
            Vec3 side = shoulder(player, partial, right).subtract(shoulder(player, partial, !right)).normalize();
            Vec3 dir = new Vec3(0.0D, 1.0D, 0.0D).add(side.scale(0.55D)).normalize();
            double radius = Math.max(2.0D, shelter.aux().x);
            return new Goal(shoulder(player, partial, right).add(dir.scale(radius)), b, 1.25F);
        }
        GrootMorphs.Morph wall = GrootMorphs.get(player, GrootMorphs.WALL);
        if (wall != null) {
            float f = hold(wall.progress(now, partial), 0.3F, 0.3F);
            return new Goal(right ? wall.point() : wall.aux(), f, 1.3F);
        }
        if (!right) {
            GrootMorphs.Morph grapple = GrootMorphs.get(player, GrootMorphs.GRAPPLE);
            if (grapple != null) {
                Vec3 anchor = grapple.targets().length > 0 ? entityCenter(player, grapple.targets()[0], partial, grapple.point()) : grapple.point();
                return new Goal(anchor, grapple.held(now, partial), 1.3F);
            }
            return null;
        }
        GrootMorphs.Morph skewer = GrootMorphs.get(player, GrootMorphs.SKEWER);
        if (skewer != null) {
            return new Goal(skewer.point(), skewerReach(skewer, now, partial), 1.5F);
        }
        GrootBranches.Branch branch = GrootBranches.get(player);
        if (branch != null) {
            float factor = GrootBranches.extension(branch, player, partial);
            if (factor > 0.0F) {
                return new Goal(GrootBranches.point(branch, player, partial), factor, 1.0F + 0.3F * factor);
            }
        }
        return null;
    }

    private static void aim(ModelPart arm, Player player, float partial, Vec3 from, Vec3 to, float blend) {
        Vec3 delta = to.subtract(from);
        if (delta.lengthSqr() < 1.0E-4D || blend <= 0.0F) {
            return;
        }
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yawTo = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitchTo = (float) (-(Mth.atan2(delta.y, horizontal) * Mth.RAD_TO_DEG));
        float body = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        arm.xRot = Mth.lerp(blend, arm.xRot, -Mth.HALF_PI + pitchTo * Mth.DEG_TO_RAD);
        arm.yRot = Mth.lerp(blend, arm.yRot, Mth.wrapDegrees(yawTo - body) * Mth.DEG_TO_RAD);
        arm.zRot = Mth.lerp(blend, arm.zRot, 0.0F);
    }

    private static void turn(ModelPart part, float xRot, float yRot, float zRot, float b) {
        part.xRot = Mth.lerp(b, part.xRot, xRot);
        part.yRot = Mth.lerp(b, part.yRot, yRot);
        part.zRot = Mth.lerp(b, part.zRot, zRot);
    }

    public static void pose(Player player, HumanoidModel<?> model, float partial) {
        if (!GrootMorphs.any(player) && GrootBranches.get(player) == null) {
            return;
        }
        long now = player.level().getGameTime();
        float clock = player.tickCount + partial;
        GrootMorphs.Morph root = GrootMorphs.get(player, GrootMorphs.TAKE_ROOT);
        if (root != null) {
            float b = root.held(now, partial);
            float sway = Mth.sin(clock * 0.08F) * 0.06F;
            turn(model.rightArm, -0.6F + sway, 0.0F, 0.5F, b);
            turn(model.leftArm, -0.6F - sway, 0.0F, -0.5F, b);
            turn(model.rightLeg, 0.0F, 0.0F, 0.14F, b);
            turn(model.leftLeg, 0.0F, 0.0F, -0.14F, b);
        }
        GrootMorphs.Morph lift = GrootMorphs.get(player, GrootMorphs.TRUNK_LIFT);
        if (lift != null) {
            float b = lift.held(now, partial);
            turn(model.rightLeg, 0.0F, 0.0F, 0.0F, b);
            turn(model.leftLeg, 0.0F, 0.0F, 0.0F, b);
            turn(model.rightArm, -0.3F, 0.0F, 0.35F, b);
            turn(model.leftArm, -0.3F, 0.0F, -0.35F, b);
        }
        GrootMorphs.Morph grapple = GrootMorphs.get(player, GrootMorphs.GRAPPLE);
        if (grapple != null) {
            float b = grapple.held(now, partial);
            turn(model.rightLeg, 0.6F, 0.0F, 0.08F, b);
            turn(model.leftLeg, 0.45F, 0.0F, -0.08F, b);
            turn(model.rightArm, 0.4F + Mth.sin(clock * 0.9F) * 0.25F, 0.0F, 0.35F, b);
        }
        GrootMorphs.Morph slam = GrootMorphs.get(player, GrootMorphs.TRUNK_SLAM);
        if (slam != null) {
            float t = slam.progress(now, partial);
            float x = t < 0.4F ? Mth.lerp(t / 0.4F, 0.0F, -Mth.PI) : t < 0.55F ? Mth.lerp((t - 0.4F) / 0.15F, -Mth.PI, -0.5F) : -0.5F;
            float b = hold(t, 0.15F, 0.3F);
            turn(model.rightArm, x, 0.0F, 0.1F, b);
            turn(model.leftArm, x, 0.0F, -0.1F, b);
        }
        GrootMorphs.Morph thorn = GrootMorphs.get(player, GrootMorphs.THORN);
        if (thorn != null) {
            float env = Mth.sin(thorn.progress(now, partial) * Mth.PI);
            turn(model.rightArm, 0.0F, 0.0F, 1.45F, env);
            turn(model.leftArm, 0.0F, 0.0F, -1.45F, env);
            turn(model.head, -0.4F, model.head.yRot, 0.0F, env);
        }
        GrootMorphs.Morph bloom = GrootMorphs.get(player, GrootMorphs.BLOOM);
        if (bloom != null) {
            float age = bloom.age(now, partial);
            float b = age < 30.0F ? hold(age / 30.0F, 0.25F, 0.35F) : 0.0F;
            turn(model.head, -0.25F, model.head.yRot, 0.0F, b);
            turn(model.rightArm, 0.1F, 0.0F, 0.65F, b);
            turn(model.leftArm, 0.1F, 0.0F, -0.65F, b);
        }
        GrootMorphs.Morph shelter = GrootMorphs.get(player, GrootMorphs.SHELTER);
        if (shelter != null) {
            turn(model.head, -0.3F, model.head.yRot, 0.0F, shelter.held(now, partial));
        }
        for (boolean right : new boolean[]{true, false}) {
            Goal goal = arm(player, partial, right);
            if (goal != null) {
                aim(right ? model.rightArm : model.leftArm, player, partial, shoulder(player, partial, right), goal.point(),
                        Mth.clamp(goal.factor() * 4.0F, 0.0F, 1.0F));
            }
        }
        if (model instanceof PlayerModel<?> playerModel) {
            playerModel.rightSleeve.copyFrom(model.rightArm);
            playerModel.leftSleeve.copyFrom(model.leftArm);
            playerModel.rightPants.copyFrom(model.rightLeg);
            playerModel.leftPants.copyFrom(model.leftLeg);
            playerModel.hat.copyFrom(model.head);
        }
    }

    private static void hips(PoseStack pose, float degrees) {
        pose.translate(0.0F, 0.75F, 0.0F);
        pose.mulPose(Axis.XP.rotationDegrees(degrees));
        pose.translate(0.0F, -0.75F, 0.0F);
    }

    private static void widen(ModelPart part, ModelPart outer, float width, float height, float b) {
        part.xScale = Mth.lerp(b, part.xScale, width);
        part.zScale = Mth.lerp(b, part.zScale, width);
        part.yScale = Mth.lerp(b, part.yScale, height);
        outer.xScale = part.xScale;
        outer.yScale = part.yScale;
        outer.zScale = part.zScale;
    }

    public static void morph(Player player, PlayerModel<?> model, float partial, PoseStack pose) {
        if (!GrootMorphs.any(player)) {
            return;
        }
        long now = player.level().getGameTime();
        GrootMorphs.Morph root = GrootMorphs.get(player, GrootMorphs.TAKE_ROOT);
        if (root != null) {
            float b = root.held(now, partial);
            pose.translate(0.0F, 0.25F * b, 0.0F);
            widen(model.rightLeg, model.rightPants, 1.5F, 0.85F, b);
            widen(model.leftLeg, model.leftPants, 1.5F, 0.85F, b);
            widen(model.body, model.jacket, 1.12F, 1.0F, b);
        }
        GrootMorphs.Morph lift = GrootMorphs.get(player, GrootMorphs.TRUNK_LIFT);
        if (lift != null) {
            float b = lift.held(now, partial);
            widen(model.rightLeg, model.rightPants, 1.6F, 1.0F, b);
            widen(model.leftLeg, model.leftPants, 1.6F, 1.0F, b);
        }
        GrootMorphs.Morph grapple = GrootMorphs.get(player, GrootMorphs.GRAPPLE);
        if (grapple != null) {
            hips(pose, 35.0F * grapple.held(now, partial));
        }
        GrootMorphs.Morph skewer = GrootMorphs.get(player, GrootMorphs.SKEWER);
        if (skewer != null) {
            hips(pose, 15.0F * skewerReach(skewer, now, partial));
        }
        GrootMorphs.Morph wall = GrootMorphs.get(player, GrootMorphs.WALL);
        if (wall != null) {
            float f = hold(wall.progress(now, partial), 0.3F, 0.3F);
            pose.translate(0.0F, 0.1F * f, 0.0F);
            hips(pose, 12.0F * f);
        }
        GrootMorphs.Morph shelter = GrootMorphs.get(player, GrootMorphs.SHELTER);
        if (shelter != null) {
            hips(pose, -8.0F * shelter.held(now, partial));
        }
        GrootMorphs.Morph thorn = GrootMorphs.get(player, GrootMorphs.THORN);
        if (thorn != null) {
            float s = 1.0F + 0.25F * Mth.sin(thorn.progress(now, partial) * Mth.PI);
            pose.translate(0.0F, 0.75F, 0.0F);
            pose.scale(s, s, s);
            pose.translate(0.0F, -0.75F, 0.0F);
            widen(model.body, model.jacket, s, 1.0F, 1.0F);
        }
    }
}
