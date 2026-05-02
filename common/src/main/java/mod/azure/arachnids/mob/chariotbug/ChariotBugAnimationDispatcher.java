package mod.azure.arachnids.mob.chariotbug;

import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;

import mod.azure.arachnids.util.CommonStrings;

public class ChariotBugAnimationDispatcher {

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

    private final AzCommand deathCommand = AzCommand.create(
        CommonStrings.BASE_CONTROLLER,
        CommonStrings.DEATH_ANIMATION_NAME,
        AzPlayBehaviors.HOLD_ON_LAST_FRAME
    );

    private final ChariotBug chariotBug;

    public ChariotBugAnimationDispatcher(ChariotBug chariotBug) {
        this.chariotBug = chariotBug;
    }

    public void clientIdle() {
        idleCommand.sendForEntity(chariotBug);
    }

    public void clientWalk() {
        walkCommand.sendForEntity(chariotBug);
    }

    public void clientDeath() {
        deathCommand.sendForEntity(chariotBug);
    }
}
