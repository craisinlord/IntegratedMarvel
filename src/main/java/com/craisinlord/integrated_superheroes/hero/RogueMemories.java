package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.api.RogueAbsorption;
import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.RogueAlexsCavesMemories;
import com.craisinlord.integrated_superheroes.compat.RogueAntarchyMemories;
import com.craisinlord.integrated_superheroes.compat.RogueIceAndFireMemories;
import com.craisinlord.integrated_superheroes.compat.RogueMutantMonstersMemories;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteCompat;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class RogueMemories {
    private static boolean registered;
    private static RogueMemory vigor;
    private static RogueMemory healingFactor;
    private static RogueMemory spiderPowers;

    private RogueMemories() {
    }

    public static void reg(String entityId, RogueMemory memory) {
        RogueAbsorption.registerIfAbsent(ResourceLocation.parse(entityId), memory);
    }

    public static void registerAll() {
        if (registered) {
            return;
        }
        registered = true;
        vigor = RogueKit.memory("vigor", "Life Force")
                .active("Stolen Vigor", RogueKit.buff(30.0D, SoundEvents.TOTEM_USE,
                        new MobEffectInstance(MobEffects.ABSORPTION, 600, 1), new MobEffectInstance(MobEffects.REGENERATION, 100, 1)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)))
                .build();
        RogueAbsorption.remember(vigor);
        healingFactor = RogueKit.memory("healing_factor", "Wolverine")
                .active("Berserker Rage", RogueKit.buff(25.0D, SoundEvents.RAVAGER_ROAR,
                        new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1), new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.REGENERATION, 1)))
                .build();
        RogueAbsorption.remember(healingFactor);
        spiderPowers = RogueKit.memory("spider_powers", "Spider-Man")
                .active("Web Shot", RogueKit.web(24.0D, 160, 3.0D))
                .passives(RogueKit.climb(), RogueKit.effects(RogueKit.fx(MobEffects.JUMP, 1)), RogueKit.noFall())
                .build();
        RogueAbsorption.remember(spiderPowers);
        vanilla();
        marvel();
        if (ModCompat.ANTARCHY) {
            RogueAntarchyMemories.register();
        }
        if (ModCompat.ALEXS_CAVES) {
            RogueAlexsCavesMemories.register();
        }
        if (ModCompat.MUTANT_MONSTERS) {
            RogueMutantMonstersMemories.register();
        }
        if (ModCompat.ICE_AND_FIRE) {
            RogueIceAndFireMemories.register();
        }
        if (ModCompat.VILTRUMITE) {
            ViltrumiteCompat.registerRogue();
        }
    }

    public static RogueMemory vigor() {
        return vigor;
    }

    public static RogueMemory legacy(String primary) {
        if ("weapon_x_logan".equals(primary)) {
            return healingFactor;
        }
        if ("spider_powers_peter_parker".equals(primary)) {
            return spiderPowers;
        }
        if (ViltrumitePower.ID.equals(primary)) {
            return ViltrumiteCompat.memory();
        }
        return null;
    }

    private static Arrow arrow(ServerPlayer player, MobEffectInstance effect) {
        Arrow arrow = new Arrow(player.serverLevel(), player, new ItemStack(Items.ARROW), null);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(RogueKit.dmg(3.0D));
        if (effect != null) {
            arrow.addEffect(effect);
        }
        return arrow;
    }

    private static LivingEntity nearestInSight(ServerPlayer player, double range) {
        return HeroAbilities.cone(player, range, 30.0D).stream()
                .filter(entity -> RogueKit.affects(player, entity))
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .orElse(null);
    }

    private static void vanilla() {
        RogueMemory detonate = RogueKit.memory("creeper", "Creeper")
                .active("Detonate", RogueKit.detonate(3.0F, 30.0D, false, 12.0D))
                .build();
        RogueMemory charged = RogueKit.memory("charged_creeper", "Charged Creeper")
                .active("Supercharged Detonation", RogueKit.detonate(6.0F, 55.0D, false, 18.0D))
                .passive(RogueKit.trail(ParticleTypes.ELECTRIC_SPARK, 4))
                .build();
        RogueAbsorption.registerIfAbsent(ResourceLocation.withDefaultNamespace("creeper"),
                entity -> entity instanceof Creeper creeper && creeper.isPowered() ? charged : detonate, detonate, charged);

        reg("minecraft:skeleton", RogueKit.memory("skeleton", "Skeleton")
                .active("Bone Volley", RogueKit.shoot((player, level) -> arrow(player, null), 2.6D, 3, 8.0F, 3.0D, SoundEvents.SKELETON_SHOOT))
                .build());
        reg("minecraft:stray", RogueKit.memory("stray", "Stray")
                .active("Frost Volley", RogueKit.shoot((player, level) -> arrow(player, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1)),
                        2.6D, 3, 8.0F, 3.0D, SoundEvents.STRAY_AMBIENT))
                .passive(RogueKit.immune(DamageTypeTags.IS_FREEZING))
                .build());
        reg("minecraft:bogged", RogueKit.memory("bogged", "Bogged")
                .active("Toxic Volley", RogueKit.shoot((player, level) -> arrow(player, new MobEffectInstance(MobEffects.POISON, 100, 0)),
                        2.4D, 3, 8.0F, 3.0D, SoundEvents.BOGGED_AMBIENT))
                .build());
        reg("minecraft:wither_skeleton", RogueKit.memory("wither_skeleton", "Wither Skeleton")
                .passives(RogueKit.onHit(RogueKit.apply(MobEffects.WITHER, 100, 0)), RogueKit.immune(DamageTypeTags.IS_FIRE),
                        RogueKit.effects(RogueKit.fx(MobEffects.FIRE_RESISTANCE, 0)))
                .build());

        RogueMemory spider = RogueKit.memory("spider", "Spider")
                .active("Web Shot", RogueKit.web(20.0D, 140, 4.0D))
                .passive(RogueKit.climb())
                .build();
        reg("minecraft:spider", spider);
        reg("minecraft:cave_spider", RogueKit.memory("cave_spider", "Cave Spider")
                .active("Web Shot", RogueKit.web(20.0D, 140, 4.0D))
                .passives(RogueKit.climb(), RogueKit.onHit(RogueKit.apply(MobEffects.POISON, 80, 0)))
                .build());

        reg("minecraft:enderman", RogueKit.memory("enderman", "Enderman")
                .active("Blink", RogueKit.blink(24.0D, 3.0D))
                .passive(RogueKit.waterHurts(1.0D))
                .build());
        reg("minecraft:endermite", RogueKit.memory("endermite", "Endermite")
                .active("Short Blink", RogueKit.blink(8.0D, 1.5D))
                .build());

        reg("minecraft:blaze", RogueKit.memory("blaze", "Blaze")
                .active("Blaze Volley", RogueKit.shoot((player, level) -> new SmallFireball(level, player, Vec3.ZERO), 1.4D, 3, 6.0F, 2.0D,
                        SoundEvents.BLAZE_SHOOT))
                .passives(RogueKit.immune(DamageTypeTags.IS_FIRE), RogueKit.effects(RogueKit.fx(MobEffects.FIRE_RESISTANCE, 0)),
                        RogueKit.trail(ParticleTypes.SMOKE, 6))
                .build());
        reg("minecraft:ghast", RogueKit.memory("ghast", "Ghast")
                .active("Ghast Fireball", RogueKit.shoot((player, level) -> new LargeFireball(level, player, Vec3.ZERO, 1), 1.2D, 1, 0.0F, 4.0D,
                        SoundEvents.GHAST_SHOOT))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0)))
                .build());

        reg("minecraft:slime", RogueKit.memory("slime", "Slime")
                .active("Bounce Slam", RogueKit.leap(1.3D, 0.6D, 4.0D, 18.0D, ParticleTypes.ITEM_SLIME, SoundEvents.SLIME_JUMP, 6.0D, RogueKit.knock(1.2D)))
                .passives(RogueKit.noFall(), RogueKit.effects(RogueKit.fx(MobEffects.JUMP, 1)))
                .build());
        reg("minecraft:magma_cube", RogueKit.memory("magma_cube", "Magma Cube")
                .active("Molten Slam", RogueKit.leap(1.3D, 0.6D, 4.0D, 22.0D, ParticleTypes.LAVA, SoundEvents.MAGMA_CUBE_JUMP, 6.0D, RogueKit.ignite(4.0F)))
                .passives(RogueKit.noFall(), RogueKit.immune(DamageTypeTags.IS_FIRE), RogueKit.effects(RogueKit.fx(MobEffects.JUMP, 1),
                        RogueKit.fx(MobEffects.FIRE_RESISTANCE, 0)))
                .build());

        reg("minecraft:witch", RogueKit.memory("witch", "Witch")
                .active("Hex Brew", player -> {
                    ThrownPotion potion = new ThrownPotion(player.serverLevel(), player);
                    var options = List.of(Potions.HARMING, Potions.POISON, Potions.SLOWNESS, Potions.WEAKNESS);
                    potion.setItem(PotionContents.createItemStack(Items.SPLASH_POTION, options.get(player.getRandom().nextInt(options.size()))));
                    RogueKit.fire(player, player.serverLevel(), potion, Vec3.directionFromRotation(player.getXRot() - 10.0F, player.getYRot()), 0.9D);
                    HeroFx.play(player, SoundEvents.WITCH_THROW, 1.0F, 1.0F);
                    return 3.0D;
                })
                .passive(RogueKit.resist(0.5D, event -> event.getSource().is(DamageTypes.MAGIC) || event.getSource().is(DamageTypes.INDIRECT_MAGIC)))
                .build());
        reg("minecraft:evoker", RogueKit.memory("evoker", "Evoker")
                .active("Fang Line", player -> {
                    Vec3 look = player.getLookAngle();
                    Vec3 flat = new Vec3(look.x, 0.0D, look.z).normalize();
                    float yaw = (float) Math.atan2(flat.z, flat.x);
                    for (int i = 1; i <= 14; i++) {
                        Vec3 at = player.position().add(flat.scale(i * 1.25D));
                        player.serverLevel().addFreshEntity(new EvokerFangs(player.serverLevel(), at.x, Math.floor(player.getY()), at.z, yaw, i, player));
                    }
                    HeroFx.play(player, SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.0F);
                    return 6.0D;
                })
                .build());
        reg("minecraft:vindicator", RogueKit.memory("vindicator", "Vindicator")
                .active("Johnny!", RogueKit.dash(1.6D, 8, 24.0D, 1.2D, ParticleTypes.CRIT, SoundEvents.VINDICATOR_CELEBRATE, 5.0D))
                .passive(RogueKit.bonus(1.25D))
                .build());
        reg("minecraft:pillager", RogueKit.memory("pillager", "Pillager")
                .active("Crossbow Volley", RogueKit.shoot((player, level) -> arrow(player, null), 3.0D, 5, 12.0F, 4.0D, SoundEvents.CROSSBOW_SHOOT))
                .build());
        reg("minecraft:illusioner", RogueKit.memory("illusioner", "Illusioner")
                .active("Mirror Blink", player -> {
                    RogueKit.cone(10.0D, 60.0D, 0.0D, ParticleTypes.EFFECT, null, 0.0D, RogueKit.apply(MobEffects.BLINDNESS, 100, 0).and(RogueKit.untarget()))
                            .use(player);
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0));
                    return RogueKit.blink(16.0D, 8.0D).use(player);
                })
                .build());
        reg("minecraft:ravager", RogueKit.memory("ravager", "Ravager")
                .active("Roar", RogueKit.roar(6.0D, 20.0D, 40, 2.0D, ParticleTypes.POOF, SoundEvents.RAVAGER_ROAR, 10.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)))
                .build());
        reg("minecraft:vex", RogueKit.memory("vex", "Vex")
                .active("Vex Charge", RogueKit.dash(1.8D, 6, 16.0D, 0.5D, ParticleTypes.ENCHANTED_HIT, SoundEvents.VEX_CHARGE, 3.0D))
                .passive(RogueKit.noFall())
                .build());

        reg("minecraft:shulker", RogueKit.memory("shulker", "Shulker")
                .active("Levitation Bullet", player -> {
                    LivingEntity target = nearestInSight(player, 32.0D);
                    if (target == null) {
                        return 0.5D;
                    }
                    ShulkerBullet bullet = new ShulkerBullet(player.serverLevel(), player, target, Direction.Axis.Y);
                    Vec3 eye = player.getEyePosition();
                    bullet.moveTo(eye.x, eye.y, eye.z);
                    player.serverLevel().addFreshEntity(bullet);
                    HeroFx.play(player, SoundEvents.SHULKER_SHOOT, 1.0F, 1.0F);
                    return 4.0D;
                })
                .passive(RogueKit.resist(0.8D, event -> true))
                .build());
        reg("minecraft:guardian", RogueKit.memory("guardian", "Guardian")
                .active("Guardian Laser", RogueKit.beam(16.0D, 20.0D, 1, ParticleTypes.BUBBLE, SoundEvents.GUARDIAN_ATTACK, 4.0D, null))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0)))
                .build());
        reg("minecraft:elder_guardian", RogueKit.memory("elder_guardian", "Elder Guardian")
                .active("Elder Laser", RogueKit.beam(20.0D, 30.0D, 1, ParticleTypes.BUBBLE_POP, SoundEvents.ELDER_GUARDIAN_CURSE, 5.0D,
                        RogueKit.apply(MobEffects.DIG_SLOWDOWN, 600, 2)))
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.DOLPHINS_GRACE, 0)),
                        RogueKit.aura(8.0D, 40, RogueKit.apply(MobEffects.DIG_SLOWDOWN, 80, 1), null))
                .build());
        reg("minecraft:warden", RogueKit.memory("warden", "Warden")
                .active("Sonic Boom", player -> {
                    HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), 20.0D, 8);
                    for (LivingEntity target : beam.targets()) {
                        if (RogueKit.affects(player, target)) {
                            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().sonicBoom(player), RogueKit.dmg(40.0D)));
                            Vec3 push = target.position().subtract(player.position()).normalize();
                            target.push(push.x * 2.0D, 0.5D, push.z * 2.0D);
                            target.hurtMarked = true;
                        }
                    }
                    HeroAbilities.particleLine(player.serverLevel(), beam.start(), beam.end(), ParticleTypes.SONIC_BOOM, 1.2D);
                    HeroFx.play(player, SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 1.0F);
                    return 10.0D;
                })
                .passive(new RogueMemory.Passive() {
                    @Override
                    public void tick(ServerPlayer player) {
                        player.removeEffect(MobEffects.DARKNESS);
                    }
                })
                .build());
        reg("minecraft:breeze", RogueKit.memory("breeze", "Breeze")
                .active("Wind Charge", RogueKit.shoot((player, level) -> new WindCharge(player, level, player.getX(), player.getEyeY(), player.getZ()),
                        1.5D, 1, 0.0F, 1.0D, SoundEvents.BREEZE_SHOOT))
                .passive(RogueKit.noFall())
                .build());

        reg("minecraft:iron_golem", RogueKit.memory("iron_golem", "Iron Golem")
                .active("Golem Toss", RogueKit.toss(5.0D, 1.2D, 26.0D, 4.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_RESISTANCE, 0)))
                .build());
        reg("minecraft:snow_golem", RogueKit.memory("snow_golem", "Snow Golem")
                .active("Snowball Barrage", RogueKit.shoot((player, level) -> new Snowball(level, player), 1.8D, 6, 10.0F, 1.5D, SoundEvents.SNOW_GOLEM_SHOOT))
                .passive(RogueKit.immune(DamageTypeTags.IS_FREEZING))
                .build());
        reg("minecraft:drowned", RogueKit.memory("drowned", "Drowned")
                .active("Trident Throw", player -> {
                    ThrownTrident trident = new ThrownTrident(player.serverLevel(), player, new ItemStack(Items.TRIDENT));
                    trident.pickup = AbstractArrow.Pickup.DISALLOWED;
                    trident.setBaseDamage(RogueKit.dmg(8.0D));
                    RogueKit.fire(player, player.serverLevel(), trident, player.getLookAngle(), 2.5D);
                    HeroFx.play(player, SoundEvents.DROWNED_SHOOT, 1.0F, 1.0F);
                    return 3.0D;
                })
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0)))
                .build());
        reg("minecraft:zombie", RogueKit.memory("zombie", "Zombie")
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)), RogueKit.onHit(RogueKit.apply(MobEffects.HUNGER, 100, 0)))
                .build());
        reg("minecraft:husk", RogueKit.memory("husk", "Husk")
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)), RogueKit.onHit(RogueKit.apply(MobEffects.HUNGER, 200, 1)),
                        RogueKit.immune(DamageTypes.IN_FIRE, DamageTypes.ON_FIRE))
                .build());
        reg("minecraft:zombified_piglin", RogueKit.memory("zombified_piglin", "Zombified Piglin")
                .passives(RogueKit.immune(DamageTypeTags.IS_FIRE), RogueKit.effects(RogueKit.fx(MobEffects.FIRE_RESISTANCE, 0), RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)))
                .build());
        reg("minecraft:piglin_brute", RogueKit.memory("piglin_brute", "Piglin Brute")
                .active("Brute Charge", RogueKit.dash(1.5D, 8, 26.0D, 1.5D, ParticleTypes.CRIT, SoundEvents.PIGLIN_BRUTE_ANGRY, 5.0D))
                .passive(RogueKit.bonus(1.35D))
                .build());
        reg("minecraft:piglin", RogueKit.memory("piglin", "Piglin")
                .active("Crossbow Shot", RogueKit.shoot((player, level) -> arrow(player, null), 3.0D, 1, 0.0F, 1.0D, SoundEvents.CROSSBOW_SHOOT))
                .build());
        RogueMemory boar = RogueKit.memory("hoglin", "Hoglin")
                .active("Tusk Toss", RogueKit.toss(4.0D, 1.0D, 22.0D, 3.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)))
                .build();
        reg("minecraft:hoglin", boar);
        reg("minecraft:zoglin", boar);
        reg("minecraft:goat", RogueKit.memory("goat", "Goat")
                .active("Ram", RogueKit.dash(1.7D, 8, 14.0D, 2.5D, ParticleTypes.POOF, SoundEvents.GOAT_RAM_IMPACT, 4.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.JUMP, 1)))
                .build());
        reg("minecraft:polar_bear", RogueKit.memory("polar_bear", "Polar Bear")
                .active("Maul", RogueKit.cone(4.0D, 50.0D, 22.0D, ParticleTypes.SWEEP_ATTACK, SoundEvents.POLAR_BEAR_WARNING, 4.0D, RogueKit.knock(1.0D)))
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)), RogueKit.immune(DamageTypeTags.IS_FREEZING))
                .build());
        RogueMemory wolf = RogueKit.memory("wolf", "Wolf")
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.MOVEMENT_SPEED, 1)), RogueKit.bonus(1.15D))
                .build();
        reg("minecraft:wolf", wolf);
        reg("minecraft:fox", RogueKit.memory("fox", "Fox")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.MOVEMENT_SPEED, 0), RogueKit.fx(MobEffects.NIGHT_VISION, 0), RogueKit.fx(MobEffects.JUMP, 1)))
                .build());
        RogueMemory cat = RogueKit.memory("cat", "Cat")
                .passives(RogueKit.noFall(), RogueKit.effects(RogueKit.fx(MobEffects.MOVEMENT_SPEED, 0), RogueKit.fx(MobEffects.NIGHT_VISION, 0)))
                .build();
        reg("minecraft:cat", cat);
        reg("minecraft:ocelot", cat);
        reg("minecraft:bee", RogueKit.memory("bee", "Bee")
                .passives(RogueKit.onHit(RogueKit.apply(MobEffects.POISON, 100, 0)), RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0)))
                .build());
        reg("minecraft:pufferfish", RogueKit.memory("pufferfish", "Pufferfish")
                .passives(RogueKit.retaliate(RogueKit.apply(MobEffects.POISON, 100, 1)), RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0)))
                .build());
        RogueMemory squid = RogueKit.memory("squid", "Squid")
                .active("Ink Cloud", RogueKit.nova(6.0D, 0.0D, ParticleTypes.SQUID_INK, SoundEvents.SQUID_SQUIRT, 8.0D,
                        RogueKit.apply(MobEffects.BLINDNESS, 120, 0).and(RogueKit.untarget())))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0)))
                .build();
        reg("minecraft:squid", squid);
        reg("minecraft:glow_squid", RogueKit.memory("glow_squid", "Glow Squid")
                .active("Glow Ink", RogueKit.nova(8.0D, 0.0D, ParticleTypes.GLOW_SQUID_INK, SoundEvents.GLOW_SQUID_SQUIRT, 6.0D,
                        RogueKit.apply(MobEffects.GLOWING, 300, 0)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.NIGHT_VISION, 0)))
                .build());
        reg("minecraft:dolphin", RogueKit.memory("dolphin", "Dolphin")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DOLPHINS_GRACE, 0), RogueKit.fx(MobEffects.WATER_BREATHING, 0)))
                .build());
        reg("minecraft:turtle", RogueKit.memory("turtle", "Turtle")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.WATER_BREATHING, 0), RogueKit.fx(MobEffects.DAMAGE_RESISTANCE, 1)))
                .build());
        reg("minecraft:axolotl", RogueKit.memory("axolotl", "Axolotl")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.REGENERATION, 0), RogueKit.fx(MobEffects.WATER_BREATHING, 0)))
                .build());
        reg("minecraft:armadillo", RogueKit.memory("armadillo", "Armadillo")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_RESISTANCE, 1)))
                .build());
        RogueMemory steed = RogueKit.memory("horse", "Horse")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.MOVEMENT_SPEED, 1), RogueKit.fx(MobEffects.JUMP, 1)))
                .build();
        for (String id : List.of("minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:camel", "minecraft:llama", "minecraft:trader_llama",
                "minecraft:skeleton_horse", "minecraft:zombie_horse")) {
            reg(id, steed);
        }
        reg("minecraft:rabbit", RogueKit.memory("rabbit", "Rabbit")
                .passives(RogueKit.effects(RogueKit.fx(MobEffects.JUMP, 2), RogueKit.fx(MobEffects.MOVEMENT_SPEED, 0)), RogueKit.noFall())
                .build());
        RogueMemory feather = RogueKit.memory("chicken", "Chicken")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0)))
                .build();
        reg("minecraft:chicken", feather);
        reg("minecraft:parrot", feather);
        reg("minecraft:phantom", RogueKit.memory("phantom", "Phantom")
                .active("Swoop", RogueKit.dash(1.6D, 10, 16.0D, 0.6D, ParticleTypes.WHITE_ASH, SoundEvents.PHANTOM_SWOOP, 4.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0), RogueKit.fx(MobEffects.NIGHT_VISION, 0)))
                .build());
        reg("minecraft:bat", RogueKit.memory("bat", "Bat")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.NIGHT_VISION, 0)))
                .build());
        reg("minecraft:strider", RogueKit.memory("strider", "Strider")
                .passives(RogueKit.immune(DamageTypeTags.IS_FIRE), RogueKit.effects(RogueKit.fx(MobEffects.FIRE_RESISTANCE, 0)))
                .build());
        reg("minecraft:frog", RogueKit.memory("frog", "Frog")
                .active("Tongue Lash", RogueKit.beam(6.0D, 10.0D, 1, ParticleTypes.ITEM_SLIME, SoundEvents.FROG_TONGUE, 2.0D, RogueKit.pull(1.0D)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.JUMP, 2)))
                .build());
        RogueMemory villager = RogueKit.memory("villager", "Villager")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.HERO_OF_THE_VILLAGE, 0)))
                .build();
        reg("minecraft:villager", villager);
        reg("minecraft:wandering_trader", RogueKit.memory("wandering_trader", "Wandering Trader")
                .active("Vanish", RogueKit.buff(15.0D, SoundEvents.WANDERING_TRADER_DRINK_POTION, new MobEffectInstance(MobEffects.INVISIBILITY, 300, 0)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.HERO_OF_THE_VILLAGE, 0)))
                .build());
        reg("minecraft:allay", RogueKit.memory("allay", "Allay")
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.SLOW_FALLING, 0), RogueKit.fx(MobEffects.LUCK, 0)))
                .build());

        reg("minecraft:ender_dragon", RogueKit.memory("ender_dragon", "Ender Dragon")
                .active("Dragon Breath", RogueKit.shoot((player, level) -> new DragonFireball(level, player, Vec3.ZERO), 1.4D, 1, 0.0F, 8.0D,
                        SoundEvents.ENDER_DRAGON_SHOOT))
                .passives(RogueKit.noFall(), RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_RESISTANCE, 1)))
                .build());
        reg("minecraft:wither", RogueKit.memory("wither", "Wither")
                .active("Wither Skulls", player -> {
                    for (int i = 0; i < 3; i++) {
                        WitherSkull skull = new WitherSkull(player.serverLevel(), player, Vec3.ZERO);
                        skull.setDangerous(i == 1);
                        RogueKit.fire(player, player.serverLevel(), skull, Vec3.directionFromRotation(player.getXRot(), player.getYRot() + (i - 1) * 8.0F), 1.3D);
                    }
                    HeroFx.play(player, SoundEvents.WITHER_SHOOT, 1.2F, 1.0F);
                    return 5.0D;
                })
                .passives(RogueKit.immune(DamageTypes.WITHER), new RogueMemory.Passive() {
                    @Override
                    public void tick(ServerPlayer player) {
                        player.removeEffect(MobEffects.WITHER);
                    }
                })
                .build());
    }

    private static void marvel() {
        reg("marvel:hydra_agent", RogueKit.memory("hydra_agent", "Hydra Agent")
                .active("Hail Hydra", RogueKit.buff(20.0D, SoundEvents.PILLAGER_CELEBRATE,
                        new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0), new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0)))
                .passive(RogueKit.bonus(1.15D))
                .build());
        reg("marvel:baron_zemo", RogueKit.memory("baron_zemo", "Baron Zemo")
                .active("Tactical Strike", RogueKit.dash(1.5D, 8, 24.0D, 1.0D, ParticleTypes.CRIT, SoundEvents.PLAYER_ATTACK_SWEEP, 4.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.MOVEMENT_SPEED, 0)))
                .build());
        reg("marvel:winter_soldier", RogueKit.memory("winter_soldier", "Winter Soldier")
                .active("Bionic Arm", RogueKit.toss(5.0D, 1.0D, 30.0D, 4.0D))
                .passives(RogueKit.bonus(1.2D), RogueKit.immune(DamageTypeTags.IS_FREEZING))
                .build());
        reg("marvel:red_skull", RogueKit.memory("red_skull", "Red Skull")
                .active("Reign of the Skull", RogueKit.roar(7.0D, 10.0D, 40, 1.2D, ParticleTypes.ANGRY_VILLAGER, SoundEvents.EVOKER_PREPARE_WOLOLO, 15.0D))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_BOOST, 0)))
                .build());
        reg("marvel:iron_man_sentry", RogueKit.memory("iron_man_sentry", "Iron Man Sentry")
                .active("Repulsor", RogueKit.beam(20.0D, 24.0D, 1, ParticleTypes.END_ROD, SoundEvents.BEACON_POWER_SELECT, 2.0D, RogueKit.knock(1.5D)))
                .passive(RogueKit.effects(RogueKit.fx(MobEffects.DAMAGE_RESISTANCE, 0)))
                .build());
    }
}
