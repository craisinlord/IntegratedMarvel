package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.hero.skin.SerumCostumes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class GhostRiderPower {
    public static final String ID = "spirit_of_vengeance_johnny_blaze";
    public static final String STARE = "ghost_rider_penance_stare";
    public static final HeroPower.Toggle FLAMES = new HeroPower.Toggle("integrated_superheroes_off_ghost_rider_flames", "Flaming Skull",
            () -> Items.SOUL_TORCH);
    public static final HeroPower.Toggle TRAIL = new HeroPower.Toggle("integrated_superheroes_off_ghost_rider_trail", "Hellfire Trail",
            () -> Items.BLAZE_POWDER);
    private static final Map<UUID, Stare> STARES = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_TRAIL = new HashMap<>();
    private static boolean registered;

    private GhostRiderPower() {
    }

    private static final class Stare {
        private final UUID target;
        private final ResourceKey<Level> dimension;
        private int ticks;

        private Stare(UUID target, ResourceKey<Level> dimension) {
            this.target = target;
            this.dimension = dimension;
        }
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(GhostRiderJudgment::onDamage);
            NeoForge.EVENT_BUS.addListener(HellrideHorse::onJoin);
            NeoForge.EVENT_BUS.addListener(HellrideHorse::onIncomingDamage);
            NeoForge.EVENT_BUS.addListener(HellrideHorse::onInteract);
            NeoForge.EVENT_BUS.addListener(HellrideHorse::onInteractSpecific);
            NeoForge.EVENT_BUS.addListener(HellrideHorse::onContainerOpen);
            NeoForge.EVENT_BUS.addListener(HellrideHorse::onDrops);
            NeoForge.EVENT_BUS.addListener(HellrideHorse::onExperience);
            NeoForge.EVENT_BUS.addListener(GhostRiderPower::onServerTick);
            NeoForge.EVENT_BUS.addListener(GhostRiderPower::onLogout);
            NeoForge.EVENT_BUS.addListener(GhostRiderPower::onChangeDimension);
            NeoForge.EVENT_BUS.addListener(GhostRiderPower::onServerStopping);
            SerumCostumes.register(ID, "thematic:ghost_rider");
            RoguePower.adaptBorrowed(HellchainController.ABILITY, player -> HellchainController.press(player, true));
            RoguePower.excludeBorrowed(STARE);
            RoguePower.excludeBorrowed(HellrideHorse.ABILITY);
        }
        return new HeroPower(
                ID,
                "Spirit of Vengeance (Ghost Rider)",
                syringe,
                GhostRiderConfig.STATS,
                List.of(),
                List.of(
                        new HeroPower.Ability(HellchainController.ABILITY, "Hellchain", GhostRiderPower::hellchain),
                        new HeroPower.Ability(STARE, "Penance Stare", GhostRiderPower::penanceStare),
                        new HeroPower.Ability(HellrideHorse.ABILITY, "Hellride", GhostRiderPower::hellride),
                        HeroToggles.WHEEL
                ),
                List.of(FLAMES, TRAIL),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (player.getRemainingFireTicks() > 0 && !player.isInLava()) {
                            player.clearFire();
                        }
                        HellchainController.tick(player);
                        tickStare(player);
                        tickTrail(player);
                        if (HeroToggles.isOn(player, FLAMES)) {
                            presentation(player);
                        }
                        if (player.tickCount % 20 == 0) {
                            GhostRiderJudgment.tick(player);
                        }
                        if (player.tickCount % 100 == 0) {
                            HellfireZones.prune(player.serverLevel().getGameTime(), player);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        clearAll(player);
                        player.clearFire();
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypeTags.IS_FIRE) && !event.getSource().is(DamageTypes.LAVA)) {
                            event.setCanceled(true);
                            player.clearFire();
                            return;
                        }
                        if (STARES.containsKey(player.getUUID()) && event.getAmount() >= GhostRiderConfig.STARE_CANCEL_DAMAGE.get()) {
                            failStare(player, "§7The hit broke your stare.");
                        }
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypes.PLAYER_ATTACK) && !HeroAbilities.isAbilityDamage()
                                && event.getSource().getDirectEntity() == player) {
                            HellfireZones.ignite(player, event.getEntity());
                        }
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        clearAll(player);
                    }
                }
        );
    }

    public static boolean isRider(Player player) {
        return SerumKit.is(player, ID);
    }

    private static double hellchain(ServerPlayer player, int slot) {
        return HellchainController.press(player, !isRider(player));
    }

    private static double hellride(ServerPlayer player, int slot) {
        if (!isRider(player)) {
            return 0.0D;
        }
        return HellrideHorse.press(player);
    }

    private static double penanceStare(ServerPlayer player, int slot) {
        if (!isRider(player) || STARES.containsKey(player.getUUID())) {
            return 0.0D;
        }
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), GhostRiderConfig.STARE_RANGE.get(), 1);
        LivingEntity target = beam.targets().isEmpty() ? null : beam.targets().get(0);
        if (target == null || !HellfireZones.harmable(player, target) || !player.hasLineOfSight(target)) {
            player.displayClientMessage(Component.literal("§7Look an enemy in the eye first."), true);
            return 0.0D;
        }
        STARES.put(player.getUUID(), new Stare(target.getUUID(), player.serverLevel().dimension()));
        int stacks = GhostRiderJudgment.stacks(player, target);
        player.displayClientMessage(Component.literal("§6Judging " + target.getName().getString() + "§6, " + stacks + " sin"
                + (stacks == 1 ? "" : "s") + " counted"), true);
        HeroFx.play(player, SoundEvents.WITHER_AMBIENT, 0.6F, 0.5F);
        HeroFx.play(target, SoundEvents.SOUL_ESCAPE.value(), 1.2F, 0.6F);
        return GhostRiderConfig.STARE_CHANNEL_TICKS.get() / 20.0D + 0.5D;
    }

    private static void tickStare(ServerPlayer player) {
        Stare stare = STARES.get(player.getUUID());
        if (stare == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Entity entity = level.dimension() == stare.dimension ? level.getEntity(stare.target) : null;
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            failStare(player, null);
            return;
        }
        double range = GhostRiderConfig.STARE_RANGE.get() + 2.0D;
        if (player.distanceToSqr(target) > range * range || !player.hasLineOfSight(target) || !looking(player, target)) {
            failStare(player, "§7You looked away.");
            return;
        }
        if (stare.ticks % 4 == 0) {
            Vec3 eye = player.getEyePosition();
            Vec3 center = target.getBoundingBox().getCenter();
            HeroAbilities.particleLine(level, eye.add(player.getLookAngle().scale(0.5D)), center, ParticleTypes.SOUL_FIRE_FLAME, 0.9D);
            level.sendParticles(ParticleTypes.SOUL, center.x, center.y, center.z, 3,
                    target.getBbWidth() * 0.4D, target.getBbHeight() * 0.4D, target.getBbWidth() * 0.4D, 0.02D);
        }
        if (++stare.ticks >= GhostRiderConfig.STARE_CHANNEL_TICKS.get()) {
            STARES.remove(player.getUUID());
            judge(player, target);
        }
    }

    private static boolean looking(ServerPlayer player, LivingEntity target) {
        Vec3 to = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        double distance = to.length();
        return distance < 1.5D || to.scale(1.0D / distance).dot(player.getLookAngle()) >= Math.cos(Math.toRadians(25.0D));
    }

    private static void judge(ServerPlayer player, LivingEntity target) {
        int stacks = GhostRiderJudgment.consume(player, target);
        double scale = Math.min(GhostRiderConfig.STARE_MAX_SCALE.get(),
                GhostRiderConfig.STARE_BASE_SCALE.get() + GhostRiderConfig.STARE_STACK_SCALE.get() * stacks);
        float damage = (float) (MarvelBalanceConfig.POUNCE_DAMAGE.get() * scale);
        target.invulnerableTime = 0;
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
        if (target.isAlive() && !(target instanceof Player) && HeroAbilities.canControl(target) && !HeroAbilities.isBoss(target)) {
            HeroStun.stun(target, GhostRiderConfig.STARE_RECOIL_TICKS.get());
        }
        ServerLevel level = player.serverLevel();
        Vec3 center = target.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 30 + stacks * 10,
                target.getBbWidth() * 0.5D, target.getBbHeight() * 0.5D, target.getBbWidth() * 0.5D, 0.06D);
        level.sendParticles(ParticleTypes.SOUL, center.x, center.y, center.z, 8 + stacks * 4,
                target.getBbWidth() * 0.4D, target.getBbHeight() * 0.4D, target.getBbWidth() * 0.4D, 0.04D);
        HeroFx.play(target, SoundEvents.WITHER_SHOOT, 0.8F, 0.6F);
        HeroFx.play(target, SoundEvents.SOUL_ESCAPE.value(), 1.5F, 0.5F);
        HeroFx.shake(level, center, 6.0D, 0.3F + stacks * 0.08F, 10);
        HeroAbilities.setCooldown(player, STARE, GhostRiderConfig.STARE_COOLDOWN.get());
    }

    private static void failStare(ServerPlayer player, String message) {
        if (STARES.remove(player.getUUID()) == null) {
            return;
        }
        if (message != null) {
            player.displayClientMessage(Component.literal(message), true);
        }
        HeroFx.play(player, SoundEvents.FIRE_EXTINGUISH, 0.6F, 0.8F);
        HeroAbilities.setCooldown(player, STARE, GhostRiderConfig.STARE_FAIL_COOLDOWN.get());
    }

    private static void tickTrail(ServerPlayer player) {
        if (!HeroToggles.isOn(player, TRAIL)) {
            LAST_TRAIL.remove(player.getUUID());
            return;
        }
        SkeletonHorse horse = HellrideHorse.riding(player);
        Entity mover = horse != null ? horse : player;
        boolean moving = horse != null || player.isSprinting() && !player.isPassenger();
        if (!moving || !mover.onGround() || mover.isInWaterRainOrBubble() || mover.isInLava()) {
            return;
        }
        Vec3 at = mover.position();
        Vec3 last = LAST_TRAIL.get(player.getUUID());
        if (last != null && last.distanceToSqr(at) < 2.25D) {
            return;
        }
        LAST_TRAIL.put(player.getUUID(), at);
        if (last != null && last.distanceToSqr(at) < 64.0D) {
            HellfireZones.drop(player, at);
        }
    }

    private static void presentation(ServerPlayer player) {
        if (player.isInvisible() || player.tickCount % 2 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        if (player.isInWaterRainOrBubble()) {
            if (player.tickCount % 8 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, eye.x, eye.y + 0.2D, eye.z, 2, 0.15D, 0.1D, 0.15D, 0.01D);
            }
            return;
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, eye.x, eye.y + 0.15D, eye.z, 2, 0.17D, 0.12D, 0.17D, 0.012D);
        if (player.tickCount % 6 == 0) {
            level.sendParticles(ParticleTypes.FLAME, eye.x, eye.y + 0.35D, eye.z, 1, 0.12D, 0.05D, 0.12D, 0.01D);
        }
        if (player.tickCount % 10 == 0) {
            Vec3 chest = player.position().add(0.0D, player.getBbHeight() * 0.55D, 0.0D);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, chest.x, chest.y, chest.z, 1, 0.25D, 0.3D, 0.25D, 0.005D);
        }
        if (player.tickCount % 120 == 0) {
            HeroFx.play(player, SoundEvents.FIRE_AMBIENT, 0.4F, 0.6F);
        }
    }

    private static void clearAll(ServerPlayer player) {
        HellchainController.clear(player);
        HellfireZones.clear(player);
        GhostRiderJudgment.clear(player);
        HellrideHorse.clear(player);
        STARES.remove(player.getUUID());
        LAST_TRAIL.remove(player.getUUID());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        HellfireZones.tick(event.getServer());
        HellrideHorse.tick(event.getServer());
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearAll(player);
        }
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearAll(player);
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        HellchainController.clearAll();
        HellfireZones.clearAll();
        GhostRiderJudgment.clearAll();
        HellrideHorse.clearAll();
        STARES.clear();
        LAST_TRAIL.clear();
    }
}
