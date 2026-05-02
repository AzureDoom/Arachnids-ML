package mod.azure.arachnids.mob.hopperbug;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import mod.azure.arachnids.ai.actions.*;
import mod.azure.arachnids.ai.actions.aerial.AerialRepositionAction;
import mod.azure.arachnids.ai.actions.aerial.AerialSwoopAction;
import mod.azure.arachnids.ai.actions.aerial.AerialWanderAction;
import mod.azure.arachnids.ai.actions.aerial.FlyToTargetAction;
import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.colony.ColonyManager;

public class HopperBugTree {

    private static final double FLIGHT_SPEED = 0.55D;

    private static final double CRUISE_ALTITUDE = 6.0D;

    private static final double CLIMB_RATE = 0.35D;

    private static final int SWOOP_TOTAL_TICKS = 36;

    private static final int SWOOP_DIVE_TICKS = 22;

    private static final double SWOOP_DIVE_SPEED = 1.3D;

    private static final double SWOOP_PULLUP_POWER = 0.85D;

    private static final int SWOOP_COOLDOWN_TICKS = 80;

    private static final double MIN_SWOOP_DISTANCE = 5.0D;

    private static final double SWOOP_TRIGGER_DISTANCE = 9.0D;

    private static final double GROUND_SPEED = 0.28D;

    private static final double MELEE_STOP_DISTANCE = 1.2D;

    private static final int MELEE_TOTAL_TICKS = 28;

    private static final int MELEE_DAMAGE_TICK = 16;

    private static final int MELEE_COOLDOWN_TICKS = 30;

    private static final double AERIAL_WANDER_SPEED = 0.52D;

    private static final double AERIAL_WANDER_RADIUS = 20.0D;

    private static final double MIN_ALT_ABOVE_SURFACE = 4.0D;

    private static final double MAX_ALT_ABOVE_SURFACE = 20.0D;

    private static final int AERIAL_WANDER_MIN = 80;

    private static final int AERIAL_WANDER_MAX = 160;

    private static final double AERIAL_PREFERENCE = 0.70D;

    private static final double COLONY_WANDER_RADIUS = 18.0D;

    private static final double COLONY_RETURN_THRESHOLD = 12.0D;

    public static BehaviorNode<HopperBug> create() {
        var fleeExplosive = new ExplosiveFleeAction<HopperBug>(
            FLIGHT_SPEED,
            10,
            20,
            120
        );

        var swoop = new AerialSwoopAction<HopperBug>(
            SWOOP_TOTAL_TICKS,
            SWOOP_DIVE_TICKS,
            SWOOP_DIVE_SPEED,
            SWOOP_PULLUP_POWER,
            SWOOP_COOLDOWN_TICKS,
            100,
            b -> b.animationDispatcher.serverRangedAttack()
        );

        var reposition = new AerialRepositionAction<HopperBug>(
            FLIGHT_SPEED * 1.5,
            CRUISE_ALTITUDE,
            CLIMB_RATE,
            70
        );

        var flyToTarget = new FlyToTargetAction<HopperBug>(
            SWOOP_TRIGGER_DISTANCE,
            FLIGHT_SPEED,
            CRUISE_ALTITUDE,
            60
        );

        var groundAttack = new TimedAttackAction<HopperBug>(
            AiKeys.HOPPER_MELEE_COOLDOWN,
            MELEE_COOLDOWN_TICKS,
            MELEE_TOTAL_TICKS,
            MELEE_DAMAGE_TICK,
            50,
            b -> b.animationDispatcher.serverNormalAttack()
        );

        var groundChase = new MoveToTargetAction<HopperBug>(
            MELEE_STOP_DISTANCE,
            GROUND_SPEED,
            35
        );

        var aerialWander = new AerialWanderAction<HopperBug>(
            AERIAL_WANDER_SPEED,
            AERIAL_WANDER_RADIUS,
            MIN_ALT_ABOVE_SURFACE,
            MAX_ALT_ABOVE_SURFACE,
            AERIAL_WANDER_MIN,
            AERIAL_WANDER_MAX,
            10
        );

        return (hopper, blackboard, cooldowns) -> {

            if (fleeExplosive.hasNearbyExplosive(hopper)) {
                return BehaviorResult.run(fleeExplosive, 120);
            }

            var colony = ColonyManager.get().colonyOf(hopper);
            if (colony != null) {
                var centre = colony.getBounds().centre();
                var homeVec = new Vec3(centre.getX(), centre.getY(), centre.getZ());
                aerialWander.setHome(homeVec, COLONY_WANDER_RADIUS);
            } else {
                aerialWander.setHome(null, AERIAL_WANDER_RADIUS);
            }

            var target = blackboard.get(AiKeys.TARGET, LivingEntity.class);
            var hasThreat = target != null && target.isAlive();

            if (!hasThreat) {
                blackboard.remove(AiKeys.HOPPER_COMBAT_MODE);

                if (colony != null) {
                    var centre = colony.getBounds().centre();
                    var dx = hopper.getX() - centre.getX();
                    var dz = hopper.getZ() - centre.getZ();
                    var distSqr = dx * dx + dz * dz;

                    if (distSqr > COLONY_RETURN_THRESHOLD * COLONY_RETURN_THRESHOLD) {
                        return BehaviorResult.run(
                            buildReturnToColonyAction(centre, hopper),
                            15
                        );
                    }
                }

                return BehaviorResult.run(aerialWander, 10);
            }

            var mode = blackboard.get(AiKeys.HOPPER_COMBAT_MODE, HopperCombatMode.class);

            if (mode == null) {
                var distSqr = hopper.distanceToSqr(target);
                var tooClose = distSqr < (MIN_SWOOP_DISTANCE * MIN_SWOOP_DISTANCE);
                mode = (!tooClose && hopper.getRandom().nextDouble() < AERIAL_PREFERENCE)
                    ? HopperCombatMode.AERIAL_SWOOP
                    : HopperCombatMode.GROUND_MELEE;
                blackboard.set(AiKeys.HOPPER_COMBAT_MODE, mode);
            }

            if (mode == HopperCombatMode.AERIAL_SWOOP) {

                if (cooldowns.isOnCooldown(AiKeys.SWOOP_COOLDOWN)) {
                    return BehaviorResult.run(reposition, 70);
                }

                var distSqr = hopper.distanceToSqr(target);
                if (distSqr <= (SWOOP_TRIGGER_DISTANCE * SWOOP_TRIGGER_DISTANCE)) {
                    return BehaviorResult.run(swoop, 100);
                }

                return BehaviorResult.run(flyToTarget, 60);
            }

            if (cooldowns.ready(AiKeys.HOPPER_MELEE_COOLDOWN)) {
                var distSqr = hopper.distanceToSqr(target);
                if (distSqr <= (MELEE_STOP_DISTANCE * MELEE_STOP_DISTANCE)) {
                    return BehaviorResult.run(groundAttack, 50);
                }
            }

            return BehaviorResult.run(groundChase, 35);
        };
    }

