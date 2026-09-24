package org.alex_melan.spacereloaded.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Состояние рендера антенны (008): панели тарелки, наклон сопровождения, фокус. */
public class DsnRenderState extends BlockEntityRenderState {
    public boolean formed;
    public int[] panels = new int[0];
    public float tilt;
    public float focal;
    public boolean session;
    public float time;
    public final BlockModelRenderState tile = new BlockModelRenderState();
    public final BlockModelRenderState horn = new BlockModelRenderState();
    public final BlockDisplayContext context = BlockDisplayContext.create();
}
