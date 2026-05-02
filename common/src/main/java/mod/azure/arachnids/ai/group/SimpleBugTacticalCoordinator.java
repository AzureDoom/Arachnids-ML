package mod.azure.arachnids.ai.group;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;

import java.util.EnumMap;

public final class SimpleBugTacticalCoordinator<E extends Mob> implements TacticalCoordinator<E> {

    private static final double FLANKER_PEEL_RANGE = 20.0;

    private static final double FLANKER_PEEL_RANGE_SQ = FLANKER_PEEL_RANGE * FLANKER_PEEL_RANGE;

    private static final int FLANKER_LATERAL_OFFSET = 4;

    private static final double RATIO_FRONTLINE = 0.40;

    private static final double RATIO_FLANKER = 0.40;

    private static final double RATIO_SUPPORT = 0.20;

    @Override
    public TacticalOrder getOrder(E mob, SquadBlackboard squad) {
        var currentTick = mob.level().getGameTime();
        if (squad.lastReservationTick != currentTick) {
            squad.reservedPositions.clear();
            squad.lastReservationTick = currentTick;
        }

        if (!squad.hasPrimaryTarget()) {
            return TacticalOrder.none();
        }

        var role = squad.roles.computeIfAbsent(mob.getUUID(), uuid -> pickRole(mob, squad));

        var assignedTarget = squad.roleTargets.getOrDefault(role, squad.primaryTarget());

        if (role == TacticalRole.FLANKER) {
            var secondary = squad.secondaryTarget();
            if (secondary != null && secondary.isAlive()) {
                double distSq = mob.position().distanceToSqr(secondary.position());
                assignedTarget = distSq <= FLANKER_PEEL_RANGE_SQ ? secondary : squad.primaryTarget();
            } else {
                assignedTarget = squad.primaryTarget();
            }
        }

        var targetPos = assignedTarget.blockPosition();
        var destination = switch (role) {
            case FRONTLINE -> targetPos;
            case FLANKER -> flankerOffset(mob, targetPos);
            case RETREATING -> mob.blockPosition().offset(-4, 0, -4);
            case SUPPORT -> targetPos.offset(-3, 0, 3);
        };

        if (!squad.reservedPositions.add(destination)) {
            destination = destination.offset(
                mob.getRandom().nextInt(5) - 2,
                0,
                mob.getRandom().nextInt(5) - 2
            );
        }

        return new TacticalOrder(role, assignedTarget, destination, 25);
    }

    private BlockPos flankerOffset(E mob, BlockPos targetPos) {
        var dx = targetPos.getX() - mob.getBlockX();
        var dz = targetPos.getZ() - mob.getBlockZ();
        var len = Math.max(1, (int) Math.sqrt(dx * dx + dz * dz));

        var perpX = -dz / len;
        var perpZ = dx / len;

        if (perpX == 0 && perpZ == 0)
            perpX = 1;

        return targetPos.offset(perpX * FLANKER_LATERAL_OFFSET, 0, perpZ * FLANKER_LATERAL_OFFSET);
    }

    private TacticalRole pickRole(E mob, SquadBlackboard squad) {
        var total = squad.squadSize;
        var counts = new EnumMap<TacticalRole, Integer>(TacticalRole.class);
        for (TacticalRole r : squad.roles.values()) {
            counts.merge(r, 1, Integer::sum);
        }

        var currentFrontline = counts.getOrDefault(TacticalRole.FRONTLINE, 0);
        var currentFlanker = counts.getOrDefault(TacticalRole.FLANKER, 0);
        var currentSupport = counts.getOrDefault(TacticalRole.SUPPORT, 0);

        var wantFrontline = Math.max(1, (int) Math.round(total * RATIO_FRONTLINE));
        var wantFlanker = Math.max(1, (int) Math.round(total * RATIO_FLANKER));
        var wantSupport = Math.max(0, (int) Math.round(total * RATIO_SUPPORT));

        if (currentFrontline < wantFrontline)
            return TacticalRole.FRONTLINE;
        if (currentFlanker < wantFlanker)
            return TacticalRole.FLANKER;
        if (currentSupport < wantSupport)
            return TacticalRole.SUPPORT;

        return TacticalRole.FRONTLINE;
    }
}
