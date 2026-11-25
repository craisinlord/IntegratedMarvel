package com.craisinlord.integrated_superheroes.compat.thematic;

import com.craisinlord.integrated_superheroes.hero.ServerState;
import bond.thematic.api.ability.ThematicAbility;
import bond.thematic.api.ability.corp.CorpsType;
import bond.thematic.api.content.ability.part.MeterPart;
import bond.thematic.api.content.ability.system.MeterSystem;
import bond.thematic.api.content.action.effect.MeterActions;
import bond.thematic.api.content.flight.FlightTrait;
import bond.thematic.api.content.legacy.api.PowerChargeQueries;
import bond.thematic.api.registries.armors.armor.CharacterItem;
import bond.thematic.api.util.ThematicHelper;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

final class LanternCharge {
    private static final int FLIGHT_COST = 6;
    private static final int FLIGHT_CUT_BELOW = 50;
    private static final int WARN = 400;
    private static final int CRITICAL = 100;
    private static final int GREEN_STILL_REGEN = 15;
    private static final int GREEN_KILL_REFUND = 60;
    private static final int YELLOW_PER_AFRAID = 10;
    private static final int YELLOW_MAX = 40;
    private static final int YELLOW_KILL_REFUND = 150;
    private static final int LEGACY_REGEN = 4;
    private static final int RECHARGE_BONUS = 28;
    private static final int BEAM_HELD_COST = 30;
    private static final int SHIELD_COST_PER_DAMAGE = 8;
    private static final int OATH_COOLDOWN_TICKS = 12000;
    private static final int OATH_RANGE = 5;
    private static final String OATH_TAG = "integrated_superheroes_lantern_oath";
    private static final int OATH_FIELD_REFILL = 1000;
    private static final int RING_MAX = 2000;
    private static final int RAGE_PER_DAMAGE_TAKEN = 3;
    private static final int RAGE_PER_DAMAGE_DEALT = 2;
    private static final int RAGE_IDLE_DRAIN = 20;
    private static final int RAGE_IDLE_TICKS = 160;
    private static final int HOPE_NEAR = 20;
    private static final int HOPE_GREEN_BOOST = 30;
    private static final int COMPASSION_PER_HURT = 15;
    private static final int COMPASSION_MAX = 45;
    private static final int LOVE_NEAR = 25;
    private static final int LOVE_BREED = 100;
    private static final int DEATH_KILL = 80;
    private static final int LIFE_KILL = 40;
    private static final int LIFE_NEAR = 10;
    private static final float ORANGE_DISARM_CHANCE = 0.15F;

    private static final TagKey<Item> CONSTRUCTS = itemTag("lantern_constructs");
    private static final TagKey<Item> UPKEEP_TOOL = itemTag("construct_upkeep/tool");
    private static final TagKey<Item> UPKEEP_LIGHT = itemTag("construct_upkeep/light");
    private static final TagKey<Item> UPKEEP_HEAVY = itemTag("construct_upkeep/heavy");

    private static final Map<String, Integer> CORPS_MOVE_COSTS = Map.ofEntries(
            Map.entry("beam", 60),
            Map.entry("minigun_sweep", 150),
            Map.entry("minigun", 150),
            Map.entry("airstrike", 200),
            Map.entry("surge", 120),
            Map.entry("shield", 150),
            Map.entry("missile", 100),
            Map.entry("large_throw", 150),
            Map.entry("train", 200),
            Map.entry("jet", 150),
            Map.entry("sandwiches", 150),
            Map.entry("spawn_entity_cave_spider", 150),
            Map.entry("bat_swarm", 150),
            Map.entry("unleash", 800),
            Map.entry("unleash_hold", 800),
            Map.entry("stare", 120),
            Map.entry("run", 800),
            Map.entry("blink", 80),
            Map.entry("crystal_prison", 150));
    private static final Map<String, String[]> EXACT_MOVE_COSTS = Map.of(
            "grab_construct", new String[]{null, "100"},
            "cut_the_sky_open", new String[]{"willpower", "800"},
            "embrace_fear", new String[]{"fear", "800"},
            "fear_incarnate", new String[]{"fear", "600"},
            "mine_mine_mine", new String[]{"avarice", "150"});
    private static final Map<String, String> OATHS = Map.of(
            "willpower", "in brightest day",
            "fear", "in blackest day",
            "rage", "with blood and rage",
            "hope", "in fearful day",
            "avarice", "what's mine is mine",
            "compassion", "tor lorek san",
            "love", "for hearts that are broken",
            "death", "rise",
            "life", "life");
    private static final Map<Item, Integer> TREASURE = Map.of(
            net.minecraft.world.item.Items.GOLD_NUGGET, 5,
            net.minecraft.world.item.Items.GOLD_INGOT, 40,
            net.minecraft.world.item.Items.EMERALD, 60,
            net.minecraft.world.item.Items.DIAMOND, 150,
            net.minecraft.world.item.Items.GOLD_BLOCK, 360);

