package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MindLinkManager {
    private static final double MAX_DISTANCE = 64.0D;
    private static final int ATTACK_INTERVAL = 10;
    private static final Map<UUID, Link> LINKS = new ConcurrentHashMap<>();

    private MindLinkManager() {
    }

    public static boolean isLinked(UUID controller) {
        return LINKS.containsKey(controller);
    }

    public static boolean isLinkedTarget(UUID target) {
        return LINKS.values().stream().anyMatch(link -> link.target().equals(target));
    }

    public static void activate(GauntletCast cast) {
        ServerPlayer player = cast.player();
        if (LINKS.containsKey(player.getUUID())) {
            end(player.server, player.getUUID(), "mind_link_end");
            return;
        }
        if (cast.onCooldown()) return;
        LivingEntity target = GauntletAim.living(player, 48.0D);
        if (target == null || GauntletAim.isProtectedEntity(target) || isLinkedTarget(target.getUUID())) {
            cast.message("mind_no_target");
            return;
        }
        int duration = cast.infinity() ? 800 : 400;
        boolean playerTarget = target instanceof ServerPlayer;
        if (playerTarget) {
            if (!MindControlManager.control(player, target, duration, true)) {
                cast.message("mind_no_target");
                return;
            }
        } else if (target instanceof Mob mob) {
            GauntletEntityLocks.remember(mob);
            mob.setTarget(null);
            mob.setNoAi(true);
        } else {
            cast.message("mind_no_target");
            return;
        }
        long now = player.server.overworld().getGameTime();
        LINKS.put(player.getUUID(), new Link(target.getUUID(), target.level().dimension(), now + duration, playerTarget,
                cast.infinity(), new boolean[1], new long[1]));
        MindControlManager.seedInput(player);
        PacketDistributor.sendToPlayer(player, new MindLinkPayload(target.getId(), true));
        GauntletFx.burst(cast.level(), target.getEyePosition(), "mind", 30, 0.4D);
        GauntletFx.cast(player, "mind", SoundEvents.EVOKER_PREPARE_WOLOLO, 0.6F);
        cast.message("mind_link", target.getDisplayName());
    }

    public static void tick(MinecraftServer server) {
        if (LINKS.isEmpty()) return;
        long now = server.overworld().getGameTime();
        for (Map.Entry<UUID, Link> entry : Map.copyOf(LINKS).entrySet()) {
            ServerPlayer controller = server.getPlayerList().getPlayer(entry.getKey());
            Link link = entry.getValue();
            ServerLevel level = server.getLevel(link.dimension());
            Entity entity = level == null ? null : level.getEntity(link.target());
            if (controller == null || !controller.isAlive() || now >= link.expiry()
                    || !(entity instanceof LivingEntity target) || !target.isAlive()
                    || controller.level() != target.level() || controller.distanceToSqr(target) > MAX_DISTANCE * MAX_DISTANCE
                    || !hasMindGauntlet(controller)) {
                end(server, entry.getKey(), now >= link.expiry() ? "mind_link_end" : "mind_link_lost");
                continue;
            }
            controller.setDeltaMovement(Vec3.ZERO);
            if (!link.playerTarget() && target instanceof Mob mob) drive(controller, mob, link, now);
            if (now % 5L == 0L) {
                level.sendParticles(GauntletFx.dust("mind", 0.8F), target.getX(), target.getY() + target.getBbHeight() + 0.3D,
                        target.getZ(), 2, 0.15D, 0.05D, 0.15D, 0.0D);
            }
        }
    }

    private static boolean hasMindGauntlet(ServerPlayer player) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        return !gauntlet.isEmpty() && InfinityGauntletStones.hasStone(gauntlet, "mind");
    }

    private static void drive(ServerPlayer controller, Mob mob, Link link, long now) {
        MindControlInputPayload input = MindControlManager.input(controller.getUUID());
        if (input == null) return;
        float yaw = input.yaw();
        float pitch = input.pitch();
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.yBodyRot = yaw;
        mob.setXRot(pitch);
        double speed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * (input.sprint() ? 1.4D : 1.0D)
                * (link.infinity() ? 1.5D : 1.0D);
        speed = Math.clamp(speed, 0.12D, 0.8D);
        double forward = (input.up() ? 1.0D : 0.0D) - (input.down() ? 1.0D : 0.0D);
        double strafe = (input.left() ? 1.0D : 0.0D) - (input.right() ? 1.0D : 0.0D);
        double radians = Math.toRadians(yaw);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        Vec3 horizontal = new Vec3(strafe * cos - forward * sin, 0.0D, forward * cos + strafe * sin);
        if (horizontal.lengthSqr() > 1.0E-4D) horizontal = horizontal.normalize().scale(speed);
        Vec3 current = mob.getDeltaMovement();
        double vertical = current.y;
        boolean flying = mob.isNoGravity() || mob instanceof FlyingMob || mob instanceof FlyingAnimal;
        if (flying) {
            vertical = -Math.sin(Math.toRadians(pitch)) * speed * forward + (input.jump() ? speed * 0.6D : 0.0D);
        } else if (input.jump() && (mob.onGround() || mob.isInWater() || mob.isInLava())) {
            vertical = mob.isInWater() || mob.isInLava() ? 0.12D : 0.42D + mob.getJumpBoostPower();
        } else if (mob.onGround()) {
            vertical = 0.0D;
        } else {
            vertical = (vertical - mob.getGravity()) * 0.98D;
        }
        mob.setDeltaMovement(horizontal.x, vertical, horizontal.z);
        mob.move(MoverType.SELF, mob.getDeltaMovement());
        if (mob.onGround() && mob.getDeltaMovement().y < 0.0D) {
            mob.setDeltaMovement(mob.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
        }
        boolean pressed = input.attack() && !link.attacking()[0];
        link.attacking()[0] = input.attack();
        if (input.attack() && (pressed || now - link.lastAttack()[0] >= ATTACK_INTERVAL)) {
            link.lastAttack()[0] = now;
            attack(controller, mob);
        }
    }

    private static void attack(ServerPlayer controller, Mob mob) {
        if (mob instanceof Creeper creeper) {
            creeper.ignite();
            return;
        }
        mob.swing(InteractionHand.MAIN_HAND, true);
        if (mob instanceof RangedAttackMob ranged) {
            LivingEntity aimed = lookedAt(controller, mob, 32.0D);
            if (aimed != null) ranged.performRangedAttack(aimed, 1.0F);
            return;
        }
        LivingEntity victim = lookedAt(controller, mob, 3.0D + mob.getBbWidth());
        if (victim == null) return;
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            mob.doHurtTarget(victim);
        } else {
            victim.hurt(mob.damageSources().mobAttack(mob), 2.0F);
        }
    }

    private static LivingEntity lookedAt(ServerPlayer controller, Mob mob, double range) {
        Vec3 eye = mob.getEyePosition();
        Vec3 end = eye.add(mob.getViewVector(1.0F).scale(range));
        AABB search = mob.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(mob, eye, end, search,
                entity -> entity instanceof LivingEntity && entity != mob && entity != controller && entity.isPickable(),
                range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    public static void onControllerHurt(ServerPlayer player) {
        if (LINKS.containsKey(player.getUUID())) end(player.server, player.getUUID(), "mind_link_lost");
    }

    public static void end(MinecraftServer server, UUID controllerId, String messageKey) {
        Link link = LINKS.remove(controllerId);
        if (link == null) return;
        ServerLevel level = server.getLevel(link.dimension());
        Entity entity = level == null ? null : level.getEntity(link.target());
        if (link.playerTarget()) {
            MindControlManager.release(link.target());
        } else if (entity instanceof Mob mob) {
            mob.setDeltaMovement(Vec3.ZERO);
            net.minecraft.nbt.CompoundTag lock = mob.getPersistentData().getCompound("integrated_superheroes_lock");
            mob.setNoAi(lock.getBoolean("NoAi"));
            GauntletEntityLocks.forget(mob);
        }
        ServerPlayer controller = server.getPlayerList().getPlayer(controllerId);
        if (controller == null) return;
        PacketDistributor.sendToPlayer(controller, new MindLinkPayload(-1, false));
        if (level != null && entity != null) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getEyeY(), entity.getZ(), 20, 0.2D, 0.2D, 0.2D, 0.05D);
        }
        controller.displayClientMessage(Component.translatable("message.integrated_superheroes.gauntlet." + messageKey), true);
        GauntletAbility ability = GauntletAbilities.get("mind", 3);
        if (ability != null) {
            GauntletAbilityController.addAbilityCooldown(controller, "mind", 3, ability.cooldownTicks(link.infinity()));
        }
    }

    public static void clear(MinecraftServer server) {
        for (UUID controller : Map.copyOf(LINKS).keySet()) end(server, controller, "mind_link_end");
        LINKS.clear();
    }

    private record Link(UUID target, ResourceKey<Level> dimension, long expiry, boolean playerTarget, boolean infinity,
                        boolean[] attacking, long[] lastAttack) {
    }
}
