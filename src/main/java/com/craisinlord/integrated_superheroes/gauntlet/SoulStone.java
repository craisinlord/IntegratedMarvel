package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

public final class SoulStone {

    private SoulStone() {
    }

    public static void exchange(GauntletCast cast) {
        ServerPlayer player = cast.player();
        if (cast.infinity() && !cast.crouching()) {
            List<LivingEntity> victims = cast.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12.0D),
                    entity -> entity instanceof Enemy && entity.isAlive());
            if (victims.isEmpty()) {
                cast.message("soul_no_target");
                return;
            }
            for (LivingEntity victim : victims) {
                SoulStoneManager.exchange(player, victim);
                tether(cast.level(), player, victim);
            }
            GauntletFx.cast(player, "soul", SoundEvents.SOUL_ESCAPE.value(), 0.6F);
            cast.cooldown();
            return;
        }
        LivingEntity target = GauntletAim.living(player, 32.0D);
        if (target == null) {
            cast.message("soul_no_target");
            return;
        }
        SoulStoneManager.exchange(player, target);
        tether(cast.level(), player, target);
        GauntletFx.cast(player, "soul", SoundEvents.SOUL_ESCAPE.value(), 0.9F);
        cast.cooldown();
    }

    private static void tether(ServerLevel level, LivingEntity from, LivingEntity to) {
        Vec3 start = to.position().add(0.0D, to.getBbHeight() * 0.5D, 0.0D);
        Vec3 end = from.position().add(0.0D, from.getBbHeight() * 0.5D, 0.0D);
        Vec3 step = end.subtract(start);
        int points = (int) Math.min(48, step.length() * 2.0D);
        GauntletFx.line(level, ParticleTypes.SOUL, start, end, points, 0.05F);
    }

    public static void sever(GauntletCast cast) {
        ServerPlayer player = cast.player();
        LivingEntity target = GauntletAim.living(player, 32.0D);
        if (target == null) {
            cast.message("soul_no_target");
            return;
        }
        SoulStoneManager.sever(player, target);
        if (cast.infinity()) {
            for (LivingEntity nearby : cast.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(6.0D),
                    entity -> entity != player && entity != target && entity.isAlive())) {
                SoulStoneManager.sever(player, nearby);
            }
        }
        GauntletFx.burst(cast.level(), target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D), "soul", 40, 0.6D);
        GauntletFx.cast(player, "soul", SoundEvents.SOUL_ESCAPE.value(), 0.5F);
        cast.cooldown();
    }

    public static void reapRestore(GauntletCast cast) {
        ServerPlayer player = cast.player();
        LivingEntity target = SoulStoneManager.isPending(player) ? player : GauntletAim.living(player, 32.0D);
        if (target == null) {
            cast.message("soul_no_target");
            return;
        }
        if (SoulStoneManager.isPending(target)) {
            SoulStoneManager.fate(player, target);
            GauntletFx.burst(cast.level(), target.position().add(0.0D, 1.0D, 0.0D), "soul", 50, 0.6D);
            cast.cooldown();
            return;
        }
        if (cast.onCooldown()) return;
        SoulStoneManager.fate(player, target);
        if (cast.crouching()) {
            GauntletFx.burst(cast.level(), target.position().add(0.0D, 1.0D, 0.0D), "soul", 50, 0.6D);
            cast.cooldown();
        }
    }
}
