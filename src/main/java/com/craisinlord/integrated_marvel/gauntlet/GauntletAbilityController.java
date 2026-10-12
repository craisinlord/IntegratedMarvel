package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.integrated_marvel.gauntlet.InfinityGauntletStones;
import com.craisinlord.integrated_marvel.hero.HealingFactor;
import com.craisinlord.antarchy.content.time.TimeDilationApi;
import com.craisinlord.antarchy.content.time.TimeDilationEntityAccess;
import com.craisinlord.antarchy.content.time.TimeDilationManager;
import com.craisinlord.antarchy.content.time.TimeDilationMath;
import com.craisinlord.antarchy.content.gravity.AntarchyGravityApi;
import com.craisinlord.antarchy.content.gravity.AntarchyGravityDirection;
import com.craisinlord.antarchy.content.entity.SizeRayProjectileEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.craisinlord.antarchy.content.entity.royal.beam.RoyalBeamController;
import com.craisinlord.antarchy.content.entity.royal.beam.RoyalBeamSettings;
import com.craisinlord.antarchy.content.entity.royal.beam.RoyalBeamTerrainMode;
import com.craisinlord.antarchy.content.portal.CrossDimensionTravelApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GauntletAbilityController {
    public static final int PREVIOUS_STONE = 0;
    public static final int NEXT_STONE = 1;
    public static final int PREVIOUS_ABILITY = 2;
    public static final int NEXT_ABILITY = 3;
    public static final int TIME_ADJUST = 4;
    public static final int ACTIVATE_START = 5;
    public static final int ACTIVATE_RELEASE = 6;
    public static final int ACTIVATE = 7;
    public static final int REALITY_SIZE_ADJUST = 8;
    private static final double MIN_REALITY_SCALE = 0.01D;
    private static final double MAX_REALITY_SCALE = 100.0D;
    private static final int MAX_BEAM_CHARGE_TICKS = 40;
    private static final Map<UUID, Long> SNAP_CONFIRMATIONS = new ConcurrentHashMap<>();
    private static final Map<AbilityCooldownKey, Cooldown> ABILITY_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveBeam> ACTIVE_BEAMS = new ConcurrentHashMap<>();
    private static final Map<UUID, ChargingBeam> CHARGING_BEAMS = new ConcurrentHashMap<>();
    private static final Map<TemporaryBlockKey, TemporaryBlock> TEMPORARY_BLOCKS = new ConcurrentHashMap<>();
    private static final Map<UUID, TemporaryEntityEffect> TEMPORARY_ENTITY_EFFECTS = new ConcurrentHashMap<>();
    private static final int MAX_TEMPORARY_BLOCK_REWRITES = 4096;
    private static final List<BlockState> REALITY_BLOCK_CYCLE = List.of(Blocks.STONE.defaultBlockState(),
            Blocks.AMETHYST_BLOCK.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState(),
            Blocks.DIAMOND_BLOCK.defaultBlockState(), Blocks.EMERALD_BLOCK.defaultBlockState(),
            Blocks.OBSIDIAN.defaultBlockState(), Blocks.BEDROCK.defaultBlockState());
    private static final List<String> STONE_ORDER = List.of("power", "space", "reality", "soul", "time", "mind");

    private GauntletAbilityController() {
    }

    public static boolean isAbilityOnCooldown(Player player) {
        Cooldown cooldown = ABILITY_COOLDOWNS.get(abilityCooldownKey(player));
        return cooldown != null && cooldown.endsAt() > cooldownClock(player);
    }

    public static void addAbilityCooldown(Player player, int ticks) {
        if (ticks > 0) {
            AbilityCooldownKey key = abilityCooldownKey(player);
            ABILITY_COOLDOWNS.put(key, new Cooldown(cooldownClock(player) + ticks, ticks));
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new GauntletCooldownPayload(key.stone(), key.ability(), ticks));
            }
        }
    }

    public static float getAbilityCooldownPercent(Player player) {
        if (player.level().isClientSide) return GauntletCooldownClientState.percent(player);
        Cooldown cooldown = ABILITY_COOLDOWNS.get(abilityCooldownKey(player));
        if (cooldown == null) return 0.0F;
        long remaining = cooldown.endsAt() - cooldownClock(player);
        if (remaining <= 0) {
            ABILITY_COOLDOWNS.remove(abilityCooldownKey(player), cooldown);
            return 0.0F;
        }
        return Math.clamp((float) remaining / cooldown.duration(), 0.0F, 1.0F);
    }

    private static long cooldownClock(Player player) {
        return player instanceof ServerPlayer serverPlayer
                ? serverPlayer.server.overworld().getGameTime()
                : player.level().getGameTime();
    }

    private static AbilityCooldownKey abilityCooldownKey(Player player) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        String stone = gauntlet.isEmpty() ? "none" : InfinityGauntletStones.selectedStone(gauntlet);
        int ability = gauntlet.isEmpty() ? 0 : InfinityGauntletStones.selectedAbility(gauntlet, stone) + 1;
        return new AbilityCooldownKey(player.getUUID(), stone, ability);
    }

    public static void handleInput(ServerPlayer player, int action, double delta) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        if (gauntlet.isEmpty()) {
            return;
        }
        if (action == PREVIOUS_STONE) {
            cycleStone(player, gauntlet, -1);
        } else if (action == NEXT_STONE) {
            cycleStone(player, gauntlet, 1);
        } else if (action == PREVIOUS_ABILITY) {
            cycleAbility(player, gauntlet, -1);
        } else if (action == NEXT_ABILITY) {
            cycleAbility(player, gauntlet, 1);
        } else if (action == TIME_ADJUST) {
            adjustTargetTime(player, delta);
        } else if (action == REALITY_SIZE_ADJUST) {
            adjustTargetSize(player, delta);
        } else if (action == ACTIVATE_START) {
            beginBeamActivation(player);
        } else if (action == ACTIVATE_RELEASE) {
            releaseActivation(player);
        } else if (action == ACTIVATE) {
            activate(player);
        }
    }

    public static void activate(ServerPlayer player) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        if (gauntlet.isEmpty()) {
            return;
        }
        String stone = InfinityGauntletStones.selectedStone(gauntlet);
        int ability = InfinityGauntletStones.selectedAbility(gauntlet, stone);
        if (stone.equals("power") && InfinityGauntletStones.hasStone(gauntlet, "power")) {
            if (ability == 0) {
                if (isAbilityOnCooldown(player)) {
                    return;
                }
                BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.withDefaultNamespace("strength"))
                        .ifPresent(effect -> player.addEffect(new MobEffectInstance(effect, 400, 9, false, true, true)));
                addAbilityCooldown(player, 400);
                player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.power_strength"), true);
            } else {
                beginBeamCharge(player, BeamKind.POWER);
            }
        } else if (stone.equals("space") && InfinityGauntletStones.hasStone(gauntlet, "space")) {
            activateSpace(player, gauntlet, ability);
        } else if (stone.equals("time") && InfinityGauntletStones.hasStone(gauntlet, "time")) {
            if (ability == 2) {
                if (player.isShiftKeyDown()) {
                    activateDimensionTimeField(player, gauntlet);
                } else {
                    activateTimeField(player, gauntlet);
                }
            } else if (ability == 1) {
                player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.time_target_controls"), true);
            } else {
                TimeFreezeManager.start(player);
            }
        } else if (stone.equals("reality") && InfinityGauntletStones.hasStone(gauntlet, "reality")) {
            activateReality(player, gauntlet, ability);
        } else if (stone.equals("mind") && InfinityGauntletStones.hasStone(gauntlet, "mind")) {
            activateMind(player, gauntlet, ability);
        } else if (stone.equals("soul") && InfinityGauntletStones.hasStone(gauntlet, "soul")) {
            activateSoul(player, gauntlet, ability);
        } else if (stone.equals("snap") && InfinityGauntletStones.isFullGauntlet(gauntlet) && InfinityGauntletStones.hasAllStones(gauntlet)) {
            if (ability == 0) {
                beginBeamCharge(player, BeamKind.INFINITY);
            } else {
                activateLifeHalvingSnap(player, gauntlet);
            }
        }
    }

    public static boolean usesHeldActivation(ItemStack gauntlet) {
        String stone = InfinityGauntletStones.selectedStone(gauntlet);
        int ability = InfinityGauntletStones.selectedAbility(gauntlet, stone);
        return switch (stone) {
            case "power" -> ability == 1 && InfinityGauntletStones.hasStone(gauntlet, "power");
            case "mind" -> ability == 2 && InfinityGauntletStones.hasStone(gauntlet, "mind");
            case "time" -> ability == 0 && InfinityGauntletStones.hasStone(gauntlet, "time");
            case "snap" -> ability == 0 && InfinityGauntletStones.isFullGauntlet(gauntlet)
                    && InfinityGauntletStones.hasAllStones(gauntlet);
            default -> false;
        };
    }

    public static boolean canAdjustTargetTime(Player player) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        if (gauntlet.isEmpty() || !InfinityGauntletStones.hasStone(gauntlet, "time")
                || !InfinityGauntletStones.selectedStone(gauntlet).equals("time")) {
            return false;
        }
        return InfinityGauntletStones.selectedAbility(gauntlet, "time") == 1;
    }

    public static boolean canAdjustTargetSize(Player player) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        if (gauntlet.isEmpty() || !InfinityGauntletStones.hasStone(gauntlet, "reality")
                || !InfinityGauntletStones.selectedStone(gauntlet).equals("reality")) {
            return false;
        }
        return InfinityGauntletStones.selectedAbility(gauntlet, "reality") == 2;
    }

    private static void activateTimeField(ServerPlayer player, ItemStack gauntlet) {
        if (isAbilityOnCooldown(player)) {
            return;
        }
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize().scale(32.0D));
        BlockHitResult hit = player.serverLevel().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 center = hit.getType() == HitResult.Type.BLOCK
                ? hit.getLocation()
                : player.position().add(player.getLookAngle().normalize().scale(16.0D));
        TimeDilationApi.createField(player.serverLevel(), center, 20.0D, 0.25D, 400, player.getUUID(), true);
        addAbilityCooldown(player, 600);
        player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.time_field"), true);
    }

    private static void activateDimensionTimeField(ServerPlayer player, ItemStack gauntlet) {
        if (isAbilityOnCooldown(player)) return;
        DimensionTimeOverrideManager.create(player.serverLevel(), player.getUUID());
        addAbilityCooldown(player, 1200);
        player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.time_dimension_field"), true);
    }

    private static void cycleStone(ServerPlayer player, ItemStack gauntlet, int direction) {
        List<String> installed = new ArrayList<>(InfinityGauntletStones.installedStoneKeys(gauntlet));
        if (InfinityGauntletStones.isFullGauntlet(gauntlet) && InfinityGauntletStones.hasAllStones(gauntlet)) {
            installed.add("snap");
        }
        if (installed.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.no_stones"), true);
            return;
        }
        String selected = InfinityGauntletStones.selectedStone(gauntlet);
        int current = installed.indexOf(selected);
        if (current < 0) current = direction > 0 ? -1 : 0;
        String next = installed.get(Math.floorMod(current + direction, installed.size()));
        InfinityGauntletStones.setSelectedStone(gauntlet, next);
        sendSelectionToast(player, next, 0);
    }

    private static void cycleAbility(ServerPlayer player, ItemStack gauntlet, int direction) {
        String stone = InfinityGauntletStones.selectedStone(gauntlet);
        int count = InfinityGauntletStones.abilityCount(stone);
        if (count < 2) {
            return;
        }
        int current = InfinityGauntletStones.selectedAbility(gauntlet, stone);
        int next = Math.floorMod(current + direction, count);
        InfinityGauntletStones.setSelectedAbility(gauntlet, stone, next);
        sendSelectionToast(player, stone, next);
    }

    private static void sendSelectionToast(ServerPlayer player, String stone, int ability) {
        PacketDistributor.sendToPlayer(player, new GauntletSelectionToastPayload(stone, ability));
    }

    private static void activateSpace(ServerPlayer player, ItemStack gauntlet, int ability) {
        if (isAbilityOnCooldown(player)) {
            return;
        }
        boolean moved;
        if (ability == 0) {
            Vec3 start = player.getEyePosition();
            Vec3 end = start.add(player.getLookAngle().normalize().scale(320.0D));
            BlockHitResult hit = player.serverLevel().clip(new ClipContext(start, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.BLOCK) {
                player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.space_no_target"), true);
                return;
            }
            moved = CrossDimensionTravelApi.transferToSafePosition(player, player.serverLevel(), hit.getBlockPos().relative(hit.getDirection()));
        } else if (ability == 1 && player.isShiftKeyDown()) {
            SpaceDestinationManager.travel(player);
            return;
        } else {
            Vec3 start = player.getEyePosition();
            Vec3 end = start.add(player.getLookAngle().normalize().scale(32.0D));
            BlockHitResult hit = player.serverLevel().clip(new ClipContext(start, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.BLOCK) {
                player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.space_no_target"), true);
                return;
            }
            SpaceDestinationManager.Destination selected = SpaceDestinationManager.selected(player);
            if (selected == null) {
                player.displayClientMessage(Component.literal("Choose a Space destination first (default key: O)."), true);
                return;
            }
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            Vec3 portalPosition = hit.getLocation();
            if (hit.getDirection().getAxis().isVertical()) {
                Vec3 look = player.getLookAngle();
                normal = new Vec3(look.x, 0.0D, look.z).normalize();

                portalPosition = portalPosition.add(0.0D, 0.9D, 0.0D);
            }
            SpaceDestinationManager.openPortal(player, hit.getBlockPos(), portalPosition, normal);
            return;
        }
        if (moved) {
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    player.getSoundSource(), 1.0F, 1.0F);
            addAbilityCooldown(player, 100);
            player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.space_travelled"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.space_arrival_failed"), true);
        }
    }

    private static void startBeam(ServerPlayer player, BeamKind kind) {
        startBeam(player, kind, 1.0D);
    }

    private static void startBeam(ServerPlayer player, BeamKind kind, double charge) {
        RoyalBeamSettings base = kind == BeamKind.INFINITY ? infinityBeamSettings()
                : kind == BeamKind.MIND ? mindBeamSettings() : annihilationBeamSettings();
        double strength = 0.35D + Math.clamp(charge, 0.0D, 1.0D) * 0.65D;
        RoyalBeamSettings settings = new RoyalBeamSettings(
                base.range() * (0.5D + 0.5D * charge), base.targetTracking(), base.pathStep(),
                Math.max(6, (int) Math.round(base.durationTicks() * (0.5D + 0.5D * charge))), base.travelTicks(),
                base.cooldownTicks(), (float) (base.pathDamageRadius() * strength),
                (float) (base.impactDamageRadius() * strength), (float) (base.damage() * strength),
                (float) (base.knockback() * strength), base.terrainIntervalTicks(), base.maxTerrainDistance() * (0.5D + 0.5D * charge),
                (float) (base.pathTerrainRadius() * strength), (float) (base.impactTerrainRadius() * strength),
                Math.max(0, (int) Math.round(base.maxTerrainMutationsPerTick() * strength)), base.terrainMutationChance(),
                base.terrainDropChance(), base.maxTerrainResistance(), base.terrainEnabled(), base.requireLineOfSightForDamage());
        RoyalBeamController controller = new RoyalBeamController(player);
        Vec3 start = player.getEyePosition();
        controller.start(start, start.add(player.getLookAngle().normalize().scale(settings.range())), settings.durationTicks());
        ACTIVE_BEAMS.put(player.getUUID(), new ActiveBeam(controller, settings, kind));
    }

    private static void beginBeamActivation(ServerPlayer player) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        if (gauntlet.isEmpty()) return;
        String stone = InfinityGauntletStones.selectedStone(gauntlet);
        int ability = InfinityGauntletStones.selectedAbility(gauntlet, stone);
        if (stone.equals("time") && ability == 0 && InfinityGauntletStones.hasStone(gauntlet, "time")) {
            TimeFreezeManager.start(player);
            return;
        }
        if (stone.equals("power") && InfinityGauntletStones.hasStone(gauntlet, "power")) {
            if (ability == 1) beginBeamCharge(player, BeamKind.POWER);
        } else if (stone.equals("mind") && ability == 2 && InfinityGauntletStones.hasStone(gauntlet, "mind")) {
            beginBeamCharge(player, BeamKind.MIND);
        } else if (stone.equals("snap") && ability == 0 && InfinityGauntletStones.isFullGauntlet(gauntlet) && InfinityGauntletStones.hasAllStones(gauntlet)) {
            beginBeamCharge(player, BeamKind.INFINITY);
        }
    }

    private static void beginBeamCharge(ServerPlayer player, BeamKind kind) {
        long now = player.server.overworld().getGameTime();
        if (CHARGING_BEAMS.putIfAbsent(player.getUUID(), new ChargingBeam(kind, now)) == null) {
            player.displayClientMessage(Component.literal("Charging " + (kind == BeamKind.POWER ? "Power Beam" :
                    kind == BeamKind.INFINITY ? "Infinity Beam" : "Mind Beam") + "…"), true);
        }
    }

    private static void releaseActivation(ServerPlayer player) {
        TimeFreezeManager.stop(player);
        ChargingBeam charge = CHARGING_BEAMS.remove(player.getUUID());
        if (charge == null) return;
        long elapsed = Math.clamp(player.server.overworld().getGameTime() - charge.startedAt(), 1L, MAX_BEAM_CHARGE_TICKS);
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        if (gauntlet.isEmpty()) return;
        String selected = InfinityGauntletStones.selectedStone(gauntlet);
        boolean valid = switch (charge.kind()) {
            case MIND -> selected.equals("mind") && InfinityGauntletStones.hasStone(gauntlet, "mind");
            case POWER -> selected.equals("power") && InfinityGauntletStones.hasStone(gauntlet, "power");
            case INFINITY -> selected.equals("snap") && InfinityGauntletStones.selectedAbility(gauntlet, "snap") == 0 && InfinityGauntletStones.isFullGauntlet(gauntlet) && InfinityGauntletStones.hasAllStones(gauntlet);
        };
        if (!valid) return;
        startBeam(player, charge.kind(), elapsed / (double) MAX_BEAM_CHARGE_TICKS);
    }

    private static RoyalBeamSettings mindBeamSettings() {
        return new RoyalBeamSettings(64.0D, 0.0D, 1.5D, 24, 3, 1, 3.0F, 6.0F,
                100.0F, 6.0F, 1, 64.0D, 0.0F, 0.0F, 0, 0.0F, 0.0F, 0.0F, false, false);
    }

    private static RoyalBeamSettings annihilationBeamSettings() {
        return new RoyalBeamSettings(64.0D, 0.0D, 2.0D, 18, 4, 1, 4.0F, 8.0F,
                250.0F, 8.0F, 1, 64.0D, 3.0F, 8.0F, 512, 1.0F, 1.0F, 1_000_000.0F, true, false);
    }

    private static RoyalBeamSettings infinityBeamSettings() {
        return new RoyalBeamSettings(96.0D, 0.0D, 1.5D, 24, 4, 1, 4.0F, 8.0F,
                250.0F, 10.0F, 1, 96.0D, 0.0F, 0.0F, 0, 0.0F, 0.0F, 0.0F, false, false);
    }

    public static void tickBeam(ServerPlayer player) {
        ChargingBeam charging = CHARGING_BEAMS.get(player.getUUID());
        if (charging != null) {
            ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
            String selected = gauntlet.isEmpty() ? "" : InfinityGauntletStones.selectedStone(gauntlet);
            boolean valid = !gauntlet.isEmpty() && switch (charging.kind()) {
                case MIND -> selected.equals("mind") && InfinityGauntletStones.hasStone(gauntlet, "mind");
                case POWER -> selected.equals("power") && InfinityGauntletStones.hasStone(gauntlet, "power");
                case INFINITY -> selected.equals("snap") && InfinityGauntletStones.selectedAbility(gauntlet, "snap") == 0 && InfinityGauntletStones.isFullGauntlet(gauntlet) && InfinityGauntletStones.hasAllStones(gauntlet);
            };
            if (!valid || !player.isAlive()) {
                CHARGING_BEAMS.remove(player.getUUID(), charging);
                return;
            }
            long elapsed = player.server.overworld().getGameTime() - charging.startedAt();
            if (elapsed >= MAX_BEAM_CHARGE_TICKS) releaseActivation(player);
            return;
        }
        ActiveBeam active = ACTIVE_BEAMS.get(player.getUUID());
        if (active == null) {
            return;
        }
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        String selected = InfinityGauntletStones.selectedStone(gauntlet);
        boolean selectedStoneValid = switch (active.kind()) {
            case MIND -> selected.equals("mind");
            case POWER -> selected.equals("power");
            case INFINITY -> selected.equals("snap") && InfinityGauntletStones.selectedAbility(gauntlet, "snap") == 0;
        };
        if (!player.isAlive() || gauntlet.isEmpty() || !selectedStoneValid
                || active.kind() == BeamKind.MIND && !InfinityGauntletStones.hasStone(gauntlet, "mind")
                || active.kind() == BeamKind.INFINITY && (!InfinityGauntletStones.isFullGauntlet(gauntlet)
                        || !InfinityGauntletStones.hasAllStones(gauntlet))) {
            cancelBeam(player);
            return;
        }
        Vec3 start = player.getEyePosition();
        active.controller().tick(start, player.getLookAngle(), null, active.settings(),
                active.kind() == BeamKind.POWER ? RoyalBeamTerrainMode.DESTROY : RoyalBeamTerrainMode.NONE, ignored -> {});
        Vec3 end = active.controller().beamEndPosition();
        if (end == null) {
            ACTIVE_BEAMS.remove(player.getUUID());
            broadcastBeam(player, active.kind(), Vec3.ZERO, Vec3.ZERO, false, 0);
            return;
        }
        broadcastBeam(player, active.kind(), start, end, true,
                active.settings().durationTicks() - active.controller().beamTicks());
        ServerLevel level = player.serverLevel();
        level.sendParticles(new DustParticleOptions(new Vector3f(0.82F, 0.62F, 1.0F),
                        active.kind() == BeamKind.POWER ? 2.8F : 1.6F),
                end.x, end.y, end.z, 5, 0.15D, 0.15D, 0.15D, 0.02D);
        if (!active.controller().isFiring()) {
            ACTIVE_BEAMS.remove(player.getUUID());
            broadcastBeam(player, active.kind(), Vec3.ZERO, Vec3.ZERO, false, 0);
        }
    }

    private static void broadcastBeam(ServerPlayer owner, BeamKind kind, Vec3 start, Vec3 end, boolean active, int age) {
        int beamKind = switch (kind) { case POWER -> 0; case INFINITY -> 1; case MIND -> 2; };
        MindBeamStatePayload payload = new MindBeamStatePayload(active, beamKind, owner.getId(), start.x, start.y, start.z,
                end.x, end.y, end.z, age);
        for (ServerPlayer viewer : owner.serverLevel().players()) {
            if (viewer.distanceToSqr(owner) <= 192.0D * 192.0D) {
                PacketDistributor.sendToPlayer(viewer, payload);
            }
        }
    }

    public static void tickReality(net.minecraft.server.MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % 20L == 0L) {
            SNAP_CONFIRMATIONS.entrySet().removeIf(entry -> now - entry.getValue() > 100L
                    || server.getPlayerList().getPlayer(entry.getKey()) == null);
        }
        for (var iterator = TEMPORARY_BLOCKS.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<TemporaryBlockKey, TemporaryBlock> entry = iterator.next();
            TemporaryBlock temporary = entry.getValue();
            ServerLevel level = server.getLevel(temporary.dimension());
            if (level == null || level.getGameTime() < temporary.expiry()) {
                continue;
            }
            BlockPos pos = entry.getKey().position();
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            if (level.getBlockState(pos) == temporary.replacement()) {
                level.setBlock(pos, temporary.original(), 3);
            }
            iterator.remove();
        }
        for (var iterator = TEMPORARY_ENTITY_EFFECTS.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<UUID, TemporaryEntityEffect> entry = iterator.next();
            TemporaryEntityEffect effect = entry.getValue();
            ServerLevel level = server.getLevel(effect.dimension());
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            boolean expired = level == null || level.getGameTime() >= effect.expiry();
            if (!expired) {
                continue;
            }
            if (entity == null) {
                for (ServerLevel candidate : server.getAllLevels()) {
                    entity = candidate.getEntity(entry.getKey());
                    if (entity != null) break;
                }
            }
            if (entity == null) {
                continue;
            }
            if (effect.gravity() != null && entity instanceof LivingEntity living
                    && AntarchyGravityApi.getGravityDirection(living) == AntarchyGravityDirection.UP) {
                AntarchyGravityApi.setGravityDirection(living, effect.gravity());
                AntarchyGravityApi.notifyGravityStateChanged(living);
            }
            if (effect.scaleOffset() != null && entity instanceof LivingEntity living) {
                AttributeInstance scale = living.getAttribute(Attributes.SCALE);
                if (scale != null && scale.getModifier(SizeRayProjectileEntity.SIZE_RAY_MODIFIER_ID) != null) {
                    SizeRayProjectileEntity.writeModifier(scale, effect.previousScaleOffset());
                }
            }
            iterator.remove();
        }
    }

    public static void clearReality(net.minecraft.server.MinecraftServer server) {
        for (Map.Entry<TemporaryBlockKey, TemporaryBlock> entry : TEMPORARY_BLOCKS.entrySet()) {
            TemporaryBlock temporary = entry.getValue();
            ServerLevel level = server.getLevel(temporary.dimension());
            BlockPos pos = entry.getKey().position();
            if (level != null && level.hasChunkAt(pos) && level.getBlockState(pos) == temporary.replacement()) {
                level.setBlock(pos, temporary.original(), 3);
            }
        }
        TEMPORARY_BLOCKS.clear();
        for (Map.Entry<UUID, TemporaryEntityEffect> entry : TEMPORARY_ENTITY_EFFECTS.entrySet()) {
            TemporaryEntityEffect effect = entry.getValue();
            ServerLevel level = server.getLevel(effect.dimension());
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (effect.gravity() != null && AntarchyGravityApi.getGravityDirection(living) == AntarchyGravityDirection.UP) {
                AntarchyGravityApi.setGravityDirection(living, effect.gravity());
                AntarchyGravityApi.notifyGravityStateChanged(living);
            }
            if (effect.scaleOffset() != null) {
                AttributeInstance scale = living.getAttribute(Attributes.SCALE);
                if (scale != null && scale.getModifier(SizeRayProjectileEntity.SIZE_RAY_MODIFIER_ID) != null) {
                    SizeRayProjectileEntity.writeModifier(scale, effect.previousScaleOffset());
                }
            }
        }
        TEMPORARY_ENTITY_EFFECTS.clear();
    }

    private static void activateReality(ServerPlayer player, ItemStack gauntlet, int ability) {
        if (ability == 2) {
            player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.reality_size_controls"), true);
            return;
        }
        if (ability == 0) {
            Vec3 start = player.getEyePosition();
            Vec3 end = start.add(player.getLookAngle().normalize().scale(24.0D));
            BlockHitResult hit = player.serverLevel().clip(new ClipContext(start, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.BLOCK) {
                player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.reality_no_block"), true);
                return;
            }
            BlockPos pos = hit.getBlockPos().immutable();
            TemporaryBlockKey blockKey = new TemporaryBlockKey(player.serverLevel().dimension(), pos);
            BlockState original = player.serverLevel().getBlockState(pos);
            TemporaryBlock currentRewrite = TEMPORARY_BLOCKS.get(blockKey);
            int nextCycleIndex = 0;
            if (currentRewrite != null && original == currentRewrite.replacement()) {
                original = currentRewrite.original();
                nextCycleIndex = (currentRewrite.cycleIndex() + 1) % REALITY_BLOCK_CYCLE.size();
            } else {
                for (int index = 0; index < REALITY_BLOCK_CYCLE.size(); index++) {
                    if (original == REALITY_BLOCK_CYCLE.get(index)) {
                        nextCycleIndex = (index + 1) % REALITY_BLOCK_CYCLE.size();
                        break;
                    }
                }
            }
            if (original.isAir() || original.hasBlockEntity()) {
                player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.reality_protected"), true);
                return;
            }
            BlockState replacement = REALITY_BLOCK_CYCLE.get(nextCycleIndex);
            if (!TEMPORARY_BLOCKS.containsKey(blockKey) && TEMPORARY_BLOCKS.size() >= MAX_TEMPORARY_BLOCK_REWRITES) {
                player.displayClientMessage(Component.literal("Too many temporary Reality rewrites are active"), true);
                return;
            }
            if (player.serverLevel().setBlock(pos, replacement, 3)) {
                TEMPORARY_BLOCKS.put(blockKey, new TemporaryBlock(player.serverLevel().dimension(), original,
                        replacement, nextCycleIndex, player.serverLevel().getGameTime() + 200L));
                player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.reality_rewrite"), true);
            }
            return;
        }
        LivingEntity target = aimedLivingTarget(player, 32.0D);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.reality_no_entity"), true);
            return;
        }
        if (ability == 1) {
            TemporaryEntityEffect previousEffect = TEMPORARY_ENTITY_EFFECTS.get(target.getUUID());
            AntarchyGravityDirection original = previousEffect != null && previousEffect.gravity() != null
                    ? previousEffect.gravity() : AntarchyGravityApi.getGravityDirection(target);
            if (original != AntarchyGravityDirection.UP) {
                AntarchyGravityApi.setGravityDirection(target, AntarchyGravityDirection.UP);
                AntarchyGravityApi.notifyGravityStateChanged(target);
            }
            TEMPORARY_ENTITY_EFFECTS.put(target.getUUID(), new TemporaryEntityEffect(target.level().dimension(),
                    player.serverLevel().getGameTime() + 300L, original, null, 0.0D));
            player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.reality_gravity"), true);
        }
    }

    private static LivingEntity aimedLivingTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize().scale(range));
        BlockHitResult blockHit = player.serverLevel().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? range : start.distanceTo(blockHit.getLocation());
        AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player,
                start, end, search, entity -> entity instanceof LivingEntity && entity.isPickable() && entity != player,
                maxDistance * maxDistance);
        return hit != null && hit.getEntity() instanceof LivingEntity living
                && start.distanceToSqr(hit.getLocation()) <= maxDistance * maxDistance ? living : null;
    }

    private static void activateMind(ServerPlayer player, ItemStack gauntlet, int ability) {
        if (isAbilityOnCooldown(player)) {
            return;
        }
        if (ability == 0) {
            LivingEntity target = aimedLivingTarget(player, 48.0D);
            if (target == null || !MindControlManager.control(player, target)) {
                player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.mind_no_target"), true);
                return;
            }
            addAbilityCooldown(player, 300);
            player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.mind_command", target.getDisplayName()), true);
            return;
        }
        if (ability == 1) {
            LivingEntity target = aimedLivingTarget(player, 48.0D);
            if (target == null) {
                List<LivingEntity> sensed = player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(32.0D), entity -> entity.isAlive() && entity != player);
                for (LivingEntity entity : sensed) {
                    entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, true, true), player);
                }
                addAbilityCooldown(player, 200);
                player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.mind_sense", sensed.size()), true);
                return;
            }
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, true, true), player);
            addAbilityCooldown(player, 200);
            player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.mind_sight_target",
                    target.getDisplayName(), String.format("%.1f", target.getHealth()),
                    String.format("%.1f", target.getMaxHealth()), target.getArmorValue()), true);
            return;
        }
        beginBeamCharge(player, BeamKind.MIND);
    }

    private static void activateSoul(ServerPlayer player, ItemStack gauntlet, int ability) {
        LivingEntity target = ability == 2 && SoulStoneManager.isPending(player)
                ? player : aimedLivingTarget(player, 32.0D);
        if (target == null) {
            player.displayClientMessage(Component.literal("Aim at a living entity within 32 blocks"), true);
            return;
        }
        if (ability == 2 && SoulStoneManager.isPending(target)) {
            SoulStoneManager.fate(player, target);
            addAbilityCooldown(player, 200);
            return;
        }
        if (isAbilityOnCooldown(player)) return;
        if (ability == 0) {
            SoulStoneManager.exchange(player, target);
            addAbilityCooldown(player, 100);
        } else if (ability == 1) {
            SoulStoneManager.sever(player, target);
            addAbilityCooldown(player, 240);
        } else {
            SoulStoneManager.fate(player, target);
            addAbilityCooldown(player, 200);
        }
    }

    private record TemporaryBlockKey(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos position) {}

    private record TemporaryBlock(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            BlockState original, BlockState replacement, int cycleIndex, long expiry) {}

    private record TemporaryEntityEffect(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            long expiry, AntarchyGravityDirection gravity, Double scaleOffset, double previousScaleOffset) {}

    public static void cancelBeam(ServerPlayer player) {
        CHARGING_BEAMS.remove(player.getUUID());
        TimeFreezeManager.stop(player);
        ActiveBeam active = ACTIVE_BEAMS.remove(player.getUUID());
        if (active != null) {
            active.controller().stop();
            broadcastBeam(player, active.kind(), Vec3.ZERO, Vec3.ZERO, false, 0);
        }
    }

    private record AbilityCooldownKey(UUID playerId, String stone, int ability) {}
    private record Cooldown(long endsAt, int duration) {}

    private enum BeamKind { POWER, INFINITY, MIND }

    private record ActiveBeam(RoyalBeamController controller, RoyalBeamSettings settings, BeamKind kind) {}
    private record ChargingBeam(BeamKind kind, long startedAt) {}

    private static void activateLifeHalvingSnap(ServerPlayer player, ItemStack gauntlet) {
        long gameTime = player.server.overworld().getGameTime();
        Long confirmation = SNAP_CONFIRMATIONS.get(player.getUUID());
        if (confirmation == null || gameTime - confirmation > 100L) {
            SNAP_CONFIRMATIONS.put(player.getUUID(), gameTime);
            player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.snap_confirm"), true);
            return;
        }
        SNAP_CONFIRMATIONS.remove(player.getUUID());

        List<LivingEntity> loadedLivingEntities = new ArrayList<>();
        for (ServerLevel level : player.server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    loadedLivingEntities.add(living);
                }
            }
        }
        for (int index = loadedLivingEntities.size() - 1; index > 0; index--) {
            int swap = player.getRandom().nextInt(index + 1);
            LivingEntity entity = loadedLivingEntities.get(index);
            loadedLivingEntities.set(index, loadedLivingEntities.get(swap));
            loadedLivingEntities.set(swap, entity);
        }
        int victims = loadedLivingEntities.size() / 2;
        for (int index = 0; index < victims; index++) {
            LivingEntity target = loadedLivingEntities.get(index);
            if (target.level() instanceof ServerLevel level && target.isAlive()) {
                spawnSnapDust(level, target);
                HealingFactor.snap(() -> target.hurt(target.damageSources().genericKill(), 1.0E9F));
            }
        }
        player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.snap_result", victims), true);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.craisinlord.integrated_marvel.api.GauntletSnapEvent(player, victims));
        addAbilityCooldown(player, 1200);
    }

    private static void spawnSnapDust(ServerLevel level, LivingEntity target) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        double spread = Math.max(0.25D, Math.min(0.8D, target.getBbWidth() * 0.55D));
        level.sendParticles(ParticleTypes.ASH, center.x, center.y, center.z, 28,
                spread, target.getBbHeight() * 0.35D, spread, 0.035D);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 10,
                spread * 0.65D, target.getBbHeight() * 0.25D, spread * 0.65D, 0.02D);
        level.playSound(null, target.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), target.getSoundSource(), 0.7F, 0.75F);
    }

    public static void clearSessionState() {
        SNAP_CONFIRMATIONS.clear();
        ABILITY_COOLDOWNS.clear();
        CHARGING_BEAMS.clear();
        for (ActiveBeam active : ACTIVE_BEAMS.values()) active.controller().stop();
        ACTIVE_BEAMS.clear();
    }

    public static void clearPlayerSession(UUID playerId) {
        SNAP_CONFIRMATIONS.remove(playerId);
        ABILITY_COOLDOWNS.keySet().removeIf(key -> key.playerId().equals(playerId));
        CHARGING_BEAMS.remove(playerId);
    }

    private static void adjustTargetTime(ServerPlayer player, double delta) {
        if (!canAdjustTargetTime(player) || !Double.isFinite(delta)) {
            return;
        }
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize().scale(32.0D));
        BlockHitResult blockHit = player.serverLevel().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? 32.0D : start.distanceTo(blockHit.getLocation());
        AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, start, end, search, entity -> entity instanceof LivingEntity && entity.isPickable() && entity != player,
                maxDistance * maxDistance
        );
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)
                || start.distanceToSqr(hit.getLocation()) > maxDistance * maxDistance
                || !(target instanceof TimeDilationEntityAccess access)) {
            player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.time_no_target"), true);
            return;
        }
        double currentRate = access.antarchy$getInheritedTimeDilationRate();
        double adjustedRate = Math.clamp(currentRate + Math.signum(delta) * 0.05D, 0.05D, 3.0D);
        TimeDilationApi.setInheritedRate(target, adjustedRate);
        TimeDilationManager.trackPotentiallyAffected(target);
        TimeDilationApi.syncEntityRate(target, TimeDilationApi.getRate(target));
        TimeRateTuningManager.hold(player, target);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(),
                player.getSoundSource(), 0.5F, 1.4F);
        player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.time_rate", target.getDisplayName(), Math.round(adjustedRate * 100.0D)), true);
    }

    private static void adjustTargetSize(ServerPlayer player, double delta) {
        if (!canAdjustTargetSize(player) || !Double.isFinite(delta)) return;
        LivingEntity target = aimedLivingTarget(player, 32.0D);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.reality_no_entity"), true);
            return;
        }
        AttributeInstance scale = target.getAttribute(Attributes.SCALE);
        if (scale == null) return;
        var existing = scale.getModifier(SizeRayProjectileEntity.SIZE_RAY_MODIFIER_ID);
        double currentOffset = existing == null ? 0.0D : existing.amount();
        double currentScale = Math.clamp(1.0D + currentOffset, MIN_REALITY_SCALE, MAX_REALITY_SCALE);
        double nextScale = Math.clamp(currentScale * Math.pow(1.25D, Math.signum(delta)), MIN_REALITY_SCALE, MAX_REALITY_SCALE);
        double nextOffset = nextScale - 1.0D;
        SizeRayProjectileEntity.writeModifier(scale, nextOffset);
        player.displayClientMessage(Component.translatable("message.integrated_marvel.gauntlet.reality_size", target.getDisplayName(),
                String.format(java.util.Locale.ROOT, "%.2f", nextScale)), true);
    }
}
