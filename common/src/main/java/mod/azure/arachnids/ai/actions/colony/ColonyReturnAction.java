package mod.azure.arachnids.ai.actions.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import mod.azure.arachnids.ai.core.*;

public class ColonyReturnAction<E extends PathfinderMob> implements Action<E> {

    private final int priority;

    private BlockPos centre;

    private Vec3 destination;

    public ColonyReturnAction(int priority) {
        this.priority = priority;
    }

    public void setCentre(BlockPos centre) {
        this.centre = centre;
        this.destination = null;
    }

    @Override
    public void start(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        if (centre == null) {
            return;
        }

        var surfaceY = mob.level()
            .getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                centre.getX(),
                centre.getZ()
            );

        var targetY = surfaceY + 12;
        this.destination = new Vec3(centre.getX(), targetY, centre.getZ());

        mob.getNavigation().stop();
        mob.setAggressive(false);
    }

    @Override
    public ActionStatus tick(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        var threat = blackboard.get(AiKeys.TARGET, LivingEntity.class);
        if (threat != null && threat.isAlive())
            return ActionStatus.INTERRUPTED;

        if (mob.getHealth() <= 0)
            return ActionStatus.INTERRUPTED;

        if (centre == null || destination == null)
            return ActionStatus.INTERRUPTED;

        var dx = destination.x - mob.getX();
        var dy = destination.y - mob.getY();
        var dz = destination.z - mob.getZ();

        var horizSq = dx * dx + dz * dz;
        if (horizSq < 144) {
            return ActionStatus.SUCCESS;
        }

        var horizLen = Math.sqrt(horizSq);
        var velX = (dx / horizLen) * 0.52D;
        var velZ = (dz / horizLen) * 0.52D;
        var velY = Math.clamp(dy * 0.4D, -0.52D, 0.52D);

        mob.setDeltaMovement(velX, velY, velZ);
        mob.hasImpulse = true;

        var yaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;

        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(E mob, Blackboard blackboard, ActionStatus reason) {
        mob.setXRot(0);
        destination = null;
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
