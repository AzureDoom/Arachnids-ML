package mod.azure.arachnids.mob.warriorbug;

import mod.azure.azurelib.common.util.MoveAnalysis;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import mod.azure.arachnids.ai.util.CrawlingManager;
import mod.azure.arachnids.ai.util.NearestHostileTargetSelector;
import mod.azure.arachnids.ai.util.TargetingSystem;
import mod.azure.arachnids.ai.util.WallCrawlingMob;
import mod.azure.arachnids.colony.BugColony;
import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.mob.brainbug.BrainBug;
import mod.azure.arachnids.registry.SoundRegistry;
import mod.azure.arachnids.util.MobUtils;

public class WarriorBug extends Monster implements WallCrawlingMob {

    private final MobBrainRuntime<WarriorBug> brainRuntime;

    public final WarriorBugAnimationDispatcher animationDispatcher;

    private final MoveAnalysis moveAnalysis;

    private static final EntityDataAccessor<Boolean> DATA_WALL_CRAWLING =
        SynchedEntityData.defineId(WarriorBug.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Float> DATA_CRAWL_FORWARD_X =
        SynchedEntityData.defineId(WarriorBug.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> DATA_CRAWL_FORWARD_Y =
        SynchedEntityData.defineId(WarriorBug.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> DATA_CRAWL_FORWARD_Z =
        SynchedEntityData.defineId(WarriorBug.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> DATA_CRAWL_UP_X =
        SynchedEntityData.defineId(WarriorBug.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> DATA_CRAWL_UP_Y =
        SynchedEntityData.defineId(WarriorBug.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> DATA_CRAWL_UP_Z =
        SynchedEntityData.defineId(WarriorBug.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> DATA_CRAWL_DIST_FROM_BLOCK =
        SynchedEntityData.defineId(WarriorBug.class, EntityDataSerializers.FLOAT);

    private int wallCrawlGraceTicks;

    private Vec3 oldCrawlForward = new Vec3(0.0D, 0.0D, 1.0D);

    private Vec3 oldCrawlUp = new Vec3(0.0D, 1.0D, 0.0D);

    private double oldCrawlDistFromBlock;

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

        syncOldCrawlRenderState();

        if (!this.level().isClientSide()) {
            CrawlingManager.setWallCrawling(this, false);

            var colony = ColonyManager.get().colonyOf(this);
            if (colony != null)
                this.setPersistenceRequired();
            brainRuntime.tick();

            CrawlingManager.updateWallCrawlingPhysics(this);

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
                colony.onMemberHurt(source, amount);
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

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);

        builder.define(DATA_WALL_CRAWLING, false);

        builder.define(DATA_CRAWL_FORWARD_X, 0.0F);
        builder.define(DATA_CRAWL_FORWARD_Y, 0.0F);
        builder.define(DATA_CRAWL_FORWARD_Z, 1.0F);

        builder.define(DATA_CRAWL_UP_X, 0.0F);
        builder.define(DATA_CRAWL_UP_Y, 1.0F);
        builder.define(DATA_CRAWL_UP_Z, 0.0F);

        builder.define(DATA_CRAWL_DIST_FROM_BLOCK, 0.0F);
    }

    @Override
    public boolean arachnids$isWallCrawling() {
        return this.entityData.get(DATA_WALL_CRAWLING);
    }

    @Override
    public void arachnids$setWallCrawling(boolean crawling) {
        this.entityData.set(DATA_WALL_CRAWLING, crawling);

        if (crawling) {
            this.wallCrawlGraceTicks = 4;
        }
    }

    @Override
    public int arachnids$getWallCrawlGraceTicks() {
        return wallCrawlGraceTicks;
    }

    @Override
    public void arachnids$setWallCrawlGraceTicks(int ticks) {
        this.wallCrawlGraceTicks = ticks;
    }

    @Override
    public Vec3 arachnids$getCrawlForward() {
        return new Vec3(
            this.entityData.get(DATA_CRAWL_FORWARD_X),
            this.entityData.get(DATA_CRAWL_FORWARD_Y),
            this.entityData.get(DATA_CRAWL_FORWARD_Z)
        );
    }

    @Override
    public Vec3 arachnids$getOldCrawlForward() {
        return oldCrawlForward;
    }

    @Override
    public Vec3 arachnids$getCrawlUp() {
        return new Vec3(
            this.entityData.get(DATA_CRAWL_UP_X),
            this.entityData.get(DATA_CRAWL_UP_Y),
            this.entityData.get(DATA_CRAWL_UP_Z)
        );
    }

    @Override
    public Vec3 arachnids$getOldCrawlUp() {
        return oldCrawlUp;
    }

    @Override
    public double arachnids$getCrawlDistFromBlock() {
        return this.entityData.get(DATA_CRAWL_DIST_FROM_BLOCK);
    }

    @Override
    public double arachnids$getOldCrawlDistFromBlock() {
        return oldCrawlDistFromBlock;
    }

    @Override
    public void arachnids$setCrawlOrientation(Vec3 forward, Vec3 up, double distFromBlock) {
        if (forward.lengthSqr() > 0.0001D) {
            var normalizedForward = forward.normalize();

            this.entityData.set(DATA_CRAWL_FORWARD_X, (float) normalizedForward.x);
            this.entityData.set(DATA_CRAWL_FORWARD_Y, (float) normalizedForward.y);
            this.entityData.set(DATA_CRAWL_FORWARD_Z, (float) normalizedForward.z);
        }

        if (up.lengthSqr() > 0.0001D) {
            var normalizedUp = up.normalize();

            this.entityData.set(DATA_CRAWL_UP_X, (float) normalizedUp.x);
            this.entityData.set(DATA_CRAWL_UP_Y, (float) normalizedUp.y);
            this.entityData.set(DATA_CRAWL_UP_Z, (float) normalizedUp.z);
        }

        this.entityData.set(DATA_CRAWL_DIST_FROM_BLOCK, (float) distFromBlock);
    }

    private void syncOldCrawlRenderState() {
        this.oldCrawlForward = this.arachnids$getCrawlForward();
        this.oldCrawlUp = this.arachnids$getCrawlUp();
        this.oldCrawlDistFromBlock = this.arachnids$getCrawlDistFromBlock();
    }
}
