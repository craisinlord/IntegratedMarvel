package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.AntarchyColony;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.HeroAdvancements;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.hero.HeroGnaw;
import com.craisinlord.integrated_superheroes.hero.HeroStun;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class PymColony {
    public static final String DEFEND = "defend";
    public static final String ATTACK = "attack";
    public static final String FORAGE = "forage";
    public static final String CHEW = "chew";
    public static final String SCREEN = "screen";
    public static final String STAY = "stay";
    static final double CHEW_REACH = 2.5D;
    private static final Set<String> ANT_ORDERS = Set.of(DEFEND, ATTACK, FORAGE, CHEW, STAY);
    private static final Set<String> WASP_ORDERS = Set.of(DEFEND, ATTACK, SCREEN, STAY);
    private static final String TAG = "integrated_superheroes_pym";
    private static final String OWNER = "owner";
    private static final String ROLE = "role";
    private static final String KIND = "kind";
    private static final ResourceLocation ANTHONY_SCALE = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "anthony_scale");
    private static final Map<Mob, Member> MEMBERS = new WeakHashMap<>();
    private static final Map<UUID, Colony> COLONIES = new HashMap<>();

    enum Role {
        SUMMONED,
        RECRUITED,
        ANTHONY
    }

    static final class Member {
        final PathfinderMob mob;
        final UUID owner;
        final Role role;
        final PymHelmets.Kind kind;
        final List<Goal> goals = new ArrayList<>();
        final List<Goal> targetGoals = new ArrayList<>();
        BlockPos guard;
        LivingEntity focus;
        LivingEntity sanctioned;
        LivingEntity overrun;
        int overrunTicks;
        int overrunIndex;
        LivingEntity dive;
        int diveDelay;
        int diveTicks;
        ItemStack carried = ItemStack.EMPTY;
        ItemEntity forage;
        int screenIndex;
        int screenCount = 1;
        long screenReady;

        private Member(PathfinderMob mob, UUID owner, Role role, PymHelmets.Kind kind) {
            this.mob = mob;
            this.owner = owner;
            this.role = role;
            this.kind = kind;
        }

        boolean anthony() {
            return this.role == Role.ANTHONY;
        }

        boolean bound() {
            return MEMBERS.get(this.mob) == this && this.mob.isAlive();
        }

        boolean busy() {
            return this.overrun != null || this.dive != null || this.mob.isVehicle();
        }

        ServerPlayer owner() {
            if (!(this.mob.level() instanceof ServerLevel level)) {
                return null;
            }
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.owner);
            return player != null && player.level() == level && player.isAlive() ? player : null;
        }

        String order() {
            if (this.anthony()) {
                return DEFEND;
            }
            Colony colony = COLONIES.get(this.owner);
            return colony == null ? DEFEND : colony.order;
        }
    }

    private static final class Colony {
        private final PymHelmets.Kind kind;
        private boolean out;
        private String order = DEFEND;
        private long nextSummon;
        private long anthonyBack;
        private HeroGnaw chew;

        private Colony(PymHelmets.Kind kind) {
            this.kind = kind;
        }
    }

    private PymColony() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PymColony::onJoin);
        NeoForge.EVENT_BUS.addListener(PymColony::onLeave);
        NeoForge.EVENT_BUS.addListener(PymColony::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(PymColony::onTarget);
        NeoForge.EVENT_BUS.addListener(PymColony::onDrops);
        NeoForge.EVENT_BUS.addListener(PymColony::onExperience);
        NeoForge.EVENT_BUS.addListener(PymColony::onDeath);
        NeoForge.EVENT_BUS.addListener(PymColony::onInteract);
        NeoForge.EVENT_BUS.addListener(PymColony::onLogout);
        NeoForge.EVENT_BUS.addListener(PymColony::onServerStopped);
    }

    static Member member(Entity entity) {
        return entity instanceof Mob mob ? MEMBERS.get(mob) : null;
    }

    static List<Member> members(ServerPlayer player, boolean withAnthony) {
        List<Member> found = new ArrayList<>();
        for (Member member : MEMBERS.values()) {
            if (member.owner.equals(player.getUUID()) && member.mob.isAlive() && member.mob.level() == player.level()
                    && (withAnthony || !member.anthony())) {
                found.add(member);
            }
        }
        return found;
    }

    private static List<Member> everywhere(UUID owner) {
        List<Member> found = new ArrayList<>();
        for (Member member : MEMBERS.values()) {
            if (member.owner.equals(owner)) {
                found.add(member);
            }
        }
        return found;
    }

    private static long count(ServerPlayer player, Role role) {
        return members(player, true).stream().filter(member -> member.role == role).count();
    }

    static BlockPos chewTarget(UUID owner) {
        Colony colony = COLONIES.get(owner);
        return colony == null || colony.chew == null ? null : colony.chew.target();
    }

    static void command(Member member, LivingEntity target) {
        member.sanctioned = target;
        member.mob.setTarget(target);
    }

    static boolean allowed(Member member, LivingEntity target) {
        if (target == null || !target.isAlive() || target == member.mob || target.getUUID().equals(member.owner)) {
            return false;
        }
        Member other = member(target);
        if (other != null && other.owner.equals(member.owner)) {
            return false;
        }
        if (target instanceof OwnableEntity ownable && member.owner.equals(ownable.getOwnerUUID())) {
            return false;
        }
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
            ServerPlayer owner = member.owner();
            return owner == null || owner.canHarmPlayer(player);
        }
        return true;
    }

    static LivingEntity pickTarget(Member member, ServerPlayer owner) {
        if (member.focus != null) {
            if (allowed(member, member.focus) && member.focus.level() == member.mob.level()) {
                return member.focus;
            }
            member.focus = null;
        }
        String order = member.order();
        if (STAY.equals(order) && member.guard == null) {
            return null;
        }
        if (member.guard != null) {
            Vec3 center = Vec3.atCenterOf(member.guard);
            return member.mob.level().getEntitiesOfClass(LivingEntity.class, member.mob.getBoundingBox().inflate(12.0D),
                            entity -> entity instanceof Enemy && entity.distanceToSqr(center) < 144.0D && allowed(member, entity))
                    .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(center))).orElse(null);
        }
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker != null && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200 && allowed(member, attacker)) {
            return attacker;
        }
        boolean fighting = DEFEND.equals(order) || ATTACK.equals(order) || SCREEN.equals(order);
        if (!fighting) {
            return null;
        }
        LivingEntity victim = owner.getLastHurtMob();
        if (victim != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 200 && allowed(member, victim)
                && victim.distanceToSqr(owner) < 400.0D) {
            return victim;
        }
        List<LivingEntity> near = owner.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(16.0D),
                entity -> entity != owner && allowed(member, entity)
                        && (entity instanceof Mob mob && mob.getTarget() == owner || ATTACK.equals(order) && entity instanceof Enemy));
        return near.stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(owner))).orElse(null);
    }

    public static void tick(ServerPlayer player) {
        if (!ModCompat.ANTARCHY) {
            return;
        }
        PymHelmets.Kind kind = PymAbilities.kind(player);
        Colony colony = COLONIES.get(player.getUUID());
        if (colony != null && colony.kind != kind) {
            disband(player, true);
            colony = null;
        }
        if (kind == null) {
            if (player.tickCount % 40 == 0 && !everywhere(player.getUUID()).isEmpty()) {
                disband(player, true);
            }
            return;
        }
        if (colony == null) {
            colony = new Colony(kind);
            COLONIES.put(player.getUUID(), colony);
        }
        long now = player.serverLevel().getGameTime();
        if (player.tickCount % 5 == 0) {
            tickSummons(player, colony, now);
            tickAnthony(player, colony, now);
        }
        if (player.tickCount % 20 == 0) {
            tickStrays(player);
        }
        if (CHEW.equals(colony.order)) {
            tickChew(player, colony);
        }
        if (SCREEN.equals(colony.order)) {
            tickScreen(player, now);
        }
    }

    private static Colony colony(ServerPlayer player, PymHelmets.Kind kind) {
        Colony colony = COLONIES.get(player.getUUID());
        if (colony == null || colony.kind != kind) {
            colony = new Colony(kind);
            COLONIES.put(player.getUUID(), colony);
        }
        return colony;
    }

    private static void tickSummons(ServerPlayer player, Colony colony, long now) {
        if (!colony.out || now < colony.nextSummon) {
            return;
        }
        if (count(player, Role.SUMMONED) < summonCap(colony.kind)) {
            summon(player, colony.kind, false);
        }
        colony.nextSummon = now + (long) (PymConfig.RESPAWN_SECONDS.get() * 20.0D);
    }

    private static void tickAnthony(ServerPlayer player, Colony colony, long now) {
        Member anthony = members(player, true).stream().filter(Member::anthony).findFirst().orElse(null);
        if (colony.kind != PymHelmets.Kind.ANT || !PymConfig.ANTHONY.get()) {
            if (anthony != null) {
                vanish(anthony);
            }
            return;
        }
        if (anthony != null || now < colony.anthonyBack) {
            return;
        }
        PathfinderMob mob = spawn(player, "antarchy:brown_ant", PymHelmets.Kind.ANT, PymConfig.ANTHONY_HEALTH.get(), PymConfig.ANTHONY_DAMAGE.get(), true);
        if (mob == null) {
            return;
        }
        mob.setCustomName(Component.literal("Anthony"));
        AttributeInstance scale = mob.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.addOrUpdateTransientModifier(new AttributeModifier(ANTHONY_SCALE, PymConfig.ANTHONY_SCALE.get() - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        AttributeInstance step = mob.getAttribute(Attributes.STEP_HEIGHT);
        if (step != null) {
            step.setBaseValue(Math.max(step.getBaseValue(), 1.0D));
        }
        bind(mob, player, Role.ANTHONY, PymHelmets.Kind.ANT);
    }

    private static void tickStrays(ServerPlayer player) {
        for (Member member : everywhere(player.getUUID())) {
            if (member.role != Role.RECRUITED && member.mob.level() != player.level()) {
                vanish(member);
            }
        }
    }

    private static void tickChew(ServerPlayer player, Colony colony) {
        if (colony.chew == null) {
            return;
        }
        List<Member> termites = members(player, false).stream().filter(member -> AntarchyColony.isTermite(member.mob)).toList();
        HeroGnaw.Step step = colony.chew.tick(player,
                center -> termites.stream().filter(member -> member.mob.distanceToSqr(center) < CHEW_REACH * CHEW_REACH).count(),
                PymConfig.CHEW_TICKS_PER_HARDNESS.get(), PymConfig.CHEW_DROPS.get(), PymConfig.CHEW_MAX_BLOCKS.get());
        if (step == HeroGnaw.Step.DONE) {
            setOrder(player, colony, DEFEND);
            player.displayClientMessage(Component.literal("§6The termites finished chewing."), true);
        } else if (step == HeroGnaw.Step.BROKE) {
            HeroAdvancements.count(player, "avengers/ant_man/home_wrecker", 1.0D, 100.0D);
        }
    }

    private static void tickScreen(ServerPlayer player, long now) {
        List<Member> wasps = members(player, false);
        if (wasps.isEmpty()) {
            return;
        }
        for (int i = 0; i < wasps.size(); i++) {
            wasps.get(i).screenIndex = i;
            wasps.get(i).screenCount = wasps.size();
        }
        Vec3 center = player.getBoundingBox().getCenter();
        List<Projectile> incoming = player.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(4.0D),
                projectile -> projectile.isAlive() && !friendly(projectile.getOwner(), player)
                        && projectile.getDeltaMovement().lengthSqr() > 0.04D
                        && center.subtract(projectile.position()).dot(projectile.getDeltaMovement()) > 0.0D);
        for (Projectile projectile : incoming) {
            Member guard = wasps.stream()
                    .filter(member -> member.screenReady <= now && !member.busy())
                    .min(Comparator.comparingDouble(member -> member.mob.distanceToSqr(projectile)))
                    .orElse(null);
            if (guard == null) {
                return;
            }
            guard.screenReady = now + PymConfig.SCREEN_RECHARGE_TICKS.get();
            guard.mob.swing(InteractionHand.MAIN_HAND);
            Vec3 at = projectile.position();
            player.serverLevel().sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.15D, 0.15D, 0.15D, 0.2D);
            player.level().playSound(null, at.x, at.y, at.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7F, 1.6F);
            projectile.discard();
        }
    }

    private static boolean friendly(Entity owner, ServerPlayer player) {
        if (owner == null) {
            return false;
        }
        if (owner == player) {
            return true;
        }
        Member member = member(owner);
        return member != null && member.owner.equals(player.getUUID());
    }

    private static int summonCap(PymHelmets.Kind kind) {
        return kind == PymHelmets.Kind.ANT ? PymConfig.ANT_SUMMON_CAP.get() : PymConfig.WASP_SUMMON_CAP.get();
    }

    private static int recruitCap(PymHelmets.Kind kind) {
        return kind == PymHelmets.Kind.ANT ? PymConfig.ANT_RECRUIT_CAP.get() : PymConfig.WASP_RECRUIT_CAP.get();
    }

    private static String antType(ServerPlayer player) {
        int brown = PymConfig.BROWN_WEIGHT.get();
        int red = PymConfig.RED_WEIGHT.get();
        int termite = PymConfig.TERMITE_WEIGHT.get();
        int total = brown + red + termite;
        if (total <= 0) {
            return "antarchy:brown_ant";
        }
        int roll = player.getRandom().nextInt(total);
        if (roll < brown) {
            return "antarchy:brown_ant";
        }
        return roll < brown + red ? "antarchy:red_ant" : "antarchy:termite";
    }

    private static void summon(ServerPlayer player, PymHelmets.Kind kind, boolean dramatic) {
        PathfinderMob mob = kind == PymHelmets.Kind.ANT
                ? spawn(player, antType(player), kind, PymConfig.ANT_HEALTH.get(), PymConfig.ANT_DAMAGE.get(), dramatic)
                : spawn(player, "antarchy:wasp", kind, PymConfig.WASP_HEALTH.get(), PymConfig.WASP_DAMAGE.get(), dramatic);
        if (mob != null) {
            bind(mob, player, Role.SUMMONED, kind);
        }
    }

    private static PathfinderMob spawn(ServerPlayer player, String typeId, PymHelmets.Kind kind, double health, double damage, boolean dramatic) {
        ServerLevel level = player.serverLevel();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(typeId)).orElse(null);
        if (type == null || !(type.create(level) instanceof PathfinderMob mob)) {
            return null;
        }
        BlockPos pos = kind == PymHelmets.Kind.WASP ? airNear(player) : groundNear(player);
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(health);
            mob.setHealth((float) health);
        }
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(damage);
        }
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        if (kind == PymHelmets.Kind.WASP) {
            level.playSound(null, pos, SoundEvents.BEEHIVE_EXIT, SoundSource.NEUTRAL, 0.8F, 1.2F);
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D, 4, 0.2D, 0.2D, 0.2D, 0.01D);
        } else {
            BlockState below = level.getBlockState(pos.below());
            if (!below.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), pos.getX() + 0.5D, pos.getY() + 0.1D,
                        pos.getZ() + 0.5D, dramatic ? 14 : 8, 0.25D, 0.1D, 0.25D, 0.1D);
                level.playSound(null, pos, below.getSoundType().getBreakSound(), SoundSource.NEUTRAL, 0.6F, 1.5F);
            }
            mob.setDeltaMovement(level.random.nextGaussian() * 0.08D, 0.3D, level.random.nextGaussian() * 0.08D);
        }
        return mob;
    }

    private static BlockPos groundNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = 1.5D + level.random.nextDouble() * 3.0D;
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos pos = new BlockPos(x, origin.getY() + dy, z);
                if (level.isEmptyBlock(pos) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                    return pos;
                }
            }
        }
        return origin;
    }

    private static BlockPos airNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos pos = origin.offset(level.random.nextInt(5) - 2, 1 + level.random.nextInt(2), level.random.nextInt(5) - 2);
            if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
                return pos;
            }
        }
        return origin.above();
    }

    private static void bind(PathfinderMob mob, ServerPlayer player, Role role, PymHelmets.Kind kind) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(OWNER, player.getUUID());
        tag.putString(ROLE, role.name());
        tag.putString(KIND, kind.name());
        mob.getPersistentData().put(TAG, tag);
        mob.setPersistenceRequired();
        mob.setTarget(null);
        mob.getNavigation().stop();
        attach(mob, player.getUUID(), role, kind);
    }

    private static void attach(PathfinderMob mob, UUID owner, Role role, PymHelmets.Kind kind) {
        Member member = new Member(mob, owner, role, kind);
        MEMBERS.put(mob, member);
        PymGoals.install(member);
    }

    private static void release(Member member) {
        PymGoals.uninstall(member);
        MEMBERS.remove(member.mob);
        dropCarried(member);
        member.mob.getPersistentData().remove(TAG);
        member.mob.setNoGravity(false);
        member.mob.setTarget(null);
        member.mob.getNavigation().stop();
        if (member.mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, member.mob.getX(), member.mob.getY() + 0.3D, member.mob.getZ(), 4, 0.15D, 0.1D, 0.15D, 0.0D);
        }
    }

    private static void vanish(Member member) {
        MEMBERS.remove(member.mob);
        dropCarried(member);
        if (member.mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, member.mob.getX(), member.mob.getY() + 0.2D, member.mob.getZ(), 5, 0.15D, 0.15D, 0.15D, 0.02D);
        }
        member.mob.discard();
    }

    private static void dropCarried(Member member) {
        if (!member.carried.isEmpty()) {
            member.mob.spawnAtLocation(member.carried);
            member.carried = ItemStack.EMPTY;
        }
    }

    private static void disband(ServerPlayer player, boolean releaseRecruits) {
        for (Member member : everywhere(player.getUUID())) {
            if (member.role != Role.RECRUITED) {
                vanish(member);
            } else if (releaseRecruits) {
                release(member);
            }
        }
        Colony colony = COLONIES.remove(player.getUUID());
        if (colony != null && colony.chew != null) {
            colony.chew.stop(player.serverLevel());
        }
    }

    private static String colonyWord(PymHelmets.Kind kind) {
        return kind == PymHelmets.Kind.ANT ? "colony" : "hive";
    }

    private static String memberWord(PymHelmets.Kind kind, long count) {
        String word = kind == PymHelmets.Kind.ANT ? "ant" : "wasp";
        return count == 1 ? word : word + "s";
    }

    private static void buzz(ServerPlayer player, PymHelmets.Kind kind, float pitch) {
        HeroFx.play(player, kind == PymHelmets.Kind.ANT ? SoundEvents.SILVERFISH_AMBIENT : SoundEvents.BEE_LOOP_AGGRESSIVE, 0.8F, pitch);
    }

    static double callColony(ServerPlayer player, PymHelmets.Kind kind) {
        Colony colony = colony(player, kind);
        if (colony.out) {
            colony.out = false;
            members(player, false).stream().filter(member -> member.role == Role.SUMMONED).forEach(PymColony::vanish);
            player.displayClientMessage(Component.literal("§6The " + colonyWord(kind) + " disperses."), true);
            buzz(player, kind, 0.8F);
        } else {
            colony.out = true;
            long missing = summonCap(kind) - count(player, Role.SUMMONED);
            for (long i = 0; i < missing; i++) {
                summon(player, kind, true);
            }
            colony.nextSummon = player.serverLevel().getGameTime() + (long) (PymConfig.RESPAWN_SECONDS.get() * 20.0D);
            player.displayClientMessage(Component.literal(kind == PymHelmets.Kind.ANT ? "§6Colony, assemble!" : "§6Hive, swarm!"), true);
            buzz(player, kind, 1.2F);
            HeroAdvancements.award(player, "avengers/ant_man");
        }
        return PymConfig.SUMMON_COOLDOWN.get();
    }

    static double pheromones(ServerPlayer player, PymHelmets.Kind kind) {
        colony(player, kind);
        ServerLevel level = player.serverLevel();
        if (player.isShiftKeyDown()) {
            List<Member> recruits = members(player, false).stream().filter(member -> member.role == Role.RECRUITED).toList();
            if (recruits.isEmpty()) {
                player.displayClientMessage(Component.literal("§7You have no recruits to release."), true);
                return 0.0D;
            }
            recruits.forEach(PymColony::release);
            player.displayClientMessage(Component.literal("§6Released " + recruits.size() + " " + memberWord(kind, recruits.size()) + "."), true);
            buzz(player, kind, 0.7F);
            return 1.0D;
        }
        long room = recruitCap(kind) - count(player, Role.RECRUITED);
        if (room <= 0) {
            player.displayClientMessage(Component.literal("§7Your " + colonyWord(kind) + " can't hold any more recruits."), true);
            return 0.0D;
        }
        double radius = PymConfig.RECRUIT_RADIUS.get();
        List<PathfinderMob> wild = level.getEntitiesOfClass(PathfinderMob.class, player.getBoundingBox().inflate(radius),
                        mob -> mob.isAlive() && mob.distanceToSqr(player) <= radius * radius && wild(mob, kind))
                .stream()
                .sorted(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)))
                .limit(room)
                .toList();
        level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 1.0D, player.getZ(), 20, radius * 0.25D, 0.5D, radius * 0.25D, 0.0D);
        if (wild.isEmpty()) {
            player.displayClientMessage(Component.literal("§7No wild " + memberWord(kind, 2) + " answer the call."), true);
            return 0.0D;
        }
        for (PathfinderMob mob : wild) {
            bind(mob, player, Role.RECRUITED, kind);
            trail(level, player.position().add(0.0D, 1.0D, 0.0D), mob.position().add(0.0D, mob.getBbHeight() * 0.5D, 0.0D));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + 0.2D, mob.getZ(), 3, 0.2D, 0.1D, 0.2D, 0.0D);
        }
        player.displayClientMessage(Component.literal("§6" + wild.size() + " " + memberWord(kind, wild.size())
                + (wild.size() == 1 ? " joins" : " join") + " the " + colonyWord(kind) + "."), true);
        HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.4F);
        HeroAdvancements.award(player, "avengers/ant_man");
        return PymConfig.RECRUIT_COOLDOWN.get();
    }

    private static boolean wild(PathfinderMob mob, PymHelmets.Kind kind) {
        if (MEMBERS.containsKey(mob) || mob.getPersistentData().contains(TAG)) {
            return false;
        }
        return kind == PymHelmets.Kind.ANT ? AntarchyColony.isAnt(mob) : AntarchyColony.isWasp(mob);
    }

    private static void trail(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        int points = (int) Math.min(24, Math.max(2, step.length() * 2.0D));
        for (int i = 0; i <= points; i++) {
            Vec3 at = from.add(step.scale(i / (double) points));
            level.sendParticles(ParticleTypes.WAX_OFF, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static boolean needSwarm(ServerPlayer player, PymHelmets.Kind kind, List<Member> swarm) {
        if (!swarm.isEmpty()) {
            return true;
        }
        player.displayClientMessage(Component.literal("§7Call or recruit your " + colonyWord(kind) + " first."), true);
        return false;
    }

    private static LivingEntity aimed(ServerPlayer player) {
        return HeroAbilities.beam(player, player.getLookAngle(), PymConfig.COMMAND_RANGE.get(), 8).targets().stream()
                .filter(entity -> {
                    Member member = member(entity);
                    return (member == null || !member.owner.equals(player.getUUID())) && entity != player.getVehicle();
                })
                .findFirst()
                .orElse(null);
    }

    static double sicEm(ServerPlayer player, PymHelmets.Kind kind) {
        List<Member> swarm = members(player, false);
        if (!needSwarm(player, kind, swarm)) {
            return 0.0D;
        }
        LivingEntity target = aimed(player);
        if (target != null) {
            swarm.forEach(member -> {
                member.guard = null;
                member.focus = target;
                command(member, target);
            });
            Vec3 top = target.position().add(0.0D, target.getBbHeight() + 0.3D, 0.0D);
            player.serverLevel().sendParticles(ParticleTypes.ANGRY_VILLAGER, top.x, top.y, top.z, 3, 0.3D, 0.2D, 0.3D, 0.0D);
            player.displayClientMessage(Component.literal("§6Sic 'em!"), true);
        } else {
            Vec3 eye = player.getEyePosition();
            BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(PymConfig.COMMAND_RANGE.get())),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                return 0.0D;
            }
            BlockPos guard = hit.getBlockPos().relative(hit.getDirection());
            swarm.forEach(member -> {
                member.focus = null;
                member.sanctioned = null;
                member.guard = guard;
                member.mob.setTarget(null);
                member.mob.getNavigation().stop();
            });
            player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, guard.getX() + 0.5D, guard.getY() + 0.3D, guard.getZ() + 0.5D,
                    8, 0.4D, 0.2D, 0.4D, 0.0D);
            player.displayClientMessage(Component.literal("§6Guard that spot!"), true);
        }
        buzz(player, kind, 1.5F);
        return PymConfig.SIC_EM_COOLDOWN.get();
    }

    static double overrun(ServerPlayer player) {
        List<Member> swarm = members(player, false);
        if (!needSwarm(player, PymHelmets.Kind.ANT, swarm)) {
            return 0.0D;
        }
        LivingEntity target = aimed(player);
        if (target == null) {
            player.displayClientMessage(Component.literal("§7Look at something to overrun."), true);
            return 0.0D;
        }
        int ticks = PymConfig.OVERRUN_TICKS.get();
        List<Member> sorted = swarm.stream().sorted(Comparator.comparingDouble(member -> member.mob.distanceToSqr(target))).toList();
        for (int i = 0; i < sorted.size(); i++) {
            Member member = sorted.get(i);
            member.guard = null;
            member.dive = null;
            member.overrun = target;
            member.overrunTicks = ticks;
            member.overrunIndex = i;
        }
        if (HeroAbilities.canControl(target)) {
            HeroStun.stun(target, ticks);
        }
        if (sorted.size() >= PymConfig.ANT_SUMMON_CAP.get()) {
            HeroAdvancements.award(player, "avengers/ant_man/picnic_ruined");
        }
        HeroFx.play(target, SoundEvents.SILVERFISH_HURT, 1.2F, 0.8F);
        player.displayClientMessage(Component.literal("§6Overrun!"), true);
        return PymConfig.OVERRUN_COOLDOWN.get();
    }

    static double stingStorm(ServerPlayer player) {
        List<Member> swarm = members(player, false);
        if (!needSwarm(player, PymHelmets.Kind.WASP, swarm)) {
            return 0.0D;
        }
        LivingEntity target = aimed(player);
        if (target == null) {
            player.displayClientMessage(Component.literal("§7Look at something to sting."), true);
            return 0.0D;
        }
        int stagger = PymConfig.STING_STORM_STAGGER_TICKS.get();
        List<Member> sorted = swarm.stream().sorted(Comparator.comparingDouble(member -> member.mob.distanceToSqr(target))).toList();
        for (int i = 0; i < sorted.size(); i++) {
            Member member = sorted.get(i);
            member.guard = null;
            member.overrun = null;
            member.dive = target;
            member.diveDelay = i * stagger;
            member.diveTicks = 0;
        }
        if (sorted.size() >= PymConfig.WASP_SUMMON_CAP.get()) {
            HeroAdvancements.award(player, "avengers/ant_man/sting_operation");
        }
        buzz(player, PymHelmets.Kind.WASP, 1.6F);
        player.displayClientMessage(Component.literal("§6Sting Storm!"), true);
        return PymConfig.STING_STORM_COOLDOWN.get();
    }

    static void giveOrder(ServerPlayer player, PymHelmets.Kind kind, String choice) {
        if (!(kind == PymHelmets.Kind.ANT ? ANT_ORDERS : WASP_ORDERS).contains(choice)) {
            return;
        }
        Colony colony = colony(player, kind);
        if (colony.chew != null) {
            colony.chew.stop(player.serverLevel());
            colony.chew = null;
        }
        String option = choice;
        if (CHEW.equals(choice)) {
            if (members(player, false).stream().noneMatch(member -> AntarchyColony.isTermite(member.mob))) {
                player.displayClientMessage(Component.literal("§7You have no termites to chew with."), true);
                option = DEFEND;
            } else {
                colony.chew = HeroGnaw.aim(player, PymConfig.COMMAND_RANGE.get(), PymColony::chewable, -2000 - player.getId());
                if (colony.chew == null) {
                    player.displayClientMessage(Component.literal("§7Look at something wooden for the termites to chew."), true);
                    option = DEFEND;
                }
            }
        }
        setOrder(player, colony, option);
        if (!option.equals(choice)) {
            return;
        }
        String label = PymAbilities.orderLabel(option);
        player.displayClientMessage(Component.literal("§6" + (kind == PymHelmets.Kind.ANT ? "Colony" : "Hive") + ": " + label), true);
        buzz(player, kind, 1.1F);
    }

    private static boolean chewable(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_AXE);
    }

    private static void setOrder(ServerPlayer player, Colony colony, String option) {
        if (!CHEW.equals(option) && colony.chew != null) {
            colony.chew.stop(player.serverLevel());
            colony.chew = null;
        }
        colony.order = option;
        members(player, false).forEach(member -> {
            member.guard = null;
            member.focus = null;
            member.sanctioned = null;
            member.forage = null;
            member.mob.setTarget(null);
            member.mob.getNavigation().stop();
        });
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PathfinderMob mob) || MEMBERS.containsKey(mob)) {
            return;
        }
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(TAG)) {
            return;
        }
        CompoundTag tag = data.getCompound(TAG);
        Role role;
        PymHelmets.Kind kind;
        try {
            role = Role.valueOf(tag.getString(ROLE));
            kind = PymHelmets.Kind.valueOf(tag.getString(KIND));
        } catch (IllegalArgumentException exception) {
            data.remove(TAG);
            return;
        }
        if (role != Role.RECRUITED || !tag.hasUUID(OWNER)) {
            event.setCanceled(true);
            return;
        }
        attach(mob, tag.getUUID(OWNER), role, kind);
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob) {
            Member member = MEMBERS.get(mob);
            if (member != null) {
                PymGoals.uninstall(member);
                MEMBERS.remove(mob);
            }
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        Member defender = member(victim);
        if (defender != null) {
            if (defender.role != Role.RECRUITED && event.getSource().is(DamageTypes.IN_WALL)) {
                event.setCanceled(true);
                return;
            }
            if (attacker != null && (attacker.getUUID().equals(defender.owner) || friendlyMember(attacker, defender.owner))) {
                event.setCanceled(true);
                return;
            }
        }
        Member striker = member(attacker);
        if (striker == null) {
            return;
        }
        if (victim.getUUID().equals(striker.owner)) {
            event.setCanceled(true);
            return;
        }
        int fire = PymConfig.RED_ANT_FIRE_SECONDS.get();
        if (fire > 0 && AntarchyColony.isRedAnt(striker.mob)) {
            victim.igniteForSeconds(fire);
        }
    }

    private static boolean friendlyMember(Entity entity, UUID owner) {
        Member member = member(entity);
        return member != null && member.owner.equals(owner);
    }

    private static void onTarget(LivingChangeTargetEvent event) {
        Member member = member(event.getEntity());
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (member == null || target == null) {
            return;
        }
        if (!allowed(member, target) || target != member.sanctioned && target != member.mob.getLastHurtByMob()) {
            event.setCanceled(true);
        }
    }

    private static void onDrops(LivingDropsEvent event) {
        Member member = member(event.getEntity());
        if (member != null && member.role != Role.RECRUITED) {
            event.setCanceled(true);
        }
    }

    private static void onExperience(LivingExperienceDropEvent event) {
        Member member = member(event.getEntity());
        if (member != null && member.role != Role.RECRUITED) {
            event.setCanceled(true);
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        Member member = member(event.getEntity());
        if (member == null) {
            return;
        }
        dropCarried(member);
        if (!member.anthony() || !(member.mob.level() instanceof ServerLevel level)) {
            return;
        }
        Colony colony = COLONIES.get(member.owner);
        if (colony != null) {
            colony.anthonyBack = level.getGameTime() + (long) (PymConfig.ANTHONY_RESPAWN_SECONDS.get() * 20.0D);
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(member.owner);
        if (owner != null) {
            owner.displayClientMessage(Component.literal("§6Anthony is down! He'll be back."), true);
        }
    }

    private static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Member member = member(event.getTarget());
        if (member == null || member.role == Role.RECRUITED) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Player player = event.getEntity();
        if (!member.anthony() || event.getHand() != InteractionHand.MAIN_HAND || !player.getUUID().equals(member.owner)
                || !player.getMainHandItem().isEmpty()) {
            return;
        }
        if (player.getScale() > PymConfig.ANTHONY_RIDE_MAX_SCALE.get()) {
            player.displayClientMessage(Component.literal("§7Shrink down to ride Anthony."), true);
            return;
        }
        member.mob.getNavigation().stop();
        member.mob.setTarget(null);
        if (player.startRiding(member.mob) && player instanceof ServerPlayer serverPlayer) {
            HeroAdvancements.award(serverPlayer, "avengers/ant_man/anthony");
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            disband(player, false);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        MEMBERS.clear();
        COLONIES.clear();
    }
}
