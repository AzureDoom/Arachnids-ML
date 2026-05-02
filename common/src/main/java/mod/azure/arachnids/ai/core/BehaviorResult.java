package mod.azure.arachnids.ai.core;

public record BehaviorResult<E>(
    Action<E> action,
    int priority,
    boolean success
) {

    public static <E> BehaviorResult<E> none() {
        return new BehaviorResult<>(null, 0, false);
    }

    public static <E> BehaviorResult<E> run(Action<E> action, int priority) {
        return new BehaviorResult<>(action, priority, true);
    }
}
