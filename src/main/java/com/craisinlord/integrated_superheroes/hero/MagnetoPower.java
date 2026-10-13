package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.ScorchedGunsWeapons;
import com.github.alexmodguy.alexscaves.server.entity.item.MagneticWeaponEntity;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.github.alexmodguy.alexscaves.server.entity.util.MagnetUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class MagnetoPower {
    public static final String ID = "magnetism_erik_lehnsherr";
    public static final String SWARM_STATE = "integrated_superheroes_swarm";
    private static final String GRIP = "magnetic_grip";
    private static final ResourceLocation FLIGHT_ID = ResourceLocation.fromNamespaceAndPath("integrated_superheroes", "magneto_flight");
    private static final TagKey<Item> FERROMAGNETIC = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("alexscaves", "ferromagnetic_items"));
    private static final TagKey<EntityType<?>> FERROMAGNETIC_ENTITIES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("alexscaves", "ferromagnetic_entities"));
    private static final TagKey<EntityType<?>> RESISTS_MAGNETS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("alexscaves", "resists_magnets"));
    private static final TagKey<Block> FERROMAGNETIC_BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("alexscaves", "ferromagnetic_blocks"));
    private static final String WOLVERINE = "weapon_x_logan";
    public static final HeroPower.Toggle MAGNETIC_SHIELD = new HeroPower.Toggle("integrated_superheroes_off_magnetic_shield", "Magnetic Shield",
            () -> HeroFx.item("alexscaves:scarlet_magnet"));
    public static final HeroPower.Toggle MAGNETIC_WALK = new HeroPower.Toggle("integrated_superheroes_off_magnetic_walk", "Magnetic Walk",
            () -> HeroFx.item("alexscaves:scarlet_neodymium_pillar"));
    private static final Map<UUID, Long> SWARM_UNTIL = new HashMap<>();
    private static final Map<UUID, Direction> WALL_FACE = new HashMap<>();

    private MagnetoPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Magnetism (Erik Lehnsherr)",
                syringe,
                MarvelBalanceConfig.MAGNETO,
                List.of(),
                List.of(
                        new HeroPower.Ability(GRIP, "Magnetic Grip", MagnetoPower::grip),
                        new HeroPower.Ability("magnetic_crush", "Magnetic Crush", MagnetoPower::crush),
                        new HeroPower.Ability("weapon_swarm", "Weapon Swarm", MagnetoPower::swarm),
                        HeroToggles.WHEEL
                ),
                List.of(HeroToggles.FLIGHT, MAGNETIC_SHIELD, MAGNETIC_WALK),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        HeroFlight.tick(player, FLIGHT_ID, HeroToggles.isOn(player, HeroToggles.FLIGHT) && isMetal(player),
                                MarvelBalanceConfig.MAGNETO_FLY_SPEED.get().floatValue());
                        HeroGrip.tick(player);
                        HeroBlockLift.tick(player);
                        tickSwarm(player);
                        ServerLevel level = player.serverLevel();
                        if (player.getAbilities().flying && player.tickCount % 3 == 0) {
                            orbit(level, player.getX(), player.getY() + 0.2D, player.getZ(), player.position().add(0.0D, 0.6D, 0.0D), 2);
                            if (player.tickCount % 60 == 0) {
                                HeroFx.play(player, HeroFx.sound("alexscaves:teletor_float", SoundEvents.BEACON_AMBIENT), 0.5F, 1.0F);
                            }
                        }
                        if (HeroToggles.isOn(player, MAGNETIC_SHIELD)) {
                            deflectArrows(player);
                        }
                        tickWallWalk(player, level);
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroFlight.remove(player, FLIGHT_ID);
                        HeroGrip.clear(player);
                        HeroBlockLift.drop(player);
                        SWARM_UNTIL.remove(player.getUUID());
                        WALL_FACE.remove(player.getUUID());
                        HeroPowers.data(player).removeState(SWARM_STATE);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (!HeroToggles.isOn(player, MAGNETIC_SHIELD)) {
                            return;
                        }
                        Entity direct = event.getSource().getDirectEntity();
                        LivingEntity shooter = null;
                        if (ModCompat.SCORCHED_GUNS && ScorchedGunsWeapons.isBullet(direct)) {
                            shooter = ScorchedGunsWeapons.bulletShooter(direct);
                        } else if (direct instanceof AbstractArrow arrow) {
                            shooter = arrow.getOwner() instanceof LivingEntity living ? living : null;
                        } else {
                            return;
                        }
                        event.setCanceled(true);
                        player.causeFoodExhaustion(MarvelBalanceConfig.MAGNETIC_SHIELD_EXHAUSTION.get().floatValue());
                        ServerLevel level = player.serverLevel();
                        Vec3 hit = direct.position();
                        if (shooter != null && shooter != player && shooter.isAlive() && shooter.distanceToSqr(player) < 64.0D * 64.0D) {
                            Vec3 to = shooter.getBoundingBox().getCenter();
                            flow(level, hit, to, 6);
                            shooter.hurt(player.damageSources().mobAttack(player), event.getAmount());
                        }
                        HeroFx.play(player, HeroFx.sound("alexscaves:galena_gauntlet_stop", SoundEvents.ANVIL_LAND), 0.8F, 1.6F);
                    }
                }
        );
    }

    public static boolean magneticWalker(Entity entity) {
        return entity instanceof Player player && !player.isSpectator() && !player.isShiftKeyDown() && !player.getAbilities().flying
                && player.hasData(com.stxllarplanes.marvellegacy.init.AttachmentsRegistry.PLAYER_DATA.get())
                && ID.equals(HeroPowers.data(player).getPrimaryPower()) && HeroToggles.isOn(player, MAGNETIC_WALK);
    }

    public static boolean metalBlock(BlockState state) {
        return state.is(FERROMAGNETIC_BLOCKS);
    }

    private static BlockPos sample(Entity entity, Direction direction) {
        AABB box = entity.getBoundingBox();
        return switch (direction) {
            case DOWN -> BlockPos.containing(entity.getX(), box.minY - 0.3D, entity.getZ());
            case UP -> BlockPos.containing(entity.getX(), box.maxY + 0.3D, entity.getZ());
            case EAST -> BlockPos.containing(box.maxX + 0.3D, entity.getY(0.5D), entity.getZ());
            case WEST -> BlockPos.containing(box.minX - 0.3D, entity.getY(0.5D), entity.getZ());
            case NORTH -> BlockPos.containing(entity.getX(), entity.getY(0.5D), box.minZ - 0.3D);
            case SOUTH -> BlockPos.containing(entity.getX(), entity.getY(0.5D), box.maxZ + 0.3D);
        };
    }

    public static void selfPull(Entity entity) {
        Direction current = MagnetUtil.getEntityMagneticDirection(entity);
        Direction pull = null;
        if (current != null && metalBlock(entity.level().getBlockState(sample(entity, current)))) {
            pull = current;
        } else {
            for (Direction direction : Direction.values()) {
                if (metalBlock(entity.level().getBlockState(sample(entity, direction)))) {
                    pull = direction;
                    if (direction != Direction.DOWN) {
                        break;
                    }
                }
            }
        }
        if (pull == null) {
            return;
        }
        Vec3 step = new Vec3(pull.getStepX(), pull.getStepY(), pull.getStepZ()).scale(0.15D);
        MagnetUtil.setEntityMagneticDelta(entity, MagnetUtil.getEntityMagneticDelta(entity).add(step));
    }

    private static void tickWallWalk(ServerPlayer player, ServerLevel level) {
        Direction face = magneticWalker(player) ? MagnetUtil.getEntityMagneticDirection(player) : Direction.DOWN;
        Direction previous = WALL_FACE.getOrDefault(player.getUUID(), Direction.DOWN);
        if (face != previous) {
            WALL_FACE.put(player.getUUID(), face);
            if (face != Direction.DOWN) {
                HeroFx.play(player, HeroFx.sound("alexscaves:galena_gauntlet_stop", SoundEvents.ANVIL_PLACE), 0.5F, 1.5F);
            }
        }
        if (face != Direction.DOWN && player.tickCount % 4 == 0) {
            BlockPos wall = sample(player, face);
            Vec3 at = Vec3.atCenterOf(wall).add(Vec3.atLowerCornerOf(face.getNormal()).scale(-0.55D));
            orbit(level, at.x, at.y - 0.5D, at.z, at, 1);
        }
    }

    public static boolean isSwarming(Player player) {
        PlayerData data = HeroPowers.data(player);
        return ID.equals(data.getPrimaryPower()) && data.hasState(SWARM_STATE);
    }

    private static ParticleOptions particle(String id) {
        if (BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(id)) instanceof SimpleParticleType type) {
            return type;
        }
        return net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT;
    }

    private static void orbit(ServerLevel level, double x, double y, double z, Vec3 center, int count) {
        for (int i = 0; i < count; i++) {
            ParticleOptions type = particle(i % 2 == 0 ? "alexscaves:scarlet_magnetic_orbit" : "alexscaves:azure_magnetic_orbit");
            level.sendParticles(type, x + (level.random.nextDouble() - 0.5D), y + level.random.nextDouble(), z + (level.random.nextDouble() - 0.5D),
                    0, center.x, center.y, center.z, 1.0D);
        }
    }

    private static HeroBlockLift.Profile metalLift() {
        return new HeroBlockLift.Profile(MarvelBalanceConfig.GRIP_RANGE.get(), MarvelBalanceConfig.LIFT_MAX_BLOCKS.get(), MarvelBalanceConfig.LIFT_HOLD_TICKS.get(),
                MarvelBalanceConfig.LIFT_THROW_SPEED.get(), MarvelBalanceConfig.GRIP_THROW_DAMAGE.get(),
                state -> state.is(FERROMAGNETIC_BLOCKS), (level, center) -> orbitAround(level, center, 3),
                () -> HeroFx.sound("alexscaves:magnetron_assemble", SoundEvents.ANVIL_USE),
                () -> HeroFx.sound("alexscaves:galena_gauntlet_use_loop", SoundEvents.BEACON_AMBIENT),
                () -> HeroFx.sound("alexscaves:galena_gauntlet_stop", SoundEvents.ANVIL_LAND));
    }

    public static void orbitAround(ServerLevel level, Vec3 center, int count) {
        orbit(level, center.x, center.y - 0.5D, center.z, center, count);
    }

    private static void flow(ServerLevel level, Vec3 from, Vec3 to, int count) {
        for (int i = 0; i < count; i++) {
            ParticleOptions type = particle(i % 2 == 0 ? "alexscaves:scarlet_magnetic_flow" : "alexscaves:azure_magnetic_flow");
            level.sendParticles(type, from.x + (level.random.nextDouble() - 0.5D) * 0.5D, from.y + (level.random.nextDouble() - 0.5D) * 0.5D,
                    from.z + (level.random.nextDouble() - 0.5D) * 0.5D, 0, to.x, to.y, to.z, 1.0D);
        }
    }

    public static boolean isMetal(LivingEntity entity) {
        if (entity instanceof Player player && (WOLVERINE.equals(HeroPowers.data(player).getPrimaryPower()) || ColossusPower.isSteel(player))) {
            return true;
        }
        if (entity.getType().is(RESISTS_MAGNETS)) {
            return false;
        }
        if (entity.getType().is(FERROMAGNETIC_ENTITIES)) {
            return true;
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (entity.getItemBySlot(slot).is(FERROMAGNETIC)) {
                return true;
            }
        }
        return false;
    }

    private static double grip(ServerPlayer player, int slot) {
        if (HeroGrip.isHolding(player) || HeroBlockLift.isLifting(player)) {
            if (HeroGrip.isHolding(player)) {
                float damage = MarvelBalanceConfig.GRIP_THROW_DAMAGE.get().floatValue();
                HeroGrip.release(player, MarvelBalanceConfig.GRIP_THROW_SPEED.get(), damage);
                HeroFx.play(player, HeroFx.sound("alexscaves:galena_gauntlet_stop", SoundEvents.ANVIL_LAND), 1.0F, 0.8F);
            } else {
                HeroBlockLift.throwHeld(player);
            }
            HeroAnimations.play(player, "celestisynth:cs_crescentia_throw");
            return MarvelBalanceConfig.GRIP_COOLDOWN.get();
        }
        double range = MarvelBalanceConfig.GRIP_RANGE.get();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 1);
        LivingEntity target = beam.targets().isEmpty() ? null : beam.targets().get(0);
        if (target != null && isMetal(target)) {
            boolean grabbed = HeroGrip.grab(player, target, MarvelBalanceConfig.GRIP_HOLD_TICKS.get(), null, held -> {
                ServerLevel level = player.serverLevel();
                Vec3 center = held.getBoundingBox().getCenter();
                if (player.tickCount % 2 == 0) {
                    orbit(level, center.x, center.y - held.getBbHeight() * 0.5D, center.z, center, 3);
                }
                if (player.tickCount % 6 == 0) {
                    flow(level, player.getEyePosition().add(player.getLookAngle()), center, 2);
                }
                if (player.tickCount % 20 == 0) {
                    HeroFx.play(player, HeroFx.sound("alexscaves:galena_gauntlet_use_loop", SoundEvents.BEACON_AMBIENT), 0.6F, 1.0F);
                }
            });
            if (grabbed) {
                HeroAnimations.play(player, "web_shoot_r");
            }
            return 0.0D;
        }
        if (HeroBlockLift.lift(player, metalLift())) {
            HeroAnimations.play(player, "web_shoot_both");
            return 0.0D;
        }
        if (target != null) {
            player.displayClientMessage(Component.literal("\u00a77There's no metal on it to grab."), true);
        }
        return 0.0D;
    }

    private static double crush(ServerPlayer player, int slot) {
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), MarvelBalanceConfig.CRUSH_RANGE.get(), 1);
        if (beam.targets().isEmpty()) {
            return 0.0D;
        }
        LivingEntity target = beam.targets().get(0);
        double damage = MarvelBalanceConfig.CRUSH_BASE_DAMAGE.get() + MarvelBalanceConfig.CRUSH_PER_ARMOR_DAMAGE.get() * target.getArmorValue();
        HeroAdvancements.cast(player).kills(entity -> entity instanceof IronGolem, "x_men/magneto/iron_deficiency")
                .run(() -> target.hurt(player.damageSources().mobAttack(player), (float) damage));
        HeroFx.effect("alexscaves:magnetizing").ifPresent(effect -> target.addEffect(new MobEffectInstance(effect, 80, 0)));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        ServerLevel level = player.serverLevel();
        Vec3 center = target.getBoundingBox().getCenter();
        level.sendParticles(particle("alexscaves:magnet_lightning"), center.x, center.y, center.z, 2, 0.3D, 0.3D, 0.3D, 0.0D);
        orbit(level, center.x, center.y - target.getBbHeight() * 0.5D, center.z, center, 8);
        flow(level, player.getEyePosition().add(player.getLookAngle()), center, 6);
        HeroFx.play(target, SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
        HeroAnimations.play(player, "web_shoot_both");
        return MarvelBalanceConfig.CRUSH_COOLDOWN.get();
    }

    private static boolean isWeapon(ItemStack stack) {
        if (stack.getItem() instanceof TieredItem) {
            return true;
        }
        return stack.getAttributeModifiers().modifiers().stream()
                .anyMatch(entry -> entry.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
    }

    private static double swarm(ServerPlayer player, int slot) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("alexscaves", "magnetic_weapon"));
        List<ItemStack> sources = new java.util.ArrayList<>();
        sources.add(player.getItemInHand(InteractionHand.OFF_HAND));
        for (int i = 0; i < 9; i++) {
            if (i != player.getInventory().selected) {
                sources.add(player.getInventory().items.get(i));
            }
        }
        int max = MarvelBalanceConfig.SWARM_MAX_WEAPONS.get();
        int launched = 0;
        for (ItemStack stack : sources) {
            if (launched >= max) {
                break;
            }
            if (stack.isEmpty() || !stack.is(FERROMAGNETIC) || !isWeapon(stack)) {
                continue;
            }
            if (!(type.create(player.level()) instanceof MagneticWeaponEntity weapon)) {
                break;
            }
            ItemStack copy = stack.copy();
            stack.setCount(0);
            double angle = Math.PI * 2.0D * launched / max;
            weapon.setItemStack(copy);
            weapon.setPos(player.position().add(Math.cos(angle) * 1.2D, 1.0D + launched * 0.3D, Math.sin(angle) * 1.2D));
            weapon.setControllerUUID(player.getUUID());
            player.level().addFreshEntity(weapon);
            launched++;
        }
        if (launched == 0) {
            player.displayClientMessage(Component.literal("\u00a77No metal weapons on your hotbar or off-hand."), true);
            return 0.0D;
        }
        if (launched >= max) {
            HeroAdvancements.award(player, "x_men/magneto/heavy_metal");
        }
        SWARM_UNTIL.put(player.getUUID(), player.level().getGameTime() + MarvelBalanceConfig.SWARM_DURATION_TICKS.get());
        HeroPowers.data(player).addState(SWARM_STATE);
        PlayerData.sync(player);
        HeroFx.play(player, HeroFx.sound("alexscaves:magnetron_assemble", SoundEvents.ANVIL_USE), 1.0F, 1.2F);
        HeroAnimations.play(player, "web_shoot_both");
        return MarvelBalanceConfig.SWARM_COOLDOWN.get();
    }

    private static void tickSwarm(ServerPlayer player) {
        Long until = SWARM_UNTIL.get(player.getUUID());
        if (until == null || player.level().getGameTime() < until) {
            return;
        }
        SWARM_UNTIL.remove(player.getUUID());
        HeroPowers.data(player).removeState(SWARM_STATE);
        PlayerData.sync(player);
    }

    private static void deflectArrows(ServerPlayer player) {
        double radius = MarvelBalanceConfig.MAGNETIC_SHIELD_RADIUS.get();
        for (AbstractArrow arrow : player.level().getEntitiesOfClass(AbstractArrow.class, player.getBoundingBox().inflate(radius),
                candidate -> candidate.getOwner() != player && candidate.getDeltaMovement().lengthSqr() > 0.04D)) {
            Vec3 motion = arrow.getDeltaMovement();
            Vec3 toPlayer = player.getBoundingBox().getCenter().subtract(arrow.position());
            if (motion.dot(toPlayer) <= 0.0D) {
                continue;
            }
            arrow.setDeltaMovement(motion.scale(-0.8D));
            arrow.setOwner(player);
            arrow.hasImpulse = true;
            HeroAdvancements.award(player, "x_men/magneto/magnetic_personality");
            player.causeFoodExhaustion(MarvelBalanceConfig.MAGNETIC_SHIELD_EXHAUSTION.get().floatValue());
            flow(player.serverLevel(), arrow.position(), arrow.position().add(motion.scale(-3.0D)), 4);
            HeroFx.play(arrow, SoundEvents.ANVIL_LAND, 0.4F, 1.8F);
        }
    }
}
