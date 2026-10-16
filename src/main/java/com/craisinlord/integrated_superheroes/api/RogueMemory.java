package com.craisinlord.integrated_superheroes.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

public final class RogueMemory {
    private final ResourceLocation id;
    private final String source;
    private final String abilityName;
    private final Active active;
    private final Passive passive;

    private RogueMemory(ResourceLocation id, String source, String abilityName, Active active, Passive passive) {
        this.id = id;
        this.source = source;
        this.abilityName = abilityName;
        this.active = active;
        this.passive = passive;
    }

    public static Builder builder(ResourceLocation id, String source) {
        return new Builder(id, source);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public String source() {
        return this.source;
    }

    public String abilityName() {
        return this.abilityName;
    }

    public Active active() {
        return this.active;
    }

    public Passive passive() {
        return this.passive;
    }

    public boolean hasActive() {
        return this.active != null;
    }

    @FunctionalInterface
    public interface Active {
        double use(ServerPlayer player);
    }

    public interface Passive {
        Passive NONE = new Passive() {
        };

        default void tick(ServerPlayer player) {
        }

        default void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
        }

        default void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
        }

        default void end(ServerPlayer player) {
        }

        static Passive all(Passive... passives) {
            List<Passive> list = List.of(passives);
            return new Passive() {
                @Override
                public void tick(ServerPlayer player) {
                    list.forEach(passive -> passive.tick(player));
                }

                @Override
                public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                    for (Passive passive : list) {
                        if (event.isCanceled()) {
                            return;
                        }
                        passive.incomingDamage(player, event);
                    }
                }

                @Override
                public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                    list.forEach(passive -> passive.outgoingDamage(player, event));
                }

                @Override
                public void end(ServerPlayer player) {
                    list.forEach(passive -> passive.end(player));
                }
            };
        }
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final String source;
        private String abilityName;
        private Active active;
        private Passive passive = Passive.NONE;

        private Builder(ResourceLocation id, String source) {
            this.id = id;
            this.source = source;
        }

        public Builder active(String abilityName, Active active) {
            this.abilityName = abilityName;
            this.active = active;
            return this;
        }

        public Builder passive(Passive passive) {
            this.passive = passive;
            return this;
        }

        public Builder passives(Passive... passives) {
            this.passive = Passive.all(passives);
            return this;
        }

        public RogueMemory build() {
            return new RogueMemory(this.id, this.source, this.abilityName, this.active, this.passive);
        }
    }
}
