package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.api.RogueAbsorption;
import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.craisinlord.integrated_superheroes.hero.skin.SerumCostumes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.function.Supplier;

public final class AngelPower {
    public static final String ID = "winged_mutation_warren_worthington";
    public static final String WINGBEAT = "angel_wingbeat";
    public static final String SKYFALL = "angel_skyfall";
    public static final HeroPower.Toggle GLIDE_ASSIST = new HeroPower.Toggle("integrated_superheroes_off_angel_glide_assist", "Glide Assist",
            () -> Items.FEATHER);
    public static final HeroPower.Toggle LANDING_AID = new HeroPower.Toggle("integrated_superheroes_off_angel_landing_aid", "Landing Aid",
            () -> Items.HAY_BLOCK);
    public static final HeroPower.Toggle SHOW_WINGS = new HeroPower.Toggle("integrated_superheroes_off_angel_wings", "Show Wings",
            () -> Items.PHANTOM_MEMBRANE);
    private static boolean registered;
    private static RogueMemory memory;

    private AngelPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(AngelPower::onStartTracking);
            NeoForge.EVENT_BUS.addListener(AngelPower::onStopTracking);
            NeoForge.EVENT_BUS.addListener(AngelPower::onLogout);
            SerumCostumes.register(ID, "thematic:angel");
            rogueMemory();
        }
        return new HeroPower(
                ID,
                "Winged Mutation (Angel)",
                syringe,
                AngelConfig.ANGEL,
                List.of(),
                List.of(
                        new HeroPower.Ability(WINGBEAT, "Wingbeat", AngelPower::wingbeat),
                        new HeroPower.Ability(SKYFALL, "Skyfall", AngelPower::skyfall),
                        HeroToggles.WHEEL
                ),
                List.of(GLIDE_ASSIST, LANDING_AID, SHOW_WINGS),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        WingedFlightController.tick(player);
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        WingedFlightController.clear(player);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (!event.getSource().is(DamageTypeTags.IS_FALL)) {
                            return;
                        }
                        if (HeroToggles.isOn(player, LANDING_AID)) {
                            event.setAmount(event.getAmount() * AngelConfig.LANDING_AID_MULTIPLIER.get().floatValue());
                            if (event.getAmount() <= 0.0F) {
                                event.setCanceled(true);
                            }
                        }
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        WingedFlightController.interrupt(player);
                    }
                }
        );
    }

    public static boolean isAngel(Player player) {
        return SerumKit.is(player, ID);
    }

    private static double wingbeat(ServerPlayer player, int slot) {
        if (!isAngel(player)) {
            return 0.0D;
        }
        return WingedFlightController.toggleWingbeat(player);
    }

    private static double skyfall(ServerPlayer player, int slot) {
        if (!isAngel(player)) {
            return 0.0D;
        }
        return WingedFlightController.skyfall(player);
    }

    public static RogueMemory rogueMemory() {
        if (memory == null) {
            memory = RogueKit.memory("angel_wing_leap", "Angel")
                    .active("Wing Leap", AngelPower::wingLeap)
                    .build();
            RogueAbsorption.remember(memory);
        }
        return memory;
    }

    private static double wingLeap(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize().scale(AngelConfig.WING_LEAP_FORWARD.get());
        player.setDeltaMovement(flat.x, AngelConfig.WING_LEAP_UP.get(), flat.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        Vec3 feet = player.position();
        for (LivingEntity target : HeroAbilities.sphere(player, 2.5D)) {
            if (RogueKit.affects(player, target)) {
                SerumKit.push(target, feet, 0.6D, 0.2D);
            }
        }
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, feet.x, feet.y + 0.2D, feet.z, 16, 0.8D, 0.1D, 0.8D, 0.05D);
        HeroFx.play(player, SoundEvents.ENDER_DRAGON_FLAP, 0.6F, 1.5F);
        return AngelConfig.WING_LEAP_COOLDOWN.get();
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer angel && event.getEntity() instanceof ServerPlayer viewer && isAngel(angel)) {
            WingedFlightController.sendTo(angel, viewer);
        }
    }

    private static void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getTarget() instanceof ServerPlayer angel && event.getEntity() instanceof ServerPlayer viewer) {
            WingedFlightController.forget(angel, viewer);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WingedFlightController.logout(player);
        }
    }
}
