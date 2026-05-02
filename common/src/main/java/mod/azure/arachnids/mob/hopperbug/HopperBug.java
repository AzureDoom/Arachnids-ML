package mod.azure.arachnids.mob.hopperbug;

import mod.azure.azurelib.common.util.MoveAnalysis;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
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

public class HopperBug extends Monster {

    private final MobBrainRuntime<HopperBug> brainRuntime;

    public final HopperBugAnimationDispatcher animationDispatcher;

    private final MoveAnalysis moveAnalysis;

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
            .add(Attributes.ATTACK_DAMAGE, CommonMod.getConfig().entityConfigs.hopperBugHealth)
            .add(Attributes.KNOCKBACK_RESISTANCE, CommonMod.getConfig().entityConfigs.hopperBugKnockbackRes)
            .add(Attributes.MAX_HEALTH, CommonMod.getConfig().entityConfigs.hopperBugAttackDamage);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source) {
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
        }

        moveAnalysis.update();
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        SquadRegistry.get().remove(this);
        super.remove(reason);
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
