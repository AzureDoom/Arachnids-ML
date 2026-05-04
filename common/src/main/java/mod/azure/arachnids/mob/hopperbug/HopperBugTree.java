package mod.azure.arachnids.mob.hopperbug;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import mod.azure.arachnids.ai.actions.*;
import mod.azure.arachnids.ai.actions.aerial.AerialRepositionAction;
import mod.azure.arachnids.ai.actions.aerial.AerialSwoopAction;
import mod.azure.arachnids.ai.actions.aerial.AerialWanderAction;
import mod.azure.arachnids.ai.actions.aerial.FlyToTargetAction;
import mod.azure.arachnids.ai.actions.colony.ColonyReturnAction;
import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.colony.ColonyManager;

public class HopperBugTree {

    public static BehaviorNode<HopperBug> create() {
        var fleeExplosive = new ExplosiveFleeAction<HopperBug>(
            0.55D,
            10,
            20,
            120
        );

        var swoop = new AerialSwoopAction<>(
            36,
            22,
            1.3D,
            0.85,
            80,
            100,
            b -> b.animationDispatcher.serverRangedAttack(),
            HopperBug::tryPickUpTarget
        );

        var reposition = new AerialRepositionAction<HopperBug>(
            0.825D,
            6,
            0.35,
            70
        );

        var flyToTarget = new FlyToTargetAction<HopperBug>(
            9D,
            0.55D,
            6,
            60
        );

        var aerialWander = new AerialWanderAction<HopperBug>(
            0.52,
            20,
            4,
            20,
            80,
            160,
            10
        );

        var returnToColony = new ColonyReturnAction<HopperBug>(15);

        return (hopper, blackboard, cooldowns) -> {

            if (fleeExplosive.hasNearbyExplosive(hopper)) {
                return BehaviorResult.run(fleeExplosive, 120);
            }

            var colony = ColonyManager.get().colonyOf(hopper);
            if (colony != null) {
                var centre = colony.getBounds().centre();
                var homeVec = new Vec3(centre.getX(), centre.getY(), centre.getZ());
                aerialWander.setHome(homeVec, 18D);
            } else {
                aerialWander.setHome(null, 20D);
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

                    if (distSqr > 144) {
                        returnToColony.setCentre(centre);
                        return BehaviorResult.run(returnToColony, 15);
                    }
                }

                return BehaviorResult.run(aerialWander, 10);
            }

            var mode = blackboard.get(AiKeys.HOPPER_COMBAT_MODE, HopperCombatMode.class);

            if (mode == null) {
                var distSqr = hopper.distanceToSqr(target);
                var tooClose = distSqr < 25;
                mode = (!tooClose && hopper.getRandom().nextDouble() < 0.7D)
                    ? HopperCombatMode.AERIAL_SWOOP
                    : HopperCombatMode.GROUND_MELEE;
                blackboard.set(AiKeys.HOPPER_COMBAT_MODE, mode);
            }

            if (hopper.isCarryingTarget()) {
                return BehaviorResult.run(reposition, 70);
            }

            if (cooldowns.isOnCooldown(AiKeys.SWOOP_COOLDOWN)) {
                return BehaviorResult.run(reposition, 70);
            }

            var distSqr = hopper.distanceToSqr(target);
            if (distSqr <= 81) {
                return BehaviorResult.run(swoop, 100);
            }

            return BehaviorResult.run(flyToTarget, 60);
        };
    }

    private HopperBugTree() {}
}
