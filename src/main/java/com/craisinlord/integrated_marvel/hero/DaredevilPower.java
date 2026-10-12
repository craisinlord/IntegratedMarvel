package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.mixin.DisplayAccessor;
import com.craisinlord.integrated_marvel.mixin.ItemDisplayAccessor;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class DaredevilPower {
    public static final String ID = "radar_sense_matt_murdock";
    public static final String RADAR_STATE = "integrated_marvel_radar";
    private static final String CLUB_TAG = "integrated_marvel_club";
    private static final DustParticleOptions CABLE = new DustParticleOptions(new Vector3f(0.85F, 0.85F, 0.85F), 0.6F);
    private static final double CLUB_SPEED = 1.4D;
    private static final Map<UUID, Club> CLUBS = new HashMap<>();
    private static final Map<Holder<MobEffect>, Integer> EFFECTS = Map.of(MobEffects.MOVEMENT_SPEED, 0, MobEffects.JUMP, 0);
    private static final Map<Holder<MobEffect>, HeroPower.Toggle> EFFECT_TOGGLES = Map.of(
            MobEffects.MOVEMENT_SPEED, HeroToggles.SPEED, MobEffects.JUMP, HeroToggles.SUPER_LEAP);
    private static final String[] SPRINT_FLIPS = {"flip_high_1", "flip_high_2", "flip_high_3", "flip_high_4", "flip_both_1", "flip_both_2"};

    private DaredevilPower() {
    }

    private static final class Club {
        private final Display.ItemDisplay display;
        private final Set<Integer> hit = new HashSet<>();
        private final float damage;
        private Vec3 pos;
        private Vec3 direction;
        private LivingEntity target;
        private boolean returning;
        private int age;
        private float spin;

        private Club(Display.ItemDisplay display, Vec3 pos, Vec3 direction, LivingEntity target, float damage) {
            this.display = display;
            this.pos = pos;
            this.direction = direction;
            this.target = target;
            this.damage = damage;
        }
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Radar Sense (Matt Murdock)",
                syringe,
                MarvelBalanceConfig.DAREDEVIL,
                List.of("agility"),
                List.of(
                        new HeroPower.Ability("radar_sense", "Radar Sense", DaredevilPower::radarSense),
                        new HeroPower.Ability("billy_club", "Billy Club", DaredevilPower::billyClub),
                        new HeroPower.Ability("grapple_line", "Grapple Line", DaredevilPower::grappleLine),
                        new HeroPower.Ability("takedown", "Takedown", DaredevilPower::takedown),
                        HeroToggles.WHEEL
                ),
                List.of(HeroToggles.SPEED, HeroToggles.SUPER_LEAP),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (player.tickCount % 20 == 0) {
                            HeroToggles.applyEffects(player, EFFECTS, EFFECT_TOGGLES);
                        }
                        if (player.hasEffect(MobEffects.BLINDNESS)) {
                            player.removeEffect(MobEffects.BLINDNESS);
                        }
                        if (player.hasEffect(MobEffects.DARKNESS)) {
                            player.removeEffect(MobEffects.DARKNESS);
                        }
                        tickClub(player);
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroPowers.data(player).removeState(RADAR_STATE);
                        EFFECTS.keySet().forEach(effect -> {
                            MobEffectInstance current = player.getEffect(effect);
                            if (current != null && current.isInfiniteDuration() && current.isAmbient()) {
                                player.removeEffect(effect);
                            }
                        });
                        dropClub(player);
                        HeroGrapple.clear(player);
                    }

                    @Override
                    public void jump(Player player) {
                        leap(player);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                                && player.getRandom().nextDouble() < MarvelBalanceConfig.DANGER_SENSE_CHANCE.get()) {
                            event.setCanceled(true);
                            HeroFx.play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.4F);
                            HeroAnimations.play(player, "flip_low_2");
                        }
                    }
                }
        );
    }

    private static void leap(Player player) {
        if (player.isCrouching() && HeroToggles.isOn(player, HeroToggles.SUPER_LEAP)) {
            HeroAnimations.playLeapFlip(player);
            Vec3 motion = player.getDeltaMovement();
            double horizontal = player.isSprinting() ? MarvelBalanceConfig.DAREDEVIL_LEAP_HORIZONTAL.get() : 1.0D;
            player.setDeltaMovement(motion.x * horizontal, motion.y * MarvelBalanceConfig.DAREDEVIL_LEAP_MULTIPLIER.get(), motion.z * horizontal);
            player.hurtMarked = true;
        } else if (player.isSprinting() && player.getRandom().nextFloat() < MarvelBalanceConfig.DAREDEVIL_FLIP_CHANCE.get()) {
            HeroAnimations.requestFromClient(player, SPRINT_FLIPS[player.getRandom().nextInt(SPRINT_FLIPS.length)]);
        }
    }

    public static boolean isRadarOn(Player player) {
        PlayerData data = HeroPowers.data(player);
        return ID.equals(data.getPrimaryPower()) && data.hasState(RADAR_STATE);
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity().getTags().contains(CLUB_TAG)) {
            event.setCanceled(true);
        }
    }

    public static void dropClub(ServerPlayer player) {
        Club club = CLUBS.remove(player.getUUID());
        if (club != null) {
            club.display.discard();
        }
    }

    private static double radarSense(ServerPlayer player, int slot) {
        PlayerData data = HeroPowers.data(player);
        if (data.hasState(RADAR_STATE)) {
            data.removeState(RADAR_STATE);
            HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 0.6F);
        } else {
            data.addState(RADAR_STATE);
            HeroFx.play(player, SoundEvents.WARDEN_HEARTBEAT, 0.8F, 1.4F);
        }
        PlayerData.sync(player);
        return 0.5D;
    }

    private static double billyClub(ServerPlayer player, int slot) {
        if (CLUBS.containsKey(player.getUUID())) {
            return 0.0D;
        }
        ServerLevel level = player.serverLevel();
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display == null) {
            return 0.0D;
        }
        Vec3 start = player.getEyePosition().subtract(0.0D, 0.2D, 0.0D).add(player.getLookAngle().scale(0.6D));
        ((ItemDisplayAccessor) display).integratedMarvel$setItemStack(new ItemStack(Items.STICK));
        ((DisplayAccessor) display).integratedMarvel$setPosRotInterpolationDuration(2);
        display.addTag(CLUB_TAG);
        display.moveTo(start.x, start.y, start.z, player.getYRot(), 0.0F);
        level.addFreshEntity(display);
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), 24.0D, 1);
        LivingEntity target = beam.targets().isEmpty() ? null : beam.targets().get(0);
        float damage = (float) (HeroPowers.powerDamage(player) * MarvelBalanceConfig.BILLY_CLUB_DAMAGE_MULTIPLIER.get());
        CLUBS.put(player.getUUID(), new Club(display, start, player.getLookAngle(), target, damage));
        HeroFx.play(player, SoundEvents.TRIDENT_THROW.value(), 0.8F, 1.4F);
        HeroAnimations.play(player, "celestisynth:cs_crescentia_throw");
        return MarvelBalanceConfig.BILLY_CLUB_COOLDOWN.get();
    }

    private static void tickClub(ServerPlayer player) {
        Club club = CLUBS.get(player.getUUID());
        if (club == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        club.age++;
        if (club.age > 200 || club.display.isRemoved() || club.display.level() != level) {
            dropClub(player);
            return;
        }
        Vec3 aim;
        if (club.returning) {
            aim = player.getEyePosition().subtract(0.0D, 0.3D, 0.0D);
            if (club.pos.distanceTo(aim) < CLUB_SPEED + 0.5D) {
                dropClub(player);
                HeroFx.play(player, SoundEvents.ITEM_PICKUP, 0.6F, 0.8F);
                return;
            }
        } else if (club.target != null) {
            if (!club.target.isAlive()) {
                club.target = next(player, club);
                if (club.target == null) {
                    club.returning = true;
                }
                return;
            }
            aim = club.target.getBoundingBox().getCenter();
        } else {
            aim = club.pos.add(club.direction.scale(CLUB_SPEED));
            if (club.age > 18) {
                club.returning = true;
            }
        }
        Vec3 step = aim.subtract(club.pos);
        double length = step.length();
        Vec3 next = length > CLUB_SPEED ? club.pos.add(step.scale(CLUB_SPEED / length)) : aim;
        if (!club.returning) {
            HitResult wall = level.clip(new ClipContext(club.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, club.display));
            if (wall.getType() != HitResult.Type.MISS) {
                club.returning = true;
                HeroFx.play(club.display, SoundEvents.WOOD_HIT, 1.0F, 1.2F);
                return;
            }
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(club.pos, next).inflate(0.6D),
                    entity -> entity != player && entity.isAlive() && !club.hit.contains(entity.getId()))) {
                strike(player, club, victim);
                if (club.returning) {
                    break;
                }
            }
        }
        club.pos = next;
        club.spin = (club.spin + 50.0F) % 360.0F;
        club.display.moveTo(next.x, next.y, next.z, club.spin, 0.0F);
        if (club.age % 2 == 0) {
            level.sendParticles(ParticleTypes.CRIT, next.x, next.y, next.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void strike(ServerPlayer player, Club club, LivingEntity victim) {
        club.hit.add(victim.getId());
        HeroAbilities.runAsAbilityDamage(() -> victim.hurt(player.damageSources().mobAttack(player), club.damage));
        HeroFx.play(victim, SoundEvents.WOOD_HIT, 1.2F, 1.0F);
        if (club.hit.size() >= MarvelBalanceConfig.BILLY_CLUB_BOUNCES.get()) {
            club.returning = true;
            return;
        }
        club.target = next(player, club);
        if (club.target == null) {
            club.returning = true;
        }
    }

    private static LivingEntity next(ServerPlayer player, Club club) {
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(club.pos, club.pos).inflate(8.0D),
                        entity -> entity instanceof Enemy && entity.isAlive() && !club.hit.contains(entity.getId()) && entity.hasLineOfSight(club.display))
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(club.pos)))
                .orElse(null);
    }

    private static double grappleLine(ServerPlayer player, int slot) {
        Vec3 anchor = HeroGrapple.anchor(player, MarvelBalanceConfig.GRAPPLE_RANGE.get());
        if (anchor == null) {
            return 0.0D;
        }
        HeroAbilities.particleLine(player.serverLevel(), player.getEyePosition().subtract(0.0D, 0.3D, 0.0D), anchor, CABLE, 0.4D);
        HeroGrapple.start(player, anchor, CABLE);
        HeroFx.play(player, SoundEvents.CROSSBOW_SHOOT, 0.8F, 1.3F);
        HeroAnimations.play(player, "web_shoot_r");
        return MarvelBalanceConfig.GRAPPLE_COOLDOWN.get();
    }

    private static double takedown(ServerPlayer player, int slot) {
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), 6.0D, 1);
        if (beam.targets().isEmpty()) {
            return 0.0D;
        }
        LivingEntity target = beam.targets().get(0);
        Vec3 lunge = target.position().subtract(player.position()).normalize().scale(1.1D);
        player.setDeltaMovement(lunge.x, 0.25D, lunge.z);
        player.hurtMarked = true;
        float damage = (float) (HeroPowers.powerDamage(player) * MarvelBalanceConfig.TAKEDOWN_DAMAGE_MULTIPLIER.get());
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().mobAttack(player), damage));
        if (HeroAbilities.canControl(target)) {
            HeroStun.stun(target, MarvelBalanceConfig.TAKEDOWN_STUN_TICKS.get());
        }
        Vec3 center = target.getBoundingBox().getCenter();
        player.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        HeroFx.play(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.8F);
        HeroAnimations.play(player, "wolverine_pounce");
        return MarvelBalanceConfig.TAKEDOWN_COOLDOWN.get();
    }
}
