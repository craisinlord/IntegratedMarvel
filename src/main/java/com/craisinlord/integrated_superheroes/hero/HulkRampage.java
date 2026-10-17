package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteAnimations;
import com.craisinlord.integrated_superheroes.gauntlet.GauntletGrief;
import com.craisinlord.integrated_superheroes.mixin.FallingBlockEntityInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tintankgames.marvel.world.level.MarvelGameRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID)
public final class HulkRampage {
    public static final int CALM = 0;
    public static final int ANGRY = 1;
    public static final int FURIOUS = 2;
    public static final int UNSTOPPABLE = 3;
    private static final double[] TUBE = {0.0D, 0.5D, 0.75D, 1.0D};
    private static final int MAX_PENDING = 20000;
    private static final List<Pending> PENDING = new ArrayList<>();
    private static final Map<UUID, Long> LAST_CRUNCH = new HashMap<>();
    private static final Map<UUID, Long> LAST_SWING = new HashMap<>();
    private static long clock;

    private HulkRampage() {
    }

    private record Pending(ServerLevel level, ServerPlayer player, BlockPos pos, long due, int tier) {
    }

    private record Key(ServerPlayer player, int tier) {
    }

    private static final class Pass {
        private final ServerLevel level;
        private final ServerPlayer player;
        private final int tier;
        private final float cap;
        private final float drop;
        private final boolean solidOnly;
        private final List<BlockPos> broken = new ArrayList<>();
        private int budget;

        private Pass(ServerLevel level, ServerPlayer player, int tier, boolean solidOnly) {
            this.level = level;
            this.player = player;
            this.tier = tier;
            this.cap = cap(tier);
            this.drop = MarvelBalanceConfig.HULK_RAMPAGE_DROP_CHANCE.get().floatValue();
            this.solidOnly = solidOnly;
            this.budget = MarvelBalanceConfig.HULK_RAMPAGE_BLOCKS_PER_TICK.get();
        }
    }

    public static int tier(ServerPlayer player) {
        if (HulkPower.unstoppable(player)) {
            return UNSTOPPABLE;
        }
        float rage = HulkPower.rage(player);
        if (rage >= MarvelBalanceConfig.HULK_RAMPAGE_FURIOUS_RAGE.get()) {
            return FURIOUS;
        }
        return rage >= MarvelBalanceConfig.HULK_RAMPAGE_ANGRY_RAGE.get() ? ANGRY : CALM;
    }

    private static float cap(int tier) {
        return switch (tier) {
            case UNSTOPPABLE -> MarvelBalanceConfig.HULK_RAMPAGE_UNSTOPPABLE_HARDNESS.get().floatValue();
            case FURIOUS -> MarvelBalanceConfig.HULK_RAMPAGE_FURIOUS_HARDNESS.get().floatValue();
            case ANGRY -> MarvelBalanceConfig.HULK_RAMPAGE_ANGRY_HARDNESS.get().floatValue();
            default -> MarvelBalanceConfig.HULK_RAMPAGE_CALM_HARDNESS.get().floatValue();
        };
    }

    private static int impactTier(ServerPlayer player) {
        return Math.min(UNSTOPPABLE, tier(player) + 1);
    }

    private static boolean allowed(ServerPlayer player) {
        return MarvelBalanceConfig.HULK_RAMPAGE.get() && HulkPower.isHulk(player) && HeroToggles.isOn(player, HulkPower.RAMPAGE)
                && !player.isSpectator() && player.mayBuild()
                && player.level().getGameRules().getBoolean(MarvelGameRules.RULE_SUPERPOWERGRIEFING);
    }

