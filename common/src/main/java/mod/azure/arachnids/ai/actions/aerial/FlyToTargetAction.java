package mod.azure.arachnids.ai.actions.aerial;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.ai.util.AiDebugUtils;

public final class FlyToTargetAction<E extends Mob> implements Action<E> {

    private static final double OBSTACLE_PUSH = 0.18D;

    private final double stopDistanceSqr;

    private final double flightSpeed;

    private final double cruiseAltitude;

    private final int priority;

    public FlyToTargetAction(
        double stopDistance,
        double flightSpeed,
        double cruiseAltitude,
        int priority
    ) {
        this.stopDistanceSqr = stopDistance * stopDistance;
        this.flightSpeed = flightSpeed;
        this.cruiseAltitude = cruiseAltitude;
        this.priority = priority;
    }

    @Override
    public void start(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        mob.getNavigation().stop();
        mob.setAggressive(true);
    }

    @Override
    public ActionStatus tick(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        if (mob.getHealth() <= 0) {
            mob.setAggressive(false);
            return ActionStatus.INTERRUPTED;
        }

        var target = blackboard.get(AiKeys.TARGET, LivingEntity.class);
        if (target == null || !target.isAlive()) {
            return ActionStatus.FAILURE;
        }

        var mobPos = mob.position();

        var dx = target.getX() - mobPos.x;
        var dz = target.getZ() - mobPos.z;
        var horizLen = Math.sqrt(dx * dx + dz * dz);

        var horizDistSqr = dx * dx + dz * dz;
        if (horizDistSqr <= stopDistanceSqr) {
            if (mobPos.y >= target.getY() + (cruiseAltitude * 0.5D)) {
                return ActionStatus.SUCCESS;
            }
        }

        var verticalVel = 0D;

        var horizontalVelX = 0D;
        var horizontalVelZ = 0D;
        if (horizLen > 0.1D) {
            horizontalVelX = (dx / horizLen) * flightSpeed;
            horizontalVelZ = (dz / horizLen) * flightSpeed;
        }

        if (horizLen > 0.1D) {
            var eyeAhead = new Vec3(
                mob.getX() + (dx / horizLen) * 1.5D,
                mob.getEyeY(),
                mob.getZ() + (dz / horizLen) * 1.5D
            );
            var blockAhead = net.minecraft.core.BlockPos.containing(eyeAhead);
            if (!mob.level().getBlockState(blockAhead).getCollisionShape(mob.level(), blockAhead).isEmpty()) {
                verticalVel = OBSTACLE_PUSH;
            }
        }

        mob.setDeltaMovement(horizontalVelX, verticalVel, horizontalVelZ);
        mob.hasImpulse = true;

        faceTarget(mob, target);

        AiDebugUtils.sendParticlePath(
            mob,
            mob.position(),
            new Vec3(target.getX(), target.getY() + cruiseAltitude, target.getZ())
        );
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

    private void faceTarget(E mob, LivingEntity target) {
        double dx = target.getX() - mob.getX();
        double dy = target.getEyeY() - mob.getEyeY();
        double dz = target.getZ() - mob.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, horiz) * (180.0D / Math.PI));

        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
        mob.setXRot(pitch);
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }
}
