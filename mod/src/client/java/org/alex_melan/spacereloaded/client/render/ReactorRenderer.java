package org.alex_melan.spacereloaded.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.client.render.anim.SmoothDrive;
import org.alex_melan.spacereloaded.core.thermal.Blackbody;
import org.alex_melan.spacereloaded.multiblock.ControllerBlock;
import org.alex_melan.spacereloaded.multiblock.MultiblockTemplates;
import org.alex_melan.spacereloaded.nuclear.ReactorBlockEntity;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/**
 * Реактор Kilopower (008, US2) в работе: стержень B₄C плавно ходит в каркасе привода со скоростью
 * привода (выведен — опущен из зоны в каркас; SCRAM — быстрое падение под пружиной вверх, в зону);
 * горячая тепловая труба над зоной светится цветом чёрного тела по температуре зоны (видно выше
 * ~800 K); поршни Стирлингов качаются с частотой, пропорциональной выработке. Радиатор при ~400 K
 * глазом не светится — и не рисуется светящимся.
 */
public class ReactorRenderer implements BlockEntityRenderer<ReactorBlockEntity, ReactorRenderState> {

    private final BlockModelResolver blocks;

    public ReactorRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.blockModelResolver();
    }

    private static final class Anim {
        final SmoothDrive rod = new SmoothDrive();
        final SmoothDrive temperature = new SmoothDrive();
        final SmoothDrive.Spinner pistons = new SmoothDrive.Spinner();
    }

    @Override
    public ReactorRenderState createRenderState() {
        return new ReactorRenderState();
    }

    @Override
    public void extractRenderState(ReactorBlockEntity be, ReactorRenderState state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, breakProgress);
        state.facing = be.getBlockState().getValue(ControllerBlock.FACING);
        // свет — в воздушной клетке перед лицом контроллера: внутри непрозрачного блока он нулевой
        if (be.getLevel() != null) {
            state.lightCoords = net.minecraft.util.LightCoordsUtil.getLightCoords(be.getLevel(),
                    be.getBlockPos().relative(be.getBlockState().getValue(ControllerBlock.FACING)));
        }

        state.formed = be.formed();
        if (!(be.clientAnim instanceof Anim)) {
            be.clientAnim = new Anim();
        }
        Anim anim = (Anim) be.clientAnim;
        // сервер ведёт стержень со скоростью привода; клиент сглаживает шаги синхронизации
        double speed = be.scrammed() ? ReactorBlockEntity.SCRAM_SPEED * ReactorBlockEntity.TIME_SCALE * 1.2
                : ReactorBlockEntity.ROD_SPEED * ReactorBlockEntity.TIME_SCALE * 1.5;
        state.rod = (float) anim.rod.update(be.rodPosition(), speed, 8);
        state.temperature = (float) anim.temperature.update(be.temperature(), 400, 2);
        double nominal = Math.max(1, be.stirlings() * 250.0);
        double load = Math.min(1.2, be.electricW() / nominal);
        state.pistonPhase = (float) anim.pistons.update(2 * Math.PI * 3 * load, 0.8);
        state.pistonAmp = (float) Math.min(1, anim.pistons.omega() / (2 * Math.PI * 3));
        state.stirlingMask = be.stirlingMask();
        for (int i = 0; i < 4; i++) {
            int[] o = ReactorBlockEntity.POWER_SLOTS[i];
            BlockPos p = MultiblockTemplates.worldPos(BlockPos.ZERO, state.facing, o[0], o[1], o[2]);
            state.slot[i][0] = p.getX();
            state.slot[i][1] = p.getY();
            state.slot[i][2] = p.getZ();
        }
        BlockPos hot = MultiblockTemplates.worldPos(BlockPos.ZERO, state.facing, 0, 2, 0);
        state.hotPipe[0] = hot.getX();
        state.hotPipe[1] = hot.getY();
        state.hotPipe[2] = hot.getZ();
        blocks.update(state.rodModel, ModBlocks.CONTROL_ROD.defaultBlockState(), state.context);
        blocks.update(state.piston, ModBlocks.STIRLING_PISTON.defaultBlockState(), state.context);
    }

    @Override
    public void submit(ReactorRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        // стержень: вставлен (0) — поднят на 12 px в зону, выведен (1) — весь в каркасе привода
        pose.pushPose();
        pose.translate(0, (1 - state.rod) * 12 / 16f, 0);
        state.rodModel.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        // поршни Стирлингов
        for (int i = 0; i < 4; i++) {
            if ((state.stirlingMask & (1 << i)) == 0) {
                continue;
            }
            pose.pushPose();
            float stroke = (float) Math.sin(state.pistonPhase + i * Math.PI / 2) * 1.5f / 16 * state.pistonAmp;
            pose.translate(state.slot[i][0], state.slot[i][1] + stroke, state.slot[i][2]);
            state.piston.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        // свечение горячей тепловой трубы (над зоной, T ≈ T зоны): цвет и яркость чёрного тела
        double glow = Blackbody.glow(state.temperature);
        if (glow > 0.01) {
            int rgb = Blackbody.rgb(state.temperature);
            float a = (float) (0.15 + 0.6 * glow);
            pose.pushPose();
            pose.translate(state.hotPipe[0], state.hotPipe[1], state.hotPipe[2]);
            collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) ->
                    GlowGeometry.cube(p, vc, 5.8f / 16, 0, 5.8f / 16, 10.2f / 16, 1, 10.2f / 16, GlowGeometry.argb(a, rgb)));
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
