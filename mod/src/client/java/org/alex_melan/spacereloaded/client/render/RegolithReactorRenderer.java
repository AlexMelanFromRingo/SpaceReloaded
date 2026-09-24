package org.alex_melan.spacereloaded.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.industry.RegolithReactorBlock;
import org.alex_melan.spacereloaded.industry.RegolithReactorBlockEntity;

/**
 * Смотровое окно реголитового реактора (004, FR-234): пока идёт электролиз (LIT), в окне
 * фасада светится расплав ~1600 °C — оранжево-жёлтый квад с медленной пульсацией.
 */
public class RegolithReactorRenderer
        implements BlockEntityRenderer<RegolithReactorBlockEntity, RegolithReactorRenderState> {

    private static final int MELT_RGB = 0xFF9A2E;
    private static final int CORE_RGB = 0xFFE27A;

    public RegolithReactorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public RegolithReactorRenderState createRenderState() {
        return new RegolithReactorRenderState();
    }

    @Override
    public void extractRenderState(RegolithReactorBlockEntity be, RegolithReactorRenderState state,
                                   float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, breakProgress);
        state.facing = be.getBlockState().getValue(RegolithReactorBlock.FACING);
        state.lit = be.getBlockState().getValue(RegolithReactorBlock.LIT);
        state.time = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + partialTick;
    }

    @Override
    public void submit(RegolithReactorRenderState state, PoseStack pose, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (!state.lit) {
            return;
        }
        float pulse = 0.55f + 0.2f * (float) Math.sin(state.time * 0.12f);
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-state.facing.toYRot()));
        pose.translate(-0.5f, -0.5f, -0.5f);
        // Фасад после поворота — грань z = 1 (юг); окно 0.25..0.75, чуть снаружи
        collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> {
            float z = 1.002f;
            GlowGeometry.quad(p, vc, 0.25f, 0.3f, z, 0.75f, 0.3f, z, 0.75f, 0.7f, z, 0.25f, 0.7f, z,
                    GlowGeometry.argb(pulse, MELT_RGB));
            GlowGeometry.quad(p, vc, 0.35f, 0.38f, z + 0.001f, 0.65f, 0.38f, z + 0.001f, 0.65f, 0.55f, z + 0.001f,
                    0.35f, 0.55f, z + 0.001f, GlowGeometry.argb(pulse * 0.8f, CORE_RGB));
        });
        pose.popPose();
    }
}
