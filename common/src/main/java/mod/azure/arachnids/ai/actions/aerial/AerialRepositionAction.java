package mod.azure.arachnids.ai.actions.aerial;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.ai.util.AiDebugUtils;

public final class AerialRepositionAction<E extends Mob> implements Action<E> {

    private static final double ORBIT_SPEED_FRACTION = 0.65D;

    private static final double ALTITUDE_HOLD_DAMP = 0.3D;

    private final double flightSpeed;

    private final double cruiseAltitude;

    private final double climbRate;

    private final int priority;

    private int orbitSign;

    public AerialRepositionAction(
        double flightSpeed,
        double cruiseAltitude,
        double climbRate,
        int priority
    ) {
        this.flightSpeed = flightSpeed;
        this.cruiseAltitude = cruiseAltitude;
        this.climbRate = climbRate;
        this.priority = priority;
    }

    @Override
    public void start(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        mob.getNavigation().stop();
        orbitSign = mob.getRandom().nextBoolean() ? 1 : -1;
        var targetY = getTargetY(mob, blackboard);
        blackboard.set(AiKeys.HOPPER_CRUISE_ALTITUDE, targetY + cruiseAltitude);
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

        var targetCruiseY = target.getY() + cruiseAltitude;
        var mobY = mob.getY();
        var yDiff = targetCruiseY - mobY;

        double verticalVel;
        if (yDiff > 0.8D) {
            verticalVel = Math.min(climbRate, yDiff * 0.2D);
        } else if (yDiff < -1.5D) {
            verticalVel = Math.max(-climbRate * ALTITUDE_HOLD_DAMP, yDiff * 0.05D);
        } else {
            verticalVel = mob.getDeltaMovement().y * 0.6D;
        }

        var dx = target.getX() - mob.getX();
        var dz = target.getZ() - mob.getZ();
        var horizLen = Math.sqrt(dx * dx + dz * dz);

        var orbitX = 0D;
        var orbitZ = 0D;
        if (horizLen > 0.5D) {
            orbitX = (-dz / horizLen) * orbitSign * flightSpeed * ORBIT_SPEED_FRACTION;
            orbitZ = (dx / horizLen) * orbitSign * flightSpeed * ORBIT_SPEED_FRACTION;

            var minOrbitRadius = 8.0D;
            if (horizLen < minOrbitRadius) {
                orbitX += (-dx / horizLen) * flightSpeed * 0.35D;
                orbitZ += (-dz / horizLen) * flightSpeed * 0.35D;
            }
        }

        mob.setDeltaMovement(orbitX, verticalVel, orbitZ);
        mob.hasImpulse = true;

        if (Math.abs(orbitX) + Math.abs(orbitZ) > 0.01D) {
            var yaw = (float) (Math.atan2(orbitZ, orbitX) * (180.0D / Math.PI)) - 90.0F;
            mob.setYRot(yaw);
            mob.yBodyRot = yaw;
            mob.yHeadRot = yaw;
        }

        var targetPitch = verticalVel > 0.05D ? -20.0F : (verticalVel < -0.05D ? 15.0F : 0.0F);
        mob.setXRot(mob.getXRot() + (targetPitch - mob.getXRot()) * 0.15F);

        if (cooldowns.ready(AiKeys.SWOOP_COOLDOWN) && yDiff <= 1.0D) {
            return ActionStatus.SUCCESS;
        }

        AiDebugUtils.sendParticlePath(
            mob,
            mob.position(),
            new Vec3(target.getX(), target.getY() + cruiseAltitude, target.getZ())
        );
        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(E mob, Blackboard blackboard, ActionStatus reason) {
        mob.setXRot(0);
        var vel = mob.getDeltaMovement();
        mob.setDeltaMovement(vel.x * 0.5D, vel.y * 0.7D, vel.z * 0.5D);
    }

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public int priority() {
        return priority;
    }

    private double getTargetY(E mob, Blackboard blackboard) {
        var target = blackboard.get(AiKeys.TARGET, LivingEntity.class);
        return target != null ? target.getY() : mob.getY();
    }
}
