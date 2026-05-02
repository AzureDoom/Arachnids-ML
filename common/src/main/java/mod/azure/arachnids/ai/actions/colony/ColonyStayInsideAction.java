package mod.azure.arachnids.ai.actions.colony;

import net.minecraft.world.entity.PathfinderMob;

import mod.azure.arachnids.ai.core.Action;
import mod.azure.arachnids.ai.core.ActionStatus;
import mod.azure.arachnids.ai.core.Blackboard;
import mod.azure.arachnids.ai.core.Cooldowns;
import mod.azure.arachnids.colony.ColonyManager;

public class ColonyStayInsideAction<E extends PathfinderMob> implements Action<E> {

    private final int priority;

    private static final double RETURN_SPEED = 0.28D;

    public ColonyStayInsideAction(int priority) {
        this.priority = priority;
    }

    @Override
    public void start(E mob, Blackboard blackboard, Cooldowns cooldowns) {}

    @Override
    public ActionStatus tick(E mob, Blackboard bb, Cooldowns cd) {
        var colony = ColonyManager.get().colonyOf(mob);
        if (colony == null || colony.isDisbanded())
            return ActionStatus.FAILURE;

        if (colony.getBounds().isInsideTerritory(mob.blockPosition())) {
            mob.getNavigation().stop();
            return ActionStatus.FAILURE;
        }

        var centre = colony.getBounds().centre();
        mob.getNavigation().moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, RETURN_SPEED);
        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(E mob, Blackboard blackboard, ActionStatus reason) {}

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public int priority() {
        return priority;
    }
}
