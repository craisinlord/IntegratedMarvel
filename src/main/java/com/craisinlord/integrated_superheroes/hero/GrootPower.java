package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.api.RogueAbsorption;
import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.craisinlord.integrated_superheroes.hero.skin.SerumCostumes;
import com.stxllarplanes.marvellegacy.init.AttachmentsRegistry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class GrootPower {
    public static final String ID = "flora_colossus_groot";
    public static final String BRANCH = "groot_branch_arm";
    public static final HeroPower.Toggle BRANCH_VISUALS = new HeroPower.Toggle("integrated_superheroes_off_groot_branch", "Growth Visuals", () -> Items.OAK_SAPLING);
    public static final HeroPower.Toggle FALLING_LEAVES = new HeroPower.Toggle("integrated_superheroes_off_groot_leaves", "Falling Leaves", () -> Items.OAK_LEAVES);
    private static final int STRIKE_TICKS = 8;
    private static final int PULL_TICKS = 8;
    private static final int REEL_TICKS = 20;
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static RogueMemory rogue;
    private static boolean registered;

    private static final class State {
        private UUID hooked;
        private long hookUntil;
        private UUID reeled;
        private long reelStart;
    }

    private GrootPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(GrootPower::onLogout);
            NeoForge.EVENT_BUS.addListener(GrootPower::onChangeDimension);
            NeoForge.EVENT_BUS.addListener(GrootPower::onStopping);
            SerumCostumes.register(ID, "thematic:groot");
            RogueAbsorption.remember(rogueMemory());
        }
        HeroWheels.register(GrootRoots.wheel());
        return new HeroPower(
                ID,
                "Flora Colossus (Groot)",
                syringe,
                GrootConfig.STATS,
                List.of(),
                List.of(
                        new HeroPower.Ability(BRANCH, "Branch Arm", GrootPower::branch),
                        new HeroPower.Ability(GrootRoots.WHEEL, "Root Wheel", (player, slot) -> 0.0D),
                        new HeroPower.Ability(GrootRoots.GROW, "Grow", GrootRoots::grow),
                        HeroToggles.WHEEL
                ),
                List.of(BRANCH_VISUALS, FALLING_LEAVES, GrootRoots.PHOTOSYNTHESIS),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        tickState(player);
                        GrootRoots.tick(player);
                        if (HeroToggles.isOn(player, FALLING_LEAVES) && player.tickCount % 12 == 0 && player.getRandom().nextInt(3) == 0) {
                            leaves(player.serverLevel(), player.position().add(0.0D, player.getBbHeight() * 0.9D, 0.0D), 1, 0.4D);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        STATES.remove(player.getUUID());
                        GrootRoots.clear(player);
                        GrootBranches.clear(player);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        GrootRoots.incomingDamage(player, event);
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        clear(player);
                    }
                }
        );
    }

    public static boolean isGroot(Player player) {
        return player.hasData(AttachmentsRegistry.PLAYER_DATA.get())
                && ID.equals(HeroPowers.data(player).getPrimaryPower());
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
    }

    static Vec3 hand(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0D, flat.x);
        double scale = player.getScale();
        return player.getEyePosition().add(right.scale(0.35D * scale)).add(0.0D, -0.4D * scale, 0.0D).add(look.scale(0.3D));
    }

    private static boolean pullable(ServerPlayer player, LivingEntity target) {
        return target.isAlive() && !(target instanceof Player) && !(target instanceof ArmorStand) && KineticBurst.affects(player, target)
                && HeroAbilities.canControl(target) && !HeroAbilities.isBoss(target);
    }

    static void woodLine(ServerPlayer player, Vec3 from, Vec3 to) {
        if (!HeroToggles.isOn(player, BRANCH_VISUALS)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()), to.x, to.y, to.z, 6, 0.15D, 0.15D, 0.15D, 0.05D);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), to.x, to.y, to.z, 3, 0.2D, 0.2D, 0.2D, 0.0D);
    }

    private static void leaves(ServerLevel level, Vec3 at, int count, double spread) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), at.x, at.y, at.z, count, spread, 0.2D, spread, 0.0D);
    }

    private static double range(ServerPlayer player) {
        double range = GrootConfig.BRANCH_RANGE.get();
        return GrootRoots.rooted(player) ? range * GrootConfig.TAKE_ROOT_BRANCH_RANGE_MULTIPLIER.get() : range;
    }

    private static double branch(ServerPlayer player, int slot) {
        State state = state(player);
        long now = player.level().getGameTime();
        if (state.hooked != null && now < state.hookUntil) {
            return pull(player, state);
        }
        double range = range(player);
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 1);
        Vec3 from = hand(player);
        LivingEntity target = beam.targets().isEmpty() ? null : beam.targets().get(0);
        Vec3 point = target == null ? beam.end() : target.getBoundingBox().getCenter();
        woodLine(player, from, point);
        player.swing(InteractionHand.MAIN_HAND, true);
        HeroFx.play(player, SoundEvents.WOOD_BREAK, 1.0F, 0.7F);
        if (target == null || !KineticBurst.affects(player, target)) {
            if (beam.hitBlock()) {
                HeroFx.play(player, SoundEvents.WOOD_HIT, 1.0F, 0.8F);
            }
            GrootBranches.send(player, GrootBranches.STRIKE, null, point, STRIKE_TICKS);
            return GrootConfig.BRANCH_COOLDOWN.get();
        }
        float damage = (float) (MarvelBalanceConfig.STRETCH_PUNCH_DAMAGE.get() * GrootConfig.BRANCH_DAMAGE_MULTIPLIER.get()) * GrootRoots.markBonus(target);
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
        Vec3 look = player.getLookAngle();
        target.knockback(0.35D, -look.x, -look.z);
        HeroFx.play(target, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.7F);
        leaves(player.serverLevel(), point, 4, 0.3D);
        if (target.isAlive() && pullable(player, target)) {
            state.hooked = target.getUUID();
            state.hookUntil = now + GrootConfig.PULL_WINDOW_TICKS.get();
            GrootBranches.send(player, GrootBranches.HOLD, target, point, GrootConfig.PULL_WINDOW_TICKS.get());
            player.displayClientMessage(Component.literal("§aGot it. Press again to pull it in."), true);
            return 0.0D;
        }
        GrootBranches.send(player, GrootBranches.STRIKE, target, point, STRIKE_TICKS);
        return GrootConfig.BRANCH_COOLDOWN.get();
    }

    private static double pull(ServerPlayer player, State state) {
        Entity found = player.serverLevel().getEntity(state.hooked);
        state.hooked = null;
        state.hookUntil = 0L;
        double range = range(player) + 3.0D;
        if (!(found instanceof LivingEntity target) || !pullable(player, target) || target.distanceTo(player) > range || !player.hasLineOfSight(target)) {
            player.displayClientMessage(Component.literal("§7Lost your grip."), true);
            GrootBranches.clear(player);
            return GrootConfig.BRANCH_COOLDOWN.get();
        }
        Vec3 to = player.position().subtract(target.position());
        double distance = to.length();
        if (distance > 1.5D) {
            Vec3 flat = new Vec3(to.x, 0.0D, to.z);
            double speed = Math.min(GrootConfig.PULL_SPEED.get(), 0.3D + distance * 0.12D);
            Vec3 push = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize().scale(speed);
            double lift = Mth.clamp(to.y * 0.1D, -0.1D, 0.5D) + 0.3D;
            target.setDeltaMovement(push.x, lift, push.z);
            target.hurtMarked = true;
        }
        state.reeled = target.getUUID();
        state.reelStart = player.level().getGameTime();
        Vec3 center = target.getBoundingBox().getCenter();
        woodLine(player, hand(player), center);
        GrootBranches.send(player, GrootBranches.PULL, target, center, PULL_TICKS);
        HeroFx.play(player, SoundEvents.WOOD_PLACE, 1.0F, 0.6F);
        HeroFx.play(target, SoundEvents.VINE_STEP, 1.2F, 0.8F);
        player.swing(InteractionHand.MAIN_HAND, true);
        HeroAdvancements.award(player, "avengers/groot/branching_out");
        return GrootConfig.BRANCH_COOLDOWN.get();
    }

    private static void tickState(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (state.hooked != null && now >= state.hookUntil) {
            state.hooked = null;
            state.hookUntil = 0L;
            HeroAbilities.setCooldown(player, BRANCH, GrootConfig.BRANCH_COOLDOWN.get());
        }
        if (state.reeled != null) {
            long elapsed = now - state.reelStart;
            Entity found = player.serverLevel().getEntity(state.reeled);
            if (!(found instanceof LivingEntity target) || !target.isAlive() || elapsed > REEL_TICKS) {
                state.reeled = null;
            } else if (elapsed >= 3 && (target.distanceTo(player) <= 2.5D || target.onGround())) {
                HeroStun.stun(target, GrootConfig.PULL_STUN_TICKS.get());
                player.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()),
                        target.getX(), target.getY() + target.getBbHeight(), target.getZ(), 6, 0.3D, 0.1D, 0.3D, 0.0D);
                state.reeled = null;
            }
        }
        if (state.hooked == null && state.reeled == null) {
            STATES.remove(player.getUUID());
        }
    }

    public static RogueMemory rogueMemory() {
        if (rogue == null) {
            rogue = RogueKit.memory("groot", "Groot")
                    .active("Skewer", GrootPower::rogueSkewer)
                    .build();
        }
        return rogue;
    }

    private static double rogueSkewer(ServerPlayer player) {
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), GrootConfig.SKEWER_RANGE.get(), GrootConfig.SKEWER_MAX_TARGETS.get());
        Vec3 point = beam.end();
        ServerLevel level = player.serverLevel();
        HeroAbilities.particleLine(level, hand(player), point, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()), 0.3D);
        GrootBranches.send(player, GrootBranches.STRIKE, null, point, STRIKE_TICKS);
        player.swing(InteractionHand.MAIN_HAND, true);
        HeroFx.play(player, SoundEvents.WOOD_BREAK, 1.0F, 0.7F);
        float damage = RogueKit.dmg(MarvelBalanceConfig.STRETCH_PUNCH_DAMAGE.get() * GrootConfig.ROGUE_DAMAGE_MULTIPLIER.get());
        for (LivingEntity target : beam.targets()) {
            if (RogueKit.affects(player, target)) {
                HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
                HeroFx.play(target, SoundEvents.THORNS_HIT, 1.0F, 0.6F);
            }
        }
        return GrootConfig.SKEWER_COOLDOWN.get();
    }

    private static void clear(ServerPlayer player) {
        State state = STATES.remove(player.getUUID());
        GrootRoots.stop(player);
        GrootBranches.clear(player);
        if (state != null && state.hooked != null && SerumKit.is(player, ID)) {
            HeroAbilities.setCooldown(player, BRANCH, GrootConfig.BRANCH_COOLDOWN.get());
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    private static void onStopping(ServerStoppingEvent event) {
        STATES.clear();
        GrootRoots.clearAll();
    }
}
