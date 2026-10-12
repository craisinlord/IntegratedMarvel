package com.craisinlord.integrated_marvel.suit;

import java.util.function.Supplier;

public record SuitAbility(String name, String displayName, Supplier<SuitAbilityConfig.Settings> settings, Handler handler) {
    @FunctionalInterface
    public interface Handler {
        Result press(SuitContext context);
    }

    public enum Result {
        FAILED(false, false),

        FIRED(true, true),

        STARTED(true, false),

        FINISHED(false, true);

        final boolean spendEnergy;
        final boolean startCooldown;

        Result(boolean spendEnergy, boolean startCooldown) {
            this.spendEnergy = spendEnergy;
            this.startCooldown = startCooldown;
        }
    }
}
