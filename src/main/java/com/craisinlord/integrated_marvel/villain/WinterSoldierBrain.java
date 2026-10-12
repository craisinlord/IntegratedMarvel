package com.craisinlord.integrated_marvel.villain;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.craisinlord.integrated_marvel.compat.ModCompat;
import com.craisinlord.integrated_marvel.hero.HeroFx;
import com.craisinlord.integrated_marvel.hero.HeroStun;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.tintankgames.marvel.sounds.MarvelSoundEvents;
import net.tintankgames.marvel.world.entity.HydraAgent;
import net.tintankgames.marvel.world.entity.projectile.ThrownVibraniumShield;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class WinterSoldierBrain extends VillainBrain {
    private static final ResourceLocation RAGE_SPEED = ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "winter_soldier_comply");
    private static final String KNIFE = "marvel:winter_soldier_knife";
    private static final String SNIPER = ModCompat.SCORCHED_GUNS ? "scguns:iron_spear" : "minecraft:crossbow";
    private static final String LAUNCHER = ModCompat.SCORCHED_GUNS ? "scguns:blooper" : "minecraft:tnt";
    private static final String KEY_ALLY = "integrated_marvel_ws_ally";
    private static final String KEY_ALLY_UNTIL = "integrated_marvel_ws_ally_until";
    static final String KEY_FREED = "integrated_marvel_ws_freed";
    private final List<Move> mission;
    private final List<Move> arm;
    private final List<Move> comply;
    private final List<Move> echoMoves;
    private final Set<UUID> hesitated = new HashSet<>();
    private int bootLeft;
    private int swapLeft;
    private String swapTo;
    private int comboLeft;
    private int comboTimer;
    private int comboIndex;
    private long blockUntil;
    private long blockReady;
    private long coverUntil;
    private long vanishBlockedUntil;
    private boolean vanished;
    private UUID allyOwner;
    private long allyUntil;
    private long lastHydraSeen;
    private int leaving;

    public WinterSoldierBrain(PathfinderMob mob, boolean echo) {
        super(mob, VillainConfig.WINTER_SOLDIER, echo);
        Move combo = big("combo", 100, 12, 0, t -> holding(KNIFE) && mob.distanceTo(t) < 4.5D, this::beginCombo, this::glint, this::fireCombo);
        Move sniper = big("sniper", 120, 24, 10, t -> holding(SNIPER) && mob.hasLineOfSight(t) && (mob.distanceTo(t) >= 6.0D || unreachable()),
                NONE, this::gunTelegraph, this::fireSniper);
        Move launcher = big("launcher", 180, 20, 10, t -> holding(LAUNCHER) && mob.distanceTo(t) < 28.0D, this::lock, this::launcherTelegraph, this::fireLauncher);
        Move grab = big("grab", 200, 20, 16, t -> mob.distanceTo(t) < 5.0D && mob.hasLineOfSight(t), NONE, this::grabTelegraph, this::fireGrab);
        Move reach = priority("grab", 200, 20, 16, t -> unreachable() && mob.distanceTo(t) < 8.0D && mob.hasLineOfSight(t), NONE, this::grabTelegraph, this::fireGrab);
        Move slam = big("slam", 160, 20, 14, t -> mob.distanceTo(t) < 5.0D, NONE, this::slamTelegraph, this::fireSlam);
        Move vanish = big("vanish", 240, 30, 0, t -> !blinded() && mob.level().getGameTime() >= vanishBlockedUntil, this::beginVanish, this::vanishTelegraph, this::fireVanish);
        this.mission = List.of(combo, sniper, launcher);
        this.arm = List.of(reach, combo, sniper, launcher, grab, slam);
        this.comply = List.of(reach, combo, vanish, slam);
        this.echoMoves = List.of(big("combo", 60, 12, 0, t -> mob.distanceTo(t) < 4.5D, this::beginCombo, this::glint, this::fireCombo));
        CompoundTag data = mob.getPersistentData();
        if (data.hasUUID(KEY_ALLY)) {
            this.allyOwner = data.getUUID(KEY_ALLY);
            this.allyUntil = data.getLong(KEY_ALLY_UNTIL);
        }
    }

    @Override
    protected void initEquipment() {
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HeroFx.item("marvel:winter_soldier_knife")));
        mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        setBase(Attributes.ATTACK_DAMAGE, 6.0D);
    }

    @Override
    protected float[] thresholds() {
        return echo ? new float[0] : new float[]{0.6F, 0.25F};
    }

    @Override
    protected List<Move> moves() {
        if (echo) {
            return echoMoves;
        }
        return phase == 0 ? mission : phase == 1 ? arm : comply;
    }

    @Override
    protected String key() {
        return "winter_soldier";
    }

    @Override
    protected ChatFormatting color() {
        return ChatFormatting.WHITE;
    }

    @Override
    protected double engageRange() {
        return 6.0D;
    }

    @Override
    protected double chaseSpeed() {
        return phase >= 2 ? 1.35D : 1.15D;
    }

    public boolean ally() {
        return allyOwner != null;
    }

    @Override
    protected boolean bypassPipeline() {
        return ally();
    }

    public boolean readable() {
        return !ally() && !dying && engaged && (mob.getHealth() <= mob.getMaxHealth() * 0.25F || staggered());
    }

    private boolean holding(String id) {
        return mob.getMainHandItem().is(HeroFx.item(id));
    }

    @Override
    public void tick() {
        if (ally()) {
            allyTick(level());
            return;
        }
        super.tick();
    }

    @Override
    protected void dormantTick(ServerLevel level) {
        if (mob.tickCount % 8 == 0) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, mob.getX(), mob.getY(1.0D), mob.getZ(), 4, 0.4D, 0.8D, 0.4D, 0.0D);
        }
    }

    @Override
    protected void onEngage(ServerLevel level, LivingEntity target) {
        VillainFx.sound(mob, MarvelSoundEvents.WINTER_SOLDIER_TARGET.get(), 2.5F, 1.0F);
        level.sendParticles(ParticleTypes.SNOWFLAKE, mob.getX(), mob.getY(1.0D), mob.getZ(), 60, 0.6D, 1.0D, 0.6D, 0.05D);
        level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY(0.5D), mob.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.02D);
        bootLeft = alarmed ? 0 : 30;
        title("engage");
        say(Loadout.superSoldier(target) ? "recognize" : "engage");
    }

    @Override
    protected boolean stanceTick(ServerLevel level, long now, LivingEntity t) {
        if (bootLeft > 0) {
            bootLeft--;
            if (bootLeft % 5 == 0) {
                level.sendParticles(ParticleTypes.SNOWFLAKE, mob.getX(), mob.getY(1.0D), mob.getZ(), 6, 0.4D, 0.8D, 0.4D, 0.0D);
            }
            return true;
        }
        if (comboLeft > 0) {
            comboTick(level, t);
            return true;
        }
        if (swapLeft > 0) {
            mob.getNavigation().stop();
            if (--swapLeft == 0 && swapTo != null) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HeroFx.item(swapTo)));
                swapTo = null;
            }
            return true;
        }
        String desired = desiredWeapon(t, now);
        if (!holding(desired)) {
            swapTo = desired;
            swapLeft = 20;
            mob.getNavigation().stop();
            VillainFx.sound(mob, SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.0F, 0.8F);
            return true;
        }
        if (now < blockUntil) {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(t, 40.0F, 40.0F);
            if (now % 4L == 0L) {
                VillainFx.sparks(level, mob.position().add(mob.getLookAngle().scale(0.5D)).add(0.0D, 1.2D, 0.0D), 2);
            }
            return true;
        }
        if (!holding(KNIFE) && mob.distanceTo(t) > 6.0D) {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(t, 30.0F, 30.0F);
            return false;
        }
        return false;
    }

    private String desiredWeapon(LivingEntity t, long now) {
        if (phase >= 2 || echo) {
            return KNIFE;
        }
        double distance = mob.distanceTo(t);
        boolean sight = mob.hasLineOfSight(t);
        if (now < coverUntil) {
            return LAUNCHER;
        }
        if (Loadout.phasing(t)) {
            return LAUNCHER;
        }
        if (distance < 6.0D) {
            return KNIFE;
        }
        if (distance > 12.0D && sight) {
            return SNIPER;
        }
        if (!sight) {
            return LAUNCHER;
        }
        return holding(KNIFE) ? KNIFE : holding(SNIPER) ? SNIPER : KNIFE;
    }

    @Override
    protected void basicTick(ServerLevel level, LivingEntity t) {
        if (!holding(KNIFE)) {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(t, 30.0F, 30.0F);
            return;
        }
        super.basicTick(level, t);
    }

    private void glint(LivingEntity t, int left) {
        if (left % 2 == 0) {
            VillainFx.glint(level(), mob);
        }
        if (current != null && left == current.windup() && t instanceof ServerPlayer player) {
            VillainFx.soundTo(player, SoundEvents.NOTE_BLOCK_BELL.value(), 0.8F, 2.0F);
        }
    }

    private void beginCombo(LivingEntity t) {
        if (Loadout.superSoldier(t) && hesitated.add(t.getUUID())) {
            windupLeft += 14;
            say("hesitate");
        }
    }

    private void fireCombo(LivingEntity t) {
        comboLeft = phase >= 2 ? 4 : 3;
        comboIndex = 0;
        comboTimer = 0;
    }

    private void comboTick(ServerLevel level, LivingEntity t) {
        mob.getLookControl().setLookAt(t, 40.0F, 40.0F);
        if (mob.distanceTo(t) > 2.5D) {
            mob.getNavigation().moveTo(t, 1.5D);
        } else {
            mob.getNavigation().stop();
        }
        if (comboTimer-- > 0) {
            return;
        }
        comboTimer = 6;
        boolean last = comboLeft == 1;
        if (last) {
            Vec3 dash = t.position().subtract(mob.position()).multiply(1.0D, 0.0D, 1.0D);
            if (dash.lengthSqr() > 1.0E-4D) {
                dash = dash.normalize().scale(0.9D);
                mob.setDeltaMovement(dash.x, 0.1D, dash.z);
                mob.hurtMarked = true;
            }
        }
        mob.swing(comboIndex % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        VillainFx.sound(mob, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.3F + comboIndex * 0.1F);
        if (mob.distanceTo(t) < 3.3D) {
            hit(t, 10.0F);
        }
        comboIndex++;
        if (--comboLeft == 0) {
            int recovery = phase >= 2 ? 20 : 30;
            recoveryLeft = recovery;
            openUntil = level.getGameTime() + recovery;
            level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY(1.0D), mob.getZ(), 6, 0.3D, 0.3D, 0.3D, 0.01D);
        }
    }

    private void gunTelegraph(LivingEntity t, int left) {
        if (left > 5 || lockedSpot == null) {
            lockedSpot = t.position().add(0.0D, t.getBbHeight() * 0.6D, 0.0D);
        }
        VillainFx.lineBetween(level(), mob.getEyePosition(), lockedSpot, left > 5 ? VillainFx.WHITE : VillainFx.RED);
    }

    private void fireSniper(LivingEntity t) {
        shoot(mob.getMainHandItem(), lockedSpot);
        lockedSpot = null;
    }

    private void lock(LivingEntity t) {
        lockedSpot = t.position();
    }

    private void launcherTelegraph(LivingEntity t, int left) {
        if (lockedSpot != null && left % 2 == 0) {
            VillainFx.ring(level(), lockedSpot, 3.0D, VillainFx.WHITE);
        }
    }

    private void fireLauncher(LivingEntity t) {
        if (lockedSpot != null) {
            if (ModCompat.SCORCHED_GUNS) {
                shoot(mob.getMainHandItem(), lockedSpot.add(0.0D, 0.3D, 0.0D));
            } else {
                VillainArms.grenade(level(), mob, lockedSpot, false, 2.5F);
                mob.swing(InteractionHand.MAIN_HAND);
                VillainFx.sound(mob, SoundEvents.SNOWBALL_THROW, 1.0F, 0.5F);
            }
        }
        lockedSpot = null;
    }

    private void shoot(ItemStack gun, Vec3 at) {
        if (gun.isEmpty() || at == null) {
            return;
        }
        VillainArms.shoot(mob, gun, at, 1.5F);
        mob.swing(InteractionHand.MAIN_HAND);
    }

    private void grabTelegraph(LivingEntity t, int left) {
        if (left % 3 == 0) {
            VillainFx.sparks(level(), mob.position().add(mob.getLookAngle().scale(0.4D)).add(0.0D, 1.3D, 0.0D), 4);
        }
        if (left % 6 == 0) {
            VillainFx.sound(mob, SoundEvents.GRINDSTONE_USE, 0.8F, 0.6F);
        }
    }

    private void fireGrab(LivingEntity t) {
        double reach = unreachable() ? 8.5D : 5.5D;
        if (mob.distanceTo(t) > reach || !mob.hasLineOfSight(t)) {
            return;
        }
        if (Loadout.heavy(t)) {
            say("heavy");
            stagger(level());
            return;
        }
        VillainFx.sound(mob, SoundEvents.IRON_GOLEM_ATTACK, 1.2F, 0.7F);
        Vec3 away = t.position().subtract(mob.position()).multiply(1.0D, 0.0D, 1.0D);
        away = away.lengthSqr() < 1.0E-4D ? mob.getLookAngle() : away.normalize();
        hit(t, 8.0F);
        t.setDeltaMovement(away.x * 2.2D, 1.05D, away.z * 2.2D);
        t.hurtMarked = true;
        unreachableTicks = 0;
    }

    private void slamTelegraph(LivingEntity t, int left) {
        if (left % 2 == 0) {
            VillainFx.ring(level(), mob.position(), 5.0D, VillainFx.WHITE);
        }
    }

    private void fireSlam(LivingEntity t) {
        ServerLevel level = level();
        mob.swing(InteractionHand.OFF_HAND);
        VillainFx.sound(mob, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.3F);
        level.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 0.2D, mob.getZ(), 6, 2.0D, 0.1D, 2.0D, 0.0D);
        level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.2D, mob.getZ(), 30, 2.5D, 0.1D, 2.5D, 0.05D);
        HeroFx.shake(level, mob.position(), 14.0D, 0.6F, 10);
        for (LivingEntity victim : VillainWorld.victims(level, mob.position(), 5.0D, 3.0D, e -> !friendly(e))) {
            hit(victim, 26.0F);
            victim.setDeltaMovement(victim.getDeltaMovement().add(0.0D, 0.8D, 0.0D));
            victim.hurtMarked = true;
        }
    }

    private void beginVanish(LivingEntity t) {
        ServerLevel level = level();
        VillainFx.smoke(level, mob.position(), 3.0D);
        VillainFx.sound(mob, SoundEvents.FIRE_EXTINGUISH, 1.2F, 0.8F);
        boolean radar = false;
        for (ServerPlayer player : VillainFx.near(level, mob.position(), 32.0D)) {
            if (Loadout.radar(player)) {
                radar = true;
            }
        }
        vanished = !radar;
        mob.setInvisible(vanished);
        if (radar) {
            mob.setGlowingTag(true);
        }
    }

    private void vanishTelegraph(LivingEntity t, int left) {
        if (left % 6 == 0) {
            double angle = (30 - left) * 0.35D;
            Vec3 at = t.position().add(Math.cos(angle) * 3.0D, 0.0D, Math.sin(angle) * 3.0D);
            VillainFx.sound(level(), at, SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.6F, 0.7F);
        }
    }

    private void fireVanish(LivingEntity t) {
        ServerLevel level = level();
        Vec3 behind = t.position().subtract(t.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize().scale(2.5D));
        if (!mob.randomTeleport(behind.x, behind.y, behind.z, false)) {
            mob.randomTeleport(t.getX(), t.getY(), t.getZ(), false);
        }
        mob.setInvisible(false);
        mob.setGlowingTag(false);
        vanished = false;
        VillainFx.smoke(level, mob.position(), 1.5D);
        mob.getLookControl().setLookAt(t);
        readyAt.put("combo", 0L);
        force("combo", t);
    }

    @Override
    public boolean incoming(LivingIncomingDamageEvent event) {
        if (super.incoming(event)) {
            return true;
        }
        if (echo) {
            return false;
        }
        ServerLevel level = level();
        long now = level.getGameTime();
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (blinded()) {
            vanishBlockedUntil = now + 100L;
        }
        if (current != null && "grab".equals(current.id()) && attacker instanceof LivingEntity living && Loadout.magneto(living)) {
            say("magneto");
            stagger(level);
            return false;
        }
        boolean ranged = direct != null && direct != attacker;
        if (ranged && !staggered()) {
            if (now < blockUntil && inFront(direct, 65.0D)) {
                VillainFx.sound(mob, SoundEvents.ANVIL_LAND, 0.6F, 1.8F);
                VillainFx.sparks(level, direct.position(), 10);
                return true;
            }
            if (now >= blockReady && attacker != null && mob.distanceTo(attacker) > 6.0D && comboLeft == 0) {
                blockUntil = now + 40L;
                blockReady = now + 140L;
            }
        }
        return false;
    }

    boolean catchShield(ServerLevel level, ThrownVibraniumShield shield) {
        if (echo || ally() || dying || staggered() || !inFront(shield, 75.0D)) {
            return false;
        }
        Entity owner = shield.getOwner();
        ItemStack stack = shield.getPickupItemStackOrigin().copy();
        if (stack.isEmpty()) {
            stack = shield.getWeaponItem().copy();
        }
        shield.discard();
        VillainFx.sound(mob, MarvelSoundEvents.VIBRANIUM_SHIELD_CATCH.get(), 1.5F, 0.9F);
        say("catch");
        mob.setItemSlot(EquipmentSlot.OFFHAND, stack.copy());
        mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        ItemStack thrown = stack;
        UUID ownerId = owner == null ? null : owner.getUUID();
        VillainWorld.later(level, 20, () -> {
            mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            Entity aim = owner != null && owner.isAlive() ? owner : target;
            ThrownVibraniumShield back = new ThrownVibraniumShield(level, mob, thrown.copy());
            back.setFromWinterSoldier(true);
            back.setBaseDamage(8.0D);
            if (aim != null) {
                double dx = aim.getX() - mob.getX();
                double dy = aim.getY(0.5D) - back.getY();
                double dz = aim.getZ() - mob.getZ();
                back.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.1D, dz, 1.6F, 2.0F);
            }
            level.addFreshEntity(back);
            VillainFx.sound(mob, MarvelSoundEvents.VIBRANIUM_SHIELD_THROW.get(), 1.2F, 1.0F);
            if (ownerId != null) {
                VillainWorld.trackShield(level, back, ownerId, thrown.copy());
            } else {
                back.spawnAtLocation(thrown.copy());
            }
        });
        return true;
    }

    @Override
    protected void onTransition(ServerLevel level, int newPhase) {
        comboLeft = 0;
        swapLeft = 0;
        mob.setInvisible(false);
        VillainFx.sparks(level, mob.position().add(0.0D, 1.3D, 0.0D), 40);
        VillainFx.sound(mob, SoundEvents.GRINDSTONE_USE, 1.5F, 0.5F);
        title(newPhase == 1 ? "arm" : "comply");
        say(newPhase == 1 ? "arm" : "comply");
    }

    @Override
    protected void onPhaseStart(ServerLevel level, int newPhase) {
        long now = level.getGameTime();
        if (newPhase == 1) {
            coverUntil = now + 200L;
            HydraSquad.reinforce(level, mob, 2, target, HydraSquad.Role.SNIPER);
        }
        if (newPhase == 2) {
            AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && !speed.hasModifier(RAGE_SPEED)) {
                speed.addPermanentModifier(new AttributeModifier(RAGE_SPEED, 0.25D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HeroFx.item(KNIFE)));
            for (ServerPlayer player : VillainFx.near(level, mob.position(), 32.0D)) {
                if (RedBook.holds(player)) {
                    VillainFx.line(player, "integrated_marvel.villains.red_book.window", ChatFormatting.DARK_RED);
                }
            }
        }
    }

    @Override
    protected void onStagger(ServerLevel level) {
        comboLeft = 0;
        blockUntil = 0L;
        if (vanished) {
            vanished = false;
            mob.setInvisible(false);
        }
        mob.setGlowingTag(false);
    }

    @Override
    protected void onDeathStart(ServerLevel level) {
        say("death");
        HeroStun.stun(mob, 40);
        mob.setInvisible(false);
    }

    @Override
    protected void deathTick(ServerLevel level, int left) {
        if (left % 4 == 0) {
            VillainFx.sparks(level, mob.position().add(0.0D, 1.2D, 0.0D), 6);
        }
        if (left == 10) {
            VillainFx.sound(mob, SoundEvents.BEACON_DEACTIVATE, 1.0F, 0.7F);
        }
    }

    @Override
    public void onKilled(ServerLevel level, DamageSource source) {
        Villains.awardNear(level, mob.position(), "villains/winter_soldier");
    }

    public void becomeAlly(ServerPlayer reader) {
        ServerLevel level = level();
        allyOwner = reader.getUUID();
        allyUntil = level.getGameTime() + VillainConfig.RED_BOOK_ALLY_SECONDS.get() * 20L;
        lastHydraSeen = level.getGameTime();
        mob.getPersistentData().putUUID(KEY_ALLY, allyOwner);
        mob.getPersistentData().putLong(KEY_ALLY_UNTIL, allyUntil);
        current = null;
        comboLeft = 0;
        swapLeft = 0;
        mob.setInvisible(false);
        mob.setTarget(null);
        mob.setHealth(Math.max(mob.getHealth(), mob.getMaxHealth() * 0.5F));
        setMarvelActivated(false);
        VillainFx.bossBarVisible(mob, false);
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HeroFx.item(KNIFE)));
        VillainFx.title(level, mob.position(), 32.0D,
                Component.translatable("integrated_marvel.villains.winter_soldier.name").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                Component.translatable("integrated_marvel.villains.winter_soldier.subtitle.ally").withStyle(ChatFormatting.DARK_RED));
        say("ready");
        Villains.award(reader, "villains/winter_soldier");
        Villains.award(reader, "villains/winter_soldier_freed");
    }

    public boolean allowedAllyTarget(LivingEntity next) {
        return !(next instanceof Player) && !(next instanceof AbstractVillager) && next != mob
                && (next instanceof Enemy || Villains.isVillainSide(next));
    }

    private void allyTick(ServerLevel level) {
        long now = level.getGameTime();
        setMarvelActivated(false);
        VillainFx.bossBarVisible(mob, false);
        if (leaving > 0) {
            if (--leaving == 0) {
                VillainFx.smoke(level, mob.position(), 2.0D);
                mob.discard();
            }
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(allyOwner);
        LivingEntity enemy = mob.getTarget();
        if (enemy == null || !enemy.isAlive() || !allowedAllyTarget(enemy) || enemy.distanceTo(mob) > 24.0D) {
            enemy = findEnemy(level, owner);
            mob.setTarget(enemy);
        }
        if (now % 20L == 0L && !level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(48.0D),
                e -> e.isAlive() && Villains.isVillainSide(e)).isEmpty()) {
            lastHydraSeen = now;
        }
        if (now >= allyUntil || now - lastHydraSeen > 400L) {
            breakFree(level, owner);
            return;
        }
        if (enemy != null) {
            mob.getLookControl().setLookAt(enemy, 30.0F, 30.0F);
            double distance = mob.distanceTo(enemy);
            if (distance > 10.0D && mob.hasLineOfSight(enemy) && now % 50L == 0L) {
                ItemStack sniper = new ItemStack(HeroFx.item(SNIPER));
                shoot(sniper, enemy.position().add(0.0D, enemy.getBbHeight() * 0.6D, 0.0D));
            }
            if (--pathTimer <= 0) {
                pathTimer = 8;
                mob.getNavigation().moveTo(enemy, 1.25D);
            }
            if (attackCooldown > 0) {
                attackCooldown--;
            } else if (distance < 3.0D) {
                attackCooldown = 12;
                mob.swing(InteractionHand.MAIN_HAND);
                enemy.hurt(mob.damageSources().mobAttack(mob), 12.0F);
            }
        } else if (owner != null && owner.level() == level && mob.distanceTo(owner) > 5.0D && --pathTimer <= 0) {
            pathTimer = 10;
            mob.getNavigation().moveTo(owner, 1.2D);
        }
    }

    private LivingEntity findEnemy(ServerLevel level, ServerPlayer owner) {
        Vec3 center = owner != null && owner.level() == level ? owner.position() : mob.position();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(20.0D), this::allowedAllyTarget)) {
            if (!e.isAlive()) {
                continue;
            }
            double score = e.position().distanceTo(center) - (e instanceof HydraAgent || Villains.isVillainSide(e) ? 10.0D : 0.0D);
            if (owner != null && owner.getLastHurtByMob() == e) {
                score -= 20.0D;
            }
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private void breakFree(ServerLevel level, ServerPlayer owner) {
        say("free");
        mob.getNavigation().stop();
        Vec3 at = mob.position();
        for (String id : List.of("marvel:winter_soldier_chestplate", "marvel:winter_soldier_leggings", "marvel:winter_soldier_boots",
                "marvel:winter_soldier_knife", "integrated_marvel:syringe_super_soldier_steve_rogers")) {
            mob.spawnAtLocation(new ItemStack(HeroFx.item(id)));
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 1.0D, at.z, 30, 0.5D, 0.8D, 0.5D, 0.02D);
        if (owner != null) {
            Vec3 away = at.subtract(owner.position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() > 1.0E-4D) {
                away = away.normalize().scale(10.0D);
                mob.getNavigation().moveTo(at.x + away.x, at.y, at.z + away.z, 0.8D);
            }
        }
        leaving = 60;
        mob.getPersistentData().putBoolean(KEY_FREED, true);
    }
}
