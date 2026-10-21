package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.MarvelBalanceConfig;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class ViltrumiteGuards {
    public static final String GRAB_TAG = "ViltrumiteGrabbed";
    public static final String SONIC_BOOM_SHADER = "viltrumiteflight:shaders/post/sonic_boom.json";
    public static final TagKey<MobEffect> BYPASS_EFFECTS = TagKey.create(Registries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "viltrumite_bypass"));
    public static final int METEORS = 0;
    public static final int BLEEDING = 1;
    public static final int CLAPS = 2;
    private static final long[] LAST_RUN = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
    private static final ThreadLocal<int[]> MOVE_DEPTH = ThreadLocal.withInitial(() -> new int[1]);
    private static volatile Set<Entity> clientGrabbed = Set.of();

    private ViltrumiteGuards() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ViltrumiteGuards::onPlayerTick);
    }

    private static boolean on(ModConfigSpec.BooleanValue value) {
        return !MarvelBalanceConfig.SPEC.isLoaded() || value.get();
    }

    public static boolean flight() {
        return on(MarvelBalanceConfig.VILTRUMITE_GUARD_FLIGHT);
    }

    public static boolean teleports() {
        return on(MarvelBalanceConfig.VILTRUMITE_GUARD_TELEPORTS);
    }

    public static boolean damage() {
        return on(MarvelBalanceConfig.VILTRUMITE_GUARD_DAMAGE);
    }

    public static boolean effects() {
        return on(MarvelBalanceConfig.VILTRUMITE_GUARD_EFFECTS);
    }

    public static boolean visuals() {
        return on(MarvelBalanceConfig.VILTRUMITE_GUARD_VISUALS);
    }

    public static boolean performance() {
        return on(MarvelBalanceConfig.VILTRUMITE_GUARD_PERFORMANCE);
    }

    public static ViltrumiteFlightLedger ledger(Abilities abilities) {
        return (ViltrumiteFlightLedger) abilities;
    }

    public static boolean takeOwnedFlight(Player player) {
        ViltrumiteFlightLedger ledger = ledger(player.getAbilities());
        if (!ledger.integratedSuperheroes$viltrumiteOwned()) {
            return false;
        }
        ledger.integratedSuperheroes$setViltrumiteOwned(false);
        return true;
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        ViltrumiteFlightLedger ledger = ledger(player.getAbilities());
        if (!ledger.integratedSuperheroes$hasGrabSave() || player.getTags().contains(GRAB_TAG)) {
            return;
        }
        boolean mayfly = ledger.integratedSuperheroes$grabSavedMayfly();
        boolean flying = mayfly && ledger.integratedSuperheroes$grabSavedFlying();
        ledger.integratedSuperheroes$clearGrabSave();
        if (ViltrumiteBridge.isViltrumite(player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        Abilities abilities = player.getAbilities();
        if (abilities.mayfly != mayfly || abilities.flying != flying) {
            abilities.mayfly = mayfly;
            abilities.flying = flying;
            player.onUpdateAbilities();
        }
    }

    public static boolean bypassesReduction(DamageSource source) {
        return damage() && (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.BYPASSES_RESISTANCE));
    }

    public static boolean bypassesBlock(DamageSource source) {
        return damage() && (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.BYPASSES_SHIELD));
    }

    public static boolean bypassesEffectImmunity(MobEffectInstance effect) {
        return effects() && effect.getEffect().is(BYPASS_EFFECTS);
    }

    public static void enterMove() {
        MOVE_DEPTH.get()[0]++;
    }

    public static void exitMove() {
        int[] depth = MOVE_DEPTH.get();
        if (depth[0] > 0) {
            depth[0]--;
        }
    }

    public static boolean handlingMove() {
        return MOVE_DEPTH.get()[0] > 0;
    }

    public static boolean firstThisTick(int job, ServerLevel level) {
        if (!performance()) {
            return true;
        }
        long tick = level.getServer().getTickCount();
        if (LAST_RUN[job] == tick) {
            return false;
        }
        LAST_RUN[job] = tick;
        return true;
    }

    public static boolean mayBeGrabbed(Entity entity) {
        if (!performance()) {
            return true;
        }
        if (entity instanceof PartEntity<?> part) {
            entity = part.getParent();
        }
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        return entity.level().isClientSide ? clientGrabbed.contains(entity) : entity.getTags().contains(GRAB_TAG);
    }

    public static void setClientGrabbed(Set<Entity> grabbed) {
        clientGrabbed = grabbed;
    }

    public static boolean styled(Entity entity) {
        return !visuals() || ViltrumiteBridge.isViltrumite(entity) || ViltrumiteBridge.inFlight(entity);
    }
}
