package mod.azure.arachnids.ai.actions.aerial;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.ai.util.AiDebugUtils;

public final class AerialWanderAction<E extends Mob> implements Action<E> {

    private static final int DESTINATION_ATTEMPTS = 12;

    private final double flightSpeed;

    private final double horizRadius;

    private final double minAltAboveSurface;

    private final double maxAltAboveSurface;

    private final int minDuration;

    private final int maxDuration;

    private final int priority;

    private Vec3 home = null;

    private double homeRadius = 20.0;

    private Vec3 destination;

    private int age;

    private int duration;

    public AerialWanderAction(
        double flightSpeed,
        double horizRadius,
        double minAltAboveSurface,
        double maxAltAboveSurface,
        int minDuration,
        int maxDuration,
        int priority
    ) {
        this.flightSpeed = flightSpeed;
        this.horizRadius = horizRadius;
        this.minAltAboveSurface = minAltAboveSurface;
        this.maxAltAboveSurface = maxAltAboveSurface;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
        this.priority = priority;
    }

    public void setHome(Vec3 home, double homeRadius) {
        this.home = home;
        this.homeRadius = homeRadius;
    }

    @Override
    public void start(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        this.age = 0;
        this.duration = minDuration + mob.getRandom().nextInt(Math.max(1, maxDuration - minDuration));
        this.destination = pickDestination(mob);
        mob.getNavigation().stop();
        mob.setAggressive(false);
    }

    @Override
    public ActionStatus tick(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        if (mob.getHealth() <= 0) {
            return ActionStatus.INTERRUPTED;
        }

        var threat = blackboard.get(AiKeys.TARGET, LivingEntity.class);
        if (threat != null && threat.isAlive()) {
            return ActionStatus.INTERRUPTED;
        }

        age++;

        if (destination == null) {
            destination = pickDestination(mob);
        }

        var surfaceY = getSurfaceY(mob, mob.getX(), mob.getZ());
        var absoluteCeiling = surfaceY + maxAltAboveSurface;
        if (mob.getY() > absoluteCeiling + 1.0) {
            destination = new Vec3(
                mob.getX() + (mob.getRandom().nextDouble() * 2 - 1) * 4,
                surfaceY + minAltAboveSurface,
                mob.getZ() + (mob.getRandom().nextDouble() * 2 - 1) * 4
            );
        }

        var delta = destination.subtract(mob.position());
        var distSqr = delta.lengthSqr();

        if (distSqr < 1.5D || age >= duration) {
            slowDown(mob);
            return ActionStatus.SUCCESS;
        }

        var dx = delta.x;
        var dy = delta.y;
        var dz = delta.z;
        var horizLen = Math.sqrt(dx * dx + dz * dz);

        double velX = 0, velZ = 0;
        if (horizLen > 0.1D) {
            velX = (dx / horizLen) * flightSpeed;
            velZ = (dz / horizLen) * flightSpeed;
        }

        var maxVert = flightSpeed;
        var velY = Math.clamp(dy * 0.5D, -maxVert, maxVert);

        mob.setDeltaMovement(velX, velY, velZ);
        mob.hasImpulse = true;

        if (horizLen > 0.1D) {
            float yaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            mob.setYRot(yaw);
            mob.yBodyRot = yaw;
            mob.yHeadRot = yaw;
        }

        float targetPitch = velY > 0.05D ? -15.0F : (velY < -0.05D ? 10.0F : 0.0F);
        mob.setXRot(mob.getXRot() + (targetPitch - mob.getXRot()) * 0.1F);

        AiDebugUtils.sendParticlePath(
            mob,
            mob.position(),
            destination
        );
        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(E mob, Blackboard blackboard, ActionStatus reason) {
        slowDown(mob);
        mob.setXRot(0);
    }

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public int priority() {
        return priority;
    }

    private Vec3 pickDestination(E mob) {
        double originX = (home != null) ? home.x : mob.getX();
        double originZ = (home != null) ? home.z : mob.getZ();
        double radius = (home != null) ? Math.min(horizRadius, homeRadius) : horizRadius;

        for (int i = 0; i < DESTINATION_ATTEMPTS; i++) {
            double xOff = (mob.getRandom().nextDouble() * 2.0 - 1.0) * radius;
            double zOff = (mob.getRandom().nextDouble() * 2.0 - 1.0) * radius;

            double candidateX = originX + xOff;
            double candidateZ = originZ + zOff;

            double surfaceY = getSurfaceY(mob, candidateX, candidateZ);
            double yOff = minAltAboveSurface
                + mob.getRandom().nextDouble() * (maxAltAboveSurface - minAltAboveSurface);
            double candidateY = surfaceY + yOff;

            Vec3 candidate = new Vec3(candidateX, candidateY, candidateZ);

            var blockPos = BlockPos.containing(candidate);
            if (mob.level().getBlockState(blockPos).getCollisionShape(mob.level(), blockPos).isEmpty()) {
                return candidate;
            }
        }

        double surfaceY = getSurfaceY(mob, mob.getX(), mob.getZ());
        return new Vec3(mob.getX(), surfaceY + (minAltAboveSurface + maxAltAboveSurface) * 0.5D, mob.getZ());
    }

    private double getSurfaceY(E mob, double x, double z) {
        return mob.level()
            .getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(x),
                (int) Math.floor(z)
            );
    }

    private void slowDown(E mob) {
        Vec3 vel = mob.getDeltaMovement();
        mob.setDeltaMovement(vel.x * 0.4D, vel.y * 0.6D, vel.z * 0.4D);
    }
}
