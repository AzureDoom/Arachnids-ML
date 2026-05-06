package mod.azure.arachnids.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.azure.azurelib.common.render.AzLayerRenderer;
import mod.azure.azurelib.common.render.AzRendererPipeline;
import mod.azure.azurelib.common.render.entity.AzEntityModelRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererPipeline;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.UUID;

import mod.azure.arachnids.ai.util.WallCrawlingMob;

public class ArachnidCrawlingModelRenderer<T extends LivingEntity> extends AzEntityModelRenderer<T> {

    public ArachnidCrawlingModelRenderer(
        AzRendererPipeline<UUID, T> entityRendererPipeline,
        AzLayerRenderer<UUID, T> layerRenderer
    ) {
        super((AzEntityRendererPipeline<T>) entityRendererPipeline, layerRenderer);
    }

    public ArachnidCrawlingModelRenderer(
        AzEntityRendererPipeline<T> entityRendererPipeline,
        AzLayerRenderer<UUID, T> layerRenderer
    ) {
        super(entityRendererPipeline, layerRenderer);
    }

    @Override
    protected void applyRotations(
        T animatable,
        PoseStack poseStack,
        float ageInTicks,
        float rotationYaw,
        float partialTick,
        float nativeScale
    ) {
        if (animatable instanceof WallCrawlingMob wallCrawler) {
            // Apply climbing rotations if actively crawling OR within the grace window.
            // This prevents the model snapping back to default orientation during the
            // brief ticks where isWallCrawling flickers false near the top of a surface.
            // Hold climbing pose for 1 extra tick only (not the full grace window)
            // to avoid the model lerping back to standing orientation visibly.
            var shouldApplyClimb = wallCrawler.arachnids$isWallCrawling()
                || wallCrawler.arachnids$getWallCrawlGraceTicks() > 2;

            if (shouldApplyClimb) {
                applyClimbingRotations(animatable, wallCrawler, poseStack, partialTick);
                return;
            }
        }

        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
    }

    private static void applyClimbingRotations(
        LivingEntity entity,
        WallCrawlingMob wallCrawler,
        PoseStack poseStack,
        float partialTick
    ) {
        var forward = lerpVec(
            wallCrawler.arachnids$getOldCrawlForward(),
            wallCrawler.arachnids$getCrawlForward(),
            partialTick
        ).scale(-1.0D);

        var up = lerpVec(
            wallCrawler.arachnids$getOldCrawlUp(),
            wallCrawler.arachnids$getCrawlUp(),
            partialTick
        );

        if (forward.lengthSqr() < 0.0001D) {
            forward = new Vec3(0.0D, 0.0D, -1.0D);
        }

        if (up.lengthSqr() < 0.0001D) {
            up = new Vec3(0.0D, 1.0D, 0.0D);
        }

        forward = forward.normalize();
        up = up.normalize();

        poseStack.rotateAround(
            quaternionFromDirection(forward, up),
            0.0F,
            entity.getBbHeight() / 2.0F,
            0.0F
        );

        var distToBlock = Mth.lerp(
            partialTick,
            (float) wallCrawler.arachnids$getOldCrawlDistFromBlock(),
            (float) wallCrawler.arachnids$getCrawlDistFromBlock()
        );

        poseStack.translate(0.0D, -distToBlock * 0.25D, 0.0D);
    }

    private static Vec3 lerpVec(Vec3 oldValue, Vec3 newValue, float partialTick) {
        return oldValue.lerp(newValue, partialTick);
    }

    private static Matrix4f rotationMatrixFromDirection(Vec3 forward, Vec3 up) {
        var xAxis = up.cross(forward);

        if (xAxis.lengthSqr() < 0.0001D) {
            return new Matrix4f();
        }

        xAxis = xAxis.normalize();

        var yAxis = forward.cross(xAxis);

        if (yAxis.lengthSqr() < 0.0001D) {
            return new Matrix4f();
        }

        yAxis = yAxis.normalize();

        var zAxis = xAxis.cross(yAxis);

        if (zAxis.lengthSqr() < 0.0001D) {
            return new Matrix4f();
        }

        zAxis = zAxis.normalize();

        return new Matrix4f(
            (float) xAxis.x,
            (float) xAxis.y,
            (float) xAxis.z,
            0.0F,

            (float) yAxis.x,
            (float) yAxis.y,
            (float) yAxis.z,
            0.0F,

            (float) zAxis.x,
            (float) zAxis.y,
            (float) zAxis.z,
            0.0F,

            0.0F,
            0.0F,
            0.0F,
            1.0F
        );
    }

    private static Quaternionf quaternionFromDirection(Vec3 forward, Vec3 up) {
        return rotationMatrixFromDirection(forward, up).getNormalizedRotation(new Quaternionf());
    }
}
