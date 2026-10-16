package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.api.RogueAbsorbEvent;
import com.craisinlord.integrated_superheroes.api.RogueAbsorption;
import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.init.AttachmentsRegistry;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class RoguePower {
    public static final String ID = "absorption_anna_marie";
    public static final HeroPower.Toggle GLOVES = new HeroPower.Toggle("integrated_superheroes_off_gloves", "Gloves", () -> Items.LEATHER);
    public static final int SLOTS = 3;
    private static final String[] MEMORY_ABILITIES = {"rogue_memory_1", "rogue_memory_2", "rogue_memory_3"};
    private static final String SAVE_KEY = "integrated_superheroes_rogue_memories";
    private static final String BORROWED = "borrowed/";
    private static final Map<UUID, Held[]> STATES = new HashMap<>();
    private static final Map<UUID, Long> ABSORB_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, java.util.Set<String>> BORROWED_STATES = new HashMap<>();

    private RoguePower() {
    }

    private record Held(RogueMemory memory, String source, long expiresAt) {
        String label(long now) {
            long seconds = Math.max(0L, (expiresAt - now) / 20L);
            String time = seconds >= 60 ? (seconds + 59) / 60 + "m" : Math.max(5L, (seconds + 4) / 5 * 5) + "s";
            String ability = memory.hasActive() ? memory.abilityName() : "Passive";
            return source + ": " + ability + " §7(" + time + ")";
        }
    }

    public static HeroPower create(Supplier<Item> syringe) {
        List<HeroPower.Ability> abilities = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            int index = i;
            abilities.add(new HeroPower.Ability(MEMORY_ABILITIES[i], "Empty Memory", (player, slot) -> useMemory(player, index)));
        }
        abilities.add(new HeroPower.Ability("siphon_touch", "Siphon Touch", RoguePower::siphonTouch));
        abilities.add(HeroToggles.WHEEL);
        return new HeroPower(
                ID,
                "Absorption (Anna Marie)",
                syringe,
                MarvelBalanceConfig.ROGUE,
                List.of("agility"),
                List.copyOf(abilities),
                List.of(GLOVES),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        Held[] held = state(player);
                        RogueKit.tick(player);
                        long now = player.level().getGameTime();
                        boolean changed = false;
                        for (int i = 0; i < SLOTS; i++) {
                            Held memory = held[i];
                            if (memory == null) {
                                continue;
                            }
                            if (now >= memory.expiresAt() || i >= MarvelBalanceConfig.MEMORY_SLOTS.get()) {
                                forget(player, held, i, true);
                                changed = true;
                                continue;
                            }
                            memory.memory().passive().tick(player);
                        }
                        if (changed || player.tickCount % 20 == 0) {
                            relabel(player, held, changed);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        Held[] held = STATES.remove(player.getUUID());
                        if (held != null) {
                            for (Held memory : held) {
                                if (memory != null) {
                                    memory.memory().passive().end(player);
                                }
                            }
                        }
                        player.getPersistentData().remove(SAVE_KEY);
                        clearBorrowedStates(player);
                        RogueKit.clear(player);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL) && RogueKit.slamming(player)) {
                            event.setCanceled(true);
                            return;
                        }
                        for (Held memory : state(player)) {
                            if (memory != null && !event.isCanceled()) {
                                memory.memory().passive().incomingDamage(player, event);
                            }
                        }
                        if (event.isCanceled() || HeroToggles.isOn(player, GLOVES)) {
                            return;
                        }
                        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker == event.getSource().getEntity()
                                && attacker != player && RogueKit.affects(player, attacker)) {
                            involuntaryDrain(player, attacker);
                        }
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        for (Held memory : state(player)) {
                            if (memory != null) {
                                memory.memory().passive().outgoingDamage(player, event);
                            }
                        }
                        if (HeroToggles.isOn(player, GLOVES) || HeroAbilities.isAbilityDamage()
                                || !event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().getDirectEntity() != player
                                || !player.getMainHandItem().isEmpty()) {
                            return;
                        }
                        LivingEntity target = event.getEntity();
                        if (!RogueKit.affects(player, target)) {
                            return;
                        }
                        event.setAmount(event.getAmount() + MarvelBalanceConfig.DRAIN_DAMAGE.get().floatValue());
                        absorb(player, target);
                    }
                }
        );
    }

    public static void registerEvents() {
        NeoForge.EVENT_BUS.addListener(RoguePower::onLogout);
        NeoForge.EVENT_BUS.addListener(RoguePower::onClone);
    }

    public static boolean isRogue(Player player) {
        return player.hasData(AttachmentsRegistry.PLAYER_DATA.get()) && ID.equals(HeroPowers.data(player).getPrimaryPower());
    }

    public static List<RogueMemory> memories(ServerPlayer player) {
        List<RogueMemory> list = new ArrayList<>();
        for (Held held : state(player)) {
            if (held != null) {
                list.add(held.memory());
            }
        }
        return list;
    }

    private static Held[] state(ServerPlayer player) {
        Held[] held = STATES.get(player.getUUID());
        if (held == null) {
            held = load(player);
            STATES.put(player.getUUID(), held);
        }
        return held;
    }

    private static void relabel(ServerPlayer player, Held[] held, boolean force) {
        long now = player.level().getGameTime();
        boolean dirty = force;
        int unlocked = MarvelBalanceConfig.MEMORY_SLOTS.get();
        for (int i = 0; i < SLOTS; i++) {
            Power power = HeroAbilities.slotPower(player, MEMORY_ABILITIES[i]);
            if (power == null) {
                continue;
            }
            String label = i >= unlocked ? "§8Locked Memory" : held[i] == null ? "Empty Memory" : held[i].label(now);
            if (!label.equals(power.getDisplayName())) {
                power.setDisplayName(label);
                dirty = true;
            }
        }
        if (dirty) {
            PlayerData.sync(player);
            save(player, held);
        }
    }

    private static void forget(ServerPlayer player, Held[] held, int index, boolean expired) {
        Held memory = held[index];
        held[index] = null;
        if (memory == null) {
            return;
        }
        memory.memory().passive().end(player);
        if (expired) {
            player.displayClientMessage(Component.literal("§7The memory of " + memory.source() + " fades."), true);
        }
    }

    public static boolean grant(ServerPlayer player, RogueMemory memory, String source, double seconds) {
        if (!isRogue(player)) {
            return false;
        }
        Held[] held = state(player);
        long now = player.level().getGameTime();
        long expires = now + (long) (seconds * 20.0D);
        int unlocked = MarvelBalanceConfig.MEMORY_SLOTS.get();
        int slot = -1;
        for (int i = 0; i < unlocked; i++) {
            if (held[i] != null && held[i].memory().id().equals(memory.id())) {
                slot = i;
                break;
            }
        }
        if (slot >= 0) {
            held[slot] = new Held(memory, source, Math.max(expires, held[slot].expiresAt()));
        } else {
            for (int i = 0; i < unlocked; i++) {
                if (held[i] == null) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) {
                slot = 0;
                for (int i = 1; i < unlocked; i++) {
                    if (held[i].expiresAt() < held[slot].expiresAt()) {
                        slot = i;
                    }
                }
                forget(player, held, slot, false);
            }
            held[slot] = new Held(memory, source, expires);
            Power power = HeroAbilities.slotPower(player, MEMORY_ABILITIES[slot]);
            if (power != null) {
                power.setCooldown(0.0D);
            }
        }
        relabel(player, held, true);
        return true;
    }

    private static boolean onCooldown(ServerPlayer player, LivingEntity target) {
        long now = player.level().getGameTime();
        Long until = ABSORB_COOLDOWNS.get(target.getUUID());
        if (until != null && until > now) {
            return true;
        }
        ABSORB_COOLDOWNS.put(target.getUUID(), now + 10L);
        return false;
    }

    public static void absorb(ServerPlayer player, LivingEntity target) {
        if (onCooldown(player, target)) {
            return;
        }
        drainFx(player, target);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, MarvelBalanceConfig.DRAIN_WEAKEN_TICKS.get(), 1), player);
        if (HeroAbilities.canControl(target)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MarvelBalanceConfig.DRAIN_WEAKEN_TICKS.get(), 0), player);
        }
        player.heal(MarvelBalanceConfig.DRAIN_HEAL.get().floatValue());
        if (target instanceof ServerPlayer victim) {
            absorbPlayer(player, victim);
            return;
        }
        boolean boss = target.getType().is(Tags.EntityTypes.BOSSES);
        RogueMemory memory = RogueAbsorption.memoryFor(target).orElseGet(RogueMemories::vigor);
        double seconds = boss ? MarvelBalanceConfig.BOSS_MEMORY_SECONDS.get() : MarvelBalanceConfig.MEMORY_SECONDS.get();
        RogueAbsorbEvent event = NeoForge.EVENT_BUS.post(new RogueAbsorbEvent(player, target, memory, seconds));
        if (event.isCanceled() || event.getMemory() == null) {
            return;
        }
        String source = target.getType().getDescription().getString();
        grant(player, event.getMemory(), source, event.getSeconds());
        if (boss) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0), player);
        }
        RogueMemory granted = event.getMemory();
        player.displayClientMessage(Component.literal("§aAbsorbed " + source + "§a: "
                + (granted.hasActive() ? granted.abilityName() : "Passive")), true);
    }

    private static void absorbPlayer(ServerPlayer rogue, ServerPlayer victim) {
        if (!MarvelBalanceConfig.ABSORB_PLAYERS.get() || !rogue.canHarmPlayer(victim)) {
            return;
        }
        double seconds = MarvelBalanceConfig.BORROW_SECONDS.get();
        List<RogueMemory> borrowed = borrowedMemories(victim);
        String source = victim.getGameProfile().getName();
        if (borrowed.isEmpty()) {
            grant(rogue, RogueMemories.vigor(), source, seconds);
        } else {
            Held[] held = state(rogue);
            for (int i = 0; i < SLOTS; i++) {
                forget(rogue, held, i, false);
            }
            for (RogueMemory memory : borrowed.subList(0, Math.min(borrowed.size(), MarvelBalanceConfig.MEMORY_SLOTS.get()))) {
                grant(rogue, memory, source, seconds);
            }
        }
        shutdown(victim, MarvelBalanceConfig.VICTIM_SHUTDOWN_SECONDS.get());
        rogue.displayClientMessage(Component.literal("§aYou borrowed " + source + "'s powers."), true);
        victim.displayClientMessage(Component.literal("§2Rogue drained your powers!"), true);
        HeroFx.play(victim, SoundEvents.WARDEN_HEARTBEAT, 1.2F, 1.0F);
    }

    private static void shutdown(ServerPlayer victim, double seconds) {
        if (seconds <= 0.0D) {
            return;
        }
        PlayerData data = HeroPowers.data(victim);
        boolean changed = false;
        for (Power power : data.getActivePowers()) {
            if (power != null && !"none".equals(power.getName()) && power.getCooldown() < seconds) {
                power.setCooldown(seconds);
                changed = true;
            }
        }
        if (changed) {
            PlayerData.sync(victim);
        }
        victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (int) (seconds * 20.0D), 0), victim);
    }

    private static List<RogueMemory> borrowedMemories(ServerPlayer victim) {
        List<RogueMemory> list = new ArrayList<>();
        if (isRogue(victim)) {
            for (Held held : state(victim)) {
                if (held != null) {
                    list.add(held.memory());
                }
            }
            return list;
        }
        String primary = HeroPowers.data(victim).getPrimaryPower();
        HeroPower hero = HeroPowers.get(primary);
        if (hero != null) {
            for (HeroPower.Ability ability : hero.abilities()) {
                RogueMemory memory = borrowed(ability.name());
                if (memory != null) {
                    list.add(memory);
                }
            }
            return list;
        }
        RogueMemory legacy = RogueMemories.legacy(primary);
        if (legacy != null) {
            list.add(legacy);
        }
        return list;
    }

    private static RogueMemory borrowed(String abilityName) {
        if (HeroToggles.WHEEL_ABILITY.equals(abilityName) || HeroWheels.get(abilityName) != null) {
            return null;
        }
        for (HeroPower hero : HeroPowers.all()) {
            if (ID.equals(hero.id())) {
                continue;
            }
            for (HeroPower.Ability ability : hero.abilities()) {
                if (ability.name().equals(abilityName)) {
                    RogueMemory memory = RogueMemory.builder(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "rogue/" + BORROWED + abilityName),
                                    ability.displayName())
                            .active(ability.displayName(), player -> useBorrowed(player, ability))
                            .passive(new RogueMemory.Passive() {
                                @Override
                                public void end(ServerPlayer player) {
                                    clearBorrowedStates(player);
                                }
                            })
                            .build();
                    RogueAbsorption.remember(memory);
                    return memory;
                }
            }
        }
        return null;
    }

    private static double useBorrowed(ServerPlayer player, HeroPower.Ability ability) {
        PlayerData data = HeroPowers.data(player);
        java.util.Set<String> before = new java.util.HashSet<>(data.getStates());
        double cooldown;
        try {
            cooldown = ability.handler().press(player, 0);
        } catch (RuntimeException exception) {
            player.displayClientMessage(Component.literal("§7You can't make " + ability.displayName() + " work."), true);
            return 1.0D;
        }
        for (String state : data.getStates()) {
            if (!before.contains(state)) {
                BORROWED_STATES.computeIfAbsent(player.getUUID(), ignored -> new java.util.HashSet<>()).add(state);
            }
        }
        return cooldown;
    }

    private static void clearBorrowedStates(ServerPlayer player) {
        java.util.Set<String> states = BORROWED_STATES.remove(player.getUUID());
        if (states == null || states.isEmpty()) {
            return;
        }
        PlayerData data = HeroPowers.data(player);
        states.forEach(data::removeState);
        PlayerData.sync(player);
    }

    private static double useMemory(ServerPlayer player, int index) {
        Held held = state(player)[index];
        if (held == null) {
            player.displayClientMessage(Component.literal("§7Absorb something first. Take your gloves off and punch it."), true);
            return 0.0D;
        }
        if (!held.memory().hasActive()) {
            player.displayClientMessage(Component.literal("§7" + held.source() + "'s memory is passive."), true);
            return 0.0D;
        }
        double cooldown = held.memory().active().use(player);
        Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.5D)).subtract(0.0D, 0.3D, 0.0D);
        player.serverLevel().sendParticles(HeroAbilities.emissive(RogueKit.GREEN, 0.7F), hand.x, hand.y, hand.z, 6, 0.1D, 0.1D, 0.1D, 0.0D);
        return cooldown * MarvelBalanceConfig.MEMORY_COOLDOWN_MULTIPLIER.get();
    }

    private static double siphonTouch(ServerPlayer player, int slot) {
        double range = MarvelBalanceConfig.SIPHON_RANGE.get();
        LivingEntity target = HeroAbilities.cone(player, range, 30.0D).stream()
                .filter(entity -> RogueKit.affects(player, entity))
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .orElse(null);
        if (target == null) {
            player.displayClientMessage(Component.literal("§7Nothing in reach."), true);
            return 0.5D;
        }
        Vec3 toward = target.position().subtract(player.position());
        if (toward.lengthSqr() > 4.0D) {
            player.setDeltaMovement(toward.normalize().scale(0.6D).add(0.0D, 0.1D, 0.0D));
            player.hurtMarked = true;
        }
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), MarvelBalanceConfig.SIPHON_DAMAGE.get().floatValue()));
        ABSORB_COOLDOWNS.remove(target.getUUID());
        absorb(player, target);
        HeroAnimations.play(player, "uppercut");
        return MarvelBalanceConfig.SIPHON_COOLDOWN.get();
    }

    private static void involuntaryDrain(ServerPlayer player, LivingEntity attacker) {
        float damage = MarvelBalanceConfig.INVOLUNTARY_DRAIN_DAMAGE.get().floatValue();
        if (damage > 0.0F) {
            HeroAbilities.runAsAbilityDamage(() -> attacker.hurt(player.damageSources().indirectMagic(player, player), damage));
        }
        attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0), player);
        drainFx(player, attacker);
    }

    private static void drainFx(ServerPlayer player, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        Vec3 from = target.getBoundingBox().getCenter();
        Vec3 to = player.getBoundingBox().getCenter();
        HeroAbilities.particleLine(level, from, to, HeroAbilities.emissive(RogueKit.GREEN, 0.8F), 0.35D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, from.x, from.y, from.z, 6, 0.3D, 0.4D, 0.3D, 0.02D);
        level.sendParticles(HeroAbilities.emissive(RogueKit.GREEN, 1.0F), to.x, to.y, to.z, 10, 0.3D, 0.5D, 0.3D, 0.0D);
        HeroFx.play(target, SoundEvents.SOUL_ESCAPE.value(), 1.2F, 0.8F);
        HeroFx.play(player, SoundEvents.SCULK_CLICKING, 0.8F, 1.4F);
    }

    private static void save(ServerPlayer player, Held[] held) {
        long now = player.level().getGameTime();
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            if (held[i] == null) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            entry.putString("Id", held[i].memory().id().toString());
            entry.putString("Source", held[i].source());
            entry.putLong("Remaining", Math.max(0L, held[i].expiresAt() - now));
            list.add(entry);
        }
        player.getPersistentData().put(SAVE_KEY, list);
    }

    private static Held[] load(ServerPlayer player) {
        Held[] held = new Held[SLOTS];
        if (!player.getPersistentData().contains(SAVE_KEY, Tag.TAG_LIST)) {
            return held;
        }
        long now = player.level().getGameTime();
        ListTag list = player.getPersistentData().getList(SAVE_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Id"));
            if (slot < 0 || slot >= SLOTS || id == null) {
                continue;
            }
            RogueMemory memory = RogueAbsorption.byId(id).orElse(null);
            if (memory == null && id.getPath().startsWith("rogue/" + BORROWED)) {
                memory = borrowed(id.getPath().substring(("rogue/" + BORROWED).length()));
            }
            if (memory != null) {
                held[slot] = new Held(memory, entry.getString("Source"), now + entry.getLong("Remaining"));
            }
        }
        return held;
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Held[] held = STATES.remove(player.getUUID());
            if (held != null) {
                save(player, held);
            }
            ABSORB_COOLDOWNS.remove(player.getUUID());
        }
    }

    private static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player.getUUID());
            if (event.isWasDeath()) {
                player.getPersistentData().remove(SAVE_KEY);
            } else if (event.getOriginal().getPersistentData().contains(SAVE_KEY)) {
                player.getPersistentData().put(SAVE_KEY, event.getOriginal().getPersistentData().get(SAVE_KEY).copy());
            }
        }
    }

    public static void tickServer(MinecraftServer server) {
        if (server.getTickCount() % 200 == 0 && !ABSORB_COOLDOWNS.isEmpty()) {
            long now = server.overworld().getGameTime();
            ABSORB_COOLDOWNS.values().removeIf(until -> until < now);
        }
    }
}
