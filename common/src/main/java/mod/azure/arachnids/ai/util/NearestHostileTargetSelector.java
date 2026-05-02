package mod.azure.arachnids.ai.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Comparator;

import mod.azure.arachnids.ai.core.AiKeys;
import mod.azure.arachnids.ai.core.Blackboard;

public final class NearestHostileTargetSelector<E extends Mob> implements TargetSelector<E> {

    private final double range;

    public NearestHostileTargetSelector(double range) {
        this.range = range;
    }

    @Override
    public LivingEntity findTarget(E mob, Blackboard blackboard) {
        var current = blackboard.get(AiKeys.TARGET, LivingEntity.class);

        if (TargetingUtils.validTarget(mob).test(current)) {
            return current;
        }

        return mob.level()
            .getEntitiesOfClass(
                LivingEntity.class,
                mob.getBoundingBox().inflate(range),
                entity -> TargetingUtils.validTarget(mob).test(entity)
            )
            .stream()
            .min(Comparator.comparingDouble(mob::distanceToSqr))
            .orElse(null);
    }
}
