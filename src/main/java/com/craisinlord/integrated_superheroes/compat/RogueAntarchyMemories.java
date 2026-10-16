package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.antarchy.content.entity.OctopusBombEntity;
import com.craisinlord.antarchy.content.entity.brutalfly.BrutalflyOrbEntity;
import com.craisinlord.antarchy.content.entity.royal.RoyalBlackHoleEntity;
import com.craisinlord.antarchy.content.entity.royal.RoyalBoltEntity;
import com.craisinlord.antarchy.content.entity.royal.RoyalElementalProjectileEntity;
import com.craisinlord.antarchy.content.entity.royal.RoyalIceSpikeEntity;
import com.craisinlord.antarchy.content.entity.royal.beam.RoyalBeamElement;
import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.hero.RogueKit;
import com.craisinlord.integrated_superheroes.hero.RogueMemories;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RogueAntarchyMemories {
    private RogueAntarchyMemories() {
    }

    private static ParticleOptions particle(String name, ParticleOptions fallback) {
        return HeroFx.particle("antarchy:" + name, fallback);
    }

    private static RogueMemory.Active royalBolt(RoyalBeamElement element) {
        return RogueKit.shoot((player, level) -> {
            Entity entity = RogueKit.entity("antarchy:royal_bolt").create(player, level);
            if (entity instanceof RoyalBoltEntity bolt) {
                bolt.setElement(element);
            }
            return entity;
        }, 1.6D, 3, 6.0F, 2.5D, SoundEvents.BLAZE_SHOOT);
    }

    public static void register() {
        RogueMemories.reg("antarchy:queen", RogueKit.memory("antarchy/queen", "The Queen")
                .active("Black Hole", player -> {
                    ServerLevel level = player.serverLevel();
                    Vec3 look = player.getLookAngle();
                    Vec3 from = player.getEyePosition().add(look.scale(2.5D));
                    RogueKit.add(level, RoyalBlackHoleEntity.createProjectile(level, from, look.scale(0.6D), player.getUUID()));
                    HeroFx.play(player, SoundEvents.WARDEN_SONIC_CHARGE, 1.5F, 0.6F);
                    HeroAbilities.particleLine(level, player.getEyePosition(), from, particle("lucid_bolt_impact_small", ParticleTypes.REVERSE_PORTAL), 0.5D);
                    return 25.0D;
                })
                .passives(RogueKit.noFall(), RogueKit.resist(0.8D, event -> true))
                .build());
        RogueMemories.reg("antarchy:king", RogueKit.memory("antarchy/king", "The King")
                .active("Royal Volley", player -> {
                    ServerLevel level = player.serverLevel();
                    List<RoyalBeamElement> elements = List.of(RoyalBeamElement.FIRE, RoyalBeamElement.ICE, RoyalBeamElement.LIGHTNING, RoyalBeamElement.KING_GOLD);
                    for (int i = 0; i < elements.size(); i++) {
                        Vec3 direction = Vec3.directionFromRotation(player.getXRot(), player.getYRot() - 12.0F + i * 8.0F);
                        Vec3 from = player.getEyePosition().add(direction.scale(1.0D));
                        RoyalElementalProjectileEntity projectile = RoyalElementalProjectileEntity.create(level, player, elements.get(i), from, direction);
                        if (projectile != null) {
                            projectile.setOwner(player);
                            projectile.shoot(direction.x, direction.y, direction.z, 1.4F, 0.0F);
                            RogueKit.add(level, projectile);
                        }
                    }
                    HeroFx.play(player, SoundEvents.EVOKER_CAST_SPELL, 1.5F, 0.7F);
                    return 6.0D;
                })
                .passives(RogueKit.resist(0.8D, event -> true), RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)))
                .build());
        RogueMemories.reg("antarchy:prince", RogueKit.memory("antarchy/prince", "The Prince")
                .active("Royal Bolts", royalBolt(RoyalBeamElement.LIGHTNING))
                .passive(RogueKit.noFall())
                .build());
        RogueMemories.reg("antarchy:princess", RogueKit.memory("antarchy/princess", "The Princess")
                .active("Ice Spikes", player -> {
                    ServerLevel level = player.serverLevel();
                    boolean any = false;
                    for (LivingEntity target : HeroAbilities.cone(player, 18.0D, 40.0D)) {
                        if (RogueKit.affects(player, target)) {
                            RogueKit.add(level, RoyalIceSpikeEntity.create(level, target.position(), player));
                            any = true;
                        }
                    }
                    if (!any) {
                        Vec3 ahead = player.position().add(player.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize().scale(4.0D));
                        RogueKit.add(level, RoyalIceSpikeEntity.create(level, ahead, player));
                    }
                    HeroFx.play(player, SoundEvents.GLASS_BREAK, 1.2F, 0.6F);
                    return 6.0D;
                })
                .passive(RogueKit.noFall())
                .build());
        RogueMemories.reg("antarchy:basilisk", RogueKit.memory("antarchy/basilisk", "Basilisk")
                .active("Petrifying Gaze", RogueKit.cone(14.0D, 25.0D, 12.0D, ParticleTypes.WITCH, SoundEvents.ELDER_GUARDIAN_CURSE, 12.0D,
                        RogueKit.stun(60).and(RogueKit.apply("antarchy:paralyzed", 60, 0)).and(RogueKit.apply(MobEffects.POISON, 100, 1))
                                .and(RogueKit.apply(MobEffects.MOVEMENT_SLOWDOWN, 120, 2))))
                .passive(RogueKit.onHit(RogueKit.apply(MobEffects.POISON, 60, 0)))
                .build());
        RogueMemories.reg("antarchy:lucid", RogueKit.memory("antarchy/lucid", "Lucid")
                .active("Lucid Bolt", RogueKit.shoot(RogueKit.entity("antarchy:lucid_bolt"), 1.5D, 3, 10.0F, 3.0D, SoundEvents.ILLUSIONER_CAST_SPELL))
                .passive(RogueKit.onHit(RogueKit.apply("antarchy:inverted", 80, 0)))
                .build());
        RogueMemories.reg("antarchy:nightmare", RogueKit.memory("antarchy/nightmare", "Nightmare")
                .active("Dread Howl", RogueKit.nova(9.0D, 10.0D, particle("nightmare_fire_flame", ParticleTypes.SOUL_FIRE_FLAME), SoundEvents.WARDEN_ROAR, 12.0D,
                        RogueKit.apply("antarchy:dread", 200, 0).and(RogueKit.apply(MobEffects.WEAKNESS, 200, 1)).and(RogueKit.untarget())))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.NIGHT_VISION, 0)))
                .build());
        RogueMemories.reg("antarchy:kraken", RogueKit.memory("antarchy/kraken", "Kraken")
                .active("Kraken Slam", RogueKit.leap(1.1D, 0.5D, 5.5D, 28.0D, ParticleTypes.SPLASH, SoundEvents.ELDER_GUARDIAN_AMBIENT, 10.0D,
                        (player, target) -> {
                            net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(player.serverLevel());
                            if (bolt != null) {
                                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                                bolt.setVisualOnly(true);
                                player.serverLevel().addFreshEntity(bolt);
                            }
                            RogueKit.hurt(player, target, 10.0D);
                        }))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.DOLPHINS_GRACE, 0)))
                .build());
        RogueMemories.reg("antarchy:toreterror", RogueKit.memory("antarchy/toreterror", "Toreterror")
                .active("Seismic Leap", RogueKit.leap(1.6D, 1.0D, 6.0D, 30.0D, ParticleTypes.CLOUD, SoundEvents.RAVAGER_ROAR, 9.0D,
                        RogueKit.knock(1.8D).and(RogueKit.launch(0.5D))))
                .passive(RogueKit.noFall())
                .build());
        RogueMemories.reg("antarchy:molevore", RogueKit.memory("antarchy/molevore", "Molevore")
                .active("Spin Drill", RogueKit.dash(1.9D, 12, 28.0D, 1.2D, ParticleTypes.POOF, SoundEvents.ROOTED_DIRT_BREAK, 6.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DIG_SPEED, 2), RogueKit.fx(MobEffects.NIGHT_VISION, 0)))
                .build());
        RogueMemories.reg("antarchy:hercules_beetle", RogueKit.memory("antarchy/hercules_beetle", "Hercules Beetle")
                .active("Launch", RogueKit.toss(5.0D, 1.9D, 28.0D, 5.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_RESISTANCE, 0)))
                .build());
        RogueMemories.reg("antarchy:brutalfly", RogueKit.memory("antarchy/brutalfly", "Brutalfly")
                .active("Toxic Orb", RogueKit.shoot((player, level) -> {
                    Entity entity = RogueKit.entity("antarchy:brutalfly_orb").create(player, level);
                    if (entity instanceof BrutalflyOrbEntity orb) {
                        orb.setOrbVariant(player.getRandom().nextBoolean() ? BrutalflyOrbEntity.OrbVariant.POISON : BrutalflyOrbEntity.OrbVariant.STICKY);
                    }
                    return entity;
                }, 1.3D, 1, 0.0F, 3.0D, SoundEvents.SLIME_SQUISH))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0)))
                .build());
        RogueMemories.reg("antarchy:vortex", RogueKit.memory("antarchy/vortex", "Vortex")
                .active("Wind Vortex", player -> {
                    AntarchyWindVortex.gale(player.serverLevel(), player.getEyePosition(), player.getLookAngle(), player);
                    HeroFx.play(player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.2F, 0.8F);
                    return 8.0D;
                })
                .passives(RogueKit.noFall(), RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0)))
                .build());
        RogueMemories.reg("antarchy:manticore", RogueKit.memory("antarchy/manticore", "Manticore")
                .active("Tail Sting", RogueKit.beam(7.0D, 20.0D, 1, ParticleTypes.CRIT, SoundEvents.BEE_STING, 3.0D,
                        RogueKit.apply(MobEffects.POISON, 120, 1).and(RogueKit.apply(MobEffects.WITHER, 60, 0))))
                .passive(RogueKit.onHit(RogueKit.apply(MobEffects.POISON, 80, 0)))
                .build());
        RogueMemories.reg("antarchy:scorpion", RogueKit.memory("antarchy/scorpion", "Scorpion")
                .active("Venom Sting", RogueKit.beam(5.0D, 14.0D, 1, ParticleTypes.CRIT, SoundEvents.SPIDER_AMBIENT, 3.0D, RogueKit.apply(MobEffects.POISON, 120, 1)))
                .passive(RogueKit.climb())
                .build());
        RogueMemories.reg("antarchy:emperor_scorpion", RogueKit.memory("antarchy/emperor_scorpion", "Emperor Scorpion")
                .active("Emperor's Sting", RogueKit.cone(6.0D, 40.0D, 26.0D, ParticleTypes.CRIT, SoundEvents.RAVAGER_ATTACK, 5.0D,
                        RogueKit.apply(MobEffects.POISON, 160, 2).and(RogueKit.knock(1.0D))))
                .passives(RogueKit.resist(0.75D, event -> true), RogueKit.onHit(RogueKit.apply(MobEffects.POISON, 80, 1)))
                .build());
        RogueMemories.reg("antarchy:spit_bug", RogueKit.memory("antarchy/spit_bug", "Spit Bug")
                .active("Acid Spit", RogueKit.shoot(RogueKit.entity("antarchy:spit_bug_projectile"), 1.5D, 3, 6.0F, 2.0D, SoundEvents.LLAMA_SPIT))
                .build());
        RogueMemories.reg("antarchy:stink_bug", RogueKit.memory("antarchy/stink_bug", "Stink Bug")
                .active("Stench", RogueKit.nova(6.0D, 4.0D, particle("stinky_gas", ParticleTypes.SNEEZE), SoundEvents.SLIME_SQUISH, 8.0D,
                        RogueKit.apply("antarchy:stinky", 200, 0).and(RogueKit.apply(MobEffects.CONFUSION, 160, 0)).and(RogueKit.untarget())))
                .build());
        RogueMemory mantis = RogueKit.memory("antarchy/mantis", "Mantis")
                .active("Scythe Slash", RogueKit.cone(4.5D, 70.0D, 20.0D, ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_SWEEP, 3.0D,
                        RogueKit.apply("antarchy:bleeding", 100, 0)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.MOVEMENT_SPEED, 0)))
                .build();
        RogueMemories.reg("antarchy:mantis", mantis);
        RogueMemories.reg("antarchy:alpha_mantis", RogueKit.memory("antarchy/alpha_mantis", "Alpha Mantis")
                .active("Alpha Scythes", RogueKit.cone(6.0D, 90.0D, 34.0D, ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_SWEEP, 4.0D,
                        RogueKit.apply("antarchy:bleeding", 160, 1).and(RogueKit.knock(0.8D))))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.MOVEMENT_SPEED, 1), RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)))
                .build());
        RogueMemories.reg("antarchy:wasp", RogueKit.memory("antarchy/wasp", "Wasp")
                .active("Sting Dive", RogueKit.dash(1.7D, 8, 14.0D, 0.4D, ParticleTypes.CRIT, SoundEvents.BEE_LOOP_AGGRESSIVE, 3.0D))
                .passives(RogueKit.onHit(RogueKit.apply(MobEffects.POISON, 80, 0)), RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0)))
                .build());
        RogueMemories.reg("antarchy:bomber", RogueKit.memory("antarchy/bomber", "Bomber")
                .active("Carpet Bomb", player -> {
                    Vec3 look = player.getLookAngle();
                    Vec3 flat = new Vec3(look.x, 0.0D, look.z).normalize();
                    for (int i = 1; i <= 4; i++) {
                        RogueKit.explodeAt(player, player.position().add(flat.scale(i * 3.0D)), 2.0F, 18.0D, false);
                    }
                    return 10.0D;
                })
                .passive(RogueKit.noFall())
                .build());
        RogueMemories.reg("antarchy:octopus_bomb", RogueKit.memory("antarchy/octopus_bomb", "Octopus Bomb")
                .active("Octo-Bomb", player -> {
                    ServerLevel level = player.serverLevel();
                    Entity entity = RogueKit.entity("antarchy:octopus_bomb").create(player, level);
                    if (entity instanceof OctopusBombEntity bomb) {
                        Vec3 eye = player.getEyePosition().add(player.getLookAngle());
                        bomb.moveTo(eye.x, eye.y, eye.z, player.getYRot(), 0.0F);
                        level.addFreshEntity(bomb);
                        bomb.launchAsExplosiveProjectile(player, player.getLookAngle().scale(1.4D));
                    }
                    HeroFx.play(player, SoundEvents.SQUID_SQUIRT, 1.0F, 0.8F);
                    return 6.0D;
                })
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0)))
                .build());
        RogueMemories.reg("antarchy:missile_squid", RogueKit.memory("antarchy/missile_squid", "Missile Squid")
                .active("Ink Missile", RogueKit.shoot((player, level) -> new LargeFireball(level, player, Vec3.ZERO, 1), 1.6D, 1, 0.0F, 5.0D, SoundEvents.SQUID_SQUIRT))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0)))
                .build());
        RogueMemories.reg("antarchy:stratoshark", RogueKit.memory("antarchy/stratoshark", "Stratoshark")
                .active("Shark Dive", RogueKit.dash(2.0D, 10, 26.0D, 1.0D, ParticleTypes.BUBBLE, SoundEvents.DOLPHIN_ATTACK, 5.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DOLPHINS_GRACE, 0), RogueKit.fx(MobEffects.WATER_BREATHING, 0)))
                .build());
        RogueMemories.reg("antarchy:bed_bug", RogueKit.memory("antarchy/bed_bug", "Bed Bug")
                .passive(RogueKit.onHit((player, target) -> player.heal(1.5F)))
                .build());
        RogueMemory ants = RogueKit.memory("antarchy/ant", "Ant Colony")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DIG_SPEED, 1), RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)))
                .build();
        for (String id : List.of("antarchy:red_ant", "antarchy:brown_ant", "antarchy:rainbow_ant", "antarchy:termite")) {
            RogueMemories.reg(id, ants);
        }
        RogueMemories.reg("antarchy:moleworm", RogueKit.memory("antarchy/moleworm", "Moleworm")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DIG_SPEED, 2)))
                .build());
        RogueMemories.reg("antarchy:flytrap", RogueKit.memory("antarchy/flytrap", "Flytrap")
                .active("Snap", RogueKit.beam(8.0D, 18.0D, 1, ParticleTypes.COMPOSTER, SoundEvents.GENERIC_EAT, 3.0D, RogueKit.pull(1.2D)))
                .build());
        RogueMemories.reg("antarchy:crawling_blight", RogueKit.memory("antarchy/crawling_blight", "Crawling Blight")
                .active("Blight", RogueKit.nova(5.0D, 8.0D, ParticleTypes.MYCELIUM, SoundEvents.SCULK_BLOCK_SPREAD, 8.0D,
                        RogueKit.apply(MobEffects.WITHER, 100, 0).and(RogueKit.apply(MobEffects.POISON, 100, 0))))
                .build());
        RogueMemories.reg("antarchy:skulking_fright", RogueKit.memory("antarchy/skulking_fright", "Skulking Fright")
                .active("Fright", RogueKit.nova(8.0D, 6.0D, ParticleTypes.SCULK_SOUL, SoundEvents.WARDEN_NEARBY_CLOSER, 10.0D,
                        RogueKit.apply(MobEffects.BLINDNESS, 120, 0).and(RogueKit.apply(MobEffects.DARKNESS, 160, 0)).and(RogueKit.untarget())))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.NIGHT_VISION, 0)))
                .build());
        RogueMemories.reg("antarchy:springbug", RogueKit.memory("antarchy/springbug", "Springbug")
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.JUMP, 3)), RogueKit.noFall())
                .build());
        RogueMemories.reg("antarchy:rolly_polly", RogueKit.memory("antarchy/rolly_polly", "Rolly Polly")
                .active("Roll", RogueKit.dash(1.6D, 12, 12.0D, 1.5D, ParticleTypes.POOF, SoundEvents.ARMADILLO_ROLL, 4.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_RESISTANCE, 1)))
                .build());
        RogueMemory glider = RogueKit.memory("antarchy/glider", "Glider")
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0)), RogueKit.noFall())
                .build();
        for (String id : List.of("antarchy:flying_squirrel", "antarchy:butterfly", "antarchy:caterpillar", "antarchy:reverie")) {
            RogueMemories.reg(id, glider);
        }
        RogueMemories.reg("antarchy:glimmer", RogueKit.memory("antarchy/glimmer", "Glimmer")
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.NIGHT_VISION, 0)), RogueKit.trail(particle("firefly", ParticleTypes.GLOW), 4))
                .build());
        RogueMemory runner = RogueKit.memory("antarchy/runner", "Swift Hooves")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.MOVEMENT_SPEED, 1), RogueKit.fx(MobEffects.JUMP, 1)))
                .build();
        for (String id : List.of("antarchy:ouranwood_deer", "antarchy:elka", "antarchy:easter_bunny")) {
            RogueMemories.reg(id, runner);
        }
        RogueMemories.reg("antarchy:dorrie", RogueKit.memory("antarchy/dorrie", "Dorrie")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.DOLPHINS_GRACE, 0),
                        RogueKit.fx(MobEffects.DAMAGE_RESISTANCE, 0)))
                .build());
        RogueMemories.reg("antarchy:cheep", RogueKit.memory("antarchy/cheep", "Cheep")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.DOLPHINS_GRACE, 0)))
                .build());
        RogueMemories.reg("antarchy:dr_trayaurus", RogueKit.memory("antarchy/dr_trayaurus", "Dr. Trayaurus")
                .active("Experimental Brew", RogueKit.buff(30.0D, SoundEvents.BREWING_STAND_BREW,
                        new MobEffectInstance(MobEffects.REGENERATION, 200, 1), new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1),
                        new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0)))
                .build());
    }
}
