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
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.kinetics.KineticBlock;
import org.alex_melan.spacereloaded.kinetics.KineticBlockEntity;
import org.alex_melan.spacereloaded.kinetics.PressBlockEntity;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/**
 * Вращение механики (005, FR-308, D53): модели-роторы (вспомогательные блоки, построены вдоль Y)
 * поворачиваются к оси узла и крутятся на угол узла — чистую функцию игрового времени от
 * синхронизированных (ω_вид, угол₀, t₀). Шток пресса ходит вниз-вверх за время удара.
 */
public class KineticRenderer<T extends KineticBlockEntity> implements BlockEntityRenderer<T, KineticRenderState> {

    private final BlockModelResolver blocks;

    public KineticRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.blockModelResolver();
    }

    @Override
    public KineticRenderState createRenderState() {
        return new KineticRenderState();
    }

    @Override
    public void extractRenderState(T be, KineticRenderState state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, breakProgress);
        KineticBlock block = be.kineticBlock();
        Direction.Axis axis = block.axis(be.getBlockState());
        state.axis = axis == null ? Direction.Axis.Y : axis;
        double time = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + partialTick;
        state.angleDeg = (float) Math.toDegrees(be.visualAngle(time) % (2 * Math.PI));
        state.hasShaft = false;
        state.hasPart = false;
        state.gearbox = false;
        state.isRam = false;
        switch (block.kind()) {
            case SHAFT -> {
                state.hasShaft = true;
                blocks.update(state.shaft, (block.material() == KineticBlock.Material.WOOD
                        ? ModBlocks.ROTOR_WOODEN_SHAFT : ModBlocks.ROTOR_STEEL_SHAFT).defaultBlockState(),
                        state.context);
            }
            case SMALL_GEAR, LARGE_GEAR -> {
                state.hasShaft = true;
                state.hasPart = true;
                blocks.update(state.shaft, ModBlocks.ROTOR_STEEL_SHAFT.defaultBlockState(), state.context);
                blocks.update(state.part, (block.kind() == KineticBlock.Kind.SMALL_GEAR
                        ? ModBlocks.ROTOR_SMALL_GEAR : ModBlocks.ROTOR_LARGE_GEAR).defaultBlockState(), state.context);
            }
            case FLYWHEEL -> {
                state.hasShaft = true;
                state.hasPart = true;
                blocks.update(state.shaft, ModBlocks.ROTOR_STEEL_SHAFT.defaultBlockState(), state.context);
                blocks.update(state.part, ModBlocks.ROTOR_FLYWHEEL.defaultBlockState(), state.context);
            }
            case PRESS -> {
                if (be instanceof PressBlockEntity press) {
                    state.isRam = true;
                    double since = time - press.strokeStartTick();
                    double stroke = 4.0;
                    double phase = since < 0 || since > stroke * 2 ? 0 : (since < stroke ? since / stroke
                            : 2 - since / stroke);
                    state.ramOffset = (float) (-0.35 * phase);
                    blocks.update(state.part, ModBlocks.ROTOR_PRESS_RAM.defaultBlockState(), state.context);
                }
            }
            case MOTOR, CLUTCH -> {
                state.hasShaft = true;
                blocks.update(state.shaft, ModBlocks.ROTOR_SHAFT_STUB.defaultBlockState(), state.context);
            }
            case GEARBOX -> {
                // Редуктор: концы валов на всех гранях (осевая скорость — «окружная» s узла)
                state.hasShaft = true;
                state.axis = Direction.Axis.Y;
                state.gearbox = true;
                blocks.update(state.shaft, ModBlocks.ROTOR_SHAFT_STUB.defaultBlockState(), state.context);
            }
            case LATHE -> {
                state.hasShaft = true;
                blocks.update(state.shaft, ModBlocks.ROTOR_LATHE_CHUCK.defaultBlockState(), state.context);
            }
            case WIND_HUB -> {
                state.hasShaft = true;
                blocks.update(state.shaft, ModBlocks.ROTOR_WIND_HUB.defaultBlockState(), state.context);
            }
            default -> {
            }
        }
    }

    @Override
    public void submit(KineticRenderState state, PoseStack pose, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (state.isRam) {
            pose.pushPose();
            pose.translate(0, state.ramOffset, 0);
            state.part.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
            return;
        }
        if (state.gearbox) {
            for (Direction.Axis axis : Direction.Axis.values()) {
                rotor(state, state.shaft, axis, pose, collector);
            }
            return;
        }
        if (state.hasShaft) {
            rotor(state, state.shaft, state.axis, pose, collector);
        }
        if (state.hasPart) {
            rotor(state, state.part, state.axis, pose, collector);
        }
    }

    /** Модель-ротор (построена вдоль Y) — повернуть к оси и провернуть на угол узла. */
    private static void rotor(KineticRenderState state, net.minecraft.client.renderer.block.BlockModelRenderState model,
                              Direction.Axis axis, PoseStack pose, SubmitNodeCollector collector) {
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        switch (axis) {
            case X -> pose.mulPose(Axis.ZP.rotationDegrees(-90));
            case Z -> pose.mulPose(Axis.XP.rotationDegrees(90));
            default -> {
            }
        }
        pose.mulPose(Axis.YP.rotationDegrees(state.angleDeg));
        pose.translate(-0.5f, -0.5f, -0.5f);
        model.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
