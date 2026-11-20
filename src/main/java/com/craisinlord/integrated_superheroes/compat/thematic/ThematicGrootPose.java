package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.model.pose.BoneHandle;
import bond.thematic.api.model.pose.PoseState;
import com.craisinlord.integrated_superheroes.hero.GrootClient;
import com.craisinlord.integrated_superheroes.hero.GrootMorphs;
import com.craisinlord.integrated_superheroes.hero.GrootPower;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public final class ThematicGrootPose {
    private static final float EXTENSION_TOP = 7.95F;
    private static final float EXTENSION_LENGTH = 6.8F;
    private static final float MAX_SCALE = 40.0F;
    private static final Map<Integer, Long> APPLIED = new HashMap<>();

    private ThematicGrootPose() {
    }

    public static boolean costumed(Player player) {
        Long at = APPLIED.get(player.getId());
        return at != null && player.level().getGameTime() - at <= 2L;
    }

    public static void stretch(Player player, PoseState pose) {
        if (pose == null || pose.model() == null || pose.model().bone("extensionRight") == null || !GrootPower.isGroot(player)) {
            return;
        }
        APPLIED.put(player.getId(), player.level().getGameTime());
        if (APPLIED.size() > 256) {
            long now = player.level().getGameTime();
            APPLIED.values().removeIf(at -> now - at > 200L);
        }
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        long now = player.level().getGameTime();
        for (boolean right : new boolean[]{true, false}) {
            GrootClient.Goal goal = GrootClient.arm(player, partial, right);
            float scale = 1.0F;
            float thick = 1.0F;
            if (goal != null && goal.factor() > 0.0F) {
                double blocks = goal.point().distanceTo(GrootClient.shoulder(player, partial, right)) * goal.factor();
                double pixels = blocks * 16.0D / (0.9375D * Math.max(0.1D, player.getScale()));
                scale = Mth.clamp((float) ((pixels - EXTENSION_TOP) / EXTENSION_LENGTH), 1.0F, MAX_SCALE);
                thick = Mth.lerp(goal.factor(), 1.0F, goal.thick());
            }
            extend(pose.bone(right ? "extensionRight" : "extensionLeft"), scale, thick);
            size(pose.bone(right ? "rightArmBottom" : "leftArmBottom"), thick, 1.0F, thick);
        }
        float top = 1.0F;
        float bottom = 1.0F;
        float legWidth = 1.0F;
        float legHeight = 1.0F;
        GrootMorphs.Morph root = GrootMorphs.get(player, GrootMorphs.TAKE_ROOT);
        if (root != null) {
            float b = root.held(now, partial);
            top = Mth.lerp(b, top, 1.1F);
            bottom = Mth.lerp(b, bottom, 1.15F);
            legWidth = Mth.lerp(b, legWidth, 1.5F);
            legHeight = Mth.lerp(b, legHeight, 0.85F);
        }
        GrootMorphs.Morph lift = GrootMorphs.get(player, GrootMorphs.TRUNK_LIFT);
        if (lift != null) {
            legWidth = Math.max(legWidth, Mth.lerp(lift.held(now, partial), 1.0F, 1.6F));
        }
        GrootMorphs.Morph thorn = GrootMorphs.get(player, GrootMorphs.THORN);
        if (thorn != null) {
            top = Math.max(top, 1.0F + 0.3F * Mth.sin(thorn.progress(now, partial) * Mth.PI));
        }
        size(pose.bone("bodyTop"), top, 1.0F, top);
        size(pose.bone("bodyBottom"), bottom, 1.0F, bottom);
        for (String leg : new String[]{"armorRightLeg", "armorLeftLeg", "armorRightBoot", "armorLeftBoot"}) {
            size(pose.bone(leg), legWidth, legHeight, legWidth);
        }
    }

    private static void extend(BoneHandle bone, float scale, float thick) {
        if (bone == null) {
            return;
        }
        if (scale <= 1.0F && thick <= 1.0F) {
            bone.updateScale(bone.restScaleX(), bone.restScaleY(), bone.restScaleZ());
            bone.updatePosition(bone.restPositionX(), bone.restPositionY(), bone.restPositionZ());
            return;
        }
        bone.updateScale(bone.restScaleX() * thick, bone.restScaleY() * scale, bone.restScaleZ() * thick);
        bone.updatePosition(bone.restPositionX(), bone.restPositionY() + EXTENSION_TOP * (scale - 1.0F), bone.restPositionZ());
    }

    private static void size(BoneHandle bone, float x, float y, float z) {
        if (bone != null) {
            bone.updateScale(bone.restScaleX() * x, bone.restScaleY() * y, bone.restScaleZ() * z);
        }
    }
}
