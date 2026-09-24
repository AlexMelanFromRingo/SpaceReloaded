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
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.client.render.anim.SmoothDrive;
import org.alex_melan.spacereloaded.comms.DsnBlockEntity;
import org.alex_melan.spacereloaded.core.comms.SkyGeometry;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/**
 * Антенна дальней связи (008, US6): параболоид F/D = 0.4 из плиток-панелей (каждая по нормали к
 * поверхности), облучатель в фокусе на четырёх растяжках; тарелка качается на вилке вокруг оси
 * север — юг, сопровождая цель по дуге неба с ограничением скорости привода (DSN 0.25°/с, ускорено);
 * цель под горизонтом — походное положение в зенит; во время сеанса светится облучатель.
 */
public class DsnRenderer implements BlockEntityRenderer<DsnBlockEntity, DsnRenderState> {

    private static final float PIVOT_Y = 1 + 12.5f / 16;
    private final BlockModelResolver blocks;

    public DsnRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.blockModelResolver();
    }

    @Override
    public DsnRenderState createRenderState() {
        return new DsnRenderState();
    }

    @Override
    public void extractRenderState(DsnBlockEntity be, DsnRenderState state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, breakProgress);
        state.formed = be.formed();
        state.panels = be.panels();
        double d = Math.max(1, be.diameterM());
        state.focal = (float) (0.4 * d);
        var level = be.getLevel();
        double target = Math.PI / 2;
        if (level != null) {
            state.lightCoords = net.minecraft.util.LightCoordsUtil.getLightCoords(level, be.getBlockPos().above(3));
            var own = DsnBlockEntity.bodyOf(level.dimension().identifier());
            double sky = DsnBlockEntity.skyAngle(level.getDefaultClockTime(), level.getGameTime(), own, be.targetBody());
            sky = Math.floorMod((long) Math.floor(Math.toDegrees(sky) * 1000), 360_000L) / 1000.0;
            if (SkyGeometry.aboveHorizon(Math.toRadians(sky))) {
                target = Math.toRadians(sky);
            }
            state.time = level.getGameTime() + partialTick;
        }
        if (!(be.clientAnim instanceof SmoothDrive)) {
            be.clientAnim = new SmoothDrive();
        }
        // наклон от зенита: 0 — зенит, отрицательный — на восток (+x), положительный — на запад
        double tiltDeg = Math.toDegrees(target) - 90;
        state.tilt = (float) ((SmoothDrive) be.clientAnim).update(tiltDeg, 5, 1.5);
        state.session = be.rateBps() > 0;
        blocks.update(state.tile, ModBlocks.DISH_TILE.defaultBlockState(), state.context);
        blocks.update(state.horn, ModBlocks.FEED_HORN.defaultBlockState(), state.context);
    }

    @Override
    public void submit(DsnRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed || state.panels.length == 0) {
            return;
        }
        float f = state.focal;
        pose.pushPose();
        pose.translate(0.5f, PIVOT_Y, 0.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(state.tilt));
        float lift = 0.3f;
        for (int i = 0; i + 1 < state.panels.length; i += 2) {
            float dx = state.panels[i];
            float dz = state.panels[i + 1];
            float h = (dx * dx + dz * dz) / (4 * f);
            pose.pushPose();
            pose.translate(dx, lift + h, dz);
            pose.mulPose(Axis.ZP.rotation((float) Math.atan(dx / (2 * f))));
            pose.mulPose(Axis.XP.rotation((float) -Math.atan(dz / (2 * f))));
            pose.translate(-0.5f, 0, -0.5f);
            state.tile.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        // облучатель в фокусе (раструбом вниз, к зеркалу) и четыре растяжки от кромки
        float rim = (float) Math.sqrt(state.panels.length / 2.0 / Math.PI) * 0.8f;
        for (int k = 0; k < 4; k++) {
            float ax = k == 0 ? rim : k == 1 ? -rim : 0;
            float az = k == 2 ? rim : k == 3 ? -rim : 0;
            float ay = lift + (rim * rim) / (4 * f);
            float vx = -ax, vy = lift + f - ay, vz = -az;
            float len = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
            pose.pushPose();
            pose.translate(ax, ay, az);
            if (az == 0) {
                pose.mulPose(Axis.ZP.rotation((float) Math.atan2(-vx, vy)));
            } else {
                pose.mulPose(Axis.XP.rotation((float) Math.atan2(vz, vy)));
            }
            pose.scale(1 / 16f, len, 1 / 16f);
            pose.translate(-0.5f, 0, -0.5f);
            state.tile.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.pushPose();
        pose.translate(0, lift + f, 0);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.translate(-0.5f, 0, -0.5f);
        state.horn.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        if (state.session) {
            float a = 0.35f + 0.15f * (float) Math.sin(state.time * 0.2f);
            collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> GlowGeometry.quad(p, vc,
                    6.2f / 16, -0.01f, 6.2f / 16, 9.8f / 16, -0.01f, 6.2f / 16, 9.8f / 16, -0.01f, 9.8f / 16,
                    6.2f / 16, -0.01f, 9.8f / 16, GlowGeometry.argb(a, 0x6FD5E8)));
        }
        pose.popPose();
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }
}
