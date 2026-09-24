package org.alex_melan.spacereloaded.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/** Кадр вращающегося узла: ось, угол (функция времени), до двух моделей-роторов, ход штока пресса. */
public class KineticRenderState extends BlockEntityRenderState {
    public Direction.Axis axis = Direction.Axis.Y;
    public float angleDeg;
    public boolean hasShaft;
    public boolean hasPart;
    public float ramOffset;
    public boolean isRam;
    public final BlockModelRenderState shaft = new BlockModelRenderState();
    public final BlockModelRenderState part = new BlockModelRenderState();
    public final BlockDisplayContext context = BlockDisplayContext.create();
}
