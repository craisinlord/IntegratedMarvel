package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

public final class SurfaceCling {
    public static final HeroPower.Toggle TOGGLE = new HeroPower.Toggle("integrated_superheroes_off_wall_cling", "Wall-Cling", () -> Items.LADDER);
    private static final double REACH = 0.15D;
    private static final double PULL = 0.05D;
    private static final int RELEASE_TICKS = 10;
    private static final int KICK_REPEAT_TICKS = 40;
    private static final double KICK_REPEAT_DISTANCE = 1.5D;
    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();
    private static final Map<UUID, Hold> HOLDS = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> LAST_CLING = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> RELEASED = ServerState.map(new HashMap<>());
    private static final Map<UUID, Boolean> LOCAL_HOLD = ClientState.map(new HashMap<>());
    private static final Map<UUID, Kick> LAST_KICK = ClientState.map(new HashMap<>());
    private static boolean listening;

    public record Style(DoubleSupplier lateral, double slide, String animation, String grabSound, boolean wallKick) {
    }

    private record Source(Predicate<Player> eligible, Style style) {
    }

    private record Hold(long since, String animation) {
    }

    private record Kick(Direction face, double x, double z, long tick) {
    }

    private SurfaceCling() {
    }

    public static void register(Predicate<Player> eligible, Style style) {
        SOURCES.add(new Source(eligible, style));
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener(SurfaceCling::onTick);
            NeoForge.EVENT_BUS.addListener(SurfaceCling::onLogout);
            NeoForge.EVENT_BUS.addListener(SurfaceCling::onChangeDimension);
            NeoForge.EVENT_BUS.addListener(SurfaceCling::onStopping);
        }
    }

    public static Style style(Player player) {
        for (Source source : SOURCES) {
            if (source.eligible().test(player)) {
                return source.style();
            }
        }
        return null;
    }

    public static boolean clinging(Player player) {
        if (player.level().isClientSide) {
            return Boolean.TRUE.equals(LOCAL_HOLD.get(player.getUUID()));
        }
        return HOLDS.containsKey(player.getUUID());
    }

    public static boolean clungWithin(ServerPlayer player, int ticks) {
        Long last = LAST_CLING.get(player.getUUID());
        return last != null && player.level().getGameTime() - last <= ticks;
    }

    public static Direction wallFace(Player player) {
        Level level = player.level();
        AABB body = player.getBoundingBox().deflate(0.0D, 0.3D, 0.0D);
        if (hits(level, player, body)) {
            return null;
        }
        Vec3 look = player.getLookAngle();
        List<Direction> order = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        order.sort(Comparator.comparingDouble(direction -> -(direction.getStepX() * look.x + direction.getStepZ() * look.z)));
        for (Direction direction : order) {
            if (hits(level, player, body.move(direction.getStepX() * REACH, 0.0D, direction.getStepZ() * REACH))) {
                return direction;
            }
        }
        return null;
    }

    private static boolean hits(Level level, Player player, AABB box) {
        return level.getBlockCollisions(player, box).iterator().hasNext();
    }

    public static boolean canCling(Player player) {
        return !player.onGround() && player.isShiftKeyDown() && !player.isInWater() && !player.isInLava() && !player.isPassenger()
                && !player.isFallFlying() && !player.getAbilities().flying && !player.isSpectator() && !player.onClimbable()
                && !player.isSleeping() && !player.isSwimming();
    }

    public static void release(ServerPlayer player) {
        HOLDS.remove(player.getUUID());
        RELEASED.put(player.getUUID(), player.level().getGameTime() + RELEASE_TICKS);
    }

    public static void clear(ServerPlayer player) {
        Hold hold = HOLDS.remove(player.getUUID());
        if (hold != null && hold.animation() != null) {
            HeroAnimations.stop(player);
        }
        RELEASED.remove(player.getUUID());
        LAST_CLING.remove(player.getUUID());
    }

    private static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            if (player.isLocalPlayer()) {
                tickClient(player, style(player));
            }
        } else if (player instanceof ServerPlayer serverPlayer) {
            tickServer(serverPlayer, style(serverPlayer));
        }
    }

    private static void tickClient(Player player, Style style) {
        UUID id = player.getUUID();
        boolean holding = Boolean.TRUE.equals(LOCAL_HOLD.get(id));
        Direction face = style != null && canCling(player) ? wallFace(player) : null;
        if (face == null) {
            if (holding) {
                LOCAL_HOLD.remove(id);
            }
            return;
        }
        int up = HeroGravity.up(player);
        Vec3 motion = player.getDeltaMovement();
        double rising = motion.y * up;
        double sideways = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (rising > 0.1D || !holding && sideways > 0.5D) {
            LOCAL_HOLD.remove(id);
            return;
        }
        LOCAL_HOLD.put(id, true);
        if (style.wallKick() && SurfaceClingClient.jumping(player) && kickAllowed(player, face)) {
            kick(player, face, up);
            LOCAL_HOLD.remove(id);
            return;
        }
        double lateral = style.lateral().getAsDouble();
        player.setDeltaMovement(motion.x * lateral + face.getStepX() * PULL, -style.slide() * up, motion.z * lateral + face.getStepZ() * PULL);
        player.fallDistance = 0.0F;
    }

    private static boolean kickAllowed(Player player, Direction face) {
        Kick last = LAST_KICK.get(player.getUUID());
        if (last == null || player.level().getGameTime() - last.tick() > KICK_REPEAT_TICKS || last.face() != face) {
            return true;
        }
        double dx = player.getX() - last.x();
        double dz = player.getZ() - last.z();
        return dx * dx + dz * dz > KICK_REPEAT_DISTANCE * KICK_REPEAT_DISTANCE;
    }

    private static void kick(Player player, Direction face, int up) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize();
        Vec3 away = new Vec3(-face.getStepX(), 0.0D, -face.getStepZ());
        Vec3 push = away.scale(0.55D).add(flat.scale(0.35D));
        player.setDeltaMovement(push.x, 0.6D * up, push.z);
        player.fallDistance = 0.0F;
        LAST_KICK.put(player.getUUID(), new Kick(face, player.getX(), player.getZ(), player.level().getGameTime()));
        SoundEvent sound = HeroFx.sound("thematic:spiderman_wall_whack_1", SoundEvents.PLAYER_ATTACK_KNOCKBACK);
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 0.8F, 1.0F, false);
        HeroAnimations.requestFromClient(player, "flip_both_2");
    }

    private static void tickServer(ServerPlayer player, Style style) {
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        Long releasedUntil = RELEASED.get(id);
        boolean released = releasedUntil != null && now < releasedUntil;
        if (releasedUntil != null && !released) {
            RELEASED.remove(id);
        }
        boolean on = style != null && !released && canCling(player) && !VillainWorld.suppressed(player) && !HealingFactor.isDowned(player)
                && wallFace(player) != null;
        Hold hold = HOLDS.get(id);
        if (on) {
            player.fallDistance = 0.0F;
            LAST_CLING.put(id, now);
            if (hold == null) {
                HOLDS.put(id, new Hold(now, style.animation()));
                if (style.animation() != null) {
                    HeroAnimations.play(player, style.animation());
                }
                if (style.grabSound() != null) {
                    HeroFx.play(player, HeroFx.sound(style.grabSound(), SoundEvents.WOOL_STEP), 0.6F, 1.0F);
                }
            }
        } else if (hold != null) {
            HOLDS.remove(id);
            if (hold.animation() != null) {
                HeroAnimations.stop(player);
            }
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
        HOLDS.clear();
        LAST_CLING.clear();
        RELEASED.clear();
    }
}
