package mod.azure.arachnids.mob.hopperbug;

import mod.azure.azurelib.common.util.MoveAnalysis;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import mod.azure.arachnids.CommonMod;
import mod.azure.arachnids.ai.core.MobBrainRuntime;
import mod.azure.arachnids.ai.group.SquadRegistry;
import mod.azure.arachnids.ai.util.NearestHostileTargetSelector;
import mod.azure.arachnids.ai.util.TargetingSystem;
import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.registry.SoundRegistry;
import mod.azure.arachnids.util.MobUtils;

public class HopperBug extends Monster {

    private final MobBrainRuntime<HopperBug> brainRuntime;

    public final HopperBugAnimationDispatcher animationDispatcher;

    private final MoveAnalysis moveAnalysis;

    private static final int CARRY_DURATION_TICKS = 200;

    private LivingEntity carriedTarget;

    private static final double FALL_DAMAGE_DROP_MULTIPLIER = 2.0D;

    private static final double MAX_CARRY_UPWARD_SPEED = 0.46D;

    private int carryTicks;

    public HopperBug(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.brainRuntime = new MobBrainRuntime<>(
            this,
            new TargetingSystem<>(
                new NearestHostileTargetSelector<>(64.0D),
                10
            ),
            HopperBugTree.create()
        );
        this.animationDispatcher = new HopperBugAnimationDispatcher(this);
        this.moveAnalysis = new MoveAnalysis(this);
    }

    @Override
    public float maxUpStep() {
        return 3F;
    }

    public static AttributeSupplier.@NotNull Builder createMobAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(Attributes.FOLLOW_RANGE, 16.0F)
            .add(Attributes.ATTACK_DAMAGE, CommonMod.getConfig().entityConfigs.hopperBugAttackDamage)
            .add(Attributes.KNOCKBACK_RESISTANCE, CommonMod.getConfig().entityConfigs.hopperBugKnockbackRes)
            .add(Attributes.MAX_HEALTH, CommonMod.getConfig().entityConfigs.hopperBugHealth);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= 40 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            var colony = ColonyManager.get().colonyOf(this);
            if (colony != null)
                this.setPersistenceRequired();

            tickCarriedTarget();

            brainRuntime.tick();

            if (this.isOnFire() && this.tickCount % 2 == 0) {
                MobUtils.spawnFireParticles(this, (ServerLevel) this.level());
            }
        }

        moveAnalysis.update();
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        dropCarriedTarget();
        SquadRegistry.get().remove(this);
        super.remove(reason);
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction moveFunction) {
        if (!this.hasPassenger(passenger)) {
            return;
        }

        var forward = Vec3.directionFromRotation(0.0F, this.getYRot());
        var offset = forward.scale(-0.45D);

        var x = this.getX() + offset.x;
        var y = this.getY() + this.getBbHeight() * 0.45D;
        var z = this.getZ() + offset.z;

        moveFunction.accept(passenger, x, y, z);
    }

    public boolean isCarryingTarget() {
        return carriedTarget != null
            && carriedTarget.isAlive()
            && carriedTarget.isPassenger()
            && carriedTarget.getVehicle() == this;
    }

    public boolean tryPickUpTarget(LivingEntity target) {
        if (this.level().isClientSide()) {
            return false;
        }

        if (target == null || !target.isAlive()) {
            return false;
        }

        if (target == this) {
            return false;
        }

        if (this.isPassenger()) {
            return false;
        }

        if (target.isPassenger()) {
            return false;
        }

        if (target.isVehicle()) {
            return false;
        }

        if (isCarryingTarget()) {
            return false;
        }

        target.stopRiding();

        if (!target.startRiding(this, true)) {
            return false;
        }

        this.carriedTarget = target;
        this.carryTicks = 0;

        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
        target.hurtMarked = true;

        return true;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    public void dropCarriedTarget() {
        if (carriedTarget == null) {
            return;
        }

        var target = carriedTarget;
        carriedTarget = null;
        carryTicks = 0;

        if (target.getVehicle() == this) {
            target.stopRiding();
        }

        target.setDeltaMovement(
            this.getDeltaMovement().x * 0.35D,
            1.55D,
            this.getDeltaMovement().z * 0.35D
        );

        target.hurtMarked = true;
    }

    public void updateAnimations() {
        var isMovingHorizontally = moveAnalysis.isMovingHorizontally();
        var isMovingOnGround = isMovingHorizontally && onGround();
        var isMovingInAir = isMovingHorizontally && !onGround();

        if (this.isDeadOrDying() || this.getHealth() <= 0) {
            animationDispatcher.clientDeath();
            return;
        }

        if (isMovingOnGround) {
            if (this.isAggressive() && !this.swinging) {
                animationDispatcher.clientRun();
            } else {
                animationDispatcher.clientWalk();
            }
            return;
        }

        if (isMovingInAir) {
            animationDispatcher.clientFly();
            return;
        }

        animationDispatcher.clientIdle();
    }

    private void tickCarriedTarget() {
        if (carriedTarget == null) {
            return;
        }

        if (
            !carriedTarget.isAlive()
                || !carriedTarget.isPassenger()
                || carriedTarget.getVehicle() != this
        ) {
            carriedTarget = null;
            carryTicks = 0;
            return;
        }

        carryTicks++;

        var estimatedFallDistance = estimateFallDistanceFrom(carriedTarget);

        var damagingDropDistance = Math.max(
            1.0D,
            carriedTarget.getMaxFallDistance() * FALL_DAMAGE_DROP_MULTIPLIER
        );

        if (estimatedFallDistance >= damagingDropDistance) {
            dropCarriedTarget();
            return;
        }

        if (carryTicks >= 200) {
            dropCarriedTarget();
            return;
        }

        limitCarryUpwardDrift();
    }

    private double estimateFallDistanceFrom(LivingEntity entity) {
        var level = entity.level();

        var x = entity.getX();
        var z = entity.getZ();
        var startY = entity.getBoundingBox().minY;

        var start = BlockPos.containing(x, startY, z);
        var minY = level.getMinBuildHeight();

        for (var y = start.getY(); y >= minY; y--) {
            var pos = new BlockPos(start.getX(), y, start.getZ());
            var state = level.getBlockState(pos);

            if (!state.getCollisionShape(level, pos).isEmpty()) {
                return Math.max(0.0D, startY - (y + 1.0D));
            }
        }

        return Double.MAX_VALUE;
    }

    private void limitCarryUpwardDrift() {
        var velocity = this.getDeltaMovement();

        if (velocity.y > MAX_CARRY_UPWARD_SPEED) {
            this.setDeltaMovement(
                velocity.x,
                MAX_CARRY_UPWARD_SPEED,
                velocity.z
            );
            this.hasImpulse = true;
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundRegistry.HOPPER_IDLE.get();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return SoundRegistry.HOPPER_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return SoundRegistry.HOPPER_DEATH.get();
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState blockIn) {
        this.playSound(SoundRegistry.HOPPER_MOVING.get(), 0.15F, 1.0F);
    }
}
