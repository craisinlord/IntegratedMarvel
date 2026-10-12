package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HealingFactor {
    public static final String WOLVERINE = "weapon_x_logan";
    private static final ResourceLocation DOWNED_MODIFIER = ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "healing_factor_downed");
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final ThreadLocal<Boolean> SNAPPING = ThreadLocal.withInitial(() -> false);

    private HealingFactor() {
    }

    private static final class State {
        private int stacks;
        private long lastStackChange;
        private boolean downed;
        private boolean ready;
        private boolean snapped;
        private long downedUntil;
        private long readyUntil;
        private float standHealth;
        private long regenPausedUntil;
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, HealingFactor::onPlayerDeath);
        NeoForge.EVENT_BUS.addListener(HealingFactor::onHeal);
        NeoForge.EVENT_BUS.addListener(HealingFactor::onTarget);
        NeoForge.EVENT_BUS.addListener(HealingFactor::onAttack);
        NeoForge.EVENT_BUS.addListener(HealingFactor::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(HealingFactor::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(HealingFactor::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(HealingFactor::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(HealingFactor::onLogout);
    }

    public static boolean covers(Player player) {
        String primary = HeroPowers.data(player).getPrimaryPower();
        return DeadpoolPower.ID.equals(primary) || WOLVERINE.equals(primary);
    }

    private static boolean isDeadpool(Player player) {
        return DeadpoolPower.ID.equals(HeroPowers.data(player).getPrimaryPower());
    }

    private static List<ModConfigSpec.DoubleValue> downedSeconds(Player player) {
        return isDeadpool(player) ? MarvelBalanceConfig.DEADPOOL_DOWNED_SECONDS : MarvelBalanceConfig.WOLVERINE_DOWNED_SECONDS;
    }

    private static List<ModConfigSpec.DoubleValue> standHealth(Player player) {
        return isDeadpool(player) ? MarvelBalanceConfig.DEADPOOL_STAND_HEALTH : MarvelBalanceConfig.WOLVERINE_STAND_HEALTH;
    }

    public static int maxRevives(Player player) {
        return downedSeconds(player).size();
    }

    private static long now(ServerPlayer player) {
        return player.server.getTickCount();
    }

    public static boolean isDowned(Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.downed;
    }

    public static int stacks(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return 0;
        }
        decay(state, now(player));
        return state.stacks;
    }

    public static boolean regenPaused(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && (state.downed || now(player) < state.regenPausedUntil);
    }

    public static void snap(Runnable kill) {
        boolean previous = SNAPPING.get();
        SNAPPING.set(true);
        try {
            kill.run();
        } finally {
            SNAPPING.set(previous);
        }
    }

    private static void decay(State state, long now) {
        long period = (long) (MarvelBalanceConfig.HEALING_FACTOR_STACK_DECAY_SECONDS.get() * 20.0D);
        while (!state.downed && state.stacks > 0 && now - state.lastStackChange >= period) {
            state.stacks--;
            state.lastStackChange += period;
        }
    }

    private static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            onDeath(player, event);
        }
    }

    public static boolean onDeath(ServerPlayer player, LivingDeathEvent event) {
        if (event.isCanceled()) {
            return false;
        }
        if (!covers(player)) {
            STATES.remove(player.getUUID());
            return false;
        }
        long now = now(player);
        State state = STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
        decay(state, now);
        DamageSource source = event.getSource();
        boolean snap = SNAPPING.get();
        boolean sticks = !snap && source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (state.downed && !sticks) {
            event.setCanceled(true);
            player.setHealth(1.0F);
            return true;
        }
        if (sticks || state.stacks >= maxRevives(player)) {
            realDeath(player, source);
            return false;
        }
        event.setCanceled(true);
        state.stacks++;
        state.lastStackChange = now;
        enterDowned(player, state, snap, source, now);
        return true;
    }

    private static void realDeath(ServerPlayer player, DamageSource source) {
        State state = STATES.remove(player.getUUID());
        if (state != null && state.downed) {
            release(player);
        }
        if (isDeadpool(player)) {
            PacketDistributor.sendToPlayer(player, new DeadpoolDownedPayload(DeadpoolDownedPayload.UP));
            DeadpoolNarration.say(player, "real_death");
            DeadpoolAdvancements.award(player, DeadpoolAdvancements.OKAY_FINE);
            if (source.getEntity() instanceof Bee) {
                DeadpoolAdvancements.award(player, DeadpoolAdvancements.NOT_THE_HERO);
            }
        }
    }

    private static void enterDowned(ServerPlayer player, State state, boolean snap, DamageSource source, long now) {
        int index = Math.min(state.stacks, maxRevives(player)) - 1;
        double seconds = snap ? MarvelBalanceConfig.HEALING_FACTOR_SNAP_SECONDS.get() : downedSeconds(player).get(index).get();
        state.downed = true;
        state.ready = false;
        state.snapped = snap;
        state.standHealth = standHealth(player).get(index).get().floatValue();
        state.downedUntil = now + Math.max(1L, (long) (seconds * 20.0D));
        player.setHealth(1.0F);
        player.clearFire();
        player.stopUsingItem();
        player.setForcedPose(Pose.SWIMMING);
        hold(player);
        dropTargets(player);
        if (snap) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int) (state.downedUntil - now) + 5, 0, false, false, false));
            player.serverLevel().sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.4D, 0.8D, 0.4D, 0.03D);
        }
        if (!isDeadpool(player)) {
            return;
        }
        if (source.getEntity() instanceof Bee) {
            DeadpoolAdvancements.award(player, DeadpoolAdvancements.NOT_THE_HERO);
        }
        if (snap) {
            DeadpoolNarration.say(player, "snap");
            return;
        }
        PacketDistributor.sendToPlayer(player, new DeadpoolDownedPayload(DeadpoolDownedPayload.DOWNED));
        DeadpoolAdvancements.award(player, DeadpoolAdvancements.NAH);
        DeadpoolNarration.say(player, "downed");
    }

    private static void hold(ServerPlayer player) {
        AttributeModifier freeze = new AttributeModifier(DOWNED_MODIFIER, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrUpdateTransientModifier(freeze);
        }
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) {
            jump.addOrUpdateTransientModifier(freeze);
        }
    }

    private static void release(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(DOWNED_MODIFIER);
        }
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) {
            jump.removeModifier(DOWNED_MODIFIER);
        }
        player.setForcedPose(null);
    }

    private static void dropTargets(ServerPlayer player) {
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48.0D), mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
        }
    }

    public static void tick(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null || !state.downed) {
            return;
        }
        if (!covers(player)) {
            stand(player, state, false);
            STATES.remove(player.getUUID());
            return;
        }
        long now = now(player);
        player.setForcedPose(Pose.SWIMMING);
        if (player.tickCount % 10 == 0) {
            dropTargets(player);
        }
        if (!state.ready && now >= state.downedUntil) {
            if (isDeadpool(player) && !state.snapped) {
                state.ready = true;
                state.readyUntil = now + (long) (MarvelBalanceConfig.HEALING_FACTOR_NAH_TIMEOUT_SECONDS.get() * 20.0D);
                PacketDistributor.sendToPlayer(player, new DeadpoolDownedPayload(DeadpoolDownedPayload.READY));
                DeadpoolNarration.say(player, "death_screen");
            } else {
                stand(player, state, true);
            }
        } else if (state.ready && now >= state.readyUntil) {
            stand(player, state, true);
        }
    }

    public static void requestStand(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state != null && state.downed && state.ready) {
            stand(player, state, true);
        }
    }

    private static void stand(ServerPlayer player, State state, boolean announce) {
        state.downed = false;
        state.ready = false;
        release(player);
        player.setHealth(Math.max(1.0F, player.getMaxHealth() * state.standHealth));
        state.regenPausedUntil = now(player) + (long) (MarvelBalanceConfig.HEALING_FACTOR_REGEN_PAUSE_SECONDS.get() * 20.0D);
        if (state.snapped) {
            player.removeEffect(MobEffects.INVISIBILITY);
            player.serverLevel().sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.4D, 0.8D, 0.4D, -0.03D);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BREATH, player.getSoundSource(), 1.0F, 0.8F);
        if (!isDeadpool(player)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new DeadpoolDownedPayload(DeadpoolDownedPayload.UP));
        if (!announce) {
            return;
        }
        boolean last = state.stacks >= maxRevives(player);
        DeadpoolNarration.say(player, last ? "last_revive" : "stand");
        DeadpoolAdvancements.award(player, DeadpoolAdvancements.UNALIVE);
        if (last) {
            DeadpoolAdvancements.award(player, DeadpoolAdvancements.REGENERATING_DEGENERATE);
        }
    }

    public static void clear(ServerPlayer player) {
        State state = STATES.remove(player.getUUID());
        if (state != null && state.downed) {
            stand(player, state, false);
        }
    }

    public static boolean blocksDamage(LivingIncomingDamageEvent event) {
        return event.getEntity() instanceof ServerPlayer player && isDowned(player)
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    private static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && regenPaused(player)) {
            event.setCanceled(true);
        }
    }

    private static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer player && isDowned(player)) {
            event.setCanceled(true);
        }
    }

    private static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isDowned(player)) {
            event.setCanceled(true);
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && isDowned(player)) {
            event.setCanceled(true);
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && isDowned(player)) {
            event.setCanceled(true);
        }
    }

    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && isDowned(player)) {
            event.setCanceled(true);
        }
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && isDowned(player)) {
            event.setCanceled(true);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            State state = STATES.get(player.getUUID());
            if (state != null && state.downed) {
                stand(player, state, false);
            }
        }
    }
}
