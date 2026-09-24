package org.alex_melan.spacereloaded.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/** Состояние рендера реактора (008): ход стержня, температура, поршни Стирлингов. */
public class ReactorRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public boolean formed;
    public float rod;
    public float temperature;
    public int stirlingMask;
    public float pistonPhase;
    public float pistonAmp;
    public final float[][] slot = new float[4][3];
    public final float[] hotPipe = new float[3];
    public final BlockModelRenderState rodModel = new BlockModelRenderState();
    public final BlockModelRenderState piston = new BlockModelRenderState();
    public final BlockDisplayContext context = BlockDisplayContext.create();
}
