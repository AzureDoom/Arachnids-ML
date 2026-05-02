package mod.azure.arachnids.mob.hopperbug;

import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;

import mod.azure.arachnids.util.CommonStrings;

public class HopperBugAnimationDispatcher {

    private final AzCommand idleCommand = AzCommand.create(
        CommonStrings.BASE_CONTROLLER,
        CommonStrings.IDLE_ANIMATION_NAME,
        AzPlayBehaviors.LOOP
    );

    private final AzCommand walkCommand = AzCommand.create(
        CommonStrings.BASE_CONTROLLER,
        CommonStrings.WALK_ANIMATION_NAME,
        AzPlayBehaviors.LOOP
    );

    private final AzCommand runCommand = AzCommand.create(
        CommonStrings.BASE_CONTROLLER,
        CommonStrings.RUN_ANIMATION_NAME,
        AzPlayBehaviors.LOOP
    );

    private final AzCommand flyCommand = AzCommand.create(
        CommonStrings.BASE_CONTROLLER,
        CommonStrings.FLYING_ANIMATION_NAME,
        AzPlayBehaviors.LOOP
    );

    private final AzCommand normalAttackCommand = AzCommand.create(
        CommonStrings.ATTACK_CONTROLLER,
        CommonStrings.NORMAL_ATTACK_ANIMATION_NAME,
        AzPlayBehaviors.PLAY_ONCE
    );

    private final AzCommand rangedAttackCommand = AzCommand.create(
        CommonStrings.ATTACK_CONTROLLER,
        CommonStrings.RANGED_ATTACK_ANIMATION_NAME,
        AzPlayBehaviors.PLAY_ONCE
    );

    private final AzCommand deathCommand = AzCommand.create(
        CommonStrings.BASE_CONTROLLER,
        CommonStrings.DEATH_ANIMATION_NAME,
        AzPlayBehaviors.HOLD_ON_LAST_FRAME
    );

    private final HopperBug hopperBug;

    public HopperBugAnimationDispatcher(HopperBug hopperBug) {
        this.hopperBug = hopperBug;
    }

    public void clientIdle() {
        idleCommand.sendForEntity(hopperBug);
    }

    public void clientWalk() {
        walkCommand.sendForEntity(hopperBug);
    }

    public void clientFly() {
        flyCommand.sendForEntity(hopperBug);
    }

    public void clientRun() {
        runCommand.sendForEntity(hopperBug);
    }

    public void serverNormalAttack() {
        if (hopperBug.isDeadOrDying() || hopperBug.getHealth() <= 0)
            return;
        normalAttackCommand.sendForEntity(hopperBug);
    }

    public void serverRangedAttack() {
        if (hopperBug.isDeadOrDying() || hopperBug.getHealth() <= 0)
            return;
        rangedAttackCommand.sendForEntity(hopperBug);
    }

    public void clientDeath() {
        deathCommand.sendForEntity(hopperBug);
    }
}
