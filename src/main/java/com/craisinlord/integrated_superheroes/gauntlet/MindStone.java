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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MindStone {
    private static final Map<UUID, MindState> STATES = new ConcurrentHashMap<>();

    private MindStone() {
    }

    private enum Mode { FRENZY, PACIFY }

    public static void command(GauntletCast cast) {
        ServerPlayer player = cast.player();
        LivingEntity target = GauntletAim.living(player, 48.0D);
        if (cast.infinity()) {
            List<LivingEntity> thralls = new ArrayList<>(GauntletAim.cone(player, 48.0D, 25.0D));
            if (target != null && !thralls.contains(target)) thralls.add(target);
            int controlled = 0;
            for (LivingEntity thrall : thralls) {
                if (GauntletAim.isProtectedEntity(thrall)) continue;
                if (MindControlManager.control(player, thrall, 1200L, controlled == 0)) {
                    controlled++;
                    GauntletFx.burst(cast.level(), thrall.getEyePosition(), "mind", 12, 0.3D);
                }
            }
            if (controlled == 0) {
                cast.message("mind_no_target");
                return;
            }
            GauntletFx.cast(player, "mind", SoundEvents.EVOKER_PREPARE_WOLOLO, 0.8F);
            cast.message("mind_mass_command", controlled);
            cast.cooldown();
            return;
        }
        if (target == null || !MindControlManager.control(player, target)) {
            cast.message("mind_no_target");
            return;
        }
        GauntletFx.burst(cast.level(), target.getEyePosition(), "mind", 20, 0.3D);
        GauntletFx.cast(player, "mind", SoundEvents.EVOKER_PREPARE_WOLOLO, 1.2F);
        cast.message("mind_command", target.getDisplayName());
        cast.cooldown();
    }

    public static void sight(GauntletCast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = cast.level();
        LivingEntity aimed = GauntletAim.living(player, 48.0D);
        double entityRange = cast.infinity() ? 96.0D : 48.0D;
        int oreRange = cast.infinity() ? 32 : 16;
        List<Integer> entities = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(entityRange),
                entity -> entity != player && entity.isAlive()).stream().map(Entity::getId).limit(512).toList();
        BlockPos origin = player.blockPosition();
        List<BlockPos> ores = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-oreRange, -oreRange, -oreRange), origin.offset(oreRange, oreRange, oreRange))) {
            if (level.isLoaded(pos) && level.getBlockState(pos).is(Tags.Blocks.ORES)) ores.add(pos.immutable());
        }
        ores.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
        List<Long> packed = ores.stream().limit(768).map(BlockPos::asLong).toList();
        int duration = cast.infinity() ? 600 : 300;
        PacketDistributor.sendToPlayer(player, new MindSightPayload(entities, packed, duration));
        GauntletFx.cast(player, cast.ability().stone(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F);
        if (aimed != null) {
            cast.message("mind_sight_target", aimed.getDisplayName(), String.format("%.1f", aimed.getHealth()),
                    String.format("%.1f", aimed.getMaxHealth()), aimed.getArmorValue());
        } else {
            cast.message("mind_sense", entities.size(), packed.size());
        }
        cast.cooldown();
    }

    public static void frenzy(GauntletCast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = cast.level();
        double radius = cast.infinity() ? 64.0D : 32.0D;
        Mode mode = cast.crouching() ? Mode.PACIFY : Mode.FRENZY;
        long duration = (mode == Mode.PACIFY ? 600L : 300L) * (cast.infinity() ? 2L : 1L);
        long expiry = player.server.overworld().getGameTime() + duration;
        int affected = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius), Entity::isAlive)) {
            if (SoulAllyManager.isAllyOf(player.getUUID(), mob)) continue;
            apply(mob, mode, expiry, player.getUUID());
            affected++;
        }
        GauntletFx.ring(level, player.position(), Math.min(radius, 24.0D), "mind");
        GauntletFx.cast(player, "mind", mode == Mode.PACIFY ? SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM : SoundEvents.RAVAGER_ROAR, 0.8F);
        cast.message(mode == Mode.PACIFY ? "mind_pacify" : "mind_frenzy", affected);
        cast.cooldown();
    }

    private static void apply(Mob mob, Mode mode, long expiry, UUID owner) {
        STATES.put(mob.getUUID(), new MindState(mode, expiry, mob.level().dimension(), owner));
        if (mode == Mode.PACIFY) {
            mob.setTarget(null);
        } else {
            mob.setTarget(null);
            retarget(mob, owner);
        }
    }

    public static void beamImpact(ServerPlayer player, Vec3 end, Set<UUID> affected) {
        long expiry = player.server.overworld().getGameTime() + 200L;
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, new AABB(end, end).inflate(3.5D), Entity::isAlive)) {
            if (affected.add(mob.getUUID())) apply(mob, Mode.FRENZY, expiry, player.getUUID());
        }
    }

    public static void onChangeTarget(LivingChangeTargetEvent event) {
        MindState state = STATES.get(event.getEntity().getUUID());
        if (state == null || event.getNewAboutToBeSetTarget() == null) return;
        if (state.mode() == Mode.PACIFY) {
            event.setCanceled(true);
        } else if (event.getNewAboutToBeSetTarget().getUUID().equals(state.owner())) {
            event.setCanceled(true);
        }
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (STATES.isEmpty() || now % 5L != 0L) return;
        for (Map.Entry<UUID, MindState> entry : Map.copyOf(STATES).entrySet()) {
            MindState state = entry.getValue();
            ServerLevel level = server.getLevel(state.dimension());
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            if (now >= state.expiry() || !(entity instanceof Mob mob) || !mob.isAlive()) {
                STATES.remove(entry.getKey());
                continue;
            }
            if (state.mode() == Mode.PACIFY) {
                if (mob.getTarget() != null) mob.setTarget(null);
                if (now % 20L == 0L) level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3D,
                        mob.getZ(), 1, 0.2D, 0.1D, 0.2D, 0.0D);
            } else {
                LivingEntity target = mob.getTarget();
                if (target == null || !target.isAlive() || target instanceof Player) retarget(mob, state.owner());
                if (now % 20L == 0L) level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3D,
                        mob.getZ(), 1, 0.2D, 0.1D, 0.2D, 0.0D);
            }
        }
    }

    private static void retarget(Mob mob, UUID owner) {
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(16.0D),
                entity -> entity != mob && entity.isAlive() && !(entity instanceof Player)
                        && !SoulAllyManager.isAllyOf(owner, entity))) {
            double distance = candidate.distanceToSqr(mob);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        if (nearest != null) mob.setTarget(nearest);
    }

    public static void clear() {
        STATES.clear();
    }

    private record MindState(Mode mode, long expiry, ResourceKey<Level> dimension, UUID owner) {
    }
}
