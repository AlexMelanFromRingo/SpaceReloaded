package org.alex_melan.spacereloaded.kinetics;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Вращающийся блок механической сети (005, FR-300/FR-301, D50): вид (правила связности),
 * момент инерции ротора и предел момента (вал — кручение, муфта — проскальзывание).
 */
public interface KineticBlock {

    /** Виды узлов: от вида зависят правила сцепления с соседями. */
    enum Kind {
        SHAFT,
        SMALL_GEAR,
        LARGE_GEAR,
        GEARBOX,
        CLUTCH,
        MOTOR,
        FLYWHEEL,
        PRESS,
        LATHE,
        WIND_HUB
    }

    /** Материал вала — предел кручения из конфига. */
    enum Material {
        NONE,
        WOOD,
        STEEL
    }

    Kind kind();

    /** Момент инерции ротора, кг·м². */
    double inertia();

    Material material();

    /** Ось вращения (null — угловой редуктор: все оси). */
    Direction.Axis axis(BlockState state);

    /** Передаёт ли блок вращение по своей оси (муфта разомкнута — нет). */
    default boolean transmitsAxially(BlockState state) {
        return kind() != Kind.GEARBOX;
    }
}
