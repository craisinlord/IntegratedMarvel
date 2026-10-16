package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tintankgames.marvel.core.particles.MarvelParticleTypes;
import net.tintankgames.marvel.world.level.MarvelGameRules;

import java.util.List;

public final class KineticBurst {
    public static final int PURPLE = 0xB24BFF;
    public static final int DEEP_PURPLE = 0x7A1FD6;
    public static final int PINK = 0xE58BFF;

    private KineticBurst() {
    }

    private static ExplosionDamageCalculator calculator(Entity owner) {
        return new ExplosionDamageCalculator() {
            @Override
            public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                return false;
            }

            @Override
            public float getKnockbackMultiplier(Entity entity) {
                return entity == owner || entity instanceof net.minecraft.world.entity.item.ItemEntity ? 0.0F : 1.0F;
            }
        };
    }

    public static boolean affects(ServerPlayer owner, LivingEntity entity) {
        if (!entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (owner == null) {
            return true;
        }
        if (entity == owner) {
            return false;
        }
        if (entity instanceof TamableAnimal tamable && tamable.isTame() && owner.getUUID().equals(tamable.getOwnerUUID())) {
            return false;
        }
        if (entity instanceof Player other) {
            return owner.canHarmPlayer(other) && !other.isCreative();
        }
        return !owner.isAlliedTo(entity);
    }

    public static ParticleOptions blast(int color) {
        return ColorParticleOption.create(MarvelParticleTypes.KINETIC_BLAST.get(), 0xFF000000 | color);
    }

    public static ParticleOptions blastEmitter(int color) {
        return ColorParticleOption.create(MarvelParticleTypes.KINETIC_BLAST_EMITTER.get(), 0xFF000000 | color);
    }

    public static void trail(ServerLevel level, Vec3 at, int count) {
        level.sendParticles(HeroAbilities.emissive(PURPLE, 0.6F), at.x, at.y, at.z, count, 0.08D, 0.08D, 0.08D, 0.0D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 1, 0.05D, 0.05D, 0.05D, 0.01D);
    }

    public static void burst(ServerLevel level, Vec3 at, Entity source, ServerPlayer owner, float damage, float radius, int kinds, ItemStack stack) {
        burst(level, at, source, owner, damage, radius, kinds, stack, true);
    }

    public static void burst(ServerLevel level, Vec3 at, Entity source, ServerPlayer owner, float damage, float radius, int kinds, ItemStack stack, boolean breakBlocks) {
        boolean griefing = breakBlocks && MarvelBalanceConfig.KINETIC_BLOCK_DAMAGE.get() && level.getGameRules().getBoolean(MarvelGameRules.RULE_SUPERPOWERGRIEFING);
        boolean fire = (kinds & GambitItems.FIRE) != 0;
        ParticleOptions small = blast(PURPLE);
        ParticleOptions large = radius >= 2.0F ? blastEmitter(PURPLE) : small;
        level.explode(source != null ? source : owner, null, calculator(owner), at.x, at.y, at.z, radius, fire && griefing,
                griefing ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE, large, large, SoundEvents.GENERIC_EXPLODE);
        visuals(level, at, radius, kinds);
        if (damage > 0.0F) {
            HeroAbilities.runAsAbilityDamage(() -> hurt(level, at, source, owner, damage, radius, kinds));
        }
        effects(level, at, owner, radius, kinds, stack);
    }

    private static void hurt(ServerLevel level, Vec3 at, Entity source, ServerPlayer owner, float damage, float radius, int kinds) {
        float reach = radius + 0.5F;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(reach),
                entity -> affects(owner, entity))) {
            double distance = target.getBoundingBox().getCenter().distanceTo(at);
            if (distance > reach) {
                continue;
            }
            float scaled = damage * (float) Math.max(0.3D, 1.0D - distance / reach);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().explosion(source != null ? source : owner, owner), scaled);
            if ((kinds & GambitItems.FIRE) != 0) {
                target.igniteForSeconds(5.0F);
            }
            if ((kinds & GambitItems.ICE) != 0) {
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), 240));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), owner);
                HeroFx.effect("iceandfire:frozen").ifPresent(effect -> {
                    if (HeroAbilities.canControl(target)) {
                        target.addEffect(new MobEffectInstance(effect, 60), owner);
                    }
                });
            }
            if ((kinds & GambitItems.LIGHTNING) != 0) {
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().lightningBolt(), scaled * 0.5F);
            }
        }
    }

    public static void visuals(ServerLevel level, Vec3 at, float radius, int kinds) {
        int ring = Math.min(40, Math.max(16, (int) (radius * 10)));
        for (int i = 0; i < ring; i++) {
            double theta = Math.PI * 2.0D * i / ring;
            double phi = Math.acos(1.0D - 2.0D * (i + 0.5D) / ring);
            Vec3 direction = new Vec3(Math.cos(theta) * Math.sin(phi), Math.cos(phi), Math.sin(theta) * Math.sin(phi));
            Vec3 point = at.add(direction.scale(radius * 0.6D));
            level.sendParticles(HeroAbilities.emissive(i % 3 == 0 ? PINK : PURPLE, 1.2F), point.x, point.y, point.z, 0,
                    direction.x, direction.y, direction.z, 0.35D);
        }
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2.0D * i / 16.0D;
            Vec3 point = at.add(Math.cos(angle) * radius, 0.1D, Math.sin(angle) * radius);
            level.sendParticles(HeroAbilities.emissive(DEEP_PURPLE, 1.0F), point.x, point.y, point.z, 1, 0.0D, 0.05D, 0.0D, 0.0D);
        }
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, (int) (radius * 8), 0.2D, 0.2D, 0.2D, 0.25D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, (int) (radius * 20), radius * 0.4D, radius * 0.4D, radius * 0.4D, 0.4D);
        level.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, (int) (radius * 10), radius * 0.5D, radius * 0.4D, radius * 0.5D, 0.1D);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        if (ModCompat.ALEXS_CAVES) {
            level.sendParticles(HeroFx.particle("alexscaves:purple_witch_explosion", ParticleTypes.WITCH), at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(HeroFx.particle("alexscaves:purple_witch_magic", ParticleTypes.WITCH), at.x, at.y, at.z, (int) (radius * 6),
                    radius * 0.5D, radius * 0.5D, radius * 0.5D, 0.05D);
        }
        if (ModCompat.CELESTISYNTH) {
            level.sendParticles(HeroFx.particle("celestisynth:crescentia_firework_purple", ParticleTypes.FIREWORK), at.x, at.y, at.z, (int) (radius * 4),
                    radius * 0.4D, radius * 0.4D, radius * 0.4D, 0.1D);
        }
        if (ModCompat.ANTARCHY && radius >= 2.5F) {
            level.sendParticles(HeroFx.particle("antarchy:lucid_bolt_impact_large", ParticleTypes.WITCH), at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if ((kinds & GambitItems.FIRE) != 0) {
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, (int) (radius * 12), radius * 0.4D, radius * 0.4D, radius * 0.4D, 0.08D);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, (int) (radius * 6), radius * 0.4D, radius * 0.4D, radius * 0.4D, 0.05D);
        }
        if ((kinds & GambitItems.ICE) != 0) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, (int) (radius * 16), radius * 0.5D, radius * 0.5D, radius * 0.5D, 0.08D);
        }
        if ((kinds & GambitItems.LIGHTNING) != 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, (int) (radius * 16), radius * 0.5D, radius * 0.5D, radius * 0.5D, 0.3D);
        }
        float pitch = 1.1F + level.random.nextFloat() * 0.3F;
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.6F, 0.7F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.2F, pitch);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4F, 1.6F);
        if (radius >= 2.0F) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.2F, 0.6F);
        }
    }

    private static void effects(ServerLevel level, Vec3 at, ServerPlayer owner, float radius, int kinds, ItemStack stack) {
        if ((kinds & GambitItems.LIGHTNING) != 0) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(at.x, at.y, at.z);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
        }
        if ((kinds & GambitItems.WARP) != 0 && owner != null && owner.isAlive() && owner.level() == level) {
            Vec3 from = owner.position();
            level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0D, from.z, 40, 0.3D, 0.6D, 0.3D, 0.5D);
            owner.teleportTo(at.x, at.y + 0.1D, at.z);
            owner.fallDistance = 0.0F;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.0D, at.z, 40, 0.3D, 0.6D, 0.3D, 0.3D);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        if ((kinds & GambitItems.POTION) != 0) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.hasEffects()) {
                AreaEffectCloud cloud = new AreaEffectCloud(level, at.x, at.y, at.z);
                if (owner != null) {
                    cloud.setOwner(owner);
                }
                cloud.setPotionContents(contents);
                cloud.setRadius(Math.max(2.0F, radius));
                cloud.setRadiusOnUse(-0.3F);
                cloud.setWaitTime(5);
                cloud.setDuration(160);
                cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
                level.addFreshEntity(cloud);
            }
        }
        if ((kinds & GambitItems.FIREWORK) != 0 && owner != null) {
            Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
            FireworkExplosion single = stack.get(DataComponents.FIREWORK_EXPLOSION);
            ItemStack rocket;
            if (fireworks != null && !fireworks.explosions().isEmpty()) {
                rocket = new ItemStack(Items.FIREWORK_ROCKET);
                rocket.set(DataComponents.FIREWORKS, new Fireworks(0, fireworks.explosions()));
            } else if (single != null) {
                rocket = new ItemStack(Items.FIREWORK_ROCKET);
                rocket.set(DataComponents.FIREWORKS, new Fireworks(0, List.of(single)));
            } else {
                rocket = JubileePower.rocket(level.random, JubileePower.Palette.GAMBIT.colors(), FireworkExplosion.Shape.LARGE_BALL, 0, true, true);
            }
            JubileePower.burst(owner, at, rocket, MarvelBalanceConfig.KINETIC_DAMAGE.get().floatValue() * 0.3F);
        }
    }
}
