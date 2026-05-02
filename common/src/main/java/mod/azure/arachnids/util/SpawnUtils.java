package mod.azure.arachnids.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

public class SpawnUtils {

    public static boolean isDesertSpawn(ServerLevelAccessor world, BlockPos pos) {
        return world.getBiome(pos).is(ModTags.SPAWN_ARACHNIDS);
    }

    public static boolean isDesertCaveSpawn(ServerLevelAccessor world, BlockPos pos) {
        if (world.canSeeSky(pos)) {
            return false;
        }

        if (world.getMaxLocalRawBrightness(pos) > 7) {
            return false;
        }

        if (pos.getY() > 60) {
            return false;
        }

        var surfacePos = world.getHeightmapPos(
            Heightmap.Types.WORLD_SURFACE,
            pos
        );

        var surfaceBiome = world.getBiome(surfacePos);

        return surfaceBiome.is(ModTags.SPAWN_ARACHNIDS);
    }
}
