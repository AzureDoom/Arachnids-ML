package mod.azure.arachnids.ai.util;

import net.minecraft.world.entity.LivingEntity;

import mod.azure.arachnids.ai.core.AiKeys;
import mod.azure.arachnids.ai.core.Blackboard;

public final class TargetingSystem<E> {

    private final TargetSelector<E> selector;

    private final int retargetInterval;

    private int age;

    public TargetingSystem(TargetSelector<E> selector, int retargetInterval) {
        this.selector = selector;
        this.retargetInterval = retargetInterval;
    }

    public void tick(E mob, Blackboard blackboard) {
        age++;

        var current = blackboard.get(AiKeys.TARGET, LivingEntity.class);

        if (current != null && current.isAlive()) {
            blackboard.set(AiKeys.LAST_KNOWN_TARGET_POS, current.blockPosition());
        }

        if (age % retargetInterval != 0 && current != null && current.isAlive()) {
            return;
        }

        var target = selector.findTarget(mob, blackboard);

        if (target != null) {
            blackboard.set(AiKeys.TARGET, target);
        } else {
            blackboard.remove(AiKeys.TARGET);
        }
    }
}
