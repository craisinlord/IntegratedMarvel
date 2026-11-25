package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public final class HeroHits {
    private static final int WINDOW_TICKS = 10;
    private static final int BLOCKING_TICKS = 20;
    private static final Map<LivingEntity, Map<UUID, Window>> WINDOWS = ServerState.map(new WeakHashMap<>());
    private static final ThreadLocal<Request> REQUEST = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> LANDING = ThreadLocal.withInitial(() -> false);

    private HeroHits() {
    }

    public interface Invulnerability {
        float integratedSuperheroes$lastHurt();

        void integratedSuperheroes$setLastHurt(float amount);
    }

    private record Request(LivingEntity caster, boolean pulse) {
    }

    private record Window(long start, float amount) {
    }

    public static boolean hurt(Entity caster, LivingEntity target, DamageSource source, float amount) {
        return request(caster, false, () -> target.hurt(source, amount));
    }

    public static boolean pulse(Entity caster, LivingEntity target, DamageSource source, float amount) {
        return request(caster, true, () -> target.hurt(source, amount));
    }

    public static boolean force(Entity caster, LivingEntity target, DamageSource source, float amount) {
        if (!(caster instanceof ServerPlayer) || target instanceof Player) {
            target.invulnerableTime = 0;
        }
        return pulse(caster, target, source, amount);
    }

    private static boolean request(Entity caster, boolean pulse, BooleanSupplier hit) {
        Request previous = REQUEST.get();
        REQUEST.set(caster instanceof LivingEntity living ? new Request(living, pulse) : null);
        try {
            return hit.getAsBoolean();
        } finally {
            REQUEST.set(previous);
        }
    }

    public static boolean land(LivingEntity target, DamageSource source, float amount, Predicate<Float> original) {
        if (target.level().isClientSide || target instanceof Player || LANDING.get()) {
            return original.test(amount);
        }
        Request request = REQUEST.get();
        boolean volley = JubileePower.isVolleyRocket(source.getDirectEntity());
        Entity caster = request != null ? request.caster() : HeroAbilities.isAbilityDamage() || volley ? source.getEntity() : null;
        if (!(caster instanceof ServerPlayer player)) {
            return original.test(amount);
        }
        Invulnerability state = (Invulnerability) target;
        float scaled = volley || HeroAbilities.isSuitDamage() ? amount : amount * AbilityDamage.scale(player);
        boolean pulse = volley || request != null && request.pulse();
        int invulnerableTime = target.invulnerableTime;
        float lastHurt = state.integratedSuperheroes$lastHurt();
        long now = target.level().getGameTime();
        Map<UUID, Window> windows = WINDOWS.computeIfAbsent(target, ignored -> new HashMap<>());
        windows.values().removeIf(window -> now - window.start() >= WINDOW_TICKS || now < window.start());
        Window window = pulse ? null : windows.get(player.getUUID());
        if (window == null) {
            target.invulnerableTime = 0;
        } else {
            target.invulnerableTime = BLOCKING_TICKS;
            state.integratedSuperheroes$setLastHurt(window.amount());
        }
        LANDING.set(true);
        try {
            boolean landed = original.test(scaled);
            if (landed && !pulse) {
                windows.put(player.getUUID(), new Window(window == null ? now : window.start(), state.integratedSuperheroes$lastHurt()));
            }
            return landed;
        } finally {
            LANDING.set(false);
            target.invulnerableTime = invulnerableTime;
            state.integratedSuperheroes$setLastHurt(lastHurt);
        }
    }
}
