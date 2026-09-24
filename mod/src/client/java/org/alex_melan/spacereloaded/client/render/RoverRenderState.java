package org.alex_melan.spacereloaded.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Кадр ровера: курс, число колёс, угол поворота колёс, батарея. */
public class RoverRenderState extends EntityRenderState {
    public float yaw;
    public int wheels;
    public boolean battery;
    public float wheelAngle;
    public final BlockModelRenderState body = new BlockModelRenderState();
    public final BlockModelRenderState wheel = new BlockModelRenderState();
    public final BlockModelRenderState pack = new BlockModelRenderState();
    public final BlockDisplayContext context = BlockDisplayContext.create();
}
