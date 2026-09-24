package org.alex_melan.spacereloaded.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/** Снимок анимации катапульты на кадр: всё — функция игрового времени и синхронизированных тиков. */
public class MassDriverRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public int railLength;
    /** Время кадра (gameTime + partialTick). */
    public float time;
    public float sinceShot = Float.MAX_VALUE;
    /** Позиция салазок в секциях от казённика (0.5 — у казённика). */
    public float sledPosition = 0.5f;
    public float charge;
    public int waveTicks = 10;
    public final BlockModelRenderState sled = new BlockModelRenderState();
    public final BlockDisplayContext context = BlockDisplayContext.create();
}
