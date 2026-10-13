package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class MrFantasticPower {
    public static final String ID = "elasticity_reed_richards";
    public static final String PARACHUTE_STATE = "integrated_superheroes_parachute";
    public static final int PUNCH = 0;
    public static final int SWEEP = 3;
    public static final int CLEAR = 4;
    public static final int SLING = 5;
    public static final int HAMMER = 6;
    public static final int BRIDGE = 7;
    public static final int HAMMER_TICKS = 14;
    public static final int HAMMER_IMPACT = 6;
    private static final Map<Integer, Stretch> STRETCHES = new HashMap<>();
    private static final Map<Integer, Vec3> BRIDGES = new HashMap<>();
    private static final Map<UUID, Hammer> HAMMERS = new HashMap<>();

    private MrFantasticPower() {
    }

    public record Stretch(int mode, Vec3 point, long start, int ticks) {
    }

    private record Hammer(Vec3 point, long impact) {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        HeroWheels.register(ReedShapes.wheel());
        return new HeroPower(
                ID,
                "Elasticity (Reed Richards)",
                syringe,
                MarvelBalanceConfig.REED,
                List.of(),
                List.of(
                        new HeroPower.Ability("stretch_punch", "Stretch Punch", MrFantasticPower::stretchPunch),
                        new HeroPower.Ability("stretch_hammer", "Stretch Hammer", MrFantasticPower::stretchHammer),
                        new HeroPower.Ability("stretch_sweep", "Stretch Sweep", MrFantasticPower::stretchSweep),
                        new HeroPower.Ability(ReedShapes.ABILITY, "Shape Shift", (player, slot) -> 0.0D),
                        new HeroPower.Ability(ReedShapes.USE_ABILITY, "Use Shape", ReedShapes::use)
                ),
                List.of(),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        tickHammer(player);
                        if (gliding(player)) {
                            player.fallDistance = 0.0F;
                        }
                        ReedShapes.tick(player);
                        FantasticSeat seat = ReedShapes.seat(player);
                        if (seat != null && seat.getPassengers().stream().anyMatch(passenger -> passenger instanceof ServerPlayer)) {
                            HeroAdvancements.award(player, "fantastic_four/mr_fantastic/family_car");
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroPowers.data(player).removeState(PARACHUTE_STATE);
                        ReedShapes.clear(player);
                        HAMMERS.remove(player.getUUID());
                        send(player, CLEAR, player.position(), 0);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (!event.getSource().is(DamageTypeTags.IS_FALL)) {
                            return;
                        }
                        event.setCanceled(true);
                        if (event.getAmount() >= 2.0F && !player.isShiftKeyDown() && !HeroPowers.data(player).hasState(PARACHUTE_STATE)
                                && !HeroPowers.data(player).hasState(ReedShapes.BALL_STATE)
                                && !HeroPowers.data(player).hasState(ReedShapes.POGO_STATE)) {
                            double bounce = Math.min(MarvelBalanceConfig.BOUNCE_MAX_SPEED.get(), 0.4D + event.getAmount() * 0.06D);
                            Vec3 motion = player.getDeltaMovement();
                            player.setDeltaMovement(motion.x, bounce, motion.z);
                            player.hurtMarked = true;
                            HeroFx.play(player, SoundEvents.SLIME_JUMP, 1.0F, 0.8F);
                        }
                    }

                    @Override
                    public void jump(Player player) {
                        ReedShapes.jump(player);
                    }
                }
        );
    }

    public static boolean isReed(Player player) {
        return player.hasData(com.stxllarplanes.marvellegacy.init.AttachmentsRegistry.PLAYER_DATA.get())
                && ID.equals(HeroPowers.data(player).getPrimaryPower());
    }

    public static boolean gliding(Player player) {
        return isReed(player) && HeroPowers.data(player).hasState(PARACHUTE_STATE) && !player.onGround()
                && !player.getAbilities().flying && !player.isInWater() && player.getY() <= player.yOld;
    }

    public static void receive(StretchPayload payload, long now) {
        if (payload.mode() == BRIDGE) {
            if (payload.ticks() > 0) {
                BRIDGES.put(payload.entityId(), new Vec3(payload.x(), payload.y(), payload.z()));
            } else {
                BRIDGES.remove(payload.entityId());
            }
        } else if (payload.mode() == CLEAR) {
            STRETCHES.remove(payload.entityId());
        } else {
            STRETCHES.put(payload.entityId(), new Stretch(payload.mode(), new Vec3(payload.x(), payload.y(), payload.z()), now, payload.ticks()));
        }
    }

    public static Vec3 bridgeEnd(Player player) {
        return BRIDGES.get(player.getId());
    }

    public static Stretch stretch(Player player, long now) {
        Stretch stretch = STRETCHES.get(player.getId());
        if (stretch == null) {
            return null;
        }
        if (now - stretch.start() > stretch.ticks()) {
            STRETCHES.remove(player.getId());
            return null;
        }
        return stretch;
    }

    public static void send(ServerPlayer player, int mode, Vec3 point, int ticks) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new StretchPayload(player.getId(), mode, (float) point.x, (float) point.y, (float) point.z, ticks));
    }

    public static void tickMovement(Player player) {
        ReedShapes.tickClient(player);
        if (!player.level().isClientSide || !player.isLocalPlayer() || !gliding(player)) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        if (motion.y >= -0.05D) {
            return;
        }
        Vec3 look = Vec3.directionFromRotation(0.0F, player.getYRot()).scale(0.03D);
        Vec3 horizontal = new Vec3(motion.x + look.x, 0.0D, motion.z + look.z);
        if (horizontal.length() > 0.6D) {
            horizontal = horizontal.normalize().scale(0.6D);
        }
        player.setDeltaMovement(horizontal.x, Math.max(motion.y, -0.12D), horizontal.z);
        player.fallDistance = 0.0F;
    }

    private static double stretchPunch(ServerPlayer player, int slot) {
        double range = MarvelBalanceConfig.STRETCH_PUNCH_RANGE.get();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 1);
        Vec3 point = beam.end();
        if (!beam.targets().isEmpty()) {
            LivingEntity target = beam.targets().get(0);
            point = target.getBoundingBox().getCenter();
            float damage = MarvelBalanceConfig.STRETCH_PUNCH_DAMAGE.get().floatValue();
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
            Vec3 look = player.getLookAngle();
            target.setDeltaMovement(look.x * 1.3D, 0.35D, look.z * 1.3D);
            target.hurtMarked = true;
            HeroFx.play(target, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.7F);
        }
        send(player, PUNCH, point, 8);
        HeroFx.play(player, SoundEvents.SLIME_SQUISH, 1.0F, 1.4F);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        return MarvelBalanceConfig.STRETCH_PUNCH_COOLDOWN.get();
    }

    private static double stretchHammer(ServerPlayer player, int slot) {
        if (HAMMERS.containsKey(player.getUUID())) {
            return 0.0D;
        }
        double range = MarvelBalanceConfig.STRETCH_HAMMER_RANGE.get();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 1);
        Vec3 point = beam.targets().isEmpty() ? beam.end() : beam.targets().get(0).position();
        if (beam.targets().isEmpty() && !beam.hitBlock()) {
            HitResult ground = player.level().clip(new ClipContext(point, point.subtract(0.0D, range, 0.0D),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            point = ground.getLocation();
        }
        HAMMERS.put(player.getUUID(), new Hammer(point, player.serverLevel().getGameTime() + HAMMER_IMPACT));
        send(player, HAMMER, point, HAMMER_TICKS);
        HeroFx.play(player, SoundEvents.SLIME_SQUISH, 1.0F, 0.6F);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        return MarvelBalanceConfig.STRETCH_HAMMER_COOLDOWN.get();
    }

    private static void tickHammer(ServerPlayer player) {
        Hammer hammer = HAMMERS.get(player.getUUID());
        ServerLevel level = player.serverLevel();
        if (hammer == null || level.getGameTime() < hammer.impact()) {
            return;
        }
        HAMMERS.remove(player.getUUID());
        Vec3 point = hammer.point();
        double radius = MarvelBalanceConfig.STRETCH_HAMMER_RADIUS.get();
        double reach = (radius + 0.5D) * (radius + 0.5D);
        float damage = MarvelBalanceConfig.STRETCH_HAMMER_DAMAGE.get().floatValue();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius, 2.0D, radius),
                entity -> entity != player && entity.isAlive() && entity.getBoundingBox().getCenter().distanceToSqr(point) <= reach)) {
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x * 0.2D, 0.45D, motion.z * 0.2D);
            target.hurtMarked = true;
            if (HeroAbilities.canControl(target)) {
                HeroStun.stun(target, 30);
            }
        }
        BlockState ground = level.getBlockState(BlockPos.containing(point.x, point.y - 0.1D, point.z));
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), point.x, point.y + 0.1D, point.z,
                    40, radius * 0.4D, 0.1D, radius * 0.4D, 0.15D);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, point.x, point.y + 0.3D, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y + 0.3D, point.z, 6, radius * 0.5D, 0.05D, radius * 0.5D, 0.0D);
        level.playSound(null, point.x, point.y, point.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, point.x, point.y, point.z, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.2F, 0.5F);
        level.playSound(null, point.x, point.y, point.z, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 1.0F, 2.0F);
    }

    private static double stretchSweep(ServerPlayer player, int slot) {
        double radius = MarvelBalanceConfig.STRETCH_SWEEP_RADIUS.get();
        float damage = MarvelBalanceConfig.STRETCH_SWEEP_DAMAGE.get().floatValue();
        HeroAdvancements.cast(player).hits(6, "fantastic_four/mr_fantastic/clean_sweep").run(() -> {
            for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
                HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
                Vec3 out = target.position().subtract(player.position()).normalize().scale(1.2D);
                target.setDeltaMovement(out.x, 0.4D, out.z);
                target.hurtMarked = true;
            }
        });
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0D, player.getZ(),
                8, radius * 0.5D, 0.2D, radius * 0.5D, 0.0D);
        send(player, SWEEP, new Vec3(radius, 0.0D, 0.0D), 16);
        HeroFx.play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
        HeroFx.play(player, SoundEvents.SLIME_SQUISH, 1.0F, 0.8F);
        return MarvelBalanceConfig.STRETCH_SWEEP_COOLDOWN.get();
    }
}