    private static final Map<String, Optional<MoveCost>> MOVE_COSTS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<UUID, State> STATES = ServerState.map(new HashMap<>());
    private static final Map<String, Optional<Holder.Reference<MobEffect>>> EFFECTS = new java.util.concurrent.ConcurrentHashMap<>();
    private static boolean failed;
    private static boolean hooked;

    private LanternCharge() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(LanternCharge::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(LanternCharge::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(LanternCharge::onDeath);
        NeoForge.EVENT_BUS.addListener(LanternCharge::onShieldBlock);
        NeoForge.EVENT_BUS.addListener(LanternCharge::onChat);
        NeoForge.EVENT_BUS.addListener(LanternCharge::onLogout);
        NeoForge.EVENT_BUS.addListener(LanternCharge::onPickup);
        NeoForge.EVENT_BUS.addListener(LanternCharge::onBreed);
        NeoForge.EVENT_BUS.addListener(LanternCharge::onServerStarting);
    }

    private static void onServerStarting(ServerAboutToStartEvent event) {
        if (hooked) {
            return;
        }
        hooked = true;
        try {
        } catch (Throwable error) {
            IntegratedSuperheroes.LOGGER.warn("Couldn't hook Thematic flight for Lantern charge", error);
        }
    }

    private static final class State {
        private Vec3 lastPos;
        private long lastHurt;
        private long lastCombat;
        private int lastValue = -1;
        private boolean grounded;
        private final Map<String, Double> cooldowns = new HashMap<>();
        private final Map<UUID, Long> attackers = new HashMap<>();
    }

    private record Ring(CorpsType corps, String prefix, String key, MeterPart.Meter meter, MeterSystem.Store store) {
        int value() {
            return MeterSystem.value(store, meter, key);
        }

        void add(int amount) {
            if (amount != 0) {
                MeterSystem.add(store, meter, key, amount);
            }
        }

        boolean economy() {
            return ThematicOptions.on(ThematicOptions.LANTERN_CHARGE) && OATHS.containsKey(prefix);
        }
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("thematic", path));
    }

    private static Ring ring(Player player) {
        if (failed || player == null) {
            return null;
        }
        try {
            CorpsType corps = PowerChargeQueries.corpsOf(player);
            if (corps == null) {
                return null;
            }
            String prefix = corps.getPrefix();
            return ring(player, corps, prefix);
        } catch (Throwable error) {
            failed = true;
            IntegratedSuperheroes.LOGGER.warn("Lantern charge turned off: this Thematic version changed its ring charge API", error);
            return null;
        }
    }

    private static Ring ring(Player player, CorpsType corps, String prefix) {
        String key = prefix + "_ringCharge";
        MeterPart.Meter meter = MeterSystem.spec(key);
        MeterSystem.Store store = MeterActions.store(player);
        if (meter == null || store == null) {
            return null;
        }
        return new Ring(corps, prefix, key, meter, store);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Ring ring = ring(player);
        if (ring == null) {
            STATES.remove(player.getUUID());
            return;
        }
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        if (!ring.economy()) {
            if (player.tickCount % 20 == 0) {
                ring.add(LEGACY_REGEN);
            }
            return;
        }
        chargeMoves(player, ring, state);
        enforceFlightCut(player, ring, state);
        if (player.tickCount % 20 == 0) {
            secondTick(player, ring, state);
        }
    }

    private record MoveCost(String prefix, int price) {
    }

