package mod.azure.arachnids.mob.chariotbug;

import net.minecraft.world.entity.LivingEntity;

import mod.azure.arachnids.ai.actions.*;
import mod.azure.arachnids.ai.core.AiKeys;
import mod.azure.arachnids.ai.core.BehaviorNode;
import mod.azure.arachnids.ai.core.BehaviorResult;
import mod.azure.arachnids.ai.transport.BrainBugCarrySystem;

public class ChariotBugTree {

    private static final double FLEE_SAFE_DISTANCE = 20.0D;

    public static BehaviorNode<ChariotBug> create() {
        var fleeExplosive = new ExplosiveFleeAction<ChariotBug>(
            0.18D,
            10,
            FLEE_SAFE_DISTANCE,
            120
        );

        var seek = new SeekBrainBugAction(
            0.14D,
            30
        );

        var carry = new CarryBrainBugAction(seek, 40);

        var flee = new FleeAction<ChariotBug>(
            0.18D,
            FLEE_SAFE_DISTANCE,
            70
        );

        var wander = new WanderAction<ChariotBug>(
            0.10D,
            10,
            12.0D,
            80,
            180
        );

        var idle = new IdleAction<ChariotBug>(40, 120, 1);

        return (mob, blackboard, cooldowns) -> {

            if (fleeExplosive.hasNearbyExplosive(mob)) {
                return BehaviorResult.run(fleeExplosive, 120);
            }

            var threat = blackboard.get(AiKeys.TARGET, LivingEntity.class);
            var hasThreat = threat != null && threat.isAlive();
            var isCarrying = BrainBugCarrySystem.get().isRegistered(mob);

            if (hasThreat && !isCarrying) {
                return BehaviorResult.run(flee, 70);
            }

            if (isCarrying) {
                return BehaviorResult.run(carry, 40);
            }

            if (!hasThreat && BrainBugCarrySystem.get().wantsCarriersExist()) {
                return BehaviorResult.run(seek, 30);
            }

            return BehaviorResult.run(wander, 10);
        };
    }
}
