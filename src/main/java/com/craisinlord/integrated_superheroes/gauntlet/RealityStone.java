package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.anthology.effect.AnthologyEffects;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RealityStone {
    private static final double MIN_SCALE = 0.01D;
    private static final double MAX_SCALE = 100.0D;
    private static final int MAX_TEMPORARY_BLOCKS = 65536;
    private static final ResourceLocation REACH_ID = IntegratedSuperheroes.id("reality_reach");
    private static final List<BlockState> CYCLE = List.of(Blocks.STONE.defaultBlockState(),
            Blocks.AMETHYST_BLOCK.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState(),
            Blocks.DIAMOND_BLOCK.defaultBlockState(), Blocks.EMERALD_BLOCK.defaultBlockState(),
            Blocks.OBSIDIAN.defaultBlockState(), Blocks.CRYING_OBSIDIAN.defaultBlockState(),
            Blocks.REDSTONE_BLOCK.defaultBlockState(), Blocks.SLIME_BLOCK.defaultBlockState(),
            Blocks.BEDROCK.defaultBlockState());
    private static final List<EntityType<?>> CREATURES = List.of(EntityType.CHICKEN, EntityType.RABBIT, EntityType.FROG,
            EntityType.PIG, EntityType.SHEEP, EntityType.PARROT, EntityType.AXOLOTL, EntityType.ALLAY, EntityType.BAT);
    private static final Map<TemporaryBlockKey, TemporaryBlock> TEMPORARY_BLOCKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> RADII = new ConcurrentHashMap<>();
    private static final PriorityQueue<Expiry> EXPIRIES = new PriorityQueue<>(Comparator.comparingLong(Expiry::expiry));

    private RealityStone() {
    }

    public static int radius(UUID player) {
        return RADII.getOrDefault(player, 0);
    }

    public static void transmuteRadius(GauntletCast cast, double delta) {
        int max = cast.infinity() ? 16 : 8;
        int next = Math.clamp(radius(cast.player().getUUID()) + (int) delta, 0, max);
        RADII.put(cast.player().getUUID(), next);
        cast.message("reality_radius", next);
    }

    public static void transmute(GauntletCast cast) {
        ServerPlayer player = cast.player();
        LivingEntity target = GauntletAim.living(player, 32.0D);
        if (target != null && !(target instanceof Player)) {
            transmuteEntity(cast, target);
            return;
        }
        BlockHitResult hit = GauntletAim.block(player, 32.0D, ClipContext.Block.OUTLINE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            cast.message("reality_no_block");
            return;
        }
        ServerLevel level = cast.level();
        BlockPos center = hit.getBlockPos().immutable();
        int radius = radius(player.getUUID());
        BlockState clipboard = RealityClipboard.singleBlock(player.getUUID());
        boolean permanent = cast.crouching();
        List<BlockEditQueue.Edit> edits = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (pos.distSqr(center) > radius * radius + 0.5D) continue;
            BlockState current = level.getBlockState(pos);
            if (current.isAir() || current.hasBlockEntity()) continue;
            BlockState replacement = clipboard != null ? clipboard : nextInCycle(level, pos, current);
            if (replacement == current) continue;
            edits.add(new BlockEditQueue.Edit(pos.immutable(), replacement, null));
        }
        if (edits.isEmpty()) {
            cast.message("reality_protected");
            return;
        }
        if (!permanent && TEMPORARY_BLOCKS.size() + edits.size() > MAX_TEMPORARY_BLOCKS) {
            cast.message("reality_too_many");
            return;
        }
        long expiry = level.getGameTime() + 200L;
        BlockEditQueue.submit(level, edits, "reality", 3).withoutUndo().guardedBy(player).onEdit((editLevel, pos, previous, replacement) -> {
            TemporaryBlockKey key = new TemporaryBlockKey(editLevel.dimension(), pos.immutable());
            TemporaryBlock existing = TEMPORARY_BLOCKS.get(key);
            BlockState original = existing != null && previous == existing.replacement() ? existing.original() : previous;
            if (permanent) {
                TEMPORARY_BLOCKS.remove(key);
            } else {
                TEMPORARY_BLOCKS.put(key, new TemporaryBlock(editLevel.dimension(), original, replacement, expiry));
                EXPIRIES.add(new Expiry(key, expiry));
            }
        }).onComplete(job -> {
            if (job.denied() == 0) return;
            if (job.applied() == 0) {
                cast.message("reality_protected");
                cast.refundCooldown();
            } else {
                cast.message(permanent ? "reality_rewrite_permanent" : "reality_rewrite", job.applied());
            }
        });
        GauntletFx.cast(player, "reality", SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F);
        cast.message(permanent ? "reality_rewrite_permanent" : "reality_rewrite", edits.size());
        cast.cooldown();
    }

    private static BlockState nextInCycle(ServerLevel level, BlockPos pos, BlockState current) {
        return CYCLE.get((CYCLE.indexOf(current) + 1) % CYCLE.size());
    }

    private static void transmuteEntity(GauntletCast cast, LivingEntity target) {
        ServerLevel level = cast.level();
        Vec3 at = target.position();
        if (cast.crouching()) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y + target.getBbHeight() * 0.5D, at.z, 80,
                    target.getBbWidth(), target.getBbHeight() * 0.5D, target.getBbWidth(), 0.05D);
            GauntletFx.burst(level, at.add(0.0D, target.getBbHeight() * 0.5D, 0.0D), "reality", 40, target.getBbWidth());
            GauntletFx.sound(level, at, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 2.0F, 0.6F);
            cast.message("reality_bubbles", target.getDisplayName());
            target.discard();
            cast.cooldown();
            return;
        }
        EntityType<?> type = CREATURES.get(level.random.nextInt(CREATURES.size()));
        Entity replacement = type.create(level, null, BlockPos.containing(at), MobSpawnType.CONVERSION, false, false);
        if (replacement == null) return;
        replacement.moveTo(at.x, at.y, at.z, target.getYRot(), target.getXRot());
        if (target.hasCustomName()) replacement.setCustomName(target.getCustomName());
        GauntletFx.burst(level, at.add(0.0D, target.getBbHeight() * 0.5D, 0.0D), "reality", 50, target.getBbWidth());
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.5D, at.z, 20, 0.4D, 0.4D, 0.4D, 0.02D);
        GauntletFx.sound(level, at, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.5F, 0.8F);
        cast.message("reality_transmuted", target.getDisplayName(), replacement.getDisplayName());
        target.discard();
        level.addFreshEntity(replacement);
        cast.cooldown();
    }

    public static void gravityField(GauntletCast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = cast.level();
        if (cast.crouching()) {
            boolean inverted = player.hasEffect(AnthologyEffects.inverted());
            if (inverted) {
                player.removeEffect(AnthologyEffects.inverted());
            } else if (!player.addEffect(new MobEffectInstance(AnthologyEffects.inverted(), MobEffectInstance.INFINITE_DURATION,
                    0, false, true, true), player)) {
                cast.message("reality_gravity_immune");
                return;
            }
            GauntletFx.cast(player, "reality", SoundEvents.SHULKER_BULLET_HIT, 0.6F);
            cast.message(inverted ? "reality_self_gravity_down" : "reality_self_gravity_up");
            return;
        }
        Vec3 center = GauntletAim.point(player, 64.0D);
        double radius = 8.0D * cast.power();
        int duration = cast.infinity() ? 600 : 300;
        int affected = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                entity -> entity != player && entity.isAlive() && entity.distanceToSqr(center) <= radius * radius
                        && !GauntletAim.isProtectedEntity(entity))) {
            if (invert(target, duration, player)) affected++;
        }
        GauntletFx.ring(level, center, radius, "reality");
        GauntletFx.ring(level, center.add(0.0D, 3.0D, 0.0D), radius, "reality");
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 1.0D, center.z, 150, radius * 0.5D, 1.0D,
                radius * 0.5D, 0.3D);
        GauntletFx.cast(player, "reality", SoundEvents.ILLUSIONER_CAST_SPELL, 0.6F);
        cast.message("reality_gravity", affected);
        cast.cooldown();
    }

    public static boolean invert(LivingEntity target, int ticks, Entity source) {
        return target.addEffect(new MobEffectInstance(AnthologyEffects.inverted(), ticks, 0, false, true, true), source);
    }

    public static void sizeShiftActivate(GauntletCast cast) {
        if (!cast.crouching()) {
            cast.message("reality_size_controls");
            return;
        }
        ServerPlayer player = cast.player();
        LivingEntity aimed = GauntletAim.living(player, 32.0D);
        LivingEntity target = aimed != null ? aimed : player;
        AttributeInstance scale = target.getAttribute(Attributes.SCALE);
        if (scale == null) return;
        GauntletScale.write(scale, 0.0D);
        if (target instanceof Player) scaleReach(target, 1.0D);
        GauntletFx.burst(cast.level(), target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D), "reality", 20,
                target.getBbWidth() * 0.5D);
        GauntletFx.sound(cast.level(), target.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.2F);
        cast.message("reality_size_reset", target.getDisplayName());
    }

    public static void sizeShift(GauntletCast cast, double delta) {
        ServerPlayer player = cast.player();
        LivingEntity aimed = GauntletAim.living(player, 32.0D);
        LivingEntity target = aimed != null ? aimed : player;
        AttributeInstance scale = target.getAttribute(Attributes.SCALE);
        if (scale == null) return;
        double currentScale = Math.clamp(1.0D + GauntletScale.offset(scale), MIN_SCALE, MAX_SCALE);
        double nextScale = Math.clamp(currentScale * Math.pow(1.25D, delta), MIN_SCALE, MAX_SCALE);
        GauntletScale.write(scale, nextScale - 1.0D);
        if (target instanceof Player) scaleReach(target, nextScale);
        GauntletFx.burst(cast.level(), target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D), "reality", 8,
                target.getBbWidth() * 0.5D);
        cast.message("reality_size", target.getDisplayName(), String.format(java.util.Locale.ROOT, "%.2f", nextScale));
    }

    private static void scaleReach(LivingEntity target, double scale) {
        double offset = Math.max(-0.9D, scale - 1.0D);
        for (Holder<Attribute> attribute : List.of(Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE,
                Attributes.STEP_HEIGHT)) {
            AttributeInstance instance = target.getAttribute(attribute);
            if (instance == null) continue;
            instance.removeModifier(REACH_ID);
            if (Math.abs(offset) > 1.0E-3D) {
                instance.addPermanentModifier(new AttributeModifier(REACH_ID, offset, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    public static void tick(MinecraftServer server) {
        if (EXPIRIES.isEmpty()) return;
        long now = server.overworld().getGameTime();
        Map<ResourceKey<Level>, List<BlockEditQueue.Edit>> reverts = new HashMap<>();
        List<Expiry> deferred = new ArrayList<>();
        while (!EXPIRIES.isEmpty() && EXPIRIES.peek().expiry() <= now) {
            Expiry next = EXPIRIES.poll();
            TemporaryBlockKey key = next.key();
            TemporaryBlock temporary = TEMPORARY_BLOCKS.get(key);
            if (temporary == null || temporary.expiry() != next.expiry()) continue;
            ServerLevel level = server.getLevel(temporary.dimension());
            if (level == null) continue;
            BlockPos pos = key.position();
            if (!level.hasChunkAt(pos)) {
                deferred.add(new Expiry(key, now + 20L));
                continue;
            }
            TEMPORARY_BLOCKS.remove(key);
            if (level.getBlockState(pos) == temporary.replacement()) {
                reverts.computeIfAbsent(temporary.dimension(), ignored -> new ArrayList<>())
                        .add(new BlockEditQueue.Edit(pos, temporary.original(), null, temporary.replacement()));
            }
        }
        for (Expiry expiry : deferred) {
            TemporaryBlock temporary = TEMPORARY_BLOCKS.get(expiry.key());
            if (temporary != null) {
                TEMPORARY_BLOCKS.put(expiry.key(), new TemporaryBlock(temporary.dimension(), temporary.original(),
                        temporary.replacement(), expiry.expiry()));
                EXPIRIES.add(expiry);
            }
        }
        reverts.forEach((dimension, edits) -> {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) return;
            BlockEditQueue.submit(level, edits, null, 3).withoutUndo().onEdit((editLevel, pos, previous, replacement) -> {
                if (editLevel.random.nextInt(6) == 0) GauntletFx.burst(editLevel, Vec3.atCenterOf(pos), "reality", 2, 0.3D);
            });
        });
    }

    public static void clear(MinecraftServer server) {
        for (Map.Entry<TemporaryBlockKey, TemporaryBlock> entry : TEMPORARY_BLOCKS.entrySet()) {
            TemporaryBlock temporary = entry.getValue();
            ServerLevel level = server.getLevel(temporary.dimension());
            BlockPos pos = entry.getKey().position();
            if (level != null && level.hasChunkAt(pos) && level.getBlockState(pos) == temporary.replacement()) {
                level.setBlock(pos, temporary.original(), 3);
            }
        }
        TEMPORARY_BLOCKS.clear();
        EXPIRIES.clear();
        RADII.clear();
    }

    private record Expiry(TemporaryBlockKey key, long expiry) {
    }

    private record TemporaryBlockKey(ResourceKey<Level> dimension, BlockPos position) {
    }

    private record TemporaryBlock(ResourceKey<Level> dimension, BlockState original, BlockState replacement, long expiry) {
    }
}
