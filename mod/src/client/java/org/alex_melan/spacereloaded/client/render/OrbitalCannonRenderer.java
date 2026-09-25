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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.cannon.OrbitalCannonBlockEntity;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/**
 * Орбитальная пушка (010, US2): ствол — отдельная модель на рендере. Выстрел — откат ствола в
 * люльку за ~30 мс и плавный накат противооткатником (τ ≈ 0.6 с), у дула — вспышка плазмы
 * электромагнитного разгона (гаснет за ~0.1 с) и искры. Заряд накопителя подсвечивает кольцо
 * башни голубым по доле энергии.
 */
public class OrbitalCannonRenderer implements BlockEntityRenderer<OrbitalCannonBlockEntity, OrbitalCannonRenderState> {

    /** Ход отката, блоки (4 px). */
    private static final float STROKE = 4 / 16f;
    private final BlockModelResolver blocks;

    public OrbitalCannonRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.blockModelResolver();
    }

    @Override
    public OrbitalCannonRenderState createRenderState() {
        return new OrbitalCannonRenderState();
    }

    @Override
    public void extractRenderState(OrbitalCannonBlockEntity be, OrbitalCannonRenderState state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, breakProgress);
        var level = be.getLevel();
        float t = level == null ? 1e6f : (level.getGameTime() - be.lastFireGameTime() + partialTick) / 20f;
        // откат: быстрый уход (τ₁ 30 мс) и накат (τ₂ 0.6 с)
        state.recoil = t < 0 || t > 4 ? 0 : STROKE * (float) ((1 - Math.exp(-t / 0.03)) * Math.exp(-t / 0.6));
        state.flash = t < 0 || t > 0.5f ? 0 : (float) Math.exp(-t / 0.1);
        state.charge = be.chargeFraction();
        state.time = level == null ? 0 : level.getGameTime() + partialTick;
        if (level != null && state.flash > 0.5f && level.getRandom().nextFloat() < 0.6f) {
            var p = be.getBlockPos();
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.5, p.getY() + 1.72, p.getZ() + 0.5,
                    (level.getRandom().nextFloat() - 0.5) * 0.4, 0.3, (level.getRandom().nextFloat() - 0.5) * 0.4);
        }
        blocks.update(state.barrel, ModBlocks.ORBITAL_CANNON_BARREL.defaultBlockState(), state.context);
    }

    @Override
    public void submit(OrbitalCannonRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0, -state.recoil, 0);
        state.barrel.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        // кольцо накопителя на башне: голубое по доле заряда, пульсирует при полном
        if (state.charge > 0.02f) {
            float a = 0.12f + 0.35f * state.charge * (state.charge >= 0.99f ? 0.8f + 0.2f * (float) Math.sin(state.time * 0.3) : 1);
            collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> GlowGeometry.cube(p, vc,
                    2.9f / 16, 12.2f / 16, 2.9f / 16, 13.1f / 16, 13.2f / 16, 13.1f / 16, GlowGeometry.argb(a, 0x6FD5E8)));
        }
        if (state.flash > 0.01f) {
            float f = state.flash;
            float r = (1.5f + 3 * f) / 16;
            collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> GlowGeometry.cube(p, vc,
                    0.5f - r, 27 / 16f, 0.5f - r, 0.5f + r, 27 / 16f + 4 * r, 0.5f + r, GlowGeometry.argb(0.9f * f, 0xDDE8FF)));
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
