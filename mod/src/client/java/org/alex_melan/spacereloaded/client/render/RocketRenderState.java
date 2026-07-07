package org.alex_melan.spacereloaded.client.render;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.ArrayList;
import java.util.List;

/** Состояние рендера ракеты: по слоту на блок структуры. */
public class RocketRenderState extends EntityRenderState {
    public final List<BlockModelRenderState> models = new ArrayList<>();
    public final List<BlockDisplayContext> contexts = new ArrayList<>();
    public final LongArrayList positions = new LongArrayList();
    public int blockCount;
    public float pitchDeg;
    public float rollDeg;
    public float halfX;
    public float halfZ;
    public float comY;
}
