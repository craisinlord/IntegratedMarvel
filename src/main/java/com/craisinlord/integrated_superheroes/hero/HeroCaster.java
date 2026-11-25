package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.voidrealm.variant.VoidVariants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class HeroCaster {
    private static final Map<UUID, Caster> CASTERS = ServerState.map(new HashMap<>());
    private static final List<Consumer<LivingEntity>> TICKERS = new ArrayList<>();
    private static final List<Consumer<LivingEntity>> CLEANUPS = new ArrayList<>();
    private static final float TURN_DEGREES = 7.0F;
    private static boolean registered;

    private static final class Caster {
        private final Mob mob;
        private LivingEntity target;
        private long castUntil;
        private long activeUntil;
        private final Map<String, Long> cooldowns = new HashMap<>();

        private Caster(Mob mob) {
            this.mob = mob;
        }
    }

    private HeroCaster() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(HeroCaster::onServerTick);
    }

    public static void onTick(Consumer<LivingEntity> ticker) {
        TICKERS.add(ticker);
    }

    public static void onCleanup(Consumer<LivingEntity> cleanup) {
        CLEANUPS.add(cleanup);
    }

    public static boolean isPlayer(Entity caster) {
        return caster instanceof ServerPlayer;
    }

    public static ServerPlayer player(Entity caster) {
        return caster instanceof ServerPlayer player ? player : null;
    }

    public static ServerLevel level(LivingEntity caster) {
        return (ServerLevel) caster.level();
    }

    public static void message(LivingEntity caster, Component message) {
        if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(message, true);
        }
    }

    public static boolean griefs(LivingEntity caster) {
        return caster instanceof Player;
    }

    public static boolean foe(LivingEntity caster, Entity entity) {
        if (entity == caster || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        if (caster instanceof Mob mob) {
            if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
                return false;
            }
            return !VoidVariants.allied(mob, living);
        }
        return true;
    }

    public static LivingEntity target(LivingEntity caster) {
        if (caster instanceof Mob mob) {
            Caster state = CASTERS.get(mob.getUUID());
            if (state != null && state.target != null && state.target.isAlive()) {
                return state.target;
            }
            return mob.getTarget();
        }
        return null;
    }

    public static void cast(Mob mob, LivingEntity target, int castTicks, int activeTicks) {
        register();
        Caster state = CASTERS.computeIfAbsent(mob.getUUID(), ignored -> new Caster(mob));
        state.target = target;
        long now = mob.level().getGameTime();
        state.castUntil = Math.max(state.castUntil, now + castTicks);
        state.activeUntil = Math.max(state.activeUntil, now + Math.max(castTicks, activeTicks));
        aim(mob, target, 360.0F);
    }

    public static void extend(LivingEntity caster, int ticks) {
        if (caster instanceof Mob mob) {
            Caster state = CASTERS.get(mob.getUUID());
            if (state != null) {
                long now = mob.level().getGameTime();
                state.castUntil = Math.max(state.castUntil, now + ticks);
                state.activeUntil = Math.max(state.activeUntil, now + ticks);
            }
        }
    }

    public static boolean casting(Mob mob) {
        Caster state = CASTERS.get(mob.getUUID());
        return state != null && mob.level().getGameTime() < state.castUntil || HeroBeams.isFiringAny(mob);
    }

    public static boolean abilityActive(LivingEntity mob) {
        Caster state = CASTERS.get(mob.getUUID());
        return state != null && mob.level().getGameTime() < state.activeUntil + 60L || HeroBeams.isFiringAny(mob);
    }

    public static boolean ready(Mob mob, String move) {
        Caster state = CASTERS.get(mob.getUUID());
        if (state == null) {
            return true;
        }
        Long until = state.cooldowns.get(move);
        return until == null || mob.level().getGameTime() >= until;
    }

    public static void cooldown(Mob mob, String move, int ticks) {
        register();
        CASTERS.computeIfAbsent(mob.getUUID(), ignored -> new Caster(mob)).cooldowns.put(move, mob.level().getGameTime() + Math.max(0, ticks));
    }

    public static void aim(Mob mob, LivingEntity target, float maxTurn) {
        if (target == null) {
            return;
        }
        Vec3 from = mob.getEyePosition();
        Vec3 to = target.getBoundingBox().getCenter();
        Vec3 path = to.subtract(from);
        double flat = Math.sqrt(path.x * path.x + path.z * path.z);
        float yaw = (float) (Mth.atan2(path.z, path.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(path.y, flat) * Mth.RAD_TO_DEG);
        float newYaw = Mth.approachDegrees(mob.getYRot(), yaw, maxTurn);
        float newPitch = Mth.approachDegrees(mob.getXRot(), pitch, maxTurn);
        mob.setYRot(newYaw);
        mob.setXRot(newPitch);
        mob.yRotO = newYaw;
        mob.xRotO = newPitch;
        mob.setYHeadRot(newYaw);
        mob.yHeadRotO = newYaw;
        mob.setYBodyRot(newYaw);
        mob.yBodyRotO = newYaw;
        mob.getLookControl().setLookAt(target, maxTurn, maxTurn);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (CASTERS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Caster>> iterator = CASTERS.entrySet().iterator();
        List<Mob> live = new ArrayList<>();
        while (iterator.hasNext()) {
            Caster state = iterator.next().getValue();
            Mob mob = state.mob;
            if (mob.isRemoved() || !mob.isAlive()) {
                cleanup(mob);
                iterator.remove();
                continue;
            }
            live.add(mob);
        }
        for (Mob mob : live) {
            Caster state = CASTERS.get(mob.getUUID());
            if (state == null) {
                continue;
            }
            long now = mob.level().getGameTime();
            if (now < state.activeUntil || HeroBeams.isFiringAny(mob)) {
                LivingEntity target = state.target != null && state.target.isAlive() ? state.target : mob.getTarget();
                aim(mob, target, TURN_DEGREES);
                if (now < state.castUntil) {
                    mob.getNavigation().stop();
                }
            }
            HeroBeams.tick(mob);
            HeroGrip.tick(mob);
            HeroAbilities.tickDash(mob);
            for (Consumer<LivingEntity> ticker : TICKERS) {
                ticker.accept(mob);
            }
        }
    }

    private static void cleanup(Mob mob) {
        HeroBeams.stopAll(mob);
        HeroGrip.clear(mob);
        HeroAbilities.clearDash(mob);
        for (Consumer<LivingEntity> cleanup : CLEANUPS) {
            cleanup.accept(mob);
        }
    }
}
