package mod.azure.arachnids.ai.core;

@FunctionalInterface
public interface BehaviorNode<E> {

    BehaviorResult<E> tick(E mob, Blackboard blackboard, Cooldowns cooldowns);
}
