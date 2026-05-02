package mod.azure.arachnids.ai.group;

@FunctionalInterface
public interface TacticalCoordinator<E> {

    TacticalOrder getOrder(E mob, SquadBlackboard squad);
}
