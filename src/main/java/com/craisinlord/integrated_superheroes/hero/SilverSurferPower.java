package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.api.RogueAbsorption;
import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.craisinlord.integrated_superheroes.hero.skin.SerumCostumes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public final class SilverSurferPower {
    public static final String ID = "power_cosmic_norrin_radd";
    public static final String BOARD = "silver_surfer_board";
    public static final String LANCE = "silver_surfer_cosmic_lance";
    public static final HeroPower.Toggle PRECISION = new HeroPower.Toggle("integrated_superheroes_on_silver_surfer_precision", "Precision Handling",
            () -> Items.COMPASS, false);
    public static final HeroPower.Toggle TRAIL = new HeroPower.Toggle("integrated_superheroes_off_silver_surfer_trail", "Silver Trail",
            () -> Items.END_ROD);
    private static final int LANCE_COLOR = 0xE8F1FF;
    private static boolean registered;
    private static RogueMemory memory;

    private SilverSurferPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(SilverSurferPower::onStartTracking);
            NeoForge.EVENT_BUS.addListener(SilverSurferPower::onStopTracking);
            NeoForge.EVENT_BUS.addListener(SilverSurferPower::onLogout);
            NeoForge.EVENT_BUS.addListener(SilverSurferPower::onLogin);
            NeoForge.EVENT_BUS.addListener(SilverSurferPower::onChangeDimension);
            SerumCostumes.register(ID, "thematic:silver_surfer");
            rogueMemory();
        }
        return new HeroPower(
                ID,
                "Power Cosmic (Silver Surfer)",
                syringe,
                SilverSurferConfig.STATS,
                List.of(),
                List.of(
                        new HeroPower.Ability(BOARD, "Board", SilverSurferPower::board),
                        HeroPower.Ability.of(LANCE, "Cosmic Lance", SilverSurferPower::cosmicLance),
                        HeroToggles.WHEEL
                ),
                List.of(PRECISION, TRAIL),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        SurferBoardController.tick(player);
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        SurferBoardController.clear(player);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        SurferBoardController.incomingDamage(player, event);
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        SurferBoardController.dismount(player, false);
                    }
                }
        );
    }

    public static boolean isSurfer(Player player) {
        return SerumKit.is(player, ID);
    }

    private static double board(ServerPlayer player, int slot) {
        if (!isSurfer(player)) {
            return 0.0D;
        }
        return SurferBoardController.press(player);
    }

    private static double cosmicLance(LivingEntity player, int slot) {
        if (player instanceof Player surfer && !isSurfer(surfer)) {
            return 0.0D;
        }
        double damage = MarvelBalanceConfig.PHOTON_BLAST_DAMAGE.get() * SilverSurferConfig.LANCE_PHOTON_MULTIPLIER.get();
        HeroAdvancements.cast(player).kills(3, "fantastic_four/silver_surfer/skewered").run(() ->
                lance(player, SerumKit.glow(player, LANCE_COLOR, 1.3F), SilverSurferConfig.LANCE_TARGETS.get(),
                        (caster, target) -> caster instanceof ServerPlayer surfer ? KineticBurst.affects(surfer, target) : HeroCaster.foe(caster, target),
                        target -> HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().mobAttack(player), (float) damage))));
        return SilverSurferConfig.LANCE_COOLDOWN.get();
    }

    private static void lance(LivingEntity player, ParticleOptions glow, int maxTargets, BiPredicate<LivingEntity, LivingEntity> affects,
                              java.util.function.Consumer<LivingEntity> hit) {
        Vec3 direction = player.getLookAngle();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, direction, SilverSurferConfig.LANCE_RANGE.get(), 32);
        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity target : beam.targets()) {
            if (targets.size() >= maxTargets) {
                break;
            }
            if (affects.test(player, target)) {
                targets.add(target);
            }
        }
        double knockback = SilverSurferConfig.LANCE_KNOCKBACK.get();
        for (LivingEntity target : targets) {
            hit.accept(target);
            if (knockback > 0.0D) {
                target.knockback(knockback, -direction.x, -direction.z);
            }
        }
        ServerLevel level = HeroCaster.level(player);
        Vec3 start = player.getEyePosition().add(direction.scale(0.6D)).subtract(0.0D, 0.25D, 0.0D);
        Vec3 end = beam.end();
        HeroAbilities.particleLine(level, start, end, glow, 0.45D);
        HeroAbilities.particleLine(level, start, end, ParticleTypes.END_ROD, 1.6D);
        level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 14, 0.25D, 0.25D, 0.25D, 0.12D);
        for (LivingEntity target : targets) {
            Vec3 center = target.getBoundingBox().getCenter();
            level.sendParticles(glow, center.x, center.y, center.z, 10, 0.3D, 0.4D, 0.3D, 0.02D);
        }
        HeroFx.play(player, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.9F);
        level.playSound(null, end.x, end.y, end.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.6F);
    }

    public static RogueMemory rogueMemory() {
        if (memory == null) {
            memory = RogueKit.memory("silver_surfer_cosmic_lance", "Silver Surfer")
                    .active("Cosmic Lance", SilverSurferPower::rogueLance)
                    .build();
            RogueAbsorption.remember(memory);
        }
        return memory;
    }

    private static double rogueLance(ServerPlayer player) {
        double damage = MarvelBalanceConfig.PHOTON_BLAST_DAMAGE.get() * SilverSurferConfig.LANCE_PHOTON_MULTIPLIER.get()
                * SilverSurferConfig.ROGUE_LANCE_MULTIPLIER.get();
        lance(player, HeroAbilities.emissive(LANCE_COLOR, 1.1F), 1, (caster, target) -> RogueKit.affects(player, target), target -> RogueKit.hurt(player, target, damage));
        return SilverSurferConfig.ROGUE_LANCE_COOLDOWN.get();
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer surfer && event.getEntity() instanceof ServerPlayer viewer) {
            SurferBoardController.sendTo(surfer, viewer);
        }
    }

    private static void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getTarget() instanceof ServerPlayer surfer && event.getEntity() instanceof ServerPlayer viewer) {
            SurferBoardController.forget(surfer, viewer);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SurferBoardController.logout(player);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isSurfer(player)) {
            HeroAbilities.setActive(player, BOARD, false);
        }
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SurferBoardController.dismount(player, true);
        }
    }
}