    private static void chargeMoves(ServerPlayer player, Ring ring, State state) {
        CharacterItem suit = ThematicHelper.getArmor(player);
        if (suit == null) {
            return;
        }
        List<ThematicAbility> abilities = suit.getAbilities(player);
        if (abilities == null) {
            return;
        }
        for (ThematicAbility ability : abilities) {
            String id = ability.getId();
            if (id == null) {
                continue;
            }
            MoveCost cost = moveCost(id).orElse(null);
            if (cost == null) {
                continue;
            }
            Ring payer = cost.prefix() == null || cost.prefix().equals(ring.prefix()) ? ring : ring(player, ring.corps(), cost.prefix());
            if (payer == null) {
                continue;
            }
            int price = cost.price();
            double now = ability.getCooldown(player);
            Double before = state.cooldowns.get(id);
            if (before != null && now > before + 2) {
                payer.add(-price);
            }
            if (payer.value() < price && now < 20) {
                ability.setCooldown(player, 20);
                now = ability.getCooldown(player);
            }
            state.cooldowns.put(id, now);
            if (player.tickCount % 20 == 0 && id.endsWith("_beam") && ability.isHeldDown(player)) {
                payer.add(-BEAM_HELD_COST);
            }
        }
    }

    private static Optional<MoveCost> moveCost(String id) {
        return MOVE_COSTS.computeIfAbsent(id, LanternCharge::findMoveCost);
    }

    private static Optional<MoveCost> findMoveCost(String id) {
        String[] exact = EXACT_MOVE_COSTS.get(id);
        if (exact != null) {
            return Optional.of(new MoveCost(exact[0], Integer.parseInt(exact[1])));
        }
        for (String corps : OATHS.keySet()) {
            if (id.startsWith(corps + "_")) {
                Integer cost = CORPS_MOVE_COSTS.get(id.substring(corps.length() + 1));
                return cost == null ? Optional.empty() : Optional.of(new MoveCost(corps, cost));
            }
        }
        return Optional.empty();
    }

