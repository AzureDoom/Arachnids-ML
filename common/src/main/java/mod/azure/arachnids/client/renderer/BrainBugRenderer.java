package mod.azure.arachnids.client.renderer;

import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import mod.azure.arachnids.CommonMod;
import mod.azure.arachnids.client.animator.BrainAnimator;
import mod.azure.arachnids.mob.brainbug.BrainBug;

public class BrainBugRenderer extends AzEntityRenderer<BrainBug> {

    private static final ResourceLocation MODEL = CommonMod.modResource("geo/entity/brainbug.geo.json");

    private static final ResourceLocation TEXTURE = CommonMod.modResource("textures/entity/brainbug.png");

    public BrainBugRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.<BrainBug>builder(MODEL, TEXTURE)
                .setRenderEntry(contextPipeline -> {
                    contextPipeline.animatable().updateAnimations();

                    return contextPipeline;
                })
                .setAnimatorProvider(BrainAnimator::new)
                .setDeathMaxRotation(0F)
                .setShadowRadius(2.7F)
                .setRenderType(RenderType.entityTranslucent(TEXTURE))
                .build(),
            context
        );
    }
}
