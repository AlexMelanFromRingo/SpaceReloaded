package org.alex_melan.spacereloaded.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.alex_melan.spacereloaded.industry.CargoPodEntity;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/** Визуальная грузовая капсула: модель возвратной капсулы в масштабе 0.7, полная яркость (раскалена). */
public class CargoPodRenderer extends EntityRenderer<CargoPodEntity, CargoPodRenderState> {

    private final BlockModelResolver blockModelResolver;

    public CargoPodRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
        this.shadowRadius = 0.0f;
    }

    @Override
    public CargoPodRenderState createRenderState() {
        return new CargoPodRenderState();
    }

    @Override
    public void extractRenderState(CargoPodEntity entity, CargoPodRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        blockModelResolver.update(state.model, ModBlocks.RETURN_CAPSULE.defaultBlockState(), state.context);
    }

    @Override
    public void submit(CargoPodRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(0.7f, 0.7f, 0.7f);
        poseStack.translate(-0.5f, 0.0f, -0.5f);
        state.model.submit(poseStack, collector, net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
