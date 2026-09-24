package org.alex_melan.spacereloaded.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/** Состояние рендера стойки жизнеобеспечения (008): модули гнёзд, фазы анимации. */
public class EclssRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public boolean formed;
    public final int[] kind = new int[4];
    public final float[][] offset = new float[4][3];
    public final float[] fanAngle = new float[4];
    public final float[] level = new float[4];
    public float time;
    public final BlockModelRenderState fan = new BlockModelRenderState();
    public final BlockModelRenderState piston = new BlockModelRenderState();
    public final BlockDisplayContext context = BlockDisplayContext.create();
}
