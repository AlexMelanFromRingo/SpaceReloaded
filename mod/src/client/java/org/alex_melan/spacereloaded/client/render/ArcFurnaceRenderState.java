package org.alex_melan.spacereloaded.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Состояние рендера дуговой печи (008): поворот свода, ход электродов, наклон, дуга, струя. */
public class ArcFurnaceRenderState extends BlockEntityRenderState {
    public boolean formed;
    public float cx, cz;
    public float yaw;
    public float roofOpen;
    public float electrodes;
    public float tilt;
    public float melt;
    public boolean arc;
    public float time;
    public final BlockModelRenderState vessel = new BlockModelRenderState();
    public final BlockModelRenderState roof = new BlockModelRenderState();
    public final BlockModelRenderState electrode = new BlockModelRenderState();
    public final BlockDisplayContext context = BlockDisplayContext.create();
}
