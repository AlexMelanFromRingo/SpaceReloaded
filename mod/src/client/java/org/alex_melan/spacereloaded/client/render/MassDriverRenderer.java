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
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.industry.MassDriverBreechBlock;
import org.alex_melan.spacereloaded.industry.MassDriverBreechBlockEntity;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/**
 * Анимация катапульты (004, FR-211, D33): один рендерер на казённик рисует весь рельс.
 * (а) Заряд — пульсирующий ореол первых секций пропорционально заряду батареи.
 * (б) Выстрел — бегущая волна свечения от казённика к срезу за {@code massDriverWaveTicks}
 * (реальный разгон ≈ 0.1 с — художественно замедлен), вспышка у среза.
 * (в) Салазки едут от среза обратно к казённику за время перезарядки.
 * Состояние клиента не хранится: всё — функция (время − тик выстрела).
 */
public class MassDriverRenderer implements BlockEntityRenderer<MassDriverBreechBlockEntity, MassDriverRenderState> {

    private static final int WAVE_RGB = 0x55C8FF;
    private static final int FLASH_RGB = 0xE8F6FF;
    private static final int CHARGE_RGB = 0x3A7BFF;

    private final BlockModelResolver blocks;

    public MassDriverRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.blockModelResolver();
    }

    @Override
    public MassDriverRenderState createRenderState() {
        return new MassDriverRenderState();
    }

    @Override
    public void extractRenderState(MassDriverBreechBlockEntity be, MassDriverRenderState state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, breakProgress);
        state.facing = be.getBlockState().getValue(MassDriverBreechBlock.FACING);
        state.railLength = be.clientRailLength();
        state.time = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + partialTick;
        state.sinceShot = state.time - be.shotTick();
        state.charge = be.chargeFraction();
        state.waveTicks = SpaceReloaded.config().massDriverWaveTicks;
        int returnTicks = SpaceReloaded.config().massDriverSledReturnTicks;
        float remaining = be.sledUntil() - state.time;
        state.sledPosition = remaining > 0 && state.railLength > 0
                ? 0.5f + (state.railLength - 0.5f) * Math.min(1f, remaining / returnTicks)
                : 0.5f;
        blocks.update(state.sled, ModBlocks.MASS_DRIVER_SLED.defaultBlockState(), state.context);
    }

    @Override
    public void submit(MassDriverRenderState state, PoseStack pose, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (state.railLength <= 0) {
            return;
        }
        float dx = state.facing.getStepX();
        float dz = state.facing.getStepZ();

        // (в) салазки поверх рельса
        pose.pushPose();
        pose.translate(dx * state.sledPosition, 1.0f, dz * state.sledPosition);
        state.sled.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        float wave = state.sinceShot;
        boolean firing = wave >= 0 && wave <= state.waveTicks + 5;
        int chargeSections = firing ? 0 : (int) Math.ceil(state.charge * Math.min(8, state.railLength));
        if (!firing && chargeSections == 0) {
            return;
        }
        collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> {
            if (firing) {
                float head = wave / state.waveTicks * state.railLength;
                int from = Math.max(1, (int) Math.floor(head - 4));
                int to = Math.min(state.railLength, (int) Math.ceil(head));
                for (int i = from; i <= to; i++) {
                    float alpha = 0.55f * (1f - (head - i) / 4f);
                    if (alpha > 0) {
                        section(p, vc, dx * i, dz * i, GlowGeometry.argb(alpha, WAVE_RGB), 0.06f);
                    }
                }
                if (wave >= state.waveTicks) {
                    float flash = 1f - (wave - state.waveTicks) / 5f;
                    int muzzle = state.railLength + 1;
                    section(p, vc, dx * muzzle, dz * muzzle, GlowGeometry.argb(0.7f * flash, FLASH_RGB),
                            0.4f * flash + 0.1f);
                }
            } else {
                float pulse = 0.18f + 0.12f * (float) Math.sin(state.time * 0.25f);
                for (int i = 1; i <= chargeSections; i++) {
                    section(p, vc, dx * i, dz * i, GlowGeometry.argb(pulse, CHARGE_RGB), 0.03f);
                }
            }
        });
    }

    private static void section(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vc,
                                float ox, float oz, int argb, float grow) {
        GlowGeometry.cube(pose, vc, ox - grow, -grow, oz - grow, ox + 1 + grow, 1 + grow, oz + 1 + grow, argb);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
