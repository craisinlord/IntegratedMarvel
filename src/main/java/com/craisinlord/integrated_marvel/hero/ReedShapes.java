package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.registry.HeroEntities;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReedShapes {
    public static final String ABILITY = "shape_shift";
    public static final String USE_ABILITY = "use_shape";
    public static final String PARACHUTE = "parachute";
    public static final String BOAT = "boat";
    public static final String SOFA = "sofa";
    public static final String TRAMPOLINE = "trampoline";
    public static final String SLINGSHOT = "slingshot";
    public static final String BALL = "bouncy_ball";
    public static final String NECK = "periscope_neck";
    public static final String TRAMPOLINE_STATE = "integrated_marvel_trampoline";
    public static final String BALL_STATE = "integrated_marvel_ball";
    public static final String NECK_STATE = "integrated_marvel_neck";
    private static final ResourceLocation ROOTED = ResourceLocation.fromNamespaceAndPath("integrated_marvel", "reed_rooted");
    private static final List<HeroWheels.Option> OPTIONS = List.of(
            new HeroWheels.Option(BOAT, "Boat", () -> Items.OAK_BOAT),
            new HeroWheels.Option(SOFA, "Sofa", () -> Items.RED_WOOL),
            new HeroWheels.Option(TRAMPOLINE, "Trampoline", () -> Items.SLIME_BLOCK),
            new HeroWheels.Option(SLINGSHOT, "Slingshot", () -> Items.STRING),
            new HeroWheels.Option(BALL, "Bouncy Ball", () -> Items.SLIME_BALL),
            new HeroWheels.Option(NECK, "Periscope Neck", () -> Items.SPYGLASS),
            new HeroWheels.Option(PARACHUTE, "Parachute", () -> Items.FEATHER)
    );
    private static final Map<UUID, Vec3> ANCHORS = new HashMap<>();
    private static final Map<UUID, String> SELECTED = new HashMap<>();
    private static final Map<UUID, Long> BALL_UNTIL = new HashMap<>();
    private static final Map<UUID, Integer> SLINGS = new HashMap<>();
    private static final Map<Integer, Long> LAST_HIT = new HashMap<>();
    private static final Map<UUID, Vec3> CLIENT_LAST_MOTION = new HashMap<>();

    private ReedShapes() {
    }

    public static HeroWheels.Wheel wheel() {
        return new HeroWheels.Wheel(ABILITY, OPTIONS, (player, option) -> true, ReedShapes::select);
    }

    public static boolean has(Player player, String state) {
        return MrFantasticPower.isReed(player) && HeroPowers.data(player).hasState(state);
    }

    public static FantasticSeat seat(Player player) {
        return player.getVehicle() instanceof FantasticSeat seat && seat.getFirstPassenger() == player ? seat : null;
    }

    private static String label(String option) {
        return OPTIONS.stream().filter(entry -> entry.id().equals(option)).map(HeroWheels.Option::label).findFirst().orElse(option);
    }

    private static void select(ServerPlayer player, String option) {
        SELECTED.put(player.getUUID(), option);
        player.displayClientMessage(Component.literal("§9Shape selected: " + label(option)), true);
        HeroFx.play(player, SoundEvents.SLIME_SQUISH_SMALL, 0.8F, 1.3F);
    }

    public static double use(ServerPlayer player, int slot) {
        if (end(player) != null) {
            HeroFx.play(player, SoundEvents.SLIME_SQUISH_SMALL, 1.0F, 0.8F);
            return 0.5D;
        }
        String option = SELECTED.getOrDefault(player.getUUID(), SLINGSHOT);
        boolean started = switch (option) {
            case BOAT -> startSeat(player, false);
            case SOFA -> startSeat(player, true);
            case TRAMPOLINE -> startTrampoline(player);
            case SLINGSHOT -> startSlingshot(player);
            case BALL -> startBall(player);
            case NECK -> startNeck(player);
            case PARACHUTE -> startParachute(player);
            default -> false;
        };
        if (!started) {
            return 0.0D;
        }
        player.displayClientMessage(Component.literal("§9" + label(option) + "!"), true);
        HeroFx.play(player, SoundEvents.SLIME_SQUISH, 1.0F, 0.9F + player.getRandom().nextFloat() * 0.4F);
        player.serverLevel().sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SLIME_BALL)),
                player.getX(), player.getY() + 1.0D, player.getZ(), 12, 0.4D, 0.5D, 0.4D, 0.05D);
        return SLINGSHOT.equals(option) ? MarvelBalanceConfig.SLINGSHOT_COOLDOWN.get() : MarvelBalanceConfig.SHAPE_COOLDOWN.get();
    }

    private static boolean startParachute(ServerPlayer player) {
        HeroPowers.data(player).addState(MrFantasticPower.PARACHUTE_STATE);
        PlayerData.sync(player);
        return true;
    }

    public static String end(ServerPlayer player) {
        String previous = null;
        FantasticSeat seat = seat(player);
        if (seat != null) {
            previous = seat.isSofa() ? SOFA : BOAT;
            player.stopRiding();
        }
        PlayerData data = HeroPowers.data(player);
        boolean changed = false;
        if (data.hasState(TRAMPOLINE_STATE)) {
            data.removeState(TRAMPOLINE_STATE);
            unroot(player);
            previous = TRAMPOLINE;
            changed = true;
        }
        if (data.hasState(BALL_STATE)) {
            data.removeState(BALL_STATE);
            previous = BALL;
            changed = true;
        }
        if (data.hasState(NECK_STATE)) {
            data.removeState(NECK_STATE);
            previous = NECK;
            changed = true;
        }
        if (data.hasState(MrFantasticPower.PARACHUTE_STATE)) {
            data.removeState(MrFantasticPower.PARACHUTE_STATE);
            previous = PARACHUTE;
            changed = true;
        }
        ANCHORS.remove(player.getUUID());
        BALL_UNTIL.remove(player.getUUID());
        if (changed) {
            PlayerData.sync(player);
        }
        return previous;
    }

    public static void clear(ServerPlayer player) {
        end(player);
        SLINGS.remove(player.getUUID());
        SELECTED.remove(player.getUUID());
    }

    private static boolean startSeat(ServerPlayer player, boolean sofa) {
        if (sofa ? !player.onGround() : !player.isInWater()) {
            player.displayClientMessage(Component.literal(sofa ? "§7Stand on the ground first." : "§7Get in the water first."), true);
            return false;
        }
        FantasticSeat seat = HeroEntities.FANTASTIC_SEAT.get().create(player.serverLevel());
        if (seat == null) {
            return false;
        }
        seat.setSofa(sofa);
        double y = player.getY();
        if (!sofa) {
            ServerLevel level = player.serverLevel();
            BlockPos pos = player.blockPosition();
            while (level.getFluidState(pos.above()).is(FluidTags.WATER) && pos.getY() < player.getBlockY() + 8) {
                pos = pos.above();
            }
            y = pos.getY() + level.getFluidState(pos).getHeight(level, pos);
        }
        seat.moveTo(player.getX(), y, player.getZ(), player.getYRot(), 0.0F);
        player.serverLevel().addFreshEntity(seat);
        if (!player.startRiding(seat, true)) {
            seat.discard();
            return false;
        }
        return true;
    }

    private static boolean startTrampoline(ServerPlayer player) {
        if (!player.onGround()) {
            player.displayClientMessage(Component.literal("§7Stand on the ground first."), true);
            return false;
        }
        HeroPowers.data(player).addState(TRAMPOLINE_STATE);
        PlayerData.sync(player);
        ANCHORS.put(player.getUUID(), player.position());
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(ROOTED)) {
            speed.addTransientModifier(new AttributeModifier(ROOTED, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        return true;
    }

    private static void unroot(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(ROOTED);
        }
    }

    private static boolean startSlingshot(ServerPlayer player) {
        SLINGS.put(player.getUUID(), 0);
        MrFantasticPower.send(player, MrFantasticPower.SLING, player.position(), 12);
        HeroFx.play(player, SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), 1.0F, 0.8F);
        return true;
    }

    private static boolean startBall(ServerPlayer player) {
        HeroPowers.data(player).addState(BALL_STATE);
        PlayerData.sync(player);
        BALL_UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + (long) (MarvelBalanceConfig.BALL_SECONDS.get() * 20.0D));
        Vec3 look = Vec3.directionFromRotation(0.0F, player.getYRot());
        player.setDeltaMovement(look.x * 0.7D, 0.7D, look.z * 0.7D);
        player.hurtMarked = true;
        return true;
    }

    private static boolean startNeck(ServerPlayer player) {
        HeroPowers.data(player).addState(NECK_STATE);
        PlayerData.sync(player);
        ANCHORS.put(player.getUUID(), player.position());
        return true;
    }

    public static void tick(ServerPlayer player) {
        tickSling(player);
        PlayerData data = HeroPowers.data(player);
        Vec3 anchor = ANCHORS.get(player.getUUID());
        if (data.hasState(TRAMPOLINE_STATE)) {
            if (anchor == null || player.isShiftKeyDown() || player.getY() > anchor.y + 0.6D || horizontal(player.position(), anchor) > 1.0D) {
                end(player);
            } else {
                tickTrampoline(player);
            }
        }
        if (data.hasState(NECK_STATE)) {
            if (anchor == null || player.isShiftKeyDown() || horizontal(player.position(), anchor) > 0.4D) {
                end(player);
            }
        }
        if (data.hasState(BALL_STATE)) {
            Long until = BALL_UNTIL.get(player.getUUID());
            if (until == null || player.serverLevel().getGameTime() >= until || player.isShiftKeyDown()) {
                end(player);
            } else {
                tickBall(player);
            }
        }
    }

    public static void tickClient(Player player) {
        if (!player.level().isClientSide || !player.isLocalPlayer()) {
            return;
        }
        if (!has(player, BALL_STATE)) {
            CLIENT_LAST_MOTION.remove(player.getUUID());
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        Vec3 last = CLIENT_LAST_MOTION.get(player.getUUID());
        double x = motion.x;
        double y = motion.y;
        double z = motion.z;
        if (last != null) {
            if (player.onGround() && last.y < -0.2D) {
                y = Math.min(1.4D, -last.y * 0.85D + 0.15D);
            }
            if (player.horizontalCollision) {
                double movedX = player.getX() - player.xo;
                double movedZ = player.getZ() - player.zo;
                if (Math.abs(movedX) < Math.abs(last.x) * 0.5D) {
                    x = -last.x * 0.9D;
                }
                if (Math.abs(movedZ) < Math.abs(last.z) * 0.5D) {
                    z = -last.z * 0.9D;
                }
            }
        }
        Vec3 push = Vec3.directionFromRotation(0.0F, player.getYRot()).scale(0.05D);
        x += push.x;
        z += push.z;
        double speed = Math.sqrt(x * x + z * z);
        if (speed > 0.9D) {
            x = x / speed * 0.9D;
            z = z / speed * 0.9D;
        }
        player.setDeltaMovement(x, y, z);
        player.fallDistance = 0.0F;
        CLIENT_LAST_MOTION.put(player.getUUID(), new Vec3(x, y, z));
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static boolean ready(Entity entity, long now, long gap) {
        Long last = LAST_HIT.get(entity.getId());
        if (last != null && now - last < gap) {
            return false;
        }
        LAST_HIT.put(entity.getId(), now);
        if (LAST_HIT.size() > 256) {
            LAST_HIT.values().removeIf(time -> now - time > 200L);
        }
        return true;
    }

    private static void tickTrampoline(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        AABB area = new AABB(player.getX() - 1.6D, player.getY() - 0.3D, player.getZ() - 1.6D,
                player.getX() + 1.6D, player.getY() + 1.4D, player.getZ() + 1.6D);
        for (Entity entity : level.getEntities(player, area, entity -> entity instanceof LivingEntity && entity.isAlive())) {
            boolean falling = entity.fallDistance > 0.4F || entity.getDeltaMovement().y < -0.25D;
            if (!falling || !ready(entity, now, 6L)) {
                continue;
            }
            double bounce = Math.min(MarvelBalanceConfig.TRAMPOLINE_MAX_BOUNCE.get(), 0.9D + entity.fallDistance * 0.07D);
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, bounce, motion.z);
            entity.fallDistance = 0.0F;
            entity.hurtMarked = true;
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SLIME_BALL)),
                    entity.getX(), player.getY() + 0.3D, entity.getZ(), 8, 0.4D, 0.1D, 0.4D, 0.1D);
            HeroFx.play(player, SoundEvents.SLIME_JUMP, 1.2F, 0.7F + player.getRandom().nextFloat() * 0.4F);
        }
    }

    private static void tickBall(ServerPlayer player) {
        double speed = Math.sqrt(Math.pow(player.getX() - player.xo, 2) + Math.pow(player.getZ() - player.zo, 2));
        if (speed < 0.2D) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        float damage = (float) (HeroPowers.powerDamage(player) * MarvelBalanceConfig.BALL_DAMAGE_MULTIPLIER.get());
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(0.4D),
                entity -> entity != player && entity.isAlive())) {
            if (!ready(target, now, 10L)) {
                continue;
            }
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
            Vec3 push = target.position().subtract(player.position()).normalize().scale(1.2D);
            target.setDeltaMovement(push.x, 0.45D, push.z);
            target.hurtMarked = true;
            HeroFx.play(target, SoundEvents.SLIME_ATTACK, 1.2F, 0.6F);
        }
    }

    private static void tickSling(ServerPlayer player) {
        Integer ticks = SLINGS.get(player.getUUID());
        if (ticks == null) {
            return;
        }
        Vec3 look = player.getLookAngle();
        if (ticks < 8) {
            SLINGS.put(player.getUUID(), ticks + 1);
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(-look.x * 0.12D, motion.y, -look.z * 0.12D);
            player.hurtMarked = true;
            return;
        }
        SLINGS.remove(player.getUUID());
        double speed = MarvelBalanceConfig.SLINGSHOT_SPEED.get();
        player.setDeltaMovement(look.x * speed, look.y * speed + 0.45D, look.z * speed);
        player.hurtMarked = true;
        HeroFx.play(player, SoundEvents.CROSSBOW_SHOOT, 1.2F, 0.7F);
        HeroFx.play(player, SoundEvents.SLIME_JUMP, 1.0F, 1.5F);
    }
}
