package com.craisinlord.integrated_superheroes.villain;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.hero.HeroStun;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ZemoBrain extends VillainBrain {
    private static final String RED_BOOK_GIVEN = "integrated_superheroes_red_book_received";
    private final List<Move> duel;
    private final List<Move> dirty;
    private final List<Move> secondWind;
    private final List<Move> echoMoves;
    private final Set<UUID> lungeHits = new HashSet<>();
    private long guardUntil;
    private int lungeTicks;
    private Vec3 lungeDirection = Vec3.ZERO;
    private int lungeCount;
    private boolean feint;
    private boolean whistled;
    private boolean honest;
    private boolean pendingChain;
    private boolean followUp;

    public ZemoBrain(PathfinderMob mob, boolean echo) {
        super(mob, VillainConfig.ZEMO, echo);
        Move guard = big("guard", 180, 0, 0, t -> true, NONE, QUIET, t -> startGuard());
        Move lunge = big("lunge", 120, 16, 24, t -> mob.distanceTo(t) < 9.0D && mob.hasLineOfSight(t), this::beginLunge, this::glintTelegraph, this::fireLunge);
        Move dart = big("dart", 240, 16, 6, t -> Loadout.kind(t) == Loadout.Kind.SERUM && mob.hasLineOfSight(t) && mob.distanceTo(t) > 3.0D,
                NONE, this::glintTelegraph, this::fireDart);
        Move emp = big("emp", 300, 30, 6, t -> Loadout.kind(t) == Loadout.Kind.SUIT, this::beginArea, this::empTelegraph, this::fireEmp);
        Move whistle = priority("whistle", 1000000, 10, 10, t -> !whistled && mob.getHealth() < mob.getMaxHealth() * 0.75F,
                NONE, QUIET, this::fireWhistle);
        Move grapple = priority("grapple", 100, 16, 4, t -> unreachable() && mob.hasLineOfSight(t), NONE, this::glintTelegraph, this::fireGrapple);
        Move pistol = big("pistol", 80, 12, 8, t -> mob.hasLineOfSight(t) && mob.distanceTo(t) > 3.5D, NONE, this::gunTelegraph, this::firePistol);
        Move adhesive = big("adhesive", 200, 20, 6, t -> mob.distanceTo(t) < 16.0D, this::beginAdhesive, this::adhesiveTelegraph, this::fireAdhesive);
        Move fastGuard = big("guard", 100, 0, 0, t -> true, NONE, QUIET, t -> startGuard());
        Move fastLunge = big("lunge", 80, 14, 20, t -> mob.distanceTo(t) < 9.0D && mob.hasLineOfSight(t), this::beginLunge, this::glintTelegraph, this::fireLunge);
        Move slowGuard = big("guard", 220, 0, 0, t -> true, NONE, QUIET, t -> startGuard());
        Move slowLunge = big("lunge", 160, 16, 24, t -> mob.distanceTo(t) < 9.0D && mob.hasLineOfSight(t), this::beginLunge, this::glintTelegraph, this::fireLunge);
        Move fastDart = big("dart", 180, 16, 6, dart.ready(), NONE, this::glintTelegraph, this::fireDart);
        Move fastEmp = big("emp", 225, 30, 6, emp.ready(), this::beginArea, this::empTelegraph, this::fireEmp);
        this.duel = List.of(whistle, grapple, guard, lunge, dart, emp);
        this.dirty = List.of(grapple, pistol, adhesive, slowGuard, slowLunge, fastDart, fastEmp);
        this.secondWind = List.of(grapple, fastGuard, fastLunge);
        this.echoMoves = List.of(big("lunge", 80, 16, 24, t -> mob.distanceTo(t) < 9.0D, this::beginLunge, this::glintTelegraph, this::fireLunge));
        this.honest = mob.getPersistentData().getBoolean("integrated_superheroes_zemo_honest");
        this.whistled = mob.getPersistentData().getBoolean("integrated_superheroes_zemo_whistled");
    }

    @Override
    protected void initEquipment() {
        ItemStack sword = new ItemStack(HeroFx.item("antarchy:ultimate_sword"));
        try {
            sword.enchant(mob.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS), 5);
        } catch (RuntimeException ignored) {
        }
        mob.setItemSlot(EquipmentSlot.MAINHAND, sword);
        mob.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
        mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    @Override
    protected float[] thresholds() {
        return echo ? new float[0] : new float[]{0.5F};
    }

    @Override
    protected List<Move> moves() {
        if (echo) {
            return echoMoves;
        }
        if (phase == 0) {
            return duel;
        }
        return honest ? secondWind : dirty;
    }

    @Override
    protected String key() {
        return "zemo";
    }

    @Override
    protected ChatFormatting color() {
        return ChatFormatting.DARK_PURPLE;
    }

    @Override
    protected double chaseSpeed() {
        return 1.15D;
    }

    @Override
    protected int meleeCooldown() {
        return 22;
    }

    @Override
    protected void onEngage(ServerLevel level, LivingEntity target) {
        title("engage");
        Loadout.Kind kind = Loadout.kind(target);
        say(kind == Loadout.Kind.SERUM ? "activate_serum" : kind == Loadout.Kind.SUIT ? "activate_suit" : "activate_honest");
    }

    private void startGuard() {
        if (blinded()) {
            return;
        }
        long now = mob.level().getGameTime();
        guardUntil = now + (phase == 1 && honest ? 40L : 60L + random.nextInt(20));
        VillainFx.sound(mob, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 0.7F);
    }

    private boolean guarding() {
        return mob.level().getGameTime() < guardUntil && !blinded();
    }

    @Override
    protected boolean stanceTick(ServerLevel level, long now, LivingEntity t) {
        if (lungeTicks > 0) {
            return true;
        }
        if (!guarding()) {
            return false;
        }
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(t, 40.0F, 40.0F);
        if (now % 4L == 0L) {
            VillainFx.glint(level, mob);
        }
        if (now % 20L == 0L) {
            VillainFx.sound(mob, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.4F, 1.6F);
        }
        return true;
    }

    @Override
    public boolean incoming(LivingIncomingDamageEvent event) {
        if (super.incoming(event)) {
            return true;
        }
        if (echo || !guarding()) {
            return false;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (attacker == null) {
            return false;
        }
        ServerLevel level = level();
        if (inFront(attacker, 70.0D)) {
            if (source.getDirectEntity() == attacker && attacker instanceof LivingEntity living) {
                riposte(level, living);
                return true;
            }
            event.setAmount(event.getAmount() * 0.25F);
            VillainFx.sound(mob, SoundEvents.ANVIL_LAND, 0.4F, 1.9F);
            return false;
        }
        guardUntil = 0L;
        stagger(level);
        return false;
    }

    private void riposte(ServerLevel level, LivingEntity attacker) {
        VillainFx.sound(mob, SoundEvents.ANVIL_LAND, 0.7F, 1.6F);
        level.sendParticles(ParticleTypes.CRIT, mob.getX(), mob.getY(1.0D), mob.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.2D);
        mob.getLookControl().setLookAt(attacker);
        mob.swing(InteractionHand.MAIN_HAND);
        if (!friendly(attacker)) {
            mob.doHurtTarget(attacker);
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        }
        if (attacker instanceof ServerPlayer player && random.nextInt(3) == 0) {
            VillainFx.line(player, "integrated_superheroes.villains.zemo.line.parry", ChatFormatting.DARK_PURPLE);
        }
    }

    private void glintTelegraph(LivingEntity t, int left) {
        if (left % 2 == 0) {
            VillainFx.glint(level(), mob);
        }
        if (current != null && left == current.windup() && t instanceof ServerPlayer player) {
            VillainFx.soundTo(player, SoundEvents.NOTE_BLOCK_BELL.value(), 0.8F, 1.8F);
        }
    }

    private void beginLunge(LivingEntity t) {
        lungeCount++;
        feint = !echo && lungeCount % 3 == 0;
        Vec3 delta = t.position().subtract(mob.position()).multiply(1.0D, 0.0D, 1.0D);
        lungeDirection = delta.lengthSqr() < 1.0E-4D ? mob.getLookAngle() : delta.normalize();
    }

    private void fireLunge(LivingEntity t) {
        if (feint) {
            feint = false;
            recoveryLeft = 0;
            startGuard();
            return;
        }
        lungeHits.clear();
        lungeTicks = 8;
        mob.setDeltaMovement(lungeDirection.x * 1.35D, 0.12D, lungeDirection.z * 1.35D);
        mob.hurtMarked = true;
        VillainFx.sound(mob, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
        if (phase == 1 && honest) {
            if (followUp) {
                followUp = false;
            } else {
                pendingChain = true;
            }
        }
    }

    @Override
    protected void passiveTick(ServerLevel level, long now) {
        if (lungeTicks > 0) {
            lungeTicks--;
            mob.setDeltaMovement(lungeDirection.x * 1.1D, mob.getDeltaMovement().y, lungeDirection.z * 1.1D);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, mob.getX(), mob.getY(1.0D), mob.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            for (LivingEntity victim : VillainWorld.victims(level, mob.position(), 1.6D, 2.2D, e -> e != mob && !friendly(e))) {
                if (lungeHits.add(victim.getUUID())) {
                    mob.swing(InteractionHand.MAIN_HAND);
                    mob.doHurtTarget(victim);
                }
            }
            if (lungeTicks == 0 && pendingChain && target != null) {
                pendingChain = false;
                VillainWorld.later(level, 6, () -> {
                    if (mob.isAlive() && target != null && current == null && !dying) {
                        followUp = true;
                        force("lunge", target);
                    }
                });
            }
        }
        if (target != null && VillainWorld.stuck(target) && current == null && lungeTicks == 0 && now % 10L == 0L
                && readyAt.getOrDefault("lunge", 0L) > now + 20L) {
            readyAt.put("lunge", now);
        }
    }

    private void fireDart(LivingEntity t) {
        Snowball dart = new Snowball(level(), mob);
        dart.setItem(new ItemStack(HeroFx.item(ModCompat.SCORCHED_GUNS ? "scguns:syringe" : "minecraft:arrow")));
        dart.setNoGravity(true);
        dart.addTag(Villains.DART_TAG);
        Vec3 aim = t.getEyePosition().subtract(dart.position()).normalize();
        dart.setDeltaMovement(aim.scale(1.9D));
        level().addFreshEntity(dart);
        mob.swing(InteractionHand.OFF_HAND);
        VillainFx.sound(mob, SoundEvents.ARROW_SHOOT, 0.8F, 1.6F);
    }

    static void dartHit(ServerLevel level, Projectile dart, LivingEntity victim) {
        Entity owner = dart.getOwner();
        victim.hurt(owner instanceof LivingEntity living ? level.damageSources().mobProjectile(dart, living) : level.damageSources().generic(), 4.0F);
        if (victim instanceof ServerPlayer player) {
            VillainWorld.suppress(player, 120);
            VillainFx.line(player, "integrated_superheroes.villains.zemo.line.dart_hit", ChatFormatting.DARK_PURPLE);
            VillainFx.soundTo(player, SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.4F);
        }
    }

    private void beginArea(LivingEntity t) {
        lockedSpot = t.position();
        lob(lockedSpot, "minecraft:redstone_torch");
    }

    private void empTelegraph(LivingEntity t, int left) {
        if (lockedSpot == null) {
            return;
        }
        if (left % 2 == 0) {
            VillainFx.ring(level(), lockedSpot, 4.0D, VillainFx.CYAN);
        }
        if (left % 8 == 0) {
            level().sendParticles(ParticleTypes.ELECTRIC_SPARK, lockedSpot.x, lockedSpot.y + 0.3D, lockedSpot.z, 6, 0.4D, 0.2D, 0.4D, 0.1D);
            VillainFx.sound(level(), lockedSpot, SoundEvents.BEACON_AMBIENT, 1.0F, 2.0F);
        }
    }

    private void fireEmp(LivingEntity t) {
        if (lockedSpot == null) {
            return;
        }
        ServerLevel level = level();
        VillainFx.sparks(level, lockedSpot.add(0.0D, 0.5D, 0.0D), 60);
        level.sendParticles(ParticleTypes.FLASH, lockedSpot.x, lockedSpot.y + 0.5D, lockedSpot.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        VillainFx.sound(level, lockedSpot, SoundEvents.BEACON_DEACTIVATE, 1.5F, 0.6F);
        for (LivingEntity victim : VillainWorld.victims(level, lockedSpot, 4.0D, 6.0D, e -> !friendly(e))) {
            if (Loadout.suit(victim)) {
                Loadout.drainSuit(victim, 0.25D);
                if (victim instanceof ServerPlayer player) {
                    VillainWorld.ground(player, 80);
                    VillainFx.line(player, "integrated_superheroes.villains.zemo.line.emp_hit", ChatFormatting.DARK_PURPLE);
                }
            }
        }
    }

    private void fireWhistle(LivingEntity t) {
        whistled = true;
        mob.getPersistentData().putBoolean("integrated_superheroes_zemo_whistled", true);
        say("whistle");
        VillainFx.sound(mob, SoundEvents.NOTE_BLOCK_FLUTE.value(), 2.0F, 1.8F);
        HydraSquad.reinforce(level(), mob, 2, t, null);
    }

    private void fireGrapple(LivingEntity t) {
        if (!mob.hasLineOfSight(t)) {
            return;
        }
        VillainFx.lineBetween(level(), mob.getEyePosition(), t.getEyePosition(), VillainFx.WHITE);
        VillainFx.sound(mob, SoundEvents.FISHING_BOBBER_RETRIEVE, 1.2F, 0.8F);
        Vec3 pull = mob.position().subtract(t.position());
        Vec3 velocity = pull.normalize().scale(Math.min(2.2D, 0.4D + pull.length() * 0.12D)).add(0.0D, 0.45D, 0.0D);
        t.setDeltaMovement(velocity);
        t.hurtMarked = true;
        hit(t, 6.0F);
        readyAt.put("lunge", mob.level().getGameTime() + 12L);
        unreachableTicks = 0;
    }

    private void gunTelegraph(LivingEntity t, int left) {
        if (left > 5 || lockedSpot == null) {
            lockedSpot = t.position().add(0.0D, t.getBbHeight() * 0.6D, 0.0D);
        }
        VillainFx.lineBetween(level(), mob.getEyePosition(), lockedSpot, left > 5 ? VillainFx.PURPLE : VillainFx.RED);
    }

    private void firePistol(LivingEntity t) {
        ItemStack pistol = mob.getItemBySlot(EquipmentSlot.OFFHAND);
        if (pistol.isEmpty() || lockedSpot == null) {
            return;
        }
        VillainArms.shoot(mob, pistol, lockedSpot, 1.0F);
        mob.swing(InteractionHand.OFF_HAND);
        lockedSpot = null;
    }

    private void beginAdhesive(LivingEntity t) {
        lockedSpot = t.position();
        lob(lockedSpot, "minecraft:slime_ball");
    }

    private void adhesiveTelegraph(LivingEntity t, int left) {
        if (lockedSpot != null && left % 2 == 0) {
            VillainFx.ring(level(), lockedSpot, 1.5D, new org.joml.Vector3f(0.4F, 0.9F, 0.3F));
        }
    }

    private void fireAdhesive(LivingEntity t) {
        if (lockedSpot == null) {
            return;
        }
        Vec3 spot = lockedSpot;
        VillainFx.sound(level(), spot, SoundEvents.SLIME_SQUISH, 1.2F, 0.6F);
        long end = mob.level().getGameTime() + 120L;
        VillainWorld.hazard(level(), (level, now) -> {
            if (now >= end) {
                return false;
            }
            if (now % 5L == 0L) {
                level.sendParticles(ParticleTypes.ITEM_SLIME, spot.x, spot.y + 0.1D, spot.z, 6, 0.9D, 0.05D, 0.9D, 0.0D);
                for (LivingEntity victim : VillainWorld.victims(level, spot, 1.6D, 1.5D, e -> !Villains.isVillainSide(e))) {
                    VillainWorld.stick(victim, 20);
                }
            }
            return true;
        });
    }

    @Override
    protected void onTransition(ServerLevel level, int newPhase) {
        honest = true;
        for (ServerPlayer player : VillainFx.near(level, mob.position(), 32.0D)) {
            if (Loadout.kind(player) != Loadout.Kind.HONEST) {
                honest = false;
            }
        }
        mob.getPersistentData().putBoolean("integrated_superheroes_zemo_honest", honest);
        guardUntil = 0L;
        VillainFx.smoke(level, mob.position(), 5.0D);
        VillainFx.sound(mob, SoundEvents.FIRE_EXTINGUISH, 1.5F, 0.6F);
        title(honest ? "honest" : "dirty");
        say(honest ? "smoke_honest" : "smoke");
    }

    @Override
    protected void transitionTick(ServerLevel level, int left) {
        if (left % 5 == 0) {
            VillainFx.smoke(level, mob.position(), 4.0D);
        }
        if (left == 25 && anchor != null) {
            Vec3 spot = groundSpot(anchor, 7.0D, 12.0D);
            if (spot != null) {
                mob.teleportTo(spot.x, spot.y, spot.z);
                VillainFx.smoke(level, spot, 3.0D);
            }
        }
    }

    @Override
    protected void onPhaseStart(ServerLevel level, int newPhase) {
        if (!honest) {
            ItemStack pistol = new ItemStack(HeroFx.item(ModCompat.SCORCHED_GUNS ? "scguns:defender_pistol" : "minecraft:crossbow"));
            mob.setItemSlot(EquipmentSlot.OFFHAND, pistol);
            mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        } else {
            mob.swing(InteractionHand.MAIN_HAND);
            VillainFx.sound(mob, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.2F);
        }
    }

    @Override
    protected void onStagger(ServerLevel level) {
        guardUntil = 0L;
        lungeTicks = 0;
    }

    @Override
    protected void onDeathStart(ServerLevel level) {
        say("death");
        HeroStun.stun(mob, 40);
        VillainFx.smoke(level, mob.position(), 3.0D);
    }

    @Override
    protected void deathTick(ServerLevel level, int left) {
        if (left % 6 == 0) {
            VillainFx.smoke(level, mob.position(), 2.5D);
        }
    }

    @Override
    protected void onFinished(ServerLevel level) {
        VillainFx.smoke(level, mob.position(), 6.0D);
        mob.setInvisible(true);
    }

    @Override
    public void onKilled(ServerLevel level, DamageSource source) {
        Villains.awardNear(level, mob.position(), "villains/baron_zemo");
        for (ServerPlayer player : VillainFx.near(level, mob.position(), 48.0D)) {
            net.minecraft.nbt.CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            if (persisted.getBoolean(RED_BOOK_GIVEN)) {
                continue;
            }
            persisted.putBoolean(RED_BOOK_GIVEN, true);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
            ItemStack book = RedBook.create();
            if (!player.getInventory().add(book)) {
                player.drop(book, false);
            }
            VillainFx.line(player, "integrated_superheroes.villains.red_book.received", ChatFormatting.DARK_RED);
        }
    }
}
