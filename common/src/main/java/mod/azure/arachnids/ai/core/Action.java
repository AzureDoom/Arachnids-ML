package mod.azure.arachnids.ai.core;

public interface Action<E> {

    void start(E mob, Blackboard blackboard, Cooldowns cooldowns);

    ActionStatus tick(E mob, Blackboard blackboard, Cooldowns cooldowns);

    void stop(E mob, Blackboard blackboard, ActionStatus reason);

    boolean isInterruptible();

    int priority();
}
