package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class IcemanPower {
    public static final String ID = "cryokinesis_bobby_drake";
    public static final HeroPower.Toggle ICE_SLIDE = new HeroPower.Toggle("integrated_superheroes_off_ice_slide", "Ice Slide", () -> Items.PACKED_ICE);
    public static final HeroPower.Toggle ICE_FORM = new HeroPower.Toggle("integrated_superheroes_off_ice_form", "Ice Form", () -> HeroFx.item("pastel:frostbite_crystal", net.minecraft.world.item.Items.PACKED_ICE));
    private static final Map<Holder<MobEffect>, Integer> FORM_EFFECTS = Map.of(MobEffects.DAMAGE_RESISTANCE, 0);
    private static final Map<Holder<MobEffect>, HeroPower.Toggle> FORM_TOGGLES = Map.of(MobEffects.DAMAGE_RESISTANCE, ICE_FORM);
    private static final Set<UUID> SLIDING = new HashSet<>();
    private static final Set<UUID> ICE_FORMED = new HashSet<>();
    private static final Map<UUID, Wall> WALLS = new java.util.HashMap<>();

    private record Wall(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, Set<BlockPos> blocks, long until) {
    }

    private IcemanPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Cryokinesis (Bobby Drake)",
                syringe,
                MarvelBalanceConfig.ICEMAN,
                List.of("agility"),
                List.of(
                        new HeroPower.Ability("freeze_blast", "Freeze Blast", IcemanPower::freezeBlast),
                        new HeroPower.Ability("cryogenesis", "Cryogenesis", IcemanPower::cryogenesis),
                        new HeroPower.Ability("ice_wall", "Ice Wall", IcemanPower::iceWall),
                        HeroToggles.WHEEL
                ),
                List.of(ICE_SLIDE, ICE_FORM),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (player.tickCount % 20 == 0) {
                            HeroToggles.applyEffects(player, FORM_EFFECTS, FORM_TOGGLES);
                        }
                        if (HeroToggles.isOn(player, ICE_FORM)) {
                            player.setTicksFrozen(0);
                            frostWalk(player);
                            if (ICE_FORMED.add(player.getUUID())) {
                                Vec3 center = player.getBoundingBox().getCenter();
                                player.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                                        center.x, center.y, center.z, 40, 0.4D, 0.8D, 0.4D, 0.2D);
                                HeroFx.play(player, SoundEvents.GLASS_PLACE, 1.0F, 0.7F);
                            }
                        } else {
                            ICE_FORMED.remove(player.getUUID());
                        }
                        tickSlide(player);
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        SLIDING.remove(player.getUUID());
                        ICE_FORMED.remove(player.getUUID());
                        FORM_EFFECTS.keySet().forEach(player::removeEffect);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (!HeroToggles.isOn(player, ICE_FORM)) {
                            return;
                        }
                        if (event.getSource().is(DamageTypeTags.IS_FREEZING)) {
                            event.setCanceled(true);
                        } else if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                            event.setAmount(event.getAmount() * MarvelBalanceConfig.ICE_FORM_FIRE_MULTIPLIER.get().floatValue());
                        }
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        LivingEntity target = event.getEntity();
                        Optional<Holder.Reference<MobEffect>> frozen = HeroFx.effect("iceandfire:frozen");
                        if (frozen.isEmpty() || !target.hasEffect(frozen.get())) {
                            return;
                        }
                        event.setAmount(event.getAmount() * MarvelBalanceConfig.SHATTER_MULTIPLIER.get().floatValue());
                        target.removeEffect(frozen.get());
                        HeroAdvancements.award(player, "x_men/iceman/cold_shoulder");
                        target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        target.removeEffect(MobEffects.DIG_SLOWDOWN);
                        ServerLevel level = player.serverLevel();
                        Vec3 center = target.getBoundingBox().getCenter();
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), center.x, center.y, center.z,
                                30, target.getBbWidth() * 0.5D, target.getBbHeight() * 0.5D, target.getBbWidth() * 0.5D, 0.1D);
                        HeroFx.play(target, SoundEvents.GLASS_BREAK, 1.0F, 0.8F);
                    }
                }
        );
    }

    public static boolean showsIceForm(Player player) {
        return player.hasData(com.stxllarplanes.marvellegacy.init.AttachmentsRegistry.PLAYER_DATA.get())
                && ID.equals(HeroPowers.data(player).getPrimaryPower()) && HeroToggles.isOn(player, ICE_FORM);
    }

    private static void freeze(LivingEntity target, int ticks) {
        if (!HeroAbilities.canControl(target)) {
            return;
        }
        HeroFx.effect("iceandfire:frozen").ifPresent(effect -> target.addEffect(new MobEffectInstance(effect, ticks)));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 2));
    }

    private static double freezeBlast(ServerPlayer player, int slot) {
        double range = MarvelBalanceConfig.FREEZE_RANGE.get();
        float damage = MarvelBalanceConfig.FREEZE_DAMAGE.get().floatValue();
        ServerLevel level = player.serverLevel();
        for (LivingEntity target : HeroAbilities.cone(player, range, 30.0D)) {
            target.hurt(player.damageSources().mobAttack(player), damage);
            freeze(target, MarvelBalanceConfig.FREEZE_TICKS.get());
            Vec3 center = target.getBoundingBox().getCenter();
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SNOWBALL)), center.x, center.y, center.z,
                    10, 0.3D, 0.4D, 0.3D, 0.05D);
        }
        Vec3 eye = player.getEyePosition().subtract(0.0D, 0.2D, 0.0D);
        for (int i = -2; i <= 2; i++) {
            Vec3 direction = Vec3.directionFromRotation(player.getXRot(), player.getYRot() + i * 10.0F);
            HeroAbilities.particleLine(level, eye.add(direction), eye.add(direction.scale(range)), ParticleTypes.SNOWFLAKE, 0.8D);
        }
        HeroFx.play(player, SoundEvents.POWDER_SNOW_BREAK, 1.2F, 0.6F);
        HeroFx.play(player, SoundEvents.PLAYER_HURT_FREEZE, 0.8F, 1.2F);
        HeroAnimations.play(player, "web_shoot_both");
        return MarvelBalanceConfig.FREEZE_COOLDOWN.get();
    }

    private static double cryogenesis(ServerPlayer player, int slot) {
        double radius = MarvelBalanceConfig.CRYOGENESIS_RADIUS.get();
        ServerLevel level = player.serverLevel();
        float damage = MarvelBalanceConfig.CRYOGENESIS_DAMAGE.get().floatValue();
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            target.hurt(player.damageSources().mobAttack(player), damage);
            freeze(target, MarvelBalanceConfig.CRYOGENESIS_TICKS.get());
        }
        int r = (int) Math.ceil(radius);
        BlockPos origin = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -2, -r), origin.offset(r, 1, r))) {
            if (pos.distSqr(origin) > radius * radius) {
                continue;
            }
            var state = level.getBlockState(pos);
            if (state.is(BlockTags.FIRE)) {
                level.removeBlock(pos, false);
            } else if (state.is(Blocks.WATER) && state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource()
                    && level.getBlockState(pos.above()).isAir()) {
                TempBlocks.place(level, player, pos, 200 + level.random.nextInt(100));
            }
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0D, player.getZ(), 120, radius * 0.5D, 0.8D, radius * 0.5D, 0.05D);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), player.getX(), player.getY() + 0.2D,
                player.getZ(), 60, radius * 0.4D, 0.1D, radius * 0.4D, 0.1D);
        HeroFx.play(player, SoundEvents.GLASS_BREAK, 1.2F, 0.5F);
        HeroFx.play(player, SoundEvents.POWDER_SNOW_PLACE, 1.5F, 0.6F);
        HeroAnimations.play(player, "celestisynth:cs_frostbound_cryogenesis");
        return MarvelBalanceConfig.CRYOGENESIS_COOLDOWN.get();
    }

    private static double iceWall(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0D, look.z).normalize();
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
        Vec3 center = player.position().add(direction.scale(3.0D));
        int placed = 0;
        Set<BlockPos> blocks = new HashSet<>();
        for (int w = -2; w <= 2; w++) {
            Vec3 column = center.add(side.scale(w));
            for (int h = 0; h < 3; h++) {
                BlockPos pos = BlockPos.containing(column.x, player.getY() + h, column.z);
                if (TempBlocks.place(level, player, pos, MarvelBalanceConfig.ICE_WALL_TICKS.get())) {
                    placed++;
                    blocks.add(pos.immutable());
                }
            }
        }
        if (placed == 0) {
            return 0.0D;
        }
        WALLS.put(player.getUUID(), new Wall(level.dimension(), blocks, level.getGameTime() + MarvelBalanceConfig.ICE_WALL_TICKS.get()));
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, player.getY() + 1.5D, center.z, 40, 1.5D, 1.0D, 1.5D, 0.02D);
        HeroFx.play(player, SoundEvents.GLASS_PLACE, 1.2F, 0.7F);
        HeroFx.play(player, SoundEvents.POWDER_SNOW_PLACE, 1.2F, 0.8F);
        HeroAnimations.play(player, "bettercombat:one_handed_slam");
        return MarvelBalanceConfig.ICE_WALL_COOLDOWN.get();
    }

    public static void onProjectile(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event) {
        if (WALLS.isEmpty() || !(event.getRayTraceResult() instanceof net.minecraft.world.phys.BlockHitResult hit)
                || !(event.getProjectile().level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        WALLS.values().removeIf(wall -> now > wall.until());
        for (Map.Entry<UUID, Wall> entry : WALLS.entrySet()) {
            Wall wall = entry.getValue();
            if (wall.dimension() == level.dimension() && wall.blocks().contains(hit.getBlockPos())
                    && (event.getProjectile().getOwner() == null || !entry.getKey().equals(event.getProjectile().getOwner().getUUID()))) {
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(entry.getKey());
                if (owner != null) {
                    HeroAdvancements.award(owner, "x_men/iceman/snow_day");
                }
            }
        }
    }

    private static void tickSlide(ServerPlayer player) {
        boolean sliding = HeroToggles.isOn(player, ICE_SLIDE) && player.isSprinting() && !player.isPassenger()
                && !player.getAbilities().flying && !player.isFallFlying();
        HeroAdvancements.travel(player, "x_men/iceman/slip_n_slide", sliding, 1000.0D);
        if (!sliding) {
            if (SLIDING.remove(player.getUUID())) {
                HeroAnimations.stop(player);
            }
            return;
        }
        if (SLIDING.add(player.getUUID())) {
            HeroAnimations.play(player, "slide");
        }
        ServerLevel level = player.serverLevel();
        int ticks = MarvelBalanceConfig.ICE_SLIDE_TICKS.get();
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0D, look.z).normalize();
        float pitch = player.getXRot();
        int floorY = Mth.floor(player.getY()) - 1;
        if (!player.onGround()) {
            BlockPos under = BlockPos.containing(player.getX(), floorY, player.getZ());
            if (player.getDeltaMovement().y < 0.0D && level.getBlockState(under.below()).getCollisionShape(level, under.below()).isEmpty()) {
                placeSlide(level, player, under, ticks);
            }
        } else {
            if (pitch <= 25.0F) {
                placeSlide(level, player, BlockPos.containing(player.getX(), floorY, player.getZ()), ticks);
            }
            for (int step = 1; step <= 2; step++) {
                Vec3 ahead = player.position().add(direction.scale(step * 1.1D));
                int y = floorY;
                if (pitch < -15.0F) {
                    y = floorY + step;
                } else if (pitch > 25.0F) {
                    y = floorY - step;
                }
                placeSlide(level, player, BlockPos.containing(ahead.x, y, ahead.z), ticks);
            }
        }
        if (player.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY(), player.getZ(), 3, 0.3D, 0.05D, 0.3D, 0.01D);
        }
    }

    private static void placeSlide(ServerLevel level, ServerPlayer player, BlockPos pos, int ticks) {
        if (pos.getY() + 1 > player.getY() + player.maxUpStep() + 0.01D || player.getBoundingBox().intersects(new AABB(pos))) {
            return;
        }
        TempBlocks.place(level, player, pos, ticks);
    }

    private static void frostWalk(ServerPlayer player) {
        if (!player.onGround() || player.tickCount % 2 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos below = player.blockPosition().below();
        for (BlockPos pos : BlockPos.betweenClosed(below.offset(-2, 0, -2), below.offset(2, 0, 2))) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.WATER) && state.getFluidState().isSource() && level.getBlockState(pos.above()).isAir()) {
                TempBlocks.place(level, player, pos, 100);
            }
        }
    }
}
