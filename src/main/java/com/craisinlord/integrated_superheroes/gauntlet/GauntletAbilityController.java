package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.anthology.beam.BeamController;
import com.craisinlord.anthology.beam.BeamSettings;
import com.craisinlord.anthology.beam.BeamTerrainMode;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
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
    public static final int ABILITY_SCROLL = 4;
    public static final int ACTIVATE_START = 5;
    public static final int ACTIVATE_RELEASE = 6;
    public static final int ACTIVATE = 7;
    private static final int MAX_BEAM_CHARGE_TICKS = 40;
    private static final Map<AbilityCooldownKey, Cooldown> ABILITY_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveBeam> ACTIVE_BEAMS = new ConcurrentHashMap<>();
    private static final Map<UUID, ChargingBeam> CHARGING_BEAMS = new ConcurrentHashMap<>();

    public enum BeamKind { POWER, INFINITY, MIND }

    private GauntletAbilityController() {
    }

    public static boolean isAbilityOnCooldown(Player player) {
        Cooldown cooldown = ABILITY_COOLDOWNS.get(abilityCooldownKey(player));
        return cooldown != null && cooldown.endsAt() > cooldownClock(player);
    }

    public static boolean isAbilityOnCooldown(Player player, String stone, int index) {
        Cooldown cooldown = ABILITY_COOLDOWNS.get(new AbilityCooldownKey(player.getUUID(), stone, index + 1));
        return cooldown != null && cooldown.endsAt() > cooldownClock(player);
    }

    public static void addAbilityCooldown(Player player, int ticks) {
        AbilityCooldownKey key = abilityCooldownKey(player);
        addAbilityCooldown(player, key.stone(), key.ability() - 1, ticks);
    }

    public static void addAbilityCooldown(Player player, String stone, int index, int ticks) {
        if (ticks <= 0) return;
        AbilityCooldownKey key = new AbilityCooldownKey(player.getUUID(), stone, index + 1);
        ABILITY_COOLDOWNS.put(key, new Cooldown(cooldownClock(player) + ticks, ticks));
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new GauntletCooldownPayload(stone, index + 1, ticks));
        }
    }

    public static void clearAbilityCooldown(ServerPlayer player, String stone, int index) {
        if (ABILITY_COOLDOWNS.remove(new AbilityCooldownKey(player.getUUID(), stone, index + 1)) != null) {
            PacketDistributor.sendToPlayer(player, new GauntletCooldownPayload(stone, index + 1, 0));
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

    public static GauntletCast cast(ServerPlayer player, ItemStack gauntlet, GauntletAbility ability) {
        return new GauntletCast(player, gauntlet, ability, GauntletAbilities.infinity(gauntlet), player.isShiftKeyDown());
    }

    public static void handleInput(ServerPlayer player, int action, double delta) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        if (gauntlet.isEmpty()) return;
        switch (action) {
            case PREVIOUS_STONE -> cycleStone(player, gauntlet, -1);
            case NEXT_STONE -> cycleStone(player, gauntlet, 1);
            case PREVIOUS_ABILITY -> cycleAbility(player, gauntlet, -1);
            case NEXT_ABILITY -> cycleAbility(player, gauntlet, 1);
            case ABILITY_SCROLL -> scroll(player, gauntlet, delta);
            case ACTIVATE_START -> beginHeldActivation(player);
            case ACTIVATE_RELEASE -> releaseActivation(player);
            case ACTIVATE -> activate(player);
            default -> {
            }
        }
    }

    public static void activate(ServerPlayer player) {
        if (MindLinkManager.isLinked(player.getUUID())) {
            MindLinkManager.end(player.server, player.getUUID(), "mind_link_end");
            return;
        }
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        GauntletAbility ability = GauntletAbilities.selected(gauntlet);
        if (ability == null || ability.held() || ability.handler() == null) return;
        GauntletCast cast = cast(player, gauntlet, ability);
        if (!ability.checksOwnCooldown() && cast.onCooldown()) return;
        ability.handler().accept(cast);
    }

    public static boolean usesHeldActivation(ItemStack gauntlet) {
        GauntletAbility ability = GauntletAbilities.selected(gauntlet);
        return ability != null && ability.held();
    }

    public static boolean canScroll(Player player, boolean aimingAtLiving) {
        GauntletAbility ability = GauntletAbilities.selected(InfinityGauntletStones.equippedGauntlet(player));
        if (ability == null || ability.scrollHandler() == null) return false;
        return ability.scroll() == GauntletAbility.Scroll.ANY
                || ability.scroll() == GauntletAbility.Scroll.ENTITY && aimingAtLiving;
    }

    private static void scroll(ServerPlayer player, ItemStack gauntlet, double delta) {
        if (!Double.isFinite(delta) || delta == 0.0D) return;
        GauntletAbility ability = GauntletAbilities.selected(gauntlet);
        if (ability == null || ability.scrollHandler() == null) return;
        ability.scrollHandler().accept(cast(player, gauntlet, ability), Math.signum(delta));
    }

    private static void cycleStone(ServerPlayer player, ItemStack gauntlet, int direction) {
        List<String> installed = new ArrayList<>();
        for (String stone : GauntletAbilities.STONES) {
            if (GauntletAbilities.unlocked(gauntlet, stone) && GauntletConfig.hasEnabledAbility(stone)) installed.add(stone);
        }
        if (installed.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.gauntlet.no_stones"), true);
            return;
        }
        int current = installed.indexOf(InfinityGauntletStones.selectedStone(gauntlet));
        if (current < 0) current = direction > 0 ? -1 : 0;
        String next = installed.get(Math.floorMod(current + direction, installed.size()));
        InfinityGauntletStones.setSelectedStone(gauntlet, next);
        int ability = InfinityGauntletStones.selectedAbility(gauntlet, next);
        if (!GauntletConfig.isEnabled(next, ability)) {
            ability = firstEnabledAbility(next);
            InfinityGauntletStones.setSelectedAbility(gauntlet, next, ability);
        }
        sendSelectionToast(player, next, ability);
    }

    private static void cycleAbility(ServerPlayer player, ItemStack gauntlet, int direction) {
        String stone = InfinityGauntletStones.selectedStone(gauntlet);
        int count = GauntletAbilities.count(stone);
        if (count == 0 || !GauntletAbilities.unlocked(gauntlet, stone)) return;
        int current = InfinityGauntletStones.selectedAbility(gauntlet, stone);
        int next = current;
        for (int attempt = 0; attempt < count; attempt++) {
            next = Math.floorMod(next + direction, count);
            if (GauntletConfig.isEnabled(stone, next)) break;
        }
        if (!GauntletConfig.isEnabled(stone, next)) return;
        InfinityGauntletStones.setSelectedAbility(gauntlet, stone, next);
        sendSelectionToast(player, stone, next);
    }

    private static int firstEnabledAbility(String stone) {
        for (int ability = 0; ability < GauntletAbilities.count(stone); ability++) {
            if (GauntletConfig.isEnabled(stone, ability)) return ability;
        }
        return 0;
    }

    private static void sendSelectionToast(ServerPlayer player, String stone, int ability) {
        PacketDistributor.sendToPlayer(player, new GauntletSelectionToastPayload(stone, ability));
    }

    private static void beginHeldActivation(ServerPlayer player) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        GauntletAbility ability = GauntletAbilities.selected(gauntlet);
        if (ability == null || !ability.held() || ability.handler() == null) return;
        ability.handler().accept(cast(player, gauntlet, ability));
    }

    public static void beginBeamCharge(GauntletCast cast, BeamKind kind) {
        if (cast.onCooldown()) return;
        ServerPlayer player = cast.player();
        long now = player.server.overworld().getGameTime();
        if (CHARGING_BEAMS.putIfAbsent(player.getUUID(), new ChargingBeam(kind, now, cast.ability(), cast.crouching(),
                cast.infinity())) == null) {
            GauntletFx.cast(player, cast.ability().stone(), SoundEvents.BEACON_POWER_SELECT, 0.6F);
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.gauntlet.charging",
                    Component.translatable("tooltip.integrated_superheroes.gauntlet.ability." + cast.ability().translationSuffix())), true);
        }
    }

    private static void releaseActivation(ServerPlayer player) {
        TimeFreezeManager.stopHeld(player);
        ChargingBeam charge = CHARGING_BEAMS.remove(player.getUUID());
        if (charge == null) return;
        long elapsed = Math.clamp(player.server.overworld().getGameTime() - charge.startedAt(), 1L, MAX_BEAM_CHARGE_TICKS);
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        if (!stillSelected(gauntlet, charge.ability())) return;
        startBeam(player, charge, elapsed / (double) MAX_BEAM_CHARGE_TICKS);
        addAbilityCooldown(player, charge.ability().stone(), charge.ability().index(),
                charge.ability().cooldownTicks(charge.infinity()));
    }

    private static boolean stillSelected(ItemStack gauntlet, GauntletAbility ability) {
        GauntletAbility selected = GauntletAbilities.selected(gauntlet);
        return selected != null && selected.stone().equals(ability.stone()) && selected.index() == ability.index();
    }

    private static void startBeam(ServerPlayer player, ChargingBeam charging, double charge) {
        BeamKind kind = charging.kind();
        BeamSettings base = kind == BeamKind.INFINITY ? infinityBeamSettings()
                : kind == BeamKind.MIND ? mindBeamSettings() : annihilationBeamSettings();
        double power = GauntletConfig.powerMultiplier() * (charging.infinity() ? 2.0D : 1.0D);
        boolean wide = kind == BeamKind.POWER && charging.wide();
        double width = wide ? 2.25D : 1.0D;
        double strength = (0.35D + Math.clamp(charge, 0.0D, 1.0D) * 0.65D) * power;
        double reach = (0.5D + 0.5D * charge) * (charging.infinity() ? 2.0D : 1.0D);
        float resistance = charging.infinity() ? Float.MAX_VALUE : base.maxTerrainResistance();
        BeamSettings settings = new BeamSettings(
                base.range() * reach, base.targetTracking(), base.pathStep(),
                Math.max(6, (int) Math.round(base.durationTicks() * (0.5D + 0.5D * charge) * (wide ? 1.5D : 1.0D))),
                base.travelTicks(), base.cooldownTicks(), (float) (base.pathDamageRadius() * strength * width),
                (float) (base.impactDamageRadius() * strength * width), (float) (base.damage() * strength),
                (float) (base.knockback() * strength), base.terrainIntervalTicks(), base.maxTerrainDistance() * reach,
                (float) (base.pathTerrainRadius() * strength * width), (float) (base.impactTerrainRadius() * strength * width),
                Math.max(0, (int) Math.round(base.maxTerrainMutationsPerTick() * strength * width)), base.terrainMutationChance(),
                base.terrainDropChance(), resistance, base.terrainEnabled() && GauntletConfig.griefing(),
                base.requireLineOfSightForDamage());
        BeamController controller = new BeamController(player);
        Vec3 start = player.getEyePosition();
        controller.start(start, start.add(player.getLookAngle().normalize().scale(settings.range())), settings.durationTicks());
        ACTIVE_BEAMS.put(player.getUUID(), new ActiveBeam(controller, settings, kind, charging.ability(), new HashSet<>()));
        GauntletFx.shake(player.serverLevel(), player.position(), (float) (0.4D + charge), 12, 24.0F);
    }

    private static BeamSettings mindBeamSettings() {
        return new BeamSettings(64.0D, 0.0D, 1.5D, 24, 3, 1, 3.0F, 6.0F,
                100.0F, 6.0F, 1, 64.0D, 0.0F, 0.0F, 0, 0.0F, 0.0F, 0.0F, false, false);
    }

    private static BeamSettings annihilationBeamSettings() {
        return new BeamSettings(64.0D, 0.0D, 2.0D, 18, 4, 1, 4.0F, 8.0F,
                250.0F, 8.0F, 1, 64.0D, 3.0F, 8.0F, 512, 1.0F, 1.0F, 1_000_000.0F, true, false);
    }

    private static BeamSettings infinityBeamSettings() {
        return new BeamSettings(96.0D, 0.0D, 1.5D, 24, 4, 1, 4.0F, 8.0F,
                250.0F, 10.0F, 1, 96.0D, 0.0F, 0.0F, 0, 0.0F, 0.0F, 0.0F, false, false);
    }

    public static void tickBeam(ServerPlayer player) {
        ChargingBeam charging = CHARGING_BEAMS.get(player.getUUID());
        if (charging != null) {
            ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
            if (!player.isAlive() || !stillSelected(gauntlet, charging.ability())) {
                CHARGING_BEAMS.remove(player.getUUID(), charging);
                return;
            }
            long elapsed = player.server.overworld().getGameTime() - charging.startedAt();
            if (elapsed % 4L == 0L) {
                GauntletFx.sphere(player.serverLevel(), player.getEyePosition().add(player.getLookAngle()),
                        0.3D + elapsed / (double) MAX_BEAM_CHARGE_TICKS, charging.ability().stone(), 6);
            }
            if (elapsed >= MAX_BEAM_CHARGE_TICKS) releaseActivation(player);
            return;
        }
        ActiveBeam active = ACTIVE_BEAMS.get(player.getUUID());
        if (active == null) return;
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        if (!player.isAlive() || !stillSelected(gauntlet, active.ability())) {
            cancelBeam(player);
            return;
        }
        Vec3 start = player.getEyePosition();
        active.controller().tick(start, player.getLookAngle(), null, active.settings(),
                active.kind() == BeamKind.POWER ? BeamTerrainMode.DESTROY : BeamTerrainMode.NONE, ignored -> {});
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
        if (active.kind() == BeamKind.MIND) MindStone.beamImpact(player, end, active.affected());
        if (active.kind() == BeamKind.INFINITY) SnapStone.beamImpact(player, end, active.affected());
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

    public static void cancelBeam(ServerPlayer player) {
        CHARGING_BEAMS.remove(player.getUUID());
        TimeFreezeManager.stopHeld(player);
        ActiveBeam active = ACTIVE_BEAMS.remove(player.getUUID());
        if (active != null) {
            active.controller().stop();
            broadcastBeam(player, active.kind(), Vec3.ZERO, Vec3.ZERO, false, 0);
        }
    }

    public static void clearSessionState() {
        ABILITY_COOLDOWNS.clear();
        CHARGING_BEAMS.clear();
        for (ActiveBeam active : ACTIVE_BEAMS.values()) active.controller().stop();
        ACTIVE_BEAMS.clear();
    }

    public static void clearPlayerLogout(UUID playerId) {
        CHARGING_BEAMS.remove(playerId);
    }

    public static void clearPlayerSession(UUID playerId) {
        ABILITY_COOLDOWNS.keySet().removeIf(key -> key.playerId().equals(playerId));
        CHARGING_BEAMS.remove(playerId);
    }

    private record AbilityCooldownKey(UUID playerId, String stone, int ability) {}
    private record Cooldown(long endsAt, int duration) {}
    private record ActiveBeam(BeamController controller, BeamSettings settings, BeamKind kind, GauntletAbility ability,
                              Set<UUID> affected) {}
    private record ChargingBeam(BeamKind kind, long startedAt, GauntletAbility ability, boolean wide, boolean infinity) {}
}
