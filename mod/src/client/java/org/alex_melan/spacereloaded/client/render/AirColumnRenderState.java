package org.alex_melan.spacereloaded.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/** Состояние рендера воздухоразделительной колонны (008): иней, уровень LOX, детандер, шапка. */
public class AirColumnRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public boolean formed;
    public int trays;
    public float frost;
    public float lox;
    public float wheel;
    public final float[] exchanger = new float[3];
    public final BlockModelRenderState wheelModel = new BlockModelRenderState();
    public final BlockModelRenderState cap = new BlockModelRenderState();
    public final BlockDisplayContext context = BlockDisplayContext.create();
}
