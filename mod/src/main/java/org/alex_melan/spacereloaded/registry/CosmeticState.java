package org.alex_melan.spacereloaded.registry;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Блок, часть свойств которого — только облик (горит, заряд, уровень, ток в кабеле). Смена
 * лишь таких свойств не должна будить пересчёт герметичности, кабельных сетей, механики и
 * структур (принцип III): индикатор не стоит пересборки зоны.
 */
public interface CosmeticState {

    /** Отличаются ли состояния только косметическими свойствами. */
    boolean onlyCosmetic(BlockState before, BlockState after);
}
