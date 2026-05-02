package mod.azure.arachnids.mob.brainbug;

import net.minecraft.world.entity.LivingEntity;

import mod.azure.arachnids.ai.actions.BeCarriedAction;
import mod.azure.arachnids.ai.actions.IdleAction;
import mod.azure.arachnids.ai.actions.MeleeCloseRangeAction;
import mod.azure.arachnids.ai.actions.ScreamAction;
import mod.azure.arachnids.ai.core.AiKeys;
import mod.azure.arachnids.ai.core.BehaviorNode;
import mod.azure.arachnids.ai.core.BehaviorResult;
import mod.azure.arachnids.ai.transport.BrainBugCarrySystem;

public class BrainBugTree {

    public static final String SCREAM_COOLDOWN = "brainbug:scream_cooldown";

    public static final String PANIC_MELEE_COOLDOWN = "brainbug:panic_melee_cooldown";

    private static final double PANIC_MELEE_RANGE = 2.0D;

    private static final float SCREAM_RADIUS = 5.0F;

    private static final float SCREAM_DAMAGE = 4.0F;

    private static final float PANIC_MELEE_MULT = 2.0F;

    public static BehaviorNode<BrainBug> create() {
        var panicMelee = new MeleeCloseRangeAction<BrainBug>(
            PANIC_MELEE_COOLDOWN,
            20,
            26,
            16,
            PANIC_MELEE_RANGE,
            PANIC_MELEE_MULT,
            100,
            mob -> mob.animationDispatcher.serverNormalAttack()
        );

        var scream = new ScreamAction<BrainBug>(
            SCREAM_COOLDOWN,
            80,
            30,
            15,
            SCREAM_RADIUS,
            PANIC_MELEE_RANGE,
            SCREAM_DAMAGE,
            80,
            mob -> mob.animationDispatcher.serverNormalAttack()
        );

        var beCarried = new BeCarriedAction();

        var idle = new IdleAction<BrainBug>(40, 100, 1);

        return (mob, blackboard, cooldowns) -> {

            var target = blackboard.get(AiKeys.TARGET, LivingEntity.class);
            var underAttack = BrainBugCarrySystem.get().isUnderAttack(mob);

            if (underAttack && BrainBugCarrySystem.get().hasOpenSlot(mob)) {
                BrainBugCarrySystem.get().signalWantsCarriers(mob);
            } else {
                BrainBugCarrySystem.get().clearWantsCarriers(mob);
            }

            if (
                underAttack && target != null
                    && mob.distanceToSqr(target) <= PANIC_MELEE_RANGE * PANIC_MELEE_RANGE
                    && cooldowns.ready(PANIC_MELEE_COOLDOWN)
            ) {
                return BehaviorResult.run(panicMelee, 100);
            }

            if (
                underAttack && target != null
                    && mob.distanceToSqr(target) > PANIC_MELEE_RANGE * PANIC_MELEE_RANGE
                    && cooldowns.ready(SCREAM_COOLDOWN)
            ) {
                return BehaviorResult.run(scream, 80);
            }

            if (BrainBugCarrySystem.get().canMoveCarried(mob)) {
                return BehaviorResult.run(beCarried, 50);
            }

            return BehaviorResult.run(idle, 1);
        };
    }
}
