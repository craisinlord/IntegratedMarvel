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
    private static final java.util.Map<java.util.UUID, Long> CAUGHT = new java.util.HashMap<>();

    private AmazonPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(AmazonPower::onProjectile);
        }
        return new HeroPower(
                ID,
                "Amazon (Diana Prince)",
                syringe,
                NewSerumConfig.AMAZON,
                List.of("agility"),
                List.of(
                        new HeroPower.Ability("lasso_of_truth", "Lasso of Truth", AmazonPower::lasso),
                        new HeroPower.Ability("bracelet_clash", "Bracelet Clash", AmazonPower::braceletClash),
                        new HeroPower.Ability("bracelets_up", "Bracelets Up", AmazonPower::guard),
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

    private static boolean inFront(Player player, Vec3 from) {
        Vec3 to = from.subtract(player.getEyePosition());
        Vec3 flat = new Vec3(to.x, 0.0D, to.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            return true;
        }
        Vec3 look = player.getLookAngle();
        return flat.normalize().dot(new Vec3(look.x, 0.0D, look.z).normalize()) > 0.2D;
    }

    private static void onProjectile(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer player)
                || !isGuarding(player)) {
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
        ServerLevel level = player.serverLevel();
        if (!HeroFx.vfx(level, projectile.position(), "impact_starburst", 0)) {
            level.sendParticles(ParticleTypes.CRIT, projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.2D);
        }
        HeroFx.play(player, SoundEvents.ANVIL_PLACE, 0.5F, 2.0F);
        HeroAdvancements.count(player, "justice_league/amazon/bulletproof", 1.0D, 25.0D);
    }

    private static double guard(ServerPlayer player, int slot) {
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

    private static double lasso(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
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
            player.displayClientMessage(Component.literal("§6" + SerumKit.describe(target)), true);
            HeroAnimations.play(player, "web_shoot_both");
            return NewSerumConfig.LASSO_COOLDOWN.get();
        }
        Vec3 anchor = HeroGrapple.anchor(player, range);
        if (anchor == null) {
            return 0.0D;
        }
        HeroGrapple.start(player, anchor, SerumKit.glow(player, LASSO_COLOR, 0.7F));
        HeroFx.play(player, SoundEvents.LEASH_KNOT_PLACE, 1.2F, 1.2F);
        HeroAnimations.play(player, "web_shoot_both");
        return NewSerumConfig.LASSO_COOLDOWN.get() * 0.5D;
    }

    private static double braceletClash(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        double radius = NewSerumConfig.BRACELET_CLASH_RADIUS.get();
        float damage = NewSerumConfig.BRACELET_CLASH_DAMAGE.get().floatValue();
        int hits = 0;
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
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
        if (hits >= 5) {
            HeroAdvancements.award(player, "justice_league/amazon/clash_of_titans");
        }
        return NewSerumConfig.BRACELET_CLASH_COOLDOWN.get();
    }
}
