package org.alex_melan.spacereloaded.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Состояние рендера каскада (008): роторы центрифуг. */
public class CascadeRenderState extends BlockEntityRenderState {
    public boolean formed;
    public int count;
    public float angle;
    public float[] dx = new float[40];
    public float[] dz = new float[40];
    public final BlockModelRenderState rotor = new BlockModelRenderState();
    public final BlockDisplayContext context = BlockDisplayContext.create();
}
