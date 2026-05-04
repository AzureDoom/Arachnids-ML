package mod.azure.arachnids.util;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class MobUtils {

    private MobUtils() {}

    public static void spawnFireParticles(LivingEntity livingEntity, ServerLevel level) {
        var random = livingEntity.getRandom();
        var box = livingEntity.getBoundingBox();

        for (var i = 0; i < 4; i++) {
            var x = box.minX + random.nextDouble() * box.getXsize();
            var y = box.minY + random.nextDouble() * box.getYsize();
            var z = box.minZ + random.nextDouble() * box.getZsize();

            level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.02D, 0.03D, 0.02D, 0.0D);

            if (random.nextFloat() < 0.35F) {
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.02D, 0.03D, 0.02D, 0.0D);
            }
        }
    }
}
