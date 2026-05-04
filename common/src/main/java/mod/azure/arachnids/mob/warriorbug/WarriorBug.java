package mod.azure.arachnids.mob.warriorbug;

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
import org.jetbrains.annotations.NotNull;

import mod.azure.arachnids.CommonMod;
import mod.azure.arachnids.ai.core.MobBrainRuntime;
import mod.azure.arachnids.ai.group.SquadRegistry;
import mod.azure.arachnids.ai.util.NearestHostileTargetSelector;
import mod.azure.arachnids.ai.util.TargetingSystem;
import mod.azure.arachnids.colony.BugColony;
import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.mob.brainbug.BrainBug;
import mod.azure.arachnids.registry.SoundRegistry;
import mod.azure.arachnids.util.MobUtils;

public class WarriorBug extends Monster {

    private final MobBrainRuntime<WarriorBug> brainRuntime;

    public final WarriorBugAnimationDispatcher animationDispatcher;

    private final MoveAnalysis moveAnalysis;

    public WarriorBug(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.brainRuntime = new MobBrainRuntime<>(
            this,
            new TargetingSystem<>(
                new NearestHostileTargetSelector<>(32.0D),
                10
            ),
            WarriorBugTree.create()
        );
        this.animationDispatcher = new WarriorBugAnimationDispatcher(this);
        this.moveAnalysis = new MoveAnalysis(this);
    }

    @Override
    public float maxUpStep() {
        return 3F;
    }

    public static AttributeSupplier.@NotNull Builder createMobAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(Attributes.FOLLOW_RANGE, 16.0F)
            .add(Attributes.ATTACK_DAMAGE, CommonMod.getConfig().entityConfigs.warriorBugAttackDamage)
            .add(Attributes.ARMOR, CommonMod.getConfig().entityConfigs.warriorBugArmor)
            .add(Attributes.ARMOR_TOUGHNESS, CommonMod.getConfig().entityConfigs.warriorBugArmorToughness)
            .add(Attributes.KNOCKBACK_RESISTANCE, CommonMod.getConfig().entityConfigs.warriorBugKnockbackRes)
            .add(Attributes.MAX_HEALTH, CommonMod.getConfig().entityConfigs.warriorBugHealth);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source) {
        if (fallDistance <= 12.0F) {
            return false;
        }
        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
        if (entity instanceof BrainBug) {
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
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide()) {
            BugColony colony = ColonyManager.get().colonyOf(this);
            if (colony != null)
                colony.onMemberHurt(this, source, amount);
        }
        return hurt;
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!this.level().isClientSide()) {
            BugColony colony = ColonyManager.get().colonyOf(this);
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
        return SoundRegistry.WARRIOR_IDLE.get();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return SoundRegistry.WARRIOR_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return SoundRegistry.WARRIOR_DEATH.get();
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState blockIn) {
        this.playSound(SoundRegistry.WARRIOR_MOVING.get(), 0.15F, 1.0F);
    }
}
