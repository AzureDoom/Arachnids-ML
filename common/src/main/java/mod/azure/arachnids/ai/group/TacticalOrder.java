package mod.azure.arachnids.ai.group;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

public record TacticalOrder(
    TacticalRole role,
    LivingEntity target,
    BlockPos destination,
    int priority
) {

    public static TacticalOrder none() {
        return new TacticalOrder(null, null, null, 0);
    }

    public boolean hasTarget() {
        return target != null && target.isAlive();
    }

    public boolean hasDestination() {
        return destination != null;
    }
}
