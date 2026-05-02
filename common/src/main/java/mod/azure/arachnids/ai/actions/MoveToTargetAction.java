package mod.azure.arachnids.ai.actions;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.ai.util.AiDebugUtils;
import mod.azure.arachnids.ai.util.MovementUtils;

public final class MoveToTargetAction<E extends Mob> implements Action<E> {

    private static final double LEDGE_CHECK_DISTANCE = 1.6D;

    private static final double MIN_LANDING_DISTANCE = 2.5D;

    private static final double MAX_LANDING_DISTANCE = 4.5D;

    private final double stopDistanceSqr;

    private final double speed;

    private final int priority;

    private final double maxLeapHeight = 5D;

    private final int[] steerBias = { 0 };

    public MoveToTargetAction(
        double stopDistance,
        double speed,
        int priority
    ) {
        this.stopDistanceSqr = stopDistance * stopDistance;
        this.speed = speed;
        this.priority = priority;
    }

    @Override
    public void start(E mob, Blackboard blackboard, Cooldowns cooldowns) {
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
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.5D));
            return ActionStatus.FAILURE;
        }

        var yDiff = target.getY() - mob.getY();

        if (yDiff > maxLeapHeight) {
            blackboard.set(AiKeys.TARGET, null);
            mob.setTarget(null);
            mob.setAggressive(false);
            mob.getNavigation().stop();
            return ActionStatus.FAILURE;
        }

        if (mob.distanceToSqr(target) <= stopDistanceSqr) {
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.4D));
            faceTarget(mob, target);
            return ActionStatus.SUCCESS;
        }

        var direction = target.position().subtract(mob.position());

        if (direction.lengthSqr() < 0.0001D) {
            return ActionStatus.SUCCESS;
        }

        return applyFlatMovement(mob, target, direction);
    }

    @Override
    public void stop(E mob, Blackboard blackboard, ActionStatus reason) {
        mob.setDeltaMovement(
            mob.getDeltaMovement().x * 0.25D,
            mob.getDeltaMovement().y,
            mob.getDeltaMovement().z * 0.25D
        );
    }

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public int priority() {
        return priority;
    }

    private ActionStatus applyFlatMovement(E mob, LivingEntity target, Vec3 direction) {
        var horizontal = new Vec3(direction.x, 0.0D, direction.z);

        if (horizontal.lengthSqr() < 0.01D) {
            halt(mob);
            return ActionStatus.RUNNING;
        }

        var movement = horizontal.normalize().scale(speed);
        var safe = MovementUtils.findSafeMovement(mob, movement, steerBias);

        if (safe.equals(Vec3.ZERO)) {
            halt(mob);
            return ActionStatus.RUNNING;
        }

        mob.setDeltaMovement(safe.x, mob.getDeltaMovement().y, safe.z);
        mob.hasImpulse = true;
        faceTarget(mob, target);

        AiDebugUtils.sendParticlePath(
            mob,
            mob.position(),
            target.position()
        );
        return ActionStatus.RUNNING;
    }

    private void halt(E mob) {
        mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);
        mob.hasImpulse = false;
    }

    private void faceTarget(E mob, LivingEntity target) {
        var dx = target.getX() - mob.getX();
        var dz = target.getZ() - mob.getZ();
        var yaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;

        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }
}
