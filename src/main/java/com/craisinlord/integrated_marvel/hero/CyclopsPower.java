package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.antarchy.content.entity.royal.beam.RoyalBeamSettings;
import com.craisinlord.antarchy.content.entity.royal.beam.RoyalBeamTerrainMode;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.tintankgames.marvel.sounds.MarvelSoundEvents;

import java.util.List;
import java.util.function.Supplier;

public final class CyclopsPower {
    public static final String ID = "optic_blasts_scott_summers";
    private static final TagKey<Item> VISOR = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("marvel", "cyclops_helmet"));
    private static final int UNCONTROLLED = 0;
    private static final int NARROW = 1;
    private static final int WIDE_FIRST = 2;
    private static final float[] WIDE_ANGLES = {-40.0F, -20.0F, 0.0F, 20.0F, 40.0F};

    private CyclopsPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Optic Blasts (Scott Summers)",
                syringe,
                MarvelBalanceConfig.CYCLOPS,
                List.of(),
                List.of(
                        new HeroPower.Ability("optic_blast", "Optic Blast", CyclopsPower::opticBlast),
                        new HeroPower.Ability("wide_blast", "Wide Blast", CyclopsPower::wideBlast)
                ),
                List.of(),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (hasVisor(player)) {
                            if (HeroBeams.isFiring(player, UNCONTROLLED)) {
                                HeroBeams.stop(player, UNCONTROLLED);
                            }
                            return;
                        }
                        if (!HeroBeams.isFiring(player, UNCONTROLLED)) {
                            HeroBeams.fire(player, UNCONTROLLED, uncontrolledSpec());
                            player.level().playSound(null, player.blockPosition(), MarvelSoundEvents.CYCLOPS_OPTIC_BLAST.get(), SoundSource.PLAYERS, 0.6F, 0.8F);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroBeams.stopAll(player);
                    }
                }
        );
    }

    private static boolean hasVisor(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(VISOR);
    }

    private static double opticBlast(ServerPlayer player, int slot) {
        if (!hasVisor(player)) {
            player.displayClientMessage(Component.literal("§cYou can't control your eyes without your visor."), true);
            return 0.0D;
        }
        HeroBeams.fire(player, NARROW, spec(MarvelBalanceConfig.OPTIC_BLAST_RANGE.get(), MarvelBalanceConfig.OPTIC_BLAST_DURATION_TICKS.get(),
                MarvelBalanceConfig.OPTIC_BLAST_DAMAGE.get(), 1.0F, 2.0F, 0.0F, 0.0F, 1));
        player.level().playSound(null, player.blockPosition(), MarvelSoundEvents.CYCLOPS_OPTIC_BLAST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return MarvelBalanceConfig.OPTIC_BLAST_COOLDOWN.get();
    }

    private static double wideBlast(ServerPlayer player, int slot) {
        if (!hasVisor(player)) {
            player.displayClientMessage(Component.literal("§cYou can't control your eyes without your visor."), true);
            return 0.0D;
        }
        for (int i = 0; i < WIDE_ANGLES.length; i++) {
            HeroBeams.fire(player, WIDE_FIRST + i, spec(MarvelBalanceConfig.WIDE_BLAST_RANGE.get(), MarvelBalanceConfig.WIDE_BLAST_DURATION_TICKS.get(),
                    MarvelBalanceConfig.WIDE_BLAST_DAMAGE.get(), 0.9F, 1.8F, WIDE_ANGLES[i], 0.0F, 1));
        }
        player.level().playSound(null, player.blockPosition(), MarvelSoundEvents.CYCLOPS_OPTIC_BLAST_BIG.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return MarvelBalanceConfig.WIDE_BLAST_COOLDOWN.get();
    }

    private static HeroBeams.Spec uncontrolledSpec() {
        return spec(MarvelBalanceConfig.UNCONTROLLED_RANGE.get(), 100, MarvelBalanceConfig.UNCONTROLLED_DAMAGE.get(), 0.8F, 1.6F, 0.0F, 4.0F, 2);
    }

    private static HeroBeams.Spec spec(double range, int duration, double damage, float pathRadius, float impactRadius, float yaw,
                                       float wobble, int terrainInterval) {
        RoyalBeamSettings settings = HeroBeams.settings(range, duration, pathRadius, impactRadius, damage, 0.6F, terrainInterval,
                MarvelBalanceConfig.OPTIC_TERRAIN_RADIUS.get(), MarvelBalanceConfig.OPTIC_BLOCK_DROP_CHANCE.get(),
                MarvelBalanceConfig.OPTIC_MAX_BLOCK_RESISTANCE.get());
        return new HeroBeams.Spec(HeroBeams.QUEEN_RED, settings, RoyalBeamTerrainMode.DESTROY, yaw, 0.6D, 0.0D,
                MarvelBalanceConfig.OPTIC_FIRE_SECONDS.get(), wobble);
    }
}
