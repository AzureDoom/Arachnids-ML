package mod.azure.arachnids.mob.brainbug;

import mod.azure.azurelib.common.util.MoveAnalysis;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
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
import mod.azure.arachnids.mob.chariotbug.ChariotBug;
import mod.azure.arachnids.registry.SoundRegistry;
import mod.azure.arachnids.util.MobUtils;
import mod.azure.arachnids.util.ModTags;

public class BrainBug extends PathfinderMob {

    private final MobBrainRuntime<BrainBug> brainRuntime;

    public final BrainBugAnimationDispatcher animationDispatcher;

    private final MoveAnalysis moveAnalysis;

    private boolean colonyRegistered = false;

    public BrainBug(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.brainRuntime = new MobBrainRuntime<>(
            this,
            new TargetingSystem<>(
                new NearestHostileTargetSelector<>(64.0D),
                10
            ),
            BrainBugTree.create()
        );
        this.animationDispatcher = new BrainBugAnimationDispatcher(this);
        this.moveAnalysis = new MoveAnalysis(this);
    }

    public static AttributeSupplier.@NotNull Builder createMobAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(Attributes.FOLLOW_RANGE, 16.0F)
            .add(Attributes.ATTACK_DAMAGE, CommonMod.getConfig().entityConfigs.brainBugAttackDamage)
            .add(Attributes.MAX_HEALTH, CommonMod.getConfig().entityConfigs.brainBugHealth)
            .add(Attributes.MOVEMENT_SPEED, 0.12F);
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
            if (!colonyRegistered) {
                if (ColonyManager.get().get(this) == null) {
                    ColonyManager.get().getOrCreate(this, (ServerLevel) this.level());
                    this.setPersistenceRequired();
                }
                colonyRegistered = true;
            }
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
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        var colony = ColonyManager.get().get(this);
        if (colony != null) {
            tag.put("Colony", colony.save());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("Colony")) {
            ColonyManager.get().load(this, tag.getCompound("Colony"), (ServerLevel) level());
            colonyRegistered = true;
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!this.level().isClientSide()) {
            ColonyManager.get().remove(this);
        }

        SquadRegistry.get().remove(this);
        BrainBugCarrySystem.get().removeBrain(this);
        super.remove(reason);
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
        if (entity instanceof ChariotBug && !BrainBugCarrySystem.get().canMoveCarried(this)) {
            return;
        }
        if (entity.getType().is(ModTags.ARACHNIDS)) {
            return;
        }

        super.doPush(entity);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        var hurt = super.hurt(source, amount);

        if (hurt && amount > 0.0F && !this.level().isClientSide()) {
            if (source.getEntity() instanceof LivingEntity attacker) {
                BrainBugCarrySystem.get().signalUnderAttack(this, attacker.position());
            } else {
                BrainBugCarrySystem.get().signalUnderAttack(this, this.position());
            }

            var colony = ColonyManager.get().get(this);
            if (colony != null) {
                colony.onMemberHurt(this, source, amount);
            }
        }

        return hurt;
    }

    public void updateAnimations() {
        if (this.isDeadOrDying() || this.getHealth() <= 0) {
            animationDispatcher.clientDeath();
            return;
        }

        animationDispatcher.clientIdle();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundRegistry.BRAINBUG_IDLE.get();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return SoundRegistry.BRAINBUG_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return SoundRegistry.BRAINBUG_DEATH.get();
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState blockIn) {
        /* */
    }
}
