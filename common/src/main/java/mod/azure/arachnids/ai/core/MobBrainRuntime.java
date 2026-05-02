package mod.azure.arachnids.ai.core;

import net.minecraft.world.entity.Mob;

import mod.azure.arachnids.ai.util.TargetingSystem;

public final class MobBrainRuntime<E extends Mob> {

    private final E mob;

    private final Blackboard blackboard = new Blackboard();

    private final Cooldowns cooldowns = new Cooldowns();

    private final TargetingSystem<E> targetingSystem;

    private final BehaviorNode<E> root;

    private Action<E> currentAction;

    public MobBrainRuntime(E mob, TargetingSystem<E> targetingSystem, BehaviorNode<E> root) {
        this.mob = mob;
        this.targetingSystem = targetingSystem;
        this.root = root;
    }

    public void tick() {
        cooldowns.tick();
        targetingSystem.tick(mob, blackboard);

        if (currentAction != null) {
            var status = currentAction.tick(mob, blackboard, cooldowns);

            if (status == ActionStatus.RUNNING && !currentAction.isInterruptible()) {
                return;
            }

            if (status != ActionStatus.RUNNING) {
                currentAction.stop(mob, blackboard, status);
                currentAction = null;
            }
        }

        var result = root.tick(mob, blackboard, cooldowns);

        if (result.action() != null) {
            var shouldSwitch = currentAction == null
                || result.priority() > currentAction.priority();

            if (shouldSwitch) {
                if (currentAction != null) {
                    currentAction.stop(mob, blackboard, ActionStatus.INTERRUPTED);
                }
                currentAction = result.action();
                currentAction.start(mob, blackboard, cooldowns);
            }
        }
    }

    public Blackboard getBlackboard() {
        return blackboard;
    }

    public Cooldowns getCooldowns() {
        return cooldowns;
    }

    public Action<E> getCurrentAction() {
        return currentAction;
    }
}
