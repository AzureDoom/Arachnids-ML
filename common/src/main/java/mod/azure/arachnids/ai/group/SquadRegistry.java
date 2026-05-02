package mod.azure.arachnids.ai.group;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import mod.azure.arachnids.ai.util.TargetingUtils;

public final class SquadRegistry {

    private static final SquadRegistry INSTANCE = new SquadRegistry();

    public static SquadRegistry get() {
        return INSTANCE;
    }

    private static final double SQUAD_RADIUS = 24.0;

    private static final double TARGET_SCAN_RADIUS = 32.0;

    public static final int TARGET_EVAL_INTERVAL = 40;

    public static final int MIN_SQUAD_SIZE = 2;

    private final Map<UUID, SquadBlackboard> squadToBoard = new HashMap<>();

    private final Map<UUID, Set<UUID>> squadToMembers = new HashMap<>();

    private final Map<UUID, UUID> mobToSquad = new HashMap<>();

    private SquadRegistry() {}

    public <E extends Mob> SquadBlackboard getOrJoinSquad(E mob) {
        pruneDeadMobs(mob);

        var mobId = mob.getUUID();
        var squadId = mobToSquad.get(mobId);

        if (squadId == null) {
            squadId = findNearbySquad(mob);
        }

        if (squadId == null) {
            squadId = mobId;
            squadToMembers.put(squadId, new HashSet<>(Collections.singleton(mobId)));
            squadToBoard.put(squadId, new SquadBlackboard());
        }

        mobToSquad.put(mobId, squadId);
        squadToMembers.computeIfAbsent(squadId, k -> new HashSet<>()).add(mobId);

        mergeNearbySquads(mob, squadId);

        var board = squadToBoard.get(squadId);
        if (board != null) {
            long now = mob.level().getGameTime();
            if (now - board.lastTargetEvalTick >= TARGET_EVAL_INTERVAL) {
                evaluateTargets(mob, squadId, board);
                board.lastTargetEvalTick = now;
            }

            var primary = board.primaryTarget();
            if (primary != null && primary.isAlive()) {
                mob.setTarget(primary);
            } else if (TargetingUtils.validTarget(mob).test(mob.getTarget())) {
                board.targetPriority.addFirst(mob.getTarget());
            }
        }

        var members = squadToMembers.get(squadId);
        if (members == null || members.size() < MIN_SQUAD_SIZE) {
            return null;
        }

        return board;
    }

    public void remove(Mob mob) {
        var mobId = mob.getUUID();
        var squadId = mobToSquad.remove(mobId);
        if (squadId == null)
            return;

        var members = squadToMembers.get(squadId);
        if (members != null) {
            members.remove(mobId);
            if (members.isEmpty()) {
                squadToMembers.remove(squadId);
                squadToBoard.remove(squadId);
            }
        }
    }

    public int squadSizeFor(Mob mob) {
        var squadId = mobToSquad.get(mob.getUUID());
        if (squadId == null)
            return 1;
        var members = squadToMembers.get(squadId);
        return members == null ? 1 : members.size();
    }

    public boolean isInSquad(Mob mob) {
        var squadId = mobToSquad.get(mob.getUUID());
        if (squadId == null)
            return false;
        var members = squadToMembers.get(squadId);
        return members != null && members.size() >= MIN_SQUAD_SIZE;
    }

    public TacticalRole getRoleFor(Mob mob, SquadBlackboard board) {
        return board.roles.get(mob.getUUID());
    }

    private <E extends Mob> void evaluateTargets(E mob, UUID squadId, SquadBlackboard board) {
        if (!(mob.level() instanceof ServerLevel serverLevel))
            return;

        var members = squadToMembers.get(squadId);
        if (members == null || members.isEmpty())
            return;

        var centroid = computeCentroid(members, serverLevel);
        if (centroid == null)
            return;

        Set<LivingEntity> candidates = new LinkedHashSet<>();

        for (var memberId : members) {
            var entity = serverLevel.getEntity(memberId);
            if (!(entity instanceof Mob squadMob))
                continue;

            if (TargetingUtils.validTarget(squadMob).test(squadMob.getTarget())) {
                candidates.add(squadMob.getTarget());
            }

            candidates.addAll(
                serverLevel.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(entity.blockPosition()).inflate(TARGET_SCAN_RADIUS),
                    e -> TargetingUtils.validTarget(squadMob).test(e)
                )
            );
        }

