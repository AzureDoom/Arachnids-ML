package mod.azure.arachnids.ai.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

import mod.azure.arachnids.util.ModTags;

public final class MovementUtils {

    private static final double DEFAULT_LOOK_AHEAD = 1.25D;

    private static final int[] STEER_ANGLES = { 30, -30, 60, -60, 90, -90, 120, -120, 150, -150 };

    private MovementUtils() {}

    public static boolean isSafeBlock(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.is(ModTags.DANGER_BLOCKS))
            return false;
        return !state.getFluidState().is(ModTags.DANGER_FLUIDS);
    }

    private static boolean hasGroundWithinDrop(Level level, BlockPos feetPos, int maxDrop) {
        for (var drop = 1; drop <= maxDrop; drop++) {
            var ground = feetPos.below(drop);

            if (
                !level.getBlockState(ground).getCollisionShape(level, ground).isEmpty()
                    && isSafeBlock(level, ground)
            ) {
                return true;
            }
        }

        return false;
    }

    public static boolean isSafeAhead(Mob mob, Vec3 forward, double distance) {
        var level = mob.level();
        var feetY = mob.getBoundingBox().minY;
        var side = new Vec3(-forward.z, 0.0D, forward.x);
        var halfW = (mob.getBbWidth() / 2.0D) + 0.15D;

        for (var d = 0.25D; d <= distance; d += 0.25D) {
            var center = mob.position().add(forward.scale(d));

            for (var s = -halfW; s <= halfW; s += halfW / 2.0D) {
                var sample = center.add(side.scale(s));

                var feetPos = BlockPos.containing(sample.x, feetY, sample.z);
                var groundPos = feetPos.below();
                var headPos = feetPos.above();

                if (!isSafeBlock(level, feetPos))
                    return false;

                if (!isSafeBlock(level, headPos))
                    return false;

                if (!level.getBlockState(feetPos).getCollisionShape(level, feetPos).isEmpty())
                    return false;

                if (!level.getBlockState(headPos).getCollisionShape(level, headPos).isEmpty())
                    return false;

                if (!hasGroundWithinDrop(level, feetPos, 9))
                    return false;

                if (level.getBlockState(feetPos).getFluidState().is(FluidTags.LAVA))
                    return false;
                if (level.getBlockState(groundPos).getFluidState().is(FluidTags.LAVA))
                    return false;
            }
        }

        return true;
    }

    public static Vec3 findSafeMovement(Mob mob, Vec3 desiredMovement, int[] steerBias) {
        var horizontal = new Vec3(desiredMovement.x, 0.0D, desiredMovement.z);
        var length = horizontal.length();

        if (length < 0.001D)
            return desiredMovement;

        var forward = horizontal.normalize();

        if (isSafeAhead(mob, forward, DEFAULT_LOOK_AHEAD)) {
            steerBias[0] = 0;
            return desiredMovement;
        }

        var angles = sortByBias(steerBias[0]);

        for (var angleDeg : angles) {
            var rotated = rotate(forward, angleDeg);
            if (isSafeAhead(mob, rotated, DEFAULT_LOOK_AHEAD)) {
                steerBias[0] = angleDeg > 0 ? 1 : -1;
                return rotated.scale(length);
            }
        }

        if (steerBias[0] != 0) {
            var wallFollow = rotate(forward, steerBias[0] > 0 ? 90 : -90);
            if (isSafeAhead(mob, wallFollow, DEFAULT_LOOK_AHEAD * 0.5D)) {
                return wallFollow.scale(length);
            }
        }

        return Vec3.ZERO;
    }

    private static Vec3 rotate(Vec3 forward, int angleDeg) {
        var radians = Math.toRadians(angleDeg);
        var cos = Math.cos(radians);
        var sin = Math.sin(radians);
        return new Vec3(
            forward.x * cos - forward.z * sin,
            0.0D,
            forward.x * sin + forward.z * cos
        );
    }

    private static int[] sortByBias(int bias) {
        if (bias == 0)
            return MovementUtils.STEER_ANGLES;

        var preferred = new ArrayList<Integer>();
        var other = new ArrayList<Integer>();

        for (var a : MovementUtils.STEER_ANGLES) {
            if ((bias > 0 && a > 0) || (bias < 0 && a < 0)) {
                preferred.add(a);
            } else {
                other.add(a);
            }
        }

        preferred.addAll(other);
        return preferred.stream().mapToInt(Integer::intValue).toArray();
    }

    public static Vec3 getDangerEntityRepulsion(Mob mob) {
        final var dangerRadius = 5.0D;
        final var dangerRadiusSqr = dangerRadius * dangerRadius;
        final var avoidStrength = 1.25D;

        var away = Vec3.ZERO;
        var box = mob.getBoundingBox().inflate(dangerRadius);

        for (var entity : mob.level().getEntities(mob, box)) {
            if (!entity.getType().is(ModTags.DANGER_ENTITIES)) {
                continue;
            }

            var offset = mob.position().subtract(entity.position());
            var distSqr = offset.lengthSqr();

            if (distSqr > dangerRadiusSqr) {
                continue;
            }

            if (distSqr < 0.0001D) {
                offset = Vec3.directionFromRotation(0.0F, mob.getYRot()).scale(-1.0D);
                distSqr = 0.0001D;
            }

            var distance = Math.sqrt(distSqr);
            var weight = 1.0D - distance / dangerRadius;

            away = away.add(offset.normalize().scale(weight * avoidStrength));
        }

        return away;
    }

    public static Vec3 steerAwayFromDangerEntities(Mob mob, Vec3 desiredMovement) {
        var away = getDangerEntityRepulsion(mob);

        if (away.lengthSqr() < 0.0001D) {
            return desiredMovement;
        }

        var desiredHorizontal = new Vec3(desiredMovement.x, 0.0D, desiredMovement.z);
        var desiredLength = desiredHorizontal.length();

        if (desiredLength < 0.001D) {
            return away.normalize().scale(0.12D);
        }

        var blended = desiredHorizontal.add(away);

        if (blended.lengthSqr() < 0.0001D) {
            return away.normalize().scale(desiredLength);
        }

        return blended.normalize().scale(desiredLength);
    }

    public static boolean hasSafeLandingAfterLeap(Mob mob, Vec3 direction, double distance) {
        if (direction.lengthSqr() < 0.0001D) {
            return false;
        }

        var level = mob.level();
        var forward = new Vec3(direction.x, 0.0D, direction.z).normalize();

        var landingCenter = mob.position().add(forward.scale(distance));
        var feetY = mob.getBoundingBox().minY;

        var feetPos = BlockPos.containing(
            landingCenter.x,
            feetY,
            landingCenter.z
        );

        var headPos = feetPos.above();

        if (!isSafeBlock(level, feetPos)) {
            return false;
        }

        if (!isSafeBlock(level, headPos)) {
            return false;
        }

        if (!level.getBlockState(feetPos).getCollisionShape(level, feetPos).isEmpty()) {
            return false;
        }

        if (!level.getBlockState(headPos).getCollisionShape(level, headPos).isEmpty()) {
            return false;
        }

        return hasGroundWithinDrop(level, feetPos, 4);
    }

    public static boolean hasNearbyDangerEntity(Mob mob) {
        return getDangerEntityRepulsion(mob).lengthSqr() > 0.0001D;
    }

    public static boolean canWallCrawl(Mob mob) {
        return !mob.isInWater() && !mob.isVehicle();
    }

    public static boolean isClimbable(Level level, int x, int y, int z, boolean generous) {
        var reachBox = new AABB(x, y, z, x + 1, y + 1, z + 1)
            .inflate(generous ? 1.5D : 0.5D);

        return !level.noBlockCollision(null, reachBox);
    }

    public static boolean isClimbable(Level level, BlockPos pos, boolean generous) {
        return isClimbable(level, pos.getX(), pos.getY(), pos.getZ(), generous);
    }

    public static boolean isSafeClimbNode(Level level, Mob mob, BlockPos feet) {
        var head = feet.above();

        if (!isSafeBlock(level, feet)) {
            return false;
        }

        if (!isSafeBlock(level, head)) {
            return false;
        }

        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) {
            return false;
        }

        if (!level.getBlockState(head).getCollisionShape(level, head).isEmpty()) {
            return false;
        }

        return isClimbable(level, feet, false);
    }

    public static boolean needsWallCrawl(Mob mob, Vec3 wanted) {
        if (!canWallCrawl(mob)) {
            return false;
        }

        var center = BlockPos.containing(
            mob.getBoundingBox().getCenter().x,
            mob.getBoundingBox().getCenter().y,
            mob.getBoundingBox().getCenter().z
        );

        if (!isClimbable(mob.level(), center, true)) {
            return false;
        }

        var current = requiredMovementAt(mob, mob.blockPosition());
        if (current == MovementType.CLIMB) {
            return true;
        }

        var wantedBlock = BlockPos.containing(wanted.x, wanted.y, wanted.z);
        var wantedType = requiredMovementAt(mob, wantedBlock);

        if (wantedType == MovementType.CLIMB) {
            return true;
        }

        return wantedBlock.equals(mob.blockPosition().above())
            && wantedType == MovementType.JUMP;
    }

    public enum MovementType {
        WALK,
        JUMP,
        CLIMB
    }

    public static MovementType requiredMovementAt(Mob mob, BlockPos pos) {
        var below = pos.below();
        var stateBelow = mob.level().getBlockState(below);

        if (stateBelow.entityCanStandOn(mob.level(), below, mob)) {
            return MovementType.WALK;
        }

        var twoBelow = below.below();
        var stateTwoBelow = mob.level().getBlockState(twoBelow);

        if (stateTwoBelow.entityCanStandOn(mob.level(), twoBelow, mob)) {
            return MovementType.JUMP;
        }

        return MovementType.CLIMB;
    }

    public static Vec3 computeWallCrawlVelocity(Mob mob, Vec3 wanted, double speed) {
        var center = mob.getBoundingBox().getCenter();
        var offset = wanted.subtract(center);
        var dist = offset.length();

        if (dist < 0.1D) {
            return Vec3.ZERO;
        }

        var clampedSpeed = Math.min(speed, dist);
        return offset.normalize().scale(clampedSpeed);
    }
}
