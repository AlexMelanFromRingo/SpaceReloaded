package org.alex_melan.spacereloaded.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class RegolithReactorRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public boolean lit;
    public float time;
}
