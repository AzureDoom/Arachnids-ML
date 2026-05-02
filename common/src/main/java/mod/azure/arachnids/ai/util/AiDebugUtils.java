package mod.azure.arachnids.ai.util;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import mod.azure.arachnids.CommonMod;

public final class AiDebugUtils {

    private AiDebugUtils() {}

    public static void sendParticlePath(Mob mob, Vec3 from, Vec3 to) {
        if (!CommonMod.getConfig().debugPathingParticlesEnabled)
            return;
        if (!(mob.level() instanceof ServerLevel serverLevel))
            return;

        var color = new Vector3f(1.0F, 0.2F, 0.1F);
        var particle = new DustParticleOptions(color, 1.2F);

        var steps = 24;

        for (var i = 0; i <= steps; i++) {
            var t = i / (double) steps;

            var x = from.x + (to.x - from.x) * t;
            var y = from.y + (to.y - from.y) * t;
            var z = from.z + (to.z - from.z) * t;

            serverLevel.sendParticles(
                particle,
                x,
                y + 0.35D,
                z,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
            );
        }
    }
}