        List<LivingEntity> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(centroid)));

        board.targetPriority.clear();
        board.targetPriority.addAll(sorted);

        board.roleTargets.clear();
        var primary = board.primaryTarget();
        var secondary = board.secondaryTarget();

        if (primary != null) {
            board.roleTargets.put(TacticalRole.FRONTLINE, primary);
            board.roleTargets.put(TacticalRole.SUPPORT, primary);
            board.roleTargets.put(TacticalRole.RETREATING, primary);
            board.roleTargets.put(TacticalRole.FLANKER, secondary != null ? secondary : primary);
        }
    }

    private Vec3 computeCentroid(Set<UUID> members, ServerLevel level) {
        double x = 0, y = 0, z = 0;
        var count = 0;

        for (var id : members) {
            var e = level.getEntity(id);
            if (e == null || !e.isAlive())
                continue;
            x += e.getX();
            y += e.getY();
            z += e.getZ();
            count++;
        }

        return count == 0 ? null : new Vec3(x / count, y / count, z / count);
    }

    private void pruneDeadMobs(Mob reference) {
        if (!(reference.level() instanceof ServerLevel serverLevel))
            return;

        Iterator<Map.Entry<UUID, UUID>> it = mobToSquad.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            var memberId = entry.getKey();
            var squadId = entry.getValue();
            var entity = serverLevel.getEntity(memberId);

            if (entity == null || !entity.isAlive()) {
                it.remove();

                var members = squadToMembers.get(squadId);
                if (members != null) {
                    members.remove(memberId);
                    if (members.isEmpty()) {
                        squadToMembers.remove(squadId);
                        squadToBoard.remove(squadId);
                    }
                }

                var board = squadToBoard.get(squadId);
                if (board != null) {
                    board.reservedPositions.clear();
                }
            }
        }
    }

    private <E extends Mob> UUID findNearbySquad(E mob) {
        var nearby = mob.level()
            .getEntitiesOfClass(
                mob.getClass(),
                new AABB(mob.blockPosition()).inflate(SQUAD_RADIUS)
            );

        for (var other : nearby) {
            if (other == mob)
                continue;
            var existingSquad = mobToSquad.get(other.getUUID());
            if (existingSquad != null)
                return existingSquad;
        }

        return null;
    }

    private <E extends Mob> void mergeNearbySquads(E mob, UUID targetSquadId) {
        var nearby = mob.level()
            .getEntitiesOfClass(
                mob.getClass(),
                new AABB(mob.blockPosition()).inflate(SQUAD_RADIUS)
            );

        for (var other : nearby) {
            if (other == mob)
                continue;

            var otherSquadId = mobToSquad.get(other.getUUID());
            if (otherSquadId == null || otherSquadId.equals(targetSquadId))
                continue;

            var otherMembers = squadToMembers.remove(otherSquadId);
            var otherBoard = squadToBoard.remove(otherSquadId);

            if (otherMembers != null) {
                var targetMembers = squadToMembers.computeIfAbsent(targetSquadId, k -> new HashSet<>());
                for (var m : otherMembers) {
                    targetMembers.add(m);
                    mobToSquad.put(m, targetSquadId);
                }
            }

            if (otherBoard != null) {
                var targetBoard = squadToBoard.get(targetSquadId);
                if (targetBoard != null && !targetBoard.hasPrimaryTarget()) {
                    targetBoard.targetPriority.addAll(otherBoard.targetPriority);
                    targetBoard.roleTargets.putAll(otherBoard.roleTargets);
                }
            }
        }
    }
}
