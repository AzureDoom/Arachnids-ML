package mod.azure.arachnids.mob.worker;

import mod.azure.azurelib.common.util.MoveAnalysis;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import mod.azure.arachnids.CommonMod;
import mod.azure.arachnids.ai.core.MobBrainRuntime;
import mod.azure.arachnids.ai.group.SquadRegistry;
import mod.azure.arachnids.ai.util.NearestHostileTargetSelector;
import mod.azure.arachnids.ai.util.TargetingSystem;
import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.registry.SoundRegistry;
import mod.azure.arachnids.util.MobUtils;
import mod.azure.arachnids.util.ModTags;

public class WorkerBug extends PathfinderMob {

    private final MobBrainRuntime<WorkerBug> brainRuntime;

    public final WorkerBugAnimationDispatcher animationDispatcher;

    private final MoveAnalysis moveAnalysis;

    public WorkerBug(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.brainRuntime = new MobBrainRuntime<>(
            this,
            new TargetingSystem<>(
                new NearestHostileTargetSelector<>(16.0D),
                10
            ),
            WorkerBugTree.create()
        );
        this.animationDispatcher = new WorkerBugAnimationDispatcher(this);
        this.moveAnalysis = new MoveAnalysis(this);
    }

    @Override
    public float maxUpStep() {
        return 3F;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source) {
        if (fallDistance <= 12.0F) {
            return false;
        }
        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    public static AttributeSupplier.@NotNull Builder createMobAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(Attributes.FOLLOW_RANGE, 16.0F)
            .add(Attributes.ATTACK_DAMAGE, CommonMod.getConfig().entityConfigs.workerBugAttackDamage)
            .add(Attributes.MAX_HEALTH, CommonMod.getConfig().entityConfigs.workerBugHealth);
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
        if (entity.getType().is(ModTags.ARACHNIDS)) {
            return;
        }

        super.doPush(entity);
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= 80 && !this.level().isClientSide() && !this.isRemoved()) {
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
            brainRuntime.tick();

            if (this.isOnFire() && this.tickCount % 2 == 0) {
                MobUtils.spawnFireParticles(this, (ServerLevel) this.level());
            }
        }

        moveAnalysis.update();
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide()) {
            var colony = ColonyManager.get().colonyOf(this);
            if (colony != null)
                colony.onMemberHurt(this, source, amount);
        }
        return hurt;
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!this.level().isClientSide()) {
            var colony = ColonyManager.get().colonyOf(this);
            if (colony != null)
                colony.onMemberDeath(this);
        }
        SquadRegistry.get().remove(this);
        super.remove(reason);
    }

    public void updateAnimations() {
        var isMovingOnGround = moveAnalysis.isMovingHorizontally() && onGround();

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

        if (!this.isAggressive()) {
            animationDispatcher.clientIdle();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundRegistry.WORKER_IDLE.get();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return SoundRegistry.WORKER_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return SoundRegistry.WORKER_DEATH.get();
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState blockIn) {
        this.playSound(SoundRegistry.WARRIOR_MOVING.get(), 0.15F, 1.0F);
    }
}
