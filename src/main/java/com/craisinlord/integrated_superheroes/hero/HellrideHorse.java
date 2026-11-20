package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class HellrideHorse {
    public static final String ABILITY = "ghost_rider_hellride";
    public static final String TAG = "integrated_superheroes_hellride";
    private static final Map<UUID, Ride> RIDES = new HashMap<>();

    private HellrideHorse() {
    }

    private record Ride(SkeletonHorse horse, ResourceKey<Level> dimension) {
    }

    public static boolean isHellride(Entity entity) {
        return entity instanceof SkeletonHorse && entity.getTags().contains(TAG);
    }

    public static SkeletonHorse riding(ServerPlayer player) {
        Ride ride = RIDES.get(player.getUUID());
        return ride != null && player.getVehicle() == ride.horse() ? ride.horse() : null;
    }

    public static double press(ServerPlayer player) {
        if (RIDES.containsKey(player.getUUID())) {
            lose(player.server, player.getUUID(), false);
            return 0.0D;
        }
        if (player.isPassenger() || player.isSpectator() || player.getAbilities().flying || player.isFallFlying()) {
            player.displayClientMessage(Component.literal("§7Get your feet on the ground first."), true);
            return 0.0D;
        }
        ServerLevel level = player.serverLevel();
        SkeletonHorse horse = EntityType.SKELETON_HORSE.create(level);
        if (horse == null) {
            return 0.0D;
        }
        horse.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        if (!level.noCollision(horse)) {
            player.displayClientMessage(Component.literal("§7Not enough room for the horse here."), true);
            return 0.0D;
        }
        horse.addTag(TAG);
        horse.setTamed(true);
        horse.setOwnerUUID(player.getUUID());
        horse.equipSaddle(new ItemStack(Items.SADDLE), null);
        horse.setPersistenceRequired();
        base(horse.getAttribute(Attributes.MAX_HEALTH), GhostRiderConfig.HORSE_HEALTH.get());
        base(horse.getAttribute(Attributes.MOVEMENT_SPEED), GhostRiderConfig.HORSE_SPEED.get());
        base(horse.getAttribute(Attributes.JUMP_STRENGTH), GhostRiderConfig.HORSE_JUMP.get());
        horse.setHealth(horse.getMaxHealth());
        horse.setPortalCooldown(Integer.MAX_VALUE / 2);
        RIDES.put(player.getUUID(), new Ride(horse, level.dimension()));
        if (!level.addFreshEntity(horse)) {
            RIDES.remove(player.getUUID());
            return 0.0D;
        }
        player.startRiding(horse, true);
        Vec3 at = horse.position();
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 0.8D, at.z, 50, 0.6D, 0.6D, 0.6D, 0.05D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.4D, at.z, 20, 0.6D, 0.3D, 0.6D, 0.02D);
        HeroFx.play(horse, SoundEvents.SKELETON_HORSE_AMBIENT, 1.2F, 0.6F);
        HeroFx.play(horse, SoundEvents.FIRECHARGE_USE, 1.0F, 0.5F);
        HeroFx.play(horse, SoundEvents.SOUL_ESCAPE.value(), 1.0F, 0.8F);
        return 0.0D;
    }

    private static void base(AttributeInstance attribute, double value) {
        if (attribute != null) {
            attribute.setBaseValue(value);
        }
    }

    public static void tick(MinecraftServer server) {
        if (RIDES.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Ride>> iterator = RIDES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Ride> entry = iterator.next();
            Ride ride = entry.getValue();
            SkeletonHorse horse = ride.horse();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            boolean valid = owner != null && owner.isAlive() && !horse.isRemoved() && horse.isAlive()
                    && horse.level().dimension() == ride.dimension() && owner.getVehicle() == horse
                    && SerumKit.is(owner, GhostRiderPower.ID);
            if (!valid) {
                iterator.remove();
                vanish(horse);
                if (owner != null) {
                    HeroAbilities.setCooldown(owner, ABILITY, GhostRiderConfig.HORSE_DISMISS_COOLDOWN.get());
                }
                continue;
            }
            horse.setPortalCooldown(Integer.MAX_VALUE / 2);
            horse.clearFire();
            horse.fallDistance = 0.0F;
            owner.fallDistance = 0.0F;
            if (owner.containerMenu instanceof HorseInventoryMenu) {
                owner.closeContainer();
            }
            if (horse.tickCount % 2 == 0 && horse.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, horse.getX(), horse.getY() + horse.getBbHeight() * 0.6D, horse.getZ(), 2,
                        horse.getBbWidth() * 0.35D, horse.getBbHeight() * 0.35D, horse.getBbWidth() * 0.35D, 0.01D);
                if (horse.tickCount % 6 == 0) {
                    Vec3 head = horse.position().add(horse.getLookAngle().scale(0.9D)).add(0.0D, horse.getBbHeight() + 0.1D, 0.0D);
                    level.sendParticles(ParticleTypes.FLAME, head.x, head.y, head.z, 2, 0.12D, 0.12D, 0.12D, 0.01D);
                }
            }
        }
    }

    private static void vanish(SkeletonHorse horse) {
        if (horse.level() instanceof ServerLevel level && !horse.isRemoved()) {
            Vec3 at = horse.position();
            level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.8D, at.z, 20, 0.5D, 0.5D, 0.5D, 0.02D);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.6D, at.z, 16, 0.5D, 0.4D, 0.5D, 0.02D);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SKELETON_HORSE_DEATH, net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, 0.7F);
        }
        horse.ejectPassengers();
        horse.discard();
    }

    public static void lose(MinecraftServer server, UUID owner, boolean destroyed) {
        Ride ride = RIDES.remove(owner);
        if (ride == null) {
            return;
        }
        vanish(ride.horse());
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player != null) {
            HeroAbilities.setCooldown(player, ABILITY, destroyed
                    ? GhostRiderConfig.HORSE_LOST_COOLDOWN.get() : GhostRiderConfig.HORSE_DISMISS_COOLDOWN.get());
        }
    }

    public static void clear(ServerPlayer player) {
        Ride ride = RIDES.remove(player.getUUID());
        if (ride != null) {
            vanish(ride.horse());
        }
    }

    public static void clearAll() {
        RIDES.values().forEach(ride -> ride.horse().discard());
        RIDES.clear();
    }

    private static UUID ownerOf(Entity horse) {
        for (Map.Entry<UUID, Ride> entry : RIDES.entrySet()) {
            if (entry.getValue().horse() == horse) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && isHellride(event.getEntity()) && ownerOf(event.getEntity()) == null) {
            event.setCanceled(true);
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!isHellride(event.getEntity())) {
            return;
        }
        SkeletonHorse horse = (SkeletonHorse) event.getEntity();
        UUID owner = ownerOf(horse);
        if (owner == null) {
            event.setCanceled(true);
            horse.discard();
            return;
        }
        if (event.getSource().is(DamageTypeTags.IS_FIRE) || event.getSource().is(DamageTypeTags.IS_FALL)
                || event.getSource().getEntity() != null && owner.equals(event.getSource().getEntity().getUUID())) {
            event.setCanceled(true);
            horse.clearFire();
            return;
        }
        if (event.getAmount() >= horse.getHealth() && horse.level() instanceof ServerLevel level) {
            event.setCanceled(true);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            if (player != null) {
                player.displayClientMessage(Component.literal("§7Your horse burned out."), true);
            }
            lose(level.getServer(), owner, true);
        }
    }

    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (isHellride(event.getTarget())) {
            event.setCanceled(true);
        }
    }

    public static void onInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isHellride(event.getTarget())) {
            event.setCanceled(true);
        }
    }

    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getContainer() instanceof HorseInventoryMenu && event.getEntity() instanceof ServerPlayer player
                && isHellride(player.getVehicle())) {
            player.closeContainer();
        }
    }

    public static void onDrops(LivingDropsEvent event) {
        if (isHellride(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void onExperience(LivingExperienceDropEvent event) {
        if (isHellride(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
