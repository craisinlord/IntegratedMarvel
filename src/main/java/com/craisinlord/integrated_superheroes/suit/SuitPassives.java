package com.craisinlord.integrated_superheroes.suit;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.compat.IceAndFireCharges;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.hero.IronManSuits;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SuitPassives {
    private static final ResourceKey<MobEffect> IRRADIATED = ResourceKey.create(Registries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath("alexscaves", "irradiated"));
    private static final String MK1 = "iron_man_mark_1";
    private static final String MIDAS = "iron_man_mark_21";
    private static final String SHADES = "iron_man_mark_23";
    private static final String JACK = "iron_man_mark_28";
    private static final String HAMMERHEAD = "iron_man_mark_37";
    private static final String WAR_MACHINE_2 = "war_machine_mark_2";

    private static final ResourceLocation SCGUNS_BLINDED = ResourceLocation.fromNamespaceAndPath("scguns", "blinded");
    private static final ResourceLocation SCGUNS_DEAFENED = ResourceLocation.fromNamespaceAndPath("scguns", "deafened");

    private SuitPassives() {
    }

    private static Map<String, List<Holder<MobEffect>>> immunities() {
        List<Holder<MobEffect>> midas = new ArrayList<>(List.of(MobEffects.MOVEMENT_SLOWDOWN));
        List<Holder<MobEffect>> hammerhead = new ArrayList<>();
        List<Holder<MobEffect>> jack = new ArrayList<>(List.of(MobEffects.WITHER, MobEffects.POISON));
        List<Holder<MobEffect>> warMachine = new ArrayList<>();
        HeroFx.effect("alexscaves:irradiated").ifPresent(irradiated -> {
            jack.add(0, irradiated);
            warMachine.add(irradiated);
        });
        if (ModCompat.ICE_AND_FIRE) {
            midas.add(IceAndFireCharges.frozen());
            hammerhead.add(IceAndFireCharges.sirenCharm());
        }
        return Map.of(
                MIDAS, midas,
                JACK, jack,
                HAMMERHEAD, hammerhead,
                WAR_MACHINE_2, warMachine);
    }

    public static boolean gorgonProof(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !IronManSuits.isLoaded()) {
            return false;
        }
        ItemStack helmet = living.getItemBySlot(EquipmentSlot.HEAD);
        String suit = helmet.isEmpty() ? null : IronManSuits.suitId(helmet.getItem());
        return suit != null && !MK1.equals(suit);
    }

    static boolean blocksEffect(ServerPlayer player, Holder<MobEffect> effect) {
        if (effect.is(IRRADIATED) && SuitStates.irradiatedImmune(player)) {
            return true;
        }

        if ((effect.is(SCGUNS_BLINDED) || effect.is(SCGUNS_DEAFENED)) && gorgonProof(player)) {
            return true;
        }
        String suit = IronManSuits.isLoaded() ? IronManSuits.fullSuitId(player) : null;
        if (suit == null) {
            return false;
        }
        for (Holder<MobEffect> immune : immunities().getOrDefault(suit, List.of())) {
            if (effect.is(immune)) {
                return true;
            }
        }
        return false;
    }

    static boolean blocksDamage(ServerPlayer player, DamageSource source) {
        String suit = IronManSuits.isLoaded() ? IronManSuits.fullSuitId(player) : null;
        return (SHADES.equals(suit) && source.is(DamageTypeTags.IS_FIRE))
                || (MIDAS.equals(suit) && source.is(DamageTypeTags.IS_FREEZING));
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 10 != 0 || !IronManSuits.isLoaded()) {
            return;
        }
        String suit = IronManSuits.fullSuitId(player);
        if (suit == null) {
            return;
        }
        for (Holder<MobEffect> immune : immunities().getOrDefault(suit, List.of())) {
            if (player.hasEffect(immune)) {
                player.removeEffect(immune);
            }
        }
        switch (suit) {
            case MIDAS -> player.setTicksFrozen(0);
            case SHADES -> player.clearFire();
            case HAMMERHEAD -> {
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 220, 0, true, false, true));
                if (player.isInWater()) {
                    player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 30, 0, true, false, true));
                }
                if (player.isEyeInFluidType(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value())) {
                    HeroFx.effect("alexscaves:deepsight").ifPresent(effect -> player.addEffect(new MobEffectInstance(effect, 220, 0, true, false, true)));
                }
            }
            default -> {
            }
        }
    }

    public static void onRepulsorHit(Entity shooter, Entity target) {
        if (target instanceof LivingEntity living && shooter instanceof LivingEntity wearer && IronManSuits.isLoaded()
                && JACK.equals(IronManSuits.fullSuitId(wearer))) {
            HeroFx.effect("alexscaves:irradiated").ifPresent(effect -> living.addEffect(new MobEffectInstance(effect, 100, 0)));
        }
    }
}
