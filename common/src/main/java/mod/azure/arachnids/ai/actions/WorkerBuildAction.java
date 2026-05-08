package mod.azure.arachnids.ai.actions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.colony.ColonyDomeBuilder;
import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.mob.worker.WorkerBug;

public class WorkerBuildAction<E extends WorkerBug> implements Action<E> {

    private static final double MOVE_SPEED = 0.40D;

    private static final double BRAIN_BUILD_DIST_SQ = 144.0D; // 12 blocks

    private static final int PLACE_TICKS = 20;

    private static final String BUILD_TASK_KEY = AiKeys.WORKER_BUILD_TASK;

    private static final String BUILD_TIMER_KEY = AiKeys.WORKER_BUILD_TIMER;

    private final int priority;

    public WorkerBuildAction(int priority) {
        this.priority = priority;
    }

    @Override
    public void start(E mob, Blackboard bb, Cooldowns cd) {
        bb.remove(BUILD_TASK_KEY);
        bb.remove(BUILD_TIMER_KEY);
    }

    @Override
    public ActionStatus tick(E mob, Blackboard bb, Cooldowns cd) {
        var colony = ColonyManager.get().colonyOf(mob);
        if (colony == null || colony.isDisbanded())
            return ActionStatus.FAILURE;

        if (!(mob.level() instanceof ServerLevel level))
            return ActionStatus.FAILURE;

        var domeBuilder = colony.getDomeBuilder();
        var blockStore = colony.getBlockStore();
        var brain = colony.getBrain();

        var brainDistSq = mob.distanceToSqr(brain.getX(), brain.getY(), brain.getZ());
        if (brainDistSq > BRAIN_BUILD_DIST_SQ) {
            if (!mob.getNavigation().isInProgress()) {
                mob.getNavigation().moveTo(brain, MOVE_SPEED);
            }
            var heldTask = bb.get(BUILD_TASK_KEY, ColonyDomeBuilder.BuildTask.class);
            if (heldTask != null) {
                domeBuilder.returnTask(heldTask.pos(), heldTask.block(), blockStore);
                bb.remove(BUILD_TASK_KEY);
                bb.remove(BUILD_TIMER_KEY);
            }
            return ActionStatus.RUNNING;
        }

        var task = bb.get(BUILD_TASK_KEY, ColonyDomeBuilder.BuildTask.class);
        if (task == null) {
            task = domeBuilder.claimNextBuildTask(level, blockStore);
            if (task == null)
                return ActionStatus.FAILURE;

            bb.set(BUILD_TASK_KEY, task);
            bb.set(BUILD_TIMER_KEY, 0);
        }

        var pos = task.pos();
        var block = task.block();

        var timer = orZero(bb) + 1;
        bb.set(BUILD_TIMER_KEY, timer);

        if (timer >= PLACE_TICKS) {
            var currentState = level.getBlockState(pos);

            if (currentState.isAir()) {
                var mobPos = mob.blockPosition();
                if (!mobPos.equals(pos) && !mobPos.equals(pos.above())) {
                    level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
                    domeBuilder.completeTask(pos);
                    bb.remove(BUILD_TASK_KEY);
                    bb.remove(BUILD_TIMER_KEY);
                    mob.getNavigation().stop();
                } else {
                    mob.getNavigation()
                        .moveTo(
                            pos.getX() + 0.5,
                            pos.getY() + 2.0,
                            pos.getZ() + 0.5,
                            MOVE_SPEED
                        );
                    bb.set(BUILD_TIMER_KEY, 0);
                }
            } else {
                domeBuilder.completeTask(pos);
                bb.remove(BUILD_TASK_KEY);
                bb.remove(BUILD_TIMER_KEY);
            }
        }

        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(E mob, Blackboard bb, ActionStatus reason) {
        var task = bb.get(BUILD_TASK_KEY, ColonyDomeBuilder.BuildTask.class);
        if (task != null) {
            var colony = ColonyManager.get().colonyOf(mob);
            if (colony != null) {
                colony.getDomeBuilder().returnTask(task.pos(), task.block(), colony.getBlockStore());
            }
            bb.remove(BUILD_TASK_KEY);
        }
        bb.remove(BUILD_TIMER_KEY);
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

    private static int orZero(Blackboard bb) {
        var v = bb.get(WorkerBuildAction.BUILD_TIMER_KEY, Integer.class);
        return v == null ? 0 : v;
    }
}
