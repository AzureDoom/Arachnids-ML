package mod.azure.arachnids.ai.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.mob.worker.WorkerBug;
import mod.azure.arachnids.util.ModTags;

public class WorkerDigAction<E extends WorkerBug> implements Action<E> {

    private static final double MOVE_SPEED = 0.46D; // slightly faster than before

    private static final int DIG_TICKS = 8;

    private static final double DIG_DIST_SQ = 16.0;

    private final int priority;

    public WorkerDigAction(int priority) {
        this.priority = priority;
    }

    @Override
    public void start(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        if (blackboard.get(AiKeys.DIG_TARGET, BlockPos.class) == null) {
            blackboard.remove(AiKeys.DIG_TIMER);
        }

        if (blackboard.get(AiKeys.DIG_BATCH_COUNT, Integer.class) == null) {
            blackboard.set(AiKeys.DIG_BATCH_COUNT, 0);
        }
    }

    @Override
    public ActionStatus tick(E mob, Blackboard bb, Cooldowns cd) {
        var colony = ColonyManager.get().colonyOf(mob);
        if (colony == null || colony.isDisbanded())
            return ActionStatus.FAILURE;

        if (!(mob.level() instanceof ServerLevel level))
            return ActionStatus.FAILURE;

        if (colony.getBlockStore().isFull())
            return ActionStatus.FAILURE;

        var targetBlock = bb.get(AiKeys.DIG_TARGET, BlockPos.class);
        var digTimer = orZero(bb);

        if (targetBlock == null) {
            targetBlock = colony.getTunnelDigger().claimNextDigTask(level, mob.blockPosition());
            if (targetBlock == null)
                return ActionStatus.FAILURE;

            bb.set(AiKeys.DIG_TARGET, targetBlock);
            bb.set(AiKeys.DIG_TIMER, 0);
            mob.getNavigation()
                .moveTo(
                    targetBlock.getX() + 0.5,
                    targetBlock.getY(),
                    targetBlock.getZ() + 0.5,
                    MOVE_SPEED
                );
        }

        var distSq = mob.distanceToSqr(
            targetBlock.getX() + 0.5,
            targetBlock.getY() + 0.5,
            targetBlock.getZ() + 0.5
        );
        if (distSq > DIG_DIST_SQ) {
            if (!mob.getNavigation().isInProgress()) {
                colony.getTunnelDigger().returnTask(targetBlock);
                bb.remove(AiKeys.DIG_TARGET);
                bb.remove(AiKeys.DIG_TIMER);
            }
            return ActionStatus.RUNNING;
        }

        digTimer++;
        bb.set(AiKeys.DIG_TIMER, digTimer);

        if (digTimer >= DIG_TICKS) {
            var state = level.getBlockState(targetBlock);

            if (!state.isAir() && state.is(ModTags.WEAK_BLOCKS)) {
                colony.getBlockStore().deposit(state);
                level.destroyBlock(targetBlock, false, mob);
            }

            colony.getTunnelDigger().completeTask(targetBlock);
            bb.remove(AiKeys.DIG_TARGET);
            bb.remove(AiKeys.DIG_TIMER);

            var batchCount = batchOrZero(bb) + 1;
            bb.set(AiKeys.DIG_BATCH_COUNT, batchCount);

            if (
                batchCount >= 12
                    || colony.getBlockStore().isFull()
                    || colony.getTunnelDigger().isDone()
            ) {
                bb.remove(AiKeys.DIG_BATCH_COUNT);
                return ActionStatus.SUCCESS;
            }

            return ActionStatus.RUNNING;
        }

        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(E mob, Blackboard blackboard, ActionStatus reason) {
        if (reason == ActionStatus.INTERRUPTED) {
            return;
        }

        var targetBlock = blackboard.get(AiKeys.DIG_TARGET, BlockPos.class);
        if (targetBlock != null) {
            var colony = ColonyManager.get().colonyOf(mob);
            if (colony != null)
                colony.getTunnelDigger().returnTask(targetBlock);
            blackboard.remove(AiKeys.DIG_TARGET);
        }

        blackboard.remove(AiKeys.DIG_TIMER);
        blackboard.remove(AiKeys.DIG_BATCH_COUNT);
        mob.getNavigation().stop();
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public int priority() {
        return priority;
    }

    private static int orZero(Blackboard bb) {
        var v = bb.get(AiKeys.DIG_TIMER, Integer.class);
        return v == null ? 0 : v;
    }

    private static int batchOrZero(Blackboard bb) {
        var v = bb.get(AiKeys.DIG_BATCH_COUNT, Integer.class);
        return v == null ? 0 : v;
    }
}
