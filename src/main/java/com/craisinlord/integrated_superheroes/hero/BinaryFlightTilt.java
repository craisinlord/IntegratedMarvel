package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class BinaryFlightTilt {
    private static final double RANGE = 0.6D;
    private static final float RESPONSE = 0.3F;
    private static final int MAX_CATCH_UP = 10;
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();

    private BinaryFlightTilt() {
    }

    public static Tilt sample(LivingEntity living, float partialTick) {
        State state = STATES.computeIfAbsent(living, ignored -> new State(living.tickCount));
        int elapsed = living.tickCount - state.tick;
        if (elapsed < 0 || elapsed > MAX_CATCH_UP) {
            state.reset(living);
        } else if (elapsed > 0) {
            Vec3 motion = new Vec3(living.getX() - living.xo, living.getY() - living.yo, living.getZ() - living.zo);
            Vec3 local = motion.yRot((living.getYRot() + 90.0F) * Mth.DEG_TO_RAD);
            float forward = (float) Mth.clamp(local.x / RANGE, -1.0D, 1.0D);
            float side = (float) Mth.clamp(local.z / RANGE, -1.0D, 1.0D);
            float climb = (float) Mth.clamp(motion.y / RANGE, -1.0D, 1.0D);
            for (int i = 0; i < elapsed; i++) {
                state.step(forward, side, climb);
            }
            state.tick = living.tickCount;
        }
        return new Tilt(
                Mth.lerp(partialTick, state.prevForward, state.forward),
                Mth.lerp(partialTick, state.prevSide, state.side),
                Mth.lerp(partialTick, state.prevClimb, state.climb));
    }

    public record Tilt(float forward, float side, float climb) {
    }

    private static final class State {
        private int tick;
        private float prevForward;
        private float forward;
        private float prevSide;
        private float side;
        private float prevClimb;
        private float climb;

        private State(int tick) {
            this.tick = tick;
        }

        private void reset(LivingEntity living) {
            this.tick = living.tickCount;
            this.prevForward = this.forward = 0.0F;
            this.prevSide = this.side = 0.0F;
            this.prevClimb = this.climb = 0.0F;
        }

        private void step(float targetForward, float targetSide, float targetClimb) {
            this.prevForward = this.forward;
            this.prevSide = this.side;
            this.prevClimb = this.climb;
            this.forward += (targetForward - this.forward) * RESPONSE;
            this.side += (targetSide - this.side) * RESPONSE;
            this.climb += (targetClimb - this.climb) * RESPONSE;
        }
    }
}
