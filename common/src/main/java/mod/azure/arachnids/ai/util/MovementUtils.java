package mod.azure.arachnids.ai.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

import mod.azure.arachnids.util.ModTags;

public final class MovementUtils {

    private static final double DEFAULT_LOOK_AHEAD = 3.5D;

    private static final int[] STEER_ANGLES = { 30, -30, 60, -60, 90, -90, 120, -120, 150, -150 };

    private MovementUtils() {}

    public static boolean isSafeBlock(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.is(ModTags.DANGER_BLOCKS))
            return false;
        return !state.getFluidState().is(ModTags.DANGER_FLUIDS);
    }

    public static boolean isSafeAhead(Mob mob, Vec3 forward, double distance) {
        var level = mob.level();
        var feetY = mob.getBoundingBox().minY;
        var side = new Vec3(-forward.z, 0.0D, forward.x);
        var halfW = (mob.getBbWidth() / 2.0D) + 0.6D;

        for (var d = 0.25D; d <= distance; d += 0.25D) {
            var center = mob.position().add(forward.scale(d));

            for (var s = -halfW; s <= halfW; s += halfW / 2.0D) {
                var sample = center.add(side.scale(s));

                var feetPos = BlockPos.containing(sample.x, feetY, sample.z);
                var groundPos = BlockPos.containing(sample.x, feetY - 0.5D, sample.z);
                var headPos = feetPos.above();

                if (!isSafeBlock(level, feetPos))
                    return false;
                if (!isSafeBlock(level, headPos))
                    return false;
                if (!isSafeBlock(level, groundPos))
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
}
