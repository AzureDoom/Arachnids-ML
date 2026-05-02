package mod.azure.arachnids.ai.actions;

import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.ai.core.Action;
import mod.azure.arachnids.ai.core.ActionStatus;
import mod.azure.arachnids.ai.core.Blackboard;
import mod.azure.arachnids.ai.core.Cooldowns;
import mod.azure.arachnids.ai.transport.BrainBugCarrySystem;
import mod.azure.arachnids.mob.brainbug.BrainBug;

public final class BeCarriedAction implements Action<BrainBug> {

    private static final int PRIORITY = 50;

    private final BrainBugCarrySystem carrySystem = BrainBugCarrySystem.get();

    @Override
    public void start(BrainBug mob, Blackboard blackboard, Cooldowns cooldowns) {
        mob.getNavigation().stop();
    }

    @Override
    public ActionStatus tick(BrainBug mob, Blackboard blackboard, Cooldowns cooldowns) {
        if (mob.getHealth() <= 0)
            return ActionStatus.INTERRUPTED;

        if (!carrySystem.isCarried(mob)) {
            return ActionStatus.FAILURE;
        }

        mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);

        carrySystem.applyPushes(mob);

        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(BrainBug mob, Blackboard blackboard, ActionStatus reason) {
        mob.setDeltaMovement(
            mob.getDeltaMovement().x * 0.1D,
            mob.getDeltaMovement().y,
            mob.getDeltaMovement().z * 0.1D
        );
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }
}
