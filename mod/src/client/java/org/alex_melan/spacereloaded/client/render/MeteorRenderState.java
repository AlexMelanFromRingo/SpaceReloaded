package org.alex_melan.spacereloaded.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class MeteorRenderState extends EntityRenderState {
    public final BlockModelRenderState model = new BlockModelRenderState();
    public final BlockDisplayContext context = BlockDisplayContext.create();
}
