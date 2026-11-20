package com.craisinlord.integrated_superheroes.voidworld.client;

import com.craisinlord.integrated_superheroes.voidworld.VoidPrunePayload;
import com.craisinlord.integrated_superheroes.voidworld.client.sky.TimelineSky;

public final class VoidTimelineSky {
    private VoidTimelineSky() {
    }

    public static void onPrune(VoidPrunePayload payload) {
        TimelineSky.onPrune(payload);
    }
}
