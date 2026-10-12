package com.craisinlord.integrated_marvel.hero;

import com.github.alexmodguy.alexscaves.server.entity.item.MovingMetalBlockEntity;
import com.github.alexmodguy.alexscaves.server.entity.util.MovingBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class HeroBlockLift {
    private static final TagKey<Block> UNMOVABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("alexscaves", "unmovable"));
    private static final Map<UUID, Held> HELD = new HashMap<>();
    private static final Map<Integer, Thrown> THROWN = new HashMap<>();

    private HeroBlockLift() {
    }

    public record Profile(double range, int maxBlocks, int holdTicks, double throwSpeed, double damageMultiplier,
                          Predicate<BlockState> filter, BiConsumer<ServerLevel, Vec3> aura,
                          Supplier<SoundEvent> liftSound, Supplier<SoundEvent> loopSound, Supplier<SoundEvent> throwSound) {
    }

    private record Held(int entityId, double distance, int ticksLeft, int blocks, Profile profile) {
    }

    private record Thrown(UUID owner, int ticksLeft, float damage, Set<Integer> hit) {
    }

    public static boolean isLifting(ServerPlayer player) {
        return HELD.containsKey(player.getUUID());
    }

    public static boolean lift(ServerPlayer player, Profile profile) {
        HitResult hit = player.pick(profile.range(), 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockPos start = lowerHalf(level, blockHit.getBlockPos());
        if (!movable(level, player, start, profile)) {
            return false;
        }
        List<BlockPos> gathered = gather(level, player, start, profile);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("alexscaves", "moving_metal_block"));
        if (!(type.create(level) instanceof MovingMetalBlockEntity moving)) {
            return false;
        }
        List<MovingBlockData> data = new ArrayList<>();
        for (BlockPos pos : gathered) {
            BlockState state = level.getBlockState(pos);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            data.add(new MovingBlockData(state, state.getShape(level, pos), pos.subtract(start),
                    blockEntity == null ? null : blockEntity.saveWithoutMetadata(level.registryAccess())));
            level.removeBlockEntity(pos);
        }
        gathered.sort((first, second) -> Integer.compare(second.getY(), first.getY()));
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos pos : gathered) {
            level.setBlock(pos, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        for (BlockPos pos : gathered) {
            air.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, Blocks.AIR);
        }
        moving.moveTo(Vec3.atCenterOf(start));
        moving.setAllBlockData(MovingMetalBlockEntity.createTagFromData(data));
        moving.setPlacementCooldown(10);
        level.addFreshEntity(moving);
        double distance = Math.max(4.0D, Math.min(8.0D, player.getEyePosition().distanceTo(Vec3.atCenterOf(start)))) + Math.sqrt(gathered.size()) * 0.5D;
        HELD.put(player.getUUID(), new Held(moving.getId(), distance, profile.holdTicks(), gathered.size(), profile));
        HeroFx.play(player, profile.liftSound().get(), 1.0F, 0.9F);
        return true;
    }

    private static BlockPos lowerHalf(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    private static boolean movable(ServerLevel level, ServerPlayer player, BlockPos pos, Profile profile) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && profile.filter().test(state) && !state.is(UNMOVABLE) && state.getDestroySpeed(level, pos) >= 0.0F
                && level.mayInteract(player, pos);
    }

    private static List<BlockPos> gather(ServerLevel level, ServerPlayer player, BlockPos start, Profile profile) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && result.size() < profile.maxBlocks()) {
            BlockPos pos = queue.poll();
            result.add(pos);
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (seen.add(next) && movable(level, player, next, profile)) {
                    queue.add(next);
                }
            }
        }
        List<BlockPos> partners = new ArrayList<>();
        for (BlockPos pos : result) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
                BlockPos upper = pos.above();
                if (!result.contains(upper) && level.getBlockState(upper).is(state.getBlock())) {
                    partners.add(upper);
                }
            }
        }
        result.addAll(partners);
        return result;
    }

    public static void tick(ServerPlayer player) {
        Held held = HELD.get(player.getUUID());
        if (held == null) {
            return;
        }
        Entity entity = player.level().getEntity(held.entityId());
        if (!(entity instanceof MovingMetalBlockEntity moving) || moving.isRemoved() || held.ticksLeft() <= 0) {
            HELD.remove(player.getUUID());
            return;
        }
        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(held.distance())).subtract(0.0D, 0.5D, 0.0D);
        Vec3 pull = target.subtract(moving.position()).scale(0.3D);
        if (pull.length() > 1.5D) {
            pull = pull.normalize().scale(1.5D);
        }
        moving.setDeltaMovement(pull);
        moving.hasImpulse = true;
        moving.setPlacementCooldown(10);
        if (player.tickCount % 3 == 0) {
            held.profile().aura().accept(player.serverLevel(), moving.position());
        }
        if (player.tickCount % 20 == 0) {
            HeroFx.play(player, held.profile().loopSound().get(), 0.6F, 0.8F);
        }
        HELD.put(player.getUUID(), new Held(held.entityId(), held.distance(), held.ticksLeft() - 1, held.blocks(), held.profile()));
    }

    public static boolean throwHeld(ServerPlayer player) {
        Held held = HELD.remove(player.getUUID());
        if (held == null) {
            return false;
        }
        Entity entity = player.level().getEntity(held.entityId());
        if (!(entity instanceof MovingMetalBlockEntity moving) || moving.isRemoved()) {
            return false;
        }
        Profile profile = held.profile();
        moving.setDeltaMovement(player.getLookAngle().scale(profile.throwSpeed()));
        moving.hasImpulse = true;
        moving.setPlacementCooldown(10);
        float damage = (float) (HeroPowers.powerDamage(player) * profile.damageMultiplier() * (1.0D + held.blocks() / 16.0D));
        THROWN.put(moving.getId(), new Thrown(player.getUUID(), 100, damage, new HashSet<>()));
        HeroFx.play(player, profile.throwSound().get(), 1.0F, 0.7F);
        return true;
    }

    public static void tickThrown(ServerLevel level) {
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
            if (!(entity instanceof MovingMetalBlockEntity moving) || moving.isRemoved()) {
                iterator.remove();
                continue;
            }
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(thrown.owner());
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, moving.getBoundingBox().inflate(0.5D),
                    candidate -> candidate != owner && candidate.isAlive() && !thrown.hit().contains(candidate.getId()))) {
                thrown.hit().add(target.getId());
                target.hurt(owner != null ? owner.damageSources().mobAttack(owner) : level.damageSources().fallingBlock(moving), thrown.damage());
            }
            if (moving.horizontalCollision || moving.verticalCollision || thrown.ticksLeft() <= 0) {
                moving.setDeltaMovement(Vec3.ZERO);
                moving.setPlacementCooldown(1);
                HeroFx.play(moving, SoundEvents.ANVIL_LAND, 1.2F, 0.6F);
                iterator.remove();
                continue;
            }
            Vec3 motion = moving.getDeltaMovement();
            moving.setDeltaMovement(motion.x, motion.y - 0.05D, motion.z);
            moving.hasImpulse = true;
            moving.setPlacementCooldown(10);
            entry.setValue(new Thrown(thrown.owner(), thrown.ticksLeft() - 1, thrown.damage(), thrown.hit()));
        }
    }

    public static void drop(ServerPlayer player) {
        HELD.remove(player.getUUID());
    }
}
