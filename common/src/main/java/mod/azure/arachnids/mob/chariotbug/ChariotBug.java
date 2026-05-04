package mod.azure.arachnids.mob.chariotbug;

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
import mod.azure.arachnids.ai.transport.BrainBugCarrySystem;
import mod.azure.arachnids.ai.util.NearestHostileTargetSelector;
import mod.azure.arachnids.ai.util.TargetingSystem;
import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.mob.brainbug.BrainBug;
import mod.azure.arachnids.registry.SoundRegistry;
import mod.azure.arachnids.util.MobUtils;

public class ChariotBug extends PathfinderMob {

    private final MobBrainRuntime<ChariotBug> brainRuntime;

    public final ChariotBugAnimationDispatcher animationDispatcher;

    private final MoveAnalysis moveAnalysis;

    public ChariotBug(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.brainRuntime = new MobBrainRuntime<>(
            this,
            new TargetingSystem<>(
                new NearestHostileTargetSelector<>(64.0D),
                10
            ),
            ChariotBugTree.create()
        );
        this.animationDispatcher = new ChariotBugAnimationDispatcher(this);
        this.moveAnalysis = new MoveAnalysis(this);
    }

    public static AttributeSupplier.@NotNull Builder createMobAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(Attributes.FOLLOW_RANGE, 16.0F)
            .add(Attributes.ATTACK_DAMAGE, CommonMod.getConfig().entityConfigs.chariotBugAttackDamage)
            .add(Attributes.MAX_HEALTH, CommonMod.getConfig().entityConfigs.chariotBugHealth);
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
    public void remove(@NotNull RemovalReason reason) {
        SquadRegistry.get().remove(this);
        super.remove(reason);
    }

    public void updateAnimations() {
        var isMovingHorizontally = moveAnalysis.isMovingHorizontally();
        var isMovingOnGround = isMovingHorizontally && onGround();

        if (this.isDeadOrDying() || this.getHealth() <= 0) {
            animationDispatcher.clientDeath();
            return;
        }

        if (isMovingOnGround) {
            animationDispatcher.clientWalk();
            return;
        }

        animationDispatcher.clientIdle();
    }

    @Override
    public float maxUpStep() {
        return 2F;
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
        if (
            entity instanceof BrainBug brain
                && !BrainBugCarrySystem.get().canMoveCarried(brain)
        ) {
            return;
        }

        super.doPush(entity);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundRegistry.CHARIOT_IDLE.get();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return SoundRegistry.CHARIOT_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return SoundRegistry.CHARIOT_DEATH.get();
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState blockIn) {
        this.playSound(SoundRegistry.CHARIOT_MOVING.get(), 0.15F, 1.0F);
    }
}
