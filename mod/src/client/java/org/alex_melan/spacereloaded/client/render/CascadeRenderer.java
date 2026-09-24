package org.alex_melan.spacereloaded.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.client.render.anim.SmoothDrive;
import org.alex_melan.spacereloaded.multiblock.ControllerBlock;
import org.alex_melan.spacereloaded.multiblock.MultiblockTemplates;
import org.alex_melan.spacereloaded.nuclear.CascadeBlockEntity;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/**
 * Каскад (008, US3): роторы из углепластика вращаются в кожухах центрифуг — плавный разгон и
 * выбег (реальные роторы крутятся ~1500 об/с, на экране — стробоскопически медленнее, светлые метки
 * показывают вращение).
 */
public class CascadeRenderer implements BlockEntityRenderer<CascadeBlockEntity, CascadeRenderState> {

    private final BlockModelResolver blocks;

    public CascadeRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.blockModelResolver();
    }

    @Override
    public CascadeRenderState createRenderState() {
        return new CascadeRenderState();
    }

    @Override
    public void extractRenderState(CascadeBlockEntity be, CascadeRenderState state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, breakProgress);
        state.formed = be.formed();
        // свет — в воздушной клетке перед лицом контроллера: внутри непрозрачного блока он нулевой
        if (be.getLevel() != null) {
            state.lightCoords = net.minecraft.util.LightCoordsUtil.getLightCoords(be.getLevel(),
                    be.getBlockPos().relative(be.getBlockState().getValue(ControllerBlock.FACING)));
        }

        if (!(be.clientAnim instanceof SmoothDrive.Spinner)) {
            be.clientAnim = new SmoothDrive.Spinner();
        }
        state.angle = (float) ((SmoothDrive.Spinner) be.clientAnim).update(be.running() ? 9 : 0, 3.0);
        var facing = be.getBlockState().getValue(ControllerBlock.FACING);
        state.count = Math.min(40, be.centrifuges());
        for (int k = 0; k < state.count; k++) {
            BlockPos p = MultiblockTemplates.worldPos(BlockPos.ZERO, facing, 0, 0, k + 1);
            state.dx[k] = p.getX();
            state.dz[k] = p.getZ();
        }
        blocks.update(state.rotor, ModBlocks.CENTRIFUGE_ROTOR.defaultBlockState(), state.context);
    }

    @Override
    public void submit(CascadeRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        for (int k = 0; k < state.count; k++) {
            pose.pushPose();
            pose.translate(state.dx[k] + 0.5f, 0, state.dz[k] + 0.5f);
            pose.mulPose(Axis.YP.rotation(state.angle + k * 0.7f));
            pose.translate(-0.5f, 0, -0.5f);
            state.rotor.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
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
