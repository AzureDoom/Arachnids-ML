package mod.azure.arachnids.ai.util;

import net.minecraft.world.entity.LivingEntity;

import mod.azure.arachnids.ai.core.Blackboard;

@FunctionalInterface
public interface TargetSelector<E> {

    LivingEntity findTarget(E mob, Blackboard blackboard);
}
