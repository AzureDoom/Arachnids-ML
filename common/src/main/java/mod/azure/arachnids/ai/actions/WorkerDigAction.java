package mod.azure.arachnids.ai.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.mob.worker.WorkerBug;
import mod.azure.arachnids.util.ModTags;

public class WorkerDigAction<E extends WorkerBug> implements Action<E> {

    private static final double MOVE_SPEED = 0.22D;

    private static final int DIG_TICKS = 15;

    private final int priority;

    public WorkerDigAction(int priority) {
        this.priority = priority;
    }

    @Override
    public void start(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        blackboard.remove(AiKeys.DIG_TARGET);
        blackboard.remove(AiKeys.DIG_TIMER);
    }

    @Override
    public ActionStatus tick(E mob, Blackboard bb, Cooldowns cd) {
        var colony = ColonyManager.get().colonyOf(mob);
        if (colony == null || colony.isDisbanded())
            return ActionStatus.FAILURE;

        if (!(mob.level() instanceof ServerLevel level))
            return ActionStatus.FAILURE;

        var targetBlock = bb.get(AiKeys.DIG_TARGET, BlockPos.class);
        var digTimer = bb.get(AiKeys.DIG_TIMER, Integer.class);

        if (digTimer == null)
            digTimer = 0;

        if (targetBlock == null) {
            targetBlock = colony.getTunnelDigger().claimNextDigTask(level);

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

        if (
            mob.distanceToSqr(
                targetBlock.getX() + 0.5,
                targetBlock.getY() + 0.5,
                targetBlock.getZ() + 0.5
            ) > 9.0
        ) {
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
                level.destroyBlock(targetBlock, false, mob);
            }

            colony.getTunnelDigger().completeTask(targetBlock);

            bb.remove(AiKeys.DIG_TARGET);
            bb.remove(AiKeys.DIG_TIMER);
        }

        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(E mob, Blackboard blackboard, ActionStatus reason) {
        var targetBlock = blackboard.get(AiKeys.DIG_TARGET, BlockPos.class);

        if (targetBlock != null) {
            var colony = ColonyManager.get().colonyOf(mob);
            if (colony != null)
                colony.getTunnelDigger().returnTask(targetBlock);

            blackboard.remove(AiKeys.DIG_TARGET);
        }

        blackboard.remove(AiKeys.DIG_TIMER);
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
}
