package mod.azure.arachnids.ai.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.*;

public class CustomAStar {

    public CustomAStar() {}

    public record Node(
        BlockPos pos,
        double g,
        double f,
        Node parent
    ) {}

    public static List<BlockPos> findPath(Mob mob, BlockPos start, BlockPos goal, int maxRange, int goalRadius) {
        var level = mob.level();

        var open = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
        Map<BlockPos, Double> bestCost = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        var startFeet = normalizeFeet(start);
        var goalFeet = normalizeFeet(goal);

        open.add(new Node(startFeet, 0.0D, heuristic(startFeet, goalFeet), null));
        bestCost.put(startFeet, 0.0D);

        var searched = 0;
        var maxSearched = 2000;
        Node bestPartial = null;
        var bestPartialScore = Double.MAX_VALUE;

        while (!open.isEmpty() && searched++ < maxSearched) {
            var current = open.poll();

            var partialScore = heuristic(current.pos(), goalFeet);

            if (partialScore < bestPartialScore) {
                bestPartialScore = partialScore;
                bestPartial = current;
            }

            if (!closed.add(current.pos())) {
                continue;
            }

            if (isCloseEnoughToGoal(current.pos(), goalFeet, goalRadius)) {
                return reconstruct(current);
            }

            for (var next : neighbors(level, mob, current.pos())) {

                if (closed.contains(next)) {
                    continue;
                }

                if (next.distManhattan(startFeet) > maxRange) {
                    continue;
                }

                var stepCost = movementCost(level, mob, current.pos(), next);

                if (stepCost >= 9999.0D) {
                    continue;
                }

                var newG = current.g() + stepCost;
                var oldG = bestCost.getOrDefault(next, Double.MAX_VALUE);

                if (newG < oldG) {
                    bestCost.put(next, newG);
                    var f = newG + heuristic(next, goalFeet);
                    open.add(new Node(next, newG, f, current));
                }
            }
        }
        if (bestPartial != null && bestPartial.parent() != null) {
            return reconstruct(bestPartial);
        }

        return Collections.emptyList();
    }

    public static boolean isCloseEnoughToGoal(BlockPos pos, BlockPos goal, int goalRadius) {
        var dx = pos.getX() - goal.getX();
        var dz = pos.getZ() - goal.getZ();

        return dx * dx + dz * dz <= goalRadius * goalRadius
            && Math.abs(pos.getY() - goal.getY()) <= 2;
    }

    public static List<BlockPos> reconstruct(Node node) {
        LinkedList<BlockPos> result = new LinkedList<>();

        var current = node;
        while (current != null) {
            result.addFirst(current.pos());
            current = current.parent();
        }

        return result;
    }

    public static double heuristic(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX())
            + Math.abs(a.getY() - b.getY()) * 1.5D
            + Math.abs(a.getZ() - b.getZ());
    }

    public static List<BlockPos> neighbors(Level level, Mob mob, BlockPos pos) {
        List<BlockPos> result = new ArrayList<>();

        int[][] dirs = {
            { 1, 0 },
            { -1, 0 },
            { 0, 1 },
            { 0, -1 }
        };

        for (var dir : dirs) {
            var base = pos.offset(dir[0], 0, dir[1]);

            tryAdd(level, mob, result, base);

            tryAdd(level, mob, result, base.above());

            for (int drop = 1; drop <= 3; drop++) {
                tryAdd(level, mob, result, base.below(drop));
            }
        }

        return result;
    }

    private static void tryAdd(Level level, Mob mob, List<BlockPos> result, BlockPos feet) {
        if (canStandAt(level, mob, feet)) {
            result.add(feet);
        }
    }

    public static boolean canStandAt(Level level, Mob mob, BlockPos feet) {
        return isSafeForMobFootprint(level, mob, feet);
    }

    public static double movementCost(Level level, Mob mob, BlockPos from, BlockPos to) {
        if (!MovementUtils.isSafeBlock(level, to)) {
            return 9999.0D;
        }

        if (!MovementUtils.isSafeBlock(level, to.below())) {
            return 9999.0D;
        }

        var cost = 1.0D;

        var dy = to.getY() - from.getY();

        if (dy > 0) {
            cost += 1.5D;
        } else if (dy < 0) {
            cost += 0.5D;
        }

        var dangerPaddingBlocks = Math.max(1, Mth.ceil(mob.getBbWidth() / 2.0D));

        for (
            var near : BlockPos.betweenClosed(
                to.offset(-dangerPaddingBlocks, -1, -dangerPaddingBlocks),
                to.offset(dangerPaddingBlocks, 1, dangerPaddingBlocks)
            )
        ) {
            if (!MovementUtils.isSafeBlock(level, near)) {
                cost += 4.0D;
            }
        }

        return cost;
    }

    public static BlockPos normalizeFeet(BlockPos pos) {
        return pos;
    }

    private static boolean isSafeForMobFootprint(Level level, Mob mob, BlockPos feet) {
        var padding = 0.10D;
        var radius = (mob.getBbWidth() / 2.0D) + padding;

        var centerX = feet.getX() + 0.5D;
        var centerZ = feet.getZ() + 0.5D;

        var minX = net.minecraft.util.Mth.floor(centerX - radius);
        var maxX = net.minecraft.util.Mth.floor(centerX + radius);
        var minZ = net.minecraft.util.Mth.floor(centerZ - radius);
        var maxZ = net.minecraft.util.Mth.floor(centerZ + radius);

        for (var x = minX; x <= maxX; x++) {
            for (var z = minZ; z <= maxZ; z++) {
                var checkFeet = new BlockPos(x, feet.getY(), z);
                var checkHead = checkFeet.above();
                var checkGround = checkFeet.below();

                if (!MovementUtils.isSafeBlock(level, checkFeet)) {
                    return false;
                }

                if (!MovementUtils.isSafeBlock(level, checkHead)) {
                    return false;
                }

                if (!level.getBlockState(checkFeet).getCollisionShape(level, checkFeet).isEmpty()) {
                    return false;
                }

                if (!level.getBlockState(checkHead).getCollisionShape(level, checkHead).isEmpty()) {
                    return false;
                }

                if (level.getBlockState(checkGround).getCollisionShape(level, checkGround).isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }
}
