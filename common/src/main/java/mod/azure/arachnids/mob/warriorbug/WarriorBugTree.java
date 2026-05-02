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
            32.0D,
            16.0D,
            120
        );

        var patrol = new ColonyPatrolAction<>(30);

        return (bug, blackboard, cooldowns) -> {

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

                    if (TargetingUtils.isInAttackRange(bug, directive, 1.25D) && cooldowns.ready("heavy_attack")) {
                        blackboard.set(AiKeys.TARGET, directive);
                        return BehaviorResult.run(
                            new TimedAttackAction<>(
                                "heavy_attack",
                                80,
                                17,
                                10,
                                110,
                                b -> b.animationDispatcher.serverHeavyAttack()
                            ),
                            110
                        );
                    }

                    if (TargetingUtils.isInAttackRange(bug, directive, 1.0D) && cooldowns.ready("normal_attack")) {
                        blackboard.set(AiKeys.TARGET, directive);
                        return BehaviorResult.run(
                            new TimedAttackAction<>(
                                "normal_attack",
                                40,
                                28,
                                16,
                                100,
                                b -> b.animationDispatcher.serverNormalAttack()
                            ),
                            100
                        );
                    }

                    blackboard.set(AiKeys.TARGET, directive);
                    return BehaviorResult.run(
                        new MoveToTargetAction<>(
                            2.5D,
                            0.42D,
                            20
                        ),
                        90
                    );
                }

                if (state == ColonyState.PEACEFUL) {
                    return BehaviorResult.run(patrol, 30);
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
                    return BehaviorResult.run(
                        new MoveToDestinationAction<>(
                            order.destination(),
                            0.5D,
                            0.55D,
                            30,
                            5.0D,
                            1.0D,
                            0.55D,
                            0.85D
                        ),
                        30
                    );
                }

                if (order.hasTarget()) {
                    var target = order.target();

                    if (order.role() == TacticalRole.SUPPORT) {
                        boolean frontlineEngaged = squad.roles.values()
                            .stream()
                            .anyMatch(r -> r == TacticalRole.FRONTLINE);

                        if (!frontlineEngaged || !TargetingUtils.isInAttackRange(bug, target, SUPPORT_ENGAGE_RANGE)) {
                            return BehaviorResult.run(
                                new MoveToDestinationAction<>(
                                    order.destination(),
                                    3.5D,
                                    0.25D,
                                    20,
                                    5.0D,
                                    1.0D,
                                    0.55D,
                                    0.85D
                                ),
                                20
                            );
                        }
                    }

                    if (TargetingUtils.isInAttackRange(bug, target, 1.25D) && cooldowns.ready("heavy_attack")) {
                        return BehaviorResult.run(
                            new TimedAttackAction<>(
                                "heavy_attack",
                                80,
                                17,
                                10,
                                100,
                                b -> b.animationDispatcher.serverHeavyAttack()
                            ),
                            100
                        );
                    }
                    if (TargetingUtils.isInAttackRange(bug, target, 1.0D) && cooldowns.ready("normal_attack")) {
                        return BehaviorResult.run(
                            new TimedAttackAction<>(
                                "normal_attack",
                                40,
                                28,
                                16,
                                80,
                                b -> b.animationDispatcher.serverNormalAttack()
                            ),
                            80
                        );
                    }
                    if (order.hasDestination()) {
                        blackboard.set(AiKeys.TARGET, target);
                        double speed = order.role() == TacticalRole.FLANKER ? 0.46D : 0.38D;
                        return BehaviorResult.run(
                            new MoveToDestinationAction<>(
                                order.destination(),
                                2.5D,
                                speed,
                                20,
                                5.0D,
                                1.0D,
                                0.55D,
                                0.85D
                            ),
                            20
                        );
                    }
                    return BehaviorResult.run(
                        new MoveToTargetAction<>(
                            2.5D,
                            0.38D,
                            20
                        ),
                        20
                    );
                }
            }

            var target = blackboard.get(AiKeys.TARGET, LivingEntity.class);
            if (target != null && target.isAlive()) {
                if (TargetingUtils.isInAttackRange(bug, target, 1.25D) && cooldowns.ready("heavy_attack")) {
                    return BehaviorResult.run(
                        new TimedAttackAction<>(
                            "heavy_attack",
                            80,
                            17,
                            10,
                            100,
                            b -> b.animationDispatcher.serverHeavyAttack()
                        ),
                        100
                    );
                }
                if (TargetingUtils.isInAttackRange(bug, target, 1.0D) && cooldowns.ready("normal_attack")) {
                    return BehaviorResult.run(
                        new TimedAttackAction<>(
                            "normal_attack",
                            40,
                            28,
                            16,
                            80,
                            b -> b.animationDispatcher.serverNormalAttack()
                        ),
                        80
                    );
                }
                return BehaviorResult.run(
                    new MoveToTargetAction<>(
                        2.5D,
                        0.38D,
                        20
                    ),
                    20
                );
            }

            if (bug.getRandom().nextFloat() < 0.35F) {
                return BehaviorResult.run(new IdleAction<>(30, 80, 5), 5);
            }
            return BehaviorResult.run(new WanderAction<>(0.18D, 5, 8.0D, 40, 120), 5);
        };
    }
}
