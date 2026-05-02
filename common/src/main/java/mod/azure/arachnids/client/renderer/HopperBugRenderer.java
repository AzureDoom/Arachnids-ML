package mod.azure.arachnids.client.renderer;

import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import mod.azure.arachnids.CommonMod;
import mod.azure.arachnids.client.animator.HopperAnimator;
import mod.azure.arachnids.mob.hopperbug.HopperBug;

public class HopperBugRenderer extends AzEntityRenderer<HopperBug> {

    private static final ResourceLocation MODEL = CommonMod.modResource("geo/entity/hopper.geo.json");

    private static final ResourceLocation TEXTURE = CommonMod.modResource("textures/entity/hopper_1.png");

    public HopperBugRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.<HopperBug>builder(MODEL, TEXTURE)
                .setRenderEntry(contextPipeline -> {
                    contextPipeline.animatable().updateAnimations();

                    return contextPipeline;
                })
                .setAnimatorProvider(HopperAnimator::new)
                .setDeathMaxRotation(0F)
                .setShadowRadius(1.5F)
                .setScale(1.25F)
                .setRenderType(RenderType.entityTranslucent(TEXTURE))
                .build(),
            context
        );
    }
}
