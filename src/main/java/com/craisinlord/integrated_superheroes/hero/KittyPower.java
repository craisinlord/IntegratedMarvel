package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class KittyPower {
    public static final String ID = "phasing_kitty_pryde";
    public static final String PHASING_STATE = "integrated_superheroes_phasing";
    private static final String PHASE = "phase";
    private static final TagKey<Block> PHASE_BLOCKING = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "phase_blocking"));
    private static final Map<UUID, Float> METER = new HashMap<>();
    private static final Map<UUID, Integer> SENT = new HashMap<>();
    private static final Map<UUID, Long> GUESTS = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_SOLID = new HashMap<>();
    private static final Map<UUID, Vec3> SERVER_OK = new HashMap<>();
    private static final Map<UUID, Vec3> CLIENT_OK = new HashMap<>();
    private static final List<BlockPos> RESCUE_OFFSETS = rescueOffsets();
    private static volatile int clientMeter = 100;

    private KittyPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Phasing (Shadowcat)",
                syringe,
                MarvelBalanceConfig.KITTY,
                List.of("agility"),
                List.of(
                        new HeroPower.Ability(PHASE, "Phase", KittyPower::phase),
                        new HeroPower.Ability("phase_strike", "Phase Strike", KittyPower::phaseStrike),
                        new HeroPower.Ability("phase_sink", "Phase Sink", KittyPower::phaseSink),
                        new HeroPower.Ability("phase_others", "Phase Others", KittyPower::phaseOthers)
                ),
                List.of(),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        float meter = METER.getOrDefault(player.getUUID(), 1.0F);
                        float step = 1.0F / (MarvelBalanceConfig.PHASE_METER_SECONDS.get().floatValue() * 20.0F);
                        if (isPhasing(player) && !GUESTS.containsKey(player.getUUID())) {
                            meter = Math.max(0.0F, meter - step);
                            if (meter <= 0.0F) {
                                endPhase(player);
                                player.displayClientMessage(Component.literal("§7You can't hold the phase any longer."), true);
                            }
                        } else {
                            meter = Math.min(1.0F, meter + step * 0.5F);
                        }
                        METER.put(player.getUUID(), meter);
                        BlockPos eye = BlockPos.containing(player.getEyePosition());
                        HeroAdvancements.travel(player, "x_men/kitty_pryde/ghosted",
                                isPhasing(player) && player.level().getBlockState(eye).isSuffocating(player.level(), eye), 500.0D);
                        int percent = Math.round(meter * 100.0F);
                        Integer sent = SENT.get(player.getUUID());
                        if (sent == null || sent != percent) {
                            SENT.put(player.getUUID(), percent);
                            PacketDistributor.sendToPlayer(player, new PhaseMeterPayload(percent));
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        if (isPhasing(player)) {
                            endPhase(player);
                        }
                        METER.remove(player.getUUID());
                        SENT.remove(player.getUUID());
                    }
                }
        );
    }

    public static boolean isPhasing(Player player) {
        return player.hasData(com.stxllarplanes.marvellegacy.init.AttachmentsRegistry.PLAYER_DATA.get())
                && HeroPowers.data(player).hasState(PHASING_STATE);
    }

    public static int clientMeter() {
        return clientMeter;
    }

    public static void setClientMeter(int value) {
        clientMeter = value;
    }

    private static List<BlockPos> rescueOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -8; x <= 8; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -8; z <= 8; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.sort(Comparator.comparingDouble(offset -> offset.getX() * offset.getX() + offset.getZ() * offset.getZ()
                + offset.getY() * offset.getY() * (offset.getY() < 0 ? 3.0D : 0.6D)));
        return offsets;
    }

    private static boolean blocked(Player player) {
        Level level = player.level();
        AABB box = player.getBoundingBox().deflate(0.01D);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (state.is(PHASE_BLOCKING) || isReinforced(state)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isReinforced(BlockState state) {
        var key = state.getBlockHolder().unwrapKey();
        return key.isPresent() && "securitycraft".equals(key.get().location().getNamespace()) && key.get().location().getPath().contains("reinforced");
    }

    public static void tickMovement(Player player) {
        Map<UUID, Vec3> ok = player.level().isClientSide ? CLIENT_OK : SERVER_OK;
        if (!isPhasing(player)) {
            ok.remove(player.getUUID());
            return;
        }
        if (player.level().isClientSide && !player.isLocalPlayer()) {
            return;
        }
        if (!blocked(player)) {
            ok.put(player.getUUID(), player.position());
            standOnFloor(player);
            return;
        }
        Vec3 back = ok.get(player.getUUID());
        if (back == null) {
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.teleport(back.x, back.y, back.z, player.getYRot(), player.getXRot());
        } else {
            player.setPos(back.x, back.y, back.z);
        }
    }

    private static void standOnFloor(Player player) {
        player.fallDistance = 0.0F;
        if (!player.level().isClientSide || player.isShiftKeyDown() || player.getDeltaMovement().y > 0.0D) {
            return;
        }
        Level level = player.level();
        BlockPos floor = BlockPos.containing(player.getX(), player.getY() + 0.001D, player.getZ()).below();
        VoxelShape shape = level.getBlockState(floor).getCollisionShape(level, floor);
        if (shape.isEmpty()) {
            return;
        }
        double top = floor.getY() + shape.max(Direction.Axis.Y);
        if (player.getY() - top > 0.05D || top - player.getY() > 0.6D) {
            return;
        }
        if (player.getY() < top) {
            player.setPos(player.getX(), top, player.getZ());
        }
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, 0.0D, motion.z);
        player.setOnGround(true);
    }

    public static void tickAny(ServerPlayer player) {
        boolean phasing = isPhasing(player);
        if (!phasing) {
            if (player.onGround()) {
                LAST_SOLID.put(player.getUUID(), player.position());
            }
            return;
        }
        Long guestUntil = GUESTS.get(player.getUUID());
        if (guestUntil != null && player.level().getGameTime() > guestUntil
                || guestUntil == null && !ID.equals(HeroPowers.data(player).getPrimaryPower())
                || player.getY() < player.level().getMinBuildHeight() + 2) {
            endPhase(player);
            return;
        }
        player.fallDistance = 0.0F;
        ServerLevel level = player.serverLevel();
        if (player.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(), 1,
                    player.getBbWidth() * 0.5D, player.getBbHeight() * 0.4D, player.getBbWidth() * 0.5D, 0.0D);
        }
        if (player.tickCount % 10 == 0) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24.0D), mob -> mob.getTarget() == player)) {
                mob.setTarget(null);
            }
        }
    }

    private static void startPhase(ServerPlayer player) {
        HeroPowers.data(player).addState(PHASING_STATE);
        PlayerData.sync(player);
        SERVER_OK.put(player.getUUID(), player.position());
        HeroFx.play(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.4F);
        Vec3 center = player.getBoundingBox().getCenter();
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 20, 0.3D, 0.6D, 0.3D, 0.02D);
    }

    public static void endPhase(ServerPlayer player) {
        HeroPowers.data(player).removeState(PHASING_STATE);
        GUESTS.remove(player.getUUID());
        PlayerData.sync(player);
        player.noPhysics = false;
        rescue(player);
        HeroFx.play(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 0.8F);
    }

    private static void rescue(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (level.noCollision(player, player.getBoundingBox())) {
            return;
        }
        BlockPos origin = player.blockPosition();
        AABB box = player.getBoundingBox();
        for (BlockPos offset : RESCUE_OFFSETS) {
            BlockPos at = origin.offset(offset);
            if (at.getY() < level.getMinBuildHeight() + 1) {
                continue;
            }
            Vec3 target = new Vec3(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
            AABB moved = box.move(target.subtract(player.position()));
            if (level.noCollision(player, moved) && !level.containsAnyLiquid(moved)) {
                player.connection.teleport(target.x, target.y, target.z, player.getYRot(), player.getXRot());
                return;
            }
        }
        Vec3 last = LAST_SOLID.get(player.getUUID());
        if (last != null) {
            player.connection.teleport(last.x, last.y, last.z, player.getYRot(), player.getXRot());
        }
    }

    public static void onLogin(ServerPlayer player) {
        if (isPhasing(player)) {
            endPhase(player);
        }
    }

    public static void onAnyDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && isPhasing(player) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    public static void onAttack(AttackEntityEvent event) {
        if (isPhasing(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isPhasing(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (isPhasing(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (isPhasing(event.getEntity())) {
            event.setNewSpeed(0.0F);
        }
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (isPhasing(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && isPhasing(player)) {
            event.setCanceled(true);
        }
    }

    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player player && isPhasing(player)) {
            event.setCanceled(true);
        }
    }

    public static void onProjectile(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof Player player && isPhasing(player)) {
            event.setCanceled(true);
        }
    }

    private static double phase(ServerPlayer player, int slot) {
        if (isPhasing(player)) {
            endPhase(player);
            return 1.0D;
        }
        if (METER.getOrDefault(player.getUUID(), 1.0F) < 0.1F) {
            player.displayClientMessage(Component.literal("§7You need a moment before phasing again."), true);
            return 0.0D;
        }
        if (player.getY() < player.level().getMinBuildHeight() + 2) {
            return 0.0D;
        }
        startPhase(player);
        return 0.0D;
    }

    private static double phaseStrike(ServerPlayer player, int slot) {
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), 4.0D, 1);
        if (beam.targets().isEmpty()) {
            return 0.0D;
        }
        LivingEntity target = beam.targets().get(0);
        if (isPhasing(player)) {
            endPhase(player);
        }
        float damage = MarvelBalanceConfig.PHASE_STRIKE_DAMAGE.get().floatValue();
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().indirectMagic(player, player), damage));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 2));
        Vec3 center = target.getBoundingBox().getCenter();
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 16, 0.3D, 0.4D, 0.3D, 0.05D);
        HeroFx.play(player, SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 1.6F);
        HeroAnimations.play(player, "uppercut");
        return MarvelBalanceConfig.PHASE_STRIKE_COOLDOWN.get();
    }

    private static double phaseSink(ServerPlayer player, int slot) {
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), 8.0D, 1);
        if (beam.targets().isEmpty() || !HeroAbilities.canControl(beam.targets().get(0))) {
            return 0.0D;
        }
        LivingEntity target = beam.targets().get(0);
        double originalY = target.getY();
        target.teleportTo(target.getX(), originalY - 1.0D, target.getZ());
        target.setDeltaMovement(Vec3.ZERO);
        HeroStun.stun(target, MarvelBalanceConfig.PHASE_SINK_TICKS.get(), originalY);
        Vec3 center = target.position();
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.5D, center.z, 20, 0.4D, 0.2D, 0.4D, 0.02D);
        HeroFx.play(target, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 0.6F);
        HeroAnimations.play(player, "bettercombat:one_handed_slam");
        return MarvelBalanceConfig.PHASE_SINK_COOLDOWN.get();
    }

    private static double phaseOthers(ServerPlayer player, int slot) {
        ServerPlayer ally = player.serverLevel().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(3.0D),
                        candidate -> candidate != player && !candidate.isSpectator() && !isPhasing(candidate))
                .stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(player)))
                .orElse(null);
        if (ally == null) {
            player.displayClientMessage(Component.literal("§7No one close enough to take with you."), true);
            return 0.0D;
        }
        float meter = METER.getOrDefault(player.getUUID(), 1.0F);
        if (meter < 0.25F) {
            player.displayClientMessage(Component.literal("§7You need a moment before phasing again."), true);
            return 0.0D;
        }
        METER.put(player.getUUID(), meter - 0.25F);
        GUESTS.put(ally.getUUID(), ally.level().getGameTime() + MarvelBalanceConfig.PHASE_OTHERS_TICKS.get());
        startPhase(ally);
        ally.displayClientMessage(Component.literal("§dKitty Pryde phases you through."), true);
        HeroAdvancements.award(player, "x_men/kitty_pryde/plus_one");
        if (!isPhasing(player)) {
            startPhase(player);
        }
        return MarvelBalanceConfig.PHASE_OTHERS_COOLDOWN.get();
    }
}
