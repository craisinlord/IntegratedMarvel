package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicGrootPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class GrootGrowthRenderer {
    private static final double RANGE = 48.0D;
    private static final int BUDGET = 96;
    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState LEAVES = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    private static final BlockState BLOSSOM = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    private static final BlockState AZALEA = Blocks.AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    private static final Vec3[] SPIKES = spikes(18);

    private GrootGrowthRenderer() {
    }

    private static final class Ctx {
        private final PoseStack pose;
        private final MultiBufferSource buffers;
        private final BlockRenderDispatcher blocks;
        private final ClientLevel level;
        private final Vec3 camera;
        private int budget;

        private Ctx(PoseStack pose, MultiBufferSource buffers, BlockRenderDispatcher blocks, ClientLevel level, Vec3 camera) {
            this.pose = pose;
            this.buffers = buffers;
            this.blocks = blocks;
            this.level = level;
            this.camera = camera;
        }
    }

    private static Vec3[] spikes(int count) {
        Vec3[] out = new Vec3[count];
        double golden = Math.PI * (3.0D - Math.sqrt(5.0D));
        for (int i = 0; i < count; i++) {
            double y = 1.0D - (i + 0.5D) / count * 2.0D;
            double r = Math.sqrt(1.0D - y * y);
            double a = golden * i;
            out[i] = new Vec3(Math.cos(a) * r, y * 0.8D, Math.sin(a) * r).normalize();
        }
        return out;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        GrootMorphs.clearClient();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (now % 40L == 0L) {
            GrootMorphs.prune(now);
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Ctx ctx = new Ctx(event.getPoseStack(), buffers, minecraft.getBlockRenderer(), level, camera);
        boolean drew = false;
        for (Player player : level.players()) {
            if (player.isInvisible() || player.distanceToSqr(camera) > RANGE * RANGE) {
                continue;
            }
            if (!GrootMorphs.any(player) && GrootBranches.get(player) == null) {
                continue;
            }
            ctx.budget = BUDGET;
            draw(ctx, minecraft, player, partial, now);
            drew = true;
        }
        if (drew) {
            buffers.endBatch();
        }
    }

    private static boolean firstPerson(Minecraft minecraft, Player player) {
        return player == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
    }

    private static Vec3 handStart(Minecraft minecraft, Player player, float partial, boolean right, Vec3 camera) {
        if (!firstPerson(minecraft, player)) {
            return GrootClient.shoulder(player, partial, right);
        }
        Vec3 look = player.getViewVector(partial);
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0.0D, flat.x);
        Vec3 up = side.cross(look).normalize();
        return camera.add(look.scale(0.25D)).add(side.scale(right ? 0.38D : -0.38D)).subtract(up.scale(0.32D));
    }

    private static void draw(Ctx ctx, Minecraft minecraft, Player player, float partial, long now) {
        boolean first = firstPerson(minecraft, player);
        boolean costumed = ThematicGrootPose.costumed(player);
        long seed = player.getUUID().getLeastSignificantBits();
        double scale = player.getScale();
        Vec3 feet = player.getPosition(partial);
        for (boolean right : new boolean[]{true, false}) {
            GrootClient.Goal goal = GrootClient.arm(player, partial, right);
            if (goal == null || goal.factor() <= 0.0F) {
                continue;
            }
            Vec3 from = handStart(minecraft, player, partial, right, ctx.camera);
            Vec3 to = from.add(goal.point().subtract(from).scale(goal.factor()));
            if (first || !costumed) {
                limb(ctx, from, to, 0.2F * goal.thick(), 0.12F * goal.thick(), LOG, seed + (right ? 1 : 2), 0.06D);
            }
            twigs(ctx, from, to, goal.factor(), seed + (right ? 11 : 12));
        }
        GrootMorphs.Morph skewer = GrootMorphs.get(player, GrootMorphs.SKEWER);
        if (skewer != null && skewer.targets().length > 0) {
            float reach = GrootClient.skewerReach(skewer, now, partial);
            Vec3 hand = handStart(minecraft, player, partial, true, ctx.camera);
            Vec3 split = hand.add(skewer.point().subtract(hand).scale(0.3D * reach));
            for (int i = 0; i < skewer.targets().length; i++) {
                Vec3 target = GrootClient.entityCenter(player, skewer.targets()[i], partial, null);
                if (target == null) {
                    continue;
                }
                limb(ctx, split, split.add(target.subtract(split).scale(reach)), 0.16F, 0.03F, LOG, seed + 100 + i, 0.12D);
            }
        }
        GrootMorphs.Morph lash = GrootMorphs.get(player, GrootMorphs.LASH);
        if (lash != null && lash.targets().length > 0) {
            Vec3 target = GrootClient.entityCenter(player, lash.targets()[0], partial, null);
            if (target != null) {
                float reach = Math.min(1.0F, Mth.sin(lash.progress(now, partial) * Mth.PI) * 1.6F);
                Vec3 start = new Vec3(feet.x, feet.y + 0.05D, feet.z);
                Vec3 under = new Vec3(target.x, feet.y + 0.05D, target.z);
                float ground = Mth.clamp(reach * 2.0F, 0.0F, 1.0F);
                limb(ctx, start, start.add(under.subtract(start).scale(ground)), 0.22F, 0.14F, LOG, seed + 200, 0.15D);
                if (reach > 0.5F) {
                    limb(ctx, under, under.add(target.subtract(under).scale((reach - 0.5F) * 2.0F)), 0.16F, 0.05F, LOG, seed + 201, 0.2D);
                }
            }
        }
        GrootMorphs.Morph root = GrootMorphs.get(player, GrootMorphs.TAKE_ROOT);
        if (root != null) {
            float b = root.held(now, partial) * Mth.clamp(root.age(now, partial) / 10.0F, 0.0F, 1.0F);
            Vec3 base = new Vec3(feet.x, feet.y + 0.04D, feet.z);
            for (int i = 0; i < 7; i++) {
                double angle = i * Mth.TWO_PI / 7.0D + (seed & 255L) / 40.0D;
                Vec3 dir = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
                double length = (2.0D + ((seed >> i) & 3L) * 0.25D) * scale * b;
                limb(ctx, base.add(dir.scale(0.2D)), base.add(dir.scale(0.2D + length)).add(0.0D, -0.02D, 0.0D), 0.22F, 0.05F, LOG,
                        seed + 300 + i, 0.18D);
            }
        }
        GrootMorphs.Morph slam = GrootMorphs.get(player, GrootMorphs.TRUNK_SLAM);
        if (slam != null) {
            float t = slam.progress(now, partial);
            float grow = Mth.clamp((t - 0.4F) / 0.25F, 0.0F, 1.0F) * GrootClient.hold(t, 0.05F, 0.2F);
            Vec3 start = new Vec3(feet.x, feet.y + 0.05D, feet.z);
            Vec3 end = new Vec3(slam.point().x, slam.point().y + 0.05D, slam.point().z);
            limb(ctx, start, start.add(end.subtract(start).scale(grow)), 0.24F, 0.16F, LOG, seed + 400, 0.12D);
        }
        GrootMorphs.Morph lift = GrootMorphs.get(player, GrootMorphs.TRUNK_LIFT);
        if (lift != null) {
            float b = lift.held(now, partial);
            for (int i = 0; i < 4; i++) {
                double angle = i * Mth.HALF_PI + 0.4D;
                Vec3 top = feet.add(Math.cos(angle) * 0.3D * scale, 0.05D, Math.sin(angle) * 0.3D * scale);
                limb(ctx, top, top.add(0.0D, -0.8D * b, 0.0D), 0.08F, 0.02F, LOG, seed + 500 + i, 0.25D);
            }
        }
        GrootMorphs.Morph wall = GrootMorphs.get(player, GrootMorphs.WALL);
        if (wall != null) {
            float grow = GrootClient.hold(wall.progress(now, partial), 0.35F, 0.25F);
            Vec3 start = new Vec3(feet.x, feet.y + 0.05D, feet.z);
            for (Vec3 corner : new Vec3[]{wall.point(), wall.aux()}) {
                Vec3 end = new Vec3(corner.x, feet.y + 0.05D, corner.z);
                limb(ctx, start, start.add(end.subtract(start).scale(grow)), 0.2F, 0.1F, LOG, seed + 600 + (corner == wall.point() ? 0 : 1), 0.15D);
            }
        }
        GrootMorphs.Morph shelter = GrootMorphs.get(player, GrootMorphs.SHELTER);
        if (shelter != null) {
            float b = shelter.held(now, partial);
            for (boolean right : new boolean[]{true, false}) {
                GrootClient.Goal goal = GrootClient.arm(player, partial, right);
                if (goal == null) {
                    continue;
                }
                Vec3 from = GrootClient.shoulder(player, partial, right);
                for (double f : new double[]{0.45D, 0.75D, 1.0D}) {
                    blob(ctx, from.add(goal.point().subtract(from).scale(f * goal.factor())), (float) (0.55D * b), f > 0.9D ? AZALEA : LEAVES);
                }
                blob(ctx, from.add(0.0D, 0.15D, 0.0D), 0.3F * b, BLOSSOM);
            }
        }
        GrootMorphs.Morph thorn = GrootMorphs.get(player, GrootMorphs.THORN);
        if (thorn != null) {
            float env = Mth.sin(thorn.progress(now, partial) * Mth.PI);
            Vec3 center = feet.add(0.0D, player.getBbHeight() * 0.55D, 0.0D);
            double length = 1.4D * scale * env;
            for (int i = 0; i < SPIKES.length; i++) {
                Vec3 dir = SPIKES[i];
                Vec3 start = center.add(dir.x * 0.3D * scale, dir.y * player.getBbHeight() * 0.45D, dir.z * 0.3D * scale);
                limb(ctx, start, start.add(dir.scale(length)), 0.14F, 0.015F, LOG, seed + 700 + i, 0.0D);
            }
        }
        GrootMorphs.Morph bloom = GrootMorphs.get(player, GrootMorphs.BLOOM);
        if (bloom != null && !first) {
            float b = bloom.held(now, partial);
            blob(ctx, feet.add(0.0D, player.getBbHeight() + 0.05D, 0.0D), 0.45F * b, BLOSSOM);
            blob(ctx, GrootClient.shoulder(player, partial, true).add(0.0D, 0.18D, 0.0D), 0.3F * b, BLOSSOM);
            blob(ctx, GrootClient.shoulder(player, partial, false).add(0.0D, 0.18D, 0.0D), 0.3F * b, AZALEA);
        }
        GrootMorphs.Morph sprout = GrootMorphs.get(player, GrootMorphs.SPROUT);
        if (sprout != null && !first) {
            float b = sprout.held(now, partial);
            blob(ctx, GrootClient.shoulder(player, partial, true).add(0.0D, 0.16D, 0.0D), 0.2F * b, LEAVES);
            blob(ctx, GrootClient.shoulder(player, partial, false).add(0.0D, 0.16D, 0.0D), 0.18F * b, LEAVES);
        }
    }

    private static void twigs(Ctx ctx, Vec3 from, Vec3 to, float factor, long seed) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.5D) {
            return;
        }
        Vec3 dir = delta.scale(1.0D / length);
        Vec3 n = perpendicular(dir);
        Vec3 m = dir.cross(n).normalize();
        float size = Mth.clamp(factor * 1.5F - 0.3F, 0.0F, 1.0F);
        for (int i = 0; i < 3; i++) {
            double at = 0.35D + i * 0.25D;
            double angle = ((seed >> (i * 5)) & 31L) / 31.0D * Mth.TWO_PI;
            Vec3 out = n.scale(Math.cos(angle)).add(m.scale(Math.sin(angle))).add(dir.scale(0.4D)).normalize();
            Vec3 base = from.add(delta.scale(at));
            Vec3 tip = base.add(out.scale(0.45D * size));
            limb(ctx, base, tip, 0.06F, 0.03F, LOG, seed + i, 0.0D);
            blob(ctx, tip, 0.28F * size, LEAVES);
        }
    }

    private static Vec3 perpendicular(Vec3 dir) {
        Vec3 n = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (n.lengthSqr() < 1.0E-4D) {
            n = dir.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        return n.normalize();
    }

    private static void limb(Ctx ctx, Vec3 from, Vec3 to, float thickFrom, float thickTo, BlockState state, long seed, double bend) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.03D) {
            return;
        }
        Vec3 dir = delta.scale(1.0D / length);
        Vec3 n = perpendicular(dir);
        Vec3 m = dir.cross(n).normalize();
        double angle = (seed * 2654435761L & 1023L) / 1023.0D * Mth.TWO_PI;
        Vec3 control = from.add(delta.scale(0.5D)).add(n.scale(Math.cos(angle) * length * bend)).add(m.scale(Math.sin(angle) * length * bend));
        int segments = Mth.clamp((int) (length * 1.5D) + 1, 1, 6);
        Vec3 previous = from;
        for (int i = 1; i <= segments; i++) {
            double s = (double) i / segments;
            double inv = 1.0D - s;
            Vec3 point = from.scale(inv * inv).add(control.scale(2.0D * inv * s)).add(to.scale(s * s));
            float thick = Mth.lerp((float) ((i - 0.5D) / segments), thickFrom, thickTo);
            segment(ctx, previous, point, thick, state);
            previous = point;
        }
    }

    private static void segment(Ctx ctx, Vec3 from, Vec3 to, float thick, BlockState state) {
        if (ctx.budget-- <= 0) {
            return;
        }
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-3D || thick <= 0.0F) {
            return;
        }
        Vec3 dir = delta.scale(1.0D / length);
        PoseStack pose = ctx.pose;
        pose.pushPose();
        pose.translate(from.x - ctx.camera.x, from.y - ctx.camera.y, from.z - ctx.camera.z);
        pose.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) dir.x, (float) dir.y, (float) dir.z));
        pose.scale(thick, (float) length, thick);
        pose.translate(-0.5F, 0.0F, -0.5F);
        int light = LevelRenderer.getLightColor(ctx.level, BlockPos.containing(from.add(delta.scale(0.5D))));
        ctx.blocks.renderSingleBlock(state, pose, ctx.buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static void blob(Ctx ctx, Vec3 center, float size, BlockState state) {
        if (size <= 0.01F || ctx.budget-- <= 0) {
            return;
        }
        PoseStack pose = ctx.pose;
        pose.pushPose();
        pose.translate(center.x - ctx.camera.x - size * 0.5D, center.y - ctx.camera.y - size * 0.5D, center.z - ctx.camera.z - size * 0.5D);
        pose.scale(size, size, size);
        int light = LevelRenderer.getLightColor(ctx.level, BlockPos.containing(center));
        ctx.blocks.renderSingleBlock(state, pose, ctx.buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
