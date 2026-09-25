package org.alex_melan.spacereloaded.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Состояние рендера орбитальной пушки (010): откат ствола, вспышка у дула, заряд батареи. */
public class OrbitalCannonRenderState extends BlockEntityRenderState {
    public float recoil;
    public float flash;
    public float charge;
    public float time;
    public final BlockModelRenderState barrel = new BlockModelRenderState();
    public final BlockDisplayContext context = BlockDisplayContext.create();
}
