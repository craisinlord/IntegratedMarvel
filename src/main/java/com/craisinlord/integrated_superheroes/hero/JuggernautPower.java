package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.api.RogueAbsorption;
import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.craisinlord.integrated_superheroes.hero.skin.SerumCostumes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;
import java.util.function.Supplier;

public final class JuggernautPower {
    public static final String ID = "cyttorak_avatar_cain_marko";
    public static final String CHARGE = "juggernaut_unstoppable_charge";
    public static final String SHOCK = "juggernaut_crimson_shock";
    public static final HeroPower.Toggle BREACH = new HeroPower.Toggle("integrated_superheroes_on_juggernaut_breach", "Breach Terrain",
            () -> Items.CRACKED_STONE_BRICKS, false);
    public static final HeroPower.Toggle FOOTSTEPS = new HeroPower.Toggle("integrated_superheroes_off_juggernaut_footsteps", "Heavy Footsteps",
            () -> Items.IRON_BOOTS);
    public static final HeroPower.Toggle SHAKE = new HeroPower.Toggle("integrated_superheroes_off_juggernaut_shake", "Screen Shake",
            () -> Items.ANVIL);
    private static RogueMemory rogueMemory;
    private static boolean registered;

    private JuggernautPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        SerumCostumes.register(ID, "thematic:juggernaut");
        rogueMemory();
        if (!registered) {
            registered = true;
            HeroCaster.onTick(caster -> {
                JuggernautCharge.tick(caster);
                if (JuggernautCharge.charging(caster)) {
                    HeroCaster.extend(caster, 2);
                }
            });
            HeroCaster.onCleanup(JuggernautCharge::clear);
        }
        return new HeroPower(
                ID,
                "Avatar of Cyttorak (Juggernaut)",
                syringe,
                JuggernautConfig.JUGGERNAUT,
                List.of(),
                List.of(
                        HeroPower.Ability.of(CHARGE, "Unstoppable Charge", JuggernautPower::charge),
                        HeroPower.Ability.of(SHOCK, "Crimson Shock", JuggernautPower::crimsonShock),
                        HeroToggles.WHEEL
                ),
                List.of(BREACH, FOOTSTEPS, SHAKE),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        JuggernautCharge.tick(player);
                        if (!JuggernautCharge.charging(player) && footstepsOn(player)) {
                            HeroStride.tick(player, 2.2D, SoundEvents.RAVAGER_STEP, 0.9F, 0.6F, 4, shakeOn(player) ? 0.05F : 0.0F);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        JuggernautCharge.clear(player);
                        HeroStride.clear(player);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (JuggernautCharge.frontal(player, event.getSource())) {
                            event.setAmount(event.getAmount() * JuggernautConfig.DIG_IN_DAMAGE_MULTIPLIER.get().floatValue());
                            HeroFx.play(player, SoundEvents.ANVIL_LAND, 0.4F, 1.6F);
                        }
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        JuggernautCharge.clear(player);
                        HeroStride.clear(player);
                    }
                }
        );
    }

    public static boolean isJuggernaut(Player player) {
        return SerumKit.is(player, ID);
    }

    static boolean breachOn(LivingEntity caster) {
        return caster instanceof Player player && HeroToggles.isOn(player, BREACH);
    }

    static boolean footstepsOn(LivingEntity caster) {
        return !(caster instanceof Player player) || HeroToggles.isOn(player, FOOTSTEPS);
    }

    static boolean shakeOn(LivingEntity caster) {
        return !(caster instanceof Player player) || HeroToggles.isOn(player, SHAKE);
    }

    private static double charge(LivingEntity player, int slot) {
        if (JuggernautCharge.charging(player)) {
            if (!(player instanceof ServerPlayer) && JuggernautCharge.momentum(player) >= JuggernautConfig.SHOCK_MIN_MOMENTUM.get()
                    && JuggernautCharge.remaining(player, SHOCK) <= 0.0D) {
                crimsonShock(player, slot);
                return 0.0D;
            }
            JuggernautCharge.digIn(player);
            return 0.0D;
        }
        if (JuggernautCharge.busy(player)) {
            return 0.0D;
        }
        double left = JuggernautCharge.remaining(player, CHARGE);
        if (left > 0.0D) {
            return left;
        }
        return JuggernautCharge.start(player) ? 0.25D : 0.0D;
    }

    private static double crimsonShock(LivingEntity player, int slot) {
        double left = JuggernautCharge.remaining(player, SHOCK);
        if (left > 0.0D) {
            return left;
        }
        if (JuggernautCharge.momentum(player) < JuggernautConfig.SHOCK_MIN_MOMENTUM.get()) {
            HeroCaster.message(player, Component.literal("§7Build up more momentum first."));
            return 0.0D;
        }
        float spent = JuggernautCharge.spend(player);
        JuggernautCharge.launchShock(player, spent);
        double cooldown = JuggernautConfig.SHOCK_COOLDOWN.get();
        JuggernautCharge.persist(player, SHOCK, cooldown);
        return cooldown;
    }

    public static RogueMemory rogueMemory() {
        if (rogueMemory == null) {
            rogueMemory = RogueKit.memory("juggernaut", "Juggernaut")
                    .active("Shoulder Rush", RogueKit.dash(1.3D, 8, 40.0D, 1.6D, ParticleTypes.CRIT, SoundEvents.RAVAGER_ROAR, 10.0D))
                    .build();
            RogueAbsorption.remember(rogueMemory);
        }
        return rogueMemory;
    }
}
