package mod.azure.arachnids.ai.actions;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.function.Consumer;

import mod.azure.arachnids.ai.core.*;

public final class TimedAttackAction<E extends Mob> implements Action<E> {

    private static final double HIT_INFLATE = 2.5D;

    private final String cooldownKey;

    private final int cooldownTicks;

    private final int totalTicks;

    private final int damageTick;

    private final int priority;

    private final Consumer<E> animationTrigger;

    private int age;

    public TimedAttackAction(
        String cooldownKey,
        int cooldownTicks,
        int totalTicks,
        int damageTick,
        int priority,
        Consumer<E> animationTrigger
    ) {
        this.cooldownKey = cooldownKey;
        this.cooldownTicks = cooldownTicks;
        this.totalTicks = totalTicks;
        this.damageTick = damageTick;
        this.priority = priority;
        this.animationTrigger = animationTrigger;
    }

    @Override
    public void start(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        this.age = 0;
        mob.hasImpulse = true;
        animationTrigger.accept(mob);
    }

    @Override
    public ActionStatus tick(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        age++;
        mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);
        mob.hasImpulse = true;

        if (mob.getHealth() <= 0) {
            mob.setAggressive(false);
            return ActionStatus.INTERRUPTED;
        }

        var target = blackboard.get(AiKeys.TARGET, LivingEntity.class);
        if (target == null || !target.isAlive()) {
            return ActionStatus.FAILURE;
        }

        if (age == damageTick) {
            if (mob.getBoundingBox().inflate(HIT_INFLATE).intersects(target.getBoundingBox())) {
                mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                mob.doHurtTarget(target);

                if (!target.isAlive()) {
                    blackboard.set(AiKeys.TARGET, null);
                    mob.setTarget(null);
                    mob.setAggressive(false);
                    cooldowns.set(cooldownKey, cooldownTicks);
                    return ActionStatus.SUCCESS;
                }
            }
        }

        if (age >= totalTicks) {
            cooldowns.set(cooldownKey, cooldownTicks);
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            return ActionStatus.SUCCESS;
        }

        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(E mob, Blackboard blackboard, ActionStatus reason) {}

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public int priority() {
        return priority;
    }
}
