package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.craisinlord.integrated_superheroes.mixin.FallingBlockEntityInvoker;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PowerStone {
    private static final int OVERCHARGE_TICKS = 120;
    private static final Map<UUID, Overcharge> OVERCHARGES = new ConcurrentHashMap<>();
    private static final List<MoonToss> MOON_TOSSES = new ArrayList<>();
    private static final int MOON_PER_TICK = 512;
    private static final double MOON_VELOCITY = -1.6D;

    private PowerStone() {
    }

    public static void overcharge(GauntletCast cast) {
        ServerPlayer player = cast.player();
        Overcharge current = OVERCHARGES.get(player.getUUID());
        if (current != null) {
            release(player, current);
            return;
        }
        if (cast.onCooldown()) return;
        long now = player.server.overworld().getGameTime();
        OVERCHARGES.put(player.getUUID(), new Overcharge(now + OVERCHARGE_TICKS, cast.infinity(), cast.crouching(), new float[1]));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, OVERCHARGE_TICKS + 200, 9, false, true, true));
        GauntletFx.cast(player, "power", SoundEvents.BEACON_ACTIVATE, 0.5F);
        cast.message("power_overcharge");
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Overcharge charge = OVERCHARGES.get(player.getUUID());
        if (charge == null || event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        charge.stored()[0] += event.getAmount();
        event.setCanceled(true);
        GauntletFx.burst(player.serverLevel(), player.position().add(0.0D, 1.0D, 0.0D), "power", 12, 0.6D);
        GauntletFx.sound(player.serverLevel(), player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 0.6F);
    }

    public static void tick(MinecraftServer server) {
        MOON_TOSSES.removeIf(toss -> tickMoonToss(server, toss));
        if (OVERCHARGES.isEmpty()) return;
        long now = server.overworld().getGameTime();
        for (Map.Entry<UUID, Overcharge> entry : List.copyOf(OVERCHARGES.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                OVERCHARGES.remove(entry.getKey());
                continue;
            }
            if (now % 4L == 0L) {
                GauntletFx.sphere(player.serverLevel(), player.position().add(0.0D, 1.0D, 0.0D),
                        1.2D + entry.getValue().stored()[0] * 0.02D, "power", 10);
            }
            if (now >= entry.getValue().expiry()) release(player, entry.getValue());
        }
    }

    private static void release(ServerPlayer player, Overcharge charge) {
        if (!OVERCHARGES.remove(player.getUUID(), charge)) return;
        ServerLevel level = player.serverLevel();
        float stored = charge.stored()[0];
        double multiplier = GauntletConfig.powerMultiplier() * (charge.infinity() ? 1.5D : 1.0D);
        double radius = Math.min(48.0D, (5.0D + stored * 0.35D) * multiplier);
        float damage = (float) ((10.0D + stored * 2.5D) * multiplier);
        Vec3 center = player.position();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive() && entity.distanceToSqr(center) <= radius * radius)) {
            Vec3 push = target.position().subtract(center);
            double falloff = 1.0D - Math.sqrt(target.distanceToSqr(center)) / radius;
            target.hurt(player.damageSources().playerAttack(player), damage * (float) Math.max(0.25D, falloff));
            Vec3 direction = push.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 1.0D, 0.0D) : push.normalize();
            target.push(direction.x * 2.5D * falloff, 0.6D + falloff, direction.z * 2.5D * falloff);
            target.hurtMarked = true;
        }
        if (GauntletConfig.griefing()) {
            crater(player, level, BlockPos.containing(center).below(), Math.min(12.0D, radius * 0.35D), charge.infinity());
        }
        for (int ring = 1; ring <= 3; ring++) GauntletFx.ring(level, center, radius * ring / 3.0D, "power");
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.5D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        GauntletFx.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 0.5F);
        GauntletFx.sound(level, center, SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 0.6F);
        GauntletFx.shake(level, center, (float) Math.min(3.0D, 1.0D + stored * 0.05D), 30, (float) radius * 2.0F);
        GauntletAbility ability = GauntletAbilities.get("power", 0);
        if (ability != null) {
            GauntletAbilityController.addAbilityCooldown(player, "power", 0, ability.cooldownTicks(charge.infinity()));
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.integrated_superheroes.gauntlet.power_release", Math.round(stored)), true);
    }

    private static void crater(ServerPlayer player, ServerLevel level, BlockPos center, double radius, boolean launchRing) {
        int r = (int) Math.ceil(radius);
        List<BlockEditQueue.Edit> edits = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, 0, r))) {
            double distance = Math.sqrt(pos.distSqr(center));
            if (distance > radius) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.hasBlockEntity() || state.getExplosionResistance(level, pos, null) > 1200.0F) continue;
            if (launchRing && distance > radius - 1.5D && level.random.nextFloat() < 0.6F) {
                if (!GauntletGrief.canEdit(player, level, pos)) continue;
                FallingBlockEntity falling = FallingBlockEntity.fall(level, pos.immutable(), state);
                falling.setDeltaMovement((pos.getX() - center.getX()) * 0.08D, 0.9D + level.random.nextDouble() * 0.6D,
                        (pos.getZ() - center.getZ()) * 0.08D);
                falling.dropItem = false;
                falling.hurtMarked = true;
                continue;
            }
            if (!level.isInWorldBounds(pos)) continue;
            edits.add(new BlockEditQueue.Edit(pos.immutable(), Blocks.AIR.defaultBlockState(), null));
        }
        BlockEditQueue.submit(level, edits, "power", 3).withoutUndo().guardedBy(player);
    }

    public static void moonToss(GauntletCast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = cast.level();
        BlockHitResult hit = GauntletAim.block(player, 96.0D);
        Vec3 target = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : GauntletAim.point(player, 96.0D);
        int radius = (int) Math.round(Mth.clamp(3.0D * cast.power(), 2.0D, 9.0D));
        BlockPos source = BlockPos.containing(target).below(radius + 1);
        Vec3 spawnCenter = target.add(0.0D, 36.0D + radius * 2.0D, 0.0D);
        List<BlockPos> offsets = new ArrayList<>();
        for (BlockPos offset : BlockPos.betweenClosed(-radius, -radius, -radius, radius, radius, radius)) {
            if (offset.distSqr(BlockPos.ZERO) <= radius * radius) offsets.add(offset.immutable());
        }
        MOON_TOSSES.add(new MoonToss(level.dimension(), player.getUUID(), source, spawnCenter, offsets,
                3.0F * (float) cast.power(), GauntletConfig.griefing()));
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, spawnCenter.x, spawnCenter.y, spawnCenter.z, 2, 2.0D, 2.0D, 2.0D, 0.0D);
        GauntletFx.cast(player, "power", SoundEvents.WITHER_SPAWN, 0.5F);
        GauntletFx.shake(level, target, 2.0F, 40, 64.0F);
        cast.cooldown();
    }

    private static boolean tickMoonToss(MinecraftServer server, MoonToss toss) {
        ServerLevel level = server.getLevel(toss.dimension);
        if (level == null) return true;
        ServerPlayer player = server.getPlayerList().getPlayer(toss.owner);
        double fallY = 0.0D;
        double velocity = MOON_VELOCITY;
        double fallen = 0.0D;
        for (int tick = 0; tick < toss.age; tick++) {
            velocity -= 0.04D;
            fallY += velocity;
            fallen -= velocity;
            velocity *= 0.98D;
        }
        int budget = MOON_PER_TICK;
        while (budget-- > 0 && toss.cursor < toss.offsets.size()) {
            BlockPos offset = toss.offsets.get(toss.cursor++);
            BlockPos ground = toss.source.offset(offset);
            BlockState state = level.getBlockState(ground);
            boolean rip = toss.griefing && player != null && !state.isAir() && !state.hasBlockEntity()
                    && state.getDestroySpeed(level, ground) >= 0.0F && GauntletGrief.canEdit(player, level, ground);
            BlockPos sky = BlockPos.containing(toss.spawnCenter.add(offset.getX(), offset.getY(), offset.getZ()));
            if (!level.isInWorldBounds(sky) || !level.isLoaded(sky) || !level.getBlockState(sky).isAir()) continue;
            BlockState moon = rip ? state : moonBlock(level.random.nextInt(10));
            if (moon.isAir() || moon.hasBlockEntity()) moon = moonBlock(level.random.nextInt(10));
            if (rip) level.setBlock(ground, Blocks.AIR.defaultBlockState(), 3);
            FallingBlockEntity falling;
            if (toss.age == 0) {
                falling = FallingBlockEntity.fall(level, sky, moon);
            } else {
                BlockState stripped = moon.hasProperty(BlockStateProperties.WATERLOGGED)
                        ? moon.setValue(BlockStateProperties.WATERLOGGED, false) : moon;
                falling = FallingBlockEntityInvoker.integratedSuperheroes$create(level, sky.getX() + 0.5D, sky.getY() + fallY,
                        sky.getZ() + 0.5D, stripped);
                falling.fallDistance = (float) fallen;
                level.addFreshEntity(falling);
            }
            falling.setDeltaMovement(0.0D, toss.age == 0 ? MOON_VELOCITY : velocity, 0.0D);
            falling.setHurtsEntities(toss.damage, 80);
            falling.dropItem = false;
            falling.hurtMarked = true;
            toss.spawned++;
        }
        toss.age++;
        if (toss.cursor < toss.offsets.size()) return false;
        if (player != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.integrated_superheroes.gauntlet.power_moon_toss", toss.spawned), true);
        }
        return true;
    }

    private static BlockState moonBlock(int roll) {
        return switch (roll) {
            case 0, 1 -> Blocks.BLACKSTONE.defaultBlockState();
            case 2 -> Blocks.MAGMA_BLOCK.defaultBlockState();
            case 3 -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            default -> Blocks.END_STONE.defaultBlockState();
        };
    }

    public static void clear() {
        OVERCHARGES.clear();
        MOON_TOSSES.clear();
    }

    private static final class MoonToss {
        private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        private final UUID owner;
        private final BlockPos source;
        private final Vec3 spawnCenter;
        private final List<BlockPos> offsets;
        private final float damage;
        private final boolean griefing;
        private int cursor;
        private int age;
        private int spawned;

        private MoonToss(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, UUID owner, BlockPos source,
                         Vec3 spawnCenter, List<BlockPos> offsets, float damage, boolean griefing) {
            this.dimension = dimension;
            this.owner = owner;
            this.source = source;
            this.spawnCenter = spawnCenter;
            this.offsets = offsets;
            this.damage = damage;
            this.griefing = griefing;
        }
    }

    private record Overcharge(long expiry, boolean infinity, boolean crouching, float[] stored) {
    }
}
