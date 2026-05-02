package mod.azure.arachnids.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

import mod.azure.arachnids.util.ModTags;

public final class ColonyTunnelDigger {

    private static final int MIN_SHAFT_SIZE = 6;

    private static final int SHAFT_IRREGULARITY = 2;

    private static final int SHAFT_COUNT = 3;

    private static final int SHAFT_SPREAD = 8;

    private static final float CHAMBER_CHANCE = 0.08F;

    private static final int CHAMBER_DEPTH = 5;

    private static final int CHAMBER_SIZE = 5;

    private static final int BLOCKS_PER_TICK = 12;

    private final Deque<BlockPos> digQueue = new ArrayDeque<>();

    private final Set<BlockPos> claimed = new HashSet<>();

    private boolean planned = false;

    public void plan(ServerLevel level, BlockPos origin, Random rng) {
        if (planned)
            return;
        planned = true;

        var surfaceY = level.getHeight(
            net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
            origin.getX(),
            origin.getZ()
        );

        for (var s = 0; s < SHAFT_COUNT; s++) {
            var offsetX = (rng.nextInt(SHAFT_SPREAD * 2 + 1)) - SHAFT_SPREAD;
            var offsetZ = (rng.nextInt(SHAFT_SPREAD * 2 + 1)) - SHAFT_SPREAD;
            var shaftBase = origin.offset(offsetX, 0, offsetZ);

            planShaft(level, shaftBase, surfaceY, rng);
        }
    }

    public void tick(ServerLevel level) {
        var budget = BLOCKS_PER_TICK;
        while (!digQueue.isEmpty() && budget-- > 0) {
            var pos = digQueue.poll();
            if (level.isLoaded(pos) && canBreak(level, pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public boolean isDone() {
        return planned && digQueue.isEmpty() && claimed.isEmpty();
    }

    private void planShaft(ServerLevel level, BlockPos base, int surfaceY, Random rng) {
        var shaftW = MIN_SHAFT_SIZE + rng.nextInt(SHAFT_IRREGULARITY + 1);
        var shaftD = MIN_SHAFT_SIZE + rng.nextInt(SHAFT_IRREGULARITY + 1);

        for (var y = base.getY(); y <= surfaceY + 1; y++) {
            var wVar = shaftW + (rng.nextInt(3) - 1);
            var dVar = shaftD + (rng.nextInt(3) - 1);
            var hw = wVar / 2;
            var hd = dVar / 2;

            for (var dx = -hw; dx <= hw; dx++) {
                for (var dz = -hd; dz <= hd; dz++) {
                    digQueue.add(base.offset(dx, y - base.getY(), dz));
                }
            }

            if (rng.nextFloat() < CHAMBER_CHANCE) {
                planChamber(base.offset(0, y - base.getY(), 0), hw, hd, rng);
            }
        }

        for (var ex = -3; ex <= 2; ex++) {
            for (var ez = -3; ez <= 2; ez++) {
                digQueue.add(base.offset(ex, surfaceY - base.getY(), ez));
                digQueue.add(base.offset(ex, surfaceY - base.getY() + 1, ez));
            }
        }
    }

    private void planChamber(BlockPos wallCenter, int halfW, int halfD, Random rng) {
        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        var dir = dirs[rng.nextInt(dirs.length)];

        var startX = dir[0] > 0 ? halfW + 1 : (dir[0] < 0 ? -(halfW + 1) : -CHAMBER_SIZE / 2);
        var startZ = dir[1] > 0 ? halfD + 1 : (dir[1] < 0 ? -(halfD + 1) : -CHAMBER_SIZE / 2);

        for (var step = 0; step < CHAMBER_DEPTH; step++) {
            for (var a = -CHAMBER_SIZE / 2; a <= CHAMBER_SIZE / 2; a++) {
                for (var b = 0; b < CHAMBER_SIZE; b++) {
                    var cx = startX + dir[0] * step + (dir[0] == 0 ? a : 0);
                    var cz = startZ + dir[1] * step + (dir[1] == 0 ? a : 0);
                    digQueue.add(wallCenter.offset(cx, b, cz));
                }
            }
        }
    }

    public BlockPos claimNextDigTask(ServerLevel level) {
        while (!digQueue.isEmpty()) {
            var pos = digQueue.poll();

            if (!level.isLoaded(pos))
                continue;

            if (!canBreak(level, pos))
                continue;

            claimed.add(pos);
            return pos;
        }

        return null;
    }

    public void completeTask(BlockPos pos) {
        claimed.remove(pos);
    }

    public void returnTask(BlockPos pos) {
        if (pos == null)
            return;

        claimed.remove(pos);
        digQueue.addFirst(pos);
    }

    private boolean canBreak(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.isAir())
            return false;
        return state.is(ModTags.WEAK_BLOCKS);
    }
}
