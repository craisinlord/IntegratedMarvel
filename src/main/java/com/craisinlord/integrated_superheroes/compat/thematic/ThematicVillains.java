package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.content.encounter.EncounterDefinition;
import bond.thematic.api.content.encounter.EncounterLauncher;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.registry.GauntletContent;
import com.craisinlord.integrated_superheroes.villain.VillainConfig;
import com.craisinlord.integrated_superheroes.villain.VillainFx;
import com.craisinlord.integrated_superheroes.villain.VillainItems;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.BossEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class ThematicVillains {
    static final String ULTRON = "thematic:villain_ultron";
    static final String BLACK_ADAM = "thematic:villain_black_adam";
    static final String REVERSE_FLASH = "thematic:villain_reverse_flash";
    static final String JOKER = "thematic:villain_joker";
    private static final String PENGUIN_BOSS = "thematic:penguin_boss";
    private static final String TWO_FACE_BOSS = "thematic:twoface_boss";
    private static final String SENTINEL = "thematic:rogue_ultron_sentinel";
    private static final String PRIME = "thematic:rogue_ultron_sentinel_prime";
    private static final String ESCAPEE = "thematic:kryptonian_criminal";
    private static final String MANHUNTER = "thematic:manhunter";
    private static final String CRADLE = "thematic:ultron_cradle";
    private static final ResourceLocation STREET = ResourceLocation.parse("thematic:sentinel");
    private static final String ULTRON_CHIP = "thematic:ultron_chip";
    private static final String LIGHTNING_SHARD = "thematic:lightning_shard";
    private static final String JOKER_VENOM = "thematic:joker_venom";
    private static final String FEAR_SHARD = "thematic:fear_shard";
    private static final String ARKILLO = "thematic:arkillo";
    private static final String CREW_COOLDOWN = "integrated_superheroes_crew_raid_next";
    private static final String KEY = "integrated_superheroes_living_villain";
    private static final String KEY_ANCHOR = "Anchor";
    private static final String KEY_INTRO = "Intro";
    private static final String KEY_WAVES = "Waves";
    private static final String KEY_STONE = "Stone";
    private static final String KEY_SUMMONER = "Summoner";
    private static final String KEY_CRADLE = "Cradle";
    private static final String MINION_TAG = "integrated_superheroes_ultron_minion";
    private static final String PLAYER_COOLDOWN = "integrated_superheroes_living_villain_next";
    private static final String BLACK_ADAM_COOLDOWN = "integrated_superheroes_black_adam_next";
    private static final String ESCAPEE_COOLDOWN = "integrated_superheroes_escapee_next";
    private static final TagKey<Biome> DESERTS = TagKey.create(Registries.BIOME, ResourceLocation.parse("c:is_desert"));
    private static final int CHECK_TICKS = 600;
    private static final Map<Entity, Tracked> TRACKED = new WeakHashMap<>();

    private ThematicVillains() {
    }

    private static final class Tracked {
        final Mob mob;
        final String type;
        final ServerBossEvent bar;
        final ArrayDeque<long[]> hits = new ArrayDeque<>();
        int lonelyTicks;

        Tracked(Mob mob, String type) {
            this.mob = mob;
            this.type = type;
            this.bar = new ServerBossEvent(mob.getDisplayName(), color(type), BossEvent.BossBarOverlay.NOTCHED_10);
            this.bar.setDarkenScreen(ULTRON.equals(type));
        }
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ThematicVillains::onJoin);
        NeoForge.EVENT_BUS.addListener(ThematicVillains::onLeave);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ThematicVillains::onDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ThematicVillains::onDeath);
        NeoForge.EVENT_BUS.addListener(ThematicVillains::onDrops);
        NeoForge.EVENT_BUS.addListener(ThematicVillains::onLevelTick);
        NeoForge.EVENT_BUS.addListener(ThematicVillains::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ThematicVillains::onServerStopped);
        NeoForge.EVENT_BUS.addListener(ThematicRaidCommand::onRegister);
        NeoForge.EVENT_BUS.addListener(ThematicRock::onRightClickBlock);
    }

    private static boolean on() {
        return VillainConfig.SPEC.isLoaded() && VillainConfig.THEMATIC_VILLAINS.get();
    }

    static String typeId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    private static boolean living(String type) {
        return ULTRON.equals(type) || BLACK_ADAM.equals(type) || REVERSE_FLASH.equals(type) || JOKER.equals(type);
    }

    private static BossEvent.BossBarColor color(String type) {
        return BLACK_ADAM.equals(type) ? BossEvent.BossBarColor.YELLOW : JOKER.equals(type) ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED;
    }

    private static String key(String type) {
        return ULTRON.equals(type) ? "ultron" : BLACK_ADAM.equals(type) ? "black_adam" : JOKER.equals(type) ? "joker" : "reverse_flash";
    }

    private static CompoundTag data(Entity entity) {
        CompoundTag root = entity.getPersistentData();
        if (!root.contains(KEY, Tag.TAG_COMPOUND)) {
            root.put(KEY, new CompoundTag());
        }
        return root.getCompound(KEY);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !on() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        String type = typeId(mob);
        if (!living(type) || TRACKED.containsKey(mob)) {
            return;
        }
        CompoundTag data = data(mob);
        if (!data.contains(KEY_ANCHOR)) {
            data.putLong(KEY_ANCHOR, mob.blockPosition().asLong());
        }
        TRACKED.put(mob, new Tracked(mob, type));
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        Tracked tracked = TRACKED.remove(entity);
        if (tracked != null) {
            tracked.bar.removeAllPlayers();
        }
        if (!(event.getLevel() instanceof ServerLevel level) || !ULTRON.equals(typeId(entity))) {
            return;
        }
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason == null || !reason.shouldDestroy()) {
            return;
        }
        CompoundTag data = data(entity);
        releaseCradle(level, data);
        returnStone(level, entity, data);
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        TRACKED.clear();
    }

    private static void onDamage(LivingDamageEvent.Pre event) {
        if (!on() || event.getEntity().level().isClientSide()) {
            return;
        }
        Tracked tracked = TRACKED.get(event.getEntity());
        if (tracked == null) {
            return;
        }
        long now = event.getEntity().level().getGameTime();
        while (!tracked.hits.isEmpty() && now - tracked.hits.peekFirst()[0] > 100L) {
            tracked.hits.pollFirst();
        }
        float amount = Math.min(event.getNewDamage(), VillainConfig.THEMATIC_PER_HIT_CAP.get().floatValue());
        float sum = 0.0F;
        for (long[] hit : tracked.hits) {
            sum += Float.intBitsToFloat((int) hit[1]);
        }
        float budget = (float) (dpsCap(tracked) * 5.0D);
        if (sum + amount > budget) {
            float over = sum + amount - budget;
            float allowed = Math.max(0.0F, amount - over);
            amount = allowed + (amount - allowed) * 0.1F;
        }
        tracked.hits.addLast(new long[]{now, Float.floatToIntBits(amount)});
        event.setNewDamage(amount);
    }

    private static double dpsCap(Tracked tracked) {
        boolean headliner = ULTRON.equals(tracked.type) || BLACK_ADAM.equals(tracked.type);
        double cap = headliner ? VillainConfig.THEMATIC_HEADLINER_DPS_CAP.get() : VillainConfig.THEMATIC_DPS_CAP.get();
        int nearby = tracked.mob.level().getEntitiesOfClass(Player.class, tracked.mob.getBoundingBox().inflate(32.0D),
                player -> player.isAlive() && !player.isSpectator()).size();
        return cap * (1.0D + VillainConfig.THEMATIC_CAP_PER_EXTRA_PLAYER.get() * Math.max(0, nearby - 1));
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !on() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity entity = event.getEntity();
        String type = typeId(entity);
        if (ULTRON.equals(type)) {
            CompoundTag data = data(entity);
            ItemStack stone = takeStone(level, data);
            if (!stone.isEmpty()) {
                ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY(0.5D), entity.getZ(), stone);
                item.setUnlimitedLifetime();
                item.setDefaultPickUpDelay();
                level.addFreshEntity(item);
            }
            releaseCradle(level, data);
            for (Entity minion : level.getEntities((Entity) null, entity.getBoundingBox().inflate(64.0D), e -> e.getTags().contains(MINION_TAG))) {
                minion.discard();
            }
        } else if (PRIME.equals(type) && event.getSource().getEntity() instanceof Player) {
            double chance = VillainConfig.ULTRON_AFTER_PRIME_CHANCE.get();
            if (chance > 0.0D && level.random.nextDouble() < chance) {
                BlockPos at = entity.blockPosition();
                VillainWorld.later(level, 200, () -> {
                    if (level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(at).inflate(96.0D), e -> ULTRON.equals(typeId(e))).isEmpty()) {
                        spawn(level, ULTRON, Vec3.atBottomCenterOf(at), null);
                    }
                });
            }
        }
    }

    private static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity entity = event.getEntity();
        String type = typeId(entity);
        boolean byPlayer = event.isRecentlyHit() || event.getSource().getEntity() instanceof Player;
        if (ModCompat.GIGERESQUE && byPlayer && VillainConfig.SPEC.isLoaded()) {
            alienBlood(level, entity, type, event);
        }
        if (!on()) {
            return;
        }
        ItemStack drop = ItemStack.EMPTY;
        if (ULTRON.equals(type)) {
            Item chip = BuiltInRegistries.ITEM.get(ResourceLocation.parse(ULTRON_CHIP));
            if (chip != net.minecraft.world.item.Items.AIR) {
                drop = new ItemStack(chip);
            }
            trophies(level, entity, event);
        } else if (BLACK_ADAM.equals(type)) {
            drop = new ItemStack(VillainItems.BLACK_ADAM_SCARAB.get());
        } else if (REVERSE_FLASH.equals(type)) {
            Item shard = BuiltInRegistries.ITEM.get(ResourceLocation.parse(LIGHTNING_SHARD));
            if (shard != net.minecraft.world.item.Items.AIR) {
                drop = new ItemStack(shard, 2 + level.random.nextInt(2));
            }
        } else if (JOKER.equals(type)) {
            Item venom = BuiltInRegistries.ITEM.get(ResourceLocation.parse(JOKER_VENOM));
            if (venom != net.minecraft.world.item.Items.AIR) {
                drop = new ItemStack(venom, 2 + level.random.nextInt(2));
            }
        } else if (ESCAPEE.equals(type) && byPlayer && level.random.nextDouble() < VillainConfig.KRYPTONIAN_BLOOD_CHANCE.get()) {
            drop = new ItemStack(VillainItems.KRYPTONIAN_BLOOD.get());
        } else if (VillainConfig.FEAR_SHARD_DROPS.get() && (ARKILLO.equals(type) || byPlayer && entity.getType() == EntityType.WARDEN)) {
            Item shard = BuiltInRegistries.ITEM.get(ResourceLocation.parse(FEAR_SHARD));
            if (shard != net.minecraft.world.item.Items.AIR) {
                drop = new ItemStack(shard, ARKILLO.equals(type) ? 3 + level.random.nextInt(3) : 2 + level.random.nextInt(3));
            }
        } else if (MANHUNTER.equals(type) && byPlayer && level.random.nextDouble() < VillainConfig.MARTIAN_BLOOD_CHANCE.get()) {
            drop = new ItemStack(VillainItems.MARTIAN_BLOOD.get());
        }
        if (!drop.isEmpty()) {
            ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY(0.5D), entity.getZ(), drop);
            item.setDefaultPickUpDelay();
            event.getDrops().add(item);
        }
    }

    private static void alienBlood(ServerLevel level, LivingEntity entity, String type, LivingDropsEvent event) {
        int count = 0;
        if (VillainConfig.ALIEN_BLOOD_SURE_MOBS.get().contains(type)) {
            count = 1 + level.random.nextInt(2);
        } else if (VillainConfig.ALIEN_BLOOD_MOBS.get().contains(type) && level.random.nextDouble() < VillainConfig.ALIEN_BLOOD_CHANCE.get()) {
            count = 1;
        }
        if (count > 0) {
            ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY(0.5D), entity.getZ(), new ItemStack(VillainItems.ALIEN_BLOOD.get(), count));
            item.setDefaultPickUpDelay();
            event.getDrops().add(item);
        }
    }

    private static void trophies(ServerLevel level, LivingEntity entity, LivingDropsEvent event) {
        List<Item> posters = new ArrayList<>();
        Item cover = net.minecraft.world.item.Items.AIR;
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!"thematic".equals(id.getNamespace())) {
                continue;
            }
            if (id.getPath().equals("cover_ultron_unlimited")) {
                cover = item;
            } else if (id.getPath().startsWith("poster_")) {
                posters.add(item);
            }
        }
        List<ItemStack> stacks = new ArrayList<>();
        if (cover != net.minecraft.world.item.Items.AIR) {
            stacks.add(new ItemStack(cover));
        }
        if (!posters.isEmpty()) {
            stacks.add(new ItemStack(posters.get(level.random.nextInt(posters.size()))));
        }
        for (ItemStack stack : stacks) {
            ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY(0.5D), entity.getZ(), stack);
            item.setDefaultPickUpDelay();
            event.getDrops().add(item);
        }
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !on()) {
            return;
        }
        long now = level.getGameTime();
        for (Tracked tracked : new ArrayList<>(TRACKED.values())) {
            if (tracked.mob.level() == level && tracked.mob.isAlive()) {
                tickVillain(level, tracked, now);
            }
        }
        if (now % 20L != 0L) {
            return;
        }
        boolean overworld = level.dimension() == Level.OVERWORLD;
        double caveChance = overworld ? 0.0D : caveEscapeeChance(level);
        if (!overworld && caveChance <= 0.0D) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if ((now + player.getId() * 20L) % CHECK_TICKS == 0L && player.isAlive() && !player.isSpectator() && !player.isCreative()) {
                if (overworld) {
                    checkSpawns(level, player);
                } else {
                    checkCaveEscapee(level, player, caveChance);
                }
            }
        }
    }

    private static double caveEscapeeChance(ServerLevel level) {
        if (!VillainConfig.ESCAPEE_SPAWNS.get()) {
            return 0.0D;
        }
        String id = level.dimension().location().toString();
        for (String entry : VillainConfig.ESCAPEE_CAVE_DIMENSIONS.get()) {
            int split = entry.lastIndexOf('=');
            if (split > 0 && entry.substring(0, split).trim().equals(id)) {
                try {
                    return Double.parseDouble(entry.substring(split + 1).trim());
                } catch (NumberFormatException ignored) {
                    return 0.0D;
                }
            }
        }
        return 0.0D;
    }

    private static void checkCaveEscapee(ServerLevel level, ServerPlayer player, double chance) {
        if (!roll(level, chance) || !ready(player, ESCAPEE_COOLDOWN) || typeNear(level, player.position(), ESCAPEE, 128.0D)) {
            return;
        }
        Vec3 at = floorSpot(level, player.position(), 12.0D, 24.0D);
        if (at != null && spawn(level, ESCAPEE, at, null) != null) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.5D, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 0.6F);
            cooldown(player, ESCAPEE_COOLDOWN, VillainConfig.ESCAPEE_COOLDOWN_MINUTES.get());
        }
    }

    private static Vec3 floorSpot(ServerLevel level, Vec3 around, double min, double max) {
        int baseY = Mth.floor(around.y);
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = min + level.random.nextDouble() * (max - min);
            int x = Mth.floor(around.x + Math.cos(angle) * distance);
            int z = Mth.floor(around.z + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, baseY, z))) {
                continue;
            }
            for (int dy = 6; dy >= -10; dy--) {
                BlockPos pos = new BlockPos(x, baseY + dy, z);
                if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                        && level.getBlockState(pos.below()).getFluidState().isEmpty()
                        && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                        && level.getBlockState(pos.above(2)).isAir()) {
                    return Vec3.atBottomCenterOf(pos);
                }
            }
        }
        return null;
    }

    private static void tickVillain(ServerLevel level, Tracked tracked, long now) {
        Mob mob = tracked.mob;
        tracked.bar.setProgress(Mth.clamp(mob.getHealth() / mob.getMaxHealth(), 0.0F, 1.0F));
        if (now % 10L != 0L) {
            return;
        }
        List<ServerPlayer> near = VillainFx.near(level, mob.position(), 48.0D);
        for (ServerPlayer player : new ArrayList<>(tracked.bar.getPlayers())) {
            if (!near.contains(player)) {
                tracked.bar.removePlayer(player);
            }
        }
        near.forEach(tracked.bar::addPlayer);
        CompoundTag data = data(mob);
        if (!data.getBoolean(KEY_INTRO) && mob.getTarget() instanceof Player) {
            data.putBoolean(KEY_INTRO, true);
            String key = key(tracked.type);
            VillainFx.title(level, mob.position(), 48.0D,
                    Component.translatable("integrated_superheroes.villains." + key + ".title").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.translatable("integrated_superheroes.villains." + key + ".subtitle").withStyle(ChatFormatting.GRAY));
        }
        if (ULTRON.equals(tracked.type)) {
            summonSentinels(level, mob, data);
        }
        if (now % 20L == 0L) {
            BlockPos anchor = BlockPos.of(data.getLong(KEY_ANCHOR));
            double radius = VillainConfig.THEMATIC_LEASH_RADIUS.get();
            if (mob.position().distanceToSqr(Vec3.atBottomCenterOf(anchor)) > radius * radius) {
                VillainFx.smoke(level, mob.position().add(0.0D, 1.0D, 0.0D), 0.6D);
                mob.getNavigation().stop();
                mob.teleportTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
                mob.setDeltaMovement(Vec3.ZERO);
            }
            tracked.lonelyTicks = near.isEmpty() && level.getNearestPlayer(mob, 128.0D) == null ? tracked.lonelyTicks + 20 : 0;
            if (tracked.lonelyTicks >= 2400) {
                mob.discard();
            }
        }
    }

    private static void summonSentinels(ServerLevel level, Mob ultron, CompoundTag data) {
        int waves = data.getInt(KEY_WAVES);
        float health = ultron.getHealth() / ultron.getMaxHealth();
        int due = health <= 0.33F ? 2 : health <= 0.66F ? 1 : 0;
        if (waves >= due) {
            return;
        }
        data.putInt(KEY_WAVES, due);
        int count = due + 1;
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            Vec3 at = ultron.position().add(Math.cos(angle) * 4.0D, 1.0D, Math.sin(angle) * 4.0D);
            Mob sentinel = spawn(level, SENTINEL, at, ultron.getTarget());
            if (sentinel != null) {
                sentinel.addTag(MINION_TAG);
                VillainFx.sparks(level, at.add(0.0D, 1.0D, 0.0D), 12);
            }
        }
        level.playSound(null, ultron.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.5F, 0.6F);
    }

    static Mob spawn(ServerLevel level, String id, Vec3 at, LivingEntity target) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElse(null);
        if (type == null || !(type.create(level) instanceof Mob mob)) {
            return null;
        }
        mob.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        if (!level.addFreshEntity(mob)) {
            return null;
        }
        if (target != null && target.isAlive()) {
            mob.setTarget(target);
        }
        return mob;
    }

    private static void checkSpawns(ServerLevel level, ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        boolean outdoors = level.canSeeSky(pos.above());
        if (outdoors && level.isThundering() && roll(level, VillainConfig.REVERSE_FLASH_CHANCE.get()) && ready(player)) {
            if (livingNear(level, player.position(), REVERSE_FLASH, 256.0D) == null) {
                Vec3 at = surfaceSpot(level, player.position(), 16.0D, 24.0D);
                if (at != null) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                    if (bolt != null) {
                        bolt.moveTo(at);
                        bolt.setVisualOnly(true);
                        level.addFreshEntity(bolt);
                    }
                    if (spawn(level, REVERSE_FLASH, at, player) != null) {
                        cooldown(player);
                    }
                }
            }
            return;
        }
        if (outdoors && level.getBiome(pos).is(DESERTS) || outdoors && level.getBiome(pos).is(Biomes.DESERT)) {
            if (roll(level, VillainConfig.BLACK_ADAM_CHANCE.get()) && ready(player, BLACK_ADAM_COOLDOWN) && livingNear(level, player.position(), BLACK_ADAM, 256.0D) == null) {
                Vec3 at = surfaceSpot(level, player.position(), 24.0D, 40.0D);
                if (at != null && spawn(level, BLACK_ADAM, at, player) != null) {
                    cooldown(player, BLACK_ADAM_COOLDOWN, VillainConfig.BLACK_ADAM_COOLDOWN_MINUTES.get());
                    return;
                }
            }
        }
        if (outdoors && level.isNight() && !level.isRaining() && VillainConfig.ESCAPEE_SPAWNS.get()
                && roll(level, VillainConfig.ESCAPEE_CHANCE.get()) && ready(player, ESCAPEE_COOLDOWN) && !typeNear(level, player.position(), ESCAPEE, 128.0D)) {
            Vec3 at = surfaceSpot(level, player.position(), 20.0D, 32.0D);
            if (at != null && spawn(level, ESCAPEE, at, null) != null) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.5D, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 0.6F);
                cooldown(player, ESCAPEE_COOLDOWN, VillainConfig.ESCAPEE_COOLDOWN_MINUTES.get());
                return;
            }
        }
        if (!level.isNight()) {
            return;
        }
        if (inStreet(level, pos) && jokerUnlocked(player) && ready(player) && roll(level, VillainConfig.JOKER_CHANCE.get())
                && livingNear(level, player.position(), JOKER, 256.0D) == null) {
            Vec3 at = surfaceSpot(level, player.position(), 10.0D, 18.0D);
            if (at != null && spawn(level, JOKER, at, player) != null) {
                for (int i = 0; i < 3; i++) {
                    Vec3 near = surfaceSpot(level, at, 2.0D, 5.0D);
                    if (near != null) {
                        spawn(level, i == 0 ? "thematic:criminal_strong_crowbar" : "thematic:criminal_strong_pistol", near, player);
                    }
                }
                level.playSound(null, BlockPos.containing(at), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 2.0F, 0.6F);
                cooldown(player);
                return;
            }
        }
        if (!crewReady(player)) {
            return;
        }
        if (inStreet(level, pos) && roll(level, VillainConfig.STREET_CREW_CHANCE.get()) || level.isVillage(pos) && roll(level, VillainConfig.VILLAGE_CREW_CHANCE.get())) {
            if (startCrew(level, player, null)) {
                player.getPersistentData().putLong(CREW_COOLDOWN, System.currentTimeMillis() + 30L * 60000L);
            }
        }
    }

    private static boolean roll(ServerLevel level, double chance) {
        return chance > 0.0D && level.random.nextDouble() < chance;
    }

    private static boolean ready(ServerPlayer player) {
        return ready(player, PLAYER_COOLDOWN);
    }

    private static boolean ready(ServerPlayer player, String key) {
        return System.currentTimeMillis() >= player.getPersistentData().getLong(key);
    }

    private static void cooldown(ServerPlayer player, String key, int minutes) {
        player.getPersistentData().putLong(key, System.currentTimeMillis() + minutes * 60000L);
    }

    private static boolean crewReady(ServerPlayer player) {
        return System.currentTimeMillis() >= player.getPersistentData().getLong(CREW_COOLDOWN);
    }

    private static void cooldown(ServerPlayer player) {
        player.getPersistentData().putLong(PLAYER_COOLDOWN, System.currentTimeMillis() + VillainConfig.LIVING_VILLAIN_COOLDOWN_MINUTES.get() * 60000L);
    }

    private static Entity livingNear(ServerLevel level, Vec3 at, String type, double radius) {
        for (Tracked tracked : TRACKED.values()) {
            if (tracked.type.equals(type) && tracked.mob.level() == level && tracked.mob.position().distanceToSqr(at) <= radius * radius) {
                return tracked.mob;
            }
        }
        return null;
    }

    private static boolean typeNear(ServerLevel level, Vec3 at, String type, double radius) {
        return !level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(at, at).inflate(radius),
                mob -> mob.isAlive() && type.equals(typeId(mob))).isEmpty();
    }

    private static Vec3 surfaceSpot(ServerLevel level, Vec3 around, double min, double max) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = min + level.random.nextDouble() * (max - min);
            int x = Mth.floor(around.x + Math.cos(angle) * distance);
            int z = Mth.floor(around.z + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos.below()).getFluidState().isEmpty() && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
                return Vec3.atBottomCenterOf(pos);
            }
        }
        return null;
    }

    private static boolean jokerUnlocked(ServerPlayer player) {
        return killed(player, PENGUIN_BOSS) && killed(player, TWO_FACE_BOSS);
    }

    private static boolean killed(ServerPlayer player, String id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElse(null);
        return type != null && player.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(type)) > 0;
    }

    private static boolean inStreet(ServerLevel level, BlockPos pos) {
        Structure street = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(STREET);
        return street != null && level.structureManager().getStructureWithPieceAt(pos, street).isValid();
    }

    public static boolean startCrew(ServerLevel level, ServerPlayer player, String crew) {
        List<ServerPlayer> party = new ArrayList<>(level.getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
                && p.distanceToSqr(player) <= 32.0D * 32.0D));
        party.remove(player);
        party.add(0, player);
        String pick = crew != null ? crew : level.random.nextBoolean() ? "penguin" : "two_face";
        String name = "village_" + pick;
        try {
            EncounterLauncher.launch(level.getServer(), name, party, EncounterDefinition.Anchor.of(level, player.blockPosition()), false);
            return true;
        } catch (RuntimeException | LinkageError error) {
            IntegratedSuperheroes.LOGGER.warn("Could not start the {} raid", name, error);
            return false;
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!on() || !VillainConfig.ULTRON_CRADLE.get()) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (!held.is(GauntletContent.MIND_STONE.get())) {
            return;
        }
        BlockPos pos = event.getPos();
        Level world = event.getLevel();
        if (!CRADLE.equals(BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).toString())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(world instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Cradles cradles = Cradles.get(level);
        if (cradles.busy.contains(pos.asLong()) && level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(pos).inflate(VillainConfig.THEMATIC_LEASH_RADIUS.get() + 32.0D), e -> ULTRON.equals(typeId(e))).isEmpty()) {
            cradles.busy.remove(pos.asLong());
            cradles.setDirty();
        }
        if (cradles.busy.contains(pos.asLong())) {
            VillainFx.line(player, "integrated_superheroes.villains.ultron.busy", ChatFormatting.GRAY);
            return;
        }
        if (NeoForge.EVENT_BUS.post(new com.craisinlord.integrated_superheroes.api.UltronSummonEvent(player, pos.immutable())).isCanceled()) {
            return;
        }
        ItemStack stone = held.copyWithCount(1);
        Mob ultron = spawn(level, ULTRON, Vec3.atBottomCenterOf(pos.above()), player);
        if (ultron == null) {
            return;
        }
        held.shrink(1);
        CompoundTag data = data(ultron);
        data.put(KEY_STONE, stone.save(level.registryAccess()));
        data.putUUID(KEY_SUMMONER, player.getUUID());
        data.putLong(KEY_CRADLE, pos.asLong());
        data.putLong(KEY_ANCHOR, pos.above().asLong());
        cradles.busy.add(pos.asLong());
        cradles.setDirty();
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.5F, 0.5F);
        VillainFx.sparks(level, Vec3.atCenterOf(pos.above()), 20);
        player.displayClientMessage(Component.translatable("integrated_superheroes.villains.ultron.summon").withStyle(ChatFormatting.GRAY), false);
    }

    private static ItemStack takeStone(ServerLevel level, CompoundTag data) {
        if (!data.contains(KEY_STONE, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        ItemStack stone = ItemStack.parseOptional(level.registryAccess(), data.getCompound(KEY_STONE));
        data.remove(KEY_STONE);
        return stone;
    }

    private static void returnStone(ServerLevel level, Entity ultron, CompoundTag data) {
        ItemStack stone = takeStone(level, data);
        if (stone.isEmpty()) {
            return;
        }
        ServerPlayer summoner = data.hasUUID(KEY_SUMMONER) ? level.getServer().getPlayerList().getPlayer(data.getUUID(KEY_SUMMONER)) : null;
        if (summoner != null && summoner.isAlive()) {
            if (!summoner.getInventory().add(stone)) {
                summoner.drop(stone, false);
            }
            VillainFx.line(summoner, "integrated_superheroes.villains.ultron.returned", ChatFormatting.GRAY);
            return;
        }
        BlockPos at = data.contains(KEY_CRADLE) ? BlockPos.of(data.getLong(KEY_CRADLE)).above() : ultron.blockPosition();
        ItemEntity item = new ItemEntity(level, at.getX() + 0.5D, at.getY() + 0.2D, at.getZ() + 0.5D, stone);
        item.setUnlimitedLifetime();
        level.addFreshEntity(item);
    }

    private static void releaseCradle(ServerLevel level, CompoundTag data) {
        if (data.contains(KEY_CRADLE)) {
            Cradles cradles = Cradles.get(level);
            if (cradles.busy.remove(data.getLong(KEY_CRADLE))) {
                cradles.setDirty();
            }
        }
    }

    static final class Cradles extends SavedData {
        private static final String NAME = "integrated_superheroes_ultron_cradles";
        final Set<Long> busy = new HashSet<>();

        static Cradles get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Cradles::new, Cradles::load, null), NAME);
        }

        private static Cradles load(CompoundTag tag, HolderLookup.Provider registries) {
            Cradles cradles = new Cradles();
            for (long pos : tag.getLongArray("Busy")) {
                cradles.busy.add(pos);
            }
            return cradles;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.put("Busy", new LongArrayTag(busy.stream().mapToLong(Long::longValue).toArray()));
            return tag;
        }
    }
}
