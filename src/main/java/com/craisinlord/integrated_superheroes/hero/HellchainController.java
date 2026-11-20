package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class HellchainController {
    public static final String ABILITY = "ghost_rider_hellchain";
    private static final int LASH_TICKS = 6;
    private static final Map<UUID, Chain> CHAINS = new HashMap<>();
    private static final Map<UUID, Pull> PULLS = new HashMap<>();
    private static final Map<Integer, int[]> CLIENT = new HashMap<>();

    private HellchainController() {
    }

    private static final class Chain {
        private final UUID target;
        private final ResourceKey<Level> dimension;
        private final double length;
        private int ticksLeft;

        private Chain(UUID target, ResourceKey<Level> dimension, double length, int ticksLeft) {
            this.target = target;
            this.dimension = dimension;
            this.length = length;
            this.ticksLeft = ticksLeft;
        }
    }

    private static final class Pull {
        private final UUID target;
        private int ticksLeft;

        private Pull(UUID target, int ticksLeft) {
            this.target = target;
            this.ticksLeft = ticksLeft;
        }
    }

    public static boolean chained(ServerPlayer player) {
        return CHAINS.containsKey(player.getUUID());
    }

    public static double press(ServerPlayer player, boolean borrowed) {
        if (!borrowed && CHAINS.containsKey(player.getUUID())) {
            LivingEntity held = resolve(player, CHAINS.get(player.getUUID()));
            if (held != null && clearLine(player, held)) {
                yank(player, held);
            }
            end(player, false);
            return GhostRiderConfig.CHAIN_COOLDOWN.get();
        }
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), GhostRiderConfig.CHAIN_RANGE.get(), 1);
        LivingEntity target = beam.targets().isEmpty() ? null : beam.targets().get(0);
        if (target == null || !HellfireZones.harmable(player, target) || !clearLine(player, target)) {
            player.displayClientMessage(Component.literal("§7Nothing in reach to chain."), true);
            HeroFx.play(player, SoundEvents.CHAIN_FALL, 0.8F, 0.8F);
            return GhostRiderConfig.CHAIN_MISS_COOLDOWN.get();
        }
        double base = MarvelBalanceConfig.CLAW_SWIPE_DAMAGE.get() * GhostRiderConfig.CHAIN_DAMAGE_SCALE.get();
        float damage = borrowed ? RogueKit.dmg(base) : (float) base;
        target.invulnerableTime = 0;
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
        ServerLevel level = player.serverLevel();
        Vec3 center = target.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 12,
                target.getBbWidth() * 0.4D, target.getBbHeight() * 0.4D, target.getBbWidth() * 0.4D, 0.03D);
        HeroFx.play(player, SoundEvents.CHAIN_HIT, 1.2F, 0.7F);
        HeroFx.play(target, SoundEvents.CHAIN_BREAK, 1.0F, 0.6F);
        HeroFx.play(player, SoundEvents.FIRECHARGE_USE, 0.5F, 0.6F);
        HeroAnimations.play(player, "web_shoot_r");
        int tether = GhostRiderConfig.CHAIN_TETHER_TICKS.get();
        boolean tetherable = !borrowed && tether > 0 && target.isAlive() && !(target instanceof Player)
                && HeroAbilities.canControl(target) && !HeroAbilities.isBoss(target);
        if (!borrowed) {
            GhostRiderJudgment.add(player, target);
        }
        if (!tetherable) {
            send(player, target.getId(), LASH_TICKS);
            return GhostRiderConfig.CHAIN_COOLDOWN.get();
        }
        double length = Math.max(3.0D, Math.min(player.distanceTo(target), GhostRiderConfig.CHAIN_RANGE.get()));
        CHAINS.put(player.getUUID(), new Chain(target.getUUID(), level.dimension(), length, tether));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, tether, 3, false, false, true));
        send(player, target.getId(), tether + 2);
        return 0.25D;
    }

    public static void tick(ServerPlayer player) {
        tickPull(player);
        Chain chain = CHAINS.get(player.getUUID());
        if (chain == null) {
            return;
        }
        LivingEntity target = resolve(player, chain);
        if (target == null) {
            end(player, true);
            return;
        }
        double distance = player.distanceTo(target);
        if (distance > GhostRiderConfig.CHAIN_BREAK_DISTANCE.get() || !clearLine(player, target)) {
            HeroFx.play(target, SoundEvents.CHAIN_BREAK, 1.0F, 1.2F);
            player.displayClientMessage(Component.literal("§7The chain snapped."), true);
            end(player, true);
            return;
        }
        if (--chain.ticksLeft <= 0) {
            end(player, true);
            return;
        }
        if (distance > chain.length) {
            Vec3 toward = player.position().subtract(target.position());
            Vec3 flat = new Vec3(toward.x, 0.0D, toward.z);
            if (flat.lengthSqr() > 1.0E-4D) {
                Vec3 pull = flat.normalize().scale(Math.min(0.6D, (distance - chain.length) * 0.3D));
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(pull.x, motion.y, pull.z);
                target.hurtMarked = true;
            }
        }
    }

    private static void yank(ServerPlayer player, LivingEntity target) {
        Vec3 toward = player.position().subtract(target.position());
        double distance = toward.length();
        if (distance < 1.0E-3D) {
            return;
        }
        Vec3 direction = toward.normalize();
        double speed = Math.min(GhostRiderConfig.CHAIN_YANK_SPEED.get(), 0.5D + distance * 0.15D);
        target.setDeltaMovement(direction.x * speed, Math.max(0.25D, direction.y * speed + 0.25D), direction.z * speed);
        target.hurtMarked = true;
        target.hasImpulse = true;
        PULLS.put(player.getUUID(), new Pull(target.getUUID(), 14));
        HeroFx.play(player, SoundEvents.CHAIN_PLACE, 1.2F, 0.6F);
        HeroFx.play(player, SoundEvents.BLAZE_SHOOT, 0.5F, 0.5F);
        HeroAnimations.play(player, "celestisynth:cs_crescentia_throw");
    }

    private static void tickPull(ServerPlayer player) {
        Pull pull = PULLS.get(player.getUUID());
        if (pull == null) {
            return;
        }
        Entity entity = player.serverLevel().getEntity(pull.target);
        if (!(entity instanceof LivingEntity target) || !target.isAlive() || --pull.ticksLeft <= 0) {
            PULLS.remove(player.getUUID());
            return;
        }
        if (target.distanceTo(player) < 2.2D || target.horizontalCollision) {
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x * 0.2D, Math.min(motion.y, 0.1D), motion.z * 0.2D);
            target.hurtMarked = true;
            PULLS.remove(player.getUUID());
        }
    }

    private static LivingEntity resolve(ServerPlayer player, Chain chain) {
        if (player.serverLevel().dimension() != chain.dimension || !player.isAlive()) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(chain.target);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static boolean clearLine(ServerPlayer player, LivingEntity target) {
        Vec3 from = player.getEyePosition();
        Vec3 to = target.getBoundingBox().getCenter();
        return player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    private static void end(ServerPlayer player, boolean cooldown) {
        Chain chain = CHAINS.remove(player.getUUID());
        if (chain == null) {
            return;
        }
        send(player, -1, 0);
        if (cooldown) {
            HeroAbilities.setCooldown(player, ABILITY, GhostRiderConfig.CHAIN_COOLDOWN.get());
        }
    }

    public static void clear(ServerPlayer player) {
        if (CHAINS.remove(player.getUUID()) != null) {
            send(player, -1, 0);
        }
        PULLS.remove(player.getUUID());
    }

    public static void clearAll() {
        CHAINS.clear();
        PULLS.clear();
    }

    private static void send(ServerPlayer player, int targetId, int ticks) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new HellchainPayload(player.getId(), targetId, ticks));
    }

    static void receive(HellchainPayload payload) {
        if (payload.ticks() <= 0) {
            CLIENT.remove(payload.ownerId());
        } else {
            CLIENT.put(payload.ownerId(), new int[]{payload.targetId(), payload.ticks()});
        }
    }

    static Map<Integer, int[]> clientChains() {
        return CLIENT;
    }

    static void tickClient() {
        Iterator<Map.Entry<Integer, int[]>> iterator = CLIENT.entrySet().iterator();
        while (iterator.hasNext()) {
            int[] chain = iterator.next().getValue();
            if (--chain[1] <= 0) {
                iterator.remove();
            }
        }
    }

    static void clearClient() {
        CLIENT.clear();
    }
}
