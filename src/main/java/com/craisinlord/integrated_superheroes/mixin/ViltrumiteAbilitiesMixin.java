package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteFlightLedger;
import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Abilities.class)
public abstract class ViltrumiteAbilitiesMixin implements ViltrumiteFlightLedger {
    @Unique
    private boolean integratedSuperheroes$owned;
    @Unique
    private boolean integratedSuperheroes$grabSaved;
    @Unique
    private boolean integratedSuperheroes$grabMayfly;
    @Unique
    private boolean integratedSuperheroes$grabFlying;

    @Override
    public boolean integratedSuperheroes$viltrumiteOwned() {
        return integratedSuperheroes$owned;
    }

    @Override
    public void integratedSuperheroes$setViltrumiteOwned(boolean owned) {
        integratedSuperheroes$owned = owned;
    }

    @Override
    public void integratedSuperheroes$saveBeforeGrab() {
        if (integratedSuperheroes$grabSaved) {
            return;
        }
        Abilities abilities = (Abilities) (Object) this;
        integratedSuperheroes$grabSaved = true;
        integratedSuperheroes$grabMayfly = abilities.mayfly;
        integratedSuperheroes$grabFlying = abilities.flying;
    }

    @Override
    public boolean integratedSuperheroes$hasGrabSave() {
        return integratedSuperheroes$grabSaved;
    }

    @Override
    public boolean integratedSuperheroes$grabSavedMayfly() {
        return integratedSuperheroes$grabMayfly;
    }

    @Override
    public boolean integratedSuperheroes$grabSavedFlying() {
        return integratedSuperheroes$grabFlying;
    }

    @Override
    public void integratedSuperheroes$clearGrabSave() {
        integratedSuperheroes$grabSaved = false;
    }
}
