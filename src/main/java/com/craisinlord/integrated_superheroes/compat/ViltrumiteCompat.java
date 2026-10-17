package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.api.RogueAbsorption;
import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.craisinlord.integrated_superheroes.hero.BinaryPower;
import com.craisinlord.integrated_superheroes.hero.HeroAdvancements;
import com.craisinlord.integrated_superheroes.hero.HeroPower;
import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import com.craisinlord.integrated_superheroes.hero.MarvelBalanceConfig;
import com.craisinlord.integrated_superheroes.hero.RogueKit;
import com.craisinlord.integrated_superheroes.hero.RoguePower;
import com.craisinlord.integrated_superheroes.hero.ViltrumitePower;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ViltrumiteCompat {
    private static final Set<String> SQUAD = Set.of("Lucan", "Vidor", "Thula");
    private static final Set<UUID> BORROWED = new HashSet<>();
    private static final long ACTIVE_WINDOW_MILLIS = 2000L;
    private static final String SCALED_TAG = "integrated_superheroes_viltrumite_scaled";
    private static volatile long activeUntil;
    private static RogueMemory memory;

    private ViltrumiteCompat() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ViltrumiteCompat::onServerStarting);
        NeoForge.EVENT_BUS.addListener(ViltrumiteCompat::onLogin);
        NeoForge.EVENT_BUS.addListener(ViltrumiteCompat::onLogout);
        NeoForge.EVENT_BUS.addListener(ViltrumiteCompat::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ViltrumiteCompat::onAnyPlayerTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingIncomingDamageEvent.class, ViltrumiteCompat::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(ViltrumiteCompat::onDeath);
    }

    public static boolean mayUseFlight(Player player) {
        if (player == null) {
            return false;
        }
        if (ViltrumiteBridge.isViltrumite(player) || ViltrumiteBridge.inFlight(player)) {
            return true;
        }
        return MarvelBalanceConfig.VILTRUMITE_CAPTAIN_MARVEL_FLIGHT.get() && BinaryPower.ID.equals(HeroPowers.data(player).getPrimaryPower());
    }

    public static RogueMemory memory() {
        return memory;
    }

    public static void registerRogue() {
        memory = RogueKit.memory("viltrumite", "Viltrumite")
                .active("Viltrumite Rush", RogueKit.dash(2.6D, 8, MarvelBalanceConfig.VILTRUMITE_ROGUE_DASH_DAMAGE.get(), 2.0D,
                        ParticleTypes.CLOUD, SoundEvents.PLAYER_ATTACK_KNOCKBACK, MarvelBalanceConfig.VILTRUMITE_ROGUE_DASH_COOLDOWN.get()))
                .passive(new RogueMemory.Passive() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (!ViltrumiteBridge.isViltrumite(player)) {
                            ViltrumiteBridge.grant(player);
                            BORROWED.add(player.getUUID());
                        }
                        if (player.tickCount % 20 == 0) {
                            ViltrumiteBridge.applyStats(player,
                                    MarvelBalanceConfig.VILTRUMITE_ROGUE_BASE_DAMAGE.get().floatValue(),
                                    MarvelBalanceConfig.VILTRUMITE_ROGUE_DAMAGE_REDUCTION.get().floatValue(),
                                    MarvelBalanceConfig.VILTRUMITE_DAMAGE_IGNORE.get().floatValue(),
                                    MarvelBalanceConfig.VILTRUMITE_HEAL_FACTOR.get().floatValue(),
                                    MarvelBalanceConfig.VILTRUMITE_MAX_FLIGHT_SPEED.get().floatValue(),
                                    MarvelBalanceConfig.VILTRUMITE_THROTTLE.get().floatValue());
                            award(player, "viltrumite/borrowed_blood");
                        }
                    }

                    @Override
                    public void end(ServerPlayer player) {
                        if (BORROWED.remove(player.getUUID())) {
                            ViltrumiteBridge.revoke(player);
                        }
                    }
                })
                .build();
        RogueAbsorption.registerMatcher(ViltrumiteBridge::isNpc, memory);
    }

    private static boolean isSerumViltrumite(ServerPlayer player) {
        HeroPower hero = HeroPowers.activeHero(player);
        return hero != null && ViltrumitePower.ID.equals(hero.id());
    }

    private static boolean borrowing(ServerPlayer player) {
        return memory != null && RoguePower.isRogue(player) && RoguePower.memories(player).contains(memory);
    }

    public static void configure() {
        ViltrumiteBridge.configure(MarvelBalanceConfig.VILTRUMITE_SYRINGE_ONLY.get(), MarvelBalanceConfig.VILTRUMITE_TAKEOFF_BREAKS_BLOCKS.get(),
                MarvelBalanceConfig.VILTRUMITE_HUMANS_BLEED.get());
    }

    private static void onServerStarting(ServerAboutToStartEvent event) {
        configure();
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        configure();
        if (event.getEntity() instanceof ServerPlayer player && !ViltrumiteBridge.isNpc(player) && MarvelBalanceConfig.VILTRUMITE_SYRINGE_ONLY.get()) {
            ViltrumiteBridge.markChosen(player);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && BORROWED.remove(player.getUUID())) {
            ViltrumiteBridge.revoke(player);
        }
    }

    public static boolean active() {
        return System.currentTimeMillis() < activeUntil;
    }

    private static void onAnyPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if ((player.tickCount & 7) == 0 && ViltrumiteBridge.isViltrumite(player)) {
            activeUntil = System.currentTimeMillis() + ACTIVE_WINDOW_MILLIS;
            if (!player.level().isClientSide && ViltrumiteBridge.isNpc(player) && !player.getTags().contains(SCALED_TAG)) {
                player.addTag(SCALED_TAG);
                ViltrumiteBridge.scaleBaseDamage(player, MarvelBalanceConfig.VILTRUMITE_NPC_DAMAGE_MULTIPLIER.get().floatValue());
                ViltrumiteBridge.capReduction(player, MarvelBalanceConfig.VILTRUMITE_NPC_MAX_REDUCTION.get().floatValue());
            }
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0 || ViltrumiteBridge.isNpc(player)
                || !ViltrumiteBridge.isViltrumite(player) || borrowing(player)) {
            return;
        }
        if (MarvelBalanceConfig.VILTRUMITE_SYRINGE_ONLY.get() && !isSerumViltrumite(player)) {
            ViltrumiteBridge.revoke(player);
            return;
        }
        ViltrumitePower.applyStats(player);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (ViltrumiteBridge.isNpc(event.getEntity())) {
            ViltrumiteBridge.capReduction(event.getEntity(), MarvelBalanceConfig.VILTRUMITE_NPC_MAX_REDUCTION.get().floatValue());
        }
        if (ViltrumiteBridge.isNpc(event.getSource().getEntity())) {
            float cap = MarvelBalanceConfig.VILTRUMITE_NPC_PER_HIT_CAP.get().floatValue();
            if (event.getAmount() > cap) {
                event.setAmount(cap);
            }
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !ViltrumiteBridge.isViltrumite(dead)) {
            return;
        }
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer player && !ViltrumiteBridge.isNpc(player) ? player : null;
        if (ViltrumiteBridge.isNpc(dead)) {
            int drops = MarvelBalanceConfig.VILTRUMITE_NPC_BLOOD_DROPS.get();
            if (drops > 0) {
                ItemEntity item = new ItemEntity(level, dead.getX(), dead.getY() + 0.5D, dead.getZ(), ViltrumiteBridge.bloodSample(drops));
                item.setDefaultPickUpDelay();
                level.addFreshEntity(item);
            }
            if (killer != null) {
                String name = dead.getName().getString();
                if ("Conquest".equals(name)) {
                    award(killer, "viltrumite/conquest");
                } else if (SQUAD.contains(name)) {
                    award(killer, "viltrumite/squad");
                }
            }
        }
        if (killer != null && ViltrumiteBridge.hasScourge(dead)) {
            award(killer, "viltrumite/scourge");
        }
    }

    private static void award(ServerPlayer player, String path) {
        HeroAdvancements.award(player, "viltrumite/root");
        HeroAdvancements.award(player, path);
    }
}
