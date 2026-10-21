package com.craisinlord.integrated_superheroes.compat;

public interface ViltrumiteFlightLedger {
    boolean integratedSuperheroes$viltrumiteOwned();

    void integratedSuperheroes$setViltrumiteOwned(boolean owned);

    void integratedSuperheroes$saveBeforeGrab();

    boolean integratedSuperheroes$hasGrabSave();

    boolean integratedSuperheroes$grabSavedMayfly();

    boolean integratedSuperheroes$grabSavedFlying();

    void integratedSuperheroes$clearGrabSave();
}
