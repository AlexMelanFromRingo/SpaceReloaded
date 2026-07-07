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

    @Override
    protected AABB getBoundingBoxForCulling(RocketEntity entity) {
        return entity.getBoundingBox().inflate(3.0);
    }
}
