package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.FallbackGrenade;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.ScorchedGunsWeapons;
import com.craisinlord.integrated_superheroes.suit.SuitFx;
import com.craisinlord.integrated_superheroes.registry.ModTags;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tintankgames.marvel.world.entity.HydraAgent;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class DeadpoolPower {
    public static final String ID = "weapon_x_wade_wilson";
    public static final String UNLOAD = "unload";
    public static final String PULL_THE_PIN = "pull_the_pin";
    public static final String RETCON = "retcon";
    public static final String FOURTH_WALL = "fourth_wall";
    public static final String SMASH_CUT = "smash_cut";
    public static final String PAUSE_MENU = "pause_menu";
    public static final String RAGE_QUIT = "rage_quit";
    private static final EffectCure DEADPOOL_CURE = EffectCure.get("deadpool");
    private static final String INJECTED = "integrated_superheroes_deadpool_injected";
    private static final String DODGES = "integrated_superheroes_deadpool_dodges";
    private static final String WALLS = "integrated_superheroes_deadpool_walls";
    private static final List<HeroWheels.Option> OPTIONS = List.of(
            new HeroWheels.Option(SMASH_CUT, "Smash Cut", () -> Items.RED_BED),
            new HeroWheels.Option(PAUSE_MENU, "Pause Menu", () -> Items.CLOCK),
            new HeroWheels.Option(RAGE_QUIT, "Rage Quit", () -> Items.GLASS_PANE)
    );
    private static final Map<UUID, Deque<Snapshot>> HISTORY = new HashMap<>();
    private static final Map<UUID, Unload> UNLOADS = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> WHEEL_READY = new HashMap<>();
    private static final Map<UUID, Cut> CUTS = new HashMap<>();
    private static final Map<UUID, Long> PAUSED_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> FUMBLE_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> FRESH_TARGETS = new HashMap<>();
    private static final Map<UUID, Long> DODGE_READY = new HashMap<>();
    private static final Map<UUID, Deque<Long>> KILLS = new HashMap<>();
    private static final Map<UUID, Ambient> AMBIENT = new HashMap<>();

    private DeadpoolPower() {
    }

    private record Snapshot(ResourceKey<Level> dimension, Vec3 position, float yRot, float xRot, float health) {
    }

    private record Cut(ResourceKey<Level> dimension, Vec3 destination, long due) {
    }

    private static final class Unload {
        private int shotsLeft;
        private long endsAt;
        private int kills;
        private final Set<UUID> hit = new HashSet<>();

        private Unload(int shots) {
            this.shotsLeft = shots;
        }
    }

    private static final class Ambient {
        private boolean night;
        private boolean rain;
        private boolean deep;
        private boolean deepDark;
        private boolean full;
        private boolean sleeping;
        private Vec3 lastPosition = Vec3.ZERO;
        private float lastYRot;
        private long lastMoved;
        private int digDown;
    }

    public static HeroPower create(Supplier<Item> syringe) {
        HeroWheels.register(new HeroWheels.Wheel(FOURTH_WALL, OPTIONS, (player, option) -> true, DeadpoolPower::fourthWall));
        return new HeroPower(
                ID,
                "Weapon X (Wade Wilson)",
                syringe,
                MarvelBalanceConfig.DEADPOOL,
                List.of("agility"),
                List.of(
                        new HeroPower.Ability(UNLOAD, "Unload", DeadpoolPower::unload),
                        new HeroPower.Ability(PULL_THE_PIN, "Pull the Pin", DeadpoolPower::pullThePin),
                        new HeroPower.Ability(RETCON, "Retcon", DeadpoolPower::retcon),
                        new HeroPower.Ability(FOURTH_WALL, "Fourth Wall", (player, slot) -> 0.0D),
                        HeroToggles.WHEEL
                ),
                List.of(DeadpoolNarration.TOGGLE),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        tickUnload(player);
                        tickCut(player);
                        if (player.tickCount % 10 == 0) {
                            tickThreats(player);
                        }
                        if (player.tickCount % 20 != 0) {
                            return;
                        }
                        ensureAbilities(player);
                        snapshot(player);
                        if (player.getHealth() < player.getMaxHealth() && !HealingFactor.regenPaused(player)) {
                            player.heal((float) (player.getMaxHealth() * MarvelBalanceConfig.DEADPOOL_REGEN_PERCENT_PER_SECOND.get()));
                        }
                        player.removeEffectsCuredBy(DEADPOOL_CURE);
                        tickAmbient(player);
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        UUID id = player.getUUID();
                        HealingFactor.clear(player);
                        HISTORY.remove(id);
                        UNLOADS.remove(id);
                        WHEEL_READY.remove(id);
                        CUTS.remove(id);
                        PAUSED_UNTIL.remove(id);
                        FUMBLE_UNTIL.remove(id);
                        DODGE_READY.remove(id);
                        KILLS.remove(id);
                        AMBIENT.remove(id);
                        persisted(player).remove(INJECTED);
                        DeadpoolNarration.speak(player, "swap");
                        DeadpoolNarration.forget(id);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (readTheScript(player, event)) {
                            return;
                        }
                        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof AbstractSkeleton) {
                            DeadpoolNarration.say(player, "skeleton");
                        }
                        if (event.getSource().is(DamageTypeTags.IS_FALL) && event.getAmount() < player.getHealth()) {
                            DeadpoolNarration.say(player, event.getAmount() >= 8.0F ? "superhero_landing" : "fall");
                        }
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (!HeroAbilities.isAbilityDamage() && holdsGun(player) && event.getSource().getDirectEntity() != player) {
                            event.setAmount(event.getAmount() * (1.0F + MarvelBalanceConfig.DEADPOOL_GUN_BONUS.get().floatValue()));
                        }
                    }
                }
        );
    }

    public static void registerEvents() {
        NeoForge.EVENT_BUS.addListener(DeadpoolPower::onTarget);
        NeoForge.EVENT_BUS.addListener(DeadpoolPower::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(DeadpoolPower::onFinishUsing);
        NeoForge.EVENT_BUS.addListener(DeadpoolPower::onBreak);
        NeoForge.EVENT_BUS.addListener(DeadpoolPower::onOpenContainer);
    }

    public static boolean isDeadpool(Player player) {
        return ID.equals(HeroPowers.data(player).getPrimaryPower());
    }

    private static boolean holdsGun(Player player) {
        if (!ModCompat.SCORCHED_GUNS) {
            return true;
        }
        return ScorchedGunsWeapons.isGun(player.getMainHandItem()) || ScorchedGunsWeapons.isGun(player.getOffhandItem());
    }

    private static long now(ServerPlayer player) {
        return player.server.getTickCount();
    }

    private static CompoundTag persisted(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static void ensureAbilities(ServerPlayer player) {
        HeroPower hero = HeroPowers.get(ID);
        PlayerData data = HeroPowers.data(player);
        boolean changed = false;
        for (HeroPower.Ability ability : hero.abilities()) {
            if (!data.hasActivePower(ability.name()) && !data.hasAvailablePower(ability.name())) {
                data.addActivePowers(new Power(ability.name(), ability.displayName(), 2, 0.0D, false, false));
                changed = true;
            }
        }
        if (changed) {
            PlayerData.sync(player);
        }
        CompoundTag persisted = persisted(player);
        if (!persisted.getBoolean(INJECTED)) {
            persisted.putBoolean(INJECTED, true);
            DeadpoolAdvancements.award(player, DeadpoolAdvancements.ROOT);
            DeadpoolNarration.say(player, "inject");
        }
    }

    private static double unload(ServerPlayer player, int slot) {
        if (!holdsGun(player)) {
            if (!DeadpoolNarration.say(player, "no_gun")) {
                player.displayClientMessage(Component.literal("§cForgot the guns. Again."), true);
            }
            return 0.0D;
        }
        UNLOADS.put(player.getUUID(), new Unload(MarvelBalanceConfig.UNLOAD_SHOTS.get()));
        DeadpoolNarration.say(player, "unload");
        return MarvelBalanceConfig.UNLOAD_COOLDOWN.get();
    }

    private static void tickUnload(ServerPlayer player) {
        Unload unload = UNLOADS.get(player.getUUID());
        if (unload == null) {
            return;
        }
        long now = now(player);
        if (unload.shotsLeft <= 0) {
            if (now > unload.endsAt) {
                UNLOADS.remove(player.getUUID());
            }
            return;
        }
        if (player.tickCount % 2 != 0) {
            return;
        }
        if (!holdsGun(player)) {
            unload.shotsLeft = 0;
            unload.endsAt = now + 100L;
            return;
        }
        fireShot(player, unload, unload.shotsLeft % 2 == 0);
        unload.shotsLeft--;
        if (unload.shotsLeft <= 0) {
            unload.endsAt = now + 100L;
        }
    }

    private static void fireShot(ServerPlayer player, Unload unload, boolean rightHand) {
        ServerLevel level = player.serverLevel();
        double range = MarvelBalanceConfig.UNLOAD_RANGE.get();
        Vec3 eye = player.getEyePosition();
        LivingEntity target = HeroAbilities.cone(player, range, 30.0D).stream()
                .filter(entity -> entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget() == player)
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .orElse(null);
        Vec3 direction = target == null ? player.getLookAngle() : target.getBoundingBox().getCenter().subtract(eye).normalize();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, direction, range, 1);
        float damage = (float) (HeroPowers.powerDamage(player) * MarvelBalanceConfig.UNLOAD_DAMAGE_MULTIPLIER.get());
        for (LivingEntity hit : beam.targets()) {
            unload.hit.add(hit.getUUID());
            hit.invulnerableTime = 0;
            HeroAbilities.runAsAbilityDamage(() -> hit.hurt(player.damageSources().playerAttack(player), damage));
        }
        Vec3 right = player.getLookAngle().cross(new Vec3(0.0D, 1.0D, 0.0D));
        right = right.lengthSqr() < 1.0E-6D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
        Vec3 barrel = eye.add(0.0D, -0.25D, 0.0D).add(right.scale(rightHand ? 0.35D : -0.35D)).add(direction.scale(0.7D));
        SuitFx.burst(level, SuitFx.MUZZLE_FLASH, ParticleTypes.FLAME, barrel, 1, 0.0D, 0.0D);
        level.sendParticles(SuitFx.particle(SuitFx.BRASS_CASING, ParticleTypes.CRIT), barrel.x, barrel.y, barrel.z, 0,
                right.x * (rightHand ? 0.3D : -0.3D), 0.25D, right.z * (rightHand ? 0.3D : -0.3D), 1.0D);
        HeroAbilities.particleLine(level, barrel, beam.end(), ParticleTypes.CRIT, 0.8D);
        SuitFx.sound(level, player.position(), "item.iron_pistol.fire", SoundEvents.FIREWORK_ROCKET_BLAST, 0.9F, 1.1F + player.getRandom().nextFloat() * 0.2F);
    }

    private static double pullThePin(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        float damage = (float) (HeroPowers.powerDamage(player) * MarvelBalanceConfig.GRENADE_DAMAGE_MULTIPLIER.get());
        float radius = MarvelBalanceConfig.GRENADE_RADIUS.get().floatValue();
        SuitFx.sound(level, player.position(), SuitFx.GRENADE_PIN, SoundEvents.TRIPWIRE_CLICK_ON, 1.0F, 1.0F);
        if (player.getRandom().nextDouble() < MarvelBalanceConfig.GRENADE_FUMBLE_CHANCE.get()) {
            Entity grenade = ModCompat.SCORCHED_GUNS
                    ? ScorchedGunsWeapons.grenade(level, player, 2, damage, radius)
                    : FallbackGrenade.create(level, player, 2).explosive(damage, radius);
            grenade.setPos(player.getX(), player.getY() + 1.0D, player.getZ());
            grenade.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(grenade);
            FUMBLE_UNTIL.put(player.getUUID(), now(player) + 60L);
            DeadpoolNarration.say(player, "fumble");
            DeadpoolAdvancements.award(player, DeadpoolAdvancements.OOPS);
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().explosion(grenade, null), MarvelBalanceConfig.GRENADE_FUMBLE_SELF_DAMAGE.get().floatValue());
            return MarvelBalanceConfig.GRENADE_COOLDOWN.get();
        }
        if (ModCompat.SCORCHED_GUNS) {
            Projectile grenade = ScorchedGunsWeapons.grenade(level, player, 30, damage, radius);
            grenade.shootFromRotation(player, player.getXRot() - 6.0F, player.getYRot(), 0.0F, 1.4F, 1.0F);
            level.addFreshEntity(grenade);
        } else {
            level.addFreshEntity(FallbackGrenade.create(level, player, 30).explosive(damage, radius)
                    .throwFrom(player, player.getXRot() - 6.0F, player.getYRot(), 1.4F));
        }
        DeadpoolNarration.say(player, "grenade");
        return MarvelBalanceConfig.GRENADE_COOLDOWN.get();
    }

    private static void snapshot(ServerPlayer player) {
        Deque<Snapshot> history = HISTORY.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        history.addLast(new Snapshot(player.level().dimension(), player.position(), player.getYRot(), player.getXRot(), player.getHealth()));
        while (history.size() > MarvelBalanceConfig.RETCON_SECONDS.get() + 1) {
            history.removeFirst();
        }
    }

    private static double retcon(ServerPlayer player, int slot) {
        Deque<Snapshot> history = HISTORY.get(player.getUUID());
        Snapshot saved = history == null ? null : history.stream()
                .filter(entry -> entry.dimension() == player.level().dimension())
                .findFirst()
                .orElse(null);
        if (saved == null) {
            player.displayClientMessage(Component.literal("§7Nothing to retcon yet."), true);
            return 0.0D;
        }
        ServerLevel level = player.serverLevel();
        float before = player.getHealth();
        boolean desperate = before < player.getMaxHealth() * 0.2F;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.4D, 0.8D, 0.4D, 0.05D);
        HeroAbilities.particleLine(level, player.position().add(0.0D, 1.0D, 0.0D), saved.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.REVERSE_PORTAL, 0.5D);
        player.teleportTo(level, saved.position().x, saved.position().y, saved.position().z, saved.yRot(), saved.xRot());
        player.fallDistance = 0.0F;
        player.setHealth(Math.max(before, Math.min(saved.health(), before + MarvelBalanceConfig.RETCON_HEAL_CAP.get().floatValue())));
        history.clear();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.4D, 0.8D, 0.4D, 0.05D);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
        PacketDistributor.sendToPlayer(player, new DeadpoolFxPayload(DeadpoolFxPayload.RETCON, 10));
        DeadpoolNarration.say(player, "retcon");
        if (desperate) {
            DeadpoolAdvancements.award(player, DeadpoolAdvancements.THAT_NEVER_HAPPENED);
        }
        return MarvelBalanceConfig.RETCON_COOLDOWN.get();
    }

    private static void fourthWall(ServerPlayer player, String option) {
        if (HealingFactor.isDowned(player)) {
            return;
        }
        long now = now(player);
        Map<String, Long> ready = WHEEL_READY.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        long readyAt = ready.getOrDefault(option, 0L);
        String label = OPTIONS.stream().filter(entry -> entry.id().equals(option)).map(HeroWheels.Option::label).findFirst().orElse(option);
        if (now < readyAt) {
            player.displayClientMessage(Component.literal("§7" + label + " recharging: " + ((readyAt - now) / 20L + 1L) + "s"), true);
            return;
        }
        double cooldown = switch (option) {
            case SMASH_CUT -> smashCut(player) ? MarvelBalanceConfig.SMASH_CUT_COOLDOWN.get() : 0.0D;
            case PAUSE_MENU -> pauseMenu(player);
            case RAGE_QUIT -> rageQuit(player);
            default -> 0.0D;
        };
        if (cooldown <= 0.0D) {
            return;
        }
        ready.put(option, now + (long) (cooldown * 20.0D));
        CompoundTag walls = persisted(player).getCompound(WALLS);
        walls.putBoolean(option, true);
        persisted(player).put(WALLS, walls);
        if (OPTIONS.stream().allMatch(entry -> walls.getBoolean(entry.id()))) {
            DeadpoolAdvancements.award(player, DeadpoolAdvancements.FIFTH_WALL);
        }
    }

    private static boolean smashCut(ServerPlayer player) {
        ServerLevel level = null;
        Vec3 destination = null;
        BlockPos respawn = player.getRespawnPosition();
        if (respawn != null) {
            level = player.server.getLevel(player.getRespawnDimension());
            if (level != null) {
                destination = standUpSpot(level, respawn, player.getYRot()).orElse(Vec3.atBottomCenterOf(respawn.above()));
            }
        }
        if (level == null || destination == null) {
            level = player.server.overworld();
            destination = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, level.getSharedSpawnPos()));
        }
        CUTS.put(player.getUUID(), new Cut(level.dimension(), destination, now(player) + 20L));
        PacketDistributor.sendToPlayer(player, new DeadpoolFxPayload(DeadpoolFxPayload.LOADING, 40));
        DeadpoolNarration.say(player, "smash_cut");
        return true;
    }

    private static Optional<Vec3> standUpSpot(ServerLevel level, BlockPos pos, float yRot) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BedBlock) {
            return BedBlock.findStandUpPosition(EntityType.PLAYER, level, pos, state.getValue(BedBlock.FACING), yRot);
        }
        if (state.getBlock() instanceof RespawnAnchorBlock) {
            return RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, level, pos);
        }
        return Optional.empty();
    }

    private static void tickCut(ServerPlayer player) {
        Cut cut = CUTS.get(player.getUUID());
        if (cut == null || now(player) < cut.due()) {
            return;
        }
        CUTS.remove(player.getUUID());
        ServerLevel level = player.server.getLevel(cut.dimension());
        if (level == null || HealingFactor.isDowned(player)) {
            return;
        }
        boolean far = level != player.serverLevel() || player.position().distanceTo(cut.destination()) >= 1000.0D;
        player.teleportTo(level, cut.destination().x, cut.destination().y, cut.destination().z, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
        if (far) {
            DeadpoolAdvancements.award(player, DeadpoolAdvancements.MONTAGE_SKIPPED);
        }
    }

    private static double pauseMenu(ServerPlayer player) {
        int ticks = (int) (MarvelBalanceConfig.PAUSE_MENU_SECONDS.get() * 20.0D);
        for (LivingEntity entity : HeroAbilities.sphere(player, MarvelBalanceConfig.PAUSE_MENU_RADIUS.get())) {
            if (entity instanceof Mob && HeroAbilities.canControl(entity)) {
                HeroStun.stun(entity, ticks);
            }
        }
        PAUSED_UNTIL.put(player.getUUID(), now(player) + ticks);
        PacketDistributor.sendToPlayer(player, new DeadpoolFxPayload(DeadpoolFxPayload.PAUSE, ticks));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        DeadpoolNarration.say(player, "pause");
        return MarvelBalanceConfig.PAUSE_MENU_COOLDOWN.get();
    }

    private static double rageQuit(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        float damage = (float) (HeroPowers.powerDamage(player) * MarvelBalanceConfig.RAGE_QUIT_DAMAGE_MULTIPLIER.get());
        double knockback = MarvelBalanceConfig.RAGE_QUIT_KNOCKBACK.get();
        for (LivingEntity target : HeroAbilities.sphere(player, MarvelBalanceConfig.RAGE_QUIT_RADIUS.get())) {
            if (!HeroAbilities.canControl(target) || target instanceof OwnableEntity pet && player.getUUID().equals(pet.getOwnerUUID())) {
                continue;
            }
            target.invulnerableTime = 0;
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
            target.knockback(knockback, player.getX() - target.getX(), player.getZ() - target.getZ());
            target.hurtMarked = true;
        }
        Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(1.2D));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()), front.x, front.y, front.z, 60, 0.6D, 0.6D, 0.6D, 0.2D);
        level.sendParticles(ParticleTypes.EXPLOSION, front.x, front.y, front.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5F, 0.8F);
        HeroAnimations.play(player, "uppercut");
        HeroFx.shake(level, player.position(), 16.0D, 0.6F, 8);
        int ticks = (int) (MarvelBalanceConfig.RAGE_QUIT_CRACK_SECONDS.get() * 20.0D);
        PacketDistributor.sendToPlayer(player, new DeadpoolFxPayload(DeadpoolFxPayload.BREAK, ticks));
        DeadpoolNarration.say(player, "rage_quit");
        return MarvelBalanceConfig.RAGE_QUIT_COOLDOWN.get();
    }

    private static boolean readTheScript(ServerPlayer player, LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Mob mob)) {
            return false;
        }
        Long targetedAt = FRESH_TARGETS.remove(mob.getUUID());
        long now = now(player);
        if (targetedAt == null || now - targetedAt > MarvelBalanceConfig.READ_THE_SCRIPT_WINDOW.get() * 20.0D
                || now < DODGE_READY.getOrDefault(player.getUUID(), 0L)) {
            return false;
        }
        event.setCanceled(true);
        DODGE_READY.put(player.getUUID(), now + (long) (MarvelBalanceConfig.READ_THE_SCRIPT_COOLDOWN.get() * 20.0D));
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0D, player.getZ(), 8, 0.3D, 0.5D, 0.3D, 0.02D);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.0F, 1.2F);
        DeadpoolNarration.say(player, "dodge");
        CompoundTag persisted = persisted(player);
        int dodges = persisted.getInt(DODGES) + 1;
        persisted.putInt(DODGES, dodges);
        if (dodges >= 50) {
            DeadpoolAdvancements.award(player, DeadpoolAdvancements.I_READ_THE_WIKI);
        }
        return true;
    }

    private static void onTarget(LivingChangeTargetEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof Mob mob
                && event.getNewAboutToBeSetTarget() instanceof ServerPlayer player && isDeadpool(player)
                && mob.getTarget() != player) {
            FRESH_TARGETS.put(mob.getUUID(), now(player));
            if (FRESH_TARGETS.size() > 256) {
                long cutoff = now(player) - (long) (MarvelBalanceConfig.READ_THE_SCRIPT_WINDOW.get() * 20.0D);
                FRESH_TARGETS.values().removeIf(time -> time < cutoff);
            }
        }
    }

    public static void onAnyDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer player) || player == event.getEntity() || !isDeadpool(player)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        long now = now(player);
        Deque<Long> kills = KILLS.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        kills.addLast(now);
        while (!kills.isEmpty() && now - kills.peekFirst() > 80L) {
            kills.removeFirst();
        }
        if (kills.size() >= 3) {
            kills.clear();
            DeadpoolNarration.say(player, "combo");
        }
        Unload unload = UNLOADS.get(player.getUUID());
        if (unload != null && unload.hit.contains(victim.getUUID()) && ++unload.kills >= 5) {
            DeadpoolAdvancements.award(player, DeadpoolAdvancements.GUN_KATA);
        }
        if (now < PAUSED_UNTIL.getOrDefault(player.getUUID(), 0L)) {
            DeadpoolAdvancements.award(player, DeadpoolAdvancements.UNPAUSE);
        }
        if (now < FUMBLE_UNTIL.getOrDefault(player.getUUID(), 0L) && event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            DeadpoolAdvancements.award(player, DeadpoolAdvancements.SELF_INFLICTED_COMBO);
        }
        if (victim instanceof HydraAgent) {
            DeadpoolNarration.say(player, "hydra");
        }
    }

    private static void tickThreats(ServerPlayer player) {
        if (!player.level().getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(6.0D), creeper -> creeper.getSwellDir() > 0).isEmpty()) {
            DeadpoolNarration.say(player, "creeper");
        }
    }

    private static void tickAmbient(ServerPlayer player) {
        Ambient ambient = AMBIENT.computeIfAbsent(player.getUUID(), ignored -> new Ambient());
        ServerLevel level = player.serverLevel();
        long now = now(player);
        if (player.getHealth() < player.getMaxHealth() * 0.25F && !HealingFactor.isDowned(player)) {
            DeadpoolNarration.say(player, "low_health");
        }
        boolean night = level.dimension() == Level.OVERWORLD && level.isNight();
        if (night && !ambient.night) {
            DeadpoolNarration.say(player, "night");
        }
        ambient.night = night;
        boolean rain = level.isRainingAt(player.blockPosition().above());
        if (rain && !ambient.rain) {
            DeadpoolNarration.say(player, "rain");
        }
        ambient.rain = rain;
        boolean deep = player.getY() < -40.0D;
        if (deep && !ambient.deep) {
            DeadpoolNarration.say(player, "deep");
        }
        ambient.deep = deep;
        boolean deepDark = level.getBiome(player.blockPosition()).is(Biomes.DEEP_DARK);
        if (deepDark && !ambient.deepDark) {
            DeadpoolNarration.say(player, "deep_dark");
        }
        ambient.deepDark = deepDark;
        boolean full = player.getInventory().getFreeSlot() == -1;
        if (full && !ambient.full) {
            DeadpoolNarration.say(player, "inventory_full");
        }
        ambient.full = full;
        if (player.isSleeping() && !ambient.sleeping) {
            DeadpoolNarration.say(player, "sleep");
        }
        ambient.sleeping = player.isSleeping();
        if (player.isUnderWater() && player.getAirSupply() < 60) {
            DeadpoolNarration.say(player, "drowning");
        }
        if (player.position().distanceToSqr(ambient.lastPosition) > 0.01D || Math.abs(player.getYRot() - ambient.lastYRot) > 1.0F) {
            ambient.lastPosition = player.position();
            ambient.lastYRot = player.getYRot();
            ambient.lastMoved = now;
        } else if (now - ambient.lastMoved >= 1200L) {
            ambient.lastMoved = now;
            DeadpoolNarration.say(player, "idle");
        }
        if (!level.getEntitiesOfClass(Warden.class, player.getBoundingBox().inflate(24.0D)).isEmpty()) {
            DeadpoolNarration.say(player, "warden");
        }
        if (!level.getEntitiesOfClass(EnderDragon.class, player.getBoundingBox().inflate(96.0D)).isEmpty()) {
            DeadpoolNarration.say(player, "dragon");
        }
        if (!level.getEntities(player, player.getBoundingBox().inflate(48.0D),
                entity -> entity.getType().is(ModTags.DEADPOOL_WATCHERS)).isEmpty()) {
            DeadpoolNarration.say(player, "watcher");
        }
        if (!level.getEntitiesOfClass(HydraAgent.class, player.getBoundingBox().inflate(24.0D)).isEmpty()) {
            DeadpoolNarration.say(player, "hydra");
        }
        if (level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24.0D), mob -> mob.getTarget() == player).size() == 2) {
            DeadpoolNarration.say(player, "two_mobs");
        }
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isDeadpool(player)) {
            if (event.getTo() == Level.NETHER) {
                DeadpoolNarration.say(player, "nether");
            } else if (event.getTo() == Level.END) {
                DeadpoolNarration.say(player, "end");
            }
        }
    }

    private static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && isDeadpool(player) && event.getItem().getFoodProperties(player) != null) {
            DeadpoolNarration.say(player, "eat");
        }
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !isDeadpool(player)) {
            return;
        }
        if ("lucky".equals(BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).getNamespace())) {
            DeadpoolNarration.say(player, "lucky");
        }
        Ambient ambient = AMBIENT.computeIfAbsent(player.getUUID(), ignored -> new Ambient());
        if (event.getPos().equals(player.blockPosition().below()) && player.getXRot() > 60.0F) {
            if (++ambient.digDown >= 3) {
                ambient.digDown = 0;
                DeadpoolNarration.say(player, "dig_down");
            }
        } else {
            ambient.digDown = 0;
        }
    }

    private static void onOpenContainer(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player && isDeadpool(player) && event.getContainer() instanceof CraftingMenu) {
            DeadpoolNarration.say(player, "crafting");
        }
    }
}
