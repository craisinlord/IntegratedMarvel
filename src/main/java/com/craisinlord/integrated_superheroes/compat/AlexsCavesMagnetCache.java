package com.craisinlord.integrated_superheroes.compat;

public interface AlexsCavesMagnetCache {
    long integratedSuperheroes$magnetCheckTick();

    boolean integratedSuperheroes$magnetPulled();

    void integratedSuperheroes$setMagnetCheck(long tick, boolean pulled);
}
