package com.craisinlord.integrated_superheroes.gauntlet;

public final class MindControlClientState {
    private static final MindControlStatePayload NONE = new MindControlStatePayload(false,
            false, false, false, false, false, false, 0.0F, 0.0F);

    private static volatile MindControlStatePayload state = NONE;

    private static volatile boolean controlling;

    private MindControlClientState() {
    }

    public static void reset() {
        state = NONE;
        controlling = false;
    }

    public static void setControlling(boolean active) {
        controlling = active;
    }

    public static boolean controlling() {
        return controlling;
    }

    public static void set(MindControlStatePayload next) {
        state = next;
    }

    public static MindControlStatePayload state() {
        return state;
    }
}
