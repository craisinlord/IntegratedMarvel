package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.hero.RogueKit;
import com.craisinlord.integrated_superheroes.hero.RogueMemories;
import com.github.alexmodguy.alexscaves.server.entity.item.GumballEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.NuclearExplosionEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.SugarStaffHexEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.TephraEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.WaveEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RogueAlexsCavesMemories {
    private RogueAlexsCavesMemories() {
    }

    private static ParticleOptions particle(String name, ParticleOptions fallback) {
        return HeroFx.particle("alexscaves:" + name, fallback);
    }

    private static RogueKit.Hit irradiate(int ticks, int amplifier) {
        return RogueKit.apply("alexscaves:irradiated", ticks, amplifier);
    }

    private static RogueMemory.Passive cleanse(String effectId) {
        return new RogueMemory.Passive() {
            @Override
            public void tick(ServerPlayer player) {
                if (player.tickCount % 10 == 0) {
                    RogueKit.effect(effectId).ifPresent(player::removeEffect);
                }
            }
        };
    }

    private static Vec3 aimPoint(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        return player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                .getLocation();
    }

    public static void register() {
        RogueMemories.reg("alexscaves:teletor", RogueKit.memory("alexscaves/teletor", "Teletor")
                .active("Magnet Pull", RogueKit.nova(10.0D, 4.0D, particle("azure_magnetic_flow", ParticleTypes.ENCHANT), SoundEvents.BEACON_POWER_SELECT, 5.0D,
                        RogueKit.pull(1.2D).and(RogueKit.apply("alexscaves:magnetizing", 100, 0))))
                .build());
        RogueMemories.reg("alexscaves:magnetron", RogueKit.memory("alexscaves/magnetron", "Magnetron")
                .active("Magnetic Repulse", RogueKit.nova(8.0D, 12.0D, particle("scarlet_magnetic_flow", ParticleTypes.CRIT), SoundEvents.BEACON_DEACTIVATE, 6.0D,
                        RogueKit.knock(2.2D).and(RogueKit.launch(0.6D))))
                .passive(RogueKit.resist(0.8D, event -> true))
                .build());
        RogueMemories.reg("alexscaves:boundroid", RogueKit.memory("alexscaves/boundroid", "Boundroid")
                .active("Winch Slam", RogueKit.leap(1.3D, 0.3D, 4.5D, 26.0D, particle("big_block_dust", ParticleTypes.CLOUD), SoundEvents.ANVIL_LAND, 6.0D,
                        RogueKit.stun(30)))
                .passive(RogueKit.noFall())
                .build());
        RogueMemories.reg("alexscaves:quarry_smasher", RogueKit.memory("alexscaves/quarry_smasher", "Quarry Smasher")
                .active("Quarry Smash", RogueKit.leap(1.1D, 0.2D, 4.0D, 30.0D, particle("big_block_dust", ParticleTypes.CLOUD), SoundEvents.ANVIL_LAND, 8.0D,
                        RogueKit.stun(30)))
                .passives(RogueKit.noFall(), RogueKit.effects(RogueKit.fx(MobEffects.DIG_SPEED, 2)))
                .build());
        RogueMemories.reg("alexscaves:ferrouslime", RogueKit.memory("alexscaves/ferrouslime", "Ferrouslime")
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_RESISTANCE, 1)), RogueKit.trail(particle("ferrouslime", ParticleTypes.ITEM_SLIME), 6))
                .build());
        RogueMemories.reg("alexscaves:notor", RogueKit.memory("alexscaves/notor", "Notor")
                .active("Scan", RogueKit.nova(24.0D, 0.0D, ParticleTypes.END_ROD, SoundEvents.BEACON_AMBIENT, 8.0D, RogueKit.apply(MobEffects.GLOWING, 240, 0)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0)))
                .build());
        RogueMemories.reg("alexscaves:subterranodon", RogueKit.memory("alexscaves/subterranodon", "Subterranodon")
                .active("Dive", RogueKit.dash(1.8D, 10, 18.0D, 0.8D, ParticleTypes.CLOUD, SoundEvents.PHANTOM_SWOOP, 4.0D))
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0)), RogueKit.noFall())
                .build());
        RogueMemories.reg("alexscaves:vallumraptor", RogueKit.memory("alexscaves/vallumraptor", "Vallumraptor")
                .active("Raptor Pounce", RogueKit.leap(0.7D, 1.5D, 2.5D, 22.0D, ParticleTypes.CRIT, SoundEvents.PLAYER_ATTACK_SWEEP, 3.0D,
                        RogueKit.apply(MobEffects.WEAKNESS, 80, 0)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.MOVEMENT_SPEED, 1)))
                .build());
        RogueMemories.reg("alexscaves:grottoceratops", RogueKit.memory("alexscaves/grottoceratops", "Grottoceratops")
                .active("Horn Charge", RogueKit.dash(1.8D, 12, 30.0D, 2.2D, ParticleTypes.POOF, SoundEvents.RAVAGER_STEP, 6.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_RESISTANCE, 0)))
                .build());
        RogueMemories.reg("alexscaves:tremorsaurus", RogueKit.memory("alexscaves/tremorsaurus", "Tremorsaurus")
                .active("Tremor Roar", player -> {
                    double cooldown = RogueKit.roar(9.0D, 12.0D, 60, 1.6D, particle("big_block_dust", ParticleTypes.CLOUD), SoundEvents.RAVAGER_ROAR, 12.0D).use(player);
                    HeroFx.shake(player.serverLevel(), player.position(), 24.0D, 0.8F, 20);
                    return cooldown;
                })
                .passive(RogueKit.bonus(1.2D))
                .build());
        RogueMemories.reg("alexscaves:relicheirus", RogueKit.memory("alexscaves/relicheirus", "Relicheirus")
                .active("Great Claw", RogueKit.cone(5.5D, 60.0D, 26.0D, ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_SWEEP, 4.0D, RogueKit.knock(1.2D)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)))
                .build());
        RogueMemories.reg("alexscaves:luxtructosaurus", RogueKit.memory("alexscaves/luxtructosaurus", "Luxtructosaurus")
                .active("Tephra Volley", player -> {
                    ServerLevel level = player.serverLevel();
                    for (int i = 0; i < 3; i++) {
                        TephraEntity tephra = new TephraEntity(level, player);
                        tephra.setMaxScale(1.0F + player.getRandom().nextFloat() * 0.6F);
                        Vec3 direction = Vec3.directionFromRotation(player.getXRot() - 20.0F, player.getYRot() - 10.0F + i * 10.0F);
                        RogueKit.fire(player, level, tephra, direction, 1.2D);
                    }
                    HeroFx.play(player, SoundEvents.BLAZE_SHOOT, 1.5F, 0.5F);
                    return 8.0D;
                })
                .passives(RogueKit.immune(net.minecraft.tags.DamageTypeTags.IS_FIRE), RogueKit.effects(RogueKit.fx(MobEffects.FIRE_RESISTANCE, 0)))
                .build());
        RogueMemories.reg("alexscaves:atlatitan", RogueKit.memory("alexscaves/atlatitan", "Atlatitan")
                .active("Titan Stomp", RogueKit.leap(1.0D, 0.3D, 7.0D, 40.0D, particle("big_block_dust", ParticleTypes.CLOUD), SoundEvents.RAVAGER_STEP, 14.0D,
                        RogueKit.stun(40).and(RogueKit.launch(0.7D))))
                .passive(RogueKit.resist(0.7D, event -> true))
                .build());
        RogueMemories.reg("alexscaves:nucleeper", RogueKit.memory("alexscaves/nucleeper", "Nucleeper")
                .active("Mini Nuke", player -> {
                    ServerLevel level = player.serverLevel();
                    Vec3 at = aimPoint(player, 12.0D);
                    Entity entity = RogueKit.entity("alexscaves:nuclear_explosion").create(player, level);
                    if (entity instanceof NuclearExplosionEntity explosion) {
                        explosion.setSize(0.6F);
                        explosion.setNoGriefing(!RogueKit.griefing(level));
                        explosion.moveTo(at.x, at.y, at.z);
                        level.addFreshEntity(explosion);
                    }
                    RogueKit.novaAt(player, at, 6.0D, 40.0D, particle("fallout", ParticleTypes.ASH), irradiate(400, 1));
                    return 60.0D;
                })
                .passive(cleanse("alexscaves:irradiated"))
                .build());
        RogueMemories.reg("alexscaves:radgill", RogueKit.memory("alexscaves/radgill", "Radgill")
                .passives(RogueKit.onHit(irradiate(160, 0)), RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0)), cleanse("alexscaves:irradiated"))
                .build());
        RogueMemories.reg("alexscaves:brainiac", RogueKit.memory("alexscaves/brainiac", "Brainiac")
                .active("Waste Drum", RogueKit.shoot(RogueKit.entity("alexscaves:thrown_waste_drum"), 1.1D, 1, 0.0F, 6.0D, SoundEvents.ANVIL_PLACE))
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)), cleanse("alexscaves:irradiated"))
                .build());
        RogueMemories.reg("alexscaves:gammaroach", RogueKit.memory("alexscaves/gammaroach", "Gammaroach")
                .active("Toxic Cloud", RogueKit.nova(5.0D, 4.0D, particle("gammaroach", ParticleTypes.SNEEZE), SoundEvents.SLIME_SQUISH, 8.0D,
                        irradiate(240, 0).and(RogueKit.apply(MobEffects.POISON, 100, 0))))
                .passive(cleanse("alexscaves:irradiated"))
                .build());
        RogueMemories.reg("alexscaves:raycat", RogueKit.memory("alexscaves/raycat", "Raycat")
                .active("Gamma Ray", RogueKit.beam(16.0D, 22.0D, 3, particle("proton", ParticleTypes.HAPPY_VILLAGER), SoundEvents.BEACON_POWER_SELECT, 4.0D,
                        irradiate(200, 0)))
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.NIGHT_VISION, 0)), cleanse("alexscaves:irradiated"))
                .build());
        RogueMemories.reg("alexscaves:tremorzilla", RogueKit.memory("alexscaves/tremorzilla", "Tremorzilla")
                .active("Atomic Breath", player -> {
                    double cooldown = RogueKit.beam(36.0D, 60.0D, 8, particle("tremorzilla_proton", ParticleTypes.HAPPY_VILLAGER), SoundEvents.WARDEN_SONIC_BOOM,
                            20.0D, irradiate(400, 2).and(RogueKit.ignite(5.0F))).use(player);
                    HeroFx.shake(player.serverLevel(), player.position(), 32.0D, 1.0F, 30);
                    return cooldown;
                })
                .passives(RogueKit.resist(0.6D, event -> true), cleanse("alexscaves:irradiated"))
                .build());
        RogueMemory aquatic = RogueKit.memory("alexscaves/abyssal", "Abyssal Creature")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.NIGHT_VISION, 0)))
                .build();
        for (String id : List.of("alexscaves:lanternfish", "alexscaves:tripodfish", "alexscaves:gossamer_worm", "alexscaves:trilocaris")) {
            RogueMemories.reg(id, aquatic);
        }
        RogueMemories.reg("alexscaves:sea_pig", RogueKit.memory("alexscaves/sea_pig", "Sea Pig")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.REGENERATION, 0)))
                .build());
        RogueMemories.reg("alexscaves:hullbreaker", RogueKit.memory("alexscaves/hullbreaker", "Hullbreaker")
                .active("Hull Breaker", RogueKit.dash(2.2D, 12, 44.0D, 2.5D, ParticleTypes.BUBBLE_COLUMN_UP, SoundEvents.ELDER_GUARDIAN_AMBIENT, 10.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.DOLPHINS_GRACE, 0),
                        RogueKit.fx(MobEffects.DAMAGE_RESISTANCE, 0)))
                .build());
        RogueMemories.reg("alexscaves:deep_one", RogueKit.memory("alexscaves/deep_one", "Deep One")
                .active("Deep Strike", RogueKit.dash(1.6D, 8, 20.0D, 1.0D, ParticleTypes.BUBBLE, SoundEvents.DROWNED_AMBIENT_WATER, 4.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)))
                .build());
        RogueMemories.reg("alexscaves:deep_one_knight", RogueKit.memory("alexscaves/deep_one_knight", "Deep One Knight")
                .active("Knight's Cleave", RogueKit.cone(5.0D, 60.0D, 26.0D, ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_SWEEP, 4.0D, RogueKit.knock(1.0D)))
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.DAMAGE_RESISTANCE, 0)), RogueKit.bonus(1.2D))
                .build());
        RogueMemories.reg("alexscaves:deep_one_mage", RogueKit.memory("alexscaves/deep_one_mage", "Deep One Mage")
                .active("Tidal Wave", player -> {
                    ServerLevel level = player.serverLevel();
                    Vec3 look = player.getLookAngle();
                    Vec3 flat = new Vec3(look.x, 0.0D, look.z).normalize();
                    for (int i = -1; i <= 1; i++) {
                        WaveEntity wave = new WaveEntity(level, player);
                        Vec3 side = new Vec3(-flat.z, 0.0D, flat.x).scale(i * 1.5D);
                        Vec3 at = player.position().add(flat.scale(1.5D)).add(side);
                        wave.moveTo(at.x, player.getY(), at.z, player.getYRot(), 0.0F);
                        wave.setOwner(player);
                        wave.setLifespan(30);
                        wave.setWaveScale(1.0F);
                        wave.setDeltaMovement(flat.scale(0.6D));
                        level.addFreshEntity(wave);
                    }
                    RogueKit.shoot(RogueKit.entity("alexscaves:water_bolt"), 1.2D, 1, 0.0F, 0.0D, null).use(player);
                    HeroFx.play(player, SoundEvents.GENERIC_SPLASH, 1.5F, 0.8F);
                    return 6.0D;
                })
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.DOLPHINS_GRACE, 0)))
                .build());
        RogueMemories.reg("alexscaves:mine_guardian", RogueKit.memory("alexscaves/mine_guardian", "Mine Guardian")
                .active("Mine Blast", RogueKit.detonate(4.0F, 36.0D, false, 12.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0)))
                .build());
        RogueMemories.reg("alexscaves:gloomoth", RogueKit.memory("alexscaves/gloomoth", "Gloomoth")
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.NIGHT_VISION, 0), RogueKit.fx(MobEffects.SLOW_FALLING, 0)),
                        RogueKit.trail(particle("moth_dust", ParticleTypes.WHITE_ASH), 5))
                .build());
        RogueMemories.reg("alexscaves:underzealot", RogueKit.memory("alexscaves/underzealot", "Underzealot")
                .active("Darkness Incarnate", player -> {
                    RogueKit.effect("alexscaves:darkness_incarnate").ifPresent(effect -> player.addEffect(new MobEffectInstance(effect, 300, 0)));
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0));
                    RogueKit.novaAt(player, player.position().add(0.0D, 0.5D, 0.0D), 6.0D, 0.0D, particle("underzealot_magic", ParticleTypes.SQUID_INK),
                            RogueKit.apply(MobEffects.DARKNESS, 160, 0).and(RogueKit.apply(MobEffects.BLINDNESS, 80, 0)).and(RogueKit.untarget()));
                    HeroFx.play(player, SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.0F, 0.6F);
                    return 25.0D;
                })
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.NIGHT_VISION, 0)))
                .build());
        RogueMemories.reg("alexscaves:watcher", RogueKit.memory("alexscaves/watcher", "Watcher")
                .active("Watcher's Gaze", RogueKit.cone(20.0D, 15.0D, 10.0D, particle("watcher_appearance", ParticleTypes.SCULK_SOUL), SoundEvents.WARDEN_AGITATED,
                        10.0D, RogueKit.apply(MobEffects.BLINDNESS, 120, 0).and(RogueKit.apply(MobEffects.DARKNESS, 160, 0)).and(RogueKit.stun(40))))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.NIGHT_VISION, 0)))
                .build());
        RogueMemories.reg("alexscaves:corrodent", RogueKit.memory("alexscaves/corrodent", "Corrodent")
                .active("Burrow Strike", RogueKit.dash(1.7D, 10, 22.0D, 1.0D, ParticleTypes.POOF, SoundEvents.ROOTED_DIRT_BREAK, 4.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DIG_SPEED, 2)))
                .build());
        RogueMemories.reg("alexscaves:vesper", RogueKit.memory("alexscaves/vesper", "Vesper")
                .active("Echolocation", RogueKit.nova(28.0D, 0.0D, ParticleTypes.SCULK_CHARGE_POP, SoundEvents.SCULK_CLICKING, 8.0D,
                        RogueKit.apply(MobEffects.GLOWING, 240, 0)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.NIGHT_VISION, 0), RogueKit.fx(MobEffects.SLOW_FALLING, 0)))
                .build());
        RogueMemories.reg("alexscaves:forsaken", RogueKit.memory("alexscaves/forsaken", "Forsaken")
                .active("Sonic Roar", player -> {
                    double cooldown = RogueKit.cone(18.0D, 30.0D, 40.0D, particle("forsaken_sonar", ParticleTypes.SONIC_BOOM), SoundEvents.WARDEN_SONIC_BOOM, 12.0D,
                            RogueKit.knock(2.2D).and(RogueKit.stun(30))).use(player);
                    HeroFx.shake(player.serverLevel(), player.position(), 20.0D, 0.7F, 15);
                    return cooldown;
                })
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.NIGHT_VISION, 0)), RogueKit.resist(0.8D, event -> true))
                .build());
        RogueMemories.reg("alexscaves:sweetish_fish", RogueKit.memory("alexscaves/sweetish_fish", "Sweetish Fish")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx("alexscaves:sugar_rush", 0)))
                .build());
        RogueMemories.reg("alexscaves:caniac", RogueKit.memory("alexscaves/caniac", "Caniac")
                .active("Frenzy", RogueKit.dash(1.8D, 10, 20.0D, 1.0D, ParticleTypes.CRIT, SoundEvents.PLAYER_ATTACK_SWEEP, 3.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.MOVEMENT_SPEED, 1)))
                .build());
        RogueMemories.reg("alexscaves:gumbeeper", RogueKit.memory("alexscaves/gumbeeper", "Gumbeeper")
                .active("Gumball Barrage", RogueKit.shoot((player, level) -> {
                    Entity entity = RogueKit.entity("alexscaves:gumball").create(player, level);
                    if (entity instanceof GumballEntity gumball) {
                        gumball.setExplosive(player.getRandom().nextInt(4) == 0);
                    }
                    return entity;
                }, 1.5D, 5, 12.0F, 3.0D, SoundEvents.CHICKEN_EGG))
                .build());
        RogueMemories.reg("alexscaves:candicorn", RogueKit.memory("alexscaves/candicorn", "Candicorn")
                .active("Candy Charge", RogueKit.dash(1.9D, 12, 28.0D, 2.0D, particle("candicorn_charge", ParticleTypes.HEART), SoundEvents.HORSE_ANGRY, 5.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.MOVEMENT_SPEED, 1), RogueKit.fx(MobEffects.JUMP, 1)))
                .build());
        RogueMemories.reg("alexscaves:gum_worm", RogueKit.memory("alexscaves/gum_worm", "Gum Worm")
                .active("Gum Burrow", RogueKit.leap(1.6D, 0.8D, 5.0D, 32.0D, ParticleTypes.POOF, SoundEvents.ROOTED_DIRT_BREAK, 9.0D, RogueKit.launch(0.8D)))
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.DIG_SPEED, 2)), RogueKit.noFall())
                .build());
        RogueMemories.reg("alexscaves:caramel_cube", RogueKit.memory("alexscaves/caramel_cube", "Caramel Cube")
                .active("Sticky Caramel", RogueKit.nova(5.0D, 6.0D, particle("caramel_drop", ParticleTypes.DRIPPING_HONEY), SoundEvents.HONEY_BLOCK_SLIDE, 8.0D,
                        RogueKit.apply(MobEffects.MOVEMENT_SLOWDOWN, 160, 3)))
                .passives(RogueKit.noFall(), RogueKit.effects(RogueKit.fx(MobEffects.JUMP, 1)))
                .build());
        RogueMemories.reg("alexscaves:gummy_bear", RogueKit.memory("alexscaves/gummy_bear", "Gummy Bear")
                .active("Bear Hug", RogueKit.cone(4.0D, 50.0D, 20.0D, ParticleTypes.HEART, SoundEvents.POLAR_BEAR_WARNING, 4.0D, RogueKit.stun(30)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 0), RogueKit.fx(MobEffects.REGENERATION, 0)))
                .build());
        RogueMemories.reg("alexscaves:licowitch", RogueKit.memory("alexscaves/licowitch", "Licowitch")
                .active("Sugar Hex", player -> {
                    ServerLevel level = player.serverLevel();
                    Vec3 at = aimPoint(player, 20.0D);
                    Entity entity = RogueKit.entity("alexscaves:sugar_staff_hex").create(player, level);
                    if (entity instanceof SugarStaffHexEntity hex) {
                        hex.moveTo(at.x, at.y, at.z);
                        hex.setOwner(player);
                        hex.setHexScale(1.5F);
                        hex.setLifespan(100);
                        level.addFreshEntity(hex);
                    }
                    HeroAbilities.particleLine(level, player.getEyePosition().subtract(0.0D, 0.2D, 0.0D), at,
                            particle("purple_witch_magic", ParticleTypes.WITCH), 0.6D);
                    HeroFx.play(player, SoundEvents.EVOKER_CAST_SPELL, 1.2F, 1.3F);
                    return 10.0D;
                })
                .passive(RogueKit.effects(RogueKit.fx("alexscaves:sugar_rush", 0)))
                .build());
        RogueMemories.reg("alexscaves:gingerbread_man", RogueKit.memory("alexscaves/gingerbread_man", "Gingerbread Man")
                .active("Run, Run!", RogueKit.dash(2.0D, 10, 10.0D, 0.6D, ParticleTypes.CRIT, SoundEvents.ARMOR_EQUIP_LEATHER.value(), 3.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.MOVEMENT_SPEED, 2)))
                .build());
    }
}
