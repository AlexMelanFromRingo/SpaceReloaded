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
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.client.render.anim.SmoothDrive;
import org.alex_melan.spacereloaded.metallurgy.ArcFurnaceBlockEntity;
import org.alex_melan.spacereloaded.metallurgy.ArcFurnaceBlockEntity.Phase;
import org.alex_melan.spacereloaded.multiblock.ControllerBlock;
import org.alex_melan.spacereloaded.multiblock.MultiblockTemplates;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/**
 * Дуговая печь (008, US5) целиком рисуется здесь (блоки корпуса в собранном виде невидимы): свод
 * приподнимается и отворачивается вокруг мачты на загрузку, три электрода плавно опускаются на
 * плавку и поднимаются, под ними мерцает дуга с искрами, ванна светится расплавом, при сливе печь
 * наклоняется вокруг ребра носка и из носка течёт струя металла.
 */
public class ArcFurnaceRenderer implements BlockEntityRenderer<ArcFurnaceBlockEntity, ArcFurnaceRenderState> {

    private static final float PIVOT_X = 30 / 16f;
    private final BlockModelResolver blocks;

    public ArcFurnaceRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.blockModelResolver();
    }

    private static final class Anim {
        final SmoothDrive roof = new SmoothDrive();
        final SmoothDrive electrodes = new SmoothDrive();
        final SmoothDrive tilt = new SmoothDrive();
        final SmoothDrive melt = new SmoothDrive();
    }

    @Override
    public ArcFurnaceRenderState createRenderState() {
        return new ArcFurnaceRenderState();
    }

    @Override
    public void extractRenderState(ArcFurnaceBlockEntity be, ArcFurnaceRenderState state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, breakProgress);
        state.formed = be.formed();
        Direction face = be.getBlockState().getValue(ControllerBlock.FACING);
        BlockPos c = MultiblockTemplates.worldPos(BlockPos.ZERO, face, 0, 0, 2);
        // свет — над сводом (внутри непрозрачных блоков корпуса он нулевой)
        if (be.getLevel() != null) {
            state.lightCoords = net.minecraft.util.LightCoordsUtil.getLightCoords(be.getLevel(), be.getBlockPos().offset(c).above(3));
        }
        state.cx = c.getX();
        state.cz = c.getZ();
        // носок — вправо от лица трансформатора (локальная +x шаблона): модель повёрнута так, что её +x туда
        int inX = -face.getStepX(), inZ = -face.getStepZ();
        int rightX = -inZ, rightZ = inX;
        state.yaw = (float) Math.atan2(-rightZ, rightX);
        if (!(be.clientAnim instanceof Anim)) {
            be.clientAnim = new Anim();
        }
        Anim anim = (Anim) be.clientAnim;
        Phase phase = be.phase();
        boolean open = phase == Phase.OPEN || phase == Phase.OPENING;
        boolean down = phase == Phase.MELT || phase == Phase.REFINE;
        state.roofOpen = (float) anim.roof.update(open ? 1 : 0, 0.6, 4);
        state.electrodes = (float) anim.electrodes.update(down && state.roofOpen < 0.05f ? 1 : 0, 0.5, 5);
        state.tilt = (float) anim.tilt.update(phase == Phase.TAP ? 22 : 0, 10, 3);
        state.melt = (float) anim.melt.update(phase == Phase.OPEN ? 0 : be.meltFraction(), 0.3, 2);
        state.arc = phase == Phase.MELT && be.powerW() > 0 && state.electrodes > 0.9f;
        state.time = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + partialTick;
        blocks.update(state.vessel, ModBlocks.EAF_VESSEL.defaultBlockState(), state.context);
        blocks.update(state.roof, ModBlocks.EAF_ROOF_PART.defaultBlockState(), state.context);
        blocks.update(state.electrode, ModBlocks.EAF_ELECTRODE.defaultBlockState(), state.context);
        var level = be.getLevel();
        if (level != null && state.formed) {
            BlockPos base = be.getBlockPos().offset(c);
            var rnd = level.getRandom();
            if (state.arc && rnd.nextFloat() < 0.6f) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, base.getX() + 0.5 + (rnd.nextFloat() - 0.5),
                        base.getY() + 2.2, base.getZ() + 0.5 + (rnd.nextFloat() - 0.5), 0, 0.1, 0);
            }
            if (state.tilt > 12 && rnd.nextFloat() < 0.5f) {
                double lx = base.getX() + 0.5 + rightX * 2.1, lz = base.getZ() + 0.5 + rightZ * 2.1;
                level.addParticle(ParticleTypes.LAVA, lx, base.getY() + 0.2, lz, 0, 0, 0);
            }
        }
    }

    @Override
    public void submit(ArcFurnaceRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        pose.pushPose();
        pose.translate(state.cx + 0.5f, 0, state.cz + 0.5f);
        pose.mulPose(Axis.YP.rotation(state.yaw));
        pose.translate(-0.5f, 0, -0.5f);
        // наклон вокруг ребра носка
        pose.translate(PIVOT_X, 0, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-state.tilt));
        pose.translate(-PIVOT_X, 0, 0);
        state.vessel.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        // расплав в ванне
        if (state.melt > 0.02f) {
            float a = 0.25f + 0.55f * state.melt;
            float y = (4 + 10 * state.melt) / 16f;
            collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> GlowGeometry.quad(p, vc,
                    -8 / 16f, y, -8 / 16f, -8 / 16f, y, 24 / 16f, 24 / 16f, y, 24 / 16f, 24 / 16f, y, -8 / 16f,
                    GlowGeometry.argb(a, 0xFF7A20)));
        }
        // свод: подъём и поворот вокруг мачты у заднего края (−x, противоположно носку)
        float lift = Math.min(1, state.roofOpen * 3) * 0.3f;
        pose.pushPose();
        pose.translate(0, 30 / 16f + lift, 0);
        pose.translate(-14 / 16f, 0, 22 / 16f);
        pose.mulPose(Axis.YP.rotationDegrees(-100 * state.roofOpen));
        pose.translate(14 / 16f, 0, -22 / 16f);
        state.roof.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        // электроды в держателях свода: подняты над сводом или опущены к ванне
        float bottom = (34 - 24 * state.electrodes) / 16f - 30 / 16f;
        for (int i = 0; i < 3; i++) {
            double a = Math.PI / 2 + i * 2 * Math.PI / 3;
            float ex = (float) (7 * Math.cos(a)) / 16f;
            float ez = (float) (7 * Math.sin(a)) / 16f;
            pose.pushPose();
            pose.translate(ex, bottom, ez);
            state.electrode.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            if (state.arc) {
                float flick = 0.5f + 0.5f * (float) Math.abs(Math.sin(state.time * 3.7f + i * 1.9f) * Math.sin(state.time * 1.3f + i));
                collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> GlowGeometry.cube(p, vc,
                        6 / 16f, -2 / 16f, 6 / 16f, 10 / 16f, 1 / 16f, 10 / 16f, GlowGeometry.argb(0.9f * flick, 0xDDE8FF)));
            }
            pose.popPose();
        }
        pose.popPose();
        pose.popPose();
        // струя металла — после корпуса, в раме без наклона (падает вертикально)
        pose.pushPose();
        pose.translate(state.cx + 0.5f, 0, state.cz + 0.5f);
        pose.mulPose(Axis.YP.rotation(state.yaw));
        pose.translate(-0.5f, 0, -0.5f);
        if (state.tilt > 12) {
            double t = Math.toRadians(state.tilt);
            float lipX = (float) (PIVOT_X + 2 / 16.0 * Math.cos(t) + 23 / 16.0 * Math.sin(t));
            float lipY = (float) (-2 / 16.0 * Math.sin(t) + 23 / 16.0 * Math.cos(t));
            float w = 0.07f + 0.02f * (float) Math.sin(state.time * 0.9f);
            collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) ->
                    GlowGeometry.cube(p, vc, lipX - w + 0.1f, -1f, 0.5f - w, lipX + w + 0.1f, lipY, 0.5f + w,
                            GlowGeometry.argb(0.9f, 0xFFB040)));
        }
        pose.popPose();
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
