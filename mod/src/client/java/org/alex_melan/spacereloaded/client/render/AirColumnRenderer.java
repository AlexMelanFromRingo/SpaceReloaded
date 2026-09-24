package org.alex_melan.spacereloaded.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.client.render.anim.SmoothDrive;
import org.alex_melan.spacereloaded.cryo.AirColumnBlockEntity;
import org.alex_melan.spacereloaded.multiblock.ControllerBlock;
import org.alex_melan.spacereloaded.multiblock.MultiblockTemplates;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/**
 * Колонна Линде (008, US4) в работе: иней нарастает на холодильном кожухе за время работы и тает
 * после остановки (минуты), в стекле испарителя поднимается жидкий кислород, крыльчатка
 * турбодетандера разгоняется с потоком, из сбросного клапана шапки идёт пар.
 */
public class AirColumnRenderer implements BlockEntityRenderer<AirColumnBlockEntity, AirColumnRenderState> {

    private final BlockModelResolver blocks;

    public AirColumnRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.blockModelResolver();
    }

    private static final class Anim {
        final SmoothDrive frost = new SmoothDrive();
        final SmoothDrive lox = new SmoothDrive();
        final SmoothDrive.Spinner wheel = new SmoothDrive.Spinner();
    }

    @Override
    public AirColumnRenderState createRenderState() {
        return new AirColumnRenderState();
    }

    @Override
    public void extractRenderState(AirColumnBlockEntity be, AirColumnRenderState state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, breakProgress);
        state.facing = be.getBlockState().getValue(ControllerBlock.FACING);
        state.formed = be.formed();
        state.trays = be.trays();
        if (!(be.clientAnim instanceof Anim)) {
            be.clientAnim = new Anim();
        }
        Anim anim = (Anim) be.clientAnim;
        boolean on = be.running() && state.formed;
        // иней: медленный рост (десятки секунд) и таяние; уровень LOX — за ним
        state.frost = (float) anim.frost.update(on ? 1 : 0, 0.03, 0.15);
        state.lox = (float) anim.lox.update(on ? 0.35 + 0.5 * Math.min(1, be.airKgS() / 0.7) : 0, 0.05, 0.3);
        state.wheel = (float) anim.wheel.update(on ? 18 * Math.min(1.3, be.airKgS() / 0.68) : 0, 2.5);
        BlockPos ex = MultiblockTemplates.worldPos(BlockPos.ZERO, state.facing, 1, 0, 0);
        state.exchanger[0] = ex.getX();
        state.exchanger[1] = ex.getY();
        state.exchanger[2] = ex.getZ();
        blocks.update(state.wheelModel, ModBlocks.ASU_EXPANDER_WHEEL.defaultBlockState(), state.context);
        blocks.update(state.cap, ModBlocks.ASU_COLUMN_CAP.defaultBlockState(), state.context);
        var level = be.getLevel();
        if (on && level != null && level.getRandom().nextFloat() < 0.25f) {
            BlockPos p = be.getBlockPos();
            level.addParticle(ParticleTypes.CLOUD, p.getX() + 0.5 + 0.2, p.getY() + state.trays + 1 + 0.4, p.getZ() + 0.5,
                    0.03, 0.02, 0);
        }
    }

    @Override
    public void submit(AirColumnRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        // шапка колонны со сбросным клапаном над верхней тарелкой
        pose.pushPose();
        pose.translate(0, state.trays + 1, 0);
        state.cap.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        // крыльчатка детандера
        pose.pushPose();
        pose.translate(state.exchanger[0] + 0.5f, state.exchanger[1], state.exchanger[2] + 0.5f);
        pose.mulPose(Axis.YP.rotation(state.wheel));
        pose.translate(-0.5f, 0, -0.5f);
        state.wheelModel.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        // жидкий кислород в стекле испарителя (ниша 2 px на лицевой стороне ключа)
        if (state.lox > 0.01f) {
            float yRot = -(state.facing.toYRot() + 180);
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.mulPose(Axis.YP.rotationDegrees(yRot));
            pose.translate(-0.5f, -0.5f, -0.5f);
            float top = (3 + 9 * state.lox) / 16f;
            collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> {
                float z = 1.9f / 16;
                GlowGeometry.quad(p, vc, 3 / 16f, 3 / 16f, z, 3 / 16f, top, z, 13 / 16f, top, z, 13 / 16f, 3 / 16f, z,
                        GlowGeometry.argb(0.45f, 0x9FD6FF));
            });
            pose.popPose();
        }
        // иней на кожухе испарителя и тарелок
        if (state.frost > 0.02f) {
            int argb = GlowGeometry.argb(0.22f * state.frost, 0xE8F4FF);
            collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> {
                for (int k = 1; k <= state.trays; k++) {
                    GlowGeometry.cube(p, vc, 1.9f / 16, k, 1.9f / 16, 14.1f / 16, k + 1, 14.1f / 16, argb);
                }
            });
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
