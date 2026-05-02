package mod.azure.arachnids.client.animator;

import mod.azure.azurelib.common.animation.AzAnimatorConfig;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import mod.azure.arachnids.CommonMod;
import mod.azure.arachnids.mob.chariotbug.ChariotBug;
import mod.azure.arachnids.util.CommonStrings;

public class ChariotAnimator extends AzEntityAnimator<ChariotBug> {

    private static final ResourceLocation ANIMATIONS = CommonMod.modResource(
        "animations/entity/chariot.animation.json"
    );

    public ChariotAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<ChariotBug> animationControllerContainer) {
        animationControllerContainer.add(
            AzAnimationController.builder(this, CommonStrings.BASE_CONTROLLER).build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ChariotBug chariotBug) {
        return ANIMATIONS;
    }
}
