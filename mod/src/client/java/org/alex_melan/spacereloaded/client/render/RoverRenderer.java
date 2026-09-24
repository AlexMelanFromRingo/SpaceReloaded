package org.alex_melan.spacereloaded.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.vehicle.RoverEntity;

/**
 * Ровер (007): рама 2 × 3 м и сиденья — модель-блок, четыре мотор-колеса Ø 81 см крутятся по
 * пройденному пути (угол = путь / радиус), ящик Ni–Fe батареи за сиденьями.
 */
public class RoverRenderer extends EntityRenderer<RoverEntity, RoverRenderState> {

    private final BlockModelResolver blocks;

    public RoverRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockModelResolver();
        this.shadowRadius = 1.2f;
    }

    @Override
    public RoverRenderState createRenderState() {
        return new RoverRenderState();
    }

    @Override
    public void extractRenderState(RoverEntity rover, RoverRenderState state, float partialTick) {
        super.extractRenderState(rover, state, partialTick);
        state.yaw = rover.getYRot(partialTick);
        state.wheels = rover.wheels();
        state.battery = rover.hasBattery();
        state.wheelAngle = rover.wheelAngle();
        blocks.update(state.body, ModBlocks.ROVER_BODY_MODEL.defaultBlockState(), state.context);
        blocks.update(state.wheel, ModBlocks.ROVER_WHEEL_MODEL.defaultBlockState(), state.context);
        blocks.update(state.pack, ModBlocks.ROVER_BATTERY_MODEL.defaultBlockState(), state.context);
    }

    @Override
    public void submit(RoverRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        pose.translate(-0.5f, 0f, -0.5f);
        state.body.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        if (state.battery) {
            state.pack.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        float[][] corners = {{-0.6f, 1.2f}, {1.6f, 1.2f}, {-0.6f, -1.2f}, {1.6f, -1.2f}};
        for (int i = 0; i < state.wheels && i < 4; i++) {
            pose.pushPose();
            pose.translate(corners[i][0], 0f, corners[i][1]);
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.mulPose(Axis.XP.rotation(state.wheelAngle));
            pose.translate(-0.5f, -0.5f, -0.5f);
            state.wheel.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            pose.popPose();
        }
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
