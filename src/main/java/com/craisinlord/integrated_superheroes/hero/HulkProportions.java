package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class HulkProportions {
    private static final DustParticleOptions GAMMA = new DustParticleOptions(new Vector3f(0.35F, 1.0F, 0.2F), 1.3F);
    private static final Map<UUID, Blend> BLENDS = new HashMap<>();
    private static final float[] SAVED = new float[36];
    private static boolean applied;

    private HulkProportions() {
    }

    private static final class Blend {
        private float hulk;
        private float hulkO;
        private float rage;
        private float rageO;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            BLENDS.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        for (Player player : level.players()) {
            boolean hulk = HulkPower.isHulk(player);
            Blend blend = BLENDS.get(player.getUUID());
            if (blend == null) {
                if (!hulk) {
                    continue;
                }
                blend = new Blend();
                BLENDS.put(player.getUUID(), blend);
            }
            blend.hulkO = blend.hulk;
            blend.rageO = blend.rage;
            blend.hulk = Mth.approach(blend.hulk, hulk ? 1.0F : 0.0F, 0.34F);
            float rage = hulk ? HulkPower.clientRage(player) / 100.0F : 0.0F;
            blend.rage += (rage - blend.rage) * 0.12F;
            if (!hulk && blend.hulk <= 0.0F && blend.rage < 0.01F) {
                BLENDS.remove(player.getUUID());
                continue;
            }
            if (hulk) {
                emitGamma(minecraft, level, player, rage);
            }
        }
        BLENDS.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
    }

    private static void emitGamma(Minecraft minecraft, ClientLevel level, Player player, float rage) {
        if (rage < 0.6F || player.isInvisible()) {
            return;
        }
        if (player == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        RandomSource random = level.getRandom();
        float chance = (rage - 0.6F) / 0.4F * 0.8F + 0.2F;
        int count = random.nextFloat() < chance ? 1 + (rage >= 1.0F ? 1 : 0) : 0;
        double radius = player.getBbWidth() * 0.6D;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            level.addParticle(GAMMA, player.getX() + Math.cos(angle) * radius, player.getY() + random.nextDouble() * player.getBbHeight(),
                    player.getZ() + Math.sin(angle) * radius, 0.0D, 0.02D, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        BLENDS.clear();
    }

    private static ModelPart[] parts(PlayerModel<?> model) {
        return new ModelPart[]{model.head, model.hat, model.body, model.jacket, model.rightArm, model.leftArm,
                model.rightSleeve, model.leftSleeve, model.rightLeg, model.leftLeg, model.rightPants, model.leftPants};
    }

    private static void save(PlayerModel<?> model) {
        ModelPart[] parts = parts(model);
        for (int i = 0; i < parts.length; i++) {
            SAVED[i * 3] = parts[i].x;
            SAVED[i * 3 + 1] = parts[i].y;
            SAVED[i * 3 + 2] = parts[i].z;
        }
        applied = true;
    }

    public static void reset(PlayerModel<?> model) {
        if (!applied) {
            return;
        }
        ModelPart[] parts = parts(model);
        for (int i = 0; i < parts.length; i++) {
            parts[i].x = SAVED[i * 3];
            parts[i].y = SAVED[i * 3 + 1];
            parts[i].z = SAVED[i * 3 + 2];
            parts[i].xScale = 1.0F;
            parts[i].yScale = 1.0F;
            parts[i].zScale = 1.0F;
        }
        applied = false;
    }

    private static void scale(ModelPart part, ModelPart layer, float x, float y, float z) {
        part.xScale = x;
        part.yScale = y;
        part.zScale = z;
        layer.copyFrom(part);
        layer.xScale = x;
        layer.yScale = y;
        layer.zScale = z;
    }

    public static void pose(Player player, PlayerModel<?> model, float partial, PoseStack pose) {
        Blend blend = BLENDS.get(player.getUUID());
        if (blend == null) {
            return;
        }
        float h = Mth.lerp(partial, blend.hulkO, blend.hulk);
        if (h <= 0.0F) {
            return;
        }
        h = h * h * (3.0F - 2.0F * h);
        float r = Mth.lerp(partial, blend.rageO, blend.rage) * h;
        save(model);

        float bodyX = 1.0F + h * 0.35F + r * 0.15F;
        float bodyY = 1.0F + h * 0.05F + r * 0.03F;
        float bodyZ = 1.0F + h * 0.4F + r * 0.15F;
        float armX = 1.0F + h * 0.55F + r * 0.2F;
        float armY = 1.0F + h * 0.12F + r * 0.05F;
        float armZ = 1.0F + h * 0.5F + r * 0.2F;
        float legX = 1.0F + h * 0.3F + r * 0.12F;
        float legY = 1.0F - h * 0.12F - r * 0.03F;
        float legZ = 1.0F + h * 0.3F + r * 0.12F;
        float headScale = 1.0F - h * 0.12F - r * 0.04F;
        float lean = (h * 8.0F + r * 5.0F) * Mth.DEG_TO_RAD;

        scale(model.body, model.jacket, bodyX, bodyY, bodyZ);

        float bodyBottom = model.body.y + 12.0F * bodyY;
        float legDrop = 12.0F * (bodyY - 1.0F);
        for (boolean right : new boolean[]{true, false}) {
            float sign = right ? -1.0F : 1.0F;
            ModelPart arm = right ? model.rightArm : model.leftArm;
            ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;
            float defaultX = sign * 5.0F;
            arm.x = arm.x - defaultX + sign * (4.0F * bodyX + armX);
            arm.y -= h * 0.6F + r * 0.5F;
            arm.z -= h * 0.5F;
            scale(arm, sleeve, armX, armY, armZ);

            ModelPart leg = right ? model.rightLeg : model.leftLeg;
            ModelPart pants = right ? model.rightPants : model.leftPants;
            leg.x = sign * (1.9F + h * 0.7F + r * 0.35F);
            leg.y += legDrop;
            leg.xRot -= lean;
            scale(leg, pants, legX, legY, legZ);
        }

        model.head.y += h * 1.6F + r * 0.6F;
        model.head.z -= h * 1.0F + r * 0.5F;
        model.head.xRot -= lean * 0.7F;
        scale(model.head, model.hat, headScale, headScale, headScale);

        float feet = model.rightLeg.y + 12.0F * legY;
        float original = SAVED[8 * 3 + 1] + 12.0F;
        float sink = (original - feet) / 16.0F;
        if (sink != 0.0F) {
            pose.translate(0.0F, sink, 0.0F);
        }
        float hip = bodyBottom / 16.0F;
        pose.translate(0.0F, hip, 0.0F);
        pose.mulPose(Axis.XP.rotation(lean));
        pose.translate(0.0F, -hip, 0.0F);
    }

    public static float handWidth(Player player) {
        Blend blend = BLENDS.get(player.getUUID());
        if (blend == null || blend.hulk <= 0.0F) {
            return 1.0F;
        }
        return 1.0F + blend.hulk * 0.4F + blend.rage * blend.hulk * 0.15F;
    }
}
