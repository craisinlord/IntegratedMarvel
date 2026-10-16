package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.mixin.FireworkRocketAccessor;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.Power;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class JubileePower {
    public static final String ID = "pyrotechnics_jubilation_lee";
    public static final String PALETTE_ABILITY = "jubilee_palette";
    public static final HeroPower.Toggle LIGHT_SHOW = new HeroPower.Toggle("integrated_superheroes_off_light_show", "Light Show", () -> Items.GLOWSTONE_DUST);
    public static final String ROCKET_TAG = "integrated_superheroes_jubilee";
    private static final String DAMAGE_KEY = "integrated_superheroes_jubilee_damage";
    private static final String PALETTE_KEY = "integrated_superheroes_jubilee_palette";
    private static final Map<UUID, Homing> HOMING = new HashMap<>();
    private static final Map<UUID, Finale> FINALES = new HashMap<>();
    private static final Map<UUID, Integer> LEAPING = new HashMap<>();

    private static final List<HeroWheels.Option> OPTIONS = java.util.Arrays.stream(Palette.values())
            .map(palette -> new HeroWheels.Option(palette.id, palette.label, palette.icon))
            .toList();

    private JubileePower() {
    }

    public enum Palette {
        RAINBOW("rainbow", "Rainbow", () -> Items.PRISMARINE_CRYSTALS, new int[]{0xFF3B3B, 0xFF9F1C, 0xFFE81C, 0x3BFF5C, 0x2EC4FF, 0x6B5BFF, 0xD95BFF}),
        XMEN("x_men", "X-Men Blue & Gold", () -> Items.BLUE_DYE, new int[]{0x1F4FD8, 0x3D7BFF, 0xFFC21C, 0xFFE066}),
        CLASSIC("classic", "Classic Jubilee", () -> Items.PINK_DYE, new int[]{0xFF5FB4, 0xFFD23F, 0xFF8AD0, 0xFFF07A}),
        PATRIOT("patriot", "Red, White & Blue", () -> Items.RED_DYE, new int[]{0xE0242F, 0xFFFFFF, 0x2244CC}),
        GAMBIT("gambit", "Gambit Purple", () -> Items.PURPLE_DYE, new int[]{0x9B30FF, 0xC77DFF, 0x6A0DAD, 0xE0AAFF});

        private final String id;
        private final String label;
        private final Supplier<Item> icon;
        private final int[] colors;

        Palette(String id, String label, Supplier<Item> icon, int[] colors) {
            this.id = id;
            this.label = label;
            this.icon = icon;
            this.colors = colors;
        }

        static Palette byId(String id) {
            for (Palette palette : values()) {
                if (palette.id.equals(id)) {
                    return palette;
                }
            }
            return RAINBOW;
        }

        public int[] colors() {
            return colors;
        }
    }

    private record Homing(UUID owner, UUID target, ResourceKey<Level> dimension) {
    }

    private static final class Finale {
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private int ticks;
        private int fired;

        private Finale(ResourceKey<Level> dimension, Vec3 center) {
            this.dimension = dimension;
            this.center = center;
        }
    }

    public static HeroPower create(Supplier<Item> syringe) {
        HeroWheels.register(new HeroWheels.Wheel(PALETTE_ABILITY, OPTIONS, (player, option) -> true, JubileePower::choosePalette));
        return new HeroPower(
                ID,
                "Pyrotechnics (Jubilation Lee)",
                syringe,
                MarvelBalanceConfig.JUBILEE,
                List.of("agility"),
                List.of(
                        new HeroPower.Ability("paff", "Paff", JubileePower::paff),
                        new HeroPower.Ability("firework_volley", "Firework Volley", JubileePower::volley),
                        new HeroPower.Ability("flashbang", "Flashbang", JubileePower::flashbang),
                        new HeroPower.Ability("pyro_jump", "Pyro Jump", JubileePower::pyroJump),
                        new HeroPower.Ability("grand_finale", "Grand Finale", JubileePower::grandFinale),
                        new HeroPower.Ability(PALETTE_ABILITY, "Palette", (player, slot) -> 0.0D),
                        HeroToggles.WHEEL
                ),
                List.of(LIGHT_SHOW),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (player.tickCount % 20 == 0) {
                            labelPalette(player);
                        }
                        tickLeap(player);
                        if (HeroToggles.isOn(player, LIGHT_SHOW)) {
                            tickLightShow(player);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        LEAPING.remove(player.getUUID());
                        FINALES.remove(player.getUUID());
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypeTags.IS_FALL) && LEAPING.containsKey(player.getUUID())) {
                            event.setCanceled(true);
                        }
                    }
                }
        );
    }

    public static void clear(ServerPlayer player) {
        LEAPING.remove(player.getUUID());
        FINALES.remove(player.getUUID());
    }

    public static Palette palette(Player player) {
        return Palette.byId(player.getPersistentData().getString(PALETTE_KEY));
    }

    private static void choosePalette(ServerPlayer player, String option) {
        Palette palette = Palette.byId(option);
        player.getPersistentData().putString(PALETTE_KEY, palette.id);
        labelPalette(player);
        player.displayClientMessage(Component.literal("§dPalette: " + palette.label), true);
        HeroFx.play(player, SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.6F, 1.4F);
    }

    private static void labelPalette(ServerPlayer player) {
        Power power = HeroAbilities.slotPower(player, PALETTE_ABILITY);
        String label = "Palette: " + palette(player).label;
        if (power != null && !label.equals(power.getDisplayName())) {
            power.setDisplayName(label);
            PlayerData.sync(player);
        }
    }

    private static boolean affects(ServerPlayer player, LivingEntity entity) {
        if (entity == player || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (entity instanceof TamableAnimal tamable && tamable.isTame() && player.getUUID().equals(tamable.getOwnerUUID())) {
            return false;
        }
        if (entity instanceof Player other) {
            return player.canHarmPlayer(other) && !other.isCreative();
        }
        return !player.isAlliedTo(entity);
    }

    private static boolean hostile(ServerPlayer player, LivingEntity entity) {
        return affects(player, entity) && (entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget() == player
                || entity instanceof Player);
    }

    public static ItemStack rocket(RandomSource random, int[] colors, FireworkExplosion.Shape shape, int flight, boolean trail, boolean twinkle) {
        IntList picked = new IntArrayList();
        int count = 2 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            picked.add(colors[random.nextInt(colors.length)]);
        }
        IntList fade = new IntArrayList();
        fade.add(colors[random.nextInt(colors.length)]);
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        stack.set(DataComponents.FIREWORKS, new Fireworks(flight, List.of(new FireworkExplosion(shape, picked, fade, trail, twinkle))));
        return stack;
    }

    private static FireworkExplosion.Shape randomShape(RandomSource random, boolean big) {
        FireworkExplosion.Shape[] shapes = big
                ? new FireworkExplosion.Shape[]{FireworkExplosion.Shape.LARGE_BALL, FireworkExplosion.Shape.STAR, FireworkExplosion.Shape.BURST, FireworkExplosion.Shape.CREEPER}
                : new FireworkExplosion.Shape[]{FireworkExplosion.Shape.SMALL_BALL, FireworkExplosion.Shape.STAR, FireworkExplosion.Shape.BURST};
        return shapes[random.nextInt(shapes.length)];
    }

    private static FireworkRocketEntity launch(ServerPlayer player, Vec3 from, Vec3 direction, double speed, ItemStack stack, float damage, int lifetime) {
        ServerLevel level = player.serverLevel();
        FireworkRocketEntity rocket = new FireworkRocketEntity(level, stack, player, from.x, from.y, from.z, true);
        rocket.shoot(direction.x, direction.y, direction.z, (float) speed, 0.0F);
        rocket.addTag(ROCKET_TAG);
        rocket.getPersistentData().putFloat(DAMAGE_KEY, damage);
        ((FireworkRocketAccessor) rocket).integratedSuperheroes$setLifetime(lifetime);
        level.addFreshEntity(rocket);
        return rocket;
    }

    public static void burst(ServerPlayer player, Vec3 at, ItemStack stack, float damage) {
        ServerLevel level = player.serverLevel();
        FireworkRocketEntity rocket = new FireworkRocketEntity(level, stack, player, at.x, at.y, at.z, true);
        rocket.addTag(ROCKET_TAG);
        rocket.getPersistentData().putFloat(DAMAGE_KEY, damage);
        level.addFreshEntity(rocket);
        ((FireworkRocketAccessor) rocket).integratedSuperheroes$explode();
    }

    private static void sparkle(ServerLevel level, Palette palette, Vec3 at, int count, double spread) {
        level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y, at.z, count, spread, spread, spread, 0.15D);
        if (palette == Palette.RAINBOW && ModCompat.ALEXS_CAVES) {
            level.sendParticles(HeroFx.particle("alexscaves:rainbow", ParticleTypes.FIREWORK), at.x, at.y, at.z, Math.max(1, count / 4), spread, spread, spread, 0.0D);
        }
        if (ModCompat.CELESTISYNTH) {
            String name = switch (palette) {
                case XMEN, PATRIOT -> "celestisynth:crescentia_firework_blue";
                case CLASSIC -> "celestisynth:crescentia_firework_pink";
                default -> "celestisynth:crescentia_firework_purple";
            };
            level.sendParticles(HeroFx.particle(name, ParticleTypes.FIREWORK), at.x, at.y, at.z, Math.max(1, count / 3), spread, spread, spread, 0.05D);
        }
    }

    private static double paff(ServerPlayer player, int slot) {
        Palette palette = palette(player);
        Vec3 look = player.getLookAngle();
        Vec3 from = player.getEyePosition().add(look.scale(0.6D)).subtract(0.0D, 0.15D, 0.0D);
        launch(player, from, look, MarvelBalanceConfig.PAFF_SPEED.get(),
                rocket(player.getRandom(), palette.colors, randomShape(player.getRandom(), false), 0, false, player.getRandom().nextBoolean()),
                MarvelBalanceConfig.PAFF_DAMAGE.get().floatValue(), 30);
        sparkle(player.serverLevel(), palette, from, 6, 0.1D);
        HeroFx.play(player, SoundEvents.FIREWORK_ROCKET_SHOOT, 0.8F, 1.6F);
        HeroAnimations.play(player, "web_shoot_r");
        return MarvelBalanceConfig.PAFF_COOLDOWN.get();
    }

    private static double volley(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        Palette palette = palette(player);
        double range = MarvelBalanceConfig.VOLLEY_RANGE.get();
        List<LivingEntity> targets = new ArrayList<>(HeroAbilities.cone(player, range, 60.0D).stream()
                .filter(entity -> hostile(player, entity))
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .toList());
        if (targets.isEmpty()) {
            targets.addAll(HeroAbilities.sphere(player, range * 0.5D).stream().filter(entity -> hostile(player, entity))
                    .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player))).toList());
        }
        int count = MarvelBalanceConfig.VOLLEY_ROCKETS.get();
        float damage = MarvelBalanceConfig.VOLLEY_DAMAGE.get().floatValue();
        for (int i = 0; i < count; i++) {
            float spread = count == 1 ? 0.0F : -40.0F + 80.0F * i / (count - 1);
            Vec3 direction = Vec3.directionFromRotation(player.getXRot() - 25.0F, player.getYRot() + spread);
            Vec3 from = player.getEyePosition().add(direction.scale(0.8D));
            FireworkRocketEntity rocket = launch(player, from, direction, 0.9D,
                    rocket(player.getRandom(), palette.colors, randomShape(player.getRandom(), false), 1, true, true), damage, 80);
            if (!targets.isEmpty()) {
                LivingEntity target = targets.get(i % targets.size());
                HOMING.put(rocket.getUUID(), new Homing(player.getUUID(), target.getUUID(), level.dimension()));
            }
        }
        sparkle(level, palette, player.getEyePosition(), 20, 0.4D);
        HeroFx.play(player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2F, 1.0F);
        HeroFx.play(player, SoundEvents.FIREWORK_ROCKET_SHOOT, 1.0F, 0.8F);
        HeroAnimations.play(player, "web_shoot_both");
        return MarvelBalanceConfig.VOLLEY_COOLDOWN.get();
    }

    private static double flashbang(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        Palette palette = palette(player);
        Vec3 center = player.getEyePosition().add(player.getLookAngle().scale(1.2D));
        burst(player, center, rocket(player.getRandom(), palette.colors, FireworkExplosion.Shape.LARGE_BALL, 0, true, true), 0.0F);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 3, 0.2D, 0.2D, 0.2D, 0.0D);
        sparkle(level, palette, center, 60, 1.2D);
        double radius = MarvelBalanceConfig.FLASHBANG_RADIUS.get();
        int ticks = MarvelBalanceConfig.FLASHBANG_TICKS.get();
        float damage = MarvelBalanceConfig.FLASHBANG_DAMAGE.get().floatValue();
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            if (!affects(player, target) || !target.hasLineOfSight(player)) {
                continue;
            }
            if (damage > 0.0F) {
                target.hurt(player.damageSources().playerAttack(player), damage);
            }
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0), player);
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ticks, 0), player);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2), player);
            if (target instanceof Mob mob) {
                mob.setTarget(null);
                if (HeroAbilities.canControl(mob)) {
                    HeroStun.stun(mob, ticks / 2);
                }
            }
        }
        HeroFx.play(player, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 2.0F, 1.2F);
        HeroFx.play(player, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.5F, 1.0F);
        HeroAnimations.play(player, "web_shoot_both");
        return MarvelBalanceConfig.FLASHBANG_COOLDOWN.get();
    }

    private static double pyroJump(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        Palette palette = palette(player);
        if (player.isFallFlying()) {
            ItemStack stack = rocket(player.getRandom(), palette.colors, FireworkExplosion.Shape.BURST, MarvelBalanceConfig.PYRO_JUMP_ELYTRA_FLIGHT.get(), true, false);
            level.addFreshEntity(new FireworkRocketEntity(level, stack, player));
            HeroFx.play(player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
            return MarvelBalanceConfig.PYRO_JUMP_COOLDOWN.get() * 0.5D;
        }
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        Vec3 horizontal = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize().scale(MarvelBalanceConfig.PYRO_JUMP_HORIZONTAL.get());
        player.setDeltaMovement(horizontal.x, MarvelBalanceConfig.PYRO_JUMP_VERTICAL.get(), horizontal.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        LEAPING.put(player.getUUID(), 0);
        burst(player, player.position().add(0.0D, 0.2D, 0.0D),
                rocket(player.getRandom(), palette.colors, FireworkExplosion.Shape.BURST, 0, false, true), 0.0F);
        sparkle(level, palette, player.position(), 30, 0.5D);
        HeroFx.play(player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2F, 1.2F);
        return MarvelBalanceConfig.PYRO_JUMP_COOLDOWN.get();
    }

    private static void tickLeap(ServerPlayer player) {
        Integer ticks = LEAPING.get(player.getUUID());
        if (ticks == null) {
            return;
        }
        if (ticks > 5 && (player.onGround() || player.isInWater() || player.isFallFlying())) {
            LEAPING.remove(player.getUUID());
            return;
        }
        LEAPING.put(player.getUUID(), ticks + 1);
        if (ticks < 20) {
            ServerLevel level = player.serverLevel();
            level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY(), player.getZ(), 3, 0.15D, 0.05D, 0.15D, 0.02D);
            if (ticks % 2 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY(), player.getZ(), 2, 0.2D, 0.1D, 0.2D, 0.05D);
            }
        }
    }

    private static double grandFinale(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        Palette palette = palette(player);
        Finale finale = new Finale(level.dimension(), player.position());
        FINALES.put(player.getUUID(), finale);
        for (int i = 0; i < 10; i++) {
            Vec3 direction = Vec3.directionFromRotation(-75.0F, 36.0F * i);
            launch(player, player.position().add(direction.x, 1.0D, direction.z), direction, 0.8D,
                    rocket(player.getRandom(), palette.colors, randomShape(player.getRandom(), true), 1, true, true), 0.0F, 16 + player.getRandom().nextInt(6));
        }
        sparkle(level, palette, player.position().add(0.0D, 1.0D, 0.0D), 50, 1.0D);
        HeroFx.play(player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 2.0F, 0.7F);
        HeroFx.play(player, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, 2.0F, 0.8F);
        player.displayClientMessage(Component.literal("§dGrand Finale!"), true);
        HeroAnimations.play(player, "web_shoot_both");
        return MarvelBalanceConfig.FINALE_COOLDOWN.get();
    }

    private static void tickFinale(ServerPlayer player, Finale finale) {
        ServerLevel level = player.serverLevel();
        int total = MarvelBalanceConfig.FINALE_TICKS.get();
        int rockets = MarvelBalanceConfig.FINALE_ROCKETS.get();
        finale.ticks++;
        int due = Math.min(rockets, (int) Math.ceil((double) finale.ticks * rockets / total));
        double radius = MarvelBalanceConfig.FINALE_RADIUS.get();
        RandomSource random = player.getRandom();
        Palette palette = palette(player);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(finale.center, finale.center).inflate(radius, 8.0D, radius),
                entity -> hostile(player, entity) && entity.position().distanceToSqr(finale.center) <= radius * radius);
        while (finale.fired < due) {
            finale.fired++;
            Vec3 aim;
            if (!targets.isEmpty() && random.nextFloat() < 0.75F) {
                aim = targets.get(random.nextInt(targets.size())).getBoundingBox().getCenter();
            } else {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double distance = Math.sqrt(random.nextDouble()) * radius;
                aim = finale.center.add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
            }
            Vec3 from = aim.add(random.nextGaussian() * 3.0D, 14.0D + random.nextDouble() * 4.0D, random.nextGaussian() * 3.0D);
            Vec3 direction = aim.subtract(from).normalize();
            launch(player, from, direction, 1.6D,
                    rocket(random, palette.colors, randomShape(random, true), 1, true, true), MarvelBalanceConfig.FINALE_DAMAGE.get().floatValue(), 40);
            sparkle(level, palette, from, 6, 0.3D);
        }
        if (finale.ticks % 10 == 0) {
            level.playSound(null, finale.center.x, finale.center.y + 10.0D, finale.center.z, SoundEvents.FIREWORK_ROCKET_BLAST_FAR,
                    net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 0.8F + random.nextFloat() * 0.4F);
        }
        if (finale.ticks >= total) {
            FINALES.remove(player.getUUID());
        }
    }

    private static void tickLightShow(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (player.tickCount % 3 == 0) {
            Vec3 look = player.getLookAngle();
            Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize().scale(0.35D);
            Vec3 hand = player.position().add(0.0D, player.getBbHeight() * 0.55D, 0.0D).add(look.scale(0.3D));
            Vec3 left = hand.add(side);
            Vec3 right = hand.subtract(side);
            ParticleOptions spark = ModCompat.ANTARCHY ? HeroFx.particle("antarchy:firefly", ParticleTypes.ELECTRIC_SPARK) : ParticleTypes.ELECTRIC_SPARK;
            level.sendParticles(ParticleTypes.FIREWORK, left.x, left.y, left.z, 1, 0.05D, 0.05D, 0.05D, 0.01D);
            level.sendParticles(ParticleTypes.FIREWORK, right.x, right.y, right.z, 1, 0.05D, 0.05D, 0.05D, 0.01D);
            level.sendParticles(spark, hand.x, hand.y, hand.z, 1, 0.3D, 0.2D, 0.3D, 0.0D);
        }
        if (player.tickCount % 5 == 0) {
            BlockPos pos = BlockPos.containing(player.getEyePosition());
            if (level.getBlockState(pos).isAir() || level.getBlockState(pos).is(Blocks.LIGHT)) {
                TempBlocks.place(level, player, pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, MarvelBalanceConfig.LIGHT_SHOW_LEVEL.get()), 12);
            }
        }
    }

    public static void tickServer(net.minecraft.server.MinecraftServer server) {
        if (!HOMING.isEmpty()) {
            Iterator<Map.Entry<UUID, Homing>> iterator = HOMING.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Homing> entry = iterator.next();
                ServerLevel level = server.getLevel(entry.getValue().dimension());
                Entity rocket = level == null ? null : level.getEntity(entry.getKey());
                if (!(rocket instanceof FireworkRocketEntity firework) || !firework.isAlive()) {
                    iterator.remove();
                    continue;
                }
                if (!(level.getEntity(entry.getValue().target()) instanceof LivingEntity target) || !target.isAlive()) {
                    continue;
                }
                Vec3 velocity = firework.getDeltaMovement();
                double speed = Math.min(1.6D, Math.max(0.9D, velocity.length() + 0.08D));
                Vec3 desired = target.getBoundingBox().getCenter().subtract(firework.position()).normalize().scale(speed);
                Vec3 steered = velocity.add(desired.subtract(velocity).scale(firework.tickCount < 6 ? 0.15D : 0.45D));
                firework.setDeltaMovement(steered.normalize().scale(speed));
                firework.hurtMarked = true;
            }
        }
        if (!FINALES.isEmpty()) {
            for (Map.Entry<UUID, Finale> entry : new ArrayList<>(FINALES.entrySet())) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player == null || player.serverLevel().dimension() != entry.getValue().dimension) {
                    FINALES.remove(entry.getKey());
                    continue;
                }
                tickFinale(player, entry.getValue());
            }
        }
    }

    public static void onAnyDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypes.FIREWORKS) || !(event.getSource().getDirectEntity() instanceof FireworkRocketEntity rocket)
                || !rocket.getTags().contains(ROCKET_TAG)) {
            return;
        }
        if (!(rocket.getOwner() instanceof ServerPlayer owner) || !affects(owner, event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        float stored = rocket.getPersistentData().getFloat(DAMAGE_KEY);
        if (stored <= 0.0F) {
            event.setCanceled(true);
            return;
        }
        Fireworks fireworks = rocket.getItem().get(DataComponents.FIREWORKS);
        int explosions = fireworks == null ? 1 : Math.max(1, fireworks.explosions().size());
        float vanilla = 5.0F + explosions * 2.0F;
        float ratio = Math.max(0.25F, Math.min(1.0F, event.getAmount() / vanilla));
        event.setAmount(stored * ratio);
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof FireworkRocketEntity rocket) || rocket.getTags().contains(ROCKET_TAG)) {
            return;
        }
        FireworkRocketAccessor accessor = (FireworkRocketAccessor) rocket;
        if (accessor.integratedSuperheroes$getAttachedToEntity() instanceof Player player
                && player.hasData(com.stxllarplanes.marvellegacy.init.AttachmentsRegistry.PLAYER_DATA.get())
                && ID.equals(HeroPowers.data(player).getPrimaryPower())) {
            accessor.integratedSuperheroes$setLifetime((int) (accessor.integratedSuperheroes$getLifetime() * MarvelBalanceConfig.ELYTRA_BOOST_MULTIPLIER.get()));
        }
    }
}
