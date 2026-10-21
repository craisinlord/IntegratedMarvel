package com.craisinlord.integrated_superheroes.villain;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.tintankgames.marvel.sounds.MarvelSoundEvents;
import net.tintankgames.marvel.world.entity.projectile.TesseractCharge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RedSkullBrain extends VillainBrain {
    private static final Map<UUID, int[]> PORTAL_HITS = new HashMap<>();
    private final List<Move> commander;
    private final List<Move> cube;
    private final List<Move> overload;
    private final List<Vec3> pillarSpots = new ArrayList<>();
    private boolean blinkPending;
    private long blinkReady;
    private long channelUntil;
    private int channelHits;
    private boolean saluted;
    private int portalsOpen;

    public RedSkullBrain(PathfinderMob mob) {
        super(mob, VillainConfig.RED_SKULL);
        Move haymaker = big("haymaker", 100, 16, 14, t -> mob.distanceTo(t) < 3.5D, NONE, this::glint, this::fireHaymaker);
        Move volley = big("volley", 120, 20, 10, t -> mob.hasLineOfSight(t) && mob.distanceTo(t) > 3.0D, NONE, this::chargeTelegraph, this::fireVolley);
        Move blink = priority("blink", 0, 10, 0, t -> blinkPending, this::beginBlink, QUIET, this::fireBlink);
        Move salute = priority("salute", 1000000, 10, 10, t -> !saluted && mob.getHealth() < mob.getMaxHealth() * 0.85F, NONE, QUIET, this::fireSalute);
        Move swap = big("swap", 240, 20, 0, t -> mob.hasLineOfSight(t) && mob.distanceTo(t) > 5.0D, this::beginSwap, this::swapTelegraph, this::fireSwap);
        Move reach = priority("swap", 120, 20, 0, t -> unreachable() && mob.hasLineOfSight(t), this::beginSwap, this::swapTelegraph, this::fireSwap);
        Move strike = small("strike", 0, 4, 14, t -> false, NONE, this::glint, this::fireHaymaker);
        Move portals = big("portals", 500, 30, 10, t -> portalsOpen == 0, this::beginPortals, this::portalTelegraph, this::firePortals);
        Move channel = big("channel", 360, 0, 0, t -> mob.getHealth() < mob.getMaxHealth() * 0.6F, NONE, QUIET, this::startChannel);
        Move pillars = big("pillars", 80, 24, 6, t -> true, this::beginPillars, this::pillarTelegraph, this::firePillars);
        Move dust = big("dust", 280, 20, 8, t -> mob.distanceTo(t) < 18.0D, this::lock, this::dustTelegraph, this::fireDust);
        Move fastHaymaker = big("haymaker", 80, 16, 14, haymaker.ready(), NONE, this::glint, this::fireHaymaker);
        Move fastVolley = big("volley", 96, 20, 10, volley.ready(), NONE, this::chargeTelegraph, this::fireVolley);
        Move fastSwap = big("swap", 190, 20, 0, swap.ready(), this::beginSwap, this::swapTelegraph, this::fireSwap);
        this.commander = List.of(blink, salute, haymaker, volley);
        this.cube = List.of(blink, reach, strike, haymaker, volley, swap, portals, channel);
        this.overload = List.of(blink, reach, strike, fastHaymaker, fastVolley, fastSwap, pillars, dust);        this.saluted = mob.getPersistentData().getBoolean("integrated_superheroes_skull_saluted");
    }

    @Override
    protected void initEquipment() {
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HeroFx.item(ModCompat.SCORCHED_GUNS ? "scguns:krauser" : "minecraft:iron_sword")));
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        setBase(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 14.0D);
    }

    @Override
    protected float[] thresholds() {
        return new float[]{0.65F, 0.3F};
    }

    @Override
    protected List<Move> moves() {
        return phase == 0 ? commander : phase == 1 ? cube : overload;
    }

    @Override
    protected String key() {
        return "red_skull";
    }

    @Override
    protected ChatFormatting color() {
        return ChatFormatting.RED;
    }

    @Override
    protected int deathTicks() {
        return 60;
    }

    @Override
    protected void onEngage(ServerLevel level, LivingEntity target) {
        title("engage");
        say(Loadout.superSoldier(target) ? "rival" : "engage");
    }

    @Override
    protected LivingEntity pickTarget(ServerLevel level) {
        if (mob.tickCount % 40 == 0) {
            for (ServerPlayer player : VillainFx.near(level, mob.position(), 24.0D)) {
                if (Villains.validTarget(player) && Loadout.spaceStone(player) && mob.getTarget() != player) {
                    mob.setTarget(player);
                    VillainFx.line(player, "integrated_superheroes.villains.red_skull.line.stone", ChatFormatting.RED);
                    break;
                }
            }
        }
        return super.pickTarget(level);
    }

    private void glint(LivingEntity t, int left) {
        if (left % 2 == 0) {
            VillainFx.glint(level(), mob);
        }
        if (current != null && left == current.windup() && t instanceof ServerPlayer player) {
            VillainFx.soundTo(player, SoundEvents.NOTE_BLOCK_BELL.value(), 0.8F, 1.4F);
        }
    }

    private void fireHaymaker(LivingEntity t) {
        mob.swing(InteractionHand.OFF_HAND);
        if (mob.distanceTo(t) > 3.9D || friendly(t)) {
            return;
        }
        hit(t, 30.0F);
        double strength = Loadout.heavy(t) ? 1.2D : 2.5D;
        t.knockback(strength, mob.getX() - t.getX(), mob.getZ() - t.getZ());
        t.hurtMarked = true;
        VillainFx.sound(mob, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.7F);
    }

    private void chargeTelegraph(LivingEntity t, int left) {
        if (left % 2 == 0) {
            Vec3 hand = mob.position().add(0.0D, mob.getBbHeight() * 0.6D, 0.0D).add(mob.getLookAngle().scale(0.6D));
            level().sendParticles(new DustParticleOptions(VillainFx.CYAN, 1.0F), hand.x, hand.y, hand.z, 4, 0.1D, 0.1D, 0.1D, 0.0D);
        }
        if (current != null && left == current.windup()) {
            VillainFx.sound(mob, SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.6F);
        }
    }

    private void fireVolley(LivingEntity t) {
        ServerLevel level = level();
        Vec3 aim = t.position().add(0.0D, t.getBbHeight() * 0.5D, 0.0D).subtract(mob.getEyePosition());
        for (float yaw : new float[]{-8.0F, 0.0F, 8.0F}) {
            TesseractCharge charge = new TesseractCharge(level, mob);
            Vec3 direction = aim.yRot((float) Math.toRadians(yaw));
            charge.shoot(direction.x, direction.y, direction.z, 1.6F, 1.0F);
            level.addFreshEntity(charge);
        }
        VillainFx.sound(mob, MarvelSoundEvents.RED_SKULL_SHOOT.get(), 1.2F, 1.0F);
        mob.swing(InteractionHand.MAIN_HAND);
    }

    private void beginBlink(LivingEntity t) {
        VillainFx.spaceBurst(level(), mob.position().add(0.0D, mob.getBbHeight() * 0.5D, 0.0D), 1.5F, mob.getYHeadRot(), false);
    }

    private void fireBlink(LivingEntity t) {
        blinkPending = false;
        Vec3 spot = groundSpot(mob.position(), 6.0D, 10.0D);
        if (spot == null) {
            return;
        }
        ServerLevel level = level();
        mob.teleportTo(spot.x, spot.y, spot.z);
        VillainFx.spaceBurst(level, spot.add(0.0D, 1.0D, 0.0D), 1.5F, mob.getYHeadRot(), true);
        VillainFx.sound(mob, MarvelSoundEvents.TESSERACT_TELEPORT.get(), 1.0F, 1.0F);
    }

    private void fireSalute(LivingEntity t) {
        saluted = true;
        mob.getPersistentData().putBoolean("integrated_superheroes_skull_saluted", true);
        say("hail");
        HydraSquad.salute(level(), mob, 20.0D);
        if (HydraAlarm.active(level(), mob.position()) != null) {
            HydraSquad.reinforce(level(), mob, 2, t, HydraSquad.Role.TROOPER);
        }
    }

    private void beginSwap(LivingEntity t) {
        lockedSpot = t.position();
    }

    private void swapTelegraph(LivingEntity t, int left) {
        if (left % 4 == 0) {
            ServerLevel level = level();
            level.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY(1.0D), mob.getZ(), 20, 0.4D, 0.8D, 0.4D, 0.4D);
            level.sendParticles(ParticleTypes.PORTAL, t.getX(), t.getY(1.0D), t.getZ(), 20, 0.4D, 0.8D, 0.4D, 0.4D);
            level.sendParticles(new DustParticleOptions(VillainFx.CYAN, 1.2F), t.getX(), t.getY(0.5D), t.getZ(), 6, 0.3D, 0.6D, 0.3D, 0.0D);
        }
        if (current != null && left == current.windup()) {
            VillainFx.sound(mob, MarvelSoundEvents.TESSERACT_TELEPORT.get(), 0.8F, 0.6F);
        }
    }

    private void fireSwap(LivingEntity t) {
        ServerLevel level = level();
        if (Loadout.spaceStone(t)) {
            if (t instanceof ServerPlayer player) {
                VillainFx.line(player, "integrated_superheroes.villains.red_skull.line.swap_fail", ChatFormatting.RED);
            }
            VillainFx.sound(mob, SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.2F);
            return;
        }
        if (!mob.hasLineOfSight(t)) {
            return;
        }
        Vec3 mine = mob.position();
        Vec3 theirs = t.position();
        mob.teleportTo(theirs.x, theirs.y, theirs.z);
        t.teleportTo(mine.x, mine.y, mine.z);
        VillainFx.spaceBurst(level, mine.add(0.0D, 1.0D, 0.0D), 1.2F, 0.0F, true);
        VillainFx.spaceBurst(level, theirs.add(0.0D, 1.0D, 0.0D), 1.2F, 0.0F, true);
        VillainFx.sound(mob, MarvelSoundEvents.TESSERACT_TELEPORT.get(), 1.2F, 1.0F);
        unreachableTicks = 0;
        recoveryLeft = 0;
        if (Villains.validTarget(t) || t.isAlive()) {
            VillainWorld.later(level, 2, () -> {
                if (mob.isAlive() && current == null) {
                    force("strike", t);
                }
            });
        }
    }

    private void beginPortals(LivingEntity t) {
        pillarSpots.clear();
        Vec3 center = anchor != null ? anchor : mob.position();
        for (int i = 0; i < 2; i++) {
            Vec3 spot = groundSpot(center, 6.0D, 11.0D);
            if (spot != null) {
                pillarSpots.add(spot);
            }
        }
    }

    private void portalTelegraph(LivingEntity t, int left) {
        if (left % 2 == 0) {
            for (Vec3 spot : pillarSpots) {
                VillainFx.ring(level(), spot, 1.2D, VillainFx.CYAN);
            }
        }
    }

    private void firePortals(LivingEntity t) {
        ServerLevel level = level();
        for (Vec3 spot : pillarSpots) {
            openPortal(level, spot);
        }
        if (!pillarSpots.isEmpty()) {
            say("portals");
        }
        pillarSpots.clear();
    }

    private void openPortal(ServerLevel level, Vec3 spot) {
        Interaction portal = EntityType.INTERACTION.create(level);
        if (portal == null) {
            return;
        }
        portal.moveTo(spot.x, spot.y, spot.z);
        portal.addTag(Villains.PORTAL_TAG);
        portal.setInvulnerable(true);
        CompoundSize.size(portal);
        level.addFreshEntity(portal);
        UUID id = portal.getUUID();
        PORTAL_HITS.put(id, new int[]{0});
        portalsOpen++;
        long start = level.getGameTime();
        long end = start + 300L;
        VillainFx.sound(level, spot, MarvelSoundEvents.TESSERACT_TELEPORT.get(), 1.5F, 0.5F);
        VillainWorld.hazard(level, (lvl, now) -> {
            int[] hits = PORTAL_HITS.get(id);
            boolean closed = hits == null || hits[0] >= 3 || now >= end || !mob.isAlive() || dying;
            if (closed) {
                PORTAL_HITS.remove(id);
                Entity entity = lvl.getEntity(id);
                if (entity != null) {
                    entity.discard();
                }
                portalsOpen = Math.max(0, portalsOpen - 1);
                VillainFx.spaceBurst(lvl, spot.add(0.0D, 1.0D, 0.0D), 1.0F, 0.0F, false);
                VillainFx.sound(lvl, spot, SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.5F);
                return false;
            }
            if (now % 10L == 0L) {
                VillainFx.spaceBurst(lvl, spot.add(0.0D, 1.0D, 0.0D), 0.8F, (now % 360L), true);
                VillainFx.ring(lvl, spot, 1.0D, VillainFx.CYAN);
            }
            if ((now - start) % 120L == 60L) {
                HydraSquad.spawnAt(lvl, spot, target, HydraSquad.Role.TROOPER);
                VillainFx.spaceBurst(lvl, spot.add(0.0D, 1.0D, 0.0D), 1.5F, 0.0F, true);
            }
            return true;
        });
    }

    static void hitPortal(ServerLevel level, Entity portal) {
        int[] hits = PORTAL_HITS.get(portal.getUUID());
        if (hits == null) {
            return;
        }
        hits[0]++;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, portal.getX(), portal.getY() + 1.0D, portal.getZ(), 10, 0.3D, 0.5D, 0.3D, 0.1D);
        VillainFx.sound(level, portal.position(), SoundEvents.AMETHYST_BLOCK_HIT, 1.2F, 0.6F + hits[0] * 0.2F);
    }

    private void startChannel(LivingEntity t) {
        if (blinded()) {
            return;
        }
        channelUntil = mob.level().getGameTime() + 120L;
        channelHits = 0;
        say("channel");
        HydraSquad.guard(level(), mob, t);
    }

    private boolean channeling() {
        return mob.level().getGameTime() < channelUntil;
    }

    @Override
    protected boolean stanceTick(ServerLevel level, long now, LivingEntity t) {
        if (!channeling()) {
            return false;
        }
        if (blinded()) {
            interruptChannel(level);
            return false;
        }
        mob.getNavigation().stop();
        mob.setXRot(-45.0F);
        float heal = 0.2F;
        if (now % 20L == 0L) {
            int siphoned = 0;
            for (ServerPlayer player : VillainFx.near(level, mob.position(), 8.0D)) {
                if (Loadout.suit(player)) {
                    Loadout.drainSuit(player, 0.02D);
                    siphoned++;
                    VillainFx.lineBetween(level, player.position().add(0.0D, 1.0D, 0.0D), mob.position().add(0.0D, 2.0D, 0.0D), VillainFx.CYAN);
                }
            }
            heal += siphoned;
            VillainFx.sound(mob, SoundEvents.BEACON_AMBIENT, 1.2F, 1.4F);
        }
        mob.heal(heal);
        if (now % 3L == 0L) {
            Vec3 top = mob.position().add(0.0D, mob.getBbHeight() + 0.4D, 0.0D);
            level.sendParticles(new DustParticleOptions(VillainFx.CYAN, 1.3F), top.x, top.y, top.z, 6, 0.2D, 0.2D, 0.2D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, top.x, top.y, top.z, 1, 0.1D, 0.4D, 0.1D, 0.02D);
        }
        return true;
    }

    private void interruptChannel(ServerLevel level) {
        channelUntil = 0L;
        say("interrupted");
        stagger(level);
    }

    @Override
    public boolean incoming(LivingIncomingDamageEvent event) {
        if (super.incoming(event)) {
            return true;
        }
        if (finishing || dying) {
            return false;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        boolean ranged = direct != null && direct != attacker;
        if (channeling() && attacker != null) {
            channelHits += ranged && attacker instanceof LivingEntity living && Loadout.suit(living) ? 2 : 1;
            if (channelHits >= 3) {
                interruptChannel(level());
            }
        }
        long now = mob.level().getGameTime();
        if (ranged && !channeling() && now >= blinkReady && !source.is(DamageTypes.MAGIC)) {
            blinkReady = now + 120L;
            blinkPending = true;
        }
        return false;
    }

    private void beginPillars(LivingEntity t) {
        pillarSpots.clear();
        ServerLevel level = level();
        for (ServerPlayer player : VillainFx.near(level, mob.position(), 18.0D)) {
            pillarSpots.add(player.position().add(random.nextGaussian() * 0.8D, 0.0D, random.nextGaussian() * 0.8D));
        }
        int extra = 3 + random.nextInt(3) - pillarSpots.size();
        Vec3 center = anchor != null ? anchor : mob.position();
        for (int i = 0; i < extra; i++) {
            Vec3 spot = groundSpot(center, 2.0D, 12.0D);
            if (spot != null && spot.distanceTo(mob.position()) > 2.0D) {
                pillarSpots.add(spot);
            }
        }
    }

    private void pillarTelegraph(LivingEntity t, int left) {
        if (left % 2 == 0) {
            for (Vec3 spot : pillarSpots) {
                VillainFx.ring(level(), spot, 1.3D, VillainFx.CYAN);
            }
        }
        if (current != null && left == current.windup()) {
            VillainFx.sound(mob, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.8F);
        }
    }

    private void firePillars(LivingEntity t) {
        ServerLevel level = level();
        for (Vec3 spot : pillarSpots) {
            for (int y = 0; y < 8; y++) {
                level.sendParticles(ParticleTypes.END_ROD, spot.x, spot.y + y * 0.6D, spot.z, 3, 0.2D, 0.2D, 0.2D, 0.01D);
            }
            VillainFx.spaceBurst(level, spot.add(0.0D, 1.0D, 0.0D), 0.8F, 0.0F, false);
            for (LivingEntity victim : VillainWorld.victims(level, spot, 1.3D, 5.0D, e -> !friendly(e))) {
                hit(victim, 18.0F);
                victim.setDeltaMovement(victim.getDeltaMovement().x, 1.0D, victim.getDeltaMovement().z);
                victim.hurtMarked = true;
            }
        }
        VillainFx.sound(mob, SoundEvents.BEACON_POWER_SELECT, 1.5F, 0.6F);
        pillarSpots.clear();
    }

    private void lock(LivingEntity t) {
        lockedSpot = t.position();
    }

    private void dustTelegraph(LivingEntity t, int left) {
        if (lockedSpot != null && left % 2 == 0) {
            VillainFx.ring(level(), lockedSpot, 4.0D, VillainFx.RED);
        }
    }

    private void fireDust(LivingEntity t) {
        if (lockedSpot == null) {
            return;
        }
        Vec3 spot = lockedSpot;
        lockedSpot = null;
        long end = mob.level().getGameTime() + 120L;
        VillainFx.sound(level(), spot, SoundEvents.FIRE_EXTINGUISH, 1.2F, 0.5F);
        DustParticleOptions red = new DustParticleOptions(VillainFx.RED, 2.0F);
        VillainWorld.hazard(level(), (level, now) -> {
            if (now >= end) {
                return false;
            }
            if (now % 3L == 0L) {
                level.sendParticles(red, spot.x, spot.y + 1.0D, spot.z, 14, 2.0D, 0.8D, 2.0D, 0.0D);
            }
            if (now % 20L == 0L) {
                for (LivingEntity victim : VillainWorld.victims(level, spot, 4.0D, 3.0D, e -> !Villains.isVillainSide(e))) {
                    victim.hurt(mob.damageSources().indirectMagic(mob, mob), 4.0F);
                    victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                }
            }
            return true;
        });
    }

    @Override
    protected void onTransition(ServerLevel level, int newPhase) {
        channelUntil = 0L;
        if (newPhase == 1) {
            title("cube");
            say("cube");
            for (ServerPlayer player : VillainFx.near(level, mob.position(), 24.0D)) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, false, false));
            }
        } else {
            title("overload");
            say("overload");
            level.sendParticles(ParticleTypes.FLASH, mob.getX(), mob.getY(1.0D), mob.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            for (LivingEntity victim : VillainWorld.victims(level, mob.position(), 10.0D, 4.0D, e -> !friendly(e))) {
                victim.knockback(1.6D, mob.getX() - victim.getX(), mob.getZ() - victim.getZ());
                victim.hurtMarked = true;
            }
        }
    }

    @Override
    protected void transitionTick(ServerLevel level, int left) {
        if (left % 8 == 0) {
            VillainFx.spaceBurst(level, mob.position().add(0.0D, mob.getBbHeight() + 0.3D, 0.0D), phase == 2 ? 2.0F : 1.2F, mob.getYHeadRot(), phase == 1);
        }
    }

    @Override
    protected void onPhaseStart(ServerLevel level, int newPhase) {
        if (newPhase == 1) {
            readyAt.put("portals", level.getGameTime());
        }
    }

    @Override
    protected void onStagger(ServerLevel level) {
        channelUntil = 0L;
        HydraSquad.guard(level, mob, target);
    }

    @Override
    protected void onDeathStart(ServerLevel level) {
        channelUntil = 0L;
        blinkPending = false;
        pillarSpots.clear();
        say("death");
        mob.setNoGravity(true);
        HeroFx.shake(level, mob.position(), 24.0D, 0.8F, 40);
        VillainFx.sound(mob, SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.4F);
    }

    @Override
    protected void deathTick(ServerLevel level, int left) {
        mob.setNoGravity(true);
        if (left > 20) {
            mob.setDeltaMovement(0.0D, 0.06D, 0.0D);
            mob.hurtMarked = true;
        } else {
            mob.setDeltaMovement(Vec3.ZERO);
        }
        if (left % 5 == 0) {
            VillainFx.spaceBurst(level, mob.position().add(0.0D, mob.getBbHeight() * 0.5D, 0.0D), 1.0F + (60 - left) / 20.0F, left * 6.0F, left % 10 == 0);
        }
        if (left == 30) {
            say("vormir");
        }
    }

    @Override
    protected void onFinished(ServerLevel level) {
        Vec3 at = mob.position().add(0.0D, mob.getBbHeight() * 0.5D, 0.0D);
        VillainFx.spaceBurst(level, at, 3.0F, 0.0F, false);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 2, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 80, 0.5D, 0.8D, 0.5D, 0.15D);
        VillainFx.sound(mob, MarvelSoundEvents.TESSERACT_TELEPORT.get(), 2.5F, 0.5F);
        mob.setInvisible(true);
        mob.setNoGravity(false);
    }

    @Override
    public void onKilled(ServerLevel level, DamageSource source) {
        Villains.awardNear(level, mob.position(), "villains/red_skull");
    }

    private static final class CompoundSize {
        static void size(Interaction portal) {
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            portal.saveWithoutId(tag);
            tag.putFloat("width", 1.4F);
            tag.putFloat("height", 2.2F);
            tag.putBoolean("response", true);
            portal.load(tag);
        }
    }
}
