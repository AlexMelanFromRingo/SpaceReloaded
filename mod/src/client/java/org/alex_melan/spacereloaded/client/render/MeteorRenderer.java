package org.alex_melan.spacereloaded.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import org.alex_melan.spacereloaded.impact.MeteorEntity;

/** Метеорит: раскалённый магма-блок ~1.4 куба (путь TntRenderer, как ракета). */
public class MeteorRenderer extends EntityRenderer<MeteorEntity, MeteorRenderState> {

    private final BlockModelResolver blockModelResolver;

    public MeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
        this.shadowRadius = 0.0f;
    }

    @Override
    public MeteorRenderState createRenderState() {
        return new MeteorRenderState();
    }

    @Override
    public void extractRenderState(MeteorEntity entity, MeteorRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        blockModelResolver.update(state.model, Blocks.MAGMA_BLOCK.defaultBlockState(), state.context);
    }

    @Override
    public void submit(MeteorRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(1.4f, 1.4f, 1.4f);
        poseStack.translate(-0.5f, 0.0f, -0.5f);
        state.model.submit(poseStack, collector, 0x00F000F0, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
