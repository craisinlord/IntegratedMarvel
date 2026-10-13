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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class ThingPower {
    public static final String ID = "rock_hide_ben_grimm";
    public static final String BRACE_STATE = "integrated_superheroes_brace";
    private static final BlockState ROCK = Blocks.TERRACOTTA.defaultBlockState();
    private static final Map<UUID, Long> LAST_HURT = new HashMap<>();
    private static final Map<UUID, Combo> COMBOS = new HashMap<>();
    private static final Map<UUID, Wrestle> WRESTLES = new HashMap<>();
    private static final Map<UUID, Long> SERVER_LEAPS = new HashMap<>();
    private static final Map<UUID, Long> CLIENT_LEAPS = new HashMap<>();
    public static final HeroPower.Toggle ROCK_LEAP = new HeroPower.Toggle("integrated_superheroes_off_jump", "Super Jump", () -> Items.TUFF);

    private ThingPower() {
    }

    private record Combo(int hits, long last) {
    }

    private static final class Wrestle {
        private final LivingEntity target;
        private int ticks;

        private Wrestle(LivingEntity target) {
            this.target = target;
        }
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Rock Hide (Ben Grimm)",
                syringe,
                MarvelBalanceConfig.THING,
                List.of(),
                List.of(
                        new HeroPower.Ability("clobberin_time", "It's Clobberin' Time!", ThingPower::clobber),
                        new HeroPower.Ability("wrestle", "Wrestle", ThingPower::wrestle),
                        new HeroPower.Ability("rip_and_toss", "Rip and Toss", ThingPower::ripAndToss),
                        new HeroPower.Ability("brace", "Brace", ThingPower::brace),
                        HeroToggles.WHEEL
                ),
                List.of(ROCK_LEAP),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (player.tickCount % 20 == 0) {
                            if (player.isUnderWater() && player.onGround() && deepWater(player, 20)) {
                                HeroAdvancements.award(player, "fantastic_four/thing/rock_bottom");
                            }
                        }
                        HeroBlockLift.tick(player);
                        tickWrestle(player);
                        tickPlating(player);
                        if (player.isSprinting()) {
                            smashThrough(player);
                        }
                        if (isBracing(player) && player.tickCount % 10 == 0) {
                            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, true, false, false));
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroPowers.data(player).removeState(BRACE_STATE);
                        HeroBlockLift.drop(player);
                        WRESTLES.remove(player.getUUID());
                        COMBOS.remove(player.getUUID());
                        LAST_HURT.remove(player.getUUID());
                        player.setAbsorptionAmount(0.0F);
                    }

                    @Override
                    public void jump(Player player) {
                        leap(player);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypeTags.IS_FALL)) {
                            event.setCanceled(true);
                            if (event.getAmount() >= 3.0F) {
                                crater(player, event.getAmount());
                            }
                            if (event.getAmount() >= 47.0F) {
                                HeroAdvancements.award(player, "fantastic_four/thing/crater_maker");
                            }
                            return;
                        }
                        LAST_HURT.put(player.getUUID(), player.serverLevel().getGameTime());
                        if (isBracing(player)) {
                            HeroAdvancements.count(player, "fantastic_four/thing/immovable", event.getAmount(), 100.0D);
                            event.setAmount(event.getAmount() * MarvelBalanceConfig.BRACE_DAMAGE_MULTIPLIER.get().floatValue());
                            if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != player) {
                                Vec3 push = attacker.position().subtract(player.position()).normalize().scale(1.4D);
                                attacker.setDeltaMovement(push.x, 0.4D, push.z);
                                attacker.hurtMarked = true;
                                HeroFx.play(player, SoundEvents.ANVIL_LAND, 0.5F, 1.5F);
                            }
                        }
                        if (player.getAbsorptionAmount() > 0.0F) {
                            Vec3 at = player.position().add(0.0D, 1.0D, 0.0D);
                            player.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ROCK), at.x, at.y, at.z,
                                    8, 0.3D, 0.5D, 0.3D, 0.1D);
                            HeroFx.play(player, SoundEvents.STONE_BREAK, 0.8F, 0.8F);
                        }
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().getDirectEntity() != player || !player.getMainHandItem().isEmpty() || HeroAbilities.isAbilityDamage()) {
                            return;
                        }
                        long now = player.serverLevel().getGameTime();
                        Combo combo = COMBOS.get(player.getUUID());
                        int hits = combo != null && now - combo.last() < 40L ? combo.hits() + 1 : 1;
                        if (hits >= 3) {
                            COMBOS.remove(player.getUUID());
                            event.setAmount(event.getAmount() * MarvelBalanceConfig.COMBO_MULTIPLIER.get().floatValue());
                            launch(player, event.getEntity(), 1.6D, 0.7D);
                            HeroFx.play(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.6F);
                        } else {
                            COMBOS.put(player.getUUID(), new Combo(hits, now));
                        }
                    }
                }
        );
    }

    public static boolean isThing(Player player) {
        return player.hasData(com.stxllarplanes.marvellegacy.init.AttachmentsRegistry.PLAYER_DATA.get())
                && ID.equals(HeroPowers.data(player).getPrimaryPower());
    }

    private static boolean deepWater(Player player, int depth) {
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        for (int i = 0; i < depth; i++) {
            if (!player.level().getFluidState(eye.above(i)).is(net.minecraft.tags.FluidTags.WATER)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBracing(Player player) {
        return HeroPowers.data(player).hasState(BRACE_STATE);
    }

    private static void leap(Player player) {
        if (!isThing(player) || !player.isCrouching() || !HeroToggles.isOn(player, ROCK_LEAP)) {
            return;
        }
        boolean client = player.level().isClientSide;
        Map<UUID, Long> last = client ? CLIENT_LEAPS : SERVER_LEAPS;
        long now = player.level().getGameTime();
        Long previous = last.get(player.getUUID());
        if (previous != null && now - previous < MarvelBalanceConfig.THING_LEAP_COOLDOWN_TICKS.get()) {
            return;
        }
        last.put(player.getUUID(), now);
        if (client) {
            if (player.isLocalPlayer()) {
                Vec3 motion = player.getDeltaMovement();
                if (new Vec3(motion.x, 0.0D, motion.z).lengthSqr() > 0.0009D) {
                    Vec3 look = player.getLookAngle();
                    Vec3 direction = new Vec3(look.x, 0.0D, look.z).normalize().scale(MarvelBalanceConfig.THING_LEAP_HORIZONTAL.get());
                    player.setDeltaMovement(direction.x, MarvelBalanceConfig.THING_LEAP_VERTICAL.get(), direction.z);
                } else {
                    player.setDeltaMovement(0.0D, MarvelBalanceConfig.THING_LEAP_STANDING_VERTICAL.get(), 0.0D);
                }
                HeroAnimations.requestFromClient(player, "celestisynth:cs_breezebreaker_jump");
            }
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ServerLevel level = serverPlayer.serverLevel();
            BlockState ground = level.getBlockState(serverPlayer.blockPosition().below());
            if (!ground.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), player.getX(), player.getY() + 0.1D, player.getZ(),
                        40, 1.0D, 0.1D, 1.0D, 0.3D);
            }
            HeroFx.play(player, SoundEvents.ANVIL_LAND, 0.8F, 0.6F);
            HeroFx.shake(level, player.position(), 16.0D, 0.4F, 8);
        }
    }

    public static void tickMovement(Player player) {
        if (!player.level().isClientSide || !player.isLocalPlayer() || !isThing(player) || !player.isInWater()) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        player.setSwimming(false);
        player.setDeltaMovement(motion.x, Math.min(motion.y, -0.08D), motion.z);
    }

    private static void tickPlating(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        long last = LAST_HURT.getOrDefault(player.getUUID(), 0L);
        if (now - last < MarvelBalanceConfig.ROCK_PLATING_DELAY_SECONDS.get() * 20L) {
            return;
        }
        float max = MarvelBalanceConfig.ROCK_PLATING_MAX.get().floatValue();
        float current = player.getAbsorptionAmount();
        if (current < max) {
            player.setAbsorptionAmount(Math.min(max, current + MarvelBalanceConfig.ROCK_PLATING_REGEN.get().floatValue()));
        }
    }

    private static boolean smashable(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES)
                || state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_TRAPDOORS);
    }

    private static void smashThrough(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 ahead = Vec3.directionFromRotation(0.0F, player.getYRot()).scale(0.9D);
        for (int h = 0; h < 2; h++) {
            BlockPos pos = BlockPos.containing(player.getX() + ahead.x, player.getY() + 0.2D + h, player.getZ() + ahead.z);
            BlockState state = level.getBlockState(pos);
            if (!smashable(state) || !level.mayInteract(player, pos)) {
                continue;
            }
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
            NeoForge.EVENT_BUS.post(event);
            if (!event.isCanceled()) {
                level.destroyBlock(pos, true, player);
            }
        }
    }

    private static void crater(ServerPlayer player, float amount) {
        ServerLevel level = player.serverLevel();
        BlockState below = level.getBlockState(player.blockPosition().below());
        if (!below.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), player.getX(), player.getY() + 0.1D, player.getZ(),
                    40, 1.2D, 0.1D, 1.2D, 0.2D);
        }
        HeroShockwaves.ring(player, 8, Math.min(6, 2 + (int) (amount / 4.0F)), MarvelBalanceConfig.CRATER_DAMAGE.get().floatValue());
        HeroFx.shake(level, player.position(), 16.0D, 0.6F, 12);
        HeroFx.play(player, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.6F);
    }

    private static void launch(ServerPlayer player, LivingEntity target, double horizontal, double vertical) {
        Vec3 push = target.position().subtract(player.position());
        Vec3 flat = new Vec3(push.x, 0.0D, push.z);
        flat = flat.lengthSqr() < 1.0E-4D ? player.getLookAngle() : flat.normalize();
        target.setDeltaMovement(flat.x * horizontal, vertical, flat.z * horizontal);
        target.hurtMarked = true;
        Vec3 at = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        player.serverLevel().sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static LivingEntity reach(ServerPlayer player, double range) {
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 1);
        return beam.targets().isEmpty() ? null : beam.targets().get(0);
    }

    private static double clobber(ServerPlayer player, int slot) {
        LivingEntity target = reach(player, 4.5D);
        if (target == null) {
            return 0.0D;
        }
        float damage = MarvelBalanceConfig.CLOBBER_DAMAGE.get().floatValue();
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
        launch(player, target, 2.6D, 1.0D);
        HeroFx.shake(player.serverLevel(), target.position(), 12.0D, 0.7F, 10);
        HeroFx.play(player, SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 1.2F);
        HeroFx.play(player, SoundEvents.PLAYER_ATTACK_CRIT, 1.2F, 0.6F);
        HeroAnimations.play(player, "uppercut");
        player.displayClientMessage(Component.literal("§6It's clobberin' time!"), true);
        return MarvelBalanceConfig.CLOBBER_COOLDOWN.get();
    }

    private static double wrestle(ServerPlayer player, int slot) {
        if (WRESTLES.containsKey(player.getUUID())) {
            return 0.0D;
        }
        LivingEntity target = reach(player, 4.0D);
        if (target == null || !HeroAbilities.canControl(target)) {
            return 0.0D;
        }
        WRESTLES.put(player.getUUID(), new Wrestle(target));
        HeroStun.stun(target, 24);
        HeroFx.play(player, SoundEvents.RAVAGER_ROAR, 0.6F, 1.4F);
        HeroAnimations.play(player, "bettercombat:two_handed_slam");
        return MarvelBalanceConfig.WRESTLE_COOLDOWN.get();
    }

    private static void tickWrestle(ServerPlayer player) {
        Wrestle wrestle = WRESTLES.get(player.getUUID());
        if (wrestle == null) {
            return;
        }
        LivingEntity target = wrestle.target;
        if (!target.isAlive() || target.level() != player.level()) {
            WRESTLES.remove(player.getUUID());
            return;
        }
        wrestle.ticks++;
        if (wrestle.ticks < 12) {
            Vec3 above = player.position().add(0.0D, player.getBbHeight() + 0.3D, 0.0D);
            move(target, above);
            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0.0F;
            return;
        }
        WRESTLES.remove(player.getUUID());
        Vec3 behind = player.position().subtract(Vec3.directionFromRotation(0.0F, player.getYRot()).scale(1.5D)).add(0.0D, 0.5D, 0.0D);
        move(target, behind);
        target.setDeltaMovement(0.0D, -1.5D, 0.0D);
        target.hurtMarked = true;
        float damage = MarvelBalanceConfig.WRESTLE_DAMAGE.get().floatValue();
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
        ServerLevel level = player.serverLevel();
        BlockState ground = level.getBlockState(BlockPos.containing(behind).below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), behind.x, behind.y, behind.z, 30, 0.8D, 0.1D, 0.8D, 0.2D);
        }
        HeroFx.shake(level, behind, 12.0D, 0.6F, 10);
        HeroFx.play(player, SoundEvents.ANVIL_LAND, 1.0F, 0.6F);
    }

    private static void move(LivingEntity target, Vec3 to) {
        if (target instanceof ServerPlayer victim) {
            victim.teleportTo(to.x, to.y, to.z);
        } else {
            target.setPos(to.x, to.y, to.z);
        }
    }

    private static HeroBlockLift.Profile tossProfile() {
        return new HeroBlockLift.Profile(5.0D, MarvelBalanceConfig.TOSS_MAX_BLOCKS.get(), 100, MarvelBalanceConfig.TOSS_THROW_SPEED.get(),
                MarvelBalanceConfig.TOSS_DAMAGE.get(),
                state -> !(state.getBlock() instanceof LiquidBlock) && !state.is(BlockTags.BEDS) && !state.hasBlockEntity() && state.isSolid(),
                (level, center) -> level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ROCK), center.x, center.y, center.z, 2, 0.4D, 0.4D, 0.4D, 0.0D),
                () -> SoundEvents.STONE_BREAK, () -> SoundEvents.GRAVEL_STEP, () -> SoundEvents.PLAYER_ATTACK_SWEEP);
    }

    private static double ripAndToss(ServerPlayer player, int slot) {
        if (HeroBlockLift.isLifting(player)) {
            HeroBlockLift.throwHeld(player);
            HeroAnimations.play(player, "celestisynth:cs_crescentia_throw");
            HeroFx.play(player, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.6F);
            return MarvelBalanceConfig.TOSS_COOLDOWN.get();
        }
        if (HeroBlockLift.lift(player, tossProfile())) {
            HeroAnimations.play(player, "bettercombat:two_handed_slam");
            HeroFx.play(player, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 0.8F, 0.6F);
        }
        return 0.0D;
    }

    private static double brace(ServerPlayer player, int slot) {
        PlayerData data = HeroPowers.data(player);
        if (data.hasState(BRACE_STATE)) {
            data.removeState(BRACE_STATE);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        } else {
            data.addState(BRACE_STATE);
            HeroFx.play(player, SoundEvents.ANVIL_PLACE, 0.6F, 0.6F);
        }
        PlayerData.sync(player);
        return 0.5D;
    }
}
