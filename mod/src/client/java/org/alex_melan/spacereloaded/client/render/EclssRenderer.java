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
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.client.render.anim.SmoothDrive;
import org.alex_melan.spacereloaded.eclss.EclssControllerBlockEntity;
import org.alex_melan.spacereloaded.multiblock.ControllerBlock;
import org.alex_melan.spacereloaded.multiblock.MultiblockTemplates;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/**
 * Работа стойки жизнеобеспечения (008, US1) в нишах модулей: вентилятор CDRA раскручивается и
 * тормозит (инерция ротора), в окне электролизёра поднимаются пузыри газа, катализатор Сабатье
 * пульсирует жаром ~300 °C, поршень насоса WRS качается. Анимация идёт по времени кадра и не влияет
 * на физику; без работы модуля его часть плавно останавливается.
 */
public class EclssRenderer implements BlockEntityRenderer<EclssControllerBlockEntity, EclssRenderState> {

    private final BlockModelResolver blocks;

    public EclssRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.blockModelResolver();
    }

    /** Клиентская анимация гнёзд: роторы вентиляторов и «уровень работы» (плавное включение). */
    private static final class Anim {
        final SmoothDrive.Spinner[] fans = {new SmoothDrive.Spinner(), new SmoothDrive.Spinner(),
                new SmoothDrive.Spinner(), new SmoothDrive.Spinner()};
        final SmoothDrive[] level = {new SmoothDrive(), new SmoothDrive(), new SmoothDrive(), new SmoothDrive()};
    }

    @Override
    public EclssRenderState createRenderState() {
        return new EclssRenderState();
    }

    @Override
    public void extractRenderState(EclssControllerBlockEntity be, EclssRenderState state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, breakProgress);
        state.facing = be.getBlockState().getValue(ControllerBlock.FACING);
        // свет — в воздушной клетке перед лицом контроллера: внутри непрозрачного блока он нулевой
        if (be.getLevel() != null) {
            state.lightCoords = net.minecraft.util.LightCoordsUtil.getLightCoords(be.getLevel(),
                    be.getBlockPos().relative(be.getBlockState().getValue(ControllerBlock.FACING)));
        }

        state.formed = be.formed();
        state.time = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + partialTick;
        if (!(be.clientAnim instanceof Anim)) {
            be.clientAnim = new Anim();
        }
        Anim anim = (Anim) be.clientAnim;
        for (int i = 0; i < 4; i++) {
            state.kind[i] = state.formed ? be.socketKind(i) : 0;
            boolean on = state.formed && be.socketRunning(i);
            state.level[i] = (float) anim.level[i].update(on ? 1 : 0, 1.5, 3);
            state.fanAngle[i] = (float) anim.fans[i].update(on && state.kind[i] == EclssControllerBlockEntity.CDRA ? 14 : 0, 1.2);
            BlockPos p = MultiblockTemplates.worldPos(BlockPos.ZERO, state.facing,
                    EclssControllerBlockEntity.SOCKETS[i][0], EclssControllerBlockEntity.SOCKETS[i][1], 0);
            state.offset[i][0] = p.getX();
            state.offset[i][1] = p.getY();
            state.offset[i][2] = p.getZ();
        }
        blocks.update(state.fan, ModBlocks.ECLSS_FAN.defaultBlockState(), state.context);
        blocks.update(state.piston, ModBlocks.ECLSS_PISTON.defaultBlockState(), state.context);
    }

    @Override
    public void submit(EclssRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        // модели частей смотрят лицом на север; поворот как у варианта блокстейта (y = toYRot + 180)
        float yRot = -(state.facing.toYRot() + 180);
        for (int i = 0; i < 4; i++) {
            int kind = state.kind[i];
            float lvl = state.level[i];
            if (kind == 0) {
                continue;
            }
            pose.pushPose();
            pose.translate(state.offset[i][0] + 0.5f, state.offset[i][1] + 0.5f, state.offset[i][2] + 0.5f);
            pose.mulPose(Axis.YP.rotationDegrees(yRot));
            pose.translate(-0.5f, -0.5f, -0.5f);
            switch (kind) {
                case EclssControllerBlockEntity.CDRA -> {
                    pose.pushPose();
                    pose.translate(0.5f, 0.5f, 0);
                    pose.mulPose(Axis.ZP.rotation(state.fanAngle[i]));
                    pose.translate(-0.5f, -0.5f, 0);
                    state.fan.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                    pose.popPose();
                }
                case EclssControllerBlockEntity.WRS -> {
                    pose.pushPose();
                    pose.translate(0, (float) (Math.sin(state.time * 0.35f + i) * 2.5 / 16 * lvl), 0);
                    state.piston.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                    pose.popPose();
                }
                case EclssControllerBlockEntity.OGS -> {
                    if (lvl > 0.01f) {
                        bubbles(pose, collector, state.time + i * 37, lvl);
                    }
                }
                case EclssControllerBlockEntity.SABATIER -> {
                    if (lvl > 0.01f) {
                        float pulse = (0.25f + 0.12f * (float) Math.sin(state.time * 0.08f + i)) * lvl;
                        collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> {
                            float z = 2.9f / 16;
                            GlowGeometry.quad(p, vc, 2 / 16f, 2 / 16f, z, 2 / 16f, 14 / 16f, z, 14 / 16f, 14 / 16f, z,
                                    14 / 16f, 2 / 16f, z, GlowGeometry.argb(pulse, 0xFF8A30));
                        });
                    }
                }
                default -> {
                }
            }
            pose.popPose();
        }
    }

    /** Пузыри O₂ и H₂ у электродов: поднимаются к поверхности и исчезают, скорость ∝ работе модуля. */
    private static void bubbles(PoseStack pose, SubmitNodeCollector collector, float time, float lvl) {
        collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> {
            float z = 2.2f / 16;
            for (int b = 0; b < 10; b++) {
                float speed = 0.09f + 0.03f * (b % 3);
                float phase = (time * speed * lvl + b * 0.37f) % 1f;
                float x = (b % 2 == 0 ? 5.2f : 10.2f) + ((b * 7) % 5) * 0.3f - 0.6f;
                float y = 4 + phase * 9;
                float s = 0.35f + 0.25f * ((b * 3) % 4) / 3f;
                int argb = GlowGeometry.argb(0.55f * lvl * (1 - phase * 0.6f), 0xDDF2FF);
                GlowGeometry.quad(p, vc, (x - s) / 16, (y - s) / 16, z, (x - s) / 16, (y + s) / 16, z,
                        (x + s) / 16, (y + s) / 16, z, (x + s) / 16, (y - s) / 16, z, argb);
            }
        });
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
