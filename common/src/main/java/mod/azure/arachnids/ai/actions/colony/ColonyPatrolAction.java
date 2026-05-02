package mod.azure.arachnids.ai.actions.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

import mod.azure.arachnids.ai.core.Action;
import mod.azure.arachnids.ai.core.ActionStatus;
import mod.azure.arachnids.ai.core.Blackboard;
import mod.azure.arachnids.ai.core.Cooldowns;
import mod.azure.arachnids.colony.BugColony;
import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.colony.ColonyState;
import mod.azure.arachnids.mob.warriorbug.WarriorBug;

public class ColonyPatrolAction<E extends WarriorBug> implements Action<E> {

    private static final double PATROL_SPEED = 0.22D;

    private static final int ARRIVAL_SQ = 9;

    private final int priority;

    private BlockPos patrolDest = null;

    public ColonyPatrolAction(int priority) {
        this.priority = priority;
    }

    @Override
    public void start(E mob, Blackboard bb, Cooldowns cd) {
        patrolDest = pickPatrolPoint(mob);
        if (patrolDest != null) {
            mob.getNavigation()
                .moveTo(patrolDest.getX() + 0.5, patrolDest.getY(), patrolDest.getZ() + 0.5, PATROL_SPEED);
        }
    }

    @Override
    public ActionStatus tick(E mob, Blackboard bb, Cooldowns cd) {
        var colony = findColony(mob);
        if (colony == null || colony.isDisbanded())
            return ActionStatus.FAILURE;

        if (colony.getWarriorDirective() != null)
            return ActionStatus.SUCCESS;

        if (colony.getState() == ColonyState.PANIC)
            return ActionStatus.SUCCESS;

        if (
            patrolDest == null || mob.distanceToSqr(
                patrolDest.getX(),
                patrolDest.getY(),
                patrolDest.getZ()
            ) < ARRIVAL_SQ
        ) {
            patrolDest = pickPatrolPoint(mob);
            if (patrolDest != null) {
                mob.getNavigation()
                    .moveTo(patrolDest.getX() + 0.5, patrolDest.getY(), patrolDest.getZ() + 0.5, PATROL_SPEED);
            }
        }

        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(E mob, Blackboard bb, ActionStatus status) {
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

    private BlockPos pickPatrolPoint(E mob) {
        var colony = findColony(mob);
        if (colony == null)
            return null;

        var centre = colony.patrolCentre();
        var rng = mob.getRandom();
        var r = rng.nextInt(12) + 4;
        var angle = rng.nextDouble() * Math.PI * 2;
        var x = centre.getX() + (int) (Math.cos(angle) * r);
        var z = centre.getZ() + (int) (Math.sin(angle) * r);

        var y = mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return BlockPos.containing(x, y, z);
    }

    private BugColony findColony(E mob) {
        return ColonyManager.get().colonyOf(mob);
    }
}
