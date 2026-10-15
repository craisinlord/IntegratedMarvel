package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RealityClipboard {
    private static final int HISTORY = 5;
    private static final long ANCHOR_TICKS = 160L;
    private static final Map<UUID, Clip> CLIPS = new ConcurrentHashMap<>();
    private static final Map<UUID, Anchor> ANCHORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ROTATIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Deque<PasteRecord>> HISTORIES = new ConcurrentHashMap<>();

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
        if (aimed != null && !(aimed instanceof Player)) {
            CompoundTag tag = new CompoundTag();
            if (!aimed.saveAsPassenger(tag)) return;
            tag.remove("UUID");
            tag.remove("Passengers");
            CLIPS.put(player.getUUID(), new Clip(List.of(), new Vec3i(1, 1, 1), player.getDirection(), tag,
                    aimed.getType().getDescriptionId(), 1));
            ANCHORS.remove(player.getUUID());
            GauntletFx.burst(level, aimed.position().add(0.0D, aimed.getBbHeight() * 0.5D, 0.0D), "reality", 30, aimed.getBbWidth());
            GauntletFx.sound(level, aimed.position(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.0F, 1.0F);
            cast.message("reality_copied", aimed.getDisplayName());
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
        CLIPS.put(player.getUUID(), new Clip(List.of(new Piece(BlockPos.ZERO, state, blockData(level, pos))),
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
        int limit = GauntletConfig.REALITY_MAX_REGION_VOLUME.get() * (cast.infinity() ? 4 : 1);
        if (volume > limit) {
            cast.message("reality_region_too_large", volume, limit);
            return;
        }
        List<Piece> pieces = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(GauntletGrief.UNCOPYABLE)) continue;
            pieces.add(new Piece(pos.subtract(min), state, blockData(level, pos)));
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

    private static CompoundTag blockData(ServerLevel level, BlockPos pos) {
        if (!GauntletConfig.REALITY_BLOCK_ENTITY_CONTENTS.get()) return null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity == null ? null : blockEntity.saveWithFullMetadata(level.registryAccess());
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
            Vec3 at = hit.getType() == HitResult.Type.BLOCK
                    ? Vec3.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection()))
                    : player.getEyePosition().add(player.getLookAngle().scale(6.0D));
            Entity clone = EntityType.loadEntityRecursive(clip.entity().copy(), level, entity -> {
                entity.moveTo(at.x, at.y, at.z, player.getYRot() + 180.0F, 0.0F);
                return entity;
            });
            if (clone == null || !level.tryAddFreshEntityWithPassengers(clone)) {
                cast.message("reality_paste_blocked");
                return;
            }
            GauntletFx.burst(level, at.add(0.0D, clone.getBbHeight() * 0.5D, 0.0D), "reality", 40, clone.getBbWidth());
            GauntletFx.sound(level, at, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.0F);
            cast.message("reality_pasted", clone.getDisplayName());
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
        BlockPos origin = hit.getBlockPos().relative(hit.getDirection());
        BlockPos shift = rotatedMin(clip.size(), rotation);
        List<BlockEditQueue.Edit> edits = new ArrayList<>();
        for (Piece piece : clip.blocks()) {
            BlockPos pos = origin.offset(StructureTemplate.transform(piece.offset(), Mirror.NONE, rotation, BlockPos.ZERO).subtract(shift));
            if (!level.isInWorldBounds(pos)) continue;
            edits.add(new BlockEditQueue.Edit(pos.immutable(), piece.state().rotate(level, pos, rotation), piece.data()));
        }
        edits.sort(Comparator.comparingInt(edit -> edit.pos().getY()));
        if (edits.isEmpty()) {
            cast.message("reality_paste_blocked");
            return;
        }
        UUID owner = player.getUUID();
        BlockEditQueue.submit(level, edits, "reality", 18).guardedBy(player).onComplete(job -> finishPaste(level, job, owner));
        GauntletFx.cast(player, "reality", SoundEvents.ILLUSIONER_CAST_SPELL, 0.8F);
        cast.message("reality_region_pasting", edits.size());
    }

    private static void finishPaste(ServerLevel level, BlockEditQueue.Job job, UUID owner) {
        for (BlockEditQueue.Edit edit : job.undo()) {
            level.blockUpdated(edit.pos(), level.getBlockState(edit.pos()).getBlock());
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (job.applied() == 0 && job.denied() > 0) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.integrated_superheroes.gauntlet.reality_paste_blocked"), true);
            }
            return;
        }
        Deque<PasteRecord> history = HISTORIES.computeIfAbsent(owner, key -> new ArrayDeque<>());
        history.push(new PasteRecord(job.dimension(), List.copyOf(job.undo())));
        while (history.size() > HISTORY) history.removeLast();
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.gauntlet.reality_region_pasted",
                    job.applied()), true);
        }
    }

    private static void pasteSingle(GauntletCast cast, Piece piece, BlockHitResult hit, Rotation rotation) {
        ServerPlayer player = cast.player();
        ServerLevel level = cast.level();
        BlockPos target = hit.getBlockPos();
        if (!level.getBlockState(target).canBeReplaced()) target = target.relative(hit.getDirection());
        target = target.immutable();
        BlockState state = piece.state().rotate(level, target, rotation);
        if (!level.isInWorldBounds(target) || !level.getBlockState(target).canBeReplaced()
                || !GauntletGrief.canEdit(player, level, target)
                || !level.isUnobstructed(state, target, CollisionContext.of(player))) {
            cast.message("reality_paste_blocked");
            return;
        }
        UUID owner = player.getUUID();
        BlockEditQueue.submit(level, List.of(new BlockEditQueue.Edit(target, state, piece.data())), null, 3)
                .onComplete(job -> {
                    Deque<PasteRecord> history = HISTORIES.computeIfAbsent(owner, key -> new ArrayDeque<>());
                    history.push(new PasteRecord(job.dimension(), List.copyOf(job.undo())));
                    while (history.size() > HISTORY) history.removeLast();
                });
        GauntletFx.burst(level, Vec3.atCenterOf(target), "reality", 24, 0.4D);
        level.playSound(null, target, state.getSoundType(level, target, player).getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        cast.message("reality_pasted", state.getBlock().getName());
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
        BlockEditQueue.submit(level, restore, "reality", 18).onComplete(job -> {
            for (BlockEditQueue.Edit edit : job.undo()) {
                level.blockUpdated(edit.pos(), level.getBlockState(edit.pos()).getBlock());
            }
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

    public static void tick(long now) {
        ANCHORS.entrySet().removeIf(entry -> now > entry.getValue().expiry());
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
    }

    public record Clip(List<Piece> blocks, Vec3i size, Direction facing, CompoundTag entity, String label, int count) {
    }

    public record Piece(BlockPos offset, BlockState state, CompoundTag data) {
    }

    private record Anchor(ResourceKey<Level> dimension, BlockPos pos, long expiry) {
    }

    private record PasteRecord(ResourceKey<Level> dimension, List<BlockEditQueue.Edit> undo) {
    }
}
