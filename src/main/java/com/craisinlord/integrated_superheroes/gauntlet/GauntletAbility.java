package com.craisinlord.integrated_superheroes.gauntlet;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public record GauntletAbility(String stone, int index, String id, int icon, Activation activation, Scroll scroll,
                              boolean crouchVariant, boolean checksOwnCooldown, int defaultCooldown,
                              Consumer<GauntletCast> handler, BiConsumer<GauntletCast, Double> scrollHandler) {
    public enum Activation { TAP, HOLD, CHARGE }

    public enum Scroll { NONE, ENTITY, ANY }

    public GauntletAbility withIndex(int newIndex) {
        return new GauntletAbility(stone, newIndex, id, icon, activation, scroll, crouchVariant, checksOwnCooldown,
                defaultCooldown, handler, scrollHandler);
    }

    public boolean held() {
        return activation != Activation.TAP;
    }

    public boolean enabled() {
        return GauntletConfig.isEnabled(stone, index);
    }

    public int cooldownTicks(boolean infinity) {
        int base = GauntletConfig.cooldown(stone, index, defaultCooldown);
        return infinity ? base / 2 : base;
    }

    public String translationSuffix() {
        return stone + "." + (index + 1);
    }

    public static Builder tap(String id, int icon, int cooldown, Consumer<GauntletCast> handler) {
        return new Builder(id, icon, Activation.TAP, cooldown, handler);
    }

    public static Builder hold(String id, int icon, int cooldown, Consumer<GauntletCast> handler) {
        return new Builder(id, icon, Activation.HOLD, cooldown, handler);
    }

    public static Builder charge(String id, int icon, int cooldown, Consumer<GauntletCast> handler) {
        return new Builder(id, icon, Activation.CHARGE, cooldown, handler);
    }

    public static final class Builder {
        private final String id;
        private final int icon;
        private final Activation activation;
        private final int cooldown;
        private final Consumer<GauntletCast> handler;
        private Scroll scroll = Scroll.NONE;
        private BiConsumer<GauntletCast, Double> scrollHandler;
        private boolean crouch;
        private boolean ownCooldown;

        private Builder(String id, int icon, Activation activation, int cooldown, Consumer<GauntletCast> handler) {
            this.id = id;
            this.icon = icon;
            this.activation = activation;
            this.cooldown = cooldown;
            this.handler = handler;
        }

        public Builder crouch() {
            crouch = true;
            return this;
        }

        public Builder ownCooldown() {
            ownCooldown = true;
            return this;
        }

        public Builder scroll(Scroll mode, BiConsumer<GauntletCast, Double> handler) {
            scroll = mode;
            scrollHandler = handler;
            return this;
        }

        GauntletAbility build(String stone, int index) {
            return new GauntletAbility(stone, index, id, icon, activation, scroll, crouch, ownCooldown, cooldown,
                    handler, scrollHandler);
        }
    }
}
