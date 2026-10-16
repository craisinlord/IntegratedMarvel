package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.hero.MarvelBalanceConfig;
import com.craisinlord.integrated_superheroes.hero.RogueKit;
import com.craisinlord.integrated_superheroes.hero.RogueMemories;
import com.iafenvoy.iceandfire.entity.StoneStatueEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class RogueIceAndFireMemories {
    private RogueIceAndFireMemories() {
    }

    private static RogueKit.Hit petrify() {
        return (player, target) -> {
            if (!MarvelBalanceConfig.PETRIFY_MOBS.get() || target instanceof Player || !(target instanceof Mob mob) || !HeroAbilities.canControl(target)
                    || mob.hasCustomName() || !(player.level() instanceof ServerLevel level)) {
                RogueKit.stun(80).hit(player, target);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 4), player);
                return;
            }
            StoneStatueEntity statue = StoneStatueEntity.buildStatueEntity(target);
            statue.absMoveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            statue.yBodyRot = target.yBodyRot;
            level.addFreshEntity(statue);
            level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 20,
                    target.getBbWidth() * 0.5D, target.getBbHeight() * 0.5D, target.getBbWidth() * 0.5D, 0.02D);
            target.remove(Entity.RemovalReason.KILLED);
        };
    }

    private static RogueMemory dragon(String path, String name, String ability, RogueKit.Spawner charge, RogueMemory.Passive passive) {
        return RogueKit.memory("iceandfire/" + path, name)
                .active(ability, RogueKit.shoot(charge, 1.6D, 1, 0.0F, 3.0D, SoundEvents.ENDER_DRAGON_SHOOT))
                .passives(passive, RogueKit.noFall(), RogueKit.resist(0.8D, event -> true))
                .build();
    }

    public static void register() {
        RogueMemories.reg("iceandfire:fire_dragon", dragon("fire_dragon", "Fire Dragon", "Fire Charge", (player, level) -> IceAndFireCharges.fire(level),
                RogueMemory.Passive.all(RogueKit.immune(DamageTypeTags.IS_FIRE), RogueKit.effects(RogueKit.fx(MobEffects.FIRE_RESISTANCE, 0)),
                        RogueKit.onHit(RogueKit.ignite(4.0F)))));
        RogueMemory ice = dragon("ice_dragon", "Ice Dragon", "Ice Charge", (player, level) -> IceAndFireCharges.ice(level),
                RogueMemory.Passive.all(RogueKit.immune(DamageTypeTags.IS_FREEZING), RogueKit.onHit((player, target) -> {
                    if (HeroAbilities.canControl(target)) {
                        target.addEffect(new MobEffectInstance(IceAndFireCharges.frozen(), 60, 0), player);
                    }
                })));
        RogueMemories.reg("iceandfire:ice_dragon", ice);
        RogueMemories.reg("iceandfire:black_frost_dragon", ice);
        RogueMemories.reg("iceandfire:lightning_dragon", dragon("lightning_dragon", "Lightning Dragon", "Lightning Charge",
                (player, level) -> IceAndFireCharges.lightning(level), RogueKit.immune(DamageTypes.LIGHTNING_BOLT)));
        RogueMemories.reg("iceandfire:gorgon", RogueKit.memory("iceandfire/gorgon", "Gorgon")
                .active("Petrifying Gaze", RogueKit.cone(16.0D, 20.0D, 0.0D, ParticleTypes.WHITE_ASH, SoundEvents.ELDER_GUARDIAN_CURSE, 15.0D, petrify()))
                .build());
        RogueMemories.reg("iceandfire:cockatrice", RogueKit.memory("iceandfire/cockatrice", "Cockatrice")
                .active("Withering Gaze", RogueKit.cone(14.0D, 20.0D, 8.0D, ParticleTypes.WITCH, SoundEvents.CHICKEN_HURT, 8.0D,
                        RogueKit.apply(MobEffects.WITHER, 160, 1).and(RogueKit.stun(40))))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0)))
                .build());
        RogueMemories.reg("iceandfire:cyclops", RogueKit.memory("iceandfire/cyclops", "Cyclops")
                .active("Giant's Grab", RogueKit.toss(5.0D, 1.6D, 34.0D, 6.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 1)))
                .build());
        RogueMemories.reg("iceandfire:deathworm", RogueKit.memory("iceandfire/deathworm", "Death Worm")
                .active("Sand Burst", RogueKit.leap(1.4D, 0.6D, 5.0D, 26.0D, ParticleTypes.POOF,
                        SoundEvents.SAND_BREAK, 8.0D, RogueKit.launch(0.6D)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DIG_SPEED, 2)))
                .build());
        RogueMemories.reg("iceandfire:hydra", RogueKit.memory("iceandfire/hydra", "Hydra")
                .active("Hydra Breath", RogueKit.cone(10.0D, 35.0D, 16.0D, ParticleTypes.SNEEZE, SoundEvents.HOGLIN_ANGRY, 4.0D,
                        RogueKit.apply(MobEffects.POISON, 160, 1)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.REGENERATION, 1)))
                .build());
        RogueMemories.reg("iceandfire:sea_serpent", RogueKit.memory("iceandfire/sea_serpent", "Sea Serpent")
                .active("Bubble Breath", RogueKit.cone(12.0D, 30.0D, 22.0D, ParticleTypes.BUBBLE, SoundEvents.ELDER_GUARDIAN_AMBIENT, 5.0D, RogueKit.knock(1.5D)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.DOLPHINS_GRACE, 0)))
                .build());
        RogueMemories.reg("iceandfire:siren", RogueKit.memory("iceandfire/siren", "Siren")
                .active("Siren Song", RogueKit.nova(12.0D, 0.0D, ParticleTypes.NOTE, SoundEvents.NOTE_BLOCK_CHIME.value(), 12.0D,
                        RogueKit.untarget().and(RogueKit.stun(60)).and((player, target) -> {
                            if (target instanceof Player) {
                                target.addEffect(new MobEffectInstance(IceAndFireCharges.sirenCharm(), 100, 0), player);
                            }
                        })))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0)))
                .build());
        RogueMemories.reg("iceandfire:stymphalian_bird", RogueKit.memory("iceandfire/stymphalian_bird", "Stymphalian Bird")
                .active("Feather Volley", RogueKit.shoot(RogueKit.entity("iceandfire:stymphalian_feather"), 2.2D, 5, 15.0F, 3.0D, SoundEvents.PARROT_FLY))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0)))
                .build());
        RogueMemories.reg("iceandfire:troll", RogueKit.memory("iceandfire/troll", "Troll")
                .active("Troll Smash", RogueKit.leap(0.9D, 0.4D, 5.0D, 34.0D, ParticleTypes.CLOUD, SoundEvents.RAVAGER_STEP, 8.0D, RogueKit.stun(30)))
                .passives(RogueKit.resist(0.8D, event -> true), RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)))
                .build());
        RogueMemories.reg("iceandfire:dread_lich", RogueKit.memory("iceandfire/dread_lich", "Dread Lich")
                .active("Lich Skulls", RogueKit.shoot(RogueKit.entity("iceandfire:dread_lich_skull"), 1.4D, 3, 10.0F, 4.0D, SoundEvents.EVOKER_CAST_SPELL))
                .passive(RogueKit.immune(DamageTypes.WITHER))
                .build());
        RogueMemory dread = RogueKit.memory("iceandfire/dread", "Dread")
                .passives(RogueKit.onHit(RogueKit.apply(MobEffects.WITHER, 60, 0)), RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)),
                        RogueKit.immune(DamageTypeTags.IS_FREEZING))
                .build();
        for (String id : List.of("iceandfire:dread_knight", "iceandfire:dread_ghoul", "iceandfire:dread_thrall", "iceandfire:dread_scuttler",
                "iceandfire:dread_beast", "iceandfire:dread_horse")) {
            RogueMemories.reg(id, dread);
        }
        RogueMemories.reg("iceandfire:dread_queen", RogueKit.memory("iceandfire/dread_queen", "Dread Queen")
                .active("Dread Command", RogueKit.roar(10.0D, 24.0D, 60, 1.5D, ParticleTypes.SNOWFLAKE, SoundEvents.WITHER_AMBIENT, 15.0D))
                .passives(RogueKit.immune(DamageTypeTags.IS_FREEZING), RogueKit.resist(0.75D, event -> true))
                .build());
        RogueMemories.reg("iceandfire:ghost", RogueKit.memory("iceandfire/ghost", "Ghost")
                .active("Haunt", player -> {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 160, 0));
                    return RogueKit.nova(8.0D, 8.0D, ParticleTypes.SOUL, SoundEvents.SOUL_ESCAPE.value(), 10.0D,
                            RogueKit.apply(MobEffects.BLINDNESS, 100, 0).and(RogueKit.untarget())).use(player);
                })
                .passive(RogueKit.noFall())
                .build());
        RogueMemories.reg("iceandfire:pixie", RogueKit.memory("iceandfire/pixie", "Pixie")
                .active("Pixie Charge", RogueKit.shoot(RogueKit.entity("iceandfire:pixie_charge"), 1.4D, 3, 8.0F, 2.0D, SoundEvents.AMETHYST_BLOCK_CHIME))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0)))
                .build());
        RogueMemories.reg("iceandfire:amphithere", RogueKit.memory("iceandfire/amphithere", "Amphithere")
                .active("Wing Gust", RogueKit.cone(10.0D, 45.0D, 8.0D, ParticleTypes.CLOUD, SoundEvents.ENDER_DRAGON_FLAP, 5.0D, RogueKit.knock(2.0D)))
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0)), RogueKit.noFall())
                .build());
        RogueMemories.reg("iceandfire:hippogryph", RogueKit.memory("iceandfire/hippogryph", "Hippogryph")
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.MOVEMENT_SPEED, 1), RogueKit.fx(MobEffects.JUMP, 1), RogueKit.fx(MobEffects.SLOW_FALLING, 0)),
                        RogueKit.noFall())
                .build());
        RogueMemories.reg("iceandfire:hippocampus", RogueKit.memory("iceandfire/hippocampus", "Hippocampus")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.DOLPHINS_GRACE, 0)))
                .build());
    }
}
