package mod.azure.arachnids.client.animator;

import mod.azure.azurelib.common.animation.AzAnimatorConfig;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import mod.azure.arachnids.CommonMod;
import mod.azure.arachnids.mob.hopperbug.HopperBug;
import mod.azure.arachnids.util.CommonStrings;

public class HopperAnimator extends AzEntityAnimator<HopperBug> {

    private static final ResourceLocation ANIMATIONS = CommonMod.modResource(
        "animations/entity/hopper.animation.json"
    );

    public HopperAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<HopperBug> animationControllerContainer) {
        animationControllerContainer.add(
            AzAnimationController.builder(this, CommonStrings.BASE_CONTROLLER).build()
        );
        animationControllerContainer.add(
            AzAnimationController.builder(this, CommonStrings.ATTACK_CONTROLLER).build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(HopperBug workerBug) {
        return ANIMATIONS;
    }
}