    private static Action<HopperBug> buildReturnToColonyAction(BlockPos centre, HopperBug hopper) {
        var surfaceY = hopper.level()
            .getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                centre.getX(),
                centre.getZ()
            );
        var targetY = surfaceY + MIN_ALT_ABOVE_SURFACE + (MAX_ALT_ABOVE_SURFACE - MIN_ALT_ABOVE_SURFACE) * 0.5;
        var destination = new Vec3(centre.getX(), targetY, centre.getZ());

        return new Action<>() {

            @Override
            public void start(HopperBug mob, Blackboard bb, Cooldowns cd) {
                mob.getNavigation().stop();
                mob.setAggressive(false);
            }

            @Override
            public ActionStatus tick(HopperBug mob, Blackboard bb, Cooldowns cd) {
                var threat = bb.get(AiKeys.TARGET, LivingEntity.class);
                if (threat != null && threat.isAlive())
                    return ActionStatus.INTERRUPTED;
                if (mob.getHealth() <= 0)
                    return ActionStatus.INTERRUPTED;

                var dx = destination.x - mob.getX();
                var dy = destination.y - mob.getY();
                var dz = destination.z - mob.getZ();

                var horizSq = dx * dx + dz * dz;
                if (horizSq < COLONY_RETURN_THRESHOLD * COLONY_RETURN_THRESHOLD) {
                    return ActionStatus.SUCCESS;
                }

                var horizLen = Math.sqrt(horizSq);
                var velX = (dx / horizLen) * AERIAL_WANDER_SPEED;
                var velZ = (dz / horizLen) * AERIAL_WANDER_SPEED;
                var velY = Math.clamp(dy * 0.4, -AERIAL_WANDER_SPEED, AERIAL_WANDER_SPEED);

                mob.setDeltaMovement(velX, velY, velZ);
                mob.hasImpulse = true;

                var yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                mob.setYRot(yaw);
                mob.yBodyRot = yaw;
                mob.yHeadRot = yaw;

                return ActionStatus.RUNNING;
            }

            @Override
            public void stop(HopperBug mob, Blackboard bb, ActionStatus reason) {
                mob.setXRot(0);
            }

            @Override
            public boolean isInterruptible() {
                return true;
            }

            @Override
            public int priority() {
                return 15;
            }
        };
    }

    private HopperBugTree() {}
}
