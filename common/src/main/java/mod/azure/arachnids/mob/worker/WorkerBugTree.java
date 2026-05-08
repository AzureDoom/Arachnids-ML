package mod.azure.arachnids.mob.worker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import mod.azure.arachnids.ai.actions.*;
import mod.azure.arachnids.ai.actions.colony.ColonyStayInsideAction;
import mod.azure.arachnids.ai.core.AiKeys;
import mod.azure.arachnids.ai.core.BehaviorNode;
import mod.azure.arachnids.ai.core.BehaviorResult;
import mod.azure.arachnids.ai.core.Blackboard;
import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.colony.ColonyState;

public class WorkerBugTree {

    public static BehaviorNode<WorkerBug> create() {
        var fleeExplosive = new ExplosiveFleeAction<WorkerBug>(0.32D, 10, 20, 120);

        var cornerFight = new CornerFightAction<WorkerBug>(
            AiKeys.CORNERED_ATTACK_COOLDOWN,
            60,
            14,
            9,
            90,
            b -> b.animationDispatcher.serverLightAttack()
        );

        var flee = new FleeAction<WorkerBug>(0.32D, 20.0D, 50);
        var sapper = new WorkerSapperAction<>(45);
        var dig = new WorkerDigAction<>(40);
        var build = new WorkerBuildAction<>(35);
        var stayInside = new ColonyStayInsideAction<WorkerBug>(80);
        var wander = new WanderAction<WorkerBug>(0.18D, 5, 10.0D, 60, 160);
        var idle = new IdleAction<WorkerBug>(40, 100, 1);

        return (bug, blackboard, cooldowns) -> {
            if (fleeExplosive.hasNearbyExplosive(bug)) {
                return BehaviorResult.run(fleeExplosive, 120);
            }

            var colony = ColonyManager.get().colonyOf(bug);
            var hasDigTarget = blackboard.get(AiKeys.DIG_TARGET, BlockPos.class) != null;
            var digBatchCount = blackboard.get(AiKeys.DIG_BATCH_COUNT, Integer.class);
            var hasActiveDigBatch = hasDigTarget || (digBatchCount != null && digBatchCount > 0);
            var hasSapperTarget = blackboard.get(AiKeys.SAPPER_CURRENT_BLOCK, BlockPos.class) != null;
            var threat = blackboard.get(AiKeys.TARGET, LivingEntity.class);
            var hasThreat = threat != null && threat.isAlive();
            var isCornered = Boolean.TRUE.equals(blackboard.get(AiKeys.IS_CORNERED, Boolean.class));

            if (
                hasThreat || (colony != null && colony.getWarriorDirective() != null
                    && colony.getState() != ColonyState.PEACEFUL)
            ) {

                if (
                    colony != null && !colony.isDisbanded()
                        && colony.getWarriorDirective() != null
                        && colony.getState() != ColonyState.PANIC
                ) {
                    return BehaviorResult.run(sapper, 45);
                }

                if (hasThreat && isCornered && cooldowns.ready(AiKeys.CORNERED_ATTACK_COOLDOWN)) {
                    return BehaviorResult.run(cornerFight, 90);
                }

                if (hasThreat) {
                    if (!isCornered)
                        blackboard.remove(AiKeys.IS_CORNERED);
                    return BehaviorResult.run(flee, 50);
                }
            }
            blackboard.remove(AiKeys.IS_CORNERED);

            if (
                colony != null && !colony.isDisbanded()
                    && hasActiveDigBatch
                    && !colony.getTunnelDigger().isDone()
                    && !colony.getBlockStore().isFull()
            ) {
                return BehaviorResult.run(dig, 40);
            }

            if (
                colony != null && !colony.isDisbanded()
                    && !colony.getBlockStore().isEmpty()
                    && !colony.getDomeBuilder().isDone()
            ) {
                return BehaviorResult.run(build, 35);
            }

            if (
                colony != null && !colony.isDisbanded()
                    && !colony.getTunnelDigger().isDone()
                    && !colony.getBlockStore().isFull()
            ) {
                return BehaviorResult.run(dig, 40);
            }

            if (
                colony != null && !colony.isDisbanded()
                    && !colony.getBounds().isInsideTerritory(bug.blockPosition())
                    && !hasDigTarget
                    && !hasSapperTarget
            ) {
                return BehaviorResult.run(stayInside, 80);
            }

            return BehaviorResult.run(bug.getRandom().nextDouble() < 0.2 ? idle : wander, 5);
        };
    }

    public static void markCornered(Blackboard blackboard) {
        blackboard.set(AiKeys.IS_CORNERED, Boolean.TRUE);
    }

    @SuppressWarnings("unused")
    private static boolean isAboveGround(WorkerBug bug) {
        var surfY = bug.level()
            .getHeight(Heightmap.Types.WORLD_SURFACE, bug.getBlockX(), bug.getBlockZ());
        return bug.getBlockY() >= surfY - 2;
    }
}
