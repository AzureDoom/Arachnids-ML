package mod.azure.arachnids.ai.actions;

import net.minecraft.world.phys.Vec3;

import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.ai.core.Action;
import mod.azure.arachnids.ai.core.ActionStatus;
import mod.azure.arachnids.ai.core.Blackboard;
import mod.azure.arachnids.ai.core.Cooldowns;
import mod.azure.arachnids.ai.transport.BrainBugCarrySystem;
import mod.azure.arachnids.ai.util.MovementUtils;
import mod.azure.arachnids.mob.brainbug.BrainBug;
import mod.azure.arachnids.mob.chariotbug.ChariotBug;

public final class CarryBrainBugAction implements Action<ChariotBug> {

    private static final double FORMATION_RADIUS = 0.9D;

    private static final double FORMATION_SPEED = 0.22D;

    private static final double THREAT_SENSE_RADIUS = 24.0D;

    private final SeekBrainBugAction seekAction;

    private final int priority;

    private final int[] steerBias = { 0 };

    private int slotIndex = 0;

    private BrainBug brain;

    public CarryBrainBugAction(SeekBrainBugAction seekAction, int priority) {
        this.seekAction = seekAction;
        this.priority = priority;
    }

    @Override
    public void start(ChariotBug mob, Blackboard blackboard, Cooldowns cooldowns) {
        brain = seekAction.getTarget();

        if (brain == null || !brain.isAlive())
            return;

        var count = BrainBugCarrySystem.get().carrierCount(brain);
        slotIndex = Math.max(0, count - 1) % BrainBugCarrySystem.MAX_CARRIERS;

        mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);
    }

    @Override
    public ActionStatus tick(ChariotBug mob, Blackboard blackboard, Cooldowns cooldowns) {
        if (mob.getHealth() <= 0) {
            BrainBugCarrySystem.get().unregister(mob);
            return ActionStatus.INTERRUPTED;
        }

        if (
            brain == null || !brain.isAlive()
                || !BrainBugCarrySystem.get().isRegistered(mob)
        ) {
            return ActionStatus.FAILURE;
        }

        if (BrainBugCarrySystem.get().canMoveCarried(brain)) {
            var pushDir = computeFleeDirection(mob);
            if (pushDir != null) {
                BrainBugCarrySystem.get().pushCarried(mob, pushDir);

                mob.setDeltaMovement(
                    pushDir.x * FORMATION_SPEED,
                    mob.getDeltaMovement().y,
                    pushDir.z * FORMATION_SPEED
                );
                mob.hasImpulse = true;
                faceDirection(mob, pushDir);
            }
        } else {
            BrainBugCarrySystem.get().unregister(mob);
            mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);
            brain = null;
            return ActionStatus.SUCCESS;
        }

        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(ChariotBug mob, Blackboard blackboard, ActionStatus reason) {
        BrainBugCarrySystem.get().unregister(mob);
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

    private Vec3 computeFormationSlot() {
        var total = BrainBugCarrySystem.MAX_CARRIERS;
        var angle = (2.0D * Math.PI * slotIndex) / total
            + Math.toRadians(brain.getYRot());
        return brain.position()
            .add(
                Math.sin(angle) * FORMATION_RADIUS,
                0.0D,
                Math.cos(angle) * FORMATION_RADIUS
            );
    }

    private void moveToward(ChariotBug mob, Vec3 target, double speed) {
        var delta = target.subtract(mob.position());
        if (delta.horizontalDistanceSqr() < 0.04D) {
            mob.setDeltaMovement(
                mob.getDeltaMovement().x * 0.5D,
                mob.getDeltaMovement().y,
                mob.getDeltaMovement().z * 0.5D
            );
            return;
        }
        var desired = delta.normalize().scale(speed);
        var safe = MovementUtils.findSafeMovement(mob, desired, steerBias);
        if (!safe.equals(Vec3.ZERO)) {
            mob.setDeltaMovement(safe.x, mob.getDeltaMovement().y, safe.z);
            mob.hasImpulse = true;
            faceDirection(mob, safe);
        }
    }

    private Vec3 computeFleeDirection(ChariotBug mob) {
        var origin = BrainBugCarrySystem.get().getThreatOrigin(brain);
        if (origin == null)
            return null;

        var away = brain.position().subtract(origin);

        if (away.horizontalDistanceSqr() < 0.0001D) {
            double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
            return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        }

        return new Vec3(away.x, 0.0D, away.z).normalize();
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
