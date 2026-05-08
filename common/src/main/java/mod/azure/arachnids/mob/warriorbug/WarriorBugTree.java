package mod.azure.arachnids.mob.warriorbug;

import net.minecraft.world.entity.LivingEntity;

import mod.azure.arachnids.ai.actions.*;
import mod.azure.arachnids.ai.actions.colony.ColonyPatrolAction;
import mod.azure.arachnids.ai.core.AiKeys;
import mod.azure.arachnids.ai.core.BehaviorNode;
import mod.azure.arachnids.ai.core.BehaviorResult;
import mod.azure.arachnids.ai.group.SimpleBugTacticalCoordinator;
import mod.azure.arachnids.ai.group.SquadRegistry;
import mod.azure.arachnids.ai.group.TacticalRole;
import mod.azure.arachnids.ai.util.CrawlingManager;
import mod.azure.arachnids.ai.util.TargetingUtils;
import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.colony.ColonyState;

public class WarriorBugTree {

    private static final SimpleBugTacticalCoordinator<WarriorBug> COORDINATOR =
        new SimpleBugTacticalCoordinator<>();

    private static final float RETREAT_HEALTH = 0.25F;

    private static final double SUPPORT_ENGAGE_RANGE = 2.0D;

    public static BehaviorNode<WarriorBug> create() {
        var fleeExplosive = new ExplosiveFleeAction<WarriorBug>(
            0.42D,
            16.0D,
            8.0D,
            120
        );

        var patrol = new ColonyPatrolAction<>(30);

        var moveToTargetFast = new MoveToTargetAction<WarriorBug>(
            2.5D,
            0.42D,
            20
        );

        var moveToTargetCombat = new MoveToTargetAction<WarriorBug>(
            2.5D,
            0.38D,
            20
        );

        var crawlToTargetFast = new CrawlToTargetAction<WarriorBug>(
            2.5D,
            0.42D,
            20
        );

        var crawlToTargetCombat = new CrawlToTargetAction<WarriorBug>(
            2.5D,
            0.38D,
            20
        );

        var retreatMove = new MoveToDestinationAction<WarriorBug>(
            0.5D,
            0.55D,
            30,
            5.0D,
            1.0D,
            0.55D,
            0.85D
        );

        var supportMove = new MoveToDestinationAction<WarriorBug>(
            3.5D,
            0.25D,
            25,
            5.0D,
            1.0D,
            0.55D,
            0.85D
        );

        var destinationMove = new MoveToDestinationAction<WarriorBug>(
            2.5D,
            0.38D,
            25,
            5.0D,
            1.0D,
            0.55D,
            0.85D
        );

        var flankerDestinationMove = new MoveToDestinationAction<WarriorBug>(
            2.5D,
            0.46D,
            25,
            5.0D,
            1.0D,
            0.55D,
            0.85D
        );

        var crawlRetreatMove = new CrawlToDestinationAction<WarriorBug>(
            0.5D,
            0.55D,
            30,
            5.0D,
            1.0D,
            0.55D,
            0.85D
        );

        var crawlSupportMove = new CrawlToDestinationAction<WarriorBug>(
            3.5D,
            0.25D,
            25,
            5.0D,
            1.0D,
            0.55D,
            0.85D
        );

        var crawlDestinationMove = new CrawlToDestinationAction<WarriorBug>(
            2.5D,
            0.38D,
            25,
            5.0D,
            1.0D,
            0.55D,
            0.85D
        );

        var crawlFlankerDestinationMove = new CrawlToDestinationAction<WarriorBug>(
            2.5D,
            0.46D,
            25,
            5.0D,
            1.0D,
            0.55D,
            0.85D
        );

        var wander = new WanderAction<WarriorBug>(0.18D, 5, 8.0D, 40, 120);

        var idle = new IdleAction<WarriorBug>(30, 80, 5);

        var heavyAttack110 = new TimedAttackAction<WarriorBug>(
            "heavy_attack",
            40,
            17,
            10,
            110,
            b -> b.animationDispatcher.serverHeavyAttack()
        );

        var heavyAttack100 = new TimedAttackAction<WarriorBug>(
            "heavy_attack",
            40,
            17,
            10,
            100,
            b -> b.animationDispatcher.serverHeavyAttack()
        );

        var normalAttack100 = new TimedAttackAction<WarriorBug>(
            "normal_attack",
            40,
            28,
            16,
            100,
            b -> b.animationDispatcher.serverNormalAttack()
        );

        var normalAttack80 = new TimedAttackAction<WarriorBug>(
            "normal_attack",
            40,
            28,
            16,
            80,
            b -> b.animationDispatcher.serverNormalAttack()
        );

        return (bug, blackboard, cooldowns) -> {
            var currentTarget = blackboard.get(AiKeys.TARGET, LivingEntity.class);
            if (currentTarget != null && !currentTarget.isAlive()) {
                blackboard.set(AiKeys.TARGET, null);
                bug.setTarget(null);
            }

            if (fleeExplosive.hasNearbyExplosive(bug)) {
                return BehaviorResult.run(fleeExplosive, 120);
            }

            var colony = ColonyManager.get().colonyOf(bug);

            if (colony != null && !colony.isDisbanded()) {
                var state = colony.getState();
                var directive = colony.getWarriorDirective();

                if (
                    directive != null && directive.isAlive()
                        && (state == ColonyState.DEFEND || state == ColonyState.PANIC)
                ) {

                    var yGapDirective = directive.getY() - bug.getY();
                    var canReachDirective = yGapDirective <= 1.5D;
                    if (
                        canReachDirective && TargetingUtils.isInAttackRange(bug, directive, 1.25D) && cooldowns.ready(
                            "heavy_attack"
                        )
                    ) {
                        blackboard.set(AiKeys.TARGET, directive);
                        return BehaviorResult.run(heavyAttack110, 110);
                    }

                    if (
                        canReachDirective && TargetingUtils.isInAttackRange(bug, directive, 1.0D) && cooldowns.ready(
                            "normal_attack"
                        )
                    ) {
                        blackboard.set(AiKeys.TARGET, directive);
                        return BehaviorResult.run(normalAttack100, 100);
                    }

                    blackboard.set(AiKeys.TARGET, directive);

                    if (CrawlingManager.shouldUseWallCrawlingToTarget(bug, directive)) {
                        return BehaviorResult.run(crawlToTargetFast, 90);
                    }

                    return BehaviorResult.run(moveToTargetFast, 90);
                }

                if (state == ColonyState.PEACEFUL) {
                    var target = blackboard.get(AiKeys.TARGET, LivingEntity.class);

                    if (target == null || !target.isAlive()) {
                        target = bug.getTarget();
                    }

                    if (target == null || !target.isAlive() || !TargetingUtils.validTarget(bug).test(target)) {
                        return BehaviorResult.run(patrol, 30);
                    }

                    blackboard.set(AiKeys.TARGET, target);
                }
            }

            var registry = SquadRegistry.get();
            var squad = registry.getOrJoinSquad(bug);

            if (squad != null) {
                squad.squadSize = registry.squadSizeFor(bug);

                if (bug.getHealth() / bug.getMaxHealth() < RETREAT_HEALTH) {
                    squad.roles.put(bug.getUUID(), TacticalRole.RETREATING);
                }

                var order = COORDINATOR.getOrder(bug, squad);

                if (order.role() == TacticalRole.RETREATING && order.hasDestination()) {
                    blackboard.set(AiKeys.DESTINATION, order.destination());

                    if (CrawlingManager.shouldUseWallCrawlingTo(bug, order.destination())) {
                        return BehaviorResult.run(crawlRetreatMove, 30);
                    }

                    return BehaviorResult.run(retreatMove, 30);
                }

                if (order.hasTarget()) {
                    var target = order.target();

                    if (order.role() == TacticalRole.SUPPORT) {
                        var frontlineEngaged = false;
                        for (var role : squad.roles.values()) {
                            if (role == TacticalRole.FRONTLINE) {
                                frontlineEngaged = true;
                                break;
                            }
                        }

                        if (
                            order.hasDestination() && (!frontlineEngaged || !TargetingUtils.isInAttackRange(
                                bug,
                                target,
                                SUPPORT_ENGAGE_RANGE
                            ))
                        ) {
                            blackboard.set(AiKeys.DESTINATION, order.destination());

                            if (CrawlingManager.shouldUseWallCrawlingTo(bug, order.destination())) {
                                return BehaviorResult.run(crawlSupportMove, 25);
                            }

                            return BehaviorResult.run(supportMove, 25);
                        }
                    }

                    var yGapSquad = target.getY() - bug.getY();
                    var canReachSquad = yGapSquad <= 1.5D;
                    if (
                        canReachSquad && TargetingUtils.isInAttackRange(bug, target, 1.25D) && cooldowns.ready(
                            "heavy_attack"
                        )
                    ) {
                        return BehaviorResult.run(heavyAttack100, 100);
                    }
                    if (
                        canReachSquad && TargetingUtils.isInAttackRange(bug, target, 1.0D) && cooldowns.ready(
                            "normal_attack"
                        )
                    ) {
                        return BehaviorResult.run(normalAttack80, 80);
                    }
                    if (order.hasDestination()) {
                        blackboard.set(AiKeys.TARGET, target);
                        blackboard.set(AiKeys.DESTINATION, order.destination());

                        if (order.role() == TacticalRole.FLANKER) {
                            if (CrawlingManager.shouldUseWallCrawlingTo(bug, order.destination())) {
                                return BehaviorResult.run(crawlFlankerDestinationMove, 25);
                            }

                            return BehaviorResult.run(flankerDestinationMove, 25);
                        }

                        if (CrawlingManager.shouldUseWallCrawlingTo(bug, order.destination())) {
                            return BehaviorResult.run(crawlDestinationMove, 25);
                        }

                        return BehaviorResult.run(destinationMove, 25);
                    }

                    if (CrawlingManager.shouldUseWallCrawlingTo(bug, target)) {
                        return BehaviorResult.run(crawlToTargetCombat, 20);
                    }

                    return BehaviorResult.run(moveToTargetCombat, 20);
                }
            }

            var target = blackboard.get(AiKeys.TARGET, LivingEntity.class);
            if (target != null && target.isAlive()) {
                var yGap = target.getY() - bug.getY();
                var canReachVertically = yGap <= 1.5D;

                if (
                    canReachVertically && TargetingUtils.isInAttackRange(bug, target, 1.25D) && cooldowns.ready(
                        "heavy_attack"
                    )
                ) {
                    return BehaviorResult.run(heavyAttack100, 100);
                }
                if (
                    canReachVertically && TargetingUtils.isInAttackRange(bug, target, 1.0D) && cooldowns.ready(
                        "normal_attack"
                    )
                ) {
                    return BehaviorResult.run(normalAttack80, 80);
                }

                if (CrawlingManager.shouldUseWallCrawlingToTarget(bug, target)) {
                    return BehaviorResult.run(crawlToTargetCombat, 20);
                }

                return BehaviorResult.run(moveToTargetCombat, 20);
            }

            if (bug.getRandom().nextFloat() < 0.35F) {
                return BehaviorResult.run(idle, 5);
            }
            return BehaviorResult.run(wander, 5);
        };
    }
}
