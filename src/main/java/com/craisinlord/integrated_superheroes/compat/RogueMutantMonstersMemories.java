package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.hero.HeroShockwaves;
import com.craisinlord.integrated_superheroes.hero.RogueKit;
import com.craisinlord.integrated_superheroes.hero.RogueMemories;
import fuzs.mutantmonsters.world.entity.projectile.MutantArrow;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Snowball;

public final class RogueMutantMonstersMemories {
    private RogueMutantMonstersMemories() {
    }

    public static void register() {
        RogueMemories.reg("mutantmonsters:mutant_creeper", RogueKit.memory("mutantmonsters/mutant_creeper", "Mutant Creeper")
                .active("Mutated Blast", player -> {
                    RogueKit.explodeAt(player, player.position().add(0.0D, 0.5D, 0.0D), 5.0F, 60.0D, false);
                    HeroFx.shake(player.serverLevel(), player.position(), 24.0D, 0.8F, 14);
                    return 18.0D;
                })
                .passives(RogueKit.immune(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION), RogueKit.noFall(), RogueKit.effects(RogueKit.fx(MobEffects.JUMP, 2)))
                .build());
        RogueMemories.reg("mutantmonsters:mutant_zombie", RogueKit.memory("mutantmonsters/mutant_zombie", "Mutant Zombie")
                .active("Seismic Slam", player -> {
                    HeroShockwaves.ring(player, 10, 9, RogueKit.dmg(30.0D));
                    HeroFx.play(player, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.5F, 0.6F);
                    HeroFx.shake(player.serverLevel(), player.position(), 20.0D, 0.7F, 12);
                    return 10.0D;
                })
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 1)), RogueKit.resist(0.8D, event -> true))
                .build());
        RogueMemories.reg("mutantmonsters:mutant_skeleton", RogueKit.memory("mutantmonsters/mutant_skeleton", "Mutant Skeleton")
                .active("Mutant Arrows", RogueKit.shoot((player, level) -> {
                    MutantArrow arrow = new MutantArrow(level, player);
                    arrow.setClones(4);
                    arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                    arrow.setBaseDamage(RogueKit.dmg(6.0D));
                    return arrow;
                }, 3.2D, 1, 0.0F, 4.0D, SoundEvents.SKELETON_SHOOT))
                .passive(RogueKit.resist(0.85D, event -> true))
                .build());
        RogueMemories.reg("mutantmonsters:mutant_enderman", RogueKit.memory("mutantmonsters/mutant_enderman", "Mutant Enderman")
                .active("Mutant Scream", player -> {
                    double cooldown = RogueKit.nova(12.0D, 14.0D, HeroFx.particle("mutantmonsters:endersoul", ParticleTypes.PORTAL), SoundEvents.ENDERMAN_SCREAM, 14.0D,
                            RogueKit.apply(MobEffects.CONFUSION, 160, 0).and(RogueKit.apply(MobEffects.BLINDNESS, 100, 0))
                                    .and(RogueKit.apply(MobEffects.WEAKNESS, 160, 1)).and(RogueKit.stun(40))).use(player);
                    HeroFx.shake(player.serverLevel(), player.position(), 16.0D, 0.6F, 20);
                    return cooldown;
                })
                .passives(RogueKit.waterHurts(1.0D), RogueKit.trail(HeroFx.particle("mutantmonsters:endersoul", ParticleTypes.PORTAL), 3))
                .build());
        RogueMemories.reg("mutantmonsters:mutant_snow_golem", RogueKit.memory("mutantmonsters/mutant_snow_golem", "Mutant Snow Golem")
                .active("Ice Barrage", RogueKit.shoot((player, level) -> new Snowball(level, player), 2.0D, 9, 14.0F, 3.0D, SoundEvents.SNOW_GOLEM_SHOOT))
                .passives(RogueKit.immune(net.minecraft.tags.DamageTypeTags.IS_FREEZING), RogueKit.onHit(RogueKit.freeze(80)))
                .build());
        RogueMemories.reg("mutantmonsters:spider_pig", RogueKit.memory("mutantmonsters/spider_pig", "Spider Pig")
                .active("Web Shot", RogueKit.web(16.0D, 120, 4.0D))
                .passives(RogueKit.climb(), RogueKit.effects(RogueKit.fx(MobEffects.JUMP, 1)), RogueKit.noFall())
                .build());
        RogueMemories.reg("mutantmonsters:creeper_minion", RogueKit.memory("mutantmonsters/creeper_minion", "Creeper Minion")
                .active("Mini Detonate", RogueKit.detonate(2.0F, 16.0D, false, 6.0D))
                .build());
    }
}
