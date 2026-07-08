package org.alex_melan.spacereloaded.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import org.alex_melan.spacereloaded.cannon.KineticProjectileEntity;

/**
 * Вольфрамовый лом в полёте: вытянутый тёмный металлический столбик
 * (незеритовая модель, путь TntRenderer — как {@link RocketRenderer}).
 */
public class KineticProjectileRenderer
        extends EntityRenderer<KineticProjectileEntity, KineticProjectileRenderState> {

    private final BlockModelResolver blockModelResolver;

    public KineticProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
        this.shadowRadius = 0.0f;
    }

    @Override
    public KineticProjectileRenderState createRenderState() {
        return new KineticProjectileRenderState();
    }

    @Override
    public void extractRenderState(KineticProjectileEntity entity,
                                   KineticProjectileRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        blockModelResolver.update(state.model, Blocks.NETHERITE_BLOCK.defaultBlockState(), state.context);
    }

    @Override
    public void submit(KineticProjectileRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(0.3f, 2.6f, 0.3f);
        poseStack.translate(-0.5f, 0.0f, -0.5f);
        state.model.submit(poseStack, collector, state.lightCoords,
                OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
