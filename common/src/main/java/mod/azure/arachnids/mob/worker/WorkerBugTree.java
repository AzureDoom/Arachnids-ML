package mod.azure.arachnids.mob.worker;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import mod.azure.arachnids.ai.actions.*;
import mod.azure.arachnids.ai.actions.colony.ColonyStayInsideAction;
import mod.azure.arachnids.ai.core.AiKeys;
import mod.azure.arachnids.ai.core.BehaviorNode;
import mod.azure.arachnids.ai.core.BehaviorResult;
import mod.azure.arachnids.ai.core.Blackboard;
import mod.azure.arachnids.colony.ColonyManager;

public class WorkerBugTree {

    private static final double WANDER_SPEED = 0.18D;

    private static final double FLEE_SPEED = 0.32D;

    private static final double FLEE_SAFE_DIST = 20.0D;

    private static final int CORNER_TOTAL = 14;

    private static final int CORNER_DAMAGE_T = 9;

    private static final int CORNER_COOLDOWN = 60;

    public static BehaviorNode<WorkerBug> create() {
        var fleeExplosive = new ExplosiveFleeAction<WorkerBug>(
            FLEE_SPEED,
            10,
            20,
            120
        );

        var cornerFight = new CornerFightAction<WorkerBug>(
            AiKeys.CORNERED_ATTACK_COOLDOWN,
            CORNER_COOLDOWN,
            CORNER_TOTAL,
            CORNER_DAMAGE_T,
            90,
            b -> b.animationDispatcher.serverLightAttack()
        );

        var flee = new FleeAction<WorkerBug>(FLEE_SPEED, FLEE_SAFE_DIST, 50);
        var dig = new WorkerDigAction<>(25);
        var stayInside = new ColonyStayInsideAction<WorkerBug>(80);
        var wander = new WanderAction<WorkerBug>(WANDER_SPEED, 5, 10.0D, 60, 160);
        var idle = new IdleAction<WorkerBug>(40, 100, 1);

        return (bug, blackboard, cooldowns) -> {

            if (fleeExplosive.hasNearbyExplosive(bug)) {
                return BehaviorResult.run(fleeExplosive, 120);
            }

            var threat = blackboard.get(AiKeys.TARGET, LivingEntity.class);
            var hasThreat = threat != null && threat.isAlive();
            var isCornered = Boolean.TRUE.equals(blackboard.get(AiKeys.IS_CORNERED, Boolean.class));

            if (hasThreat && isCornered && cooldowns.ready(AiKeys.CORNERED_ATTACK_COOLDOWN)) {
                return BehaviorResult.run(cornerFight, 90);
            }

            var colony = ColonyManager.get().colonyOf(bug);
            if (
                colony != null && !colony.isDisbanded()
                    && !colony.getBounds().isInsideTerritory(bug.blockPosition())
            ) {
                return BehaviorResult.run(stayInside, 80);
            }

            if (hasThreat) {
                if (!isCornered)
                    blackboard.remove(AiKeys.IS_CORNERED);
                return BehaviorResult.run(flee, 50);
            }
            blackboard.remove(AiKeys.IS_CORNERED);

            if (colony != null && !colony.isDisbanded()) {
                if (!colony.getTunnelDigger().isDone()) {
                    return BehaviorResult.run(dig, 25);
                }
            }

            return BehaviorResult.run(bug.getRandom().nextDouble() < 0.2 ? idle : wander, 5);
        };
    }

    public static void markCornered(Blackboard blackboard) {
        blackboard.set(AiKeys.IS_CORNERED, Boolean.TRUE);
    }

    private static boolean isAboveGround(WorkerBug bug) {
        var surfY = bug.level()
            .getHeight(
                Heightmap.Types.WORLD_SURFACE,
                bug.getBlockX(),
                bug.getBlockZ()
            );
        return bug.getBlockY() >= surfY - 2;
    }
}
