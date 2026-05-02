package mod.azure.arachnids.mob.warriorbug;

import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;

import mod.azure.arachnids.util.CommonStrings;

public class WarriorBugAnimationDispatcher {

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

    private final AzCommand normalAttackCommand = AzCommand.create(
        CommonStrings.ATTACK_CONTROLLER,
        CommonStrings.NORMAL_ATTACK_ANIMATION_NAME,
        AzPlayBehaviors.PLAY_ONCE
    );

    private final AzCommand heavyAttackCommand = AzCommand.create(
        CommonStrings.ATTACK_CONTROLLER,
        CommonStrings.HEAVY_ATTACK_ANIMATION_NAME,
        AzPlayBehaviors.PLAY_ONCE
    );

    private final AzCommand deathCommand = AzCommand.create(
        CommonStrings.BASE_CONTROLLER,
        CommonStrings.DEATH_ANIMATION_NAME,
        AzPlayBehaviors.HOLD_ON_LAST_FRAME
    );

    private final WarriorBug warriorBug;

    public WarriorBugAnimationDispatcher(WarriorBug warriorBug) {
        this.warriorBug = warriorBug;
    }

    public void clientIdle() {
        idleCommand.sendForEntity(warriorBug);
    }

    public void clientWalk() {
        walkCommand.sendForEntity(warriorBug);
    }

    public void clientRun() {
        runCommand.sendForEntity(warriorBug);
    }

    // First attack animation
    public void serverLightAttack() {
        if (warriorBug.isDeadOrDying() || warriorBug.getHealth() <= 0)
            return;
        lightAttackCommand.sendForEntity(warriorBug);
    }

    // Normal attacking animation
    public void serverNormalAttack() {
        if (warriorBug.isDeadOrDying() || warriorBug.getHealth() <= 0)
            return;
        normalAttackCommand.sendForEntity(warriorBug);
    }

    // When finishing a mob
    public void serverHeavyAttack() {
        if (warriorBug.isDeadOrDying() || warriorBug.getHealth() <= 0)
            return;
        heavyAttackCommand.sendForEntity(warriorBug);
    }

    public void clientDeath() {
        deathCommand.sendForEntity(warriorBug);
    }
}
