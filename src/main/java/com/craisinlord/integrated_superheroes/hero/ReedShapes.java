package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.registry.HeroEntities;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
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
    public static final String BRIDGE = "bridge";
    public static final String SLINGSHOT = "slingshot";
    public static final String BALL = "bouncy_ball";
    public static final String NECK = "periscope_neck";
    public static final String POGO = "pogo_stick";
    public static final String STILTS = "stilts";
    public static final String BOAT_STATE = "integrated_superheroes_boat";
    public static final String SOFA_STATE = "integrated_superheroes_sofa";
    public static final String BRIDGE_STATE = "integrated_superheroes_bridge";
    public static final String BALL_STATE = "integrated_superheroes_ball";
    public static final String NECK_STATE = "integrated_superheroes_neck";
    public static final String POGO_STATE = "integrated_superheroes_pogo";
    public static final String STILTS_STATE = "integrated_superheroes_stilts";
    private static final ResourceLocation ROOTED = ResourceLocation.fromNamespaceAndPath("integrated_superheroes", "reed_rooted");
    private static final ResourceLocation STILT_STEP = ResourceLocation.fromNamespaceAndPath("integrated_superheroes", "reed_stilt_step");
    private static final ResourceLocation STILT_STRIDE = ResourceLocation.fromNamespaceAndPath("integrated_superheroes", "reed_stilt_stride");
    private static final List<HeroWheels.Option> OPTIONS = List.of(
            new HeroWheels.Option(BOAT, "Boat", () -> Items.OAK_BOAT),
            new HeroWheels.Option(SOFA, "Sofa", () -> Items.RED_WOOL),
            new HeroWheels.Option(BRIDGE, "Bridge", () -> Items.OAK_SLAB),
            new HeroWheels.Option(SLINGSHOT, "Slingshot", () -> Items.STRING),
            new HeroWheels.Option(BALL, "Bouncy Ball", () -> Items.SLIME_BALL),
            new HeroWheels.Option(NECK, "Periscope Neck", () -> Items.SPYGLASS),
            new HeroWheels.Option(PARACHUTE, "Parachute", () -> Items.FEATHER),
            new HeroWheels.Option(POGO, "Pogo Stick", () -> Items.PISTON),
            new HeroWheels.Option(STILTS, "Stilts", () -> Items.BAMBOO)
    );
    private static final Map<UUID, Vec3> ANCHORS = new HashMap<>();
    private static final Map<UUID, String> SELECTED = new HashMap<>();
    private static final Map<UUID, Long> BALL_UNTIL = new HashMap<>();
    private static final Map<UUID, Integer> SLINGS = new HashMap<>();
    private static final Map<UUID, FantasticSeat> SEATS = new HashMap<>();
    private static final Map<UUID, Bridge> BRIDGES = new HashMap<>();
    private static final Map<Integer, Long> LAST_HIT = new HashMap<>();
    private static final Map<UUID, Vec3> CLIENT_LAST_MOTION = new HashMap<>();

    private ReedShapes() {
    }

    private record Bridge(ServerLevel level, List<BlockPos> blocks, Vec3 end, long until) {
    }

    public static HeroWheels.Wheel wheel() {
        return new HeroWheels.Wheel(ABILITY, OPTIONS, (player, option) -> true, ReedShapes::select);
    }

    public static boolean has(Player player, String state) {
        return MrFantasticPower.isReed(player) && HeroPowers.data(player).hasState(state);
    }

    public static FantasticSeat seat(Player player) {
        FantasticSeat seat = SEATS.get(player.getUUID());
        return seat != null && !seat.isRemoved() ? seat : null;
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
            case BOAT -> startBoat(player);
            case SOFA -> startSofa(player);
            case BRIDGE -> startBridge(player);
            case SLINGSHOT -> startSlingshot(player);
            case BALL -> startBall(player);
            case NECK -> startNeck(player);
            case PARACHUTE -> startParachute(player);
            case POGO -> startPogo(player);
            case STILTS -> startStilts(player);
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

    public static String end(ServerPlayer player) {
        String previous = null;
        FantasticSeat seat = SEATS.remove(player.getUUID());
        if (seat != null) {
            seat.ejectPassengers();
            seat.discard();
        }
        PlayerData data = HeroPowers.data(player);
        boolean changed = false;
        if (data.hasState(SOFA_STATE)) {
            data.removeState(SOFA_STATE);
            removeModifier(player, Attributes.MOVEMENT_SPEED, ROOTED);
            previous = SOFA;
            changed = true;
        }
        if (data.hasState(BOAT_STATE)) {
            data.removeState(BOAT_STATE);
            previous = BOAT;
            changed = true;
        }
        if (data.hasState(BRIDGE_STATE)) {
            data.removeState(BRIDGE_STATE);
            removeModifier(player, Attributes.MOVEMENT_SPEED, ROOTED);
            clearBridge(player);
            previous = BRIDGE;
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
        if (data.hasState(POGO_STATE)) {
            data.removeState(POGO_STATE);
            previous = POGO;
            changed = true;
        }
        if (data.hasState(STILTS_STATE)) {
            data.removeState(STILTS_STATE);
            removeModifier(player, Attributes.STEP_HEIGHT, STILT_STEP);
            removeModifier(player, Attributes.MOVEMENT_SPEED, STILT_STRIDE);
            previous = STILTS;
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

    private static void addModifier(ServerPlayer player, Holder<Attribute> attribute,
                                    ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && !instance.hasModifier(id)) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void removeModifier(ServerPlayer player, Holder<Attribute> attribute,
                                       ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private static void root(ServerPlayer player) {
        addModifier(player, Attributes.MOVEMENT_SPEED, ROOTED, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void spawnSeat(ServerPlayer player, boolean sofa) {
        FantasticSeat seat = HeroEntities.FANTASTIC_SEAT.get().create(player.serverLevel());
        if (seat == null) {
            return;
        }
        seat.setSofa(sofa);
        seat.setOwner(player);
        seat.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        player.serverLevel().addFreshEntity(seat);
        SEATS.put(player.getUUID(), seat);
    }

    private static boolean startSofa(ServerPlayer player) {
        if (!player.onGround()) {
            player.displayClientMessage(Component.literal("§7Stand on the ground first."), true);
            return false;
        }
        HeroPowers.data(player).addState(SOFA_STATE);
        PlayerData.sync(player);
        ANCHORS.put(player.getUUID(), player.position());
        root(player);
        spawnSeat(player, true);
        return true;
    }

    private static boolean startBoat(ServerPlayer player) {
        if (!player.isInWater()) {
            player.displayClientMessage(Component.literal("§7Get in the water first."), true);
            return false;
        }
        HeroPowers.data(player).addState(BOAT_STATE);
        PlayerData.sync(player);
        spawnSeat(player, false);
        return true;
    }

    private static boolean startBridge(ServerPlayer player) {
        if (!player.onGround()) {
            player.displayClientMessage(Component.literal("§7Stand on the ground first."), true);
            return false;
        }
        ServerLevel level = player.serverLevel();
        Direction direction = player.getDirection();
        BlockPos feet = player.blockPosition();
        int length = 0;
        for (int i = 1; i <= MarvelBalanceConfig.BRIDGE_LENGTH.get(); i++) {
            BlockPos column = feet.relative(direction, i);
            if (!level.getBlockState(column).getCollisionShape(level, column).isEmpty()
                    || !level.getBlockState(column.above()).getCollisionShape(level, column.above()).isEmpty()) {
                break;
            }
            length = i;
        }
        if (length < 2) {
            player.displayClientMessage(Component.literal("§7No room to stretch out."), true);
            return false;
        }
        int ticks = (int) (MarvelBalanceConfig.BRIDGE_SECONDS.get() * 20.0D);
        List<BlockPos> blocks = new ArrayList<>();
        for (int i = 1; i <= length; i++) {
            BlockPos below = feet.relative(direction, i).below();
            if (TempBlocks.placeOver(level, player, below, Blocks.BARRIER.defaultBlockState(), ticks + 40)) {
                blocks.add(below.immutable());
            }
        }
        Vec3 start = Vec3.atBottomCenterOf(feet);
        player.connection.teleport(start.x, player.getY(), start.z, direction.toYRot(), player.getXRot());
        Vec3 end = start.add(direction.getStepX() * (length + 0.5D), 0.0D, direction.getStepZ() * (length + 0.5D));
        HeroPowers.data(player).addState(BRIDGE_STATE);
        PlayerData.sync(player);
        ANCHORS.put(player.getUUID(), new Vec3(start.x, player.getY(), start.z));
        root(player);
        BRIDGES.put(player.getUUID(), new Bridge(level, blocks, end, level.getGameTime() + ticks));
        MrFantasticPower.send(player, MrFantasticPower.BRIDGE, end, ticks);
        return true;
    }

    private static void clearBridge(ServerPlayer player) {
        Bridge bridge = BRIDGES.remove(player.getUUID());
        if (bridge != null) {
            bridge.blocks().forEach(pos -> TempBlocks.release(bridge.level(), pos));
        }
        MrFantasticPower.send(player, MrFantasticPower.BRIDGE, player.position(), 0);
    }

    private static boolean startStilts(ServerPlayer player) {
        if (player.isInWater()) {
            player.displayClientMessage(Component.literal("§7Get out of the water first."), true);
            return false;
        }
        HeroPowers.data(player).addState(STILTS_STATE);
        PlayerData.sync(player);
        addModifier(player, Attributes.STEP_HEIGHT, STILT_STEP, MarvelBalanceConfig.STILTS_STEP_HEIGHT.get(), AttributeModifier.Operation.ADD_VALUE);
        addModifier(player, Attributes.MOVEMENT_SPEED, STILT_STRIDE, MarvelBalanceConfig.STILTS_SPEED_BONUS.get(),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        return true;
    }

    private static boolean startParachute(ServerPlayer player) {
        HeroPowers.data(player).addState(MrFantasticPower.PARACHUTE_STATE);
        PlayerData.sync(player);
        return true;
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

    private static boolean startPogo(ServerPlayer player) {
        if (!player.onGround()) {
            player.displayClientMessage(Component.literal("§7Stand on the ground first."), true);
            return false;
        }
        HeroPowers.data(player).addState(POGO_STATE);
        PlayerData.sync(player);
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, MarvelBalanceConfig.POGO_BOUNCE.get(), motion.z);
        player.hurtMarked = true;
        return true;
    }

    public static void jump(Player player) {
        if (!player.level().isClientSide || !player.isLocalPlayer() || !has(player, POGO_STATE)) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, MarvelBalanceConfig.POGO_BOOST_BOUNCE.get(), motion.z);
    }

    public static void tick(ServerPlayer player) {
        tickSling(player);
        PlayerData data = HeroPowers.data(player);
        Vec3 anchor = ANCHORS.get(player.getUUID());
        if (data.hasState(SOFA_STATE) && (anchor == null || player.isShiftKeyDown() || player.getY() > anchor.y + 0.6D
                || horizontal(player.position(), anchor) > 1.0D)) {
            end(player);
        }
        if (data.hasState(BOAT_STATE) && (player.isShiftKeyDown() || !player.isInWater() && player.onGround())) {
            end(player);
        }
        if (data.hasState(BRIDGE_STATE)) {
            Bridge bridge = BRIDGES.get(player.getUUID());
            if (bridge == null || anchor == null || player.isShiftKeyDown() || horizontal(player.position(), anchor) > 0.6D
                    || player.serverLevel().getGameTime() >= bridge.until()) {
                end(player);
            } else {
                tickBridge(player, anchor, bridge);
            }
        }
        if (data.hasState(NECK_STATE) && (anchor == null || player.isShiftKeyDown() || horizontal(player.position(), anchor) > 0.4D)) {
            end(player);
        }
        if (data.hasState(POGO_STATE)) {
            if (player.isShiftKeyDown() || player.isInWater() || player.isPassenger() || player.getAbilities().flying) {
                end(player);
            } else {
                tickPogo(player);
            }
        }
        if (data.hasState(STILTS_STATE) && (player.isShiftKeyDown() || player.isInWater() || player.isPassenger() || player.getAbilities().flying)) {
            end(player);
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
        if (has(player, POGO_STATE)) {
            tickPogoClient(player);
        }
        if (has(player, BOAT_STATE)) {
            tickBoatClient(player);
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

    private static void tickPogoClient(Player player) {
        Vec3 motion = player.getDeltaMovement();
        double y = player.onGround() && motion.y <= 0.0D ? MarvelBalanceConfig.POGO_BOUNCE.get() : motion.y;
        double x = motion.x;
        double z = motion.z;
        if (player.zza > 0.0F) {
            Vec3 push = Vec3.directionFromRotation(0.0F, player.getYRot()).scale(0.04D);
            x += push.x;
            z += push.z;
        }
        double speed = Math.sqrt(x * x + z * z);
        if (speed > 0.6D) {
            x = x / speed * 0.6D;
            z = z / speed * 0.6D;
        }
        player.setDeltaMovement(x, y, z);
        player.fallDistance = 0.0F;
    }

    private static void tickBoatClient(Player player) {
        if (!player.isInWater()) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        double y = Mth.clamp((player.getFluidHeight(FluidTags.WATER) - 0.35D) * 0.4D, -0.08D, 0.1D);
        double max = MarvelBalanceConfig.BOAT_SPEED.get();
        double thrust = player.zza > 0.0F ? max * 0.2D : player.zza < 0.0F ? -max * 0.1D : 0.0D;
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getYRot());
        double x = motion.x + forward.x * thrust;
        double z = motion.z + forward.z * thrust;
        double speed = Math.sqrt(x * x + z * z);
        if (speed > max) {
            x = x / speed * max;
            z = z / speed * max;
        }
        player.setDeltaMovement(x, y, z);
        player.fallDistance = 0.0F;
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

    private static void tickBridge(ServerPlayer player, Vec3 anchor, Bridge bridge) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        AABB path = new AABB(anchor, bridge.end()).inflate(0.5D, 0.0D, 0.5D).expandTowards(0.0D, 1.0D, 0.0D);
        for (LivingEntity walker : level.getEntitiesOfClass(LivingEntity.class, path,
                entity -> entity != player && entity.isAlive() && entity.onGround())) {
            if (!ready(walker, now, 20L)) {
                continue;
            }
            HeroFx.play(walker, SoundEvents.SLIME_SQUISH_SMALL, 0.8F, 0.6F);
            HeroFx.play(player, SoundEvents.PLAYER_HURT, 0.5F, 0.8F + player.getRandom().nextFloat() * 0.3F);
        }
    }

    private static void tickPogo(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (player.onGround() && ready(player, now, 6L)) {
            HeroFx.play(player, SoundEvents.SLIME_JUMP_SMALL, 0.7F, 0.9F + player.getRandom().nextFloat() * 0.3F);
        }
        if (player.getY() - player.yOld >= -0.05D) {
            return;
        }
        float damage = MarvelBalanceConfig.POGO_STOMP_DAMAGE.get().floatValue();
        AABB feet = new AABB(player.getX() - 0.5D, player.getY() - 0.8D, player.getZ() - 0.5D,
                player.getX() + 0.5D, player.getY() + 0.2D, player.getZ() + 0.5D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, feet,
                entity -> entity != player && entity.isAlive() && entity.getBoundingBox().maxY <= player.getY() + 0.4D)) {
            if (!ready(target, now, 10L)) {
                continue;
            }
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, MarvelBalanceConfig.POGO_BOOST_BOUNCE.get(), motion.z);
            player.hurtMarked = true;
            HeroFx.play(target, SoundEvents.SLIME_ATTACK, 1.2F, 0.6F);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SLIME_BALL)),
                    target.getX(), target.getBoundingBox().maxY, target.getZ(), 10, 0.3D, 0.1D, 0.3D, 0.1D);
            return;
        }
    }

    private static void tickBall(ServerPlayer player) {
        double speed = Math.sqrt(Math.pow(player.getX() - player.xo, 2) + Math.pow(player.getZ() - player.zo, 2));
        if (speed < 0.2D) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        float damage = MarvelBalanceConfig.BALL_DAMAGE.get().floatValue();
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
