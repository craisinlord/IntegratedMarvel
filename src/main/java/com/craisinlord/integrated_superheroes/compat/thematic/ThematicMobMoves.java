package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.ability.ThematicAbility;
import bond.thematic.api.content.ability.part.ComponentDef;
import bond.thematic.api.content.ability.part.Exit;
import bond.thematic.api.content.ability.part.KeyPart;
import bond.thematic.api.content.ability.part.MobPart;
import bond.thematic.api.content.ability.part.PhaseDef;
import bond.thematic.api.content.ability.part.PhaseSpec;
import bond.thematic.api.content.ability.part.SequenceSpec;
import bond.thematic.api.content.ability.state.AbilityEventKind;
import bond.thematic.api.content.ability.state.Guard;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantPoolData;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ThematicMobMoves {
    private static final int TUNED_TICKS = 60;
    private static final int OPEN_TICKS = 60;
    private static final int MAX_TICKS = 160;
    private static final int MAX_CHAIN = 4;

    enum Kind { PRESS, CHANNEL, STEPS }

    record Built(MobPart part, Kind kind) {
    }

    private ThematicMobMoves() {
    }

    static Built build(ThematicAbility ability, ComponentDef def, VariantPoolData.MobMove move) {
        KeyPart key = def.key();
        if (key == null || key instanceof KeyPart.Toggle) {
            return null;
        }
        PhaseDef first = phase(def, key.then());
        if (first == null) {
            if (key instanceof KeyPart.Hold && key.steps().steps().isEmpty()) {
                return null;
            }
            return new Built(new MobPart(move.minRange(), move.maxRange(), move.cooldownTicks(), move.weight()), Kind.PRESS);
        }
        List<SequenceSpec.Step> steps = new ArrayList<>(steps(key.steps()));
        List<PhaseSpec.TickRule> every = new ArrayList<>();
        List<JsonObject> end = List.of();
        Map<AbilityEventKind, PhaseSpec.Reaction> on = Map.of();
        Guard until = null;
        Set<String> seen = new HashSet<>();
        Exit exit = key.then();
        PhaseDef phase = first;
        int offset = 0;
        int depth = 0;
        while (phase != null && seen.add(phase.name()) && depth++ < MAX_CHAIN) {
            PhaseSpec spec = phase.state();
            int dwell = dwell(spec);
            if (!exit.actions().isEmpty()) {
                steps.add(new SequenceSpec.Step(offset, exit.actions()));
            }
            for (SequenceSpec.Step step : steps(spec.steps())) {
                steps.add(new SequenceSpec.Step(step.atTicks() + offset, step.actions()));
            }
            PhaseDef following = spec.dwell().timed() ? phase(def, spec.then()) : null;
            boolean last = following == null || seen.contains(following.name()) || depth >= MAX_CHAIN;
            for (Object raw : spec.every()) {
                every.add(shift((PhaseSpec.TickRule) raw, offset, last ? Double.POSITIVE_INFINITY : offset + dwell));
            }
            if (on.isEmpty()) {
                PhaseSpec.Reaction contact = contact(spec);
                if (contact != null) {
                    on = Map.of(AbilityEventKind.COLLIDE, contact);
                }
            }
            until = depth == 1 && !spec.until().isEmpty() ? ((PhaseSpec.Until) spec.until().get(0)).guard() : null;
            offset += dwell;
            if (last) {
                end = actions(spec.end());
                break;
            }
            if (!spec.end().isEmpty()) {
                steps.add(new SequenceSpec.Step(offset, actions(spec.end())));
            }
            exit = spec.then();
            phase = following;
        }
        steps.sort(Comparator.comparingInt(SequenceSpec.Step::atTicks));
        SequenceSpec timeline = new SequenceSpec(List.copyOf(steps));
        if (every.isEmpty() && on.isEmpty()) {
            if (steps.isEmpty()) {
                return null;
            }
            return new Built(new MobPart(move.minRange(), move.maxRange(), move.cooldownTicks(), move.weight(), 0, false,
                    timeline, List.of(), Map.of(), List.of(), true, null, true), Kind.STEPS);
        }
        int lasts = Math.max(1, Math.min(MAX_TICKS, offset));
        return new Built(new MobPart(move.minRange(), move.maxRange(), move.cooldownTicks(), move.weight(), lasts, true,
                timeline, List.copyOf(every), on, end, true, until, true), Kind.CHANNEL);
    }

    private static PhaseDef phase(ComponentDef def, Exit exit) {
        if (exit == null || Exit.COOLDOWN_NAME.equals(exit.target()) || Exit.READY_NAME.equals(exit.target())) {
            return null;
        }
        return def.phase(exit.target());
    }

    private static int dwell(PhaseSpec spec) {
        return switch (spec.dwell().kind()) {
            case FIXED -> Math.max(1, spec.dwell().ticks());
            case TUNED -> TUNED_TICKS;
            case OPEN -> OPEN_TICKS;
        };
    }

    @SuppressWarnings("unchecked")
    private static List<SequenceSpec.Step> steps(SequenceSpec spec) {
        return spec == null ? List.of() : (List<SequenceSpec.Step>) spec.steps();
    }

    @SuppressWarnings("unchecked")
    private static List<JsonObject> actions(List<?> raw) {
        return raw == null ? List.of() : List.copyOf((List<JsonObject>) raw);
    }

    @SuppressWarnings("unchecked")
    private static PhaseSpec.TickRule shift(PhaseSpec.TickRule rule, int offset, double stop) {
        if (offset == 0 && stop == Double.POSITIVE_INFINITY) {
            return rule;
        }
        List<Double> frames = new ArrayList<>();
        for (Object frame : rule.frames()) {
            frames.add(((Number) frame).doubleValue() + offset);
        }
        return new PhaseSpec.TickRule(rule.intervalTicks(), rule.chance(), rule.actions(), rule.clock(), rule.startTicks() + offset,
                rule.exactInterval(), frames, rule.cycleTicks(), rule.fromTicks() + offset, Math.min(rule.untilTicks() + offset, stop));
    }

    private static PhaseSpec.Reaction contact(PhaseSpec spec) {
        PhaseSpec.Reaction collide = null;
        PhaseSpec.Reaction land = null;
        for (Map.Entry<?, ?> entry : spec.on().entrySet()) {
            if (!(entry.getKey() instanceof AbilityEventKind kind) || !(entry.getValue() instanceof PhaseSpec.Reaction reaction)) {
                continue;
            }
            if (kind == AbilityEventKind.COLLIDE) {
                collide = reaction;
            } else if ("land".equals(kind.jsonName())) {
                land = reaction;
            }
        }
        if (collide != null) {
            return collide;
        }
        if (land == null) {
            return null;
        }
        return new PhaseSpec.Reaction(land.guard(), land.actions(), land.steps(), land.then() != null ? land.then() : Exit.COOLDOWN);
    }
}
