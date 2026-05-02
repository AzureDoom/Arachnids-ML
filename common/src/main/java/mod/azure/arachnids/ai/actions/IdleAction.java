package mod.azure.arachnids.ai.actions;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import mod.azure.arachnids.ai.core.*;

public final class IdleAction<E extends Mob> implements Action<E> {

    private final int minDuration;

    private final int maxDuration;

    private final int priority;

    private int age;

    private int duration;

    public IdleAction(int minDuration, int maxDuration, int priority) {
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
        this.priority = priority;
    }

    @Override
    public void start(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        this.age = 0;
        this.duration = minDuration + mob.getRandom().nextInt(maxDuration - minDuration + 1);
        mob.setAggressive(false);
    }

    @Override
    public ActionStatus tick(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        if (mob.getHealth() <= 0) {
            return ActionStatus.INTERRUPTED;
        }

        var target = blackboard.get(AiKeys.TARGET, LivingEntity.class);
        if (target != null && target.isAlive()) {
            return ActionStatus.INTERRUPTED;
        }

        age++;

        if (age % 20 == 0) {
            mob.setYRot((float) (mob.getRandom().nextDouble() * 360.0));
        }

        return age >= duration ? ActionStatus.SUCCESS : ActionStatus.RUNNING;
    }

    @Override
    public void stop(E mob, Blackboard blackboard, ActionStatus reason) {}

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public int priority() {
        return priority;
    }
}
