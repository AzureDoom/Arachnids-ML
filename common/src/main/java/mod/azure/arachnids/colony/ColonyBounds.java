package mod.azure.arachnids.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import mod.azure.arachnids.mob.brainbug.BrainBug;

public final class ColonyBounds {

    public static final int MIN_COLONY_SPACING = 128;

    public static final int RADIUS_H = 24;

    public static final int RADIUS_V = 16;

    public static final int SPAWN_RADIUS_H = 10;

    public static final int SPAWN_RADIUS_V = 8;

    public static final double FAR_DIST_SQ = (RADIUS_H + 32.0) * (RADIUS_H + 32.0);

    private BlockPos centre;

    public ColonyBounds(BlockPos initialCentre) {
        this.centre = initialCentre;
    }

    public void updateCentre(BlockPos newCentre) {
        this.centre = newCentre;
    }

    public BlockPos centre() {
        return centre;
    }

    public AABB territoryAABB() {
        return new AABB(
            centre.getX() - RADIUS_H,
            centre.getY() - RADIUS_V,
            centre.getZ() - RADIUS_H,
            centre.getX() + RADIUS_H,
            centre.getY() + RADIUS_V,
            centre.getZ() + RADIUS_H
        );
    }

    public AABB spawnAABB() {
        return new AABB(
            centre.getX() - SPAWN_RADIUS_H,
            centre.getY() - SPAWN_RADIUS_V,
            centre.getZ() - SPAWN_RADIUS_H,
            centre.getX() + SPAWN_RADIUS_H,
            centre.getY() + SPAWN_RADIUS_V,
            centre.getZ() + SPAWN_RADIUS_H
        );
    }

    public boolean isInsideTerritory(BlockPos pos) {
        return territoryAABB().contains(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean isFarAway(Entity entity) {
        var dx = entity.getX() - centre.getX();
        var dz = entity.getZ() - centre.getZ();
        return (dx * dx + dz * dz) > FAR_DIST_SQ;
    }

    public boolean isTooCloseToAnotherColony(ServerLevel level, BlockPos pos) {
        var box = new AABB(pos).inflate(MIN_COLONY_SPACING);

        return !level.getEntitiesOfClass(
            BrainBug.class,
            box,
            other -> other.isAlive() && other.blockPosition().distSqr(pos) < MIN_COLONY_SPACING * MIN_COLONY_SPACING
        ).isEmpty();
    }

    public CompoundTag save() {
        var tag = new CompoundTag();

        tag.putInt("X", centre.getX());
        tag.putInt("Y", centre.getY());
        tag.putInt("Z", centre.getZ());

        return tag;
    }

    public static ColonyBounds load(CompoundTag tag) {
        return new ColonyBounds(
            new BlockPos(
                tag.getInt("X"),
                tag.getInt("Y"),
                tag.getInt("Z")
            )
        );
    }
}
