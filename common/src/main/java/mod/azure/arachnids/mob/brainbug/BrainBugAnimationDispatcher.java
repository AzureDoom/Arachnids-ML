package mod.azure.arachnids.mob.brainbug;

import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;

import mod.azure.arachnids.util.CommonStrings;

public class BrainBugAnimationDispatcher {

    private final AzCommand idleCommand = AzCommand.create(
        CommonStrings.BASE_CONTROLLER,
        CommonStrings.IDLE_ANIMATION_NAME,
        AzPlayBehaviors.LOOP
    );

    private final AzCommand screamAttackCommand = AzCommand.create(
        CommonStrings.BASE_CONTROLLER,
        "scream",
        AzPlayBehaviors.PLAY_ONCE
    );

    private final AzCommand normalAttackCommand = AzCommand.create(
        CommonStrings.BASE_CONTROLLER,
        CommonStrings.NORMAL_ATTACK_ANIMATION_NAME,
        AzPlayBehaviors.PLAY_ONCE
    );

    private final AzCommand deathCommand = AzCommand.create(
        CommonStrings.BASE_CONTROLLER,
        CommonStrings.DEATH_ANIMATION_NAME,
        AzPlayBehaviors.HOLD_ON_LAST_FRAME
    );

    private final BrainBug brainBug;

    public BrainBugAnimationDispatcher(BrainBug brainBug) {
        this.brainBug = brainBug;
    }

    public void clientIdle() {
        idleCommand.sendForEntity(brainBug);
    }

    public void serverScreamAttack() {
        if (brainBug.isDeadOrDying() || brainBug.getHealth() <= 0)
            return;
        screamAttackCommand.sendForEntity(brainBug);
    }

    public void serverNormalAttack() {
        if (brainBug.isDeadOrDying() || brainBug.getHealth() <= 0)
            return;
        normalAttackCommand.sendForEntity(brainBug);
    }

    public void clientDeath() {
        deathCommand.sendForEntity(brainBug);
    }
}
