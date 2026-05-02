package mod.azure.arachnids.ai.actions;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.ai.transport.BrainBugCarrySystem;
import mod.azure.arachnids.ai.util.MovementUtils;
import mod.azure.arachnids.mob.brainbug.BrainBug;
import mod.azure.arachnids.mob.chariotbug.ChariotBug;

public final class SeekBrainBugAction implements Action<ChariotBug> {

    private static final double SEARCH_RADIUS = 32.0D;

    private static final double MOUNT_DISTANCE = 1.8D;

    private static final double MOUNT_DISTANCE_SQ = MOUNT_DISTANCE * MOUNT_DISTANCE;

    private final double speed;

    private final int priority;

    private final int[] steerBias = { 0 };

    private BrainBug target;

    public SeekBrainBugAction(double speed, int priority) {
        this.speed = speed;
        this.priority = priority;
    }

    @Override
    public void start(ChariotBug mob, Blackboard blackboard, Cooldowns cooldowns) {
        this.target = findBrainBug(mob);
    }

    @Override
    public ActionStatus tick(ChariotBug mob, Blackboard blackboard, Cooldowns cooldowns) {
        if (mob.getHealth() <= 0)
            return ActionStatus.INTERRUPTED;

        var threat = blackboard.get(AiKeys.TARGET, LivingEntity.class);
        if (threat != null && threat.isAlive())
            return ActionStatus.INTERRUPTED;

        if (
            target == null || !target.isAlive()
                || !BrainBugCarrySystem.get().hasOpenSlot(target)
        ) {
            target = findBrainBug(mob);
            if (target == null)
                return ActionStatus.FAILURE;
        }

        var distSq = mob.distanceToSqr(target);

        if (distSq <= MOUNT_DISTANCE_SQ) {
            var registered = BrainBugCarrySystem.get().tryRegister(target, mob);
            return registered ? ActionStatus.SUCCESS : ActionStatus.FAILURE;
        }

        var direction = target.position().subtract(mob.position());
        var movement = direction.normalize().scale(speed);
        var safe = MovementUtils.findSafeMovement(mob, movement, steerBias);

        if (safe.equals(Vec3.ZERO)) {
            return ActionStatus.RUNNING;
        }

        mob.setDeltaMovement(safe.x, mob.getDeltaMovement().y, safe.z);
        mob.hasImpulse = true;
        faceDirection(mob, safe);

        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(ChariotBug mob, Blackboard blackboard, ActionStatus reason) {
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

    public BrainBug getTarget() {
        return target;
    }

    private BrainBug findBrainBug(ChariotBug mob) {
        var candidates = mob.level()
            .getEntitiesOfClass(
                BrainBug.class,
                new AABB(mob.blockPosition()).inflate(SEARCH_RADIUS),
                b -> b.isAlive() && BrainBugCarrySystem.get().hasOpenSlot(b)
            );

        if (candidates.isEmpty())
            return null;

        BrainBug closest = null;
        var minDist = Double.MAX_VALUE;
        for (var b : candidates) {
            var d = mob.distanceToSqr(b);
            if (d < minDist) {
                minDist = d;
                closest = b;
            }
        }
        return closest;
    }

    private void faceDirection(ChariotBug mob, Vec3 dir) {
        if (dir.horizontalDistanceSqr() < 0.0001D)
            return;
        var yaw = (float) (Math.atan2(dir.z, dir.x) * (180.0D / Math.PI)) - 90.0F;
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
    }
}