    private static void enforceFlightCut(ServerPlayer player, Ring ring, State state) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        boolean cut = ring.value() < FLIGHT_CUT_BELOW;
        if (cut && player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
            if (!state.grounded) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false, true));
            }
        }
        state.grounded = cut;
    }

    private static void secondTick(ServerPlayer player, Ring ring, State state) {
        int spend = upkeep(player.getMainHandItem());
        if (!player.isCreative() && (FlightTrait.flying(player) || player.getAbilities().flying)) {
            spend += FLIGHT_COST;
        }
        int gain = fieldRegen(player, ring, state);
        if (ring.prefix().equals("willpower")) {
            player.removeEffect(MobEffects.DARKNESS);
            effect("thematic", "yellow_flashed").ifPresent(player::removeEffect);
        }
        if (ring.prefix().equals("hope")) {
            boostGreens(player);
        }
        if (hasEffect(player, "thematic", "recharge")) {
            gain += RECHARGE_BONUS;
        }
        ring.add(gain - spend);
        state.lastPos = player.position();
        int value = ring.value();
        if (state.lastValue >= 0) {
            if (state.lastValue >= WARN && value < WARN) {
                warn(player, "message.integrated_superheroes.lantern.low", 1.0F);
            } else if (state.lastValue >= CRITICAL && value < CRITICAL) {
                warn(player, "message.integrated_superheroes.lantern.critical", 1.4F);
            }
            if (state.lastValue > 0 && value <= 0) {
                shatter(player);
                warn(player, "message.integrated_superheroes.lantern.empty", 0.6F);
            }
        }
        state.lastValue = value;
        long now = player.level().getGameTime();
        state.attackers.values().removeIf(time -> now - time > 200);
    }

    private static int upkeep(ItemStack held) {
        if (held.isEmpty()) {
            return 0;
        }
        if (held.is(UPKEEP_HEAVY)) {
            return 6;
        }
        if (held.is(UPKEEP_LIGHT)) {
            return 4;
        }
        if (held.is(UPKEEP_TOOL)) {
            return 2;
        }
        return 0;
    }

    private static int fieldRegen(ServerPlayer player, Ring ring, State state) {
        long now = player.level().getGameTime();
        if (ring.prefix().equals("willpower")) {
            boolean still = state.lastPos != null && state.lastPos.distanceToSqr(player.position()) < 0.02;
            boolean calm = now - state.lastHurt > 60;
            return player.onGround() && calm && (still || player.isShiftKeyDown()) ? GREEN_STILL_REGEN : 0;
        }
        if (ring.prefix().equals("fear")) {
            AABB area = player.getBoundingBox().inflate(16);
            int afraid = player.level().getEntitiesOfClass(LivingEntity.class, area,
                    mob -> mob instanceof Enemy && mob.isAlive() && afraid(mob)).size();
            return Math.min(YELLOW_MAX, afraid * YELLOW_PER_AFRAID);
        }
        return switch (ring.prefix()) {
            case "rage" -> now - state.lastCombat > RAGE_IDLE_TICKS ? -RAGE_IDLE_DRAIN : 0;
            case "hope" -> companions(player, 24, false) > 0 ? HOPE_NEAR : 0;
            case "compassion" -> Math.min(COMPASSION_MAX, companions(player, 12, true) * COMPASSION_PER_HURT);
            case "love" -> {
                int near = companions(player, 16, false);
                int hurt = companions(player, 16, true);
                yield near == 0 ? 0 : hurt > 0 ? LOVE_NEAR * 2 : LOVE_NEAR;
            }
            case "life" -> {
                boolean still = state.lastPos != null && state.lastPos.distanceToSqr(player.position()) < 0.02;
                int field = (still && player.onGround() ? GREEN_STILL_REGEN / 2 : 0)
                        + (companions(player, 24, false) > 0 ? LIFE_NEAR : 0);
                yield field;
            }
            default -> 0;
        };
    }

    private static int companions(ServerPlayer player, double range, boolean hurtOnly) {
        AABB area = player.getBoundingBox().inflate(range);
        return player.level().getEntitiesOfClass(LivingEntity.class, area, other -> other != player && other.isAlive()
                && (other instanceof Player || other instanceof net.minecraft.world.entity.TamableAnimal tame && tame.isTame()
                || other instanceof net.minecraft.world.entity.npc.AbstractVillager)
                && (!hurtOnly || other.getHealth() < other.getMaxHealth())).size();
    }

    private static void boostGreens(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(24);
        for (Player other : player.level().getEntitiesOfClass(Player.class, area, other -> other != player)) {
            Ring green = ring(other);
            if (green != null && green.prefix().equals("willpower") && green.economy()) {
                green.add(HOPE_GREEN_BOOST);
            }
        }
    }

    private static Optional<Holder.Reference<MobEffect>> effect(String namespace, String path) {
        return EFFECTS.computeIfAbsent(namespace + ":" + path, id -> BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)));
    }

    private static boolean undead(LivingEntity entity) {
        return entity.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD);
    }

    private static boolean afraid(LivingEntity mob) {
        return mob.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || mob.hasEffect(MobEffects.DARKNESS)
                || hasEffect(mob, "thematic", "yellow_flashed");
    }

    private static boolean hasEffect(LivingEntity entity, String namespace, String path) {
        if (entity.getActiveEffectsMap().isEmpty()) {
            return false;
        }
        Optional<Holder.Reference<MobEffect>> effect = EFFECTS.computeIfAbsent(namespace + ":" + path,
                id -> BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)));
        return effect.isPresent() && entity.hasEffect(effect.get());
    }

    private static void shatter(ServerPlayer player) {
        var inventory = player.getInventory();
        boolean any = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(CONSTRUCTS)) {
                inventory.setItem(slot, ItemStack.EMPTY);
                any = true;
            }
        }
        if (any) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
    }

    private static void warn(ServerPlayer player, String key, float pitch) {
        player.displayClientMessage(Component.translatable(key), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7F, pitch);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        long now = victim.level().getGameTime();
        if (victim instanceof ServerPlayer player) {
            State state = STATES.get(player.getUUID());
            if (state != null) {
                state.lastHurt = now;
                if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                    state.attackers.put(attacker.getUUID(), now);
                }
            }
        }
        if (victim instanceof ServerPlayer hurt) {
            Ring ring = ring(hurt);
            if (ring != null && ring.economy() && ring.prefix().equals("rage")) {
                ring.add((int) Math.ceil(event.getAmount() * RAGE_PER_DAMAGE_TAKEN));
                combat(hurt, now);
            }
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer attacker) || attacker == victim) {
            return;
        }
        Ring ring = ring(attacker);
        if (ring == null || !ring.economy()) {
            return;
        }
        switch (ring.prefix()) {
            case "fear" -> {
                if (attacker.getMainHandItem().is(CONSTRUCTS)) {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0), attacker);
                }
            }
            case "rage" -> {
                ring.add((int) Math.ceil(event.getAmount() * RAGE_PER_DAMAGE_DEALT));
                combat(attacker, now);
                victim.igniteForSeconds(3);
                effect("thematic", "bleed").ifPresent(bleed -> victim.addEffect(new MobEffectInstance(bleed, 40, 0), attacker));
            }
            case "hope" -> attacker.getActiveEffects().stream()
                    .filter(active -> active.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)
                    .findFirst().ifPresent(active -> attacker.removeEffect(active.getEffect()));
            case "avarice" -> {
                if (victim instanceof net.minecraft.world.entity.Mob mob && !mob.getMainHandItem().isEmpty()
                        && attacker.getRandom().nextFloat() < ORANGE_DISARM_CHANCE) {
                    mob.spawnAtLocation(mob.getMainHandItem().copy());
                    mob.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
            case "compassion" -> victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), attacker);
            case "love" -> {
                if (victim instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == attacker) {
                    mob.setTarget(null);
                }
            }
            case "death" -> victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), attacker);
            case "life" -> {
                if (undead(victim)) {
                    event.setAmount(event.getAmount() * 2.0F);
                }
                attacker.heal(1.0F);
            }
            default -> {
            }
        }
    }

    private static void combat(ServerPlayer player, long now) {
        State state = STATES.get(player.getUUID());
        if (state != null) {
            state.lastCombat = now;
        }
    }

    private static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItemEntity().getItem();
        Integer worth = TREASURE.get(stack.getItem());
        if (worth == null) {
            return;
        }
        Ring ring = ring(player);
        if (ring == null || !ring.economy() || !ring.prefix().equals("avarice") || ring.value() >= RING_MAX) {
            return;
        }
        ring.add(worth * stack.getCount());
        event.getItemEntity().discard();
        event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.7F);
    }

    private static void onBreed(net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player) {
            Ring ring = ring(player);
            if (ring != null && ring.economy() && ring.prefix().equals("love")) {
                ring.add(LOVE_BREED);
            }
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Ring ring = ring(player);
        if (ring == null || !ring.economy()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (ring.prefix().equals("willpower")) {
            State state = STATES.get(player.getUUID());
            if (state != null && state.attackers.containsKey(victim.getUUID())) {
                ring.add(GREEN_KILL_REFUND);
            }
        } else if (ring.prefix().equals("fear") && afraid(victim)) {
            ring.add(YELLOW_KILL_REFUND);
        } else if (ring.prefix().equals("death") && !undead(victim)) {
            ring.add(DEATH_KILL);
        } else if (ring.prefix().equals("life") && !undead(victim)) {
            ring.add(LIFE_KILL);
        }
    }

    private static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getBlocked()) {
            return;
        }
        ItemStack shield = player.getUseItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(shield.getItem());
        if (!id.getNamespace().equals("thematic") || !id.getPath().startsWith("lantern_shield")) {
            return;
        }
        Ring ring = ring(player);
        if (ring == null || !ring.economy()) {
            return;
        }
        int cost = Math.max(1, (int) Math.ceil(event.getBlockedDamage() * SHIELD_COST_PER_DAMAGE));
        if (ring.value() < cost) {
            event.setBlocked(false);
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.lantern.shield_failed"), true);
            return;
        }
        ring.add(-cost);
        event.setShieldDamage(0);
    }

    private static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String text = event.getRawText().toLowerCase(Locale.ROOT);
        player.server.execute(() -> tryOath(player, text));
    }

    private static void tryOath(ServerPlayer player, String text) {
        Ring ring = ring(player);
        if (ring == null || !ring.economy()) {
            return;
        }
        String phrase = OATHS.get(ring.prefix());
        if (phrase == null || !text.contains(phrase)) {
            return;
        }
        Block battery = ring.corps().getBatteryBlock();
        if (battery != null && !near(player, battery)) {
            return;
        }
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong(OATH_TAG);
        if (last > 0 && now - last < OATH_COOLDOWN_TICKS && now >= last) {
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.lantern.oath_resting"), true);
            return;
        }
        player.getPersistentData().putLong(OATH_TAG, now);
        if (battery == null) {
            ring.add(OATH_FIELD_REFILL);
        } else {
            MeterSystem.refill(ring.store(), ring.meter(), ring.key());
        }
        BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("thematic", "recharge"))
                .ifPresent(effect -> player.addEffect(new MobEffectInstance(effect, 600, 0)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.integrated_superheroes.lantern.oath"), true);
    }

    private static boolean near(ServerPlayer player, Block battery) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-OATH_RANGE, -OATH_RANGE, -OATH_RANGE), center.offset(OATH_RANGE, OATH_RANGE, OATH_RANGE))) {
            if (level.getBlockState(pos).is(battery)) {
                return true;
            }
        }
        return false;
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }
}
