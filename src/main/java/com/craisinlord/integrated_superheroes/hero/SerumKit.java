package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.hero.skin.SerumSkins;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;


final class SerumKit {
    static final String INVERTED = "inverted";
    static final String NO_KRYPTONITE = "no_kryptonite_weakness";

    private SerumKit() {
    }

    static boolean is(Player player, String powerId) {
        return player.hasData(com.stxllarplanes.marvellegacy.init.AttachmentsRegistry.PLAYER_DATA.get())
                && powerId.equals(HeroPowers.data(player).getPrimaryPower());
    }

    static int primary(Player player, int fallback) {
        return SerumSkins.primary(player, fallback);
    }

    static int secondary(Player player, int fallback) {
        return SerumSkins.secondary(player, fallback);
    }

    static boolean trait(Player player, String trait) {
        return SerumSkins.has(player, trait);
    }

    static ParticleOptions glow(Player player, int fallback, float size) {
        return HeroAbilities.emissive(primary(player, fallback), size);
    }

    static ParticleOptions glowSecondary(Player player, int fallback, float size) {
        return HeroAbilities.emissive(secondary(player, fallback), size);
    }

    static boolean reddish(int color) {
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        return red > 170 && green < 110 && blue < 110;
    }

    static void keepEffect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getAmplifier() < amplifier) {
            player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true, false, true));
        }
    }

    static void dropEffect(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && current.isInfiniteDuration() && current.isAmbient()) {
            player.removeEffect(effect);
        }
    }

    static void push(LivingEntity target, Vec3 from, double horizontal, double vertical) {
        Vec3 away = target.position().subtract(from);
        Vec3 flat = new Vec3(away.x, 0.0D, away.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize();
        target.setDeltaMovement(target.getDeltaMovement().add(flat.x * horizontal, vertical, flat.z * horizontal));
        target.hurtMarked = true;
    }

    static void ring(ServerLevel level, Vec3 center, ParticleOptions particle, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius,
                    1, 0.0D, 0.05D, 0.0D, 0.0D);
        }
    }

    private static final java.util.Map<java.util.UUID, Integer> METER_SENT = new java.util.HashMap<>();

    static void meter(ServerPlayer player, float fraction) {
        int percent = fraction < 0.0F ? -1 : Math.round(Math.min(1.0F, fraction) * 100.0F);
        Integer sent = METER_SENT.get(player.getUUID());
        if (sent == null || sent != percent) {
            METER_SENT.put(player.getUUID(), percent);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new PhaseMeterPayload(percent));
        }
    }

    static String describe(LivingEntity target) {
        StringBuilder text = new StringBuilder(target.getName().getString());
        text.append(": ").append((int) Math.ceil(target.getHealth())).append("/").append((int) Math.ceil(target.getMaxHealth())).append(" health");
        int armor = target.getArmorValue();
        if (armor > 0) {
            text.append(", ").append(armor).append(" armor");
        }
        if (target instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != null) {
            text.append(", after ").append(mob.getTarget().getName().getString());
        }
        return text.toString();
    }

    static void clearMeter(ServerPlayer player) {
        METER_SENT.remove(player.getUUID());
    }
}
