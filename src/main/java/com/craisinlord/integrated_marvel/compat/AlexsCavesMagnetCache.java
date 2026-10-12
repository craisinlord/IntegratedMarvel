package com.craisinlord.integrated_marvel.compat;

public interface AlexsCavesMagnetCache {
    long integratedMarvel$magnetCheckTick();

    boolean integratedMarvel$magnetPulled();

    void integratedMarvel$setMagnetCheck(long tick, boolean pulled);
}
