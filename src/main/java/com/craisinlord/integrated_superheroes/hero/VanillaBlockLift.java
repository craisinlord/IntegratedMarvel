package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class VanillaBlockLift {
    private static final Map<UUID, Held> HELD = new HashMap<>();
    private static final Map<Integer, Thrown> THROWN = new HashMap<>();

    private VanillaBlockLift() {
    }

    private record Held(List<Integer> entityIds, List<Vec3> offsets, double distance, int ticksLeft, HeroBlockLift.Profile profile) {
    }

    private record Thrown(UUID owner, int ticksLeft, float damage, Set<Integer> hit) {
    }

    static boolean isLifting(ServerPlayer player) {
        return HELD.containsKey(player.getUUID());
    }

    static boolean lift(ServerPlayer player, HeroBlockLift.Profile profile) {
        HitResult hit = player.pick(profile.range(), 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockPos start = blockHit.getBlockPos();
        if (!HeroBlockLift.movable(level, player, start, profile) || !HeroBlockLift.simple(level, start)) {
            return false;
        }
        List<BlockPos> gathered = HeroBlockLift.gather(level, player, start, profile, true);
        Vec3 center = Vec3.atCenterOf(start);
        List<Integer> ids = new ArrayList<>();
        List<Vec3> offsets = new ArrayList<>();
        for (BlockPos pos : gathered) {
            BlockState state = level.getBlockState(pos);
            FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
            falling.setNoGravity(true);
            falling.time = 1;
            ids.add(falling.getId());
            offsets.add(Vec3.atCenterOf(pos).subtract(center));
        }
        double distance = Math.max(4.0D, Math.min(8.0D, player.getEyePosition().distanceTo(center))) + Math.sqrt(gathered.size()) * 0.5D;
        HELD.put(player.getUUID(), new Held(ids, offsets, distance, profile.holdTicks(), profile));
        HeroFx.play(player, profile.liftSound().get(), 1.0F, 0.9F);
        return true;
    }

    static void tick(ServerPlayer player) {
        Held held = HELD.get(player.getUUID());
        if (held == null) {
            return;
        }
        if (held.ticksLeft() <= 0) {
            drop(player);
            return;
        }
        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(held.distance())).subtract(0.0D, 0.5D, 0.0D);
        boolean any = false;
        for (int i = 0; i < held.entityIds().size(); i++) {
            if (!(player.level().getEntity(held.entityIds().get(i)) instanceof FallingBlockEntity falling) || falling.isRemoved()) {
                continue;
            }
            any = true;
            Vec3 pull = target.add(held.offsets().get(i)).subtract(falling.position()).scale(0.3D);
            if (pull.length() > 1.5D) {
                pull = pull.normalize().scale(1.5D);
            }
            falling.setDeltaMovement(pull);
            falling.hasImpulse = true;
            falling.time = 1;
        }
        if (!any) {
            HELD.remove(player.getUUID());
            return;
        }
        if (player.tickCount % 3 == 0) {
            held.profile().aura().accept(player.serverLevel(), target);
        }
        if (player.tickCount % 20 == 0) {
            HeroFx.play(player, held.profile().loopSound().get(), 0.6F, 0.8F);
        }
        HELD.put(player.getUUID(), new Held(held.entityIds(), held.offsets(), held.distance(), held.ticksLeft() - 1, held.profile()));
    }

    static boolean throwHeld(ServerPlayer player) {
        Held held = HELD.remove(player.getUUID());
        if (held == null) {
            return false;
        }
        HeroBlockLift.Profile profile = held.profile();
        float damage = (float) (profile.damage() * (1.0D + held.entityIds().size() / 16.0D));
        Set<Integer> hit = new HashSet<>();
        Vec3 velocity = player.getLookAngle().scale(profile.throwSpeed());
        boolean any = false;
        for (int id : held.entityIds()) {
            if (player.level().getEntity(id) instanceof FallingBlockEntity falling && !falling.isRemoved()) {
                falling.setNoGravity(false);
                falling.setDeltaMovement(velocity);
                falling.hasImpulse = true;
                falling.time = 1;
                THROWN.put(id, new Thrown(player.getUUID(), 100, damage, hit));
                any = true;
            }
        }
        if (any) {
            HeroFx.play(player, profile.throwSound().get(), 1.0F, 0.7F);
        }
        return any;
    }

    static void tickThrown(ServerLevel level) {
        if (THROWN.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Integer, Thrown>> iterator = THROWN.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Thrown> entry = iterator.next();
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null) {
                continue;
            }
            Thrown thrown = entry.getValue();
            if (!(entity instanceof FallingBlockEntity falling) || falling.isRemoved() || thrown.ticksLeft() <= 0) {
                iterator.remove();
                continue;
            }
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(thrown.owner());
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, falling.getBoundingBox().inflate(0.5D),
                    candidate -> candidate != owner && candidate.isAlive() && !thrown.hit().contains(candidate.getId()))) {
                thrown.hit().add(target.getId());
                HeroAdvancements.thrownBlock(owner, target);
                target.hurt(owner != null ? owner.damageSources().mobAttack(owner) : level.damageSources().fallingBlock(falling), thrown.damage());
            }
            if (falling.horizontalCollision) {
                HeroFx.play(falling, SoundEvents.ANVIL_LAND, 1.2F, 0.6F);
                falling.setDeltaMovement(0.0D, falling.getDeltaMovement().y, 0.0D);
                iterator.remove();
                continue;
            }
            falling.time = 1;
            entry.setValue(new Thrown(thrown.owner(), thrown.ticksLeft() - 1, thrown.damage(), thrown.hit()));
        }
    }

    static void drop(ServerPlayer player) {
        Held held = HELD.remove(player.getUUID());
        if (held == null) {
            return;
        }
        for (int id : held.entityIds()) {
            if (player.level().getEntity(id) instanceof FallingBlockEntity falling) {
                falling.setNoGravity(false);
            }
        }
    }
}
