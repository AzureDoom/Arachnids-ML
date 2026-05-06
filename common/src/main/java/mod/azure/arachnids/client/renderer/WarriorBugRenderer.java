package mod.azure.arachnids.client.renderer;

import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import mod.azure.arachnids.CommonMod;
import mod.azure.arachnids.client.animator.WarriorAnimator;
import mod.azure.arachnids.client.model.ArachnidCrawlingModelRenderer;
import mod.azure.arachnids.mob.warriorbug.WarriorBug;

public class WarriorBugRenderer extends AzEntityRenderer<WarriorBug> {

    private static final ResourceLocation MODEL = CommonMod.modResource("geo/entity/warriorworker.geo.json");

    private static final ResourceLocation TEXTURE = CommonMod.modResource("textures/entity/soldier_3.png");

    public WarriorBugRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.<WarriorBug>builder(MODEL, TEXTURE)
                .setRenderEntry(contextPipeline -> {
                    contextPipeline.animatable().updateAnimations();

                    return contextPipeline;
                })
                .setModelRenderer(ArachnidCrawlingModelRenderer::new)
                .setAnimatorProvider(WarriorAnimator::new)
                .setDeathMaxRotation(0F)
                .setShadowRadius(1.5F)
                .setScale(1.25F)
                .setRenderType(RenderType.entityTranslucent(TEXTURE))
                .build(),
            context
        );
    }
}
