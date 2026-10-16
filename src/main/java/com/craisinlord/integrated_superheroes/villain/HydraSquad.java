package com.craisinlord.integrated_superheroes.villain;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.ScorchedGunsVillains;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.hero.HeroStun;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tintankgames.marvel.world.entity.HydraAgent;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HydraSquad {
    private static final String KEY_ARMED = "integrated_superheroes_hydra_armed";
    private static final String KEY_ROLE = "integrated_superheroes_hydra_role";
    private static final String KEY_FORCED_ROLE = "integrated_superheroes_hydra_forced_role";
    private static final String KEY_NO_OFFICER = "integrated_superheroes_hydra_no_officer";
    private static final String TAG_PREFIX = "integrated_superheroes_hydra_";
    private static final Map<UUID, Disarm> DISARMED = new HashMap<>();
    private static final Map<UUID, Long> OFFICER_COOLDOWN = new HashMap<>();

    private HydraSquad() {
    }

    private record Disarm(ItemStack stack, long until) {
    }

    public enum Role {
        // Mob guns fire once per gun "rate" ticks, so these are copper/blackpowder picks chosen on damage per second.
        RIFLEMAN("scguns:makeshift_rifle", "minecraft:crossbow", 22, 2),
        ASSAULT("scguns:rusty_gnat", "minecraft:iron_sword", 16, 2),
        // Every lower-tier shotgun fires about twice as fast, so the combat shotgun is still the gentlest one.
        BREACHER("scguns:combat_shotgun", "minecraft:iron_axe", 12, 2),
        SNIPER("scguns:llr_director", "minecraft:crossbow", 10, 3),
        GRENADIER("scguns:umax_pistol", "minecraft:iron_sword", 12, 2),
        TROOPER("scguns:laser_musket", "minecraft:crossbow", 10, 2),
        CROSSBOWMAN("marvel:tesseract_crossbow", "marvel:tesseract_crossbow", 10, 2),
        OFFICER("scguns:flintlock_pistol", "minecraft:crossbow", 8, 3);

        final String gun;
        final String fallback;
        final int weight;
        final int aiDifficulty;

        Role(String gun, String fallback, int weight, int aiDifficulty) {
            this.gun = gun;
            this.fallback = fallback;
            this.weight = weight;
            this.aiDifficulty = aiDifficulty;
        }

        String weapon() {
            return ModCompat.SCORCHED_GUNS ? gun : fallback;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        static Role parse(String id) {
            for (Role role : values()) {
                if (role.id().equals(id)) {
                    return role;
                }
            }
            return null;
        }
    }

    public static Role role(Entity agent) {
        return Role.parse(agent.getPersistentData().getString(KEY_ROLE));
    }

    static void onJoin(HydraAgent agent) {
        CompoundTag data = agent.getPersistentData();
        data.putBoolean(VillainBrain.MANAGED, true);
        if (VillainConfig.ARM_AGENTS.get() && !data.getBoolean(KEY_ARMED)) {
            data.putBoolean(KEY_ARMED, true);
            Role role = Role.parse(data.getString(KEY_FORCED_ROLE));
            if (role == null) {
                role = roll(agent, data.getBoolean(KEY_NO_OFFICER));
            }
            arm(agent, role);
        }
        if (VillainArms.gun(agent.getMainHandItem())) {
            Villains.stripCombatGoals(agent, false);
            ScorchedGunsVillains.reassessGunner(agent);
        }
        agent.goalSelector.addGoal(0, new SquadGoal(agent));
    }

    private static Role roll(HydraAgent agent, boolean noOfficer) {
        int total = 0;
        for (Role role : Role.values()) {
            if (!(noOfficer && role == Role.OFFICER)) {
                total += role.weight;
            }
        }
        int pick = agent.getRandom().nextInt(total);
        for (Role role : Role.values()) {
            if (noOfficer && role == Role.OFFICER) {
                continue;
            }
            pick -= role.weight;
            if (pick < 0) {
                return role;
            }
        }
        return Role.RIFLEMAN;
    }

    private static void arm(HydraAgent agent, Role role) {
        CompoundTag data = agent.getPersistentData();
        data.putString(KEY_ROLE, role.id());
        agent.addTag(TAG_PREFIX + role.id());
        ItemStack weapon = new ItemStack(HeroFx.item(role.weapon()));
        if (ModCompat.SCORCHED_GUNS) {
            ScorchedGunsVillains.prepareGunner(weapon, data, role.aiDifficulty);
        }
        agent.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        agent.setDropChance(EquipmentSlot.MAINHAND, VillainConfig.GUN_DROP_CHANCE.get().floatValue());
        if (role == Role.OFFICER) {
            agent.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(HeroFx.item("minecraft:diamond_sword")));
            agent.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        }
    }

    static void onHurt(HydraAgent agent, DamageSource source) {
        if (source.getEntity() instanceof Player player && Villains.validTarget(player) && agent.level() instanceof ServerLevel level) {
            alert(level, agent.position(), VillainConfig.ALERT_RADIUS.get(), player);
            if (Loadout.magneto(player) && VillainArms.disarmable(agent.getMainHandItem()) && !DISARMED.containsKey(agent.getUUID())) {
                DISARMED.put(agent.getUUID(), new Disarm(agent.getMainHandItem().copy(), level.getGameTime() + 100L));
                agent.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                VillainFx.sound(agent, SoundEvents.ITEM_BREAK, 0.8F, 0.6F);
                VillainFx.sparks(level, agent.position().add(0.0D, 1.2D, 0.0D), 8);
            }
        }
    }

    static void onDeath(ServerLevel level, HydraAgent agent, DamageSource source) {
        Disarm disarm = DISARMED.remove(agent.getUUID());
        if (disarm != null && agent.getRandom().nextFloat() < VillainConfig.GUN_DROP_CHANCE.get()) {
            agent.spawnAtLocation(disarm.stack());
        }
        HydraAlarm.Alarm alarm = HydraAlarm.active(level, agent.position());
        if (alarm == null || alarm.spawned >= VillainConfig.REINFORCEMENTS_PER_ALARM.get()) {
            return;
        }
        long now = level.getGameTime();
        List<HydraAgent> officers = level.getEntitiesOfClass(HydraAgent.class, agent.getBoundingBox().inflate(24.0D),
                e -> e.isAlive() && e != agent && role(e) == Role.OFFICER);
        for (HydraAgent officer : officers) {
            if (OFFICER_COOLDOWN.getOrDefault(officer.getUUID(), 0L) > now) {
                continue;
            }
            OFFICER_COOLDOWN.put(officer.getUUID(), now + 400L);
            LivingEntity killer = source.getEntity() instanceof LivingEntity living ? living : null;
            int spawned = reinforce(level, officer, 1, killer, null);
            alarm.spawned += spawned;
            if (spawned > 0) {
                VillainFx.sound(officer, SoundEvents.NOTE_BLOCK_FLUTE.value(), 1.5F, 1.6F);
            }
            return;
        }
    }

    public static int reinforce(ServerLevel level, Mob near, int count, LivingEntity target, Role role) {
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Vec3 spot = hiddenSpot(level, near.position(), target);
            if (spot != null && spawnAt(level, spot, target, role) != null) {
                spawned++;
            }
        }
        return spawned;
    }

    public static HydraAgent spawnAt(ServerLevel level, Vec3 spot, LivingEntity target, Role role) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("marvel:hydra_agent"));
        if (!(type.create(level) instanceof HydraAgent agent)) {
            return null;
        }
        agent.moveTo(spot.x, spot.y, spot.z, level.random.nextFloat() * 360.0F, 0.0F);
        agent.getPersistentData().putBoolean(KEY_NO_OFFICER, true);
        if (role != null) {
            agent.getPersistentData().putString(KEY_FORCED_ROLE, role.id());
        }
        agent.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(spot)), MobSpawnType.REINFORCEMENT, null);
        if (!level.addFreshEntity(agent)) {
            return null;
        }
        if (target != null && target.isAlive()) {
            agent.setTarget(target);
        }
        return agent;
    }

    private static Vec3 hiddenSpot(ServerLevel level, Vec3 around, LivingEntity target) {
        Vec3 fallback = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = 6.0D + level.random.nextDouble() * 8.0D;
            int x = Mth.floor(around.x + Math.cos(angle) * distance);
            int z = Mth.floor(around.z + Math.sin(angle) * distance);
            for (int dy = 3; dy >= -4; dy--) {
                BlockPos pos = BlockPos.containing(x, around.y + dy, z);
                if (!level.getBlockState(pos.below()).blocksMotion()
                        || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                    continue;
                }
                Vec3 spot = Vec3.atBottomCenterOf(pos);
                if (fallback == null) {
                    fallback = spot;
                }
                if (target == null || !visible(level, target, spot)) {
                    return spot;
                }
                break;
            }
        }
        return fallback;
    }

    private static boolean visible(ServerLevel level, LivingEntity viewer, Vec3 spot) {
        Vec3 eye = viewer.getEyePosition();
        Vec3 head = spot.add(0.0D, 1.6D, 0.0D);
        return level.clip(new net.minecraft.world.level.ClipContext(eye, head, net.minecraft.world.level.ClipContext.Block.VISUAL,
                net.minecraft.world.level.ClipContext.Fluid.NONE, viewer)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    static void alert(ServerLevel level, Vec3 center, double radius, LivingEntity intruder) {
        for (HydraAgent other : level.getEntitiesOfClass(HydraAgent.class, new net.minecraft.world.phys.AABB(center, center).inflate(radius),
                e -> e.isAlive() && e.getTarget() == null)) {
            other.setTarget(intruder);
        }
    }

    public static void salute(ServerLevel level, Mob boss, double radius) {
        for (HydraAgent agent : level.getEntitiesOfClass(HydraAgent.class, boss.getBoundingBox().inflate(radius), Entity::isAlive)) {
            HeroStun.stun(agent, 20);
            agent.getLookControl().setLookAt(boss);
            VillainWorld.later(level, 20, () -> {
                if (agent.isAlive()) {
                    agent.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
                    agent.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
                }
            });
        }
        VillainFx.line(level, boss.position(), radius + 8.0D, "integrated_superheroes.villains.hydra.hail", net.minecraft.ChatFormatting.DARK_RED);
    }

    public static void guard(ServerLevel level, Mob boss, LivingEntity threat) {
        if (threat == null) {
            return;
        }
        Vec3 middle = boss.position().add(threat.position()).scale(0.5D);
        for (HydraAgent agent : level.getEntitiesOfClass(HydraAgent.class, boss.getBoundingBox().inflate(12.0D), Entity::isAlive)) {
            agent.getNavigation().moveTo(middle.x + level.random.nextGaussian(), middle.y, middle.z + level.random.nextGaussian(), 1.3D);
        }
    }

    private static final class SquadGoal extends Goal {
        private final HydraAgent agent;
        private final Role role;
        private long sightStart;
        private boolean alerted;
        private long nextGrenade;
        private int grenadeWindup;
        private Vec3 grenadeSpot;
        private boolean stun;

        SquadGoal(HydraAgent agent) {
            this.agent = agent;
            this.role = role(agent);
            this.setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            return agent.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return agent.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!(agent.level() instanceof ServerLevel level)) {
                return;
            }
            long now = level.getGameTime();
            restoreDisarm(level, now);
            LivingEntity target = agent.getTarget();
            boolean sees = target instanceof Player player && Villains.validTarget(player) && agent.hasLineOfSight(target);
            if (sees) {
                if (sightStart == 0L) {
                    sightStart = now;
                }
                if (!alerted && now - sightStart >= 20L) {
                    alerted = true;
                    alert(level, agent.position(), VillainConfig.ALERT_RADIUS.get(), target);
                }
                if (now - sightStart >= 60L && now % 20L == 0L) {
                    HydraAlarm.raise(level, agent.position(), target);
                }
            } else {
                sightStart = 0L;
            }
            if (role == Role.OFFICER && now % 20L == 0L) {
                rally(level);
            }
            if (role == Role.GRENADIER) {
                grenadeTick(level, now, target, sees);
            }
        }

        private void restoreDisarm(ServerLevel level, long now) {
            Disarm disarm = DISARMED.get(agent.getUUID());
            if (disarm != null && now >= disarm.until()) {
                DISARMED.remove(agent.getUUID());
                agent.setItemSlot(EquipmentSlot.MAINHAND, disarm.stack());
            }
        }

        private void rally(ServerLevel level) {
            DustParticleOptions red = new DustParticleOptions(VillainFx.RED, 1.0F);
            for (HydraAgent other : level.getEntitiesOfClass(HydraAgent.class, agent.getBoundingBox().inflate(12.0D), e -> e.isAlive() && e != agent)) {
                other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
                other.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false));
                level.sendParticles(red, other.getX(), other.getY(1.1D), other.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);
            }
            level.sendParticles(red, agent.getX(), agent.getY(1.2D), agent.getZ(), 4, 0.25D, 0.3D, 0.25D, 0.0D);
        }

        private void grenadeTick(ServerLevel level, long now, LivingEntity target, boolean sees) {
            if (grenadeWindup > 0) {
                if (grenadeSpot != null && grenadeWindup % 2 == 0) {
                    VillainFx.ring(level, grenadeSpot, stun ? 2.0D : 2.5D, stun ? VillainFx.WHITE : VillainFx.ORANGE);
                }
                if (--grenadeWindup == 0 && grenadeSpot != null) {
                    throwGrenade(level);
                }
                return;
            }
            if (!sees || now < nextGrenade) {
                return;
            }
            double distance = agent.distanceTo(target);
            if (distance < 2.5D || distance > 20.0D) {
                return;
            }
            nextGrenade = now + 160L + agent.getRandom().nextInt(80);
            stun = distance < 6.0D;
            grenadeSpot = target.position();
            grenadeWindup = 20;
        }

        private void throwGrenade(ServerLevel level) {
            VillainArms.grenade(level, agent, grenadeSpot, stun, 2.0F);
            agent.swing(net.minecraft.world.InteractionHand.OFF_HAND);
            VillainFx.sound(agent, SoundEvents.SNOWBALL_THROW, 1.0F, 0.6F);
            grenadeSpot = null;
        }
    }
}
