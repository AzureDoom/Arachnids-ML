package mod.azure.arachnids.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;

public class SpawnUtils {

    public static boolean isDesertSpawn(ServerLevelAccessor world, BlockPos pos) {
        return world.getBiome(pos).is(ModTags.SPAWN_ARACHNIDS);
    }
}
