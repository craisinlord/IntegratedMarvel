package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.registry.ModEffects;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class GunJam {
    private static final ResourceLocation JAM_SOUND = ResourceLocation.fromNamespaceAndPath("scguns", "item.rusty_gnat.copper_jam");
    private static final int CLICK_GAP = 15;
    private static final Map<LivingEntity, Long> LAST_CLICK = new WeakHashMap<>();

    private GunJam() {
    }

    public static boolean jammed(LivingEntity entity) {
        return entity != null && ModEffects.JAMMED.isBound() && entity.hasEffect(ModEffects.JAMMED);
    }

    public static boolean blocksShot(LivingEntity shooter) {
        if (!jammed(shooter)) {
            return false;
        }
        click(shooter);
        return true;
    }

    private static void click(LivingEntity shooter) {
        if (!(shooter.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        Long last = LAST_CLICK.get(shooter);
        if (last != null && now - last < CLICK_GAP && now >= last) {
            return;
        }
        LAST_CLICK.put(shooter, now);
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(JAM_SOUND).orElse(SoundEvents.DISPENSER_FAIL);
        level.playSound(null, shooter.getX(), shooter.getEyeY(), shooter.getZ(), sound, SoundSource.HOSTILE, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        Vec3 hand = shooter.getEyePosition().add(shooter.getLookAngle().scale(0.6D)).subtract(0.0D, 0.4D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, hand.x, hand.y, hand.z, 4, 0.08D, 0.08D, 0.08D, 0.01D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 3, 0.1D, 0.1D, 0.1D, 0.05D);
    }
}
