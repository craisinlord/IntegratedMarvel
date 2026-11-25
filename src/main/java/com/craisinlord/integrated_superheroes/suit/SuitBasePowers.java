package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.ScorchedGunsTurret;
import com.craisinlord.integrated_superheroes.gauntlet.GauntletFx;
import com.craisinlord.integrated_superheroes.hero.ServerState;
import com.craisinlord.integrated_superheroes.hero.HeroCaster;
import com.craisinlord.integrated_superheroes.hero.IronManSuits;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.tintankgames.marvel.core.components.MarvelDataComponents;
import net.tintankgames.marvel.core.particles.EmissiveDustParticleOptions;
import net.tintankgames.marvel.core.particles.MarvelParticleTypes;
import net.tintankgames.marvel.sounds.MarvelSoundEvents;
import net.tintankgames.marvel.world.damagesources.MarvelDamageTypes;
import net.tintankgames.marvel.world.entity.projectile.Flame;
import net.tintankgames.marvel.world.entity.projectile.Repulsor;
import net.tintankgames.marvel.world.entity.projectile.ThrownMjolnir;
import net.tintankgames.marvel.world.entity.projectile.WaspSting;
import net.tintankgames.marvel.world.item.MarvelItems;
import net.tintankgames.marvel.world.item.ShoulderTurretItem;
import net.tintankgames.marvel.world.level.KineticExplosionDamageCalculator;
import net.tintankgames.marvel.world.level.MarvelGameRules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class SuitBasePowers {
    private static final int UNIBEAM_COLOR = 11466748;
    private static final int UNIBEAM_CHARGE_TICKS = 20;
    private static final int REPULSOR_CHARGE_TICKS = 6;
    private static final int FLAMETHROWER_TICKS = 30;
    private static final int TURRET_SHOTS = 3;
    private static final int TURRET_SPACING = 5;
    private static final int TURRET_ROUNDS = 6;
    private static final int TURRET_ROUND_SPACING = 2;
    private static final int MJOLNIR_LIFETIME = 200;
    private static final float MAX_ABSORBED = 25.0F;
    private static final Map<UUID, Integer> FLAMETHROWERS = ServerState.map(new HashMap<>());
    private static final Map<ThrownMjolnir, UUID> HAMMERS = ServerState.map(new HashMap<>());
    private static boolean registered;

    private SuitBasePowers() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(SuitBasePowers::onDamaged);
        NeoForge.EVENT_BUS.addListener(SuitBasePowers::onJoin);
        HeroCaster.onTick(SuitBasePowers::tick);
        HeroCaster.onCleanup(entity -> {
            FLAMETHROWERS.remove(entity.getUUID());
            HAMMERS.entrySet().removeIf(entry -> {
                boolean owned = entry.getValue().equals(entity.getUUID()) || !entry.getKey().isAlive();
                if (owned) {
                    entry.getKey().discard();
                }
                return owned;
            });
        });
    }

    static double repulsor(ServerLevel level, Mob mob, LivingEntity target) {
        IronManSuits.Stats stats = IronManSuits.forWearer(mob);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), MarvelSoundEvents.IRON_MAN_REPULSOR_CHARGE.get(), SoundSource.PLAYERS);
        VillainWorld.later(level, REPULSOR_CHARGE_TICKS, () -> {
            if (!mob.isAlive()) {
                return;
            }
            reaim(mob);
            Repulsor repulsor = new Repulsor(level, mob);
            repulsor.shootFromRotation(mob, mob.getXRot(), mob.getYRot(), 0.0F, 2.5F, 1.0F);
            level.addFreshEntity(repulsor);
            level.playSound(null, repulsor.getX(), repulsor.getY(), repulsor.getZ(), MarvelSoundEvents.IRON_MAN_REPULSOR_SHOOT.get(), SoundSource.PLAYERS);
        });
        return stats == null ? 0.75D : stats.repulsorCooldownTicks().get() / 20.0D;
    }

    static double unibeam(ServerLevel level, Mob mob, LivingEntity target) {
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), MarvelSoundEvents.IRON_MAN_UNIBEAM_CHARGE.get(), SoundSource.PLAYERS);
        VillainWorld.later(level, UNIBEAM_CHARGE_TICKS, () -> {
            if (mob.isAlive()) {
                reaim(mob);
                fireUnibeam(level, mob);
            }
        });
        return 5.0D;
    }

    private static void fireUnibeam(ServerLevel level, Mob mob) {
        double chestHeight = Math.max(0.5D, mob.getEyeHeight() - 0.37D);
        Vec3 origin = mob.position().add(0.0D, chestHeight, 0.0D);
        Vec3 view = mob.getViewVector(0.0F);
        HitResult hit = unibeamHit(mob, origin, view, 100.0D);
        double overcharge = SuitStates.consumeOvercharge(mob);
        IronManSuits.Stats stats = IronManSuits.forWearer(mob);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living && stats != null && stats.unibeamDamage().get() > 0.0D) {
            SuitTargeting.hit(mob, living, (float) (stats.unibeamDamage().get() * overcharge));
        }
        if (hit.getType() != HitResult.Type.MISS) {
            ItemStack chest = mob.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            float multiplier = chest.is(MarvelItems.Tags.IRON_MAN_MARK_17_ARMOR) ? 2.0F : chest.is(MarvelItems.Tags.IRON_MAN_MARK_24_ARMOR) ? 1.5F : 1.0F;
            float radius = level.getGameRules().getBoolean(MarvelGameRules.RULE_SUPERPOWERGRIEFING) ? 3.0F * multiplier : 0.0F;
            Vec3 at = hit.getLocation();
            level.explode(mob, at.x, at.y, at.z, radius, Level.ExplosionInteraction.NONE);
        }
        double length = origin.distanceTo(hit.getLocation());
        GauntletFx.line(level, new EmissiveDustParticleOptions(UNIBEAM_COLOR, 1.0F), origin, hit.getLocation(), Math.min(512, (int) (length / 0.1D)), 0.0F);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), MarvelSoundEvents.IRON_MAN_UNIBEAM_SHOOT.get(), SoundSource.PLAYERS);
    }

    private static HitResult unibeamHit(Mob mob, Vec3 origin, Vec3 view, double range) {
        Vec3 end = origin.add(view.scale(range));
        HitResult hit = mob.level().clip(new ClipContext(mob.getEyePosition(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        if (hit.getType() != HitResult.Type.MISS) {
            end = hit.getLocation();
        }
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(mob.level(), mob, mob.getEyePosition(), end,
                mob.getBoundingBox().expandTowards(view.scale(range)).inflate(1.0D), candidate -> candidate instanceof LivingEntity && HeroCaster.foe(mob, candidate));
        return entity != null ? entity : hit;
    }

    static double flamethrower(ServerLevel level, Mob mob, LivingEntity target) {
        FLAMETHROWERS.put(mob.getUUID(), FLAMETHROWER_TICKS);
        return 4.0D;
    }

    private static void tickFlamethrower(Mob mob) {
        Integer left = FLAMETHROWERS.get(mob.getUUID());
        if (left == null) {
            return;
        }
        if (left <= 0 || mob.isInWater()) {
            FLAMETHROWERS.remove(mob.getUUID());
            return;
        }
        FLAMETHROWERS.put(mob.getUUID(), left - 1);
        Level level = mob.level();
        int used = FLAMETHROWER_TICKS - left;
        if (used % 5 == 0) {
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS);
        }
        ItemStack thrower = MarvelItems.FLAMETHROWER.toStack();
        Flame flame = new SafeFlame(level, mob, thrower);
        flame.shootFromRotation(mob, mob.getXRot(), mob.getYRot(), 0.0F, 2.0F, 2.0F);
        level.addFreshEntity(flame);
    }

    static double shoulderTurret(ServerLevel level, Mob mob, LivingEntity target) {
        if (!(MarvelItems.SHOULDER_TURRET.get() instanceof ShoulderTurretItem turret)) {
            return 0.0D;
        }
        if (ModCompat.SCORCHED_GUNS) {
            ItemStack gun = ScorchedGunsTurret.variantGun();
            for (int i = 0; i < TURRET_ROUNDS; i++) {
                VillainWorld.later(level, i * TURRET_ROUND_SPACING, () -> {
                    LivingEntity aim = mob.isAlive() ? HeroCaster.target(mob) : null;
                    if (aim != null) {
                        ScorchedGunsTurret.variantShot(level, mob, gun, aim);
                    }
                });
            }
            return 2.0D;
        }
        ItemStack stack = new ItemStack(turret);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS);
        for (int i = 0; i < TURRET_SHOTS; i++) {
            VillainWorld.later(level, i * TURRET_SPACING, () -> {
                if (mob.isAlive()) {
                    turret.performShooting(level, mob, InteractionHand.MAIN_HAND, stack, 3.15F, 1.0F, HeroCaster.target(mob));
                }
            });
        }
        return 2.0D;
    }

    static double waspSting(ServerLevel level, Mob mob, LivingEntity target) {
        WaspSting sting = new WaspSting(level, mob.getX(), mob.getEyeY() - 0.1D, mob.getZ());
        sting.setOwner(mob);
        sting.setDeltaMovement(mob.getViewVector(1.0F).scale(3.0D));
        sting.shootFromRotation(mob, mob.getXRot(), mob.getYRot(), 0.0F, 2.5F, 1.0F);
        level.addFreshEntity(sting);
        level.playSound(null, sting.getX(), sting.getY(), sting.getZ(), MarvelSoundEvents.WASP_STING.get(), SoundSource.PLAYERS);
        return 0.75D;
    }

    static double mjolnir(ServerLevel level, Mob mob, LivingEntity target) {
        for (Map.Entry<ThrownMjolnir, UUID> entry : HAMMERS.entrySet()) {
            if (entry.getKey().isAlive() && entry.getValue().equals(mob.getUUID())) {
                return 0.0D;
            }
        }
        ItemStack held = mob.getMainHandItem();
        ItemStack stack = held.is(MarvelItems.MJOLNIR) ? held.copyWithCount(1) : MarvelItems.MJOLNIR.toStack();
        ThrownMjolnir hammer = new ThrownMjolnir(level, mob, stack);
        hammer.setBaseDamage(mjolnirDamage(stack));
        hammer.shootFromRotation(mob, mob.getXRot(), mob.getYRot(), 0.0F, 2.5F, 1.0F);
        level.addFreshEntity(hammer);
        level.playSound(null, hammer, MarvelSoundEvents.MJOLNIR_THROW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        HAMMERS.put(hammer, mob.getUUID());
        return 3.0D;
    }

    private static double mjolnirDamage(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers != null) {
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
                    return entry.modifier().amount() + 1.0D;
                }
            }
        }
        return 12.0D;
    }

    private static void tickHammers(Mob mob) {
        Iterator<Map.Entry<ThrownMjolnir, UUID>> iterator = HAMMERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ThrownMjolnir, UUID> entry = iterator.next();
            ThrownMjolnir hammer = entry.getKey();
            if (!hammer.isAlive()) {
                iterator.remove();
                continue;
            }
            if (!entry.getValue().equals(mob.getUUID())) {
                continue;
            }
            boolean home = hammer.returningToOwner() && hammer.distanceToSqr(mob.getEyePosition()) < 4.0D;
            if (home || hammer.tickCount > MJOLNIR_LIFETIME || !mob.isAlive()) {
                if (home) {
                    hammer.playSound(MarvelSoundEvents.MJOLNIR_CATCH.get());
                }
                hammer.discard();
                iterator.remove();
            }
        }
    }

    static double kineticBlast(ServerLevel level, Mob mob, LivingEntity target) {
        float absorbed = 0.0F;
        for (ItemStack stack : mob.getArmorSlots()) {
            if (stack.has(MarvelDataComponents.ABSORBED_DAMAGE)) {
                absorbed += stack.getOrDefault(MarvelDataComponents.ABSORBED_DAMAGE, 0.0F);
                stack.set(MarvelDataComponents.ABSORBED_DAMAGE, 0.0F);
            }
        }
        if (absorbed <= 0.0F) {
            return 0.0D;
        }
        int color = mob.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getBarColor();
        level.explode(mob, mob.damageSources().source(MarvelDamageTypes.KINETIC_BLAST, mob), new KineticExplosionDamageCalculator(absorbed / 5.0F, null),
                mob.getX(), mob.getY(), mob.getZ(), absorbed / 20.0F, false, Level.ExplosionInteraction.NONE,
                ColorParticleOption.create(MarvelParticleTypes.KINETIC_BLAST_EMITTER.get(), color),
                ColorParticleOption.create(MarvelParticleTypes.KINETIC_BLAST_EMITTER.get(), color),
                SoundEvents.GENERIC_EXPLODE);
        return 1.0D;
    }

    private static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity living = event.getEntity();
        if (living instanceof Player || living.level().isClientSide || event.getSource().getEntity() == null) {
            return;
        }
        float absorbed = event.getNewDamage() / 2.0F;
        for (ItemStack stack : living.getArmorSlots()) {
            if (stack.has(MarvelDataComponents.ABSORBED_DAMAGE)) {
                stack.update(MarvelDataComponents.ABSORBED_DAMAGE, 0.0F, damage -> Math.min(damage + absorbed, MAX_ABSORBED));
            }
        }
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LightningBolt bolt) || HAMMERS.isEmpty()
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (ThrownMjolnir hammer : HAMMERS.keySet()) {
            if (hammer.isAlive() && hammer.getOwner() instanceof Mob owner && hammer.distanceToSqr(bolt) < 16.0D) {
                bolt.setVisualOnly(true);
                Vec3 at = bolt.position();
                for (Entity struck : level.getEntities(bolt, new AABB(at.x - 3.0D, at.y - 3.0D, at.z - 3.0D, at.x + 3.0D, at.y + 9.0D, at.z + 3.0D),
                        entity -> entity.isAlive() && entity != owner && HeroCaster.foe(owner, entity))) {
                    struck.thunderHit(level, bolt);
                }
                return;
            }
        }
    }

    private static void tick(LivingEntity entity) {
        if (entity instanceof Mob mob) {
            tickFlamethrower(mob);
            if (!HAMMERS.isEmpty()) {
                tickHammers(mob);
            }
        }
    }

    private static void reaim(Mob mob) {
        LivingEntity now = HeroCaster.target(mob);
        if (now != null) {
            HeroCaster.aim(mob, now, 360.0F);
        }
    }

    private static final class SafeFlame extends Flame {
        private SafeFlame(Level level, LivingEntity owner, ItemStack thrower) {
            super(level, owner, thrower, thrower);
        }

        @Override
        protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {
            discard();
        }

        @Override
        protected void onHitEntity(EntityHitResult result) {
            Entity entity = result.getEntity();
            Entity owner = getOwner();
            float damage = (float) (2.0D * (IronManSuits.isLoaded() ? IronManSuits.FLAMETHROWER_DAMAGE.get() / 2.0D : 1.0D));
            var source = damageSources().source(MarvelDamageTypes.FLAMETHROWER, this, owner == null ? this : owner);
            if (entity.hurt(source, damage) && entity.getType() != net.minecraft.world.entity.EntityType.ENDERMAN) {
                entity.igniteForSeconds(4.0F);
                if (entity instanceof LivingEntity living) {
                    doKnockback(living, source);
                    doPostHurtEffects(living);
                }
            }
            discard();
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }
    }
}
