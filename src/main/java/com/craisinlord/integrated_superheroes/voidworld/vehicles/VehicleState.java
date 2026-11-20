package com.craisinlord.integrated_superheroes.voidworld.vehicles;

import net.minecraft.world.phys.Vec3;

public record VehicleState(Vec3 position, boolean playerAboard, boolean othersAboard, double speed) {
}
