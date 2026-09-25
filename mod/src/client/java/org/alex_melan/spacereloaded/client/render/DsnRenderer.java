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
 * Антенна дальней связи (008, US6): параболоид F/D = 0.4 — сплошная оболочка по панелям (общие
 * вершины, нормали поверхности), облучатель в фокусе на четырёх растяжках; тарелка качается на вилке вокруг оси
 * север — юг, сопровождая цель по дуге неба с ограничением скорости привода (DSN 0.25°/с, ускорено);
 * цель под горизонтом — походное положение в зенит; во время сеанса светится облучатель.
 */
public class DsnRenderer implements BlockEntityRenderer<DsnBlockEntity, DsnRenderState> {

    private static final float PIVOT_Y = 1 + 12.5f / 16;
    private static final net.minecraft.resources.Identifier DISH_TEXTURE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("spacereloaded", "textures/block/dish_panel.png");
    /** Толщина оболочки зеркала, блоки. */
    private static final float SHELL = 1.5f / 16;
    private static final int FRONT = 0xFFFFFFFF;
    private static final int BACK = 0xFFA8ADB4;

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /** Вершина на поверхности параболоида (или на тыльной, со сдвигом вниз) с нормалью поверхности. */
    private static void vertex(com.mojang.blaze3d.vertex.PoseStack.Pose p, com.mojang.blaze3d.vertex.VertexConsumer vc,
                               float x, float z, float f, float base, float u, float v, int color, int light, boolean back) {
        float y = base + (x * x + z * z) / (4 * f);
        float nx = -x / (2 * f), nz = -z / (2 * f);
        float len = (float) Math.sqrt(nx * nx + 1 + nz * nz);
        float s = back ? -1 / len : 1 / len;
        vc.addVertex(p, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(p, nx * s, s, nz * s);
    }

    private static final net.minecraft.resources.Identifier STRUT_TEXTURE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("spacereloaded", "textures/block/dsn_mount.png");

    /** Брус квадратного сечения 2w от точки A к точке B (четыре боковые грани). */
    private static void bar(com.mojang.blaze3d.vertex.PoseStack.Pose p, com.mojang.blaze3d.vertex.VertexConsumer vc,
                            float ax, float ay, float az, float bx, float by, float bz, float w, int light) {
        org.joml.Vector3f d = new org.joml.Vector3f(bx - ax, by - ay, bz - az).normalize();
        org.joml.Vector3f u = Math.abs(d.y) < 0.9f ? new org.joml.Vector3f(0, 1, 0) : new org.joml.Vector3f(1, 0, 0);
        org.joml.Vector3f e1 = d.cross(u, new org.joml.Vector3f()).normalize().mul(w);
        org.joml.Vector3f e2 = d.cross(e1, new org.joml.Vector3f()).normalize().mul(w);
        org.joml.Vector3f[] side = {e1, e2, new org.joml.Vector3f(e1).negate(), new org.joml.Vector3f(e2).negate()};
        for (int i = 0; i < 4; i++) {
            org.joml.Vector3f s0 = side[i], s1 = side[(i + 1) % 4];
            org.joml.Vector3f n = new org.joml.Vector3f(s0).add(s1).normalize();
            float[][] q = {
                    {ax + s0.x, ay + s0.y, az + s0.z, 0, 0}, {bx + s0.x, by + s0.y, bz + s0.z, 0, 1},
                    {bx + s1.x, by + s1.y, bz + s1.z, 0.1f, 1}, {ax + s1.x, ay + s1.y, az + s1.z, 0.1f, 0}};
            // порядок обхода — наружу по n: (b − a) × (c − b) сонаправлен n
            org.joml.Vector3f ab = new org.joml.Vector3f(q[1][0] - q[0][0], q[1][1] - q[0][1], q[1][2] - q[0][2]);
            org.joml.Vector3f bc = new org.joml.Vector3f(q[2][0] - q[1][0], q[2][1] - q[1][1], q[2][2] - q[1][2]);
            boolean flip = ab.cross(bc).dot(n) < 0;
            for (int j = 0; j < 4; j++) {
                float[] v = q[flip ? 3 - j : j];
                vc.addVertex(p, v[0], v[1], v[2]).setColor(FRONT).setUv(v[3], v[4]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light).setNormal(p, n.x, n.y, n.z);
            }
        }
    }

    /** Торец кромки от (ax, az) к (bx, bz), нормаль (nx, 0, nz) наружу. */
    private static void rim(com.mojang.blaze3d.vertex.PoseStack.Pose p, com.mojang.blaze3d.vertex.VertexConsumer vc,
                            float ax, float az, float bx, float bz, float f, float lift, float nx, float nz, int light) {
        float ya = lift + (ax * ax + az * az) / (4 * f), yb = lift + (bx * bx + bz * bz) / (4 * f);
        vc.addVertex(p, ax, ya - SHELL, az).setColor(BACK).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, 0, nz);
        vc.addVertex(p, bx, yb - SHELL, bz).setColor(BACK).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, 0, nz);
        vc.addVertex(p, bx, yb, bz).setColor(BACK).setUv(1, 0.9f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, 0, nz);
        vc.addVertex(p, ax, ya, az).setColor(BACK).setUv(0, 0.9f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, 0, nz);
    }
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
        state.tilt = (float) ((SmoothDrive) be.clientAnim).update(tiltDeg, org.alex_melan.spacereloaded.SpaceReloaded.config().dsnDriveDegPerS, 1.5);
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
        // зеркало — сплошная оболочка по параболоиду y = r²/4f: вершины в углах клеток общие для
        // соседних панелей, поэтому поверхность без щелей и ступенек; торцы — только по кромке
        java.util.Set<Long> cells = new java.util.HashSet<>();
        for (int i = 0; i + 1 < state.panels.length; i += 2) {
            cells.add(key(state.panels[i], state.panels[i + 1]));
        }
        int light = state.lightCoords;
        int[] panels = state.panels;
        collector.submitCustomGeometry(pose, RenderTypes.entitySolid(DISH_TEXTURE), (p, vc) -> {
            for (int i = 0; i + 1 < panels.length; i += 2) {
                int cx = panels[i], cz = panels[i + 1];
                float x0 = cx - 0.5f, x1 = cx + 0.5f, z0 = cz - 0.5f, z1 = cz + 0.5f;
                // вогнутая сторона к облучателю
                vertex(p, vc, x0, z1, f, lift, 0, 1, FRONT, light, false);
                vertex(p, vc, x1, z1, f, lift, 1, 1, FRONT, light, false);
                vertex(p, vc, x1, z0, f, lift, 1, 0, FRONT, light, false);
                vertex(p, vc, x0, z0, f, lift, 0, 0, FRONT, light, false);
                // тыльная сторона
                vertex(p, vc, x0, z1, f, lift - SHELL, 0, 1, BACK, light, true);
                vertex(p, vc, x0, z0, f, lift - SHELL, 0, 0, BACK, light, true);
                vertex(p, vc, x1, z0, f, lift - SHELL, 1, 0, BACK, light, true);
                vertex(p, vc, x1, z1, f, lift - SHELL, 1, 1, BACK, light, true);
                if (!cells.contains(key(cx - 1, cz))) {
                    rim(p, vc, x0, z0, x0, z1, f, lift, -1, 0, light);
                }
                if (!cells.contains(key(cx + 1, cz))) {
                    rim(p, vc, x1, z1, x1, z0, f, lift, 1, 0, light);
                }
                if (!cells.contains(key(cx, cz - 1))) {
                    rim(p, vc, x1, z0, x0, z0, f, lift, 0, -1, light);
                }
                if (!cells.contains(key(cx, cz + 1))) {
                    rim(p, vc, x0, z1, x1, z1, f, lift, 0, 1, light);
                }
            }
        });
        // четыре растяжки от кромки к облучателю в фокусе — брусья 1.5 px
        float rim = (float) Math.sqrt(state.panels.length / 2.0 / Math.PI) * 0.8f;
        float rimY = lift + (rim * rim) / (4 * f);
        collector.submitCustomGeometry(pose, RenderTypes.entitySolid(STRUT_TEXTURE), (p, vc) -> {
            for (int k = 0; k < 4; k++) {
                float ax = k == 0 ? rim : k == 1 ? -rim : 0;
                float az = k == 2 ? rim : k == 3 ? -rim : 0;
                bar(p, vc, ax, rimY, az, 0, lift + f - 0.3f, 0, 0.75f / 16, light);
            }
        });
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
