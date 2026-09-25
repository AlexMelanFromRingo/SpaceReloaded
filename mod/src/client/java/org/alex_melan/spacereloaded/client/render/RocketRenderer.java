package org.alex_melan.spacereloaded.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.rocket.RocketData;
import org.alex_melan.spacereloaded.rocket.RocketEntity;

import java.util.List;

/**
 * Рендер ракеты (T050, спайк D4 → решение): submit-конвейер 26.2, путь
 * TntRenderer — {@link BlockModelResolver} + {@link BlockModelRenderState}
 * с явным светом на каждый блок структуры. Тангаж/крен применяются к позе
 * вокруг центра масс. Запечённый общий буфер — оптимизация после стабилизации
 * Vulkan-пайплайна (сейчас каждый блок сабмитится отдельно; для сотен блоков
 * этого достаточно).
 */
public class RocketRenderer extends EntityRenderer<RocketEntity, RocketRenderState> {

    private final BlockModelResolver blockModelResolver;

    public RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
        this.shadowRadius = 0.0f;
    }

    @Override
    public RocketRenderState createRenderState() {
        return new RocketRenderState();
    }

    @Override
    public void extractRenderState(RocketEntity entity, RocketRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        List<RocketData.Entry> blocks = entity.clientBlocks();
        state.blockCount = blocks.size();
        state.pitchDeg = entity.pitchDeg();
        state.rollDeg = entity.rollDeg();
        state.halfX = entity.halfX();
        state.halfZ = entity.halfZ();
        state.comY = entity.comY();

        while (state.models.size() < state.blockCount) {
            state.models.add(new BlockModelRenderState());
            state.contexts.add(BlockDisplayContext.create());
        }
        state.positions.clear();
        for (int i = 0; i < state.blockCount; i++) {
            RocketData.Entry entry = blocks.get(i);
            blockModelResolver.update(state.models.get(i), entry.state(), state.contexts.get(i));
            state.positions.add(entry.localPos());
        }
        // 010: факел — у нижних двигателей (отработавшие ступени уже отделены отдельными телами)
        state.thrusting = entity.clientThrusting();
        state.plumes.clear();
        state.time = entity.tickCount + partialTick;
        if (state.thrusting) {
            int lowest = Integer.MAX_VALUE;
            for (RocketData.Entry e : blocks) {
                if (e.role().equals("engine")) {
                    lowest = Math.min(lowest, PackedPos.unpackY(e.localPos()));
                }
            }
            for (RocketData.Entry e : blocks) {
                if (e.role().equals("engine") && PackedPos.unpackY(e.localPos()) == lowest) {
                    state.plumes.add(new float[] {PackedPos.unpackX(e.localPos()) + 0.5f - state.halfX,
                            lowest, PackedPos.unpackZ(e.localPos()) + 0.5f - state.halfZ, fuelCode(e.fuel())});
                }
            }
            state.spreadTan = spread(entity);
        }
    }

    /** Код топлива для цвета факела: 0 — керолокс, 1 — метанокс, 2 — гидролокс. */
    private static float fuelCode(String fuel) {
        return fuel.endsWith("hydrolox") ? 2 : fuel.endsWith("methalox") ? 1 : 0;
    }

    /**
     * Раскрытие факела — по давлению среды на высоте ракеты: у земли струя перерасширена и узка
     * (≈ 4°), в вакууме газ уходит в стороны (≈ 30°) — ρ/ρ₀ = e^(−(y − y₀)/H) профиля планеты.
     */
    private static float spread(RocketEntity entity) {
        var level = entity.level();
        var access = level.registryAccess();
        var id = org.alex_melan.spacereloaded.planet.Navigation.entryIdFor(access, level.dimension().identifier());
        double ratio = 0;
        if (id != null) {
            var profile = org.alex_melan.spacereloaded.planet.PlanetManager.profileById(access, id);
            if (profile.isPresent() && profile.get().aero() != null && profile.get().aero().density() > 0) {
                var aero = profile.get().aero();
                ratio = Math.min(1, Math.exp(-(entity.getY() - aero.datumY()) / aero.scaleHeight()));
            }
        }
        return (float) Math.tan(Math.toRadians(4 + 26 * (1 - ratio)));
    }

    @Override
    public void submit(RocketRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // Поворот структуры вокруг центра масс
        poseStack.translate(0.0f, state.comY, 0.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitchDeg));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.rollDeg));
        poseStack.translate(0.0f, -state.comY, 0.0f);

        if (state.thrusting) {
            for (float[] plume : state.plumes) {
                submitPlume(poseStack, collector, plume, state.spreadTan, state.time);
            }
        }
        for (int i = 0; i < state.blockCount; i++) {
            long local = state.positions.getLong(i);
            poseStack.pushPose();
            poseStack.translate(
                    PackedPos.unpackX(local) - state.halfX,
                    (float) PackedPos.unpackY(local),
                    PackedPos.unpackZ(local) - state.halfZ);
            state.models.get(i).submit(poseStack, collector, state.lightCoords,
                    OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    /** Цвета (ядро, оболочка) по топливу: сажа керосина светится оранжевым, метан — синим, водород почти прозрачен. */
    private static final int[][] COLORS = {{0xFFF4C0, 0xFF8A2A}, {0xC8D8FF, 0x6A5AE0}, {0xE8F4FF, 0x7FB8FF}};
    private static final float[] ALPHA = {0.85f, 0.7f, 0.35f};

    /** Факел под соплом: вложенные усечённые конусы (ядро и оболочка) аддитивно, мерцание, скачки уплотнения. */
    private static void submitPlume(PoseStack pose, SubmitNodeCollector collector, float[] plume, float spreadTan, float time) {
        int kind = (int) plume[3];
        float flicker = 0.85f + 0.15f * (float) Math.sin(time * 2.7 + plume[0] * 3) * (float) Math.sin(time * 1.3 + plume[2]);
        float length = (2.2f + 0.6f * flicker) * (1 + 1.5f * spreadTan);
        float y0 = plume[1];
        collector.submitCustomGeometry(pose, net.minecraft.client.renderer.rendertype.RenderTypes.lightning(), (p, vc) -> {
            cone(p, vc, plume[0], y0, plume[2], 0.30f, 0.30f + length * spreadTan, length,
                    GlowGeometry.argb(ALPHA[kind] * 0.45f * flicker, COLORS[kind][1]));
            cone(p, vc, plume[0], y0, plume[2], 0.16f, 0.10f + length * spreadTan * 0.5f, length * 0.6f,
                    GlowGeometry.argb(ALPHA[kind] * flicker, COLORS[kind][0]));
            // скачки уплотнения (ромбы Маха) — только в плотной атмосфере (узкий факел)
            if (spreadTan < 0.12f) {
                for (int k = 1; k <= 3; k++) {
                    float yk = y0 - 0.45f * k;
                    float r = 0.12f;
                    GlowGeometry.cube(p, vc, plume[0] - r, yk - 0.06f, plume[2] - r, plume[0] + r, yk + 0.06f, plume[2] + r,
                            GlowGeometry.argb(0.6f * flicker / k, COLORS[kind][0]));
                }
            }
        });
    }

    /** Усечённый конус вниз от (x, y, z): радиус r0 у сопла, r1 на длине len; 12 граней. */
    private static void cone(com.mojang.blaze3d.vertex.PoseStack.Pose p, com.mojang.blaze3d.vertex.VertexConsumer vc,
                             float x, float y, float z, float r0, float r1, float len, int argb) {
        int n = 12;
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n, b = 2 * Math.PI * (i + 1) / n;
            float ax0 = x + (float) Math.cos(a) * r0, az0 = z + (float) Math.sin(a) * r0;
            float bx0 = x + (float) Math.cos(b) * r0, bz0 = z + (float) Math.sin(b) * r0;
            float ax1 = x + (float) Math.cos(a) * r1, az1 = z + (float) Math.sin(a) * r1;
            float bx1 = x + (float) Math.cos(b) * r1, bz1 = z + (float) Math.sin(b) * r1;
            GlowGeometry.quad(p, vc, ax0, y, az0, bx0, y, bz0, bx1, y - len, bz1, ax1, y - len, az1, argb);
        }
    }

    @Override
    protected AABB getBoundingBoxForCulling(RocketEntity entity) {
        return entity.getBoundingBox().inflate(3.0);
    }
}
