package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.ScorchedGunsWeapons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class GambitPower {
    public static final String ID = "kinetic_charge_remy_lebeau";
    public static final HeroPower.Toggle CHARGED_AMMO = new HeroPower.Toggle("integrated_superheroes_off_charged_ammo", "Charged Ammo", () -> Items.AMETHYST_SHARD);
    private static final String CHARGED_TAG = "integrated_superheroes_kinetic_ammo";
    private static final Map<UUID, ChargedAmmo> AMMO = new HashMap<>();
    private static final Map<UUID, Mark> ENTITY_MARKS = new HashMap<>();
    private static final Map<BlockKey, Mark> BLOCK_MARKS = new HashMap<>();
    private static final Map<UUID, Vault> VAULTS = new HashMap<>();
    private static final List<PendingCard> PENDING = new ArrayList<>();

    private GambitPower() {
    }

    private record ChargedAmmo(ResourceKey<Level> dimension, UUID owner, int kinds, ItemStack stack) {
    }

    private record Mark(ResourceKey<Level> dimension, UUID owner, long detonateAt) {
    }

    private record BlockKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private record Vault(boolean staff, int ticks) {
    }

    private record PendingCard(UUID owner, UUID target, long fireAt) {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Kinetic Charge (Remy LeBeau)",
                syringe,
                MarvelBalanceConfig.GAMBIT,
                List.of("agility"),
                List.of(
                        new HeroPower.Ability("kinetic_throw", "Kinetic Throw", GambitPower::kineticThrow),
                        new HeroPower.Ability("card_fan", "Card Fan", GambitPower::cardFan),
                        new HeroPower.Ability("charge_touch", "Charge Touch", GambitPower::chargeTouch),
                        new HeroPower.Ability("bo_vault", "Bo Vault", GambitPower::boVault),
                        new HeroPower.Ability("full_house", "Full House", GambitPower::fullHouse),
                        HeroToggles.WHEEL
                ),
                List.of(CHARGED_AMMO),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        tickVault(player);
                        if (player.tickCount % 100 == 0 && MarvelBalanceConfig.CAJUN_CHARM.get()) {
                            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 140, 0, true, false, true));
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        VAULTS.remove(player.getUUID());
                        MobEffectInstance charm = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
                        if (charm != null && charm.isAmbient()) {
                            player.removeEffect(MobEffects.HERO_OF_THE_VILLAGE);
                        }
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypeTags.IS_FALL) && VAULTS.containsKey(player.getUUID())) {
                            event.setCanceled(true);
                        }
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().getDirectEntity() != player) {
                            return;
                        }
                        Mark mark = ENTITY_MARKS.get(event.getEntity().getUUID());
                        if (mark != null && mark.owner().equals(player.getUUID())) {
                            ENTITY_MARKS.put(event.getEntity().getUUID(), new Mark(mark.dimension(), mark.owner(), 0L));
                        }
                    }
                }
        );
    }

    public static void clear(ServerPlayer player) {
        VAULTS.remove(player.getUUID());
    }

    private static boolean isGambit(Entity entity) {
        return entity instanceof ServerPlayer player
                && player.hasData(com.stxllarplanes.marvellegacy.init.AttachmentsRegistry.PLAYER_DATA.get())
                && ID.equals(HeroPowers.data(player).getPrimaryPower());
    }

    private static boolean hostile(ServerPlayer player, LivingEntity entity) {
        return KineticBurst.affects(player, entity) && (entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget() == player
                || entity instanceof Player);
    }

    private static ItemStack takeThrowable(ServerPlayer player, InteractionHand hand, int amount) {
        ItemStack held = player.getItemInHand(hand);
        if (!GambitItems.throwable(held)) {
            return ItemStack.EMPTY;
        }
        if (player.getAbilities().instabuild) {
            return held.copyWithCount(Math.min(amount, held.getCount()));
        }
        return held.split(amount);
    }

    private static KineticCharge throwItem(ServerPlayer player, ItemStack stack, Vec3 direction, float baseDamage, float baseRadius, double speedScale) {
        GambitItems.Profile profile = GambitItems.profile(stack, baseDamage, baseRadius);
        float impact = MarvelBalanceConfig.KINETIC_IMPACT_DAMAGE.get().floatValue();
        double attack = attackDamage(stack);
        if (attack > 0.0D) {
            impact += (float) (attack * MarvelBalanceConfig.KINETIC_WEAPON_MULTIPLIER.get());
        }
        if (profile.has(GambitItems.PHANTOM)) {
            impact *= MarvelBalanceConfig.PHANTOM_CARD_MULTIPLIER.get().floatValue();
        }
        boolean dropBack = profile.has(GambitItems.RETURNING) && !player.getAbilities().instabuild;
        KineticCharge charge = KineticCharge.create(player, stack, profile, impact, dropBack);
        Vec3 eye = player.getEyePosition();
        charge.setPos(eye.x + direction.x * 0.4D, eye.y - 0.15D + direction.y * 0.4D, eye.z + direction.z * 0.4D);
        double speed = MarvelBalanceConfig.KINETIC_THROW_SPEED.get() * profile.speed() * speedScale;
        charge.shoot(direction.x, direction.y, direction.z, (float) speed, profile.has(GambitItems.CARD) ? 0.0F : 0.5F);
        player.serverLevel().addFreshEntity(charge);
        return charge;
    }

    private static double attackDamage(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0D;
        }
        double attack = 0.0D;
        for (net.minecraft.world.item.component.ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if (entry.attribute().value() == net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE.value()
                    && entry.slot().test(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                    && entry.modifier().operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                attack += entry.modifier().amount();
            }
        }
        return attack;
    }

    private static double kineticThrow(ServerPlayer player, int slot) {
        ItemStack stack = takeThrowable(player, InteractionHand.MAIN_HAND, 1);
        if (stack.isEmpty() && player.getMainHandItem().isEmpty()) {
            stack = takeThrowable(player, InteractionHand.OFF_HAND, 1);
        }
        throwItem(player, stack, player.getLookAngle(), MarvelBalanceConfig.KINETIC_DAMAGE.get().floatValue(),
                MarvelBalanceConfig.KINETIC_RADIUS.get().floatValue(), 1.0D);
        chargeFx(player);
        HeroFx.play(player, SoundEvents.SNOWBALL_THROW, 1.0F, 0.6F);
        HeroAnimations.play(player, "web_shoot_r");
        return MarvelBalanceConfig.KINETIC_THROW_COOLDOWN.get();
    }

    private static double cardFan(ServerPlayer player, int slot) {
        int count = MarvelBalanceConfig.CARD_FAN_COUNT.get();
        float spread = MarvelBalanceConfig.CARD_FAN_SPREAD.get().floatValue();
        ItemStack held = player.getMainHandItem();
        boolean useHeld = GambitItems.throwable(held) && !held.is(GambitItems.RETURNS);
        float damage = MarvelBalanceConfig.KINETIC_DAMAGE.get().floatValue() * 0.6F;
        float radius = MarvelBalanceConfig.KINETIC_RADIUS.get().floatValue() * 0.8F;
        for (int i = 0; i < count; i++) {
            float offset = count == 1 ? 0.0F : -spread + 2.0F * spread * i / (count - 1);
            ItemStack stack = useHeld ? takeThrowable(player, InteractionHand.MAIN_HAND, 1) : ItemStack.EMPTY;
            throwItem(player, stack, Vec3.directionFromRotation(player.getXRot(), player.getYRot() + offset), damage, radius, 1.0D);
        }
        chargeFx(player);
        HeroFx.play(player, SoundEvents.BOOK_PAGE_TURN, 1.2F, 1.4F);
        HeroFx.play(player, SoundEvents.SNOWBALL_THROW, 1.0F, 0.5F);
        HeroAnimations.play(player, "celestisynth:cs_crescentia_throw");
        return MarvelBalanceConfig.CARD_FAN_COOLDOWN.get();
    }

    private static double chargeTouch(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        double range = MarvelBalanceConfig.CHARGE_TOUCH_RANGE.get();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 reach = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, reach, new AABB(eye, reach).inflate(1.0D),
                entity -> entity instanceof LivingEntity living && KineticBurst.affects(player, living), range * range);
        long detonateAt = level.getGameTime() + MarvelBalanceConfig.CHARGE_TOUCH_FUSE_TICKS.get();
        if (entityHit != null) {
            LivingEntity target = (LivingEntity) entityHit.getEntity();
            ENTITY_MARKS.put(target.getUUID(), new Mark(level.dimension(), player.getUUID(), detonateAt));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, MarvelBalanceConfig.CHARGE_TOUCH_FUSE_TICKS.get() + 5, 0), player);
            HeroAbilities.particleLine(level, eye.subtract(0.0D, 0.3D, 0.0D), target.getBoundingBox().getCenter(), HeroAbilities.emissive(KineticBurst.PURPLE, 0.8F), 0.4D);
        } else if (blockHit.getType() == HitResult.Type.BLOCK && !level.getBlockState(blockHit.getBlockPos()).isAir()) {
            BLOCK_MARKS.put(new BlockKey(level.dimension(), blockHit.getBlockPos().immutable()), new Mark(level.dimension(), player.getUUID(), detonateAt));
            HeroAbilities.particleLine(level, eye.subtract(0.0D, 0.3D, 0.0D), blockHit.getLocation(), HeroAbilities.emissive(KineticBurst.PURPLE, 0.8F), 0.4D);
        } else {
            player.displayClientMessage(Component.literal("§7Nothing to charge."), true);
            return 0.5D;
        }
        HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 0.8F);
        HeroFx.play(player, SoundEvents.BEACON_ACTIVATE, 0.6F, 1.8F);
        HeroAnimations.play(player, "web_shoot_r");
        return MarvelBalanceConfig.CHARGE_TOUCH_COOLDOWN.get();
    }

    private static void detonateMark(ServerLevel level, Vec3 at, ServerPlayer owner) {
        KineticBurst.burst(level, at, null, owner, MarvelBalanceConfig.CHARGE_TOUCH_DAMAGE.get().floatValue(),
                MarvelBalanceConfig.CHARGE_TOUCH_RADIUS.get().floatValue(), 0, ItemStack.EMPTY);
    }

    private static double boVault(ServerPlayer player, int slot) {
        boolean staff = GambitItems.isStaff(player.getMainHandItem()) || GambitItems.isStaff(player.getOffhandItem());
        double multiplier = staff ? MarvelBalanceConfig.VAULT_STAFF_MULTIPLIER.get() : 1.0D;
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        Vec3 horizontal = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize().scale(MarvelBalanceConfig.VAULT_HORIZONTAL.get() * multiplier);
        player.setDeltaMovement(horizontal.x, MarvelBalanceConfig.VAULT_VERTICAL.get() * Math.sqrt(multiplier), horizontal.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        VAULTS.put(player.getUUID(), new Vault(staff, 0));
        ServerLevel level = player.serverLevel();
        KineticBurst.burst(level, player.position().add(0.0D, 0.1D, 0.0D), null, player, MarvelBalanceConfig.KINETIC_DAMAGE.get().floatValue() * 0.4F, 1.5F, 0, ItemStack.EMPTY, false);
        HeroFx.play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
        HeroAnimations.play(player, "flip_low_2");
        return MarvelBalanceConfig.VAULT_COOLDOWN.get();
    }

    private static void tickVault(ServerPlayer player) {
        Vault vault = VAULTS.get(player.getUUID());
        if (vault == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (vault.ticks() < 20) {
            KineticBurst.trail(level, player.position().add(0.0D, 0.6D, 0.0D), 2);
        }
        if (vault.ticks() > 4 && (player.onGround() || player.isInWater())) {
            VAULTS.remove(player.getUUID());
            if (vault.staff()) {
                KineticBurst.burst(level, player.position().add(0.0D, 0.2D, 0.0D), null, player,
                        MarvelBalanceConfig.VAULT_SLAM_DAMAGE.get().floatValue(), 3.0F, 0, ItemStack.EMPTY, false);
                HeroFx.shake(level, player.position(), 12.0D, 0.4F, 8);
            }
            return;
        }
        VAULTS.put(player.getUUID(), new Vault(vault.staff(), vault.ticks() + 1));
        if (vault.ticks() > 200) {
            VAULTS.remove(player.getUUID());
        }
    }

    private static double fullHouse(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        List<LivingEntity> targets = HeroAbilities.cone(player, MarvelBalanceConfig.FULL_HOUSE_RANGE.get(), 35.0D).stream()
                .filter(entity -> hostile(player, entity))
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .limit(MarvelBalanceConfig.FULL_HOUSE_TARGETS.get())
                .toList();
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.literal("§7No one to deal in."), true);
            return 1.0D;
        }
        long now = level.getGameTime();
        for (int i = 0; i < targets.size(); i++) {
            LivingEntity target = targets.get(i);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40 + i * 4, 0), player);
            Vec3 center = target.getBoundingBox().getCenter();
            level.sendParticles(HeroAbilities.emissive(KineticBurst.PURPLE, 1.0F), center.x, center.y + target.getBbHeight() * 0.6D, center.z,
                    12, 0.3D, 0.1D, 0.3D, 0.0D);
            PENDING.add(new PendingCard(player.getUUID(), target.getUUID(), now + 6 + i * 4L));
        }
        player.displayClientMessage(Component.literal("§5Full House."), true);
        HeroFx.play(player, SoundEvents.BOOK_PAGE_TURN, 1.5F, 0.8F);
        HeroFx.play(player, SoundEvents.EVOKER_PREPARE_ATTACK, 0.8F, 1.6F);
        return MarvelBalanceConfig.FULL_HOUSE_COOLDOWN.get();
    }

    private static void chargeFx(ServerPlayer player) {
        Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.6D)).subtract(0.0D, 0.25D, 0.0D);
        player.serverLevel().sendParticles(HeroAbilities.emissive(KineticBurst.PURPLE, 0.9F), hand.x, hand.y, hand.z, 8, 0.1D, 0.1D, 0.1D, 0.0D);
        player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 4, 0.1D, 0.1D, 0.1D, 0.05D);
        HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.6F);
    }

    public static void tickServer(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (!PENDING.isEmpty()) {
            Iterator<PendingCard> iterator = PENDING.iterator();
            List<PendingCard> due = new ArrayList<>();
            while (iterator.hasNext()) {
                PendingCard card = iterator.next();
                if (card.fireAt() <= now) {
                    due.add(card);
                    iterator.remove();
                }
            }
            for (PendingCard card : due) {
                ServerPlayer player = server.getPlayerList().getPlayer(card.owner());
                if (player == null || !(player.serverLevel().getEntity(card.target()) instanceof LivingEntity target) || !target.isAlive()) {
                    continue;
                }
                Vec3 direction = target.getBoundingBox().getCenter().subtract(player.getEyePosition()).normalize().add(0.0D, 0.15D, 0.0D).normalize();
                throwItem(player, ItemStack.EMPTY, direction, MarvelBalanceConfig.FULL_HOUSE_DAMAGE.get().floatValue()
                                / Math.max(0.05F, MarvelBalanceConfig.PHANTOM_CARD_MULTIPLIER.get().floatValue()),
                        MarvelBalanceConfig.KINETIC_RADIUS.get().floatValue(), 0.8D).homing(target);
                HeroFx.play(player, SoundEvents.SNOWBALL_THROW, 0.8F, 1.3F);
            }
        }
        if (!ENTITY_MARKS.isEmpty()) {
            Iterator<Map.Entry<UUID, Mark>> iterator = ENTITY_MARKS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Mark> entry = iterator.next();
                Mark mark = entry.getValue();
                ServerLevel level = server.getLevel(mark.dimension());
                Entity entity = level == null ? null : level.getEntity(entry.getKey());
                if (entity == null) {
                    iterator.remove();
                    continue;
                }
                Vec3 center = entity.getBoundingBox().getCenter();
                if (now >= mark.detonateAt() || !entity.isAlive()) {
                    iterator.remove();
                    detonateMark(level, center, server.getPlayerList().getPlayer(mark.owner()));
                    continue;
                }
                crackle(level, center, entity.getBbWidth() * 0.6D, entity.getBbHeight() * 0.5D, mark.detonateAt() - now);
            }
        }
        if (!BLOCK_MARKS.isEmpty()) {
            Iterator<Map.Entry<BlockKey, Mark>> iterator = BLOCK_MARKS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockKey, Mark> entry = iterator.next();
                Mark mark = entry.getValue();
                ServerLevel level = server.getLevel(mark.dimension());
                if (level == null) {
                    iterator.remove();
                    continue;
                }
                Vec3 center = Vec3.atCenterOf(entry.getKey().pos());
                if (now >= mark.detonateAt() || level.getBlockState(entry.getKey().pos()).isAir()) {
                    iterator.remove();
                    detonateMark(level, center, server.getPlayerList().getPlayer(mark.owner()));
                    continue;
                }
                crackle(level, center, 0.55D, 0.55D, mark.detonateAt() - now);
            }
        }
        if (!AMMO.isEmpty() && now % 2 == 0) {
            Iterator<Map.Entry<UUID, ChargedAmmo>> iterator = AMMO.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, ChargedAmmo> entry = iterator.next();
                ServerLevel level = server.getLevel(entry.getValue().dimension());
                Entity entity = level == null ? null : level.getEntity(entry.getKey());
                if (entity == null) {
                    iterator.remove();
                    continue;
                }
                if (entity.getDeltaMovement().lengthSqr() > 1.0E-3D) {
                    KineticBurst.trail(level, entity.getBoundingBox().getCenter(), 1);
                }
            }
        }
    }

    private static void crackle(ServerLevel level, Vec3 center, double width, double height, long remaining) {
        int count = remaining < 20 ? 4 : 2;
        level.sendParticles(HeroAbilities.emissive(remaining % 4 < 2 ? KineticBurst.PURPLE : KineticBurst.PINK, 0.8F),
                center.x, center.y, center.z, count, width, height, width, 0.0D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 1, width, height, width, 0.02D);
        if (remaining % (remaining < 20 ? 4 : 10) == 0) {
            level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F,
                    remaining < 20 ? 2.0F : 1.2F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 3, width, height, width, 0.1D);
        }
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity().getTags().contains(CHARGED_TAG) || event.getEntity() instanceof KineticCharge
                || event.getEntity().getTags().contains(JubileePower.ROCKET_TAG)) {
            return;
        }
        Entity entity = event.getEntity();
        Entity owner;
        if (entity instanceof Projectile projectile) {
            if (entity instanceof FishingHook || ModCompat.SCORCHED_GUNS && ScorchedGunsWeapons.isBullet(entity)
                    || entity instanceof net.minecraft.world.entity.projectile.FireworkRocketEntity rocket
                    && ((com.craisinlord.integrated_superheroes.mixin.FireworkRocketAccessor) rocket).integratedSuperheroes$getAttachedToEntity() != null) {
                return;
            }
            owner = projectile.getOwner();
        } else if (ModCompat.SCORCHED_GUNS && MarvelBalanceConfig.CHARGE_GUN_PROJECTILES.get() && ScorchedGunsWeapons.isBullet(entity)) {
            owner = ScorchedGunsWeapons.bulletShooter(entity);
        } else {
            return;
        }
        if (!isGambit(owner) || !HeroToggles.isOn((ServerPlayer) owner, CHARGED_AMMO)) {
            return;
        }
        ServerPlayer player = (ServerPlayer) owner;
        ItemStack stack = entity instanceof ItemSupplier supplier ? supplier.getItem().copy() : ItemStack.EMPTY;
        int kinds = stack.isEmpty() ? 0 : GambitItems.kinds(stack) & ~(GambitItems.CARD | GambitItems.PHANTOM | GambitItems.RETURNING);
        entity.addTag(CHARGED_TAG);
        AMMO.put(entity.getUUID(), new ChargedAmmo(entity.level().dimension(), player.getUUID(), kinds, stack));
        player.causeFoodExhaustion(MarvelBalanceConfig.CHARGED_AMMO_EXHAUSTION.get().floatValue());
    }

    private static boolean detonatesOnRemoval(Entity entity) {
        return !(entity instanceof Projectile) || ModCompat.SCORCHED_GUNS && ScorchedGunsWeapons.isThrowable(entity);
    }

    public static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide() || !projectile.getTags().contains(CHARGED_TAG) || detonatesOnRemoval(projectile)) {
            return;
        }
        HitResult hit = event.getRayTraceResult();
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living
                && projectile.getOwner() instanceof ServerPlayer owner && !KineticBurst.affects(owner, living)) {
            return;
        }
        Vec3 at = hit instanceof EntityHitResult entityHit ? entityHit.getEntity().getBoundingBox().getCenter() : hit.getLocation();
        detonateAmmo(projectile, at);
    }

    public static void onLeave(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide() || !entity.getTags().contains(CHARGED_TAG) || !detonatesOnRemoval(entity)) {
            return;
        }
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason == null || !reason.shouldDestroy()) {
            return;
        }
        detonateAmmo(entity, entity.position());
    }

    private static void detonateAmmo(Entity entity, Vec3 at) {
        ChargedAmmo ammo = AMMO.remove(entity.getUUID());
        entity.removeTag(CHARGED_TAG);
        if (ammo == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ammo.owner());
        KineticBurst.burst(level, at, null, owner, MarvelBalanceConfig.CHARGED_AMMO_DAMAGE.get().floatValue(),
                MarvelBalanceConfig.CHARGED_AMMO_RADIUS.get().floatValue(), ammo.kinds(), ammo.stack());
    }
}
