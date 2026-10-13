package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.registry.ModSounds;
import com.craisinlord.integrated_superheroes.registry.HeroEntities;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class SquirrelGirlPower {
    public static final String ID = "squirrel_powers_doreen_green";
    public static final String SWARM_STATE = "integrated_superheroes_swarm";
    public static final String ORDERS_ABILITY = "swarm_orders";
    public static final String DEFEND = "defend";
    public static final String FORAGE = "forage";
    public static final String GNAW = "gnaw";
    public static final String STAY = "stay";
    public static final double GNAW_REACH = 3.0D;
    private static final Map<Holder<MobEffect>, Integer> EFFECTS = Map.of(MobEffects.MOVEMENT_SPEED, 0, MobEffects.JUMP, 0);
    private static final Map<Holder<MobEffect>, HeroPower.Toggle> EFFECT_TOGGLES = Map.of(
            MobEffects.MOVEMENT_SPEED, HeroToggles.SPEED, MobEffects.JUMP, HeroToggles.JUMP);
    private static final List<HeroWheels.Option> OPTIONS = List.of(
            new HeroWheels.Option(DEFEND, "Defend", () -> Items.SHIELD),
            new HeroWheels.Option(FORAGE, "Forage", () -> HeroFx.item("antarchy:ouranwood_acorn", Items.SWEET_BERRIES)),
            new HeroWheels.Option(GNAW, "Gnaw", () -> Items.OAK_LOG),
            new HeroWheels.Option(STAY, "Stay", () -> Items.LEAD)
    );
    private static final Map<UUID, String> ORDERS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_SQUIRREL = new HashMap<>();
    private static final Map<UUID, UUID> TIPPY = new HashMap<>();
    private static final Map<UUID, Long> TIPPY_BACK = new HashMap<>();
    private static final Map<UUID, HeroGnaw> GNAWING = new HashMap<>();

    private SquirrelGirlPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        HeroWheels.register(new HeroWheels.Wheel(ORDERS_ABILITY, OPTIONS, (player, option) -> true, SquirrelGirlPower::giveOrder));
        return new HeroPower(
                ID,
                "Squirrel Powers (Doreen Green)",
                syringe,
                MarvelBalanceConfig.SQUIRREL_GIRL,
                List.of("agility"),
                List.of(
                        new HeroPower.Ability("call_swarm", "Call the Swarm", SquirrelGirlPower::callSwarm),
                        new HeroPower.Ability("sic_em", "Sic 'Em", SquirrelGirlPower::sicEm),
                        new HeroPower.Ability("squirrel_pile", "Squirrel Pile", SquirrelGirlPower::squirrelPile),
                        new HeroPower.Ability(ORDERS_ABILITY, "Swarm Orders", (player, slot) -> 0.0D),
                        HeroToggles.WHEEL
                ),
                List.of(HeroToggles.SPEED, HeroToggles.JUMP),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (player.tickCount % 20 == 0) {
                            HeroToggles.applyEffects(player, EFFECTS, EFFECT_TOGGLES);
                        }
                        if (player.tickCount % 5 == 0) {
                            tickSwarm(player);
                        }
                        tickGnaw(player);
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroPowers.data(player).removeState(SWARM_STATE);
                        EFFECTS.keySet().forEach(effect -> {
                            MobEffectInstance current = player.getEffect(effect);
                            if (current != null && current.isInfiniteDuration() && current.isAmbient()) {
                                player.removeEffect(effect);
                            }
                        });
                        squirrels(player).forEach(SwarmSquirrel::vanish);
                        SwarmSquirrel tippy = tippy(player);
                        if (tippy != null) {
                            tippy.vanish();
                        }
                        stopGnaw(player);
                        ORDERS.remove(player.getUUID());
                        NEXT_SQUIRREL.remove(player.getUUID());
                        TIPPY.remove(player.getUUID());
                        TIPPY_BACK.remove(player.getUUID());
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != player
                                && !(attacker instanceof SwarmSquirrel)) {
                            SwarmSquirrel tippy = tippy(player);
                            if (tippy != null) {
                                tippy.defend(attacker);
                            }
                        }
                    }
                }
        );
    }

    public static boolean has(Player player) {
        return ID.equals(HeroPowers.data(player).getPrimaryPower());
    }

    public static boolean swarmOut(Player player) {
        return has(player) && HeroPowers.data(player).hasState(SWARM_STATE);
    }

    public static String order(Player player) {
        return ORDERS.getOrDefault(player.getUUID(), DEFEND);
    }

    public static boolean isTippy(Player player, SwarmSquirrel squirrel) {
        return squirrel.getUUID().equals(TIPPY.get(player.getUUID()));
    }

    public static void tippyLost(ServerPlayer player) {
        TIPPY.remove(player.getUUID());
        TIPPY_BACK.put(player.getUUID(), player.serverLevel().getGameTime() + MarvelBalanceConfig.TIPPY_RESPAWN_SECONDS.get() * 20L);
        player.displayClientMessage(Component.literal("§6Tippy-Toe is down! She'll be back."), true);
    }

    public static BlockPos gnawTarget(Player player) {
        HeroGnaw gnaw = GNAWING.get(player.getUUID());
        return gnaw == null ? null : gnaw.target();
    }

    private static List<SwarmSquirrel> squirrels(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(SwarmSquirrel.class, player.getBoundingBox().inflate(64.0D),
                squirrel -> squirrel.isAlive() && !squirrel.isTippy() && squirrel.ownedBy(player));
    }

    private static SwarmSquirrel tippy(ServerPlayer player) {
        UUID id = TIPPY.get(player.getUUID());
        if (id == null) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(id);
        if (entity instanceof SwarmSquirrel squirrel && squirrel.isAlive()) {
            return squirrel;
        }
        TIPPY.remove(player.getUUID());
        return null;
    }

    private static void tickSwarm(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        if (tippy(player) == null && now >= TIPPY_BACK.getOrDefault(player.getUUID(), 0L) && !player.isSpectator()) {
            SwarmSquirrel tippy = spawn(player, true, false);
            if (tippy != null) {
                TIPPY.put(player.getUUID(), tippy.getUUID());
                tippy.mountShoulder(player);
            }
        }
        if (!swarmOut(player)) {
            return;
        }
        if (squirrels(player).size() < MarvelBalanceConfig.SWARM_SIZE.get() && now >= NEXT_SQUIRREL.getOrDefault(player.getUUID(), 0L)) {
            SwarmSquirrel squirrel = spawn(player, false, true);
            if (squirrel != null && STAY.equals(order(player))) {
                squirrel.setOrderedToSit(true);
            }
            NEXT_SQUIRREL.put(player.getUUID(), now + (long) (MarvelBalanceConfig.SWARM_RESPAWN_SECONDS.get() * 20.0D));
        }
    }

    private static SwarmSquirrel spawn(ServerPlayer player, boolean tippy, boolean dramatic) {
        ServerLevel level = player.serverLevel();
        SwarmSquirrel squirrel = HeroEntities.SWARM_SQUIRREL.get().create(level);
        if (squirrel == null) {
            return null;
        }
        float bite = (tippy ? MarvelBalanceConfig.TIPPY_BITE_DAMAGE.get() : MarvelBalanceConfig.SWARM_BITE_DAMAGE.get()).floatValue();
        squirrel.setup(player, tippy, bite, MarvelBalanceConfig.SWARM_SQUIRREL_HEALTH.get());
        BlockPos ground = tippy ? player.blockPosition() : groundNear(player);
        BlockPos sky = ground.above(8);
        boolean glide = dramatic && level.canSeeSky(ground) && level.isEmptyBlock(sky) && level.isEmptyBlock(sky.above()) && level.random.nextBoolean();
        if (glide) {
            squirrel.moveTo(sky.getX() + 0.5D, sky.getY(), sky.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        } else {
            squirrel.moveTo(ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        }
        level.addFreshEntity(squirrel);
        if (glide) {
            squirrel.startGliding(ground);
        } else if (dramatic) {
            BlockState below = level.getBlockState(ground.below());
            if (!below.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), ground.getX() + 0.5D, ground.getY() + 0.1D,
                        ground.getZ() + 0.5D, 12, 0.25D, 0.1D, 0.25D, 0.1D);
            }
            squirrel.setDeltaMovement(level.random.nextGaussian() * 0.1D, 0.35D, level.random.nextGaussian() * 0.1D);
            level.playSound(null, ground, below.getSoundType().getBreakSound(), squirrel.getSoundSource(), 0.6F, 1.4F);
        }
        return squirrel;
    }

    private static BlockPos groundNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = 2.0D + level.random.nextDouble() * 3.0D;
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos pos = new BlockPos(x, origin.getY() + dy, z);
                if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())
                        && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
                    return pos;
                }
            }
        }
        return origin;
    }

    private static double callSwarm(ServerPlayer player, int slot) {
        PlayerData data = HeroPowers.data(player);
        if (data.hasState(SWARM_STATE)) {
            data.removeState(SWARM_STATE);
            squirrels(player).forEach(SwarmSquirrel::vanish);
            stopGnaw(player);
            HeroFx.play(player, ModSounds.SWARM_SQUIRREL_BEG.get(), 0.8F, 0.8F);
            player.displayClientMessage(Component.literal("§6The swarm scatters."), true);
        } else {
            data.addState(SWARM_STATE);
            int size = MarvelBalanceConfig.SWARM_SIZE.get();
            for (int i = 0; i < size; i++) {
                SwarmSquirrel squirrel = spawn(player, false, true);
                if (squirrel != null && STAY.equals(order(player))) {
                    squirrel.setOrderedToSit(true);
                }
            }
            NEXT_SQUIRREL.put(player.getUUID(), player.serverLevel().getGameTime() + (long) (MarvelBalanceConfig.SWARM_RESPAWN_SECONDS.get() * 20.0D));
            HeroFx.play(player, ModSounds.SWARM_SQUIRREL_IDLE.get(), 1.2F, 1.0F);
            HeroFx.play(player, ModSounds.SWARM_SQUIRREL_BEG.get(), 1.0F, 1.3F);
            HeroAnimations.play(player, "web_shoot_both");
            player.displayClientMessage(Component.literal("§6Squirrels, assemble!"), true);
        }
        PlayerData.sync(player);
        return MarvelBalanceConfig.SWARM_CALL_COOLDOWN.get();
    }

    private static LivingEntity aimed(ServerPlayer player, double range) {
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 8);
        return beam.targets().stream()
                .filter(entity -> !(entity instanceof SwarmSquirrel squirrel && squirrel.ownedBy(player)))
                .findFirst()
                .orElse(null);
    }

    private static boolean needSwarm(ServerPlayer player) {
        if (swarmOut(player) && !squirrels(player).isEmpty()) {
            return true;
        }
        player.displayClientMessage(Component.literal("§7Call the swarm first."), true);
        return false;
    }

    private static double sicEm(ServerPlayer player, int slot) {
        if (!needSwarm(player)) {
            return 0.0D;
        }
        double range = MarvelBalanceConfig.SIC_EM_RANGE.get();
        LivingEntity target = aimed(player, range);
        List<SwarmSquirrel> swarm = squirrels(player);
        if (target != null) {
            swarm.forEach(squirrel -> {
                squirrel.setOrderedToSit(false);
                squirrel.focus(target);
            });
            Vec3 top = target.position().add(0.0D, target.getBbHeight() + 0.3D, 0.0D);
            player.serverLevel().sendParticles(ParticleTypes.ANGRY_VILLAGER, top.x, top.y, top.z, 3, 0.3D, 0.2D, 0.3D, 0.0D);
            player.displayClientMessage(Component.literal("§6Sic 'em!"), true);
        } else {
            Vec3 eye = player.getEyePosition();
            BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(range)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                return 0.0D;
            }
            BlockPos guard = hit.getBlockPos().relative(hit.getDirection());
            swarm.forEach(squirrel -> {
                squirrel.setOrderedToSit(false);
                squirrel.guard(guard);
            });
            player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, guard.getX() + 0.5D, guard.getY() + 0.3D, guard.getZ() + 0.5D,
                    8, 0.4D, 0.2D, 0.4D, 0.0D);
            player.displayClientMessage(Component.literal("§6Guard that spot!"), true);
        }
        HeroFx.play(player, ModSounds.SWARM_SQUIRREL_BEG.get(), 1.0F, 1.5F);
        HeroAnimations.play(player, "web_shoot_r");
        return MarvelBalanceConfig.SIC_EM_COOLDOWN.get();
    }

    private static double squirrelPile(ServerPlayer player, int slot) {
        if (!needSwarm(player)) {
            return 0.0D;
        }
        LivingEntity target = aimed(player, MarvelBalanceConfig.SIC_EM_RANGE.get());
        if (target == null) {
            return 0.0D;
        }
        int ticks = MarvelBalanceConfig.PILE_TICKS.get();
        float damage = MarvelBalanceConfig.PILE_BITE_DAMAGE.get().floatValue();
        List<SwarmSquirrel> swarm = squirrels(player).stream()
                .sorted(Comparator.comparingDouble(squirrel -> squirrel.distanceToSqr(target)))
                .toList();
        for (int i = 0; i < swarm.size(); i++) {
            SwarmSquirrel squirrel = swarm.get(i);
            squirrel.setOrderedToSit(false);
            squirrel.clearGuard();
            squirrel.pile(target, ticks, damage, i);
        }
        if (HeroAbilities.canControl(target)) {
            HeroStun.stun(target, ticks);
        }
        if (swarm.size() >= MarvelBalanceConfig.SWARM_SIZE.get()) {
            HeroAdvancements.award(player, "street_level/squirrel_girl/squirrel_pile");
        }
        HeroFx.play(target, ModSounds.SWARM_SQUIRREL_BEG.get(), 1.5F, 1.1F);
        HeroFx.play(target, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.6F);
        HeroAnimations.play(player, "web_shoot_both");
        player.displayClientMessage(Component.literal("§6Squirrel pile!"), true);
        return MarvelBalanceConfig.PILE_COOLDOWN.get();
    }

    private static void giveOrder(ServerPlayer player, String choice) {
        stopGnaw(player);
        String option = choice;
        if (GNAW.equals(choice) && !startGnaw(player)) {
            player.displayClientMessage(Component.literal("§7Look at a block for the swarm to gnaw."), true);
            option = DEFEND;
        }
        setOrder(player, option);
    }

    private static void setOrder(ServerPlayer player, String option) {
        ORDERS.put(player.getUUID(), option);
        boolean stay = STAY.equals(option);
        squirrels(player).forEach(squirrel -> {
            squirrel.clearGuard();
            squirrel.setOrderedToSit(stay);
            squirrel.setTarget(null);
            squirrel.getNavigation().stop();
        });
        String label = OPTIONS.stream().filter(entry -> entry.id().equals(option)).map(HeroWheels.Option::label).findFirst().orElse(option);
        player.displayClientMessage(Component.literal("§6Swarm: " + label), true);
        HeroFx.play(player, ModSounds.SWARM_SQUIRREL_IDLE.get(), 0.8F, 1.2F);
    }

    private static boolean startGnaw(ServerPlayer player) {
        HeroGnaw gnaw = HeroGnaw.aim(player, MarvelBalanceConfig.SIC_EM_RANGE.get(), state -> true, crackId(player));
        if (gnaw == null) {
            return false;
        }
        GNAWING.put(player.getUUID(), gnaw);
        return true;
    }

    private static void stopGnaw(ServerPlayer player) {
        HeroGnaw gnaw = GNAWING.remove(player.getUUID());
        if (gnaw != null) {
            gnaw.stop(player.serverLevel());
        }
    }

    private static int crackId(ServerPlayer player) {
        return -1000 - player.getId();
    }

    private static void tickGnaw(ServerPlayer player) {
        HeroGnaw gnaw = GNAWING.get(player.getUUID());
        if (gnaw == null) {
            return;
        }
        if (!swarmOut(player) || !GNAW.equals(order(player))) {
            stopGnaw(player);
            return;
        }
        HeroGnaw.Step step = gnaw.tick(player,
                center -> squirrels(player).stream().filter(squirrel -> squirrel.distanceToSqr(center) < GNAW_REACH * GNAW_REACH).count(),
                MarvelBalanceConfig.GNAW_TICKS_PER_HARDNESS.get(), MarvelBalanceConfig.GNAW_DROPS.get(), MarvelBalanceConfig.GNAW_MAX_BLOCKS.get());
        if (step == HeroGnaw.Step.DONE) {
            stopGnaw(player);
            setOrder(player, DEFEND);
            player.displayClientMessage(Component.literal("§6The swarm finished gnawing."), true);
        } else if (step == HeroGnaw.Step.BROKE) {
            HeroAdvancements.count(player, "street_level/squirrel_girl/nuts_about_you", 1.0D, 100.0D);
        }
    }
}
