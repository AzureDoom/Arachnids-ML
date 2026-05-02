package mod.azure.arachnids.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Random;

import mod.azure.arachnids.mob.brainbug.BrainBug;
import mod.azure.arachnids.mob.chariotbug.ChariotBug;
import mod.azure.arachnids.mob.hopperbug.HopperBug;
import mod.azure.arachnids.mob.warriorbug.WarriorBug;
import mod.azure.arachnids.mob.worker.WorkerBug;
import mod.azure.arachnids.registry.EntityRegistry;

public final class ColonySpawner {

    private static final int SPAWN_ATTEMPT_TRIES = 8;

    public ChariotBug trySpawnChariot(ServerLevel level, ColonyBounds bounds, BrainBug brain, Random rng) {
        var pos = findSpawnPos(level, bounds, rng);
        if (pos == null)
            return null;

        var entity = EntityRegistry.CHARIOTBUG.get().create(level);
        if (entity == null)
            return null;

        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rng.nextFloat() * 360, 0);
        level.addFreshEntityWithPassengers(entity);
        return entity;
    }

    public WorkerBug trySpawnWorker(ServerLevel level, ColonyBounds bounds, BrainBug brain, Random rng) {
        var pos = findSpawnPos(level, bounds, rng);
        if (pos == null)
            return null;

        var entity = EntityRegistry.WORKERBUG.get().create(level);
        if (entity == null)
            return null;

        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rng.nextFloat() * 360, 0);
        level.addFreshEntityWithPassengers(entity);
        return entity;
    }

    public WarriorBug trySpawnWarrior(ServerLevel level, ColonyBounds bounds, BrainBug brain, Random rng) {
        var pos = findSpawnPos(level, bounds, rng);
        if (pos == null)
            return null;

        var entity = EntityRegistry.WARRIORBUG.get().create(level);
        if (entity == null)
            return null;

        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rng.nextFloat() * 360, 0);
        level.addFreshEntityWithPassengers(entity);
        return entity;
    }

    public HopperBug trySpawnHopper(ServerLevel level, ColonyBounds bounds, BrainBug brain, Random rng) {
        var pos = findSpawnPos(level, bounds, rng);
        if (pos == null)
            return null;

        var entity = EntityRegistry.HOPPERBUG.get().create(level);
        if (entity == null)
            return null;

        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rng.nextFloat() * 360, 0);
        level.addFreshEntityWithPassengers(entity);
        return entity;
    }

    private BlockPos findSpawnPos(ServerLevel level, ColonyBounds bounds, Random rng) {
        var aabb = bounds.spawnAABB();
        int minX = (int) aabb.minX, maxX = (int) aabb.maxX;
        int minY = (int) aabb.minY, maxY = (int) aabb.maxY;
        int minZ = (int) aabb.minZ, maxZ = (int) aabb.maxZ;

        for (var attempt = 0; attempt < SPAWN_ATTEMPT_TRIES; attempt++) {
            var x = minX + rng.nextInt(Math.max(1, maxX - minX));
            var z = minZ + rng.nextInt(Math.max(1, maxZ - minZ));

            for (var y = maxY; y >= minY; y--) {
                var floor = BlockPos.containing(x, y, z);
                var above = floor.above();

                if (
                    !level.getBlockState(floor).isAir()
                        && level.getBlockState(above).isAir()
                        && level.getBlockState(above.above()).isAir()
                ) {
                    return above;
                }
            }
        }
        return null;
    }
}
