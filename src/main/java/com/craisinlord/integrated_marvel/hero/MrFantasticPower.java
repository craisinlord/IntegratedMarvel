package com.craisinlord.integrated_marvel.hero;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class MrFantasticPower {
    public static final String ID = "elasticity_reed_richards";
    public static final String PARACHUTE_STATE = "integrated_marvel_parachute";
    public static final int PUNCH = 0;
    public static final int GRAB = 1;
    public static final int GRAPPLE = 2;
    public static final int SWEEP = 3;
    public static final int CLEAR = 4;
    public static final int SLING = 5;
    private static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.2F, 0.35F, 0.85F), 0.01F);
    private static final Map<Integer, Stretch> STRETCHES = new HashMap<>();
    private static final Set<UUID> GRAPPLING = new HashSet<>();

    private MrFantasticPower() {
    }

    public record Stretch(int mode, Vec3 point, long start, int ticks) {
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
                        new HeroPower.Ability("stretch_grab", "Stretch Grab", MrFantasticPower::stretchGrab),
                        new HeroPower.Ability("stretch_sweep", "Stretch Sweep", MrFantasticPower::stretchSweep),
                        new HeroPower.Ability(ReedShapes.ABILITY, "Shape Shift", (player, slot) -> 0.0D),
                        new HeroPower.Ability(ReedShapes.USE_ABILITY, "Use Shape", ReedShapes::use)
                ),
                List.of(),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (GRAPPLING.contains(player.getUUID()) && !HeroGrapple.isPulling(player)) {
                            GRAPPLING.remove(player.getUUID());
                            send(player, CLEAR, player.position(), 0);
                        }
                        if (gliding(player)) {
                            player.fallDistance = 0.0F;
                        }
                        ReedShapes.tick(player);
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroPowers.data(player).removeState(PARACHUTE_STATE);
                        ReedShapes.clear(player);
                        if (GRAPPLING.remove(player.getUUID())) {
                            HeroGrapple.clear(player);
                        }
                        send(player, CLEAR, player.position(), 0);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (!event.getSource().is(DamageTypeTags.IS_FALL)) {
                            return;
                        }
                        event.setCanceled(true);
                        if (event.getAmount() >= 2.0F && !player.isShiftKeyDown() && !HeroPowers.data(player).hasState(PARACHUTE_STATE)
                                && !HeroPowers.data(player).hasState(ReedShapes.BALL_STATE)) {
                            double bounce = Math.min(MarvelBalanceConfig.BOUNCE_MAX_SPEED.get(), 0.4D + event.getAmount() * 0.06D);
                            Vec3 motion = player.getDeltaMovement();
                            player.setDeltaMovement(motion.x, bounce, motion.z);
                            player.hurtMarked = true;
                            HeroFx.play(player, SoundEvents.SLIME_JUMP, 1.0F, 0.8F);
                        }
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
        if (payload.mode() == CLEAR) {
            STRETCHES.remove(payload.entityId());
        } else {
            STRETCHES.put(payload.entityId(), new Stretch(payload.mode(), new Vec3(payload.x(), payload.y(), payload.z()), now, payload.ticks()));
        }
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
            float damage = (float) (HeroPowers.powerDamage(player) * MarvelBalanceConfig.STRETCH_PUNCH_DAMAGE_MULTIPLIER.get());
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

    private static double stretchGrab(ServerPlayer player, int slot) {
        double range = MarvelBalanceConfig.STRETCH_GRAB_RANGE.get();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 1);
        if (!beam.targets().isEmpty()) {
            LivingEntity target = beam.targets().get(0);
            pull(player, target);
            if (HeroAbilities.canControl(target)) {
                HeroStun.stun(target, 20);
            }
            send(player, GRAB, target.getBoundingBox().getCenter(), 10);
            HeroFx.play(player, SoundEvents.SLIME_SQUISH, 1.0F, 1.0F);
            return MarvelBalanceConfig.STRETCH_GRAB_COOLDOWN.get();
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = beam.end();
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, new AABB(eye, end).inflate(1.0D),
                item -> item.getBoundingBox().inflate(0.8D).clip(eye, end).isPresent());
        if (!items.isEmpty()) {
            items.forEach(item -> pull(player, item));
            send(player, GRAB, items.get(0).position(), 10);
            HeroFx.play(player, SoundEvents.SLIME_SQUISH, 1.0F, 1.2F);
            return 1.0D;
        }
        Vec3 anchor = HeroGrapple.anchor(player, range);
        if (anchor == null) {
            return 0.0D;
        }
        HeroGrapple.start(player, anchor, BLUE);
        GRAPPLING.add(player.getUUID());
        send(player, GRAPPLE, anchor, 100);
        HeroFx.play(player, SoundEvents.SLIME_SQUISH, 1.0F, 0.7F);
        return MarvelBalanceConfig.STRETCH_GRAB_COOLDOWN.get();
    }

    private static void pull(ServerPlayer player, net.minecraft.world.entity.Entity target) {
        Vec3 offset = player.position().subtract(target.position());
        Vec3 velocity = offset.scale(0.22D).add(0.0D, 0.35D, 0.0D);
        if (velocity.length() > 2.5D) {
            velocity = velocity.normalize().scale(2.5D);
        }
        target.setDeltaMovement(velocity);
        target.hurtMarked = true;
    }

    private static double stretchSweep(ServerPlayer player, int slot) {
        double radius = MarvelBalanceConfig.STRETCH_SWEEP_RADIUS.get();
        float damage = (float) (HeroPowers.powerDamage(player) * MarvelBalanceConfig.STRETCH_SWEEP_DAMAGE_MULTIPLIER.get());
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
            Vec3 out = target.position().subtract(player.position()).normalize().scale(1.2D);
            target.setDeltaMovement(out.x, 0.4D, out.z);
            target.hurtMarked = true;
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0D, player.getZ(),
                8, radius * 0.5D, 0.2D, radius * 0.5D, 0.0D);
        send(player, SWEEP, new Vec3(radius, 0.0D, 0.0D), 16);
        HeroFx.play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
        HeroFx.play(player, SoundEvents.SLIME_SQUISH, 1.0F, 0.8F);
        return MarvelBalanceConfig.STRETCH_SWEEP_COOLDOWN.get();
    }
}
