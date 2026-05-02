package mod.azure.arachnids.mob.worker;

import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;

import mod.azure.arachnids.util.CommonStrings;

public class WorkerBugAnimationDispatcher {

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

    private final AzCommand lightAttackCommand = AzCommand.create(
        CommonStrings.ATTACK_CONTROLLER,
        CommonStrings.LIGHT_ATTACK_ANIMATION_NAME,
        AzPlayBehaviors.PLAY_ONCE
    );

    private final AzCommand deathCommand = AzCommand.create(
        CommonStrings.BASE_CONTROLLER,
        CommonStrings.DEATH_ANIMATION_NAME,
        AzPlayBehaviors.HOLD_ON_LAST_FRAME
    );

    private final WorkerBug workerBug;

    public WorkerBugAnimationDispatcher(WorkerBug workerBug) {
        this.workerBug = workerBug;
    }

    public void clientIdle() {
        idleCommand.sendForEntity(workerBug);
    }

    public void clientWalk() {
        walkCommand.sendForEntity(workerBug);
    }

    public void clientRun() {
        runCommand.sendForEntity(workerBug);
    }

    public void serverLightAttack() {
        if (workerBug.isDeadOrDying() || workerBug.getHealth() <= 0)
            return;
        lightAttackCommand.sendForEntity(workerBug);
    }

    public void clientDeath() {
        deathCommand.sendForEntity(workerBug);
    }
}
