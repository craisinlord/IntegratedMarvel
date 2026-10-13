package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class HulkPower {
    public static final String ID = "gamma_mutation_bruce_banner";
    public static final String HULK_STATE = "integrated_superheroes_hulk";
    private static final String HULK_OUT = "hulk_out";
    public static final HeroPower.Toggle GAMMA_LEAP = new HeroPower.Toggle("integrated_superheroes_off_jump", "Gamma Leap",
            () -> ModCompat.MUTANT_MONSTERS ? HeroFx.item("mutantmonsters:hulk_hammer") : Items.MACE);
    private static final Map<Holder<MobEffect>, Integer> HULK_EFFECTS = Map.of(MobEffects.FIRE_RESISTANCE, 0, MobEffects.MOVEMENT_SPEED, 0);
    private static final Map<Holder<MobEffect>, HeroPower.Toggle> EFFECT_TOGGLES = Map.of(MobEffects.MOVEMENT_SPEED, HeroToggles.SPEED);
    private static final Map<UUID, Rage> RAGE = new HashMap<>();
    private static final Map<UUID, Leap> SERVER_LEAPS = new HashMap<>();
    private static final Map<UUID, Leap> CLIENT_LEAPS = new HashMap<>();
    private static final Map<UUID, Long> SERVER_LAST_LEAP = new HashMap<>();
    private static final Map<UUID, Long> CLIENT_LAST_LEAP = new HashMap<>();
    private static final Map<UUID, Long> LAST_STAND_READY = new HashMap<>();
    private static volatile int clientRage;

    private HulkPower() {
    }

    private static final class Rage {
        private float value;
        private long lastCombat;
        private int sent = -1;
        private boolean unstoppable;
    }

    private record Leap(Vec3 horizontal, int age, double peakY) {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Gamma Mutation (Bruce Banner)",
                syringe,
                MarvelBalanceConfig.HULK,
                List.of(),
                List.of(
                        new HeroPower.Ability(HULK_OUT, "Hulk Out", HulkPower::hulkOut),
                        new HeroPower.Ability("thunderclap", "Thunderclap", HulkPower::thunderclap),
                        new HeroPower.Ability("hulk_smash", "Hulk Smash", HulkPower::hulkSmash),
                        HeroToggles.WHEEL
                ),
                List.of(GAMMA_LEAP, HeroToggles.SPEED),
                new HeroPower.Hooks() {
                    @Override
                    public MarvelBalanceConfig.HeroStats stats(Player player) {
                        return isHulk(player) ? MarvelBalanceConfig.HULK : MarvelBalanceConfig.BANNER;
                    }

                    @Override
                    public void tick(ServerPlayer player) {
                        tickRage(player);
                        if (player.tickCount % 20 == 0) {
                            if (isHulk(player)) {
                                HeroToggles.applyEffects(player, HULK_EFFECTS, EFFECT_TOGGLES);
                            } else {
                                removeHulkEffects(player);
                            }
                        }
                        if (isHulk(player)) {
                            tickFootsteps(player);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        PlayerData data = HeroPowers.data(player);
                        data.removeState(HULK_STATE);
                        removeHulkEffects(player);
                        RAGE.remove(player.getUUID());
                        SERVER_LEAPS.remove(player.getUUID());
                        HeroStride.clear(player);
                        PacketDistributor.sendToPlayer(player, new HulkRagePayload(0));
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypeTags.IS_FALL) && (isHulk(player) || SERVER_LEAPS.containsKey(player.getUUID()))) {
                            event.setCanceled(true);
                            return;
                        }
                        addRage(player, event.getAmount() / 2.0F * MarvelBalanceConfig.RAGE_PER_HEART.get().floatValue());
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (!isHulk(player)) {
                            return;
                        }
                        if (event.getSource().getDirectEntity() == player && !HeroAbilities.isAbilityDamage()) {
                            addRage(player, MarvelBalanceConfig.RAGE_PER_HIT.get().floatValue());
                        }
                        event.setAmount(event.getAmount() * rageMultiplier(player));
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        if (lastStand(player)) {
                            event.setCanceled(true);
                            return;
                        }
                        RAGE.remove(player.getUUID());
                        PacketDistributor.sendToPlayer(player, new HulkRagePayload(0));
                    }

                    @Override
                    public void jump(Player player) {
                        startLeap(player);
                    }
                }
        );
    }

    public static boolean isHulk(Player player) {
        PlayerData data = HeroPowers.data(player);
        return ID.equals(data.getPrimaryPower()) && data.hasState(HULK_STATE);
    }

    private static void removeHulkEffects(ServerPlayer player) {
        for (Holder<MobEffect> effect : HULK_EFFECTS.keySet()) {
            MobEffectInstance current = player.getEffect(effect);
            if (current != null && current.isInfiniteDuration() && current.isAmbient()) {
                player.removeEffect(effect);
            }
        }
    }

    public static void setClientRage(int rage) {
        clientRage = rage;
    }

    private static double rageCooldown(Player player, double cooldown) {
        float rage;
        if (player.level().isClientSide) {
            rage = clientRage;
        } else {
            Rage state = RAGE.get(player.getUUID());
            rage = state == null ? 0.0F : state.value;
        }
        return cooldown * (1.0D - MarvelBalanceConfig.RAGE_COOLDOWN_REDUCTION.get() * rage / 100.0D);
    }

    private static boolean lastStand(ServerPlayer player) {
        if (isHulk(player)) {
            return false;
        }
        long now = player.level().getGameTime();
        Long ready = LAST_STAND_READY.get(player.getUUID());
        if (ready != null && now < ready) {
            return false;
        }
        if (!transform(player, true)) {
            return false;
        }
        LAST_STAND_READY.put(player.getUUID(), now + (long) (MarvelBalanceConfig.LAST_STAND_COOLDOWN.get() * 20.0D));
        Rage rage = RAGE.computeIfAbsent(player.getUUID(), ignored -> new Rage());
        rage.value = 100.0F;
        rage.lastCombat = now;
        player.setHealth(player.getMaxHealth() * 0.5F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3));
        player.displayClientMessage(Component.literal("\u00a72HULK SMASH."), true);
        return true;
    }

    private static float rageMultiplier(ServerPlayer player) {
        Rage rage = RAGE.get(player.getUUID());
        return rage == null ? 1.0F : 1.0F + MarvelBalanceConfig.RAGE_DAMAGE_BONUS.get().floatValue() * rage.value / 100.0F;
    }

    private static void addRage(ServerPlayer player, float amount) {
        Rage rage = RAGE.computeIfAbsent(player.getUUID(), ignored -> new Rage());
        rage.value = Math.min(100.0F, rage.value + Math.max(0.0F, amount));
        rage.lastCombat = player.level().getGameTime();
    }

    private static void tickRage(ServerPlayer player) {
        Rage rage = RAGE.computeIfAbsent(player.getUUID(), ignored -> new Rage());
        long now = player.level().getGameTime();
        if (now - rage.lastCombat > MarvelBalanceConfig.RAGE_DECAY_DELAY_TICKS.get() && rage.value > 0.0F) {
            rage.value = Math.max(0.0F, rage.value - MarvelBalanceConfig.RAGE_DECAY_PER_SECOND.get().floatValue() / 20.0F);
        }
        if (rage.value >= 100.0F) {
            rage.unstoppable = true;
        } else if (rage.value < 80.0F) {
            rage.unstoppable = false;
        }
        if (rage.value >= 100.0F && !isHulk(player) && MarvelBalanceConfig.RAGE_FORCES_HULK_OUT.get() && player.tickCount % 10 == 0) {
            transform(player, true);
        }
        if (rage.unstoppable && isHulk(player) && player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, true, false, true));
        }
        int strength = (int) (rage.value / 100.0F * MarvelBalanceConfig.RAGE_MAX_STRENGTH_LEVEL.get());
        if (strength > 0 && isHulk(player) && player.tickCount % 10 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 25, strength - 1, true, true, true));
        }
        int rounded = Math.round(rage.value);
        if (rounded != rage.sent) {
            rage.sent = rounded;
            PacketDistributor.sendToPlayer(player, new HulkRagePayload(rounded));
        }
    }

    private static void tickFootsteps(ServerPlayer player) {
        Rage rage = RAGE.get(player.getUUID());
        float fury = rage == null ? 0.0F : rage.value / 100.0F;
        HeroStride.tick(player, 2.4D, SoundEvents.RAVAGER_STEP, 1.4F, 0.6F, 12, 0.12F + fury * 0.5F);
    }

    private static boolean wearsPymSuit(ServerPlayer player) {
        var key = BuiltInRegistries.ITEM.getKey(player.getItemBySlot(EquipmentSlot.CHEST).getItem());
        return "marvel".equals(key.getNamespace()) && (key.getPath().contains("ant_man") || key.getPath().contains("wasp"));
    }

    private static boolean transform(ServerPlayer player, boolean forced) {
        if (wearsPymSuit(player)) {
            if (!forced) {
                player.displayClientMessage(Component.literal("§cThe suit won't stretch that far."), true);
                HeroAdvancements.award(player, "avengers/hulk/wardrobe_malfunction");
            }
            return false;
        }
        double width = player.getBbWidth() * 1.5D;
        double height = player.getBbHeight() * 1.5D;
        AABB grown = new AABB(player.getX() - width / 2.0D, player.getY(), player.getZ() - width / 2.0D,
                player.getX() + width / 2.0D, player.getY() + height, player.getZ() + width / 2.0D);
        if (!player.level().noCollision(player, grown)) {
            if (!forced) {
                player.displayClientMessage(Component.literal("§cNot enough room to Hulk out."), true);
            }
            return false;
        }
        HeroPowers.data(player).addState(HULK_STATE);
        PlayerData.sync(player);
        HeroAttributes.apply(player, MarvelBalanceConfig.HULK);
        HeroAnimations.play(player, "uppercut");
        HeroFx.play(player, HeroFx.sound("mutantmonsters:entity.mutant_zombie.roar", SoundEvents.RAVAGER_ROAR), 2.0F, 0.9F);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.5D, player.getZ(), 4, 0.6D, 0.2D, 0.6D, 0.0D);
        HeroFx.shake(level, player.position(), 24.0D, 0.6F, 12);
        if (forced) {
            player.displayClientMessage(Component.literal("§2You won't like him when he's angry."), true);
            HeroAbilities.setCooldown(player, HULK_OUT, MarvelBalanceConfig.HULK_OUT_COOLDOWN.get());
        }
        return true;
    }

    private static void revert(ServerPlayer player) {
        HeroPowers.data(player).removeState(HULK_STATE);
        PlayerData.sync(player);
        HeroAttributes.apply(player, MarvelBalanceConfig.BANNER);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        removeHulkEffects(player);
        HeroFx.play(player, SoundEvents.PLAYER_BREATH, 1.0F, 0.8F);
    }

    private static double hulkOut(ServerPlayer player, int slot) {
        if (isHulk(player)) {
            revert(player);
            return MarvelBalanceConfig.HULK_OUT_COOLDOWN.get();
        }
        return transform(player, false) ? MarvelBalanceConfig.HULK_OUT_COOLDOWN.get() : 0.0D;
    }

    private static boolean requireHulk(ServerPlayer player) {
        if (isHulk(player)) {
            return true;
        }
        player.displayClientMessage(Component.literal("§7Banner can't do that. Hulk out first."), true);
        return false;
    }

    private static double thunderclap(ServerPlayer player, int slot) {
        if (!requireHulk(player)) {
            return 0.0D;
        }
        double range = MarvelBalanceConfig.THUNDERCLAP_RANGE.get();
        float damage = MarvelBalanceConfig.THUNDERCLAP_DAMAGE.get().floatValue();
        Vec3 look = player.getLookAngle();
        HeroAdvancements.cast(player).hits(8, "avengers/hulk/thunderclap").run(() -> {
            for (LivingEntity target : HeroAbilities.cone(player, range, 50.0D)) {
                target.hurt(player.damageSources().mobAttack(player), damage);
                target.knockback(3.0D, -look.x, -look.z);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
        });
        ServerLevel level = player.serverLevel();
        Vec3 front = player.getEyePosition().add(look.scale(1.5D));
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, front.x, front.y - 0.3D, front.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        HeroAbilities.particleLine(level, front, front.add(look.scale(range * 0.8D)), ParticleTypes.SONIC_BOOM, 2.5D);
        HeroFx.play(player, SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 0.6F);
        HeroAnimations.play(player, "bettercombat:two_handed_slam");
        HeroFx.shake(level, player.position(), 24.0D, 0.7F, 10);
        addRage(player, MarvelBalanceConfig.RAGE_PER_IMPACT.get().floatValue());
        return rageCooldown(player, MarvelBalanceConfig.THUNDERCLAP_COOLDOWN.get());
    }

    private static double hulkSmash(ServerPlayer player, int slot) {
        if (!requireHulk(player)) {
            return 0.0D;
        }
        float damage = MarvelBalanceConfig.HULK_SMASH_DAMAGE.get().floatValue();
        HeroAdvancements.cast(player).kills(entity -> entity instanceof net.minecraft.world.entity.animal.IronGolem
                        || entity instanceof net.minecraft.world.entity.monster.Ravager, "avengers/hulk/puny")
                .run(() -> HeroShockwaves.ring(player, 8, MarvelBalanceConfig.HULK_SMASH_LENGTH.get(), damage));
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY(), player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        HeroFx.play(player, SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 0.6F);
        HeroFx.play(player, HeroFx.sound("mutantmonsters:entity.mutant_zombie.attack", SoundEvents.RAVAGER_ATTACK), 1.5F, 0.8F);
        HeroAnimations.play(player, "celestisynth:cs_keres_smash");
        HeroFx.shake(level, player.position(), 32.0D, 1.0F, 16);
        addRage(player, MarvelBalanceConfig.RAGE_PER_IMPACT.get().floatValue());
        return rageCooldown(player, MarvelBalanceConfig.HULK_SMASH_COOLDOWN.get());
    }

    private static void startLeap(Player player) {
        if (!isHulk(player) || !player.isCrouching() || !HeroToggles.isOn(player, GAMMA_LEAP)) {
            return;
        }
        boolean client = player.level().isClientSide;
        Map<UUID, Long> last = client ? CLIENT_LAST_LEAP : SERVER_LAST_LEAP;
        long now = player.level().getGameTime();
        Long previous = last.get(player.getUUID());
        if (previous != null && now - previous < rageCooldown(player, MarvelBalanceConfig.GAMMA_LEAP_COOLDOWN_TICKS.get())) {
            return;
        }
        last.put(player.getUUID(), now);
        Vec3 motion = player.getDeltaMovement();
        Vec3 flat = new Vec3(motion.x, 0.0D, motion.z);
        Vec3 horizontal;
        double vertical;
        if (flat.lengthSqr() > 0.0009D) {
            Vec3 look = player.getLookAngle();
            Vec3 direction = new Vec3(look.x, 0.0D, look.z).normalize();
            horizontal = direction.scale(MarvelBalanceConfig.GAMMA_LEAP_HORIZONTAL.get());
            vertical = MarvelBalanceConfig.GAMMA_LEAP_VERTICAL.get();
        } else {
            horizontal = Vec3.ZERO;
            vertical = MarvelBalanceConfig.GAMMA_LEAP_STANDING_VERTICAL.get();
        }
        if (client) {
            if (player.isLocalPlayer()) {
                player.setDeltaMovement(horizontal.x, vertical, horizontal.z);
                CLIENT_LEAPS.put(player.getUUID(), new Leap(horizontal, 0, player.getY()));
                HeroAnimations.requestFromClient(player, "celestisynth:cs_breezebreaker_jump");
            }
            return;
        }
        SERVER_LEAPS.put(player.getUUID(), new Leap(horizontal, 0, player.getY()));
        if (player instanceof ServerPlayer serverPlayer) {
            ServerLevel level = serverPlayer.serverLevel();
            BlockState ground = level.getBlockState(serverPlayer.blockPosition().below());
            if (!ground.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), player.getX(), player.getY() + 0.1D, player.getZ(),
                        40, 1.0D, 0.1D, 1.0D, 0.3D);
            }
            HeroFx.play(player, SoundEvents.RAVAGER_ROAR, 1.2F, 0.7F);
            HeroFx.shake(level, player.position(), 16.0D, 0.4F, 8);
        }
    }

    public static void tickMovement(Player player) {
        if (player.level().isClientSide) {
            Leap leap = CLIENT_LEAPS.get(player.getUUID());
            if (leap == null) {
                return;
            }
            if (leap.age() > 2 && player.onGround() || leap.age() > 200 || player.isInWater() || !isHulk(player)) {
                CLIENT_LEAPS.remove(player.getUUID());
                return;
            }
            if (leap.horizontal().lengthSqr() > 0.0D) {
                Vec3 look = player.getLookAngle();
                Vec3 steer = new Vec3(look.x, 0.0D, look.z).normalize().scale(leap.horizontal().length());
                Vec3 horizontal = leap.horizontal().scale(0.92D).add(steer.scale(0.08D));
                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(horizontal.x, motion.y, horizontal.z);
                CLIENT_LEAPS.put(player.getUUID(), new Leap(horizontal, leap.age() + 1, leap.peakY()));
            } else {
                CLIENT_LEAPS.put(player.getUUID(), new Leap(leap.horizontal(), leap.age() + 1, leap.peakY()));
            }
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Leap leap = SERVER_LEAPS.get(player.getUUID());
        if (leap == null) {
            return;
        }
        double peak = Math.max(leap.peakY(), player.getY());
        if (leap.age() > 4 && player.onGround() || player.isInWater()) {
            SERVER_LEAPS.remove(player.getUUID());
            land(serverPlayer, peak - player.getY());
            return;
        }
        if (leap.age() > 400) {
            SERVER_LEAPS.remove(player.getUUID());
            return;
        }
        SERVER_LEAPS.put(player.getUUID(), new Leap(leap.horizontal(), leap.age() + 1, peak));
    }

    private static void land(ServerPlayer player, double drop) {
        if (!isHulk(player)) {
            return;
        }
        double scale = Math.min(MarvelBalanceConfig.GAMMA_LANDING_DAMAGE_MULTIPLIER.get(), 0.4D + drop / 20.0D);
        float damage = (float) (MarvelBalanceConfig.GAMMA_LANDING_DAMAGE.get() * scale);
        double radius = MarvelBalanceConfig.GAMMA_LANDING_RADIUS.get();
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            target.hurt(player.damageSources().mobAttack(player), damage);
            Vec3 push = target.position().subtract(player.position()).normalize();
            target.setDeltaMovement(push.x * 1.2D, 0.7D, push.z * 1.2D);
            target.hurtMarked = true;
        }
        HeroShockwaves.ring(player, 12, (int) Math.ceil(radius), damage * 0.3F);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY(), player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        HeroFx.play(player, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.5F);
        HeroFx.shake(level, player.position(), 40.0D, (float) Math.min(1.4D, 0.5D + drop / 25.0D), 18);
        addRage(player, MarvelBalanceConfig.RAGE_PER_IMPACT.get().floatValue());
    }
}
