package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.api.RogueAbsorption;
import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.craisinlord.integrated_superheroes.hero.skin.SerumCostumes;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class SwampThingPower {
    public static final String ID = "avatar_of_the_green_alec_holland";
    public static final String ENTANGLE = "swamp_thing_entangle";
    public static final String ROOT_STEP = "swamp_thing_root_step";
    public static final HeroPower.Toggle MENDING = new HeroPower.Toggle("integrated_superheroes_off_swamp_mending", "Rooted Mending", () -> Items.MOSS_BLOCK);
    public static final HeroPower.Toggle SWAMP_LUNGS = new HeroPower.Toggle("integrated_superheroes_off_swamp_lungs", "Swamp Lungs", () -> Items.LILY_PAD);
    public static final TagKey<Block> ROOTABLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "green_rootable"));
    private static final HeroTeleportSafety.Policy ROOT_STEP_POLICY = new HeroTeleportSafety.Policy(16.0D, 1, 1, false, false, 0.0D, 10);
    private static final ParticleOptions ROOTS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState());
    private static final ParticleOptions HANGING = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HANGING_ROOTS.defaultBlockState());
    private static final ParticleOptions MOSS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSS_BLOCK.defaultBlockState());
    private static final ParticleOptions VINES = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.VINE.defaultBlockState());
    private static final Map<UUID, Long> BIND_IMMUNE = new HashMap<>();
    private static final Map<UUID, Long> LAST_HURT = new HashMap<>();
    private static final Map<UUID, Long> MESSAGED = new HashMap<>();
    private static RogueMemory rogue;
    private static boolean registered;

    private SwampThingPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(SwampThingPower::onLogout);
            NeoForge.EVENT_BUS.addListener(SwampThingPower::onStopping);
            SerumCostumes.register(ID, "thematic:swamp_thing");
            RogueAbsorption.remember(rogueMemory());
        }
        return new HeroPower(
                ID,
                "Avatar of the Green (Swamp Thing)",
                syringe,
                SwampThingConfig.STATS,
                List.of(),
                List.of(
                        new HeroPower.Ability(ENTANGLE, "Entangle", SwampThingPower::entangle),
                        new HeroPower.Ability(ROOT_STEP, "Root Step", SwampThingPower::rootStep),
                        HeroToggles.WHEEL
                ),
                List.of(MENDING, SWAMP_LUNGS),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (player.tickCount % 20 == 0) {
                            Map<Holder<MobEffect>, Integer> effects = Map.of(MobEffects.WATER_BREATHING, 0);
                            Map<Holder<MobEffect>, HeroPower.Toggle> toggles = Map.of(MobEffects.WATER_BREATHING, SWAMP_LUNGS);
                            HeroToggles.applyEffects(player, effects, toggles);
                        }
                        mend(player);
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        LAST_HURT.remove(player.getUUID());
                        MESSAGED.remove(player.getUUID());
                        HeroTeleportSafety.forget(player);
                        SerumKit.dropEffect(player, MobEffects.WATER_BREATHING);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getAmount() > 0.0F) {
                            LAST_HURT.put(player.getUUID(), player.level().getGameTime());
                        }
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        LAST_HURT.remove(player.getUUID());
                    }
                }
        );
    }

    public static boolean rootable(BlockState state) {
        return state.is(ROOTABLE);
    }

    private static boolean standingOnRoots(ServerPlayer player) {
        return player.onGround() && !player.isInWater() && rootable(player.getBlockStateOn());
    }

    private static void tell(ServerPlayer player, String text) {
        long now = player.level().getGameTime();
        Long last = MESSAGED.get(player.getUUID());
        if (last != null && Math.abs(now - last) < 10L) {
            return;
        }
        MESSAGED.put(player.getUUID(), now);
        player.displayClientMessage(Component.literal("§7" + text), true);
    }

    private static void mend(ServerPlayer player) {
        int interval = SwampThingConfig.MENDING_INTERVAL_TICKS.get();
        if (player.tickCount % interval != 0 || !HeroToggles.isOn(player, MENDING)) {
            return;
        }
        if (player.getHealth() >= player.getMaxHealth() || player.hasEffect(MobEffects.REGENERATION)
                || VillainWorld.suppressed(player) || HealingFactor.isDowned(player) || !standingOnRoots(player)) {
            return;
        }
        long now = player.level().getGameTime();
        Long hurt = LAST_HURT.get(player.getUUID());
        if (hurt != null && now - hurt >= 0L && now - hurt < SwampThingConfig.MENDING_COMBAT_PAUSE_TICKS.get()) {
            return;
        }
        player.heal(SwampThingConfig.MENDING_HEAL.get().floatValue());
        player.serverLevel().sendParticles(ParticleTypes.COMPOSTER, player.getX(), player.getY() + 0.2D, player.getZ(), 4, 0.3D, 0.1D, 0.3D, 0.0D);
    }

    private static BlockHitResult aimGround(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        if (hit.getType() != HitResult.Type.BLOCK || !rootable(player.level().getBlockState(hit.getBlockPos()))) {
            return null;
        }
        return hit;
    }

    private static Vec3 groundTop(Level level, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        double top = pos.getY() + Math.max(0.0D, level.getBlockState(pos).getCollisionShape(level, pos).max(Direction.Axis.Y));
        if (hit.getDirection() == Direction.UP) {
            return new Vec3(hit.getLocation().x, top, hit.getLocation().z);
        }
        return new Vec3(pos.getX() + 0.5D, top, pos.getZ() + 0.5D);
    }

    private static boolean exposed(Level level, BlockPos ground) {
        BlockState above = level.getBlockState(ground.above());
        return above.getCollisionShape(level, ground.above()).isEmpty() && above.getFluidState().isEmpty();
    }

    private static double entangle(ServerPlayer player, int slot) {
        BlockHitResult hit = aimGround(player, SwampThingConfig.ENTANGLE_RANGE.get());
        if (hit == null) {
            tell(player, "Aim at grass, dirt, mud or moss.");
            return 0.0D;
        }
        ServerLevel level = player.serverLevel();
        if (!exposed(level, hit.getBlockPos())) {
            tell(player, "The roots need open ground to break through.");
            return 0.0D;
        }
        Vec3 center = groundTop(level, hit);
        double radius = SwampThingConfig.ENTANGLE_RADIUS.get();
        float damage = (float) (MarvelBalanceConfig.CRYOGENESIS_DAMAGE.get() * SwampThingConfig.ENTANGLE_DAMAGE_MULTIPLIER.get());
        int bound = rootBurst(player, level, center, radius, damage, false);
        rootFx(level, center, radius);
        HeroFx.play(player, SoundEvents.MANGROVE_ROOTS_BREAK, 0.8F, 0.7F);
        HeroAnimations.play(player, "web_shoot_both");
        if (bound >= 3) {
            HeroAdvancements.award(player, "justice_league/swamp_thing/overgrown");
        }
        return SwampThingConfig.ENTANGLE_COOLDOWN.get();
    }

    private static int rootBurst(ServerPlayer player, ServerLevel level, Vec3 center, double radius, float damage, boolean borrowed) {
        AABB area = new AABB(center.x - radius, center.y - 1.0D, center.z - radius, center.x + radius, center.y + 2.0D, center.z + radius);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area, entity -> {
            if (entity == player || entity instanceof ArmorStand || !KineticBurst.affects(player, entity)) {
                return false;
            }
            double dx = entity.getX() - center.x;
            double dz = entity.getZ() - center.z;
            if (dx * dx + dz * dz > radius * radius) {
                return false;
            }
            Vec3 from = center.add(0.0D, 0.3D, 0.0D);
            Vec3 to = entity.getBoundingBox().getCenter();
            return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
        });
        long now = level.getGameTime();
        int slowTicks = SwampThingConfig.ENTANGLE_SLOW_TICKS.get();
        int slowLevel = SwampThingConfig.ENTANGLE_SLOW_LEVEL.get();
        int bindTicks = SwampThingConfig.ENTANGLE_BIND_TICKS.get();
        int bound = 0;
        for (LivingEntity target : victims) {
            if (damage > 0.0F) {
                HeroAbilities.runAsAbilityDamage(() -> target.hurt(borrowed ? player.damageSources().mobAttack(player)
                        : player.damageSources().playerAttack(player), damage));
            }
            boolean ordinary = HeroAbilities.canControl(target) && !HeroAbilities.isBoss(target);
            if (slowTicks > 0) {
                int amplifier = ordinary ? slowLevel - 1 : Math.max(0, slowLevel - 2);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, amplifier), player);
            }
            if (ordinary && bindTicks > 0 && target.isAlive()) {
                Long immune = BIND_IMMUNE.get(target.getUUID());
                if (immune == null || now >= immune) {
                    HeroStun.stun(target, bindTicks);
                    BIND_IMMUNE.put(target.getUUID(), now + bindTicks + SwampThingConfig.ENTANGLE_BIND_IMMUNITY_TICKS.get());
                    bound++;
                }
            }
            bindFx(level, target);
        }
        if (BIND_IMMUNE.size() > 256) {
            BIND_IMMUNE.values().removeIf(until -> until < now);
        }
        return bound;
    }

    private static void rootFx(ServerLevel level, Vec3 center, double radius) {
        SerumKit.ring(level, center.add(0.0D, 0.1D, 0.0D), ROOTS, radius, 24);
        SerumKit.ring(level, center.add(0.0D, 0.1D, 0.0D), MOSS, radius * 0.5D, 12);
        level.sendParticles(ROOTS, center.x, center.y + 0.2D, center.z, 30, radius * 0.4D, 0.2D, radius * 0.4D, 0.05D);
        level.sendParticles(VINES, center.x, center.y + 0.4D, center.z, 16, radius * 0.4D, 0.3D, radius * 0.4D, 0.02D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ROOTED_DIRT_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.MUD_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static void bindFx(ServerLevel level, LivingEntity target) {
        double height = Math.min(1.2D, target.getBbHeight() * 0.6D);
        for (int i = 0; i < 4; i++) {
            double y = target.getY() + height * i / 3.0D;
            level.sendParticles(HANGING, target.getX(), y, target.getZ(), 4, target.getBbWidth() * 0.4D, 0.05D, target.getBbWidth() * 0.4D, 0.0D);
        }
        level.sendParticles(ROOTS, target.getX(), target.getY() + 0.1D, target.getZ(), 6, target.getBbWidth() * 0.5D, 0.05D, target.getBbWidth() * 0.5D, 0.02D);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.HANGING_ROOTS_PLACE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    private static boolean blockedHere(ServerPlayer player) {
        return SwampThingConfig.ROOT_STEP_BLOCKED_DIMENSIONS.get().contains(player.level().dimension().location().toString());
    }

    private static boolean rootedBelow(Level level, Vec3 feet) {
        return rootable(level.getBlockState(BlockPos.containing(feet.x, feet.y - 0.01D, feet.z)));
    }

    private static Vec3 landing(ServerPlayer player, Vec3 desired, HeroTeleportSafety.Policy policy) {
        Level level = player.level();
        Vec3 from = player.position();
        BlockPos center = BlockPos.containing(desired.x, desired.y - 0.01D, desired.z);
        for (int dy = 0; dy >= -1; dy--) {
            for (int dx = 0; Math.abs(dx) <= 1; dx = dx <= 0 ? 1 - dx : -dx) {
                for (int dz = 0; Math.abs(dz) <= 1; dz = dz <= 0 ? 1 - dz : -dz) {
                    Vec3 feet;
                    if (dx == 0 && dz == 0 && dy == 0) {
                        feet = desired;
                    } else {
                        BlockPos ground = center.offset(dx, dy, dz);
                        if (!rootable(level.getBlockState(ground))) {
                            continue;
                        }
                        double top = ground.getY() + Math.max(0.0D, level.getBlockState(ground).getCollisionShape(level, ground).max(Direction.Axis.Y));
                        feet = new Vec3(ground.getX() + 0.5D, top, ground.getZ() + 0.5D);
                    }
                    if (feet.distanceTo(from) > policy.range() + 2.0D || !rootedBelow(level, feet)) {
                        continue;
                    }
                    if (HeroTeleportSafety.fits(player, feet, false) && HeroTeleportSafety.supported(player, feet)
                            && HeroTeleportSafety.clearPath(player, from, feet)) {
                        return feet;
                    }
                }
            }
        }
        return null;
    }

    private static double rootStep(ServerPlayer player, int slot) {
        if (blockedHere(player)) {
            tell(player, "The roots don't reach here.");
            return 0.0D;
        }
        HeroTeleportSafety.Policy policy = ROOT_STEP_POLICY.withRange(SwampThingConfig.ROOT_STEP_RANGE.get());
        if (!HeroTeleportSafety.canStart(player) || HeroTeleportSafety.throttled(player, policy)) {
            return 0.0D;
        }
        if (!standingOnRoots(player)) {
            tell(player, "You need to stand on grass, dirt, mud or moss.");
            return 0.0D;
        }
        BlockHitResult hit = aimGround(player, policy.range());
        if (hit == null) {
            tell(player, "Look at a patch of natural ground you can see.");
            return 0.0D;
        }
        ServerLevel level = player.serverLevel();
        Vec3 desired = groundTop(level, hit);
        if (desired.distanceTo(player.position()) < 1.5D) {
            return 0.0D;
        }
        Vec3 to = landing(player, desired, policy);
        if (to == null) {
            HeroTeleportSafety.failed(player);
            tell(player, "There's no room to come up there.");
            return 0.0D;
        }
        Vec3 origin = player.position();
        BlockState sinkGround = player.getBlockStateOn();
        if (!HeroTeleportSafety.move(player, to, player.getYRot(), player.getXRot(), policy)) {
            HeroTeleportSafety.failed(player);
            tell(player, "The roots can't get through.");
            return 0.0D;
        }
        HeroTeleportSafety.forget(player);
        player.fallDistance = 0.0F;
        sinkFx(level, origin, sinkGround);
        emergeFx(level, player.position(), player.getBlockStateOn());
        return SwampThingConfig.ROOT_STEP_COOLDOWN.get();
    }

    private static void sinkFx(ServerLevel level, Vec3 at, BlockState ground) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1D, at.z, 30, 0.4D, 0.1D, 0.4D, 0.1D);
        level.sendParticles(ROOTS, at.x, at.y + 0.6D, at.z, 20, 0.3D, 0.6D, 0.3D, 0.02D);
        level.sendParticles(MOSS, at.x, at.y + 1.0D, at.z, 12, 0.3D, 0.6D, 0.3D, 0.02D);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.MUD_STEP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.MUDDY_MANGROVE_ROOTS_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    private static void emergeFx(ServerLevel level, Vec3 at, BlockState ground) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1D, at.z, 30, 0.4D, 0.1D, 0.4D, 0.15D);
        level.sendParticles(ROOTS, at.x, at.y + 0.8D, at.z, 24, 0.35D, 0.7D, 0.35D, 0.03D);
        level.sendParticles(HANGING, at.x, at.y + 1.6D, at.z, 12, 0.3D, 0.3D, 0.3D, 0.0D);
        level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, at.x, at.y + 2.0D, at.z, 8, 0.4D, 0.2D, 0.4D, 0.0D);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.MANGROVE_ROOTS_PLACE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.7F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.MOSS_PLACE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    public static RogueMemory rogueMemory() {
        if (rogue == null) {
            rogue = RogueKit.memory("swamp_thing", "Swamp Thing")
                    .active("Root Pulse", SwampThingPower::rogueRootPulse)
                    .build();
        }
        return rogue;
    }

    private static double rogueRootPulse(ServerPlayer player) {
        if (!standingOnRoots(player)) {
            tell(player, "You need to stand on grass, dirt, mud or moss.");
            return 0.0D;
        }
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        double radius = SwampThingConfig.ROGUE_RADIUS.get();
        float damage = RogueKit.dmg(MarvelBalanceConfig.CRYOGENESIS_DAMAGE.get() * SwampThingConfig.ROGUE_DAMAGE_MULTIPLIER.get());
        rootBurst(player, level, center, radius, damage, true);
        rootFx(level, center, radius);
        return SwampThingConfig.ROGUE_COOLDOWN.get();
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_HURT.remove(player.getUUID());
            MESSAGED.remove(player.getUUID());
        }
    }

    private static void onStopping(ServerStoppingEvent event) {
        BIND_IMMUNE.clear();
        LAST_HURT.clear();
        MESSAGED.clear();
    }
}
