package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteAnimations;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class AmazonPower {
    public static final String ID = "amazon_diana_prince";
    public static final String GUARD_STATE = "integrated_superheroes_amazon_guard";
    private static final String GUARD_UNTIL = "integrated_superheroes_amazon_guard_until";
    private static final int LASSO_COLOR = 0xFFC83D;
    private static boolean registered;
    private static final java.util.Map<java.util.UUID, Long> CAUGHT = ServerState.map(new java.util.HashMap<>());
    private static final java.util.Map<java.util.UUID, Long> MOB_GUARD = ServerState.map(new java.util.HashMap<>());

    private AmazonPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(AmazonPower::onProjectile);
            NeoForge.EVENT_BUS.addListener(AmazonPower::onMobDamage);
            HeroCaster.onTick(AmazonPower::tickMobGuard);
            HeroCaster.onCleanup(caster -> {
                MOB_GUARD.remove(caster.getUUID());
                CAUGHT.remove(caster.getUUID());
            });
        }
        return new HeroPower(
                ID,
                "Amazon (Diana Prince)",
                syringe,
                NewSerumConfig.AMAZON,
                List.of("agility"),
                List.of(
                        HeroPower.Ability.of("lasso_of_truth", "Lasso of Truth", AmazonPower::lasso),
                        HeroPower.Ability.of("bracelet_clash", "Bracelet Clash", AmazonPower::braceletClash),
                        HeroPower.Ability.of("bracelets_up", "Bracelets Up", AmazonPower::guard),
                        HeroToggles.WHEEL
                ),
                List.of(HeroToggles.SUPER_LEAP),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (player.tickCount % 20 == 0) {
                            Map<Holder<MobEffect>, Integer> effects = Map.of(MobEffects.JUMP, NewSerumConfig.AMAZON_LEAP_LEVEL.get() - 1);
                            Map<Holder<MobEffect>, HeroPower.Toggle> toggles = Map.of(MobEffects.JUMP, HeroToggles.SUPER_LEAP);
                            HeroToggles.applyEffects(player, effects, toggles);
                        }
                        if (isGuarding(player) && player.server.overworld().getGameTime() >= player.getPersistentData().getLong(GUARD_UNTIL)) {
                            HeroPowers.data(player).removeState(GUARD_STATE);
                            PlayerData.sync(player);
                            HeroFx.play(player, SoundEvents.ARMOR_EQUIP_IRON.value(), 0.6F, 0.8F);
                        }
                        if (isGuarding(player) && player.tickCount % 4 == 0) {
                            Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(0.6D)).subtract(0.0D, 0.4D, 0.0D);
                            player.serverLevel().sendParticles(SerumKit.glow(player, 0xE0E0E0, 0.6F), front.x, front.y, front.z, 2, 0.2D, 0.2D, 0.2D, 0.0D);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroPowers.data(player).removeState(GUARD_STATE);
                        CAUGHT.remove(player.getUUID());
                        HeroGrapple.clear(player);
                        SerumKit.dropEffect(player, MobEffects.JUMP);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypeTags.IS_FALL) && HeroToggles.isOn(player, HeroToggles.SUPER_LEAP)) {
                            event.setAmount(event.getAmount() * 0.25F);
                        }
                        if (!isGuarding(player) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                            return;
                        }
                        if (catches(player, event)) {
                            event.setCanceled(true);
                            long now = player.level().getGameTime();
                            Long ready = CAUGHT.get(player.getUUID());
                            if (ready == null || now >= ready) {
                                CAUGHT.put(player.getUUID(), now + 20L);
                                braceletClash(player, 0);
                                HeroAdvancements.count(player, "justice_league/amazon/bulletproof", 1.0D, 25.0D);
                            }
                            return;
                        }
                        if (event.getSource().getSourcePosition() != null && inFront(player, event.getSource().getSourcePosition())) {
                            event.setAmount(event.getAmount() * NewSerumConfig.GUARD_MELEE_MULTIPLIER.get().floatValue());
                            HeroFx.play(player, SoundEvents.ANVIL_LAND, 0.4F, 1.8F);
                        }
                    }
                }
        );
    }

    private static boolean catches(ServerPlayer player, LivingIncomingDamageEvent event) {
        if (!NewSerumConfig.BRACELETS_CATCH.get()) {
            return false;
        }
        if (event.getSource().is(DamageTypeTags.IS_LIGHTNING)) {
            return true;
        }
        Vec3 from = event.getSource().getSourcePosition();
        return event.getSource().getDirectEntity() == null && event.getSource().getEntity() != null && from != null && inFront(player, from);
    }

    public static boolean isGuarding(Player player) {
        return SerumKit.is(player, ID) && HeroPowers.data(player).hasState(GUARD_STATE);
    }

    private static boolean guarding(LivingEntity entity) {
        if (entity instanceof Player player) {
            return isGuarding(player);
        }
        Long until = MOB_GUARD.get(entity.getUUID());
        return until != null && entity.level().getGameTime() < until;
    }

    private static void tickMobGuard(LivingEntity caster) {
        Long until = MOB_GUARD.get(caster.getUUID());
        if (until == null) {
            return;
        }
        if (caster.level().getGameTime() >= until) {
            MOB_GUARD.remove(caster.getUUID());
            HeroFx.play(caster, SoundEvents.ARMOR_EQUIP_IRON.value(), 0.6F, 0.8F);
            return;
        }
        if (caster.tickCount % 4 == 0) {
            Vec3 front = caster.getEyePosition().add(caster.getLookAngle().scale(0.6D)).subtract(0.0D, 0.4D, 0.0D);
            HeroCaster.level(caster).sendParticles(SerumKit.glow(caster, 0xE0E0E0, 0.6F), front.x, front.y, front.z, 2, 0.2D, 0.2D, 0.2D, 0.0D);
        }
    }

    private static void onMobDamage(LivingIncomingDamageEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob instanceof Player || !MOB_GUARD.containsKey(mob.getUUID()) || !guarding(mob)
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        Vec3 from = event.getSource().getSourcePosition();
        boolean front = from != null && inFront(mob, from);
        if (NewSerumConfig.BRACELETS_CATCH.get() && (event.getSource().is(DamageTypeTags.IS_LIGHTNING)
                || event.getSource().getDirectEntity() == null && event.getSource().getEntity() != null && front)) {
            event.setCanceled(true);
            long now = mob.level().getGameTime();
            Long ready = CAUGHT.get(mob.getUUID());
            if (ready == null || now >= ready) {
                CAUGHT.put(mob.getUUID(), now + 20L);
                braceletClash(mob, -1);
            }
            return;
        }
        if (front) {
            event.setAmount(event.getAmount() * NewSerumConfig.GUARD_MELEE_MULTIPLIER.get().floatValue());
            HeroFx.play(mob, SoundEvents.ANVIL_LAND, 0.4F, 1.8F);
        }
    }

    private static boolean inFront(LivingEntity player, Vec3 from) {
        Vec3 to = from.subtract(player.getEyePosition());
        Vec3 flat = new Vec3(to.x, 0.0D, to.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            return true;
        }
        Vec3 look = player.getLookAngle();
        return flat.normalize().dot(new Vec3(look.x, 0.0D, look.z).normalize()) > 0.2D;
    }

    private static void onProjectile(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity player)
                || !(player instanceof ServerPlayer || player instanceof Mob) || !guarding(player)) {
            return;
        }
        Projectile projectile = event.getProjectile();
        if (!inFront(player, projectile.position())) {
            return;
        }
        event.setCanceled(true);
        Vec3 motion = projectile.getDeltaMovement();
        projectile.setDeltaMovement(motion.scale(-0.8D));
        projectile.setOwner(player);
        projectile.hurtMarked = true;
        projectile.hasImpulse = true;
        ServerLevel level = HeroCaster.level(player);
        if (!HeroFx.vfx(level, projectile.position(), "impact_starburst", 0)) {
            level.sendParticles(ParticleTypes.CRIT, projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.2D);
        }
        HeroFx.play(player, SoundEvents.ANVIL_PLACE, 0.5F, 2.0F);
        if (player instanceof ServerPlayer user) {
            HeroAdvancements.count(user, "justice_league/amazon/bulletproof", 1.0D, 25.0D);
        }
    }

    private static double guard(LivingEntity caster, int slot) {
        if (!(caster instanceof ServerPlayer player)) {
            if (guarding(caster)) {
                return 0.0D;
            }
            MOB_GUARD.put(caster.getUUID(), caster.level().getGameTime() + Math.round(NewSerumConfig.GUARD_SECONDS.get() * 20.0D));
            HeroFx.play(caster, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.2F);
            return NewSerumConfig.GUARD_COOLDOWN.get();
        }
        PlayerData data = HeroPowers.data(player);
        if (data.hasState(GUARD_STATE)) {
            return 0.0D;
        }
        data.addState(GUARD_STATE);
        player.getPersistentData().putLong(GUARD_UNTIL, player.server.overworld().getGameTime()
                + Math.round(NewSerumConfig.GUARD_SECONDS.get() * 20.0D));
        HeroFx.play(player, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.2F);
        PlayerData.sync(player);
        return NewSerumConfig.GUARD_COOLDOWN.get();
    }

    private static double lasso(LivingEntity player, int slot) {
        ServerLevel level = HeroCaster.level(player);
        double range = NewSerumConfig.LASSO_RANGE.get();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 1);
        Vec3 hand = player.getEyePosition().subtract(0.0D, 0.4D, 0.0D);
        if (!beam.targets().isEmpty()) {
            LivingEntity target = beam.targets().get(0);
            if (!HeroFx.beam(level, hand, target.getBoundingBox().getCenter(), "harpoon_beam", 0xE0000000 | SerumKit.primary(player, LASSO_COLOR) & 0xFFFFFF)) {
                HeroAbilities.particleLine(level, hand, target.getBoundingBox().getCenter(), SerumKit.glow(player, LASSO_COLOR, 0.7F), 0.4D);
            }
            if (HeroAbilities.canControl(target)) {
                Vec3 pull = player.position().subtract(target.position());
                Vec3 velocity = pull.normalize().scale(Math.min(2.0D, 0.5D + pull.length() * 0.12D));
                target.setDeltaMovement(velocity.x, Math.max(0.3D, velocity.y + 0.2D), velocity.z);
                target.hurtMarked = true;
                int ticks = NewSerumConfig.LASSO_PACIFY_TICKS.get();
                HeroStun.stun(target, ticks);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
                if (target instanceof Mob mob) {
                    mob.setTarget(null);
                }
                if (target instanceof ServerPlayer victim) {
                    victim.displayClientMessage(Component.literal("§6The lasso tightens. You don't feel like fighting."), true);
                    InvisibleWomanPower.reveal(victim);
                }
                target.removeEffect(MobEffects.INVISIBILITY);
            }
            HeroFx.play(player, SoundEvents.LEASH_KNOT_PLACE, 1.2F, 0.8F);
            HeroCaster.message(player, Component.literal("§6" + SerumKit.describe(target)));
            HeroAnimations.play(player, "web_shoot_both");
            return NewSerumConfig.LASSO_COOLDOWN.get();
        }
        if (!(player instanceof ServerPlayer user)) {
            return 0.0D;
        }
        Vec3 anchor = HeroGrapple.anchor(user, range);
        if (anchor == null) {
            return 0.0D;
        }
        HeroGrapple.start(user, anchor, SerumKit.glow(player, LASSO_COLOR, 0.7F));
        HeroFx.play(player, SoundEvents.LEASH_KNOT_PLACE, 1.2F, 1.2F);
        HeroAnimations.play(player, "web_shoot_both");
        return NewSerumConfig.LASSO_COOLDOWN.get() * 0.5D;
    }

    private static net.minecraft.world.damagesource.DamageSource attack(LivingEntity caster) {
        return caster instanceof net.minecraft.world.entity.player.Player player ? caster.damageSources().playerAttack(player) : caster.damageSources().mobAttack(caster);
    }

    private static double braceletClash(LivingEntity player, int slot) {
        ServerLevel level = HeroCaster.level(player);
        double radius = NewSerumConfig.BRACELET_CLASH_RADIUS.get();
        float damage = NewSerumConfig.BRACELET_CLASH_DAMAGE.get().floatValue();
        int hits = 0;
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(attack(player), damage));
            SerumKit.push(target, player.position(), 1.6D, 0.5D);
            hits++;
        }
        Vec3 center = player.position().add(0.0D, 1.2D, 0.0D);
        if (HeroFx.vfx(level, player.position(), "powergirl_slam_wave", 0, (float) radius)) {
            HeroFx.vfx(level, center, "impact_starburst", 0, 2.5F);
            HeroFx.cue(player, "thematic:thunder_clap", 1.2F, 1.0F);
        } else {
            level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            for (int i = 1; i <= 3; i++) {
                SerumKit.ring(level, center, SerumKit.glow(player, 0xF2F2F2, 1.0F), radius * i / 3.0D, 12 * i);
            }
            HeroFx.play(player, SoundEvents.GENERIC_EXPLODE.value(), 0.6F, 1.6F);
        }
        HeroFx.play(player, SoundEvents.ANVIL_LAND, 1.2F, 1.4F);
        HeroFx.shake(level, player.position(), 12.0D, 0.5F, 8);
        if (!ViltrumiteAnimations.play(player, ViltrumiteAnimations.THUNDERCLAP)) {
            HeroAnimations.play(player, "bettercombat:two_handed_slam");
        }
        if (hits >= 5 && player instanceof ServerPlayer user) {
            HeroAdvancements.award(user, "justice_league/amazon/clash_of_titans");
        }
        return NewSerumConfig.BRACELET_CLASH_COOLDOWN.get();
    }
}
