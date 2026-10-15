package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpaceStone {
    private static final int FOLD_WINDOW = 60;
    private static final int SINGULARITY_TICKS = 120;
    private static final Map<UUID, FoldChain> FOLD_CHAINS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PULLING_RIFTS = new ConcurrentHashMap<>();
    private static final List<Singularity> SINGULARITIES = new ArrayList<>();

    private SpaceStone() {
    }

    public static void fold(GauntletCast cast) {
        if (cast.onCooldown()) return;
        ServerPlayer player = cast.player();
        boolean moved;
        if (cast.crouching()) {
            LivingEntity target = GauntletAim.living(player, 96.0D);
            if (target == null || GauntletAim.isProtectedEntity(target)) {
                cast.message("space_no_target");
                return;
            }
            Vec3 mine = player.position();
            Vec3 theirs = target.position();
            float yaw = player.getYRot();
            moved = SpaceTravel.teleport(target, cast.level(), mine, target.getYRot())
                    && SpaceTravel.teleport(player, cast.level(), theirs, yaw);
            if (moved) {
                GauntletFx.burst(cast.level(), mine.add(0.0D, 1.0D, 0.0D), "space", 30, 0.5D);
                GauntletFx.burst(cast.level(), theirs.add(0.0D, 1.0D, 0.0D), "space", 30, 0.5D);
                cast.message("space_swapped", target.getDisplayName());
            }
        } else {
            BlockHitResult hit = GauntletAim.block(player, 320.0D);
            if (hit.getType() != HitResult.Type.BLOCK) {
                cast.message("space_no_target");
                return;
            }
            Vec3 from = player.position();
            Vec3 arrival = SpaceTravel.arrivalPosition(player, cast.level(), hit.getBlockPos().relative(hit.getDirection()));
            moved = SpaceTravel.teleport(player, cast.level(), arrival, player.getYRot());
            if (moved) {
                GauntletFx.burst(cast.level(), from.add(0.0D, 1.0D, 0.0D), "space", 30, 0.5D);
                GauntletFx.burst(cast.level(), arrival.add(0.0D, 1.0D, 0.0D), "space", 30, 0.5D);
            }
        }
        if (!moved) {
            cast.message("space_arrival_failed");
            return;
        }
        long now = player.server.overworld().getGameTime();
        int max = cast.infinity() ? 5 : 3;
        FoldChain chain = FOLD_CHAINS.get(player.getUUID());
        int used = chain == null || now > chain.windowEnd() ? 1 : chain.used() + 1;
        if (used >= max) {
            FOLD_CHAINS.remove(player.getUUID());
            cast.cooldown();
            cast.message("space_travelled");
        } else {
            FOLD_CHAINS.put(player.getUUID(), new FoldChain(used, now + FOLD_WINDOW, cast.infinity()));
            cast.message("space_fold_chain", max - used);
        }
    }

    public static void rift(GauntletCast cast) {
        ServerPlayer player = cast.player();
        if (cast.crouching()) {
            SpaceDestinationManager.travel(player);
            return;
        }
        BlockHitResult hit = GauntletAim.block(player, 32.0D);
        if (hit.getType() != HitResult.Type.BLOCK) {
            cast.message("space_no_target");
            return;
        }
        if (SpaceDestinationManager.selected(player) == null) {
            cast.message("space_choose_destination");
            return;
        }
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        Vec3 portalPosition = hit.getLocation();
        if (hit.getDirection().getAxis().isVertical()) {
            Vec3 look = player.getLookAngle();
            normal = new Vec3(look.x, 0.0D, look.z).normalize();
            portalPosition = portalPosition.add(0.0D, 0.9D, 0.0D);
        }
        if (SpaceDestinationManager.openPortal(player, hit.getBlockPos(), portalPosition, normal) && cast.infinity()) {
            PULLING_RIFTS.put(player.getUUID(), player.server.overworld().getGameTime() + 1200L);
        }
    }

    public static void singularity(GauntletCast cast) {
        ServerPlayer player = cast.player();
        Vec3 center = GauntletAim.point(player, 64.0D).add(0.0D, 2.5D, 0.0D);
        double radius = (cast.infinity() ? 32.0D : 20.0D) * Math.min(2.0D, GauntletConfig.powerMultiplier());
        SINGULARITIES.add(new Singularity(cast.level().dimension(), center, player.getUUID(),
                player.server.overworld().getGameTime() + SINGULARITY_TICKS, radius, cast.power(), cast.infinity()));
        GauntletFx.cast(player, "space", SoundEvents.END_PORTAL_SPAWN, 0.5F);
        GauntletFx.shake(cast.level(), center, 1.0F, SINGULARITY_TICKS, (float) radius * 2.0F);
        cast.message("space_singularity");
        cast.cooldown();
    }

    public static void banish(GauntletCast cast) {
        ServerPlayer player = cast.player();
        LivingEntity target = GauntletAim.living(player, 64.0D);
        if (target == null || GauntletAim.isProtectedEntity(target)) {
            cast.message("space_no_target");
            return;
        }
        ServerLevel level = cast.level();
        Vec3 from = target.position();
        boolean moved;
        if (cast.crouching()) {
            ServerLevel end = player.server.getLevel(Level.END);
            if (end == null) end = level;
            moved = SpaceTravel.teleport(target, end, new Vec3(target.getX(), end.getMinBuildHeight() - 48.0D, target.getZ()),
                    target.getYRot());
        } else {
            double distance = GauntletConfig.BANISH_DISTANCE.get() * (cast.infinity() ? 2.0D : 1.0D);
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            BlockPos preferred = BlockPos.containing(target.getX() + Math.cos(angle) * distance, target.getY(),
                    target.getZ() + Math.sin(angle) * distance);
            Vec3 arrival = SpaceTravel.arrivalPosition(target, level, preferred);
            moved = SpaceTravel.teleport(target, level, arrival, target.getYRot());
        }
        if (!moved) {
            cast.message("space_arrival_failed");
            return;
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1.0D, from.z, 120, 0.4D, 0.8D, 0.4D, 0.4D);
        GauntletFx.burst(level, from.add(0.0D, 1.0D, 0.0D), "space", 40, 0.6D);
        GauntletFx.cast(player, "space", SoundEvents.ENDERMAN_TELEPORT, 0.5F);
        cast.message(cast.crouching() ? "space_banished_void" : "space_banished", target.getDisplayName());
        cast.cooldown();
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Map.Entry<UUID, FoldChain> entry : FOLD_CHAINS.entrySet()) {
            if (now <= entry.getValue().windowEnd()) continue;
            if (!FOLD_CHAINS.remove(entry.getKey(), entry.getValue())) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            GauntletAbility ability = GauntletAbilities.get("space", 0);
            if (player != null && ability != null) {
                GauntletAbilityController.addAbilityCooldown(player, "space", 0, ability.cooldownTicks(entry.getValue().infinity()));
            }
        }
        PULLING_RIFTS.entrySet().removeIf(entry -> now >= entry.getValue());
        for (UUID owner : PULLING_RIFTS.keySet()) {
            for (SpacePortalManager.Opening opening : SpacePortalManager.openings(owner)) {
                ServerLevel level = server.getLevel(opening.dimension());
                if (level != null) pull(level, opening.center(), 12.0D, 0.18D, owner);
            }
        }
        for (var iterator = SINGULARITIES.iterator(); iterator.hasNext();) {
            Singularity singularity = iterator.next();
            ServerLevel level = server.getLevel(singularity.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }
            if (now >= singularity.expiry()) {
                iterator.remove();
                collapse(server, level, singularity);
                continue;
            }
            tickSingularity(server, level, singularity, now);
        }
    }

    private static void tickSingularity(MinecraftServer server, ServerLevel level, Singularity singularity, long now) {
        Vec3 center = singularity.center();
        double radius = singularity.radius();
        pull(level, center, radius, 0.12D * singularity.power(), singularity.owner());
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(2.5D),
                entity -> !entity.getUUID().equals(singularity.owner()) && !GauntletAim.isProtectedEntity(entity))) {
            if (now % 10L == 0L) living.hurt(level.damageSources().magic(), (float) (4.0D * singularity.power()));
        }
        Player owner = server.getPlayerList().getPlayer(singularity.owner());
        if (GauntletConfig.griefing() && now % 4L == 0L) {
            int rips = singularity.infinity() ? 12 : 6;
            double ripRadius = radius * 0.5D;
            for (int index = 0; index < rips; index++) {
                BlockPos pos = BlockPos.containing(center.add((level.random.nextDouble() * 2.0D - 1.0D) * ripRadius,
                        (level.random.nextDouble() * 2.0D - 1.0D) * ripRadius, (level.random.nextDouble() * 2.0D - 1.0D) * ripRadius));
                if (!level.isLoaded(pos)) continue;
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.hasBlockEntity()) continue;
                if (!GauntletGrief.canEdit(owner instanceof ServerPlayer serverPlayer ? serverPlayer : null, level, pos)) continue;
                FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
                falling.dropItem = false;
                falling.setHurtsEntities(1.0F, 10);
            }
        }
        double core = 1.5D + Math.sin(now * 0.4D) * 0.3D;
        GauntletFx.sphere(level, center, core, "space", 24);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 40, radius * 0.3D, radius * 0.3D,
                radius * 0.3D, -0.6D);
        level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 6, 0.4D, 0.4D, 0.4D, 0.0D);
        if (now % 20L == 0L) GauntletFx.sound(level, center, SoundEvents.PORTAL_AMBIENT, 3.0F, 0.4F);
    }

    private static void pull(ServerLevel level, Vec3 center, double radius, double strength, UUID owner) {
        for (Entity entity : level.getEntities((Entity) null, new AABB(center, center).inflate(radius),
                entity -> pullable(entity) && !entity.getUUID().equals(owner) && !entity.isSpectator()
                        && !GauntletAim.isProtectedEntity(entity))) {
            Vec3 toward = center.subtract(entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D));
            double distance = toward.length();
            if (distance > radius || distance < 0.3D) continue;
            double force = strength * (1.2D - distance / radius);
            entity.setDeltaMovement(entity.getDeltaMovement().scale(0.85D).add(toward.scale(force / distance)));
            entity.resetFallDistance();
            entity.hurtMarked = true;
        }
    }

    private static boolean pullable(Entity entity) {
        return entity instanceof LivingEntity || entity instanceof net.minecraft.world.entity.item.ItemEntity
                || entity instanceof FallingBlockEntity || entity instanceof net.minecraft.world.entity.projectile.Projectile
                || entity instanceof net.minecraft.world.entity.ExperienceOrb
                || entity instanceof net.minecraft.world.entity.item.PrimedTnt;
    }

    private static void collapse(MinecraftServer server, ServerLevel level, Singularity singularity) {
        Vec3 center = singularity.center();
        Entity owner = server.getPlayerList().getPlayer(singularity.owner());
        float power = (float) Math.min(14.0D, 6.0D * singularity.power());
        level.explode(owner, center.x, center.y, center.z, power,
                GauntletConfig.griefing() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 6, 1.0D, 1.0D, 1.0D, 0.0D);
        GauntletFx.sphere(level, center, singularity.radius() * 0.4D, "space", 200);
        GauntletFx.shake(level, center, 3.0F, 30, (float) singularity.radius() * 3.0F);
    }

    public static void clear() {
        FOLD_CHAINS.clear();
        PULLING_RIFTS.clear();
        SINGULARITIES.clear();
    }

    private record FoldChain(int used, long windowEnd, boolean infinity) {
    }

    private record Singularity(ResourceKey<Level> dimension, Vec3 center, UUID owner, long expiry, double radius,
                               double power, boolean infinity) {
    }
}
