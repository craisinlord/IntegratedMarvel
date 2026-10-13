package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class SuperSoldierPower {
    public static final String ID = "super_soldier_steve_rogers";
    private static final Map<Holder<MobEffect>, Integer> PERMANENT_EFFECTS = Map.of(
            MobEffects.MOVEMENT_SPEED, 0, MobEffects.JUMP, 0, MobEffects.DAMAGE_RESISTANCE, 0);
    private static final Map<Holder<MobEffect>, HeroPower.Toggle> EFFECT_TOGGLES = Map.of(
            MobEffects.MOVEMENT_SPEED, HeroToggles.SPEED, MobEffects.JUMP, HeroToggles.SUPER_LEAP);

    private SuperSoldierPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Super Soldier (Steve Rogers)",
                syringe,
                MarvelBalanceConfig.SUPER_SOLDIER,
                List.of("agility"),
                List.of(
                        new HeroPower.Ability("shield_charge", "Shield Charge", SuperSoldierPower::shieldCharge),
                        new HeroPower.Ability("rally", "Rally", SuperSoldierPower::rally),
                        HeroToggles.WHEEL
                ),
                List.of(HeroToggles.SPEED, HeroToggles.SUPER_LEAP),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (player.tickCount % 20 == 0) {
                            HeroToggles.applyEffects(player, PERMANENT_EFFECTS, EFFECT_TOGGLES);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroAbilities.clearDash(player);
                        PERMANENT_EFFECTS.keySet().forEach(player::removeEffect);
                    }

                    @Override
                    public void jump(Player player) {
                        if (player.isCrouching() && HeroToggles.isOn(player, HeroToggles.SUPER_LEAP)) {
                            HeroAnimations.playLeapFlip(player);
                            Vec3 motion = player.getDeltaMovement();
                            double horizontal = player.isSprinting() ? 2.5D : 1.0D;
                            player.setDeltaMovement(motion.x * horizontal, motion.y * MarvelBalanceConfig.SUPER_SOLDIER_JUMP_MULTIPLIER.get(), motion.z * horizontal);
                            player.hurtMarked = true;
                        }
                    }
                }
        );
    }

    private static double shieldCharge(ServerPlayer player, int slot) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return 0.0D;
        }
        Vec3 push = horizontal.normalize().scale(MarvelBalanceConfig.SHIELD_CHARGE_SPEED.get());
        double damage = MarvelBalanceConfig.SHIELD_CHARGE_DAMAGE.get();
        if (player.getOffhandItem().getItem() instanceof ShieldItem) {
            damage *= MarvelBalanceConfig.SHIELD_CHARGE_SHIELD_MULTIPLIER.get();
        }
        double charge = damage;
        HeroAdvancements.cast(player).hits(4, "avengers/captain_america/on_your_left")
                .run(() -> HeroAbilities.startDash(player, new Vec3(push.x, 0.2D, push.z), 10, charge, 1.2D));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
        return MarvelBalanceConfig.SHIELD_CHARGE_COOLDOWN.get();
    }

    private static double rally(ServerPlayer player, int slot) {
        int duration = MarvelBalanceConfig.RALLY_DURATION_TICKS.get();
        if (player.getHealth() <= 4.0F) {
            HeroAdvancements.award(player, "avengers/captain_america/get_up");
        }
        if (player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(MarvelBalanceConfig.RALLY_RADIUS.get()),
                ally -> ally != player && !ally.isSpectator()).size() >= 3) {
            HeroAdvancements.award(player, "avengers/captain_america/avengers_assemble");
        }
        for (Player ally : player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(MarvelBalanceConfig.RALLY_RADIUS.get()))) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1));
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 1.5F, 1.0F);
        return MarvelBalanceConfig.RALLY_COOLDOWN.get();
    }
}
