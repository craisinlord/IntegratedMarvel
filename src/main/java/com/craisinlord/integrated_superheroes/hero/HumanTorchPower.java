package com.craisinlord.integrated_superheroes.hero;

import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class HumanTorchPower {
    public static final String ID = "flame_on_johnny_storm";
    public static final String FLAME_STATE = "integrated_superheroes_flame";
    private static final String FLAME_ON = "flame_on";
    private static final String FIREBALL_TAG = "integrated_superheroes_torch_fireball";
    private static final Map<UUID, Cage> CAGES = new HashMap<>();
    private static final ResourceLocation FLIGHT_ID = ResourceLocation.fromNamespaceAndPath("integrated_superheroes", "torch_flight");

    private HumanTorchPower() {
    }

    private static final class Cage {
        private final LivingEntity target;
        private int ticks;

        private Cage(LivingEntity target, int ticks) {
            this.target = target;
            this.ticks = ticks;
        }
    }

    private static boolean hostile(ServerPlayer player, LivingEntity entity) {
        if (entity instanceof TamableAnimal tamable && tamable.isTame() && player.getUUID().equals(tamable.getOwnerUUID())) {
            return false;
        }
        return !(entity instanceof Player other) || player.canHarmPlayer(other);
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Flame On (Johnny Storm)",
                syringe,
                MarvelBalanceConfig.TORCH,
                List.of(),
                List.of(
                        new HeroPower.Ability(FLAME_ON, "Flame On", HumanTorchPower::flameOn),
                        new HeroPower.Ability("fireball", "Fireball", HumanTorchPower::fireball),
                        new HeroPower.Ability("flame_cage", "Flame Cage", HumanTorchPower::flameCage),
                        new HeroPower.Ability("nova_flare", "Nova Flare", HumanTorchPower::novaFlare)
                ),
                List.of(),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        tickCage(player);
                        boolean flamed = isFlamed(player);
                        if (flamed && player.isInWaterRainOrBubble()) {
                            flameOut(player, MarvelBalanceConfig.FLAME_OUT_COOLDOWN.get());
                            player.displayClientMessage(Component.literal("§7Flame out!"), true);
                            HeroFx.play(player, SoundEvents.FIRE_EXTINGUISH, 1.0F, 1.0F);
                            flamed = false;
                        }
                        HeroFlight.tick(player, FLIGHT_ID, flamed, MarvelBalanceConfig.TORCH_FLY_SPEED.get().floatValue());
                        if (!flamed) {
                            if (player.getRemainingFireTicks() > 0) {
                                player.clearFire();
                            }
                            return;
                        }
                        ServerLevel level = player.serverLevel();
                        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
                        level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 3, 0.3D, 0.5D, 0.3D, 0.01D);
                        if (player.getAbilities().flying) {
                            level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY(), player.getZ(), 1, 0.1D, 0.1D, 0.1D, 0.0D);
                        }
                        if (player.tickCount % 10 == 0) {
                            for (LivingEntity touched : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(0.6D),
                                    entity -> entity != player && entity.isAlive() && !entity.fireImmune() && hostile(player, entity))) {
                                touched.igniteForSeconds(4.0F);
                            }
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroPowers.data(player).removeState(FLAME_STATE);
                        HeroFlight.remove(player, FLIGHT_ID);
                        CAGES.remove(player.getUUID());
                        player.clearFire();
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                            event.setCanceled(true);
                            player.clearFire();
                        }
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (isFlamed(player) && event.getSource().getDirectEntity() == player) {
                            event.getEntity().igniteForSeconds(5.0F);
                        }
                    }
                }
        );
    }

    public static boolean isFlamed(Player player) {
        PlayerData data = HeroPowers.data(player);
        return ID.equals(data.getPrimaryPower()) && data.hasState(FLAME_STATE);
    }

    public static boolean showsFlames(Entity entity) {
        return entity instanceof Player player && player.hasData(com.stxllarplanes.marvellegacy.init.AttachmentsRegistry.PLAYER_DATA.get())
                && isFlamed(player);
    }

    private static void flameOut(ServerPlayer player, double cooldown) {
        HeroPowers.data(player).removeState(FLAME_STATE);
        PlayerData.sync(player);
        HeroAbilities.setCooldown(player, FLAME_ON, cooldown);
    }

    private static boolean needFlame(ServerPlayer player) {
        if (isFlamed(player)) {
            return true;
        }
        player.displayClientMessage(Component.literal("§7Flame on first."), true);
        return false;
    }

    private static double flameOn(ServerPlayer player, int slot) {
        PlayerData data = HeroPowers.data(player);
        if (data.hasState(FLAME_STATE)) {
            data.removeState(FLAME_STATE);
            HeroFx.play(player, SoundEvents.FIRE_EXTINGUISH, 0.8F, 1.2F);
        } else {
            if (player.isInWaterRainOrBubble()) {
                player.displayClientMessage(Component.literal("§7Too wet to flame on."), true);
            HeroAdvancements.award(player, "fantastic_four/human_torch/flame_off");
                return 0.0D;
            }
            data.addState(FLAME_STATE);
            ServerLevel level = player.serverLevel();
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.4D, 0.9D, 0.4D, 0.08D);
            Vec3 center = player.getBoundingBox().getCenter();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.defaultBlockState()),
                    center.x, center.y, center.z, 40, 0.4D, 0.8D, 0.4D, 0.2D);
            HeroFx.play(player, SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F);
            HeroFx.play(player, SoundEvents.BLAZE_SHOOT, 0.8F, 0.7F);
            player.displayClientMessage(Component.literal("§6Flame on!"), true);
        }
        PlayerData.sync(player);
        return 1.0D;
    }

    private static double fireball(ServerPlayer player, int slot) {
        if (!needFlame(player)) {
            return 0.0D;
        }
        Vec3 look = player.getLookAngle();
        SmallFireball fireball = new SmallFireball(player.level(), player, look.scale(1.6D));
        Vec3 start = player.getEyePosition().add(look.scale(0.8D)).subtract(0.0D, 0.2D, 0.0D);
        fireball.setPos(start.x, start.y, start.z);
        fireball.addTag(FIREBALL_TAG);
        player.level().addFreshEntity(fireball);
        HeroFx.play(player, SoundEvents.BLAZE_SHOOT, 0.8F, 1.2F);
        HeroAnimations.play(player, player.getRandom().nextBoolean() ? "web_shoot_r" : "web_shoot_l");
        return MarvelBalanceConfig.FIREBALL_COOLDOWN.get();
    }

    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof SmallFireball fireball) || !fireball.getTags().contains(FIREBALL_TAG)
                || !(fireball.level() instanceof ServerLevel level)) {
            return;
        }
        HitResult hit = event.getRayTraceResult();
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() == fireball.getOwner()) {
            event.setCanceled(true);
            return;
        }
        if (hit instanceof EntityHitResult entityHit && fireball.getOwner() instanceof ServerPlayer owner
                && entityHit.getEntity() instanceof LivingEntity target) {
            float damage = MarvelBalanceConfig.FIREBALL_DAMAGE.get().floatValue();
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(owner.damageSources().fireball(fireball, owner), damage));
            target.igniteForSeconds(5.0F);
        }
        Vec3 at = hit.getLocation();
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 12, 0.2D, 0.2D, 0.2D, 0.05D);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
        fireball.discard();
        event.setCanceled(true);
    }

    private static double flameCage(ServerPlayer player, int slot) {
        if (!needFlame(player)) {
            return 0.0D;
        }
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), MarvelBalanceConfig.FLAME_CAGE_RANGE.get(), 1);
        if (beam.targets().isEmpty()) {
            return 0.0D;
        }
        LivingEntity target = beam.targets().get(0);
        int ticks = MarvelBalanceConfig.FLAME_CAGE_TICKS.get();
        CAGES.put(player.getUUID(), new Cage(target, ticks));
        if (HeroAbilities.canControl(target)) {
            HeroStun.stun(target, ticks);
        }
        target.igniteForSeconds(ticks / 20.0F + 3.0F);
        HeroAbilities.particleLine(player.serverLevel(), player.getEyePosition().subtract(0.0D, 0.3D, 0.0D), target.getBoundingBox().getCenter(),
                ParticleTypes.FLAME, 0.5D);
        HeroFx.play(target, SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
        HeroAnimations.play(player, "web_shoot_r");
        return MarvelBalanceConfig.FLAME_CAGE_COOLDOWN.get();
    }

    private static void tickCage(ServerPlayer player) {
        Cage cage = CAGES.get(player.getUUID());
        if (cage == null) {
            return;
        }
        LivingEntity target = cage.target;
        if (target.isDeadOrDying() && target.level() == player.level()) {
            HeroAdvancements.award(player, "fantastic_four/human_torch/hot_box");
        }
        if (!target.isAlive() || target.level() != player.level() || cage.ticks-- <= 0) {
            CAGES.remove(player.getUUID());
            return;
        }
        ServerLevel level = player.serverLevel();
        double radius = Math.max(1.0D, target.getBbWidth() * 0.9D + 0.6D);
        double spin = cage.ticks * 0.25D;
        for (int i = 0; i < 12; i++) {
            double angle = spin + Math.PI * 2.0D * i / 12.0D;
            double height = (i % 3) * target.getBbHeight() / 3.0D;
            level.sendParticles(ParticleTypes.FLAME, target.getX() + Math.cos(angle) * radius, target.getY() + height,
                    target.getZ() + Math.sin(angle) * radius, 1, 0.0D, 0.05D, 0.0D, 0.0D);
        }
        if (cage.ticks % 20 == 0) {
            float damage = MarvelBalanceConfig.FLAME_CAGE_DAMAGE.get().floatValue();
            target.invulnerableTime = 0;
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
            target.igniteForSeconds(3.0F);
        }
    }

    private static double novaFlare(ServerPlayer player, int slot) {
        if (!needFlame(player)) {
            return 0.0D;
        }
        ServerLevel level = player.serverLevel();
        double radius = MarvelBalanceConfig.NOVA_RADIUS.get();
        float damage = MarvelBalanceConfig.NOVA_DAMAGE.get().floatValue();
        HeroAdvancements.cast(player).kills(5, "fantastic_four/human_torch/supernova").run(() -> {
            for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
                HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
                target.igniteForSeconds(8.0F);
                Vec3 push = target.position().subtract(player.position()).normalize().scale(1.4D);
                target.setDeltaMovement(push.x, 0.5D, push.z);
                target.hurtMarked = true;
            }
        });
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2.0D * i / 48.0D;
            level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 0, Math.cos(angle), 0.05D, Math.sin(angle), 0.6D);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z, 20, 1.0D, 0.5D, 1.0D, 0.0D);
        HeroFx.shake(level, center, radius * 2.0D, 0.8F, 20);
        HeroFx.play(player, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.8F);
        HeroFx.play(player, SoundEvents.FIRECHARGE_USE, 1.2F, 0.5F);
        HeroAnimations.play(player, "web_shoot_both");
        flameOut(player, MarvelBalanceConfig.NOVA_BURNOUT_SECONDS.get());
        player.displayClientMessage(Component.literal("§6Nova flare! §7Burned out."), true);
        return MarvelBalanceConfig.NOVA_COOLDOWN.get();
    }
}
