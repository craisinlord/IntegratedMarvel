package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.api.RogueMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.tintankgames.marvel.world.level.MarvelGameRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class RogueKit {
    public static final int GREEN = 0x4CD964;
    private static final Map<UUID, Slam> SLAMS = new HashMap<>();

    private RogueKit() {
    }

    @FunctionalInterface
    public interface Hit {
        void hit(ServerPlayer player, LivingEntity target);

        default Hit and(Hit other) {
            return (player, target) -> {
                this.hit(player, target);
                other.hit(player, target);
            };
        }
    }

    @FunctionalInterface
    public interface Spawner {
        Entity create(ServerPlayer player, ServerLevel level);
    }

    public record Effect(Holder<MobEffect> effect, int amplifier) {
    }

    private record Slam(int ticks, double radius, double damage, ParticleOptions particle, Hit hit) {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "rogue/" + path);
    }

    public static RogueMemory.Builder memory(String path, String source) {
        return RogueMemory.builder(id(path), source);
    }

    public static float dmg(double base) {
        return (float) (base * MarvelBalanceConfig.MEMORY_DAMAGE_MULTIPLIER.get());
    }

    public static boolean affects(ServerPlayer player, LivingEntity entity) {
        return KineticBurst.affects(player, entity);
    }

    public static void hurt(ServerPlayer player, LivingEntity target, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().mobAttack(player), dmg(amount)));
    }

    public static Optional<Holder<MobEffect>> effect(String id) {
        return HeroFx.effect(id).map(reference -> reference);
    }

    public static Effect fx(Holder<MobEffect> effect, int amplifier) {
        return new Effect(effect, amplifier);
    }

    public static Effect fx(String id, int amplifier) {
        return effect(id).map(holder -> new Effect(holder, amplifier)).orElse(null);
    }

    public static Hit apply(Holder<MobEffect> effect, int ticks, int amplifier) {
        return (player, target) -> target.addEffect(new MobEffectInstance(effect, ticks, amplifier), player);
    }

    public static Hit apply(String id, int ticks, int amplifier) {
        return (player, target) -> effect(id).ifPresent(effect -> target.addEffect(new MobEffectInstance(effect, ticks, amplifier), player));
    }

    public static Hit ignite(float seconds) {
        return (player, target) -> target.igniteForSeconds(seconds);
    }

    public static Hit stun(int ticks) {
        return (player, target) -> {
            if (HeroAbilities.canControl(target)) {
                HeroStun.stun(target, ticks);
            }
        };
    }

    public static Hit launch(double up) {
        return (player, target) -> {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0D, up, 0.0D));
            target.hurtMarked = true;
        };
    }

    public static Hit knock(double strength) {
        return (player, target) -> {
            Vec3 away = target.position().subtract(player.position());
            if (away.lengthSqr() > 1.0E-4D) {
                target.knockback(strength, -away.x, -away.z);
            }
        };
    }

    public static Hit pull(double strength) {
        return (player, target) -> {
            Vec3 toward = player.position().subtract(target.position());
            if (toward.lengthSqr() > 1.0E-4D) {
                target.setDeltaMovement(target.getDeltaMovement().add(toward.normalize().scale(strength)).add(0.0D, 0.2D, 0.0D));
                target.hurtMarked = true;
            }
        };
    }

    public static Hit freeze(int ticks) {
        return (player, target) -> {
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), ticks + 140));
            target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, ticks, 2), player);
        };
    }

    public static Hit untarget() {
        return (player, target) -> {
            if (target instanceof Mob mob && mob.getTarget() == player) {
                mob.setTarget(null);
            }
        };
    }

    public static Spawner entity(String id) {
        return (player, level) -> BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).map(type -> type.create(level)).orElse(null);
    }

    public static Spawner entity(EntityType<?> type) {
        return (player, level) -> type.create(level);
    }

    public static boolean exists(String entityId) {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(entityId));
    }

    private static double cd(double seconds) {
        return seconds;
    }

    public static void add(ServerLevel level, Entity entity) {
        if (!entity.isAddedToLevel()) {
            level.addFreshEntity(entity);
        }
    }

    public static RogueMemory.Active shoot(Spawner spawner, double speed, int count, float spread, double cooldown, SoundEvent sound) {
        return player -> {
            ServerLevel level = player.serverLevel();
            for (int i = 0; i < count; i++) {
                float offset = count == 1 ? 0.0F : -spread + 2.0F * spread * i / (count - 1);
                Vec3 direction = Vec3.directionFromRotation(player.getXRot(), player.getYRot() + offset);
                fire(player, level, spawner.create(player, level), direction, speed);
            }
            if (sound != null) {
                HeroFx.play(player, sound, 1.0F, 0.9F + player.getRandom().nextFloat() * 0.2F);
            }
            HeroAnimations.play(player, "web_shoot_r");
            return cd(cooldown);
        };
    }

    public static Entity fire(ServerPlayer player, ServerLevel level, Entity entity, Vec3 direction, double speed) {
        if (entity == null) {
            return null;
        }
        Vec3 from = player.getEyePosition().add(direction.scale(0.9D)).subtract(0.0D, 0.15D, 0.0D);
        entity.moveTo(from.x, from.y, from.z, player.getYRot(), player.getXRot());
        if (entity instanceof Projectile projectile) {
            projectile.setOwner(player);
            projectile.shoot(direction.x, direction.y, direction.z, (float) speed, 0.5F);
            if (projectile instanceof AbstractHurtingProjectile hurting) {
                hurting.accelerationPower = 0.08D;
            }
        } else {
            entity.setDeltaMovement(direction.scale(speed));
        }
        add(level, entity);
        return entity;
    }

    public static RogueMemory.Active cone(double range, double angle, double damage, ParticleOptions particle, SoundEvent sound, double cooldown, Hit hit) {
        return player -> {
            ServerLevel level = player.serverLevel();
            for (LivingEntity target : HeroAbilities.cone(player, range, angle)) {
                if (!affects(player, target)) {
                    continue;
                }
                hurt(player, target, damage);
                if (hit != null) {
                    hit.hit(player, target);
                }
            }
            Vec3 eye = player.getEyePosition().subtract(0.0D, 0.2D, 0.0D);
            if (particle != null) {
                for (int i = -2; i <= 2; i++) {
                    Vec3 direction = Vec3.directionFromRotation(player.getXRot(), player.getYRot() + (float) (i * angle / 3.0D));
                    HeroAbilities.particleLine(level, eye.add(direction), eye.add(direction.scale(range)), particle, 0.9D);
                }
            }
            if (sound != null) {
                HeroFx.play(player, sound, 1.2F, 1.0F);
            }
            HeroAnimations.play(player, "web_shoot_both");
            return cd(cooldown);
        };
    }

    public static RogueMemory.Active nova(double radius, double damage, ParticleOptions particle, SoundEvent sound, double cooldown, Hit hit) {
        return player -> {
            novaAt(player, player.position().add(0.0D, 0.5D, 0.0D), radius, damage, particle, hit);
            if (sound != null) {
                HeroFx.play(player, sound, 1.4F, 0.9F);
            }
            return cd(cooldown);
        };
    }

    public static void novaAt(ServerPlayer player, Vec3 center, double radius, double damage, ParticleOptions particle, Hit hit) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(center, center).inflate(radius),
                entity -> affects(player, entity) && entity.distanceToSqr(center) <= radius * radius)) {
            hurt(player, target, damage);
            if (hit != null) {
                hit.hit(player, target);
            }
        }
        if (particle != null) {
            int points = Math.max(16, (int) (radius * 10));
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points;
                level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius, 1, 0.0D, 0.1D, 0.0D, 0.02D);
            }
            level.sendParticles(particle, center.x, center.y, center.z, (int) (radius * 6), radius * 0.4D, 0.4D, radius * 0.4D, 0.05D);
        }
    }

    public static RogueMemory.Active beam(double range, double damage, int maxTargets, ParticleOptions particle, SoundEvent sound, double cooldown, Hit hit) {
        return player -> {
            ServerLevel level = player.serverLevel();
            HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, maxTargets);
            for (LivingEntity target : beam.targets()) {
                if (!affects(player, target)) {
                    continue;
                }
                hurt(player, target, damage);
                if (hit != null) {
                    hit.hit(player, target);
                }
            }
            HeroAbilities.particleLine(level, beam.start().subtract(0.0D, 0.2D, 0.0D), beam.end(), particle, 0.5D);
            if (sound != null) {
                HeroFx.play(player, sound, 1.4F, 1.0F);
            }
            HeroAnimations.play(player, "web_shoot_both");
            return cd(cooldown);
        };
    }

    public static RogueMemory.Active dash(double speed, int ticks, double damage, double knockback, ParticleOptions particle, SoundEvent sound, double cooldown) {
        return player -> {
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0D, look.z);
            Vec3 direction = flat.lengthSqr() < 1.0E-4D ? look : flat.normalize();
            HeroAbilities.startDash(player, direction.scale(speed).add(0.0D, 0.15D, 0.0D), ticks, dmg(damage), knockback,
                    entity -> affects(player, entity));
            if (particle != null) {
                player.serverLevel().sendParticles(particle, player.getX(), player.getY() + 0.8D, player.getZ(), 20, 0.4D, 0.4D, 0.4D, 0.05D);
            }
            if (sound != null) {
                HeroFx.play(player, sound, 1.2F, 1.0F);
            }
            return cd(cooldown);
        };
    }

    public static RogueMemory.Active leap(double up, double forward, double radius, double damage, ParticleOptions particle, SoundEvent sound, double cooldown, Hit hit) {
        return player -> {
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0D, look.z);
            Vec3 horizontal = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize().scale(forward);
            player.setDeltaMovement(horizontal.x, up, horizontal.z);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            SLAMS.put(player.getUUID(), new Slam(0, radius, damage, particle, hit));
            if (sound != null) {
                HeroFx.play(player, sound, 1.2F, 1.0F);
            }
            return cd(cooldown);
        };
    }

    public static RogueMemory.Active blink(double range, double cooldown) {
        return player -> {
            ServerLevel level = player.serverLevel();
            Vec3 eye = player.getEyePosition();
            BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 target = hit.getType() == HitResult.Type.MISS ? hit.getLocation() : hit.getLocation().subtract(player.getLookAngle().scale(0.8D));
            BlockPos feet = BlockPos.containing(target);
            for (int i = 0; i < 3 && !level.noCollision(player, player.getBoundingBox().move(target.subtract(player.position()))); i++) {
                target = target.add(0.0D, 1.0D, 0.0D);
            }
            level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.3D, 0.6D, 0.3D, 0.5D);
            player.teleportTo(target.x, Math.max(target.y, feet.getY()), target.z);
            player.fallDistance = 0.0F;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.3D, 0.6D, 0.3D, 0.3D);
            HeroFx.play(player, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
            return cd(cooldown);
        };
    }

    public static boolean griefing(ServerLevel level) {
        return level.getGameRules().getBoolean(MarvelGameRules.RULE_SUPERPOWERGRIEFING);
    }

    public static RogueMemory.Active detonate(float radius, double damage, boolean fire, double cooldown) {
        return player -> {
            ServerLevel level = player.serverLevel();
            explodeAt(player, player.position().add(0.0D, 0.5D, 0.0D), radius, damage, fire);
            HeroFx.shake(level, player.position(), radius * 4.0D, 0.6F, 10);
            return cd(cooldown);
        };
    }

    public static void explodeAt(ServerPlayer player, Vec3 at, float radius, double damage, boolean fire) {
        ServerLevel level = player.serverLevel();
        boolean griefing = griefing(level);
        level.explode(player, null, new net.minecraft.world.level.ExplosionDamageCalculator() {
                    @Override
                    public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, Entity entity) {
                        return false;
                    }

                    @Override
                    public float getKnockbackMultiplier(Entity entity) {
                        return entity == player ? 0.0F : 1.0F;
                    }
                }, at.x, at.y, at.z, radius, fire && griefing,
                griefing ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(radius + 1.0D),
                entity -> affects(player, entity))) {
            double distance = target.getBoundingBox().getCenter().distanceTo(at);
            if (distance <= radius + 1.0D) {
                hurt(player, target, damage * Math.max(0.3D, 1.0D - distance / (radius + 1.0D)));
                if (fire) {
                    target.igniteForSeconds(4.0F);
                }
            }
        }
    }

    public static RogueMemory.Active toss(double range, double up, double damage, double cooldown) {
        return player -> {
            List<LivingEntity> targets = new ArrayList<>(HeroAbilities.cone(player, range, 45.0D).stream()
                    .filter(entity -> affects(player, entity)).toList());
            targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
            if (targets.isEmpty()) {
                return 0.5D;
            }
            LivingEntity target = targets.get(0);
            hurt(player, target, damage);
            Vec3 look = player.getLookAngle();
            target.setDeltaMovement(look.x * 0.6D, up, look.z * 0.6D);
            target.hurtMarked = true;
            player.serverLevel().sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(), target.getZ(), 12, 0.3D, 0.1D, 0.3D, 0.05D);
            HeroFx.play(player, SoundEvents.IRON_GOLEM_ATTACK, 1.2F, 1.0F);
            HeroAnimations.play(player, "uppercut");
            return cd(cooldown);
        };
    }

    public static RogueMemory.Active roar(double radius, double damage, int stunTicks, double knockback, ParticleOptions particle, SoundEvent sound, double cooldown) {
        return nova(radius, damage, particle, sound, cooldown, stun(stunTicks).and(knock(knockback)));
    }

    public static RogueMemory.Active buff(double cooldown, SoundEvent sound, MobEffectInstance... effects) {
        return player -> {
            for (MobEffectInstance effect : effects) {
                player.addEffect(new MobEffectInstance(effect));
            }
            if (sound != null) {
                HeroFx.play(player, sound, 1.0F, 1.0F);
            }
            player.serverLevel().sendParticles(HeroAbilities.emissive(GREEN, 1.0F), player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.4D, 0.6D, 0.4D, 0.0D);
            return cd(cooldown);
        };
    }

    public static RogueMemory.Active web(double range, int ticks, double cooldown) {
        return player -> {
            ServerLevel level = player.serverLevel();
            LivingEntity target = HeroAbilities.beam(player, player.getLookAngle(), range, 1).targets().stream()
                    .filter(entity -> affects(player, entity)).findFirst().orElse(null);
            Vec3 at;
            if (target != null) {
                at = target.position();
                target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, ticks, 3), player);
            } else {
                Vec3 eye = player.getEyePosition();
                at = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
            }
            BlockPos pos = BlockPos.containing(at);
            TempBlocks.place(level, player, pos, Blocks.COBWEB.defaultBlockState(), ticks);
            HeroAbilities.particleLine(level, player.getEyePosition().subtract(0.0D, 0.2D, 0.0D), at, ParticleTypes.WHITE_ASH, 0.4D);
            HeroFx.play(player, SoundEvents.SPIDER_AMBIENT, 1.0F, 1.4F);
            return cd(cooldown);
        };
    }

    public static RogueMemory.Active lightning(double range, double damage, double cooldown) {
        return player -> {
            ServerLevel level = player.serverLevel();
            HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 1);
            Vec3 at = beam.end();
            net.minecraft.world.entity.LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(at.x, at.y, at.z);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            novaAt(player, at, 2.5D, damage, ParticleTypes.ELECTRIC_SPARK, ignite(2.0F));
            return cd(cooldown);
        };
    }

    public static RogueMemory.Passive effects(Effect... effects) {
        List<Effect> list = java.util.Arrays.stream(effects).filter(java.util.Objects::nonNull).toList();
        return new RogueMemory.Passive() {
            @Override
            public void tick(ServerPlayer player) {
                if (player.tickCount % 20 != 0) {
                    return;
                }
                for (Effect effect : list) {
                    MobEffectInstance current = player.getEffect(effect.effect());
                    if (current == null || current.getAmplifier() < effect.amplifier()
                            || current.isAmbient() && current.getAmplifier() == effect.amplifier() && current.getDuration() < 40) {
                        player.addEffect(new MobEffectInstance(effect.effect(), 80, effect.amplifier(), true, false, true));
                    }
                }
            }

            @Override
            public void end(ServerPlayer player) {
                for (Effect effect : list) {
                    MobEffectInstance current = player.getEffect(effect.effect());
                    if (current != null && current.isAmbient() && current.getDuration() <= 80) {
                        player.removeEffect(effect.effect());
                    }
                }
            }
        };
    }

    public static RogueMemory.Passive onHit(Hit hit) {
        return new RogueMemory.Passive() {
            @Override
            public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                if (event.getSource().getDirectEntity() == player && !HeroAbilities.isAbilityDamage()) {
                    hit.hit(player, event.getEntity());
                }
            }
        };
    }

    public static RogueMemory.Passive retaliate(Hit hit) {
        return new RogueMemory.Passive() {
            @Override
            public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != player && affects(player, attacker)) {
                    hit.hit(player, attacker);
                }
            }
        };
    }

    public static RogueMemory.Passive bonus(double multiplier) {
        return new RogueMemory.Passive() {
            @Override
            public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                if (event.getSource().getDirectEntity() == player) {
                    event.setAmount(event.getAmount() * (float) multiplier);
                }
            }
        };
    }

    @SafeVarargs
    public static RogueMemory.Passive immune(TagKey<DamageType>... tags) {
        return new RogueMemory.Passive() {
            @Override
            public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                for (TagKey<DamageType> tag : tags) {
                    if (event.getSource().is(tag)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        };
    }

    @SafeVarargs
    public static RogueMemory.Passive immune(ResourceKey<DamageType>... types) {
        return new RogueMemory.Passive() {
            @Override
            public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                for (ResourceKey<DamageType> type : types) {
                    if (event.getSource().is(type)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        };
    }

    public static RogueMemory.Passive resist(double multiplier, Predicate<LivingIncomingDamageEvent> when) {
        return new RogueMemory.Passive() {
            @Override
            public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                if (when.test(event)) {
                    event.setAmount(event.getAmount() * (float) multiplier);
                }
            }
        };
    }

    public static RogueMemory.Passive noFall() {
        return immune(DamageTypeTags.IS_FALL);
    }

    public static RogueMemory.Passive climb() {
        return new RogueMemory.Passive() {
            @Override
            public void tick(ServerPlayer player) {
                if (player.horizontalCollision && !player.onGround() && !player.isSpectator()) {
                    Vec3 motion = player.getDeltaMovement();
                    player.setDeltaMovement(motion.x, player.isShiftKeyDown() ? 0.0D : Math.max(motion.y, 0.22D), motion.z);
                    player.hurtMarked = true;
                    player.fallDistance = 0.0F;
                }
            }
        };
    }

    public static RogueMemory.Passive waterHurts(double damage) {
        return new RogueMemory.Passive() {
            @Override
            public void tick(ServerPlayer player) {
                if (player.tickCount % 20 == 0 && player.isInWaterRainOrBubble()) {
                    player.hurt(player.damageSources().drown(), (float) damage);
                }
            }
        };
    }

    public static RogueMemory.Passive aura(double radius, int interval, Hit hit, ParticleOptions particle) {
        return new RogueMemory.Passive() {
            @Override
            public void tick(ServerPlayer player) {
                if (player.tickCount % interval != 0) {
                    return;
                }
                for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
                    if (affects(player, target) && !(target instanceof net.minecraft.world.entity.player.Player)) {
                        hit.hit(player, target);
                    }
                }
                if (particle != null) {
                    player.serverLevel().sendParticles(particle, player.getX(), player.getY() + 1.0D, player.getZ(), 6, radius * 0.4D, 0.5D, radius * 0.4D, 0.0D);
                }
            }
        };
    }

    public static RogueMemory.Passive trail(ParticleOptions particle, int interval) {
        return new RogueMemory.Passive() {
            @Override
            public void tick(ServerPlayer player) {
                if (player.tickCount % interval == 0) {
                    player.serverLevel().sendParticles(particle, player.getX(), player.getY() + 0.8D, player.getZ(), 1, 0.3D, 0.4D, 0.3D, 0.0D);
                }
            }
        };
    }

    public static void tick(ServerPlayer player) {
        Slam slam = SLAMS.get(player.getUUID());
        if (slam == null) {
            return;
        }
        if (slam.ticks() > 4 && (player.onGround() || player.isInWater())) {
            SLAMS.remove(player.getUUID());
            ServerLevel level = player.serverLevel();
            novaAt(player, player.position().add(0.0D, 0.3D, 0.0D), slam.radius(), slam.damage(), slam.particle(), slam.hit());
            level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.3D, player.getZ(), 2, 0.5D, 0.1D, 0.5D, 0.0D);
            HeroFx.shake(level, player.position(), slam.radius() * 3.0D, 0.6F, 10);
            HeroFx.play(player, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.2F);
            return;
        }
        SLAMS.put(player.getUUID(), new Slam(slam.ticks() + 1, slam.radius(), slam.damage(), slam.particle(), slam.hit()));
        if (slam.ticks() > 200) {
            SLAMS.remove(player.getUUID());
        }
    }

    public static boolean slamming(ServerPlayer player) {
        return SLAMS.containsKey(player.getUUID());
    }

    public static void clear(ServerPlayer player) {
        SLAMS.remove(player.getUUID());
    }
}
