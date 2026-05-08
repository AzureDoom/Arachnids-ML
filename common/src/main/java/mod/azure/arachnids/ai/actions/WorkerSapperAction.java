package mod.azure.arachnids.ai.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.mob.worker.WorkerBug;
import mod.azure.arachnids.util.ModTags;

public class WorkerSapperAction<E extends WorkerBug> implements Action<E> {

    private static final int TUNNEL_DEPTH = 4;

    private static final double MOVE_SPEED = 0.24D;

    private static final int DIG_TICKS = 10;

    private static final double RETARGET_DIST = 12.0D;

    private static final int MAX_TUNNEL_LENGTH = 48;

    private static final String SAPPER_CURRENT = AiKeys.SAPPER_CURRENT_BLOCK;

    private static final String SAPPER_TIMER = AiKeys.SAPPER_DIG_TIMER;

    private static final String SAPPER_SNAP = AiKeys.SAPPER_TARGET_SNAP;

    private final int priority;

    private final Deque<BlockPos> queue = new ArrayDeque<>();

    public WorkerSapperAction(int priority) {
        this.priority = priority;
    }

    @Override
    public void start(E mob, Blackboard bb, Cooldowns cd) {
        queue.clear();
        bb.remove(SAPPER_CURRENT);
        bb.remove(SAPPER_TIMER);
        bb.remove(SAPPER_SNAP);
    }

    @Override
    public ActionStatus tick(E mob, Blackboard bb, Cooldowns cd) {
        var colony = ColonyManager.get().colonyOf(mob);
        if (colony == null || colony.isDisbanded())
            return ActionStatus.FAILURE;

        if (!(mob.level() instanceof ServerLevel level))
            return ActionStatus.FAILURE;

        var target = colony.getWarriorDirective();
        if (target == null || !target.isAlive())
            return ActionStatus.FAILURE;

        var snap = bb.get(SAPPER_SNAP, BlockPos.class);
        if (snap == null || snap.distSqr(target.blockPosition()) > RETARGET_DIST * RETARGET_DIST) {
            if (!planTunnel(mob, level, target))
                return ActionStatus.FAILURE;

            snap = target.blockPosition();
            bb.set(SAPPER_SNAP, snap);
            bb.remove(SAPPER_CURRENT);
            bb.remove(SAPPER_TIMER);
        }

        if (queue.isEmpty() && bb.get(SAPPER_CURRENT, BlockPos.class) == null)
            return ActionStatus.SUCCESS;

        var current = bb.get(SAPPER_CURRENT, BlockPos.class);
        if (current == null) {
            current = nextBreakable(level);
            if (current == null)
                return ActionStatus.SUCCESS;

            bb.set(SAPPER_CURRENT, current);
            bb.set(SAPPER_TIMER, 0);
            mob.getNavigation()
                .moveTo(
                    current.getX() + 0.5,
                    current.getY(),
                    current.getZ() + 0.5,
                    MOVE_SPEED
                );
        }

        var distSq = mob.distanceToSqr(
            current.getX() + 0.5,
            current.getY() + 0.5,
            current.getZ() + 0.5
        );
        if (distSq > 9.0) {
            if (!mob.getNavigation().isInProgress()) {
                bb.remove(SAPPER_CURRENT);
                bb.remove(SAPPER_TIMER);
            }
            return ActionStatus.RUNNING;
        }

        var timer = orZero(bb) + 1;
        bb.set(SAPPER_TIMER, timer);

        if (timer >= DIG_TICKS) {
            var blockState = level.getBlockState(current);
            if (!blockState.isAir() && blockState.is(ModTags.WEAK_BLOCKS)) {
                colony.getBlockStore().deposit(blockState);
                level.destroyBlock(current, false, mob);
            }
            bb.remove(SAPPER_CURRENT);
            bb.remove(SAPPER_TIMER);
        }

        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(E mob, Blackboard bb, ActionStatus reason) {
        queue.clear();
        bb.remove(SAPPER_CURRENT);
        bb.remove(SAPPER_TIMER);
        bb.remove(SAPPER_SNAP);
        mob.getNavigation().stop();
    }

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public int priority() {
        return priority;
    }

    private boolean planTunnel(E mob, ServerLevel level, LivingEntity target) {
        queue.clear();

        var colony = ColonyManager.get().colonyOf(mob);
        if (colony == null)
            return false;

        var workerPos = mob.blockPosition();
        var targetPos = target.blockPosition();

        var surfY = level.getHeight(Heightmap.Types.WORLD_SURFACE, targetPos.getX(), targetPos.getZ());
        var tunnelY = surfY - TUNNEL_DEPTH;

        var startX = workerPos.getX();
        var startZ = workerPos.getZ();
        var endX = targetPos.getX();
        var endZ = targetPos.getZ();

        var path = bresenhamXZ(startX, startZ, endX, endZ);
        if (path.size() > MAX_TUNNEL_LENGTH)
            return false;

        for (var step : path) {
            for (var dy = 0; dy < 3; dy++) {
                var pos = new BlockPos(step[0], tunnelY + dy, step[1]);
                if (!colony.getBounds().isInsideTerritory(pos))
                    continue;
                queue.add(pos);
            }
        }

        for (var y = tunnelY; y <= surfY; y++) {
            for (var dy = 0; dy < 3; dy++) {
                queue.add(new BlockPos(endX, y + dy, endZ));
                queue.add(new BlockPos(endX + 1, y + dy, endZ));
            }
        }

        return !queue.isEmpty();
    }

    private BlockPos nextBreakable(ServerLevel level) {
        while (!queue.isEmpty()) {
            var pos = queue.poll();
            if (!level.isLoaded(pos))
                continue;
            var state = level.getBlockState(pos);
            if (!state.isAir() && state.is(ModTags.WEAK_BLOCKS))
                return pos;
        }
        return null;
    }

    private static List<int[]> bresenhamXZ(int x0, int z0, int x1, int z1) {
        List<int[]> points = new ArrayList<>();
        var dx = Math.abs(x1 - x0);
        var dz = Math.abs(z1 - z0);
        var sx = x0 < x1 ? 1 : -1;
        var sz = z0 < z1 ? 1 : -1;
        var err = dx - dz;

        while (true) {
            points.add(new int[] { x0, z0 });
            if (x0 == x1 && z0 == z1)
                break;
            var e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                z0 += sz;
            }
        }
        return points;
    }

    private static int orZero(Blackboard bb) {
        var v = bb.get(WorkerSapperAction.SAPPER_TIMER, Integer.class);
        return v == null ? 0 : v;
    }
}
