package mod.azure.arachnids.ai.actions;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.function.Consumer;

import mod.azure.arachnids.ai.core.*;

public final class ScreamAction<E extends Mob> implements Action<E> {

    private final String cooldownKey;

    private final int cooldownTicks;

    private final int totalTicks;

    private final int damageTick;

    private final float screamRadius;

    private final double minDistanceSq;

    private final float screamDamage;

    private final int priority;

    private final Consumer<E> animationTrigger;

    private int age;

    public ScreamAction(
        String cooldownKey,
        int cooldownTicks,
        int totalTicks,
        int damageTick,
        float screamRadius,
        double minDistance,
        float screamDamage,
        int priority,
        Consumer<E> animationTrigger
    ) {
        this.cooldownKey = cooldownKey;
        this.cooldownTicks = cooldownTicks;
        this.totalTicks = totalTicks;
        this.damageTick = damageTick;
        this.screamRadius = screamRadius;
        this.minDistanceSq = minDistance * minDistance;
        this.screamDamage = screamDamage;
        this.priority = priority;
        this.animationTrigger = animationTrigger;
    }

    @Override
    public void start(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        this.age = 0;
        mob.hasImpulse = true;
        mob.setAggressive(true);
        animationTrigger.accept(mob);
    }

    @Override
    public ActionStatus tick(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        age++;
        mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);
        mob.hasImpulse = true;

        if (mob.getHealth() <= 0) {
            mob.setAggressive(false);
            return ActionStatus.INTERRUPTED;
        }

        var target = blackboard.get(AiKeys.TARGET, LivingEntity.class);
        if (target == null || !target.isAlive()) {
            return ActionStatus.FAILURE;
        }

        if (mob.distanceToSqr(target) <= minDistanceSq) {
            cooldowns.set(cooldownKey, cooldownTicks / 2);
            mob.setAggressive(false);
            return ActionStatus.FAILURE;
        }

        var dx = target.getX() - mob.getX();
        var dz = target.getZ() - mob.getZ();
        var yaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;

        if (age == damageTick) {
            var area = mob.getBoundingBox().inflate(screamRadius);
            var victims = mob.level()
                .getEntitiesOfClass(
                    LivingEntity.class,
                    area,
                    e -> e != mob && e.isAlive()
                );
            for (var victim : victims) {
                victim.hurt(mob.damageSources().mobAttack(mob), screamDamage);
            }

            if (mob.level() instanceof ServerLevel serverLevel) {
                var dirX = (target.getX() - mob.getX());
                var dirZ = (target.getZ() - mob.getZ());
                var len = Math.sqrt(dirX * dirX + dirZ * dirZ);
                if (len > 0.001D) {
                    dirX /= len;
                    dirZ /= len;
                }
                for (var i = 0; i < 10; i++) {
                    var spread = (mob.getRandom().nextDouble() - 0.5D) * 2.0D;
                    var spawnX = mob.getX() + dirX * i * 0.5D + spread * 0.3D;
                    var spawnY = mob.getEyeY() - 0.2D;
                    var spawnZ = mob.getZ() + dirZ * i * 0.5D + spread * 0.3D;
                    serverLevel.sendParticles(
                        ParticleTypes.SONIC_BOOM,
                        spawnX,
                        spawnY,
                        spawnZ,
                        1,
                        0D,
                        0D,
                        0D,
                        0D
                    );
                }
            }
        }

        if (age >= totalTicks) {
            cooldowns.set(cooldownKey, cooldownTicks);
            mob.setAggressive(false);
            return ActionStatus.SUCCESS;
        }

        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(E mob, Blackboard blackboard, ActionStatus reason) {
        mob.setAggressive(false);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public int priority() {
        return priority;
    }
}