    private static boolean smashable(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES)
                || state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_TRAPDOORS);
    }

    private static boolean breakable(Pass pass, BlockPos pos, BlockState state) {
        ServerLevel level = pass.level;
        if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) {
            return false;
        }
        if (state.isAir() || !state.getFluidState().isEmpty() || state.is(GauntletGrief.IMMUNE) || level.getBlockEntity(pos) != null) {
            return false;
        }
        if (pass.solidOnly && state.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness > pass.cap && !(pass.tier == CALM && smashable(state))) {
            return false;
        }
        if (!pass.player.mayInteract(level, pos)) {
            return false;
        }
        return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, pass.player)).isCanceled();
    }

    private static boolean smash(Pass pass, BlockPos pos) {
        if (pass.budget <= 0) {
            return false;
        }
        BlockState state = pass.level.getBlockState(pos);
        if (!breakable(pass, pos, state)) {
            return false;
        }
        pass.level.destroyBlock(pos, pass.level.random.nextFloat() < pass.drop, pass.player);
        pass.broken.add(pos.immutable());
        pass.budget--;
        return true;
    }

    private static void finish(Pass pass) {
        if (!pass.broken.isEmpty()) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(pass.player, new HulkRubblePayload(pass.broken, pass.tier));
        }
    }

    private static void sweep(Pass pass, AABB box, int floor) {
        int minY = Math.max(Mth.floor(box.minY), floor);
        BlockPos min = new BlockPos(Mth.floor(box.minX), minY, Mth.floor(box.minZ));
        BlockPos max = new BlockPos(Mth.floor(box.maxX - 1.0E-4D), Mth.floor(box.maxY - 1.0E-4D), Mth.floor(box.maxZ - 1.0E-4D));
        if (max.getY() < min.getY()) {
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            smash(pass, pos);
        }
    }

    public static void tickCharge(ServerPlayer player) {
        if (!allowed(player) || player.isPassenger() || player.isFallFlying()) {
            return;
        }
        boolean charging = player.isSprinting();
        if (!charging && !player.horizontalCollision) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0D, look.z);
        if (direction.lengthSqr() < 1.0E-4D) {
            return;
        }
        direction = direction.normalize();
        int tier = tier(player);
        double side = TUBE[tier];
        AABB box = player.getBoundingBox().expandTowards(direction.scale(charging ? 1.4D : 0.6D))
                .inflate(side, 0.0D, side).expandTowards(0.0D, side, 0.0D);
        Pass pass = new Pass(player.serverLevel(), player, tier, true);
        sweep(pass, box, Mth.floor(player.getY() + 0.01D));
        if (pass.broken.isEmpty()) {
            return;
        }
        crunch(player, pass, charging);
        finish(pass);
    }

    private static void crunch(ServerPlayer player, Pass pass, boolean charging) {
        long now = player.level().getGameTime();
        Long last = LAST_CRUNCH.get(player.getUUID());
        if (last == null || now - last >= 4) {
            LAST_CRUNCH.put(player.getUUID(), now);
            ServerLevel level = pass.level;
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.PLAYERS,
                    1.0F + pass.tier * 0.2F, 0.55F + level.random.nextFloat() * 0.15F);
            if (pass.tier >= FURIOUS) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                        0.6F, 0.7F + level.random.nextFloat() * 0.2F);
            }
            HeroFx.shake(level, player.position(), 12.0D, 0.15F + pass.tier * 0.1F, 5);
        }
        Long swung = LAST_SWING.get(player.getUUID());
        if (charging && pass.broken.size() >= 3 && (swung == null || now - swung >= 15)) {
            LAST_SWING.put(player.getUUID(), now);
            ViltrumiteAnimations.play(player, ViltrumiteAnimations.PUNCH);
        }
    }

    public static void tickLeap(ServerPlayer player) {
        if (!allowed(player)) {
            return;
        }
        Vec3 motion = player.position().subtract(player.xo, player.yo, player.zo);
        if (motion.lengthSqr() < 0.01D) {
            return;
        }
        int tier = impactTier(player);
        AABB box = player.getBoundingBox().expandTowards(motion.scale(1.5D)).inflate(TUBE[tier] * 0.5D);
        Pass pass = new Pass(player.serverLevel(), player, tier, true);
        sweep(pass, box, Mth.floor(player.getY() + 0.01D));
        if (pass.broken.isEmpty()) {
            return;
        }
        crunch(player, pass, false);
        finish(pass);
    }

    public static void crater(ServerPlayer player, double drop) {
        if (!allowed(player) || drop < 3.0D) {
            return;
        }
        int tier = impactTier(player);
        double radius = Math.min(MarvelBalanceConfig.HULK_CRATER_MAX_RADIUS.get(), 1.5D + drop / 12.0D + tier * 0.75D);
        if (radius < 1.0D) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos center = BlockPos.containing(player.getX(), player.getY() - 0.5D, player.getZ());
        int reach = Mth.ceil(radius);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) {
                    continue;
                }
                int depth = Math.max(1, (int) Math.round((1.0D - distance * distance / (radius * radius)) * radius * 0.6D));
                for (int dy = 0; dy < depth; dy++) {
                    queue(level, player, center.offset(dx, -dy, dz), (long) distance, tier);
                }
            }
        }
    }

    public static void trenches(ServerPlayer player, int directions, int length) {
        if (!allowed(player)) {
            return;
        }
        int base = tier(player);
        if (base < FURIOUS) {
            return;
        }
        int tier = impactTier(player);
        int depth = base >= UNSTOPPABLE ? 2 : 1;
        int width = base >= UNSTOPPABLE ? 1 : 0;
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < directions; i++) {
            double angle = Math.PI * 2.0D * i / directions;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            for (int step = 1; step <= length; step++) {
                double x = player.getX() + dx * step;
                double z = player.getZ() + dz * step;
                for (int w = -width; w <= width; w++) {
                    BlockPos ground = surface(level, BlockPos.containing(x - dz * w, player.getY(), z + dx * w));
                    if (ground == null) {
                        continue;
                    }
                    for (int d = 0; d < depth; d++) {
                        queue(level, player, ground.below(d), step, tier);
                    }
                }
            }
        }
    }

    public static void fling(ServerPlayer player, double range) {
        if (!allowed(player) || tier(player) < FURIOUS) {
            return;
        }
        int limit = MarvelBalanceConfig.HULK_THUNDERCLAP_FLUNG_BLOCKS.get();
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);
        if (limit <= 0 || forward.lengthSqr() < 1.0E-4D) {
            return;
        }
        forward = forward.normalize();
        ServerLevel level = player.serverLevel();
        Pass pass = new Pass(level, player, impactTier(player), true);
        double reach = range * 0.6D;
        int span = Mth.ceil(reach);
        BlockPos origin = player.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -span; dx <= span; dx++) {
            for (int dz = -span; dz <= span; dz++) {
                Vec3 offset = new Vec3(origin.getX() + dx + 0.5D - player.getX(), 0.0D, origin.getZ() + dz + 0.5D - player.getZ());
                double distance = offset.length();
                if (distance < 1.5D || distance > reach || offset.scale(1.0D / distance).dot(forward) < 0.64D) {
                    continue;
                }
                BlockPos ground = surface(level, origin.offset(dx, 0, dz));
                if (ground != null) {
                    candidates.add(ground);
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(player.position())));
        for (BlockPos pos : candidates) {
            if (pass.broken.size() >= limit) {
                break;
            }
            BlockState state = level.getBlockState(pos);
            if (!breakable(pass, pos, state)) {
                continue;
            }
            BlockState stripped = state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, false) : state;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            FallingBlockEntity falling = FallingBlockEntityInvoker.integratedSuperheroes$create(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, stripped);
            falling.time = 1;
            falling.dropItem = false;
            falling.setHurtsEntities(2.0F, 20);
            double push = 0.5D + level.random.nextDouble() * 0.5D;
            falling.setDeltaMovement(forward.x * push, 0.55D + level.random.nextDouble() * 0.4D, forward.z * push);
            falling.hurtMarked = true;
            level.addFreshEntity(falling);
            pass.broken.add(pos);
        }
        finish(pass);
    }

    private static BlockPos surface(ServerLevel level, BlockPos column) {
        for (int dy = 1; dy >= -3; dy--) {
            BlockPos pos = column.offset(0, dy - 1, 0);
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty() && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    private static void queue(ServerLevel level, ServerPlayer player, BlockPos pos, long delay, int tier) {
        if (PENDING.size() < MAX_PENDING) {
            PENDING.add(new Pending(level, player, pos.immutable(), clock + delay, tier));
        }
    }

    public static void tickServer() {
        clock++;
        if (PENDING.isEmpty()) {
            return;
        }
        Map<Key, Pass> passes = new LinkedHashMap<>();
        Iterator<Pending> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            if (pending.due() > clock) {
                continue;
            }
            ServerPlayer player = pending.player();
            if (player.isRemoved() || player.level() != pending.level() || !allowed(player)) {
                iterator.remove();
                continue;
            }
            Pass pass = passes.computeIfAbsent(new Key(player, pending.tier()), key -> new Pass(pending.level(), player, pending.tier(), false));
            if (pass.budget <= 0) {
                continue;
            }
            iterator.remove();
            smash(pass, pending.pos());
        }
        passes.values().forEach(HulkRampage::finish);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer tracker && event.getTarget() instanceof ServerPlayer target) {
            HulkPower.syncRageTo(tracker, target);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        PENDING.removeIf(pending -> pending.player().getUUID().equals(id));
        LAST_CRUNCH.remove(id);
        LAST_SWING.remove(id);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
        LAST_CRUNCH.clear();
        LAST_SWING.clear();
    }
}
