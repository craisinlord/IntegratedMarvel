package com.craisinlord.integrated_superheroes.hero;

import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class NightcrawlerPower {
    public static final String ID = "teleportation_kurt_wagner";
    public static final String BAMF = "nightcrawler_bamf";
    public static final String BALLET = "nightcrawler_bamf_ballet";
    public static final HeroPower.Toggle WALL_PERCH = new HeroPower.Toggle("integrated_superheroes_off_wall_perch", "Wall-Perch", () -> Items.LADDER);
    public static final HeroPower.Toggle THICK_SMOKE = new HeroPower.Toggle("integrated_superheroes_off_thick_smoke", "Thick Smoke", () -> Items.CAMPFIRE);
    private static final String DATA = "integrated_superheroes_nightcrawler";
    private static final DustParticleOptions SMOKE_DUST = new DustParticleOptions(new Vector3f(0.18F, 0.1F, 0.28F), 1.6F);
    private static final Map<UUID, Ballet> BALLETS = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> MESSAGED = ServerState.map(new HashMap<>());
    private static final HeroTeleportSafety.Policy BAMF_POLICY = new HeroTeleportSafety.Policy(12.0D, 1, 2, true, true, 1.25D, 4);
    private static final HeroTeleportSafety.Policy STRIKE_POLICY = new HeroTeleportSafety.Policy(12.0D, 1, 1, true, true, 1.25D, 0);

    private static final long GAP_TICKS = 4L;
    private static boolean registered;

    private NightcrawlerPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            HeroCaster.onTick(caster -> {
                if (BALLETS.containsKey(caster.getUUID())) {
                    tickBallet(caster, now(caster));
                }
            });
            HeroCaster.onCleanup(caster -> {
                BALLETS.remove(caster.getUUID());
                MESSAGED.remove(caster.getUUID());
                HeroTeleportSafety.forget(caster);
            });
        }
        return new HeroPower(
                ID,
                "Teleportation (Nightcrawler)",
                syringe,
                NightcrawlerConfig.STATS,
                List.of("agility"),
                List.of(
                        HeroPower.Ability.of(BAMF, "Bamf", NightcrawlerPower::bamf),
                        HeroPower.Ability.of(BALLET, "Bamf Ballet", NightcrawlerPower::ballet),
                        HeroToggles.WHEEL
                ),
                List.of(WALL_PERCH, HeroToggles.SUPER_LEAP, THICK_SMOKE),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        long now = now(player);
                        Charges charges = Charges.load(player);
                        if (charges.recharge(now)) {
                            charges.save(player);
                        }
                        tickBallet(player, now);
                        if (player.tickCount % 5 == 0) {
                            syncHud(player, charges, now);
                        }
                        if (player.tickCount % 20 == 0) {
                            Map<Holder<MobEffect>, Integer> effects = Map.of(MobEffects.JUMP, NightcrawlerConfig.LEAP_LEVEL.get() - 1);
                            Map<Holder<MobEffect>, HeroPower.Toggle> toggles = Map.of(MobEffects.JUMP, HeroToggles.SUPER_LEAP);
                            HeroToggles.applyEffects(player, effects, toggles);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        clear(player);
                        SerumKit.dropEffect(player, MobEffects.JUMP);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (!event.getSource().is(DamageTypeTags.IS_FALL)) {
                            return;
                        }
                        Charges charges = Charges.load(player);
                        if (charges.graceUntil <= 0L || now(player) > charges.graceUntil) {
                            return;
                        }
                        charges.graceUntil = 0L;
                        charges.save(player);
                        float left = event.getAmount() - NightcrawlerConfig.LANDING_GRACE_DAMAGE.get().floatValue();
                        if (left <= 0.0F) {
                            event.setCanceled(true);
                        } else {
                            event.setAmount(left);
                        }
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        BALLETS.remove(player.getUUID());
                    }

                    @Override
                    public void jump(Player player) {
                        if (player.isCrouching() && HeroToggles.isOn(player, HeroToggles.SUPER_LEAP)) {
                            HeroAnimations.playLeapFlip(player);
                            Vec3 motion = player.getDeltaMovement();
                            double horizontal = player.isSprinting() ? 2.0D : 1.0D;
                            player.setDeltaMovement(motion.x * horizontal, motion.y * 1.6D, motion.z * horizontal);
                            player.hurtMarked = true;
                        }
                    }
                }
        );
    }

    public static void registerEvents() {
        NeoForge.EVENT_BUS.addListener(NightcrawlerPower::onLogout);
        NeoForge.EVENT_BUS.addListener(NightcrawlerPower::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(NightcrawlerPower::onRespawn);
        NeoForge.EVENT_BUS.addListener(NightcrawlerPower::onServerStopping);
        RoguePower.adaptBorrowed(BAMF, NightcrawlerPower::rogueBamf);
        RoguePower.excludeBorrowed(BALLET);
        SurfaceCling.register(player -> is(player) && HeroToggles.isOn(player, WALL_PERCH),
                new SurfaceCling.Style(() -> NightcrawlerConfig.PERCH_SHUFFLE.get(), 0.0D, null, null, true));
    }

    public static boolean is(Player player) {
        return SerumKit.is(player, ID);
    }

    public static void clear(ServerPlayer player) {
        BALLETS.remove(player.getUUID());
        MESSAGED.remove(player.getUUID());
        HeroTeleportSafety.forget(player);
        SurfaceCling.clear(player);
        Charges charges = Charges.load(player);
        if (charges.graceUntil != 0L) {
            charges.graceUntil = 0L;
            charges.save(player);
        }
    }

    private static void clearIfOurs(Player entity) {
        if (entity instanceof ServerPlayer player && (is(player) || BALLETS.containsKey(player.getUUID()))) {
            clear(player);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearIfOurs(event.getEntity());
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearIfOurs(event.getEntity());
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        clearIfOurs(event.getEntity());
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        BALLETS.clear();
        MESSAGED.clear();
        HeroTeleportSafety.clearAll();
    }

    private static long now(LivingEntity player) {
        return player.getServer().overworld().getGameTime();
    }

    private static net.minecraft.world.damagesource.DamageSource attack(LivingEntity caster) {
        return caster instanceof Player player ? caster.damageSources().playerAttack(player) : caster.damageSources().mobAttack(caster);
    }

    private static int ticks(double seconds) {
        return (int) Math.round(seconds * 20.0D);
    }

    private static boolean blockedHere(LivingEntity player) {
        String dimension = player.level().dimension().location().toString();
        return NightcrawlerConfig.BLOCKED_DIMENSIONS.get().contains(dimension);
    }

    private static void tell(LivingEntity player, String text) {
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        long now = now(player);
        Long last = MESSAGED.get(player.getUUID());
        if (last != null && now - last < 10L) {
            return;
        }
        MESSAGED.put(player.getUUID(), now);
        HeroCaster.message(player, Component.literal("§7" + text));
    }

    private static HeroTeleportSafety.Policy bamfPolicy(double range) {
        return BAMF_POLICY.withRange(range);
    }

    private static double bamf(LivingEntity player, int slot) {
        long now = now(player);
        boolean mob = !(player instanceof ServerPlayer);
        if (BALLETS.containsKey(player.getUUID())) {
            return 0.0D;
        }
        Charges charges = Charges.load(player);
        charges.recharge(now);
        if (charges.charges <= 0 || now < charges.gapUntil) {
            syncHud(player, charges, now);
            return 0.0D;
        }
        if (blockedHere(player)) {
            tell(player, "Something here keeps you from teleporting.");
            return 0.0D;
        }
        double range = NightcrawlerConfig.BAMF_RANGE.get();
        LivingEntity target = HeroCaster.target(player);
        if (mob && target != null) {
            range = Mth.clamp(player.distanceTo(target) - 1.5D, 2.0D, range);
        }
        HeroTeleportSafety.Policy policy = bamfPolicy(range);
        if (!blink(player, policy)) {
            return 0.0D;
        }
        charges.spend(1, now);
        charges.gapUntil = now + GAP_TICKS;
        if (!HeroTeleportSafety.supported(player, player.position())) {
            charges.graceUntil = now + NightcrawlerConfig.LANDING_GRACE_TICKS.get();
        }
        charges.save(player);
        syncHud(player, charges, now);
        return mob ? 1.0D : 0.0D;
    }

    public static double rogueBamf(ServerPlayer player) {
        if (blockedHere(player)) {
            tell(player, "Something here keeps you from teleporting.");
            return 0.5D;
        }
        HeroTeleportSafety.Policy policy = bamfPolicy(NightcrawlerConfig.ROGUE_BAMF_RANGE.get());
        if (!blink(player, policy)) {
            return 0.5D;
        }
        return NightcrawlerConfig.ROGUE_BAMF_COOLDOWN.get();
    }

    private static boolean blink(LivingEntity player, HeroTeleportSafety.Policy policy) {
        if (!HeroTeleportSafety.canStart(player) || HeroTeleportSafety.throttled(player, policy)) {
            return false;
        }
        Vec3 origin = player.position();
        float fall = player.fallDistance;
        var destination = HeroTeleportSafety.aimed(player, policy);
        if (destination.isEmpty()) {
            HeroTeleportSafety.failed(player);
            tell(player, "There's no room to land there.");
            return false;
        }
        Vec3 to = destination.get();
        if (!HeroTeleportSafety.move(player, to, player.getYRot(), player.getXRot(), policy)) {
            HeroTeleportSafety.failed(player);
            tell(player, "You can't get through.");
            return false;
        }
        HeroTeleportSafety.forget(player);
        settleFall(player, origin, fall);
        bamfFx(player, origin, player.position());
        return true;
    }

    private static void settleFall(LivingEntity player, Vec3 origin, float fallBefore) {
        if (HeroTeleportSafety.supported(player, player.position())) {
            player.fallDistance = 0.0F;
        } else {
            player.fallDistance = Math.max(0.0F, fallBefore - (float) Math.max(0.0D, player.getY() - origin.y));
        }
    }

    private static boolean thick(LivingEntity player) {
        return !(player instanceof Player human) || HeroToggles.isOn(human, THICK_SMOKE);
    }

    private static void bamfFx(LivingEntity player, Vec3 from, Vec3 to) {
        ServerLevel level = HeroCaster.level(player);
        boolean thick = thick(player);
        puff(level, player, from, thick);
        puff(level, player, to, thick);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 1.35F);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 1.15F);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.PLAYERS, 0.35F, 1.6F);
    }

    private static void puff(ServerLevel level, LivingEntity player, Vec3 feet, boolean thick) {
        double width = player.getBbWidth() * 0.4D;
        double height = player.getBbHeight() * 0.35D;
        double y = feet.y + player.getBbHeight() * 0.5D;
        ParticleOptions smoke = HeroFx.particle("thematic:thick_smoke", ParticleTypes.LARGE_SMOKE);
        level.sendParticles(smoke, feet.x, y, feet.z, thick ? 14 : 5, width, height, width, 0.02D);
        level.sendParticles(SMOKE_DUST, feet.x, y, feet.z, thick ? 18 : 6, width, height, width, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, feet.x, y, feet.z, thick ? 10 : 3, width, height, width, 0.03D);
    }

    private static double ballet(LivingEntity player, int slot) {
        long now = now(player);
        if (BALLETS.containsKey(player.getUUID())) {
            return 0.0D;
        }
        Charges charges = Charges.load(player);
        charges.recharge(now);
        int max = NightcrawlerConfig.BAMF_CHARGES.get();
        if (charges.charges < max) {
            tell(player, "You need every charge for that.");
            syncHud(player, charges, now);
            return 0.0D;
        }
        if (now < charges.balletUntil) {
            syncHud(player, charges, now);
            return 0.0D;
        }
        if (blockedHere(player)) {
            tell(player, "Something here keeps you from teleporting.");
            return 0.0D;
        }
        if (!HeroTeleportSafety.canStart(player)) {
            return 0.0D;
        }
        double range = NightcrawlerConfig.BALLET_RANGE.get();
        List<LivingEntity> targets = new ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                entity -> hostile(player, entity) && entity.distanceTo(player) <= range && player.hasLineOfSight(entity)));
        targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
        if (targets.size() > NightcrawlerConfig.BALLET_TARGETS.get()) {
            targets = new ArrayList<>(targets.subList(0, NightcrawlerConfig.BALLET_TARGETS.get()));
        }
        if (targets.isEmpty()) {
            tell(player, "Nobody close enough to hit.");
            return 0.0D;
        }
        Vec3 origin = player.position();
        Vec3 first = strikeSpot(player, targets.get(0), origin);
        if (first == null) {
            tell(player, "There's no room to land there.");
            return 0.0D;
        }
        double total = MarvelBalanceConfig.POUNCE_DAMAGE.get() * NightcrawlerConfig.BALLET_POUNCE_MULTIPLIER.get();
        List<UUID> queue = new ArrayList<>();
        targets.forEach(target -> queue.add(target.getUUID()));
        Ballet ballet = new Ballet(player.level().dimension(), origin, queue, total / targets.size(), player.fallDistance);
        if (!strike(player, ballet, targets.get(0), first)) {
            tell(player, "You can't get through.");
            return 0.0D;
        }
        charges.spend(max, now);
        charges.balletUntil = now + ticks(NightcrawlerConfig.BALLET_COOLDOWN.get());
        charges.save(player);
        ballet.nextTick = now + NightcrawlerConfig.BALLET_STEP_TICKS.get();
        BALLETS.put(player.getUUID(), ballet);
        syncHud(player, charges, now);
        return player instanceof ServerPlayer ? 0.0D : NightcrawlerConfig.BALLET_COOLDOWN.get();
    }

    private static boolean hostile(LivingEntity player, LivingEntity entity) {
        return TelekineticFormation.affects(player, entity)
                && (entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget() == player || entity instanceof Player
                || !(player instanceof ServerPlayer));
    }

    private static Vec3 strikeSpot(LivingEntity player, LivingEntity target, Vec3 from) {
        Vec3 toward = target.position().subtract(from);
        Vec3 flat = new Vec3(toward.x, 0.0D, toward.z);
        flat = flat.lengthSqr() < 1.0E-4D ? player.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize() : flat.normalize();
        double gap = target.getBbWidth() * 0.5D + player.getBbWidth() * 0.5D + 0.4D;
        Vec3 desired = target.position().subtract(flat.scale(gap));
        HeroTeleportSafety.Policy policy = STRIKE_POLICY.withRange(NightcrawlerConfig.BALLET_RANGE.get() * 2.0D);
        return HeroTeleportSafety.near(player, desired, from, policy).orElse(null);
    }

    private static boolean strike(LivingEntity player, Ballet ballet, LivingEntity target, Vec3 spot) {
        Vec3 from = player.position();
        Vec3 look = target.getEyePosition().subtract(spot.add(0.0D, player.getEyeHeight(), 0.0D));
        float yRot = (float) (Mth.atan2(look.z, look.x) * Mth.RAD_TO_DEG) - 90.0F;
        float xRot = (float) -(Mth.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * Mth.RAD_TO_DEG);
        HeroTeleportSafety.Policy policy = STRIKE_POLICY.withRange(NightcrawlerConfig.BALLET_RANGE.get() * 2.0D);
        if (!HeroTeleportSafety.move(player, spot, yRot, xRot, policy)) {
            return false;
        }
        settleBallet(player, ballet);
        bamfFx(player, from, player.position());
        float damage = (float) ballet.perStrike;
        HeroAbilities.runAsAbilityDamage(() -> HeroHits.force(player, target, attack(player), damage));
        target.knockback(0.4D, player.getX() - target.getX(), player.getZ() - target.getZ());
        ballet.hits++;
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.1F);
        HeroAnimations.play(player, ballet.hits % 2 == 1 ? "kick" : "uppercut");
        return true;
    }

    private static void tickBallet(LivingEntity player, long now) {
        Ballet ballet = BALLETS.get(player.getUUID());
        if (ballet == null || now < ballet.nextTick) {
            return;
        }
        if (!player.level().dimension().equals(ballet.dimension) || !HeroTeleportSafety.canStart(player)
                || player instanceof Player downed && HealingFactor.isDowned(downed)) {
            BALLETS.remove(player.getUUID());
            return;
        }
        ballet.index++;
        while (ballet.index < ballet.targets.size()) {
            Entity found = HeroCaster.level(player).getEntity(ballet.targets.get(ballet.index));
            if (found instanceof LivingEntity target && target.isAlive() && hostile(player, target)
                    && target.position().distanceTo(ballet.origin) <= NightcrawlerConfig.BALLET_RANGE.get() + 2.0D
                    && player.hasLineOfSight(target)) {
                Vec3 spot = strikeSpot(player, target, player.position());
                if (spot == null || !strike(player, ballet, target, spot)) {
                    finish(player, ballet);
                    return;
                }
                ballet.nextTick = now + NightcrawlerConfig.BALLET_STEP_TICKS.get();
                return;
            }
            ballet.index++;
        }
        finish(player, ballet);
    }

    private static void finish(LivingEntity player, Ballet ballet) {
        BALLETS.remove(player.getUUID());
        Vec3 from = player.position();
        HeroTeleportSafety.Policy policy = STRIKE_POLICY.withRange(NightcrawlerConfig.BALLET_RANGE.get() * 2.0D);
        HeroTeleportSafety.near(player, ballet.origin, from, policy).ifPresent(home -> {
            if (HeroTeleportSafety.move(player, home, player.getYRot(), player.getXRot(), policy)) {
                bamfFx(player, from, player.position());
            }
        });
        settleBallet(player, ballet);
        if (ballet.hits >= 3 && player instanceof ServerPlayer serverPlayer) {
            HeroAdvancements.award(serverPlayer, "x_men/nightcrawler/ballet");
        }
    }

    private static void settleBallet(LivingEntity player, Ballet ballet) {
        if (HeroTeleportSafety.supported(player, player.position())) {
            player.fallDistance = 0.0F;
        } else {
            player.fallDistance = Math.max(0.0F, ballet.fallBefore - (float) Math.max(0.0D, player.getY() - ballet.origin.y));
        }
    }

    private static void syncHud(LivingEntity caster, Charges charges, long now) {
        if (!(caster instanceof ServerPlayer player)) {
            return;
        }
        boolean dirty = false;
        int max = NightcrawlerConfig.BAMF_CHARGES.get();
        Power bamf = HeroAbilities.slotPower(player, BAMF);
        if (bamf != null) {
            String label = "Bamf [" + charges.charges + "/" + max + "]";
            if (!label.equals(bamf.getDisplayName())) {
                bamf.setDisplayName(label);
                dirty = true;
            }
            if (charges.charges > 0) {
                if (bamf.getCooldown() > 0.0D) {
                    bamf.setCooldown(0.0D);
                    dirty = true;
                }
            } else {
                dirty |= raise(bamf, (charges.next - now) / 20.0D);
            }
        }
        Power ballet = HeroAbilities.slotPower(player, BALLET);
        if (ballet != null) {
            long full = charges.charges >= max ? 0L : charges.next + (long) (max - charges.charges - 1) * ticks(NightcrawlerConfig.BAMF_RECHARGE_SECONDS.get());
            long until = Math.max(charges.balletUntil, full);
            dirty |= raise(ballet, (until - now) / 20.0D);
        }
        if (dirty) {
            PlayerData.sync(player);
        }
    }

    private static boolean raise(Power power, double seconds) {
        if (seconds > power.getCooldown() + 0.3D) {
            power.setCooldown(seconds);
            return true;
        }
        return false;
    }

    private static final class Ballet {
        private final ResourceKey<Level> dimension;
        private final Vec3 origin;
        private final List<UUID> targets;
        private final double perStrike;
        private final float fallBefore;
        private int index;
        private int hits;
        private long nextTick;

        private Ballet(ResourceKey<Level> dimension, Vec3 origin, List<UUID> targets, double perStrike, float fallBefore) {
            this.dimension = dimension;
            this.origin = origin;
            this.targets = targets;
            this.perStrike = perStrike;
            this.fallBefore = fallBefore;
        }
    }

    private static final class Charges {
        private int charges;
        private long next;
        private long gapUntil;
        private long balletUntil;
        private long graceUntil;

        private static CompoundTag root(LivingEntity player) {
            CompoundTag data = player.getPersistentData();
            if (!data.contains(Player.PERSISTED_NBT_TAG)) {
                data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
            }
            return data.getCompound(Player.PERSISTED_NBT_TAG);
        }

        private static Charges load(LivingEntity player) {
            Charges state = new Charges();
            CompoundTag root = root(player);
            int max = NightcrawlerConfig.BAMF_CHARGES.get();
            if (!root.contains(DATA)) {
                state.charges = max;
                return state;
            }
            CompoundTag tag = root.getCompound(DATA);
            state.charges = Mth.clamp(tag.getInt("charges"), 0, max);
            state.next = tag.getLong("next");
            state.gapUntil = tag.getLong("gap");
            state.balletUntil = tag.getLong("ballet");
            state.graceUntil = tag.getLong("grace");
            return state;
        }

        private void save(LivingEntity player) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("charges", charges);
            tag.putLong("next", next);
            tag.putLong("gap", gapUntil);
            tag.putLong("ballet", balletUntil);
            tag.putLong("grace", graceUntil);
            root(player).put(DATA, tag);
        }

        private boolean recharge(long now) {
            int max = NightcrawlerConfig.BAMF_CHARGES.get();
            int step = Math.max(1, ticks(NightcrawlerConfig.BAMF_RECHARGE_SECONDS.get()));
            boolean changed = false;
            if (charges < max && next <= 0L) {
                next = now + step;
                changed = true;
            }
            if (next > now + (long) step * max) {
                next = now + step;
                changed = true;
            }
            while (charges < max && next > 0L && now >= next) {
                charges++;
                next = charges < max ? next + step : 0L;
                changed = true;
            }
            if (charges >= max && next != 0L) {
                next = 0L;
                changed = true;
            }
            long gapLimit = now + GAP_TICKS;
            if (gapUntil > gapLimit) {
                gapUntil = gapLimit;
                changed = true;
            }
            long balletLimit = now + ticks(NightcrawlerConfig.BALLET_COOLDOWN.get());
            if (balletUntil > balletLimit) {
                balletUntil = balletLimit;
                changed = true;
            }
            if (graceUntil > now + NightcrawlerConfig.LANDING_GRACE_TICKS.get()) {
                graceUntil = 0L;
                changed = true;
            }
            return changed;
        }

        private void spend(int amount, long now) {
            charges = Math.max(0, charges - amount);
            if (next <= 0L) {
                next = now + Math.max(1, ticks(NightcrawlerConfig.BAMF_RECHARGE_SECONDS.get()));
            }
        }
    }
}
