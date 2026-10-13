package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.antarchy.content.entity.WaspEntity;
import com.craisinlord.antarchy.content.entity.ant.BaseAntEntity;
import com.craisinlord.antarchy.content.entity.ant.RedAntEntity;
import com.craisinlord.antarchy.content.entity.ant.TermiteEntity;
import net.minecraft.world.entity.Entity;

public final class AntarchyColony {
    private AntarchyColony() {
    }

    public static boolean isAnt(Entity entity) {
        return entity instanceof BaseAntEntity;
    }

    public static boolean isTermite(Entity entity) {
        return entity instanceof TermiteEntity;
    }

    public static boolean isRedAnt(Entity entity) {
        return entity instanceof RedAntEntity;
    }

    public static boolean isWasp(Entity entity) {
        return entity instanceof WaspEntity;
    }
}
