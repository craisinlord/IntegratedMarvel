package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.init.AttachmentsRegistry;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class HeroAdvancements {
    private static final String DATA = "integrated_superheroes_advancements";
    private static final String POWERS = "powers";
    private static final String LAST_POWER = "last_power";
    private static final Map<String, String> BRANCHES = new HashMap<>();
    private static final Set<String> LEGACY_POWERS = Set.of("spider_powers_peter_parker", "weapon_x_logan");
    private static final ThreadLocal<Cast> CURRENT = new ThreadLocal<>();
    private static final Map<UUID, Tagged> TAGGED = new HashMap<>();
    private static final Map<UUID, List<Window>> WINDOWS = new HashMap<>();
    private static final Map<String, Vec3> LAST_POSITIONS = new HashMap<>();
    private static final Map<UUID, Integer> CEILING_TICKS = new HashMap<>();

    static {
        branch("x_men/cyclops", CyclopsPower.ID);
        branch("x_men/iceman", IcemanPower.ID);
        branch("x_men/storm", StormPower.ID);
        branch("x_men/colossus", ColossusPower.ID);
        branch("x_men/kitty_pryde", KittyPower.ID);
        branch("x_men/wolverine", "weapon_x_logan");
        branch("x_men/magneto", MagnetoPower.ID);
        branch("fantastic_four/mr_fantastic", MrFantasticPower.ID);
        branch("fantastic_four/invisible_woman", InvisibleWomanPower.ID);
        branch("fantastic_four/human_torch", HumanTorchPower.ID);
        branch("fantastic_four/thing", ThingPower.ID);
        branch("avengers/captain_america", SuperSoldierPower.ID);
        branch("avengers/hulk", HulkPower.ID);
        branch("avengers/binary", BinaryPower.ID);
        branch("avengers/scarlet_witch", ScarletWitchPower.ID);
        branch("avengers/quicksilver", QuicksilverPower.ID);
        branch("avengers/black_panther", PantherPower.ID);
        branch("street_level/spider_man", "spider_powers_peter_parker");
        branch("street_level/daredevil", DaredevilPower.ID);
        branch("street_level/squirrel_girl", SquirrelGirlPower.ID);
    }

    private HeroAdvancements() {
    }

    private static void branch(String path, String power) {
        BRANCHES.put(power, path);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDamageEvent.Post.class, HeroAdvancements::onDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, HeroAdvancements::onDeath);
        NeoForge.EVENT_BUS.addListener(HeroAdvancements::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(HeroAdvancements::onServerTick);
        NeoForge.EVENT_BUS.addListener(HeroAdvancements::onExplosion);
    }

    public static void award(ServerPlayer player, String path) {
        AdvancementHolder advancement = player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, path));
        if (advancement == null) {
            return;
        }
        for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private static CompoundTag data(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(DATA)) {
            persisted.put(DATA, new CompoundTag());
        }
        return persisted.getCompound(DATA);
    }

    public static void count(ServerPlayer player, String path, double amount, double goal) {
        CompoundTag data = data(player);
        double total = data.getDouble(path) + amount;
        data.putDouble(path, total);
        if (total >= goal) {
            award(player, path);
        }
    }

    public static boolean flag(ServerPlayer player, String key) {
        CompoundTag data = data(player);
        if (data.getBoolean(key)) {
            return false;
        }
        data.putBoolean(key, true);
        return true;
    }

    public static boolean flagged(ServerPlayer player, String key) {
        return data(player).getBoolean(key);
    }

    public static void travel(ServerPlayer player, String path, boolean active, double goal) {
        String key = player.getUUID() + path;
        Vec3 last = LAST_POSITIONS.get(key);
        if (!active) {
            LAST_POSITIONS.remove(key);
            return;
        }
        Vec3 now = player.position();
        LAST_POSITIONS.put(key, now);
        if (last != null) {
            double moved = now.distanceTo(last);
            if (moved < 8.0D) {
                count(player, path, moved, goal);
            }
        }
    }

    public static boolean isPower(Player player, String power) {
        return player.hasData(AttachmentsRegistry.PLAYER_DATA.get()) && power.equals(HeroPowers.data(player).getPrimaryPower());
    }

    public static void thrownBlock(ServerPlayer owner, LivingEntity target) {
        if (owner != null && isPower(owner, ThingPower.ID)) {
            cast(owner).kills(entity -> true, "fantastic_four/thing/yancy_street_special").hit(target);
        }
    }

    public static Cast cast(ServerPlayer player) {
        return new Cast(player);
    }

    public static Cast current() {
        return CURRENT.get();
    }

    public static void within(Cast cast, Runnable action) {
        if (cast == null) {
            action.run();
            return;
        }
        Cast previous = CURRENT.get();
        CURRENT.set(cast);
        try {
            action.run();
        } finally {
            CURRENT.set(previous);
        }
    }

    public static void window(ServerPlayer player, int ticks, int kills, String path) {
        WINDOWS.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>())
                .add(new Window(player.server.getTickCount() + ticks, kills, path));
    }

    public static final class Cast {
        private final ServerPlayer player;
        private final Set<UUID> hits = new HashSet<>();
        private int kills;
        private int hitGoal;
        private String hitPath;
        private int killGoal;
        private String killPath;
        private Predicate<LivingEntity> killFilter = entity -> true;
        private int memoryTicks = 200;

        private Cast(ServerPlayer player) {
            this.player = player;
        }

        public Cast hits(int goal, String path) {
            this.hitGoal = goal;
            this.hitPath = path;
            return this;
        }

        public Cast kills(int goal, String path) {
            this.killGoal = goal;
            this.killPath = path;
            return this;
        }

        public Cast kills(Predicate<LivingEntity> filter, String path) {
            this.killFilter = filter;
            return this.kills(1, path);
        }

        public Cast memory(int ticks) {
            this.memoryTicks = ticks;
            return this;
        }

        public void run(Runnable action) {
            within(this, action);
        }

        public void hit(LivingEntity target) {
            if (target == this.player) {
                return;
            }
            TAGGED.put(target.getUUID(), new Tagged(this, this.player.server.getTickCount() + this.memoryTicks));
            if (this.hits.add(target.getUUID()) && this.hitPath != null && this.hits.size() >= this.hitGoal) {
                award(this.player, this.hitPath);
            }
        }

        private void killed(LivingEntity target) {
            if (this.killPath == null || !this.killFilter.test(target)) {
                return;
            }
            this.kills++;
            if (this.kills >= this.killGoal) {
                award(this.player, this.killPath);
            }
        }
    }

    private record Tagged(Cast cast, long until) {
    }

    private static final class Window {
        private final long until;
        private final int goal;
        private final String path;
        private int kills;

        private Window(long until, int goal, String path) {
            this.until = until;
            this.goal = goal;
            this.path = path;
        }
    }

    private static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        Cast cast = CURRENT.get();
        if (cast != null) {
            cast.hit(target);
        }
        if (event.getSource().getEntity() instanceof SwarmSquirrel squirrel && squirrel.getOwner() instanceof ServerPlayer owner
                && target.getType().is(Tags.EntityTypes.BOSSES)) {
            cast(owner).memory(600).kills(entity -> true, "street_level/squirrel_girl/unbeatable").hit(target);
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        Tagged tagged = TAGGED.remove(target.getUUID());
        Entity killer = event.getSource().getEntity();
        if (tagged != null && target.getServer() != null && target.getServer().getTickCount() <= tagged.until()
                && (killer == null || killer == tagged.cast().player || killer instanceof SwarmSquirrel)) {
            tagged.cast().killed(target);
            if (killer == null) {
                killer = tagged.cast().player;
            }
        }
        if (!(killer instanceof ServerPlayer player) || player == target) {
            return;
        }
        List<Window> windows = WINDOWS.get(player.getUUID());
        if (windows != null) {
            long now = player.server.getTickCount();
            for (Window window : windows) {
                if (now <= window.until && ++window.kills >= window.goal) {
                    award(player, window.path);
                }
            }
        }
        if (isPower(player, InvisibleWomanPower.ID) && InvisibleWomanPower.isInvisible(player)
                && target instanceof Mob mob && mob.getTarget() != player) {
            award(player, "fantastic_four/invisible_woman/peekaboo");
        }
        if (isPower(player, DaredevilPower.ID) && (player.hasEffect(MobEffects.BLINDNESS) || player.hasEffect(MobEffects.DARKNESS)
                || player.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(player.getEyePosition())) == 0
                && player.level().getBrightness(LightLayer.SKY, BlockPos.containing(player.getEyePosition())) == 0)) {
            award(player, "street_level/daredevil/blind_faith");
        }
        if (isPower(player, "weapon_x_logan") && event.getSource().getDirectEntity() == player
                && HeroPowers.data(player).hasState("claws_active")) {
            count(player, "x_men/wolverine/best_there_is", 1.0D, 250.0D);
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getExplosion().getDirectSourceEntity() instanceof Creeper creeper)) {
            return;
        }
        for (Player player : event.getLevel().players()) {
            if (player instanceof ServerPlayer server && isPower(server, "spider_powers_peter_parker")
                    && server.distanceToSqr(creeper) < 64.0D && !event.getAffectedEntities().contains(server)) {
                award(server, "street_level/spider_man/tingle");
            }
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().getTickCount();
        if (now % 100 != 0) {
            return;
        }
        TAGGED.values().removeIf(tagged -> now > tagged.until());
        Iterator<Map.Entry<UUID, List<Window>>> iterator = WINDOWS.entrySet().iterator();
        while (iterator.hasNext()) {
            List<Window> windows = iterator.next().getValue();
            windows.removeIf(window -> now > window.until);
            if (windows.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (isPower(player, "spider_powers_peter_parker")) {
            tickSpider(player);
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        tickPowers(player);
        tickIronMan(player);
    }

    private static void tickPowers(ServerPlayer player) {
        if (!player.hasData(AttachmentsRegistry.PLAYER_DATA.get())) {
            return;
        }
        String power = HeroPowers.data(player).getPrimaryPower();
        if (power == null || "none".equals(power) || !LEGACY_POWERS.contains(power) && HeroPowers.get(power) == null) {
            return;
        }
        String branch = BRANCHES.get(power);
        if (branch != null) {
            award(player, branch.substring(0, branch.indexOf('/')) + "/root");
            award(player, branch);
        }
        CompoundTag data = data(player);
        String last = data.getString(LAST_POWER);
        if (!last.isEmpty() && !last.equals(power)) {
            award(player, "avengers/identity_crisis");
        }
        data.putString(LAST_POWER, power);
        CompoundTag powers = data.getCompound(POWERS);
        powers.putBoolean(power, true);
        data.put(POWERS, powers);
        if (powers.size() >= 10) {
            award(player, "avengers/power_sampler");
        }
        boolean all = LEGACY_POWERS.stream().allMatch(powers::contains);
        for (HeroPower hero : HeroPowers.all()) {
            all &= powers.contains(hero.id());
        }
        if (all) {
            award(player, "avengers/who_needs_a_team");
        }
    }

    private static void tickIronMan(ServerPlayer player) {
        Set<String> marks = new HashSet<>();
        for (ItemStack stack : player.getInventory().items) {
            addMark(stack, marks);
        }
        for (ItemStack stack : player.getInventory().armor) {
            addMark(stack, marks);
        }
        if (!marks.isEmpty()) {
            award(player, "avengers/iron_man/mark_ii_prototype");
        }
        if (marks.size() >= 10) {
            award(player, "avengers/iron_man/house_party_protocol");
        }
        if (IronManSuits.fullSuitId(player) == null) {
            return;
        }
        award(player, "avengers/root");
        award(player, "avengers/iron_man");
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!player.onGround() && !player.isInWater() && IronManEnergy.hasEnergy(chest) && IronManEnergy.getFE(chest) <= 0.0D
                && player.level().getBlockState(player.blockPosition().below(2)).isAir()) {
            award(player, "avengers/iron_man/running_on_fumes");
        }
    }

    private static void addMark(ItemStack stack, Set<String> marks) {
        if (!stack.isEmpty()) {
            String id = IronManSuits.suitId(stack.getItem());
            if (id != null) {
                marks.add(id);
            }
        }
    }

    private static void tickSpider(ServerPlayer player) {
        boolean swinging = !player.onGround() && !player.level().getEntitiesOfClass(
                com.stxllarplanes.marvellegacy.entity.projectile.WebShot.class, player.getBoundingBox().inflate(64.0D),
                web -> web.getHolder() == player && web.isInGround() && !web.isFinished()).isEmpty();
        travel(player, "street_level/spider_man/thwip", swinging, 1000.0D);
        PlayerData data = HeroPowers.data(player);
        boolean ceiling = data.hasState("WallClimbCeiling")
                || !player.onGround() && !player.getAbilities().flying && Math.abs(player.getDeltaMovement().y) < 0.01D
                && !player.level().getBlockState(BlockPos.containing(player.getX(), player.getBoundingBox().maxY + 0.1D, player.getZ()))
                .getCollisionShape(player.level(), BlockPos.containing(player.getX(), player.getBoundingBox().maxY + 0.1D, player.getZ())).isEmpty();
        int ticks = ceiling ? CEILING_TICKS.getOrDefault(player.getUUID(), 0) + 1 : 0;
        CEILING_TICKS.put(player.getUUID(), ticks);
        if (ticks >= 40) {
            award(player, "street_level/spider_man/ceiling_fan");
        }
    }
}
