package org.alex_melan.spacereloaded.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.energy.SolarPanelBlockEntity;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/**
 * Одноосный трекер солнечной панели (визуальный проход 005): полотно поворачивается вокруг оси
 * север–юг вслед за солнцем (восход на востоке, закат на западе), ограничение ±70°; ночью —
 * горизонтальная «стоянка». Поток на панель ∝ cos θ между нормалью и солнцем — трекер держит θ ≈ 0,
 * поэтому выработка мода днём постоянна; анимация показывает, почему.
 */
public class SolarPanelRenderer implements BlockEntityRenderer<SolarPanelBlockEntity, SolarPanelRenderer.State> {

    private static final float MAX_TILT = 70f;

    /** Кадр: угол наклона полотна. */
    public static class State extends BlockEntityRenderState {
        public float tilt;
        public final BlockModelRenderState array = new BlockModelRenderState();
        public final BlockDisplayContext context = BlockDisplayContext.create();
    }

    private final BlockModelResolver blocks;

    public SolarPanelRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.blockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SolarPanelBlockEntity be, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, breakProgress);
        double day = be.getLevel() == null ? 6000 : (be.getLevel().getDefaultClockTime() % 24000L) + partialTick;
        // 0 — восход (солнце на востоке, +X), 6000 — зенит, 12000 — закат (запад)
        float sun = (float) ((day - 6000.0) / 6000.0 * 90.0);
        state.tilt = day < 12500 ? -Math.max(-MAX_TILT, Math.min(MAX_TILT, sun)) : 0f;
        // 006: монокристаллические элементы — чёрные псевдоквадраты вместо синих мультикремниевых
        var array = be.getBlockState().is(ModBlocks.MONO_SOLAR_PANEL) ? ModBlocks.ROTOR_MONO_SOLAR_ARRAY
                : ModBlocks.ROTOR_SOLAR_ARRAY;
        blocks.update(state.array, array.defaultBlockState(), state.context);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5f, 0.72f, 0.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(state.tilt));
        pose.translate(-0.5f, -0.72f, -0.5f);
        state.array.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
