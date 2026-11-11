package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.anthology.gravity.GravityApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * Anthology's flipped gravity (the Underside, antimetal, inversion potions) keeps an entity's position at its feet but
 * hangs the body below them in world space. Block checks that assume "feet at the bottom, head at the top" end up
 * looking into the floor.
 */
public final class HeroGravity {
    private HeroGravity() {
    }

    public static boolean inverted(Entity entity) {
        return GravityApi.isGravityInverted(entity);
    }

    /** World Y sign of the entity's own up: 1 normally, -1 when gravity is flipped. */
    public static int up(Entity entity) {
        return inverted(entity) ? -1 : 1;
    }

    /** The world direction of the floor under the entity's feet. */
    public static Direction down(Entity entity) {
        return inverted(entity) ? Direction.UP : Direction.DOWN;
    }

    /** The block the entity is standing on (or would land on), whichever way its gravity points. */
    public static BlockPos floorPos(Entity entity) {
        return BlockPos.containing(entity.getX(), entity.getY() - up(entity) * 0.5D, entity.getZ());
    }

    /** A box of the given size centered on the entity's feet and growing toward its head. */
    public static AABB standingBox(Entity entity, double width, double height) {
        double y = entity.getY();
        double minY = inverted(entity) ? y - height : y;
        return new AABB(entity.getX() - width / 2.0D, minY, entity.getZ() - width / 2.0D,
                entity.getX() + width / 2.0D, minY + height, entity.getZ() + width / 2.0D);
    }
}
