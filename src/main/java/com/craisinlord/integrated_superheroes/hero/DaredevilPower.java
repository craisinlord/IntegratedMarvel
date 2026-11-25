package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.mixin.DisplayAccessor;
import com.craisinlord.integrated_superheroes.mixin.ItemDisplayAccessor;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
    public static final String RADAR_STATE = "integrated_superheroes_radar";
    private static final String CLUB_TAG = "integrated_superheroes_club";
    private static final DustParticleOptions CABLE = new DustParticleOptions(new Vector3f(0.85F, 0.85F, 0.85F), 0.6F);
    private static final double CLUB_SPEED = 1.4D;
    private static final Map<UUID, Club> CLUBS = ServerState.map(new HashMap<>());
    private static final Map<UUID, Prop> PROPS = ServerState.map(new HashMap<>());
    private static final Map<Holder<MobEffect>, Integer> EFFECTS = Map.of(MobEffects.MOVEMENT_SPEED, 0, MobEffects.JUMP, 0);
    private static final Map<Holder<MobEffect>, HeroPower.Toggle> EFFECT_TOGGLES = Map.of(
            MobEffects.MOVEMENT_SPEED, HeroToggles.SPEED, MobEffects.JUMP, HeroToggles.SUPER_LEAP);
    private static boolean registered;
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

    private static final class Prop {
        private final Display.ItemDisplay display;
        private final boolean orbit;
        private final float spinSpeed;
        private int ticks;
        private float angle;

        private Prop(Display.ItemDisplay display, int ticks, boolean orbit, float spinSpeed, float angle) {
            this.display = display;
            this.ticks = ticks;
            this.orbit = orbit;
            this.spinSpeed = spinSpeed;
            this.angle = angle;
        }
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            HeroCaster.onTick(caster -> {
                tickClub(caster);
                tickProp(caster);
            });
            HeroCaster.onCleanup(caster -> {
                dropClub(caster);
                dropProp(caster);
            });
        }
        return new HeroPower(
                ID,
                "Radar Sense (Matt Murdock)",
                syringe,
                MarvelBalanceConfig.DAREDEVIL,
                List.of("agility"),
                List.of(
                        new HeroPower.Ability("radar_sense", "Radar Sense", DaredevilPower::radarSense),
                        HeroPower.Ability.of("billy_club", "Billy Club", DaredevilPower::billyClub),
                        new HeroPower.Ability("grapple_line", "Grapple Line", DaredevilPower::grappleLine),
                        HeroPower.Ability.of("takedown", "Takedown", DaredevilPower::takedown),
                        HeroPower.Ability.of("staff_sweep", "Staff Sweep", DaredevilPower::staffSweep),
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
                        tickProp(player);
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
                        dropProp(player);
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

    public static void dropClub(LivingEntity player) {
        Club club = CLUBS.remove(player.getUUID());
        if (club != null) {
            club.display.discard();
        }
    }

    public static void dropProp(LivingEntity player) {
        Prop prop = PROPS.remove(player.getUUID());
        if (prop != null) {
            prop.display.discard();
        }
    }

    private static ItemStack clubForm(String form, Item fallback) {
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("thematic", form)).orElse(fallback));
    }

    private static void showProp(LivingEntity player, ItemStack stack, int ticks, boolean orbit, float spinSpeed) {
        dropProp(player);
        ServerLevel level = HeroCaster.level(player);
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display == null) {
            return;
        }
        ((ItemDisplayAccessor) display).integratedSuperheroes$setItemStack(stack);
        ((DisplayAccessor) display).integratedSuperheroes$setPosRotInterpolationDuration(1);
        display.addTag(CLUB_TAG);
        Prop prop = new Prop(display, ticks, orbit, spinSpeed, player.getYRot());
        Vec3 at = propPos(player, prop);
        display.moveTo(at.x, at.y, at.z, prop.angle, 0.0F);
        level.addFreshEntity(display);
        PROPS.put(player.getUUID(), prop);
    }

    private static Vec3 propPos(LivingEntity player, Prop prop) {
        if (prop.orbit) {
            double radians = Math.toRadians(prop.angle);
            return player.position().add(-Math.sin(radians) * 1.1D, 1.0D, Math.cos(radians) * 1.1D);
        }
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x).normalize();
        return player.getEyePosition().subtract(0.0D, 0.45D, 0.0D).add(look.scale(0.7D)).add(right.scale(0.3D));
    }

    private static void tickProp(LivingEntity player) {
        Prop prop = PROPS.get(player.getUUID());
        if (prop == null) {
            return;
        }
        if (--prop.ticks <= 0 || prop.display.isRemoved() || prop.display.level() != player.level()) {
            dropProp(player);
            return;
        }
        prop.angle = prop.spinSpeed == 0.0F ? player.getYRot() : (prop.angle + prop.spinSpeed) % 360.0F;
        Vec3 at = propPos(player, prop);
        prop.display.moveTo(at.x, at.y, at.z, prop.angle, 0.0F);
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

    private static double billyClub(LivingEntity player, int slot) {
        if (CLUBS.containsKey(player.getUUID())) {
            return 0.0D;
        }
        ServerLevel level = HeroCaster.level(player);
        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
        if (display == null) {
            return 0.0D;
        }
        Vec3 start = player.getEyePosition().subtract(0.0D, 0.2D, 0.0D).add(player.getLookAngle().scale(0.6D));
        ((ItemDisplayAccessor) display).integratedSuperheroes$setItemStack(clubForm("billy_club", Items.STICK));
        ((DisplayAccessor) display).integratedSuperheroes$setPosRotInterpolationDuration(2);
        display.addTag(CLUB_TAG);
        display.moveTo(start.x, start.y, start.z, player.getYRot(), 0.0F);
        level.addFreshEntity(display);
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), 24.0D, 1);
        LivingEntity target = beam.targets().isEmpty() ? null : beam.targets().get(0);
        float damage = MarvelBalanceConfig.BILLY_CLUB_DAMAGE.get().floatValue();
        CLUBS.put(player.getUUID(), new Club(display, start, player.getLookAngle(), target, damage));
        HeroFx.play(player, SoundEvents.TRIDENT_THROW.value(), 0.8F, 1.4F);
        HeroAnimations.play(player, "celestisynth:cs_crescentia_throw");
        return MarvelBalanceConfig.BILLY_CLUB_COOLDOWN.get();
    }

    private static void tickClub(LivingEntity player) {
        Club club = CLUBS.get(player.getUUID());
        if (club == null) {
            return;
        }
        ServerLevel level = HeroCaster.level(player);
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
                    entity -> entity != player && entity.isAlive() && !club.hit.contains(entity.getId())
                            && (player instanceof ServerPlayer || HeroCaster.foe(player, entity)))) {
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

    private static void strike(LivingEntity player, Club club, LivingEntity victim) {
        club.hit.add(victim.getId());
        if (club.hit.size() >= 3 && player instanceof ServerPlayer user) {
            HeroAdvancements.award(user, "street_level/daredevil/bank_shot");
        }
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

    private static LivingEntity next(LivingEntity player, Club club) {
        return HeroCaster.level(player).getEntitiesOfClass(LivingEntity.class, new AABB(club.pos, club.pos).inflate(8.0D),
                        entity -> (player instanceof ServerPlayer ? entity instanceof Enemy : HeroCaster.foe(player, entity)) && entity.isAlive() && !club.hit.contains(entity.getId()) && entity.hasLineOfSight(club.display))
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
        showProp(player, clubForm("billy_club_grapple", Items.TRIPWIRE_HOOK), 14, false, 0.0F);
        HeroFx.play(player, SoundEvents.CROSSBOW_SHOOT, 0.8F, 1.3F);
        HeroAnimations.play(player, "web_shoot_r");
        return MarvelBalanceConfig.GRAPPLE_COOLDOWN.get();
    }

    private static double takedown(LivingEntity player, int slot) {
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), 6.0D, 1);
        if (beam.targets().isEmpty()) {
            return 0.0D;
        }
        LivingEntity target = beam.targets().get(0);
        Vec3 lunge = target.position().subtract(player.position()).normalize().scale(1.1D);
        player.setDeltaMovement(lunge.x, 0.25D, lunge.z);
        player.hurtMarked = true;
        float damage = MarvelBalanceConfig.TAKEDOWN_DAMAGE.get().floatValue();
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().mobAttack(player), damage));
        if (HeroAbilities.canControl(target)) {
            HeroStun.stun(target, MarvelBalanceConfig.TAKEDOWN_STUN_TICKS.get());
        }
        Vec3 center = target.getBoundingBox().getCenter();
        HeroCaster.level(player).sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        HeroFx.play(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.8F);
        HeroAnimations.play(player, "wolverine_pounce");
        showProp(player, clubForm("billy_club_nunchuck", Items.CHAIN), 12, false, 60.0F);
        return MarvelBalanceConfig.TAKEDOWN_COOLDOWN.get();
    }

    private static double staffSweep(LivingEntity player, int slot) {
        ServerLevel level = HeroCaster.level(player);
        double radius = MarvelBalanceConfig.STAFF_SWEEP_RADIUS.get();
        float damage = MarvelBalanceConfig.STAFF_SWEEP_DAMAGE.get().floatValue();
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().mobAttack(player), damage));
            SerumKit.push(target, player.position(), 1.1D, 0.35D);
            HeroFx.play(target, SoundEvents.WOOD_HIT, 1.0F, 0.9F);
        }
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        for (int i = 0; i < 12; i++) {
            double radians = Math.PI * 2.0D * i / 12.0D;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x + Math.cos(radians) * radius * 0.6D, center.y,
                    center.z + Math.sin(radians) * radius * 0.6D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        showProp(player, clubForm("billy_club_staff", Items.BAMBOO), 9, true, 45.0F);
        HeroFx.play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.8F);
        HeroAnimations.play(player, "celestisynth:cs_solaris_spin");
        return MarvelBalanceConfig.STAFF_SWEEP_COOLDOWN.get();
    }
}
