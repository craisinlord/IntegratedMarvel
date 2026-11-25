package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RealityClipboard {
    private static final int HISTORY = 5;
    private static final long ANCHOR_TICKS = 160L;
    /** Persistent-data key on a pasted mob: the overworld game time it fades at. */
    public static final String ECHO_TAG = "integrated_superheroes.reality_echo";
    /** Saved-data keys that carry items, trades, owners or riders. A pasted mob is only an echo of the original. */
    private static final List<String> ECHO_STRIP = List.of("UUID", "Passengers", "ArmorItems", "HandItems", "ArmorDropChances",
            "HandDropChances", "body_armor_item", "body_armor_drop_chance", "Items", "Inventory", "SaddleItem", "ArmorItem",
            "DecorItem", "ChestedHorse", "Offers", "Gossips", "Xp", "LastRestock", "RestocksToday", "Brain", "leash", "Leash",
            "Owner", "Tame", "Trusting", "ActiveEffects", "active_effects", "DeathLootTable", "DeathLootTableSeed",
            "PersistenceRequired", "CanPickUpLoot", "Saddle", "EggLayTime", "BeeData", "HasNectar", "FlowerPos", "HivePos");
    private static final Map<UUID, Clip> CLIPS = new ConcurrentHashMap<>();
    private static final Map<UUID, Anchor> ANCHORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ROTATIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Deque<PasteRecord>> HISTORIES = new ConcurrentHashMap<>();
    private static final Map<UUID, Echo> ECHOES = new ConcurrentHashMap<>();

    private RealityClipboard() {
    }

    public static BlockState singleBlock(UUID player) {
        Clip clip = CLIPS.get(player);
        if (clip == null || clip.entity() != null || clip.blocks().size() != 1 || !clip.size().equals(new Vec3i(1, 1, 1))) {
            return null;
        }
        BlockState state = clip.blocks().getFirst().state();
        return state.hasBlockEntity() ? null : state;
    }

    public static Clip clip(UUID player) {
        return CLIPS.get(player);
    }

    public static int rotation(UUID player) {
        return ROTATIONS.getOrDefault(player, 0);
    }

    public static void activate(GauntletCast cast) {
        if (cast.crouching()) {
            copy(cast);
        } else {
            paste(cast);
        }
    }

    private static void copy(GauntletCast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = cast.level();
        LivingEntity aimed = GauntletAim.living(player, 24.0D);
        if (aimed instanceof Mob mob) {
            if (mob.getType().is(ModTags.REALITY_WARP_IMMUNE)) {
                cast.message("reality_uncopyable_mob", mob.getDisplayName());
                return;
            }
            CompoundTag tag = new CompoundTag();
            if (!mob.saveAsPassenger(tag)) return;
            ECHO_STRIP.forEach(tag::remove);
            CLIPS.put(player.getUUID(), new Clip(List.of(), new Vec3i(1, 1, 1), player.getDirection(), tag,
                    mob.getType().getDescriptionId(), 1));
            ANCHORS.remove(player.getUUID());
            GauntletFx.burst(level, mob.position().add(0.0D, mob.getBbHeight() * 0.5D, 0.0D), "reality", 30, mob.getBbWidth());
            GauntletFx.sound(level, mob.position(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.0F, 1.0F);
            cast.message("reality_copied", mob.getDisplayName());
            return;
        }
        BlockHitResult hit = GauntletAim.block(player, 24.0D, ClipContext.Block.OUTLINE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            cast.message("reality_no_block");
            return;
        }
        BlockPos pos = hit.getBlockPos().immutable();
        long now = player.server.overworld().getGameTime();
        Anchor anchor = ANCHORS.remove(player.getUUID());
        if (anchor != null && anchor.dimension().equals(level.dimension()) && now <= anchor.expiry() && !anchor.pos().equals(pos)) {
            copyRegion(cast, anchor.pos(), pos);
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(GauntletGrief.UNCOPYABLE)) {
            cast.message("reality_uncopyable");
            return;
        }
        CLIPS.put(player.getUUID(), new Clip(List.of(new Piece(BlockPos.ZERO, state)),
                new Vec3i(1, 1, 1), player.getDirection(), null, state.getBlock().getDescriptionId(), 1));
        ANCHORS.put(player.getUUID(), new Anchor(level.dimension(), pos, now + ANCHOR_TICKS));
        GauntletFx.burst(level, Vec3.atCenterOf(pos), "reality", 24, 0.4D);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.6F);
        cast.message("reality_copied_anchor", state.getBlock().getName());
    }

    private static void copyRegion(GauntletCast cast, BlockPos first, BlockPos second) {
        ServerPlayer player = cast.player();
        ServerLevel level = cast.level();
        BlockPos min = BlockPos.min(first, second);
        BlockPos max = BlockPos.max(first, second);
        Vec3i size = new Vec3i(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1);
        long volume = (long) size.getX() * size.getY() * size.getZ();
        int limit = GauntletConfig.REALITY_MAX_REGION_VOLUME.get() * (cast.infinity() ? 2 : 1);
        if (volume > limit) {
            cast.message("reality_region_too_large", volume, limit);
            return;
        }
        List<Piece> pieces = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(GauntletGrief.UNCOPYABLE)) continue;
            pieces.add(new Piece(pos.subtract(min), state));
        }
        if (pieces.isEmpty()) {
            cast.message("reality_region_empty");
            return;
        }
        CLIPS.put(player.getUUID(), new Clip(List.copyOf(pieces), size, player.getDirection(), null, "region", pieces.size()));
        ROTATIONS.remove(player.getUUID());
        outline(level, min, max);
        GauntletFx.sound(level, Vec3.atCenterOf(second), SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.2F, 0.8F);
        cast.message("reality_region_copied", pieces.size(), size.getX(), size.getY(), size.getZ());
    }

    private static void outline(ServerLevel level, BlockPos min, BlockPos max) {
        Vec3 low = Vec3.atLowerCornerOf(min);
        Vec3 high = Vec3.atLowerCornerOf(max).add(1.0D, 1.0D, 1.0D);
        Vec3 span = high.subtract(low);
        int steps = (int) Math.min(64, Math.max(span.x, Math.max(span.y, span.z)) * 2);
        GauntletFx.box(level, GauntletFx.dust("reality", 1.0F), low, high, steps);
    }

    private static void paste(GauntletCast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = cast.level();
        Clip clip = CLIPS.get(player.getUUID());
        if (clip == null) {
            cast.message("reality_clipboard_empty");
            return;
        }
        BlockHitResult hit = GauntletAim.block(player, 24.0D, ClipContext.Block.OUTLINE);
        if (clip.entity() != null) {
            pasteEcho(cast, clip, hit);
            return;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            cast.message("reality_no_block");
            return;
        }
        Rotation rotation = rotationFor(clip.facing(), player.getDirection(), rotation(player.getUUID()));
        if (clip.blocks().size() == 1 && clip.size().equals(new Vec3i(1, 1, 1))) {
            pasteSingle(cast, clip.blocks().getFirst(), hit, rotation);
            return;
        }
        boolean free = RealityCost.free(cast);
        BlockPos origin = hit.getBlockPos().relative(hit.getDirection());
        BlockPos shift = rotatedMin(clip.size(), rotation);
        List<BlockEditQueue.Edit> edits = new ArrayList<>();
        for (Piece piece : clip.blocks()) {
            if (!free && !RealityCost.placeable(piece.state())) continue;
            BlockPos pos = origin.offset(StructureTemplate.transform(piece.offset(), Mirror.NONE, rotation, BlockPos.ZERO).subtract(shift));
            if (!level.isInWorldBounds(pos)) continue;
            edits.add(new BlockEditQueue.Edit(pos.immutable(), rotated(piece.state(), level, pos, rotation), null));
        }
        edits.sort(Comparator.comparingInt(edit -> edit.pos().getY()));
        if (edits.isEmpty()) {
            cast.message("reality_paste_blocked");
            return;
        }
        Map<Item, Integer> bill = free ? Map.of() : RealityCost.bill(edits.stream().map(BlockEditQueue.Edit::state).toList());
        if (!charge(cast, bill)) return;
        UUID owner = player.getUUID();
        Map<BlockPos, BlockState> placed = new HashMap<>();
        BlockEditQueue.submit(level, edits, "reality", 18).guardedBy(player)
                .onEdit((editLevel, pos, previous, replacement) -> placed.put(pos.immutable(), replacement))
                .onComplete(job -> finishPaste(level, job, owner, edits, placed, !free));
        GauntletFx.cast(player, "reality", SoundEvents.ILLUSIONER_CAST_SPELL, 0.8F);
        cast.message("reality_region_pasting", edits.size());
        cast.cooldown(Math.max(cast.ability().cooldownTicks(false), Math.max(20, edits.size() / 50)));
    }

    /** Takes the bill from the player, or tells them what's missing. Always true for a free bill. */
    private static boolean charge(GauntletCast cast, Map<Item, Integer> bill) {
        if (bill.isEmpty()) return true;
        Map<Item, Integer> missing = RealityCost.missing(cast.player(), bill);
        if (!missing.isEmpty()) {
            cast.message("reality_paste_missing", RealityCost.describe(missing));
            return false;
        }
        RealityCost.take(cast.player(), bill);
        return true;
    }

    private static void finishPaste(ServerLevel level, BlockEditQueue.Job job, UUID owner, List<BlockEditQueue.Edit> edits,
                                    Map<BlockPos, BlockState> placed, boolean paid) {
        for (BlockEditQueue.Edit edit : job.undo()) {
            level.blockUpdated(edit.pos(), level.getBlockState(edit.pos()).getBlock());
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (paid && player != null) {
            List<BlockState> unplaced = new ArrayList<>();
            for (BlockEditQueue.Edit edit : edits) {
                if (!placed.containsKey(edit.pos())) unplaced.add(edit.state());
            }
            RealityCost.refund(player, RealityCost.bill(unplaced));
        }
        if (job.applied() == 0 && job.denied() > 0) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.integrated_superheroes.gauntlet.reality_paste_blocked"), true);
            }
            return;
        }
        remember(owner, new PasteRecord(job.dimension(), List.copyOf(job.undo()), Map.copyOf(placed), paid));
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.gauntlet.reality_region_pasted",
                    job.applied()), true);
        }
    }

    private static void remember(UUID owner, PasteRecord record) {
        if (record.undo().isEmpty()) return;
        Deque<PasteRecord> history = HISTORIES.computeIfAbsent(owner, key -> new ArrayDeque<>());
        history.push(record);
        while (history.size() > HISTORY) history.removeLast();
    }

    private static BlockState rotated(BlockState state, ServerLevel level, BlockPos pos, Rotation rotation) {
        try {
            return state.rotate(level, pos, rotation);
        } catch (RuntimeException e) {
            return state;
        }
    }

    private static void pasteSingle(GauntletCast cast, Piece piece, BlockHitResult hit, Rotation rotation) {
        ServerPlayer player = cast.player();
        ServerLevel level = cast.level();
        BlockPos target = hit.getBlockPos();
        if (!level.getBlockState(target).canBeReplaced()) target = target.relative(hit.getDirection());
        target = target.immutable();
        BlockState state = rotated(piece.state(), level, target, rotation);
        boolean free = RealityCost.free(cast);
        if (!level.isInWorldBounds(target) || !level.getBlockState(target).canBeReplaced()
                || !GauntletGrief.canEdit(player, level, target)
                || !level.isUnobstructed(state, target, CollisionContext.of(player))
                || (!free && !RealityCost.placeable(state))) {
            cast.message("reality_paste_blocked");
            return;
        }
        Map<Item, Integer> bill = free ? Map.of() : RealityCost.bill(List.of(state));
        if (!charge(cast, bill)) return;
        UUID owner = player.getUUID();
        Map<BlockPos, BlockState> placed = new HashMap<>();
        BlockEditQueue.submit(level, List.of(new BlockEditQueue.Edit(target, state, null)), null, 3)
                .onEdit((editLevel, pos, previous, replacement) -> placed.put(pos.immutable(), replacement))
                .onComplete(job -> {
                    ServerPlayer online = level.getServer().getPlayerList().getPlayer(owner);
                    if (placed.isEmpty() && online != null) RealityCost.refund(online, bill);
                    remember(owner, new PasteRecord(job.dimension(), List.copyOf(job.undo()), Map.copyOf(placed), !free));
                });
        GauntletFx.burst(level, Vec3.atCenterOf(target), "reality", 24, 0.4D);
        level.playSound(null, target, state.getSoundType(level, target, player).getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        cast.message("reality_pasted", state.getBlock().getName());
    }

    private static void pasteEcho(GauntletCast cast, Clip clip, BlockHitResult hit) {
        ServerPlayer player = cast.player();
        ServerLevel level = cast.level();
        Vec3 at = hit.getType() == HitResult.Type.BLOCK
                ? Vec3.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection()))
                : player.getEyePosition().add(player.getLookAngle().scale(6.0D));
        Entity clone = EntityType.loadEntityRecursive(clip.entity().copy(), level, entity -> {
            entity.moveTo(at.x, at.y, at.z, player.getYRot() + 180.0F, 0.0F);
            return entity;
        });
        if (!(clone instanceof Mob mob)) {
            if (clone != null) clone.discard();
            cast.message("reality_paste_blocked");
            return;
        }
        long expiry = player.server.overworld().getGameTime() + GauntletConfig.REALITY_ECHO_TICKS.get();
        mob.getPersistentData().putLong(ECHO_TAG, expiry);
        mob.setCanPickUpLoot(false);
        if (!level.tryAddFreshEntityWithPassengers(mob)) {
            cast.message("reality_paste_blocked");
            return;
        }
        ECHOES.put(mob.getUUID(), new Echo(level.dimension(), expiry));
        GauntletFx.burst(level, at.add(0.0D, mob.getBbHeight() * 0.5D, 0.0D), "reality", 40, mob.getBbWidth());
        GauntletFx.sound(level, at, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.0F);
        cast.message("reality_pasted_echo", mob.getDisplayName(), GauntletConfig.REALITY_ECHO_TICKS.get() / 20);
    }

    public static boolean isEcho(Entity entity) {
        return entity.getPersistentData().contains(ECHO_TAG);
    }

    /** Re-tracks echoes loaded from disk so they still fade after a restart or chunk reload. */
    public static void onEchoJoin(Entity entity) {
        if (entity.level() instanceof ServerLevel level && isEcho(entity)) {
            ECHOES.put(entity.getUUID(), new Echo(level.dimension(), entity.getPersistentData().getLong(ECHO_TAG)));
        }
    }

    private static void tickEchoes(MinecraftServer server, long now) {
        if (ECHOES.isEmpty() || now % 10 != 0) return;
        for (var iterator = ECHOES.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<UUID, Echo> entry = iterator.next();
            Echo echo = entry.getValue();
            ServerLevel level = server.getLevel(echo.dimension());
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            if (entity == null || !entity.isAlive()) {
                // Unloaded or dead: drop the handle, the join hook picks it back up if it loads again.
                iterator.remove();
                continue;
            }
            if (now < echo.expiry()) continue;
            iterator.remove();
            Vec3 at = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            GauntletFx.burst(level, at, "reality", 30, entity.getBbWidth());
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 12, 0.3D, 0.3D, 0.3D, 0.02D);
            GauntletFx.sound(level, at, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 1.4F);
            entity.ejectPassengers();
            entity.stopRiding();
            entity.discard();
        }
    }

    public static void scroll(GauntletCast cast, double delta) {
        ServerPlayer player = cast.player();
        if (delta > 0.0D) {
            int next = Math.floorMod(rotation(player.getUUID()) + 1, 4);
            ROTATIONS.put(player.getUUID(), next);
            cast.message("reality_rotation", next * 90);
            return;
        }
        Deque<PasteRecord> history = HISTORIES.get(player.getUUID());
        PasteRecord last = history == null ? null : history.poll();
        if (last == null) {
            cast.message("reality_nothing_to_undo");
            return;
        }
        ServerLevel level = player.server.getLevel(last.dimension());
        if (level == null) return;
        List<BlockEditQueue.Edit> restore = new ArrayList<>(last.undo());
        java.util.Collections.reverse(restore);
        UUID owner = player.getUUID();
        List<BlockState> returned = new ArrayList<>();
        BlockEditQueue.submit(level, restore, "reality", 18)
                .onEdit((editLevel, pos, previous, replacement) -> {
                    // Only blocks still standing as pasted come back as items; anything already mined dropped normally.
                    if (last.paid() && previous == last.placed().get(pos)) returned.add(previous);
                })
                .onComplete(job -> {
                    for (BlockEditQueue.Edit edit : job.undo()) {
                        level.blockUpdated(edit.pos(), level.getBlockState(edit.pos()).getBlock());
                    }
                    ServerPlayer online = level.getServer().getPlayerList().getPlayer(owner);
                    if (online != null && !returned.isEmpty()) RealityCost.refund(online, RealityCost.bill(returned));
                });
        GauntletFx.sound(level, player.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 0.5F);
        cast.message("reality_undo", restore.size());
    }

    public static Rotation rotationFor(Direction copied, Direction current, int extra) {
        int copiedIndex = copied.getAxis().isHorizontal() ? copied.get2DDataValue() : 0;
        int currentIndex = current.getAxis().isHorizontal() ? current.get2DDataValue() : 0;
        return Rotation.values()[Math.floorMod(currentIndex - copiedIndex + extra, 4)];
    }

    public static BlockPos rotatedMin(Vec3i size, Rotation rotation) {
        BlockPos corner = StructureTemplate.transform(new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1),
                Mirror.NONE, rotation, BlockPos.ZERO);
        return BlockPos.min(BlockPos.ZERO, corner);
    }

    public static Vec3i rotatedSize(Vec3i size, Rotation rotation) {
        boolean swap = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
        return swap ? new Vec3i(size.getZ(), size.getY(), size.getX()) : size;
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        ANCHORS.entrySet().removeIf(entry -> now > entry.getValue().expiry());
        tickEchoes(server, now);
    }

    public static void clearPlayer(UUID player) {
        CLIPS.remove(player);
        ANCHORS.remove(player);
        ROTATIONS.remove(player);
        HISTORIES.remove(player);
    }

    public static void clear() {
        CLIPS.clear();
        ANCHORS.clear();
        ROTATIONS.clear();
        HISTORIES.clear();
        ECHOES.clear();
    }

    public record Clip(List<Piece> blocks, Vec3i size, Direction facing, CompoundTag entity, String label, int count) {
    }

    public record Piece(BlockPos offset, BlockState state) {
    }

    private record Anchor(ResourceKey<Level> dimension, BlockPos pos, long expiry) {
    }

    private record PasteRecord(ResourceKey<Level> dimension, List<BlockEditQueue.Edit> undo, Map<BlockPos, BlockState> placed,
                               boolean paid) {
    }

    private record Echo(ResourceKey<Level> dimension, long expiry) {
    }
}
