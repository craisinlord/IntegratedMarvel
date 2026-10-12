package com.craisinlord.integrated_superheroes.hero;

import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class ColossusPower {
    public static final String ID = "organic_steel_piotr_rasputin";
    public static final String STEEL_STATE = "integrated_superheroes_steel";
    private static final String STEEL_FORM = "steel_form";
    private static final Map<UUID, Long> FASTBALL = new HashMap<>();
    private static final Map<UUID, Integer> LAVA_TICKS = new HashMap<>();
    private static final Map<UUID, UUID> FASTBALL_HELD = new HashMap<>();

    private ColossusPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Organic Steel (Piotr Rasputin)",
                syringe,
                MarvelBalanceConfig.PIOTR,
                List.of(),
                List.of(
                        new HeroPower.Ability(STEEL_FORM, "Steel Form", ColossusPower::steelForm),
                        new HeroPower.Ability("fastball_special", "Fastball Special", ColossusPower::fastball),
                        new HeroPower.Ability("steel_charge", "Steel Charge", ColossusPower::steelCharge),
                        new HeroPower.Ability("iron_slam", "Iron Slam", ColossusPower::ironSlam)
                ),
                List.of(),
                new HeroPower.Hooks() {
                    @Override
                    public MarvelBalanceConfig.HeroStats stats(Player player) {
                        return isSteel(player) ? MarvelBalanceConfig.COLOSSUS : MarvelBalanceConfig.PIOTR;
                    }

                    @Override
                    public void tick(ServerPlayer player) {
                        HeroGrip.tick(player);
                        if (!isSteel(player)) {
                            LAVA_TICKS.remove(player.getUUID());
                            return;
                        }
                        HeroStride.tick(player, 2.0D, SoundEvents.IRON_GOLEM_STEP, 1.2F, 0.7F, 6, 0.08F);
                        if (player.isInLava()) {
                            int ticks = LAVA_TICKS.merge(player.getUUID(), 1, Integer::sum);
                            if (ticks > 100 && player.tickCount % 20 == 0) {
                                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, true, true));
                            }
                        } else {
                            LAVA_TICKS.remove(player.getUUID());
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroPowers.data(player).removeState(STEEL_STATE);
                        HeroStride.clear(player);
                        HeroGrip.clear(player);
                        LAVA_TICKS.remove(player.getUUID());
                        FASTBALL_HELD.remove(player.getUUID());
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (!isSteel(player)) {
                            return;
                        }
                        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
                            event.setAmount(event.getAmount() * MarvelBalanceConfig.RICOCHET_MULTIPLIER.get().floatValue());
                            Vec3 center = player.getBoundingBox().getCenter();
                            player.serverLevel().sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 12, 0.3D, 0.4D, 0.3D, 0.4D);
                            HeroFx.play(player, SoundEvents.ANVIL_LAND, 0.6F, 1.8F);
                        } else if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                            event.setAmount(event.getAmount() * MarvelBalanceConfig.STEEL_FIRE_MULTIPLIER.get().floatValue());
                        }
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        HeroPowers.data(player).removeState(STEEL_STATE);
                        PlayerData.sync(player);
                    }
                }
        );
    }

    public static boolean isSteel(Player player) {
        PlayerData data = HeroPowers.data(player);
        return ID.equals(data.getPrimaryPower()) && data.hasState(STEEL_STATE);
    }

    public static void tickMovement(Player player) {
        if (!player.level().isClientSide || !player.isLocalPlayer() || !isSteel(player) || !player.isInWater()) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        player.setSwimming(false);
        player.setDeltaMovement(motion.x, Math.min(motion.y, -0.08D), motion.z);
    }

    public static void onAnyDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target instanceof ServerPlayer thrown && FASTBALL.containsKey(thrown.getUUID()) && event.getSource().is(DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getSource().getDirectEntity() == attacker) {
            Long until = FASTBALL.get(attacker.getUUID());
            if (until != null) {
                FASTBALL.remove(attacker.getUUID());
                if (attacker.level().getGameTime() <= until) {
                    event.setAmount(event.getAmount() * MarvelBalanceConfig.FASTBALL_DAMAGE_MULTIPLIER.get().floatValue());
                    Vec3 center = target.getBoundingBox().getCenter();
                    attacker.serverLevel().sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    HeroFx.play(target, SoundEvents.ANVIL_LAND, 1.0F, 0.6F);
                }
            }
        }
        if (target instanceof ServerPlayer protectedPlayer && event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            Vec3 from = event.getSource().getSourcePosition();
            if (from == null) {
                return;
            }
            for (ServerPlayer guard : protectedPlayer.serverLevel().getEntitiesOfClass(ServerPlayer.class, protectedPlayer.getBoundingBox().inflate(3.0D),
                    candidate -> candidate != protectedPlayer && isSteel(candidate) && candidate.isCrouching())) {
                Vec3 facing = guard.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
                Vec3 toTarget = protectedPlayer.position().subtract(guard.position()).multiply(1.0D, 0.0D, 1.0D);
                Vec3 toSource = from.subtract(guard.position()).multiply(1.0D, 0.0D, 1.0D);
                if (facing.dot(toTarget) < 0.0D && facing.dot(toSource) > 0.0D) {
                    event.setAmount(event.getAmount() * MarvelBalanceConfig.BULWARK_REDUCTION.get().floatValue());
                    return;
                }
            }
        }
    }

    public static void tickFastball(ServerPlayer player) {
        Long until = FASTBALL.get(player.getUUID());
        if (until != null && player.level().getGameTime() > until && player.onGround()) {
            FASTBALL.remove(player.getUUID());
        }
    }

    private static boolean requireSteel(ServerPlayer player) {
        if (isSteel(player)) {
            return true;
        }
        player.displayClientMessage(Component.literal("§7Turn to steel first."), true);
        return false;
    }

    private static double steelForm(ServerPlayer player, int slot) {
        PlayerData data = HeroPowers.data(player);
        ServerLevel level = player.serverLevel();
        if (isSteel(player)) {
            data.removeState(STEEL_STATE);
            PlayerData.sync(player);
            HeroAttributes.apply(player, MarvelBalanceConfig.PIOTR);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
            HeroStride.clear(player);
            HeroFx.play(player, SoundEvents.CHAIN_BREAK, 1.0F, 0.8F);
            return MarvelBalanceConfig.STEEL_FORM_COOLDOWN.get();
        }
        double width = player.getBbWidth() * 1.2D;
        double height = player.getBbHeight() * 1.2D;
        AABB grown = new AABB(player.getX() - width / 2.0D, player.getY(), player.getZ() - width / 2.0D,
                player.getX() + width / 2.0D, player.getY() + height, player.getZ() + width / 2.0D);
        if (!level.noCollision(player, grown)) {
            player.displayClientMessage(Component.literal("§7Not enough room."), true);
            return 0.0D;
        }
        data.addState(STEEL_STATE);
        PlayerData.sync(player);
        HeroAttributes.apply(player, MarvelBalanceConfig.COLOSSUS);
        Vec3 center = player.getBoundingBox().getCenter();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), center.x, center.y, center.z,
                40, 0.4D, 0.8D, 0.4D, 0.2D);
        HeroFx.play(player, SoundEvents.ANVIL_PLACE, 1.0F, 0.6F);
        HeroFx.play(player, SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 0.8F);
        HeroAnimations.play(player, "bettercombat:two_handed_slam");
        return 0.0D;
    }

    private static double fastball(ServerPlayer player, int slot) {
        if (HeroGrip.isHolding(player)) {
            UUID held = FASTBALL_HELD.remove(player.getUUID());
            boolean ally = held != null && player.serverLevel().getEntity(held) instanceof ServerPlayer;
            float damage = ally ? 0.0F : (float) (HeroPowers.powerDamage(player) * 0.8D);
            HeroGrip.release(player, MarvelBalanceConfig.FASTBALL_SPEED.get(), damage);
            if (ally && player.serverLevel().getEntity(held) instanceof ServerPlayer thrown) {
                FASTBALL.put(thrown.getUUID(), player.level().getGameTime() + 60L);
                thrown.displayClientMessage(Component.literal("§bFastball Special!"), true);
                HeroAdvancements.award(player, "x_men/colossus/fastball_special");
                HeroAdvancements.award(thrown, "x_men/colossus/special_delivery");
            }
            HeroAnimations.play(player, "celestisynth:cs_crescentia_throw");
            HeroFx.play(player, SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.6F);
            return MarvelBalanceConfig.FASTBALL_COOLDOWN.get();
        }
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), 4.0D, 1);
        if (beam.targets().isEmpty()) {
            return 0.0D;
        }
        LivingEntity target = beam.targets().get(0);
        if (HeroGrip.grab(player, target, 200, null, held -> held.fallDistance = 0.0F)) {
            FASTBALL_HELD.put(player.getUUID(), target.getUUID());
            HeroFx.play(player, SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.9F);
            HeroAnimations.play(player, "uppercut");
        }
        return 0.0D;
    }

    private static double steelCharge(ServerPlayer player, int slot) {
        if (!requireSteel(player)) {
            return 0.0D;
        }
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0D, look.z).normalize();
        double damage = HeroPowers.powerDamage(player) * MarvelBalanceConfig.STEEL_CHARGE_DAMAGE_MULTIPLIER.get();
        HeroAdvancements.cast(player).hits(8, "x_men/colossus/unstoppable_object")
                .run(() -> HeroAbilities.startDash(player, direction.scale(1.6D).add(0.0D, 0.1D, 0.0D), 8, damage, 1.6D));
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        for (int step = 1; step <= 8; step++) {
            Vec3 at = player.position().add(direction.scale(step));
            for (int y = 0; y <= 2; y++) {
                BlockPos pos = BlockPos.containing(at.x, origin.getY() + y, at.z);
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || !level.mayInteract(player, pos)) {
                    continue;
                }
                if (state.is(BlockTags.LEAVES) || state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES)
                        || state.canBeReplaced() && state.getFluidState().isEmpty()) {
                    level.destroyBlock(pos, true, player);
                }
            }
        }
        HeroFx.play(player, SoundEvents.IRON_GOLEM_ATTACK, 1.4F, 0.6F);
        HeroFx.shake(level, player.position(), 16.0D, 0.4F, 8);
        HeroAnimations.play(player, "kick");
        return MarvelBalanceConfig.STEEL_CHARGE_COOLDOWN.get();
    }

    private static double ironSlam(ServerPlayer player, int slot) {
        if (!requireSteel(player)) {
            return 0.0D;
        }
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), 4.0D, 1);
        if (beam.targets().isEmpty() || !HeroAbilities.canControl(beam.targets().get(0))) {
            return 0.0D;
        }
        LivingEntity target = beam.targets().get(0);
        float damage = (float) (HeroPowers.powerDamage(player) * MarvelBalanceConfig.IRON_SLAM_DAMAGE_MULTIPLIER.get());
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().mobAttack(player), damage));
        target.setDeltaMovement(0.0D, -1.5D, 0.0D);
        target.hurtMarked = true;
        ServerLevel level = player.serverLevel();
        BlockState ground = level.getBlockState(target.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), target.getX(), target.getY(), target.getZ(),
                    40, 1.0D, 0.1D, 1.0D, 0.3D);
        }
        HeroShockwaves.ring(player, 8, 3, damage * 0.3F);
        HeroFx.play(player, SoundEvents.ANVIL_LAND, 1.4F, 0.5F);
        HeroFx.shake(level, target.position(), 20.0D, 0.6F, 10);
        HeroAnimations.play(player, "bettercombat:one_handed_slam");
        return MarvelBalanceConfig.IRON_SLAM_COOLDOWN.get();
    }
}
