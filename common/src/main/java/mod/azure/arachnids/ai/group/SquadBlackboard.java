package mod.azure.arachnids.ai.group;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public final class SquadBlackboard {

    public List<LivingEntity> targetPriority = new ArrayList<>();

    public Map<TacticalRole, LivingEntity> roleTargets = new EnumMap<>(TacticalRole.class);

    public Map<UUID, TacticalRole> roles = new HashMap<>();

    public Set<BlockPos> reservedPositions = new HashSet<>();

    public int squadSize = 1;

    public long lastReservationTick = -1;

    public long lastTargetEvalTick = -1;

    public LivingEntity primaryTarget() {
        return targetPriority == null || targetPriority.isEmpty()
            ? null
            : targetPriority.getFirst();
    }

    public LivingEntity secondaryTarget() {
        return targetPriority == null || targetPriority.size() <= 1
            ? null
            : targetPriority.get(1);
    }

    public boolean hasPrimaryTarget() {
        var t = primaryTarget();
        return t != null && t.isAlive();
    }
}
