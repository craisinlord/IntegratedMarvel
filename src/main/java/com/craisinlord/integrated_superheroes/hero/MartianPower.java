package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.anthology.beam.BeamSettings;
import com.craisinlord.anthology.beam.BeamTerrainMode;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.MorphShapeshift;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class MartianPower {
    public static final String ID = "martian_jonn_jonzz";
    public static final String DENSE_STATE = "integrated_superheroes_martian_dense";
    private static final ResourceLocation FLIGHT_ID = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "martian_flight");
    private static final ResourceLocation DENSE_ARMOR = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "martian_dense_armor");
    private static final ResourceLocation DENSE_TOUGHNESS = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "martian_dense_toughness");
    private static final ResourceLocation DENSE_KNOCKBACK = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "martian_dense_knockback");
    private static final ResourceLocation DENSE_SPEED = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "martian_dense_speed");
    private static final int MIND_COLOR = 0x3FE07A;
    private static final int VISION_COLOR = 0xFF2A2A;
    private static final String DENSITY = "density_shift";
    private static final String VANISH = "martian_vanish";
    private static final List<TagKey<EntityType<?>>> FACTIONS = List.of(EntityTypeTags.RAIDERS, EntityTypeTags.ILLAGER,
            EntityTypeTags.UNDEAD, EntityTypeTags.ZOMBIES, EntityTypeTags.SKELETONS, EntityTypeTags.ARTHROPOD);
    private static final Set<EntityType<?>> PIGLINS = Set.of(EntityType.PIGLIN, EntityType.PIGLIN_BRUTE);
    private static final Set<UUID> PHASING = ServerState.all(new HashSet<>());
    private static final Map<UUID, Long> DENSE_UNTIL = ServerState.map(new HashMap<>());
    private static final Map<UUID, Float> FALLING = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> CAMO_SEEN = ServerState.map(new HashMap<>());
    private static final Map<UUID, EntityType<?>> FORMS = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> BLOWN = ServerState.map(new HashMap<>());
    private static final Map<UUID, Thrall> THRALLS = ServerState.map(new HashMap<>());
    private static boolean registered;

    private record Thrall(UUID owner, long until) {
    }

    private MartianPower() {
    }

    public static void releaseThralls(ServerPlayer player) {
        THRALLS.values().removeIf(thrall -> thrall.owner().equals(player.getUUID()));
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(MartianPower::onTarget);
            HeroCaster.onTick(caster -> {
                tickDense(caster);
                if (caster.tickCount % 5 == 0) {
                    tickThralls(caster);
                }
            });
            HeroCaster.onCleanup(caster -> {
                endDense(caster);
                releaseThralls(caster);
            });
        }
        List<HeroPower.Ability> abilities = new ArrayList<>(List.of(
                HeroPower.Ability.of(DENSITY, "Density Shift", MartianPower::densityShift),
                new HeroPower.Ability(VANISH, "Camouflage", MartianPower::vanish),
                HeroPower.Ability.of("martian_vision", "Martian Vision", MartianPower::martianVision),
                HeroToggles.WHEEL,
                HeroPower.Ability.of("psychic_grip", "Psychic Grip", MartianPower::psychicGrip)
        ));
        if (ModCompat.MORPH) {
            abilities.add(new HeroPower.Ability("shapeshift", "Shapeshift", MartianPower::shapeshift));
        }
        return new HeroPower(
                ID,
                "Martian (J'onn J'onzz)",
                syringe,
                NewSerumConfig.MARTIAN,
                List.of("agility"),
                List.copyOf(abilities),
                HeroToggles.flying(),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        HeroFlight.tick(player, FLIGHT_ID, HeroToggles.isOn(player, HeroToggles.FLIGHT),
                                NewSerumConfig.MARTIAN_FLY_SPEED.get().floatValue());
                        tickPhase(player);
                        tickDense(player);
                        if (InvisibleWomanPower.isInvisible(player)) {
                            CAMO_SEEN.put(player.getUUID(), player.level().getGameTime());
                            if (player.tickCount % 10 == 0) {
                                InvisibleWomanPower.loseMobs(player);
                                if (KittyPower.isPhasing(player)) {
                                    HeroAdvancements.award(player, "justice_league/martian/not_here");
                                }
                            }
                        }
                        if (player.tickCount % 5 == 0) {
                            tickThralls(player);
                        }
                        if (player.tickCount % 10 == 0) {
                            updateForm(player);
                        }
                        if (player.isOnFire() && player.tickCount % 20 == 0) {
                            player.serverLevel().sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D,
                                    player.getZ(), 6, 0.3D, 0.6D, 0.3D, 0.02D);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroFlight.remove(player, FLIGHT_ID);
                        HeroBeams.stopAll(player);
                        PHASING.remove(player.getUUID());
                        if (KittyPower.isPhasing(player)) {
                            KittyPower.endPhase(player);
                        }
                        endDense(player);
                        HeroPowers.data(player).removeState(InvisibleWomanPower.INVISIBLE_STATE);
                        if (ModCompat.MORPH && MorphShapeshift.isMorphed(player)) {
                            MorphShapeshift.demorph(player);
                        }
                        releaseThralls(player);
                        FORMS.remove(player.getUUID());
                        BLOWN.remove(player.getUUID());
                        CAMO_SEEN.remove(player.getUUID());
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypeTags.IS_FALL)) {
                            event.setCanceled(true);
                            return;
                        }
                        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                            event.setAmount(event.getAmount() * NewSerumConfig.MARTIAN_FIRE_MULTIPLIER.get().floatValue());
                            if (InvisibleWomanPower.isInvisible(player)) {
                                InvisibleWomanPower.reveal(player);
                            }
                        }
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        LivingEntity victim = event.getEntity();
                        EntityType<?> form = FORMS.get(player.getUUID());
                        if (form != null && sameFaction(form, victim)) {
                            BLOWN.put(player.getUUID(), player.level().getGameTime() + NewSerumConfig.DISGUISE_BLOWN_TICKS.get());
                        }
                        boolean melee = event.getSource().getDirectEntity() == player && !HeroAbilities.isAbilityDamage();
                        if (!melee) {
                            return;
                        }
                        if (isDense(player)) {
                            event.setAmount(event.getAmount() * NewSerumConfig.DENSE_ATTACK_MULTIPLIER.get().floatValue());
                        }
                        Long seen = CAMO_SEEN.remove(player.getUUID());
                        if (seen != null && player.level().getGameTime() - seen <= 2L) {
                            event.setAmount(event.getAmount() * NewSerumConfig.CAMOUFLAGE_STRIKE_MULTIPLIER.get().floatValue());
                            InvisibleWomanPower.reveal(player);
                            HeroAbilities.setCooldown(player, VANISH, NewSerumConfig.MARTIAN_VANISH_COOLDOWN.get());
                            Vec3 center = victim.getBoundingBox().getCenter();
                            player.serverLevel().sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 12, 0.3D, 0.4D, 0.3D, 0.2D);
                            HeroFx.play(victim, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.7F);
                        }
                    }
                }
        );
    }

    private static int tint(LivingEntity player, int alpha) {
        return alpha | SerumKit.primary(player, MIND_COLOR) & 0xFFFFFF;
    }

    public static boolean isMartian(Player player) {
        return SerumKit.is(player, ID);
    }

    public static boolean isThrall(Entity entity) {
        return entity != null && THRALLS.containsKey(entity.getUUID());
    }

    public static boolean isDense(Player player) {
        return isMartian(player) && HeroPowers.data(player).hasState(DENSE_STATE);
    }

    private static boolean dense(LivingEntity caster) {
        return caster instanceof Player player ? isDense(player) : DENSE_UNTIL.containsKey(caster.getUUID());
    }

    private static void tickPhase(ServerPlayer player) {
        if (KittyPower.isPhasing(player)) {
            PHASING.add(player.getUUID());
        } else if (PHASING.remove(player.getUUID())) {
            HeroAbilities.setCooldown(player, DENSITY, NewSerumConfig.MARTIAN_PHASE_COOLDOWN.get());
        }
    }

    private static double densityShift(LivingEntity caster, int slot) {
        if (!(caster instanceof ServerPlayer player)) {
            if (dense(caster)) {
                endDense(caster);
                return NewSerumConfig.DENSE_COOLDOWN.get();
            }
            if (caster.isOnFire()) {
                return 0.0D;
            }
            startDense(caster);
            return NewSerumConfig.DENSE_TICKS.get() / 20.0D + NewSerumConfig.DENSE_COOLDOWN.get();
        }
        if (isDense(player)) {
            endDense(player);
            return NewSerumConfig.DENSE_COOLDOWN.get();
        }
        if (KittyPower.isPhasing(player)) {
            PHASING.remove(player.getUUID());
            KittyPower.endPhase(player);
            return NewSerumConfig.MARTIAN_PHASE_COOLDOWN.get();
        }
        if (player.isOnFire()) {
            player.displayClientMessage(Component.literal("§cYou can't focus while you're burning."), true);
            return 0.0D;
        }
        if (player.isShiftKeyDown()) {
            startDense(player);
            return 0.5D;
        }
        if (player.getY() < player.level().getMinBuildHeight() + 2) {
            return 0.0D;
        }
        KittyPower.timedPhase(player, NewSerumConfig.MARTIAN_PHASE_TICKS.get());
        PHASING.add(player.getUUID());
        Vec3 center = player.getBoundingBox().getCenter();
        if (HeroFx.vfx(player.serverLevel(), player.position(), "void_twist", tint(player, 0xD0000000))) {
            HeroFx.play(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.6F, 1.5F);
        } else {
            player.serverLevel().sendParticles(SerumKit.glow(player, MIND_COLOR, 1.0F), center.x, center.y, center.z, 20, 0.3D, 0.6D, 0.3D, 0.0D);
        }
        return 0.5D;
    }

    private static void startDense(LivingEntity player) {
        if (player instanceof ServerPlayer serverPlayer) {
            HeroPowers.data(serverPlayer).addState(DENSE_STATE);
            PlayerData.sync(serverPlayer);
        }
        DENSE_UNTIL.put(player.getUUID(), player.level().getGameTime() + NewSerumConfig.DENSE_TICKS.get());
        modifier(player, Attributes.ARMOR, DENSE_ARMOR, NewSerumConfig.DENSE_ARMOR.get(), AttributeModifier.Operation.ADD_VALUE);
        modifier(player, Attributes.ARMOR_TOUGHNESS, DENSE_TOUGHNESS, 6.0D, AttributeModifier.Operation.ADD_VALUE);
        modifier(player, Attributes.KNOCKBACK_RESISTANCE, DENSE_KNOCKBACK, 1.0D, AttributeModifier.Operation.ADD_VALUE);
        modifier(player, Attributes.MOVEMENT_SPEED, DENSE_SPEED, -0.3D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        Vec3 center = player.getBoundingBox().getCenter();
        HeroCaster.level(player).sendParticles(SerumKit.glow(player, MIND_COLOR, 1.4F), center.x, center.y, center.z, 24, 0.4D, 0.7D, 0.4D, 0.0D);
        HeroFx.play(player, SoundEvents.ANVIL_PLACE, 0.7F, 0.5F);
        HeroFx.play(player, SoundEvents.STONE_PLACE, 1.0F, 0.6F);
        HeroCaster.message(player, Component.literal("§2You pack yourself down. Nothing's moving you now."));
    }

    private static void endDense(LivingEntity player) {
        boolean wasMob = !(player instanceof ServerPlayer) && DENSE_UNTIL.containsKey(player.getUUID());
        DENSE_UNTIL.remove(player.getUUID());
        FALLING.remove(player.getUUID());
        unmodifier(player, Attributes.ARMOR, DENSE_ARMOR);
        unmodifier(player, Attributes.ARMOR_TOUGHNESS, DENSE_TOUGHNESS);
        unmodifier(player, Attributes.KNOCKBACK_RESISTANCE, DENSE_KNOCKBACK);
        unmodifier(player, Attributes.MOVEMENT_SPEED, DENSE_SPEED);
        if (wasMob) {
            HeroFx.play(player, SoundEvents.SLIME_SQUISH, 0.8F, 1.2F);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        PlayerData data = HeroPowers.data(serverPlayer);
        if (data.hasState(DENSE_STATE)) {
            data.removeState(DENSE_STATE);
            PlayerData.sync(serverPlayer);
            HeroFx.play(player, SoundEvents.SLIME_SQUISH, 0.8F, 1.2F);
        }
    }

    private static void tickDense(LivingEntity player) {
        if (!dense(player)) {
            return;
        }
        Long until = DENSE_UNTIL.get(player.getUUID());
        if (until == null || player.level().getGameTime() >= until) {
            endDense(player);
            if (player instanceof ServerPlayer serverPlayer) {
                HeroAbilities.setCooldown(serverPlayer, DENSITY, NewSerumConfig.DENSE_COOLDOWN.get());
            }
            return;
        }
        if (!player.onGround()) {
            FALLING.merge(player.getUUID(), player.fallDistance, Math::max);
        } else {
            Float fell = FALLING.remove(player.getUUID());
            if (fell != null && fell >= 4.0F) {
                float damage = NewSerumConfig.DENSE_SLAM_DAMAGE.get().floatValue();
                HeroAbilities.runAsAbilityDamage(() -> HeroShockwaves.ring(player, 8, 4, damage));
                HeroFx.shake(HeroCaster.level(player), player.position(), 12.0D, 0.5F, 8);
                HeroFx.play(player, SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
            }
        }
        if (player.tickCount % 8 == 0) {
            HeroCaster.level(player).sendParticles(SerumKit.glow(player, MIND_COLOR, 0.6F), player.getX(), player.getY() + 0.2D, player.getZ(),
                    3, 0.3D, 0.1D, 0.3D, 0.0D);
        }
    }

    private static void modifier(LivingEntity player, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void unmodifier(LivingEntity player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private static double vanish(ServerPlayer player, int slot) {
        PlayerData data = HeroPowers.data(player);
        if (data.hasState(InvisibleWomanPower.INVISIBLE_STATE)) {
            data.removeState(InvisibleWomanPower.INVISIBLE_STATE);
            PlayerData.sync(player);
            HeroFx.play(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 1.3F);
            return NewSerumConfig.MARTIAN_VANISH_COOLDOWN.get();
        }
        if (player.isOnFire()) {
            player.displayClientMessage(Component.literal("§cYou can't hold your camouflage while you're burning."), true);
            return 0.0D;
        }
        data.addState(InvisibleWomanPower.INVISIBLE_STATE);
        PlayerData.sync(player);
        InvisibleWomanPower.loseMobs(player);
        HeroFx.vfx(player.serverLevel(), player.position(), "void_depart", tint(player, 0xC0000000));
        HeroFx.play(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 0.7F);
        return 0.5D;
    }

    private static double martianVision(LivingEntity player, int slot) {
        double range = NewSerumConfig.MARTIAN_VISION_RANGE.get();
        BeamSettings settings = HeroBeams.settings(range, NewSerumConfig.MARTIAN_VISION_DURATION_TICKS.get(), 0.4F, 1.0F,
                NewSerumConfig.MARTIAN_VISION_DAMAGE.get(), 0.2F, 2, 0.0D, 0.0D, 0.0D);
        int color = player instanceof Player human ? SerumKit.secondary(human, VISION_COLOR) : VISION_COLOR;
        HeroBeams.Eye eye = HeroBeams.thematicEye(0xFFFFFF, color, 0.7F, "thematic:heat_vision", 20, "powergirl_heat_lock");
        HeroBeams.fire(player, 0, new HeroBeams.Spec(HeroBeams.QUEEN_RED, settings, BeamTerrainMode.NONE, -4.0F, 0.6D, 0.0D, 3, 0.0F), eye);
        HeroBeams.fire(player, 1, new HeroBeams.Spec(HeroBeams.QUEEN_RED, settings, BeamTerrainMode.NONE, 4.0F, 0.6D, 0.0D, 3, 0.0F), eye);
        if (eye == null) {
            HeroFx.play(player, SoundEvents.BEACON_ACTIVATE, 0.6F, 1.8F);
        }
        return NewSerumConfig.MARTIAN_VISION_COOLDOWN.get();
    }

    private static double psychicGrip(LivingEntity player, int slot) {
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), NewSerumConfig.PSYCHIC_GRIP_RANGE.get(), 1);
        if (beam.targets().isEmpty()) {
            HeroCaster.message(player, Component.literal("§7Look at a creature to reach into its mind."));
            return 0.0D;
        }
        LivingEntity target = beam.targets().get(0);
        ServerLevel level = HeroCaster.level(player);
        HeroCaster.message(player, Component.literal("§a" + SerumKit.describe(target)));
        Vec3 eye = player.getEyePosition();
        Vec3 center = target.getBoundingBox().getCenter();
        if (HeroFx.beam(level, eye.add(player.getLookAngle()), center, "sonar_beam", tint(player, 0xE0000000))) {
            HeroFx.vfx(level, target.position(), "void_arrive", tint(player, 0xB0000000), 1.0F);
        } else {
            HeroAbilities.particleLine(level, eye.add(player.getLookAngle()), center, SerumKit.glow(player, MIND_COLOR, 0.8F), 0.5D);
            SerumKit.ring(level, new Vec3(center.x, target.getEyeY() + 0.3D, center.z), SerumKit.glow(player, MIND_COLOR, 0.8F), 0.6D, 12);
        }
        HeroFx.play(player, SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.6F);
        if (!(target instanceof Mob mob) || !HeroAbilities.canControl(target)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            return NewSerumConfig.PSYCHIC_GRIP_COOLDOWN.get() * 0.4D;
        }
        releaseThralls(player);
        int ticks = NewSerumConfig.PSYCHIC_GRIP_TICKS.get();
        THRALLS.put(mob.getUUID(), new Thrall(player.getUUID(), level.getGameTime() + ticks));
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
        retarget(mob);
        HeroFx.play(target, SoundEvents.ELDER_GUARDIAN_CURSE, 0.4F, 1.8F);
        if (player instanceof ServerPlayer serverPlayer) {
            HeroAdvancements.award(serverPlayer, "justice_league/martian/psychic_grip");
        }
        return NewSerumConfig.PSYCHIC_GRIP_COOLDOWN.get();
    }

    private static void retarget(Mob mob) {
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(16.0D),
                entity -> entity != mob && entity.isAlive() && entity instanceof Enemy && !THRALLS.containsKey(entity.getUUID()))) {
            double distance = candidate.distanceToSqr(mob);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        if (nearest != null) {
            mob.setTarget(nearest);
        }
    }

    private static void tickThralls(LivingEntity player) {
        if (THRALLS.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        Iterator<Map.Entry<UUID, Thrall>> iterator = THRALLS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Thrall> entry = iterator.next();
            if (!entry.getValue().owner().equals(player.getUUID())) {
                continue;
            }
            Entity entity = HeroCaster.level(player).getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                iterator.remove();
                continue;
            }
            if (now >= entry.getValue().until()) {
                iterator.remove();
                mob.setTarget(null);
                continue;
            }
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive() || target instanceof Player) {
                mob.setTarget(null);
                retarget(mob);
            }
        }
    }

    private static void releaseThralls(LivingEntity player) {
        Iterator<Map.Entry<UUID, Thrall>> iterator = THRALLS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Thrall> entry = iterator.next();
            if (!entry.getValue().owner().equals(player.getUUID())) {
                continue;
            }
            iterator.remove();
            if (HeroCaster.level(player).getEntity(entry.getKey()) instanceof Mob mob) {
                mob.setTarget(null);
                mob.removeEffect(MobEffects.GLOWING);
            }
        }
    }

    private static void updateForm(ServerPlayer player) {
        if (!ModCompat.MORPH) {
            return;
        }
        ResourceLocation id = MorphShapeshift.formId(player);
        EntityType<?> type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null || type == EntityType.PLAYER) {
            FORMS.remove(player.getUUID());
        } else {
            FORMS.put(player.getUUID(), type);
        }
    }

    private static boolean sameFaction(EntityType<?> form, LivingEntity mob) {
        EntityType<?> type = mob.getType();
        if (type == form || PIGLINS.contains(form) && PIGLINS.contains(type)) {
            return true;
        }
        for (TagKey<EntityType<?>> tag : FACTIONS) {
            if (form.is(tag) && type.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean disguised(ServerPlayer player, LivingEntity mob) {
        EntityType<?> form = FORMS.get(player.getUUID());
        if (form == null || !isMartian(player)) {
            return false;
        }
        Long blown = BLOWN.get(player.getUUID());
        if (blown != null && player.level().getGameTime() < blown) {
            return false;
        }
        return sameFaction(form, mob);
    }

    private static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null) {
            return;
        }
        if (THRALLS.containsKey(event.getEntity().getUUID()) && target instanceof Player) {
            event.setCanceled(true);
            return;
        }
        if (target instanceof ServerPlayer player && disguised(player, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static void loseFaction(ServerPlayer player) {
        EntityType<?> form = FORMS.get(player.getUUID());
        if (form == null) {
            return;
        }
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48.0D),
                mob -> mob.getTarget() == player && sameFaction(form, mob))) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    private static double shapeshift(ServerPlayer player, int slot) {
        if (MorphShapeshift.isMorphed(player)) {
            MorphShapeshift.demorph(player);
            FORMS.remove(player.getUUID());
            HeroFx.play(player, SoundEvents.SLIME_SQUISH, 1.0F, 0.6F);
            return NewSerumConfig.SHAPESHIFT_COOLDOWN.get();
        }
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), 16.0D, 1);
        if (beam.targets().isEmpty() || beam.targets().get(0) instanceof Player
                || !HeroAbilities.canControl(beam.targets().get(0))) {
            player.displayClientMessage(Component.literal("§7Look at a creature to take its shape."), true);
            return 0.0D;
        }
        LivingEntity target = beam.targets().get(0);
        if (!MorphShapeshift.morph(player, target)) {
            player.displayClientMessage(Component.literal("§7That shape won't hold."), true);
            return 0.0D;
        }
        FORMS.put(player.getUUID(), target.getType());
        BLOWN.remove(player.getUUID());
        loseFaction(player);
        player.displayClientMessage(Component.literal("§aTheir kind will think you're one of them. Just don't hit any."), true);
        Vec3 center = player.getBoundingBox().getCenter();
        if (!HeroFx.vfx(player.serverLevel(), player.position(), "void_twist", tint(player, 0xD0000000))) {
            player.serverLevel().sendParticles(SerumKit.glow(player, MIND_COLOR, 1.2F), center.x, center.y, center.z, 30, 0.4D, 0.8D, 0.4D, 0.0D);
        }
        HeroFx.play(player, SoundEvents.SLIME_SQUISH, 1.0F, 0.8F);
        HeroAdvancements.award(player, "justice_league/martian/shapeshifter");
        return NewSerumConfig.SHAPESHIFT_COOLDOWN.get();
    }
}
