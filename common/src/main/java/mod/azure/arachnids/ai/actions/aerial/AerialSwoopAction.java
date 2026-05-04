package mod.azure.arachnids.ai.actions.aerial;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

import mod.azure.arachnids.ai.core.*;
import mod.azure.arachnids.ai.util.AiDebugUtils;

public final class AerialSwoopAction<E extends Mob> implements Action<E> {

    private static final double PULLUP_BLEND = 0.18D;

    private final int totalTicks;

    private final int diveTicks;

    private final double diveSpeed;

    private final double pullUpVerticalPower;

    private final double swoopCooldownTicks;

    private final int priority;

    private final Consumer<E> animationTrigger;

    private Vec3 diveVector;

    private int age;

    private Vec3 diveDestination;

    private boolean hasHit;

    private static final double PICKUP_CHANCE = 0.35D;

    private final BiPredicate<E, LivingEntity> pickupHandler;

    private boolean pickedUpTarget;

    private int pullUpStartAge;

    public AerialSwoopAction(
        int totalTicks,
        int diveTicks,
        double diveSpeed,
        double pullUpVerticalPower,
        int swoopCooldownTicks,
        int priority,
        Consumer<E> animationTrigger,
        BiPredicate<E, LivingEntity> pickupHandler
    ) {
        this.totalTicks = totalTicks;
        this.diveTicks = diveTicks;
        this.diveSpeed = diveSpeed;
        this.pullUpVerticalPower = pullUpVerticalPower;
        this.swoopCooldownTicks = swoopCooldownTicks;
        this.priority = priority;
        this.animationTrigger = animationTrigger;
        this.pickupHandler = pickupHandler;
    }

    @Override
    public void start(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        this.age = 0;

        var target = blackboard.get(AiKeys.TARGET, LivingEntity.class);
        if (target != null) {
            this.diveDestination = target.position().add(0, target.getBbHeight() * 0.5D, 0);

            var toTarget = diveDestination.subtract(mob.position());
            var len = toTarget.length();

            this.diveVector = len > 0.01D ? toTarget.scale(1.0D / len) : new Vec3(0, -1, 0);
        }

        this.hasHit = false;
        this.pickedUpTarget = false;
        this.pullUpStartAge = diveTicks;

        mob.setAggressive(true);
        animationTrigger.accept(mob);
    }

    @Override
    public ActionStatus tick(E mob, Blackboard blackboard, Cooldowns cooldowns) {
        age++;

        if (mob.getHealth() <= 0) {
            mob.setAggressive(false);
            return ActionStatus.INTERRUPTED;
        }

        var target = blackboard.get(AiKeys.TARGET, LivingEntity.class);

        if (age <= pullUpStartAge && !pickedUpTarget) {
            var velocity = diveVector.scale(diveSpeed);
            mob.setDeltaMovement(velocity);
            mob.hasImpulse = true;

            var horizLen = Math.sqrt(diveVector.x * diveVector.x + diveVector.z * diveVector.z);
            var yaw = (float) (Math.atan2(diveVector.z, diveVector.x) * (180.0D / Math.PI)) - 90.0F;
            var pitch = (float) -(Math.atan2(diveVector.y, horizLen) * (180.0D / Math.PI));
            mob.setYRot(yaw);
            mob.yBodyRot = yaw;
            mob.yHeadRot = yaw;
            mob.setXRot(pitch);

            if (target != null && target.isAlive() && !hasHit) {
                var hitInflate = Math.max(1.0D, diveSpeed * 0.5D);

                if (mob.getBoundingBox().inflate(hitInflate).intersects(target.getBoundingBox())) {
                    hasHit = true;

                    var pickedUp = false;

                    if (mob.getRandom().nextDouble() < PICKUP_CHANCE) {
                        pickedUp = pickupHandler.test(mob, target);

                        if (pickedUp) {
                            pickedUpTarget = true;

                            pullUpStartAge = age;

                            var currentVel = mob.getDeltaMovement();
                            mob.setDeltaMovement(
                                currentVel.x * 0.45D,
                                Math.max(currentVel.y, pullUpVerticalPower * 1.65D),
                                currentVel.z * 0.45D
                            );
                            mob.hasImpulse = true;

                            mob.setTarget(null);
                        }
                    }

                    if (!pickedUp) {
                        mob.doHurtTarget(target);

                        if (!target.isAlive()) {
                            blackboard.set(AiKeys.TARGET, null);
                            mob.setTarget(null);
                        }
                    }
                }
            }

            return ActionStatus.RUNNING;
        }

        var current = mob.getDeltaMovement();

        var verticalPower = pickedUpTarget
            ? pullUpVerticalPower * 1.45D
            : pullUpVerticalPower;

        var horizontalDrag = pickedUpTarget
            ? 0.45D
            : 0.7D;

        var blend = pickedUpTarget
            ? 0.38D
            : PULLUP_BLEND;

        var pullTarget = new Vec3(
            current.x * horizontalDrag,
            verticalPower,
            current.z * horizontalDrag
        );

        var blended = current.lerp(pullTarget, blend);
        mob.setDeltaMovement(blended);
        mob.hasImpulse = true;

        mob.setXRot(mob.getXRot() * 0.85F - 4.0F);

        if (age >= totalTicks) {
            cooldowns.set(AiKeys.SWOOP_COOLDOWN, (int) swoopCooldownTicks);
            mob.setXRot(0);
            return ActionStatus.SUCCESS;
        }

        if (diveDestination != null) {
            AiDebugUtils.sendParticlePath(
                mob,
                mob.position(),
                diveDestination
            );
        }
        return ActionStatus.RUNNING;
    }

    @Override
    public void stop(E mob, Blackboard blackboard, ActionStatus reason) {
        mob.setXRot(0);
        var vel = mob.getDeltaMovement();
        mob.setDeltaMovement(vel.x * 0.4D, Math.max(vel.y, 0.05D), vel.z * 0.4D);
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
